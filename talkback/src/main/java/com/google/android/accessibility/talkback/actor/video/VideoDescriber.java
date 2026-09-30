package com.google.android.accessibility.talkback.actor.video;

import static com.google.android.accessibility.utils.Performance.EVENT_ID_UNTRACKED;

import android.accessibilityservice.AccessibilityService;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.graphics.Bitmap;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import android.util.Base64;
import com.google.android.accessibility.talkback.Feedback;
import com.google.android.accessibility.talkback.Pipeline;
import com.google.android.accessibility.talkback.actor.gemini.GeminiKeyStore;
import com.google.android.accessibility.talkback.translate.TranslateEngine;
import com.google.android.accessibility.utils.output.SpeechController;
import com.google.android.accessibility.utils.output.SpeechController.SpeakOptions;
import com.google.android.accessibility.utils.screencapture.ScreenshotCapture;
import java.io.ByteArrayOutputStream;
import java.util.Locale;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;
import okhttp3.WebSocket;
import okhttp3.WebSocketListener;
import org.json.JSONObject;

/**
 * Live video description. While it runs, a small picture of the screen is taken about once a
 * second (using the screenshot permission the screen reader already has, so no screen-recording
 * permission is needed) and sent to Gemini Live. Gemini's answer is spoken with the normal screen
 * reader voice. It stops when the screen turns off or when the menu item is used again.
 */
public final class VideoDescriber {

  private static final String LIVE_URL =
      "wss://generativelanguage.googleapis.com/ws/google.ai.generativelanguage.v1beta."
          + "GenerativeService.BidiGenerateContent?key=";
  private static final int MAX_SIDE = 640;
  private static final long FRAME_EVERY_MS = 1000;
  private static final long MIN_GAP_AFTER_TURN_MS = 1500;
  private static final long SPEAK_STUCK_MS = 25000;
  private static final int MAX_CONNECT_FAILS = 6;

  private static VideoDescriber instance;

  public static synchronized boolean isRunning() {
    return instance != null && instance.running;
  }

  /** Starts the description. Does nothing if it is already running. */
  public static synchronized void start(AccessibilityService service, Pipeline.FeedbackReturner p) {
    if (instance != null && instance.running) {
      return;
    }
    if (Build.VERSION.SDK_INT < 30) {
      speakPlain(service, p, "Video description needs Android 11 or newer");
      return;
    }
    if (GeminiKeyStore.key(service).isEmpty()) {
      GeminiKeyStore.onMissingKey(service);
      return;
    }
    if (GeminiKeyStore.inQuotaCooldown(service)) {
      GeminiKeyStore.quotaShortMessage(service);
      return;
    }
    instance = new VideoDescriber(service, p);
    instance.begin();
  }

  /** Stops the description and says the given words (if not null). */
  public static synchronized void stop(String words) {
    if (instance != null) {
      instance.end(words);
    }
  }

  // ---------------------------------------------------------------------------------------------

  private final AccessibilityService service;
  private final Pipeline.FeedbackReturner pipeline;
  private final Handler main = new Handler(Looper.getMainLooper());
  private static final ExecutorService WORKER = Executors.newSingleThreadExecutor();
  private static final OkHttpClient CLIENT =
      new OkHttpClient.Builder().pingInterval(20, TimeUnit.SECONDS).build();

  private volatile boolean running = false;
  private volatile boolean ready = false;
  private volatile boolean turnActive = false;
  private volatile boolean shot = false;
  private volatile long lastTurnEnd = 0;
  private volatile int framesSinceTick = 0;
  private volatile int pendingSpeech = 0;
  private volatile long speechSince = 0;
  private int connectFails = 0;
  private int modeIndex = 0; // 0 = text answers, 1 = audio answers with transcript
  private boolean fullConfig = true;
  private WebSocket socket;
  private int[] lastSample;
  private final StringBuilder sentence = new StringBuilder();
  private BroadcastReceiver screenOffReceiver;
  private final String lang;

  private VideoDescriber(AccessibilityService service, Pipeline.FeedbackReturner pipeline) {
    this.service = service;
    this.pipeline = pipeline;
    this.lang = TranslateEngine.target(service);
  }

  private void begin() {
    running = true;
    screenOffReceiver =
        new BroadcastReceiver() {
          @Override
          public void onReceive(Context context, Intent intent) {
            synchronized (VideoDescriber.class) {
              if (instance == VideoDescriber.this) {
                end("Video description stop");
              }
            }
          }
        };
    service.registerReceiver(screenOffReceiver, new IntentFilter(Intent.ACTION_SCREEN_OFF));
    speak("Video description started", true);
    connect();
    main.postDelayed(frameLoop, 2000);
    main.postDelayed(tickLoop, 2500);
  }

  private void end(String words) {
    if (!running) {
      return;
    }
    running = false;
    ready = false;
    main.removeCallbacksAndMessages(null);
    try {
      service.unregisterReceiver(screenOffReceiver);
    } catch (RuntimeException ignored) {
    }
    if (socket != null) {
      try {
        socket.close(1000, "bye");
      } catch (RuntimeException ignored) {
      }
    }
    if (words != null) {
      speak(words, true);
    }
    if (instance == this) {
      instance = null;
    }
  }

  // ------------------------------------------------------------------------------ connection

  private String prompt() {
    String language = Locale.forLanguageTag(lang).getDisplayLanguage(Locale.ENGLISH);
    return "You are the live eyes of a blind person who is watching a video (YouTube or any other"
        + " video, or a live stream) on a phone. You receive a picture of the phone screen about"
        + " once a second. Describe ONLY what is visible and what is happening in the video, like"
        + " an audio describer: who and what is on the screen, what they wear and look like,"
        + " the place, movements and actions, scene changes, and any text shown on the screen"
        + " (read it out). Do not guess feelings, names, history or the story, and do not invent"
        + " anything you cannot see. Ignore the phone's buttons and menus unless they matter."
        + " When the app sends [TICK], describe only what is NEW or has changed since your last"
        + " description, in one to three short natural sentences. If nothing important changed,"
        + " answer with just a single dot. Speak "
        + language
        + ". Plain spoken text only: no lists, no markdown, no emojis, no preamble.";
  }

  private void connect() {
    if (!running) {
      return;
    }
    ready = false;
    turnActive = false;
    final boolean audioMode = modeIndex == 1;
    String url = LIVE_URL + GeminiKeyStore.key(service);
    Request request = new Request.Builder().url(url).build();
    socket =
        CLIENT.newWebSocket(
            request,
            new WebSocketListener() {
              @Override
              public void onOpen(WebSocket ws, Response response) {
                try {
                  JSONObject setup = new JSONObject();
                  setup.put("model", "models/" + GeminiKeyStore.liveModel(service));
                  JSONObject gen = new JSONObject();
                  gen.put("responseModalities", new org.json.JSONArray().put(audioMode ? "AUDIO" : "TEXT"));
                  setup.put("generationConfig", gen);
                  setup.put(
                      "systemInstruction",
                      new JSONObject()
                          .put(
                              "parts",
                              new org.json.JSONArray().put(new JSONObject().put("text", prompt()))));
                  if (fullConfig) {
                    setup.put(
                        "contextWindowCompression",
                        new JSONObject().put("slidingWindow", new JSONObject()));
                  }
                  if (audioMode) {
                    setup.put("outputAudioTranscription", new JSONObject());
                  }
                  ws.send(new JSONObject().put("setup", setup).toString());
                } catch (Exception e) {
                  fail("Video description could not start");
                }
              }

              @Override
              public void onMessage(WebSocket ws, String text) {
                handleMessage(ws, text, audioMode);
              }

              @Override
              public void onMessage(WebSocket ws, okio.ByteString bytes) {
                handleMessage(ws, bytes.utf8(), audioMode);
              }

              @Override
              public void onClosed(WebSocket ws, int code, String reason) {
                onDropped(ws, code, null);
              }

              @Override
              public void onFailure(WebSocket ws, Throwable t, Response response) {
                onDropped(ws, response == null ? 0 : response.code(), t);
              }
            });
  }

  private void onDropped(WebSocket ws, int code, Throwable t) {
    if (!running || ws != socket) {
      return;
    }
    if (code == 429) {
      GeminiKeyStore.onQuotaFinished(service);
      synchronized (VideoDescriber.class) {
        end("Video description stop");
      }
      return;
    }
    if (code == 401 || code == 403) {
      GeminiKeyStore.onInvalidKey(service);
      synchronized (VideoDescriber.class) {
        end("Video description stop");
      }
      return;
    }
    final boolean wasReady = ready;
    ready = false;
    if (!wasReady) {
      // The first connection failed before it was ready: try simpler settings, then audio answers.
      if (fullConfig) {
        fullConfig = false;
        main.post(this::connect);
        return;
      }
      if (modeIndex < 1) {
        modeIndex++;
        main.post(this::connect);
        return;
      }
      if (++connectFails > MAX_CONNECT_FAILS) {
        fail("Video description stopped. Connection problem");
        return;
      }
      main.postDelayed(this::connect, 1500);
      return;
    }
    // A working connection dropped (Gemini asks for a fresh one every so often): reconnect quietly.
    main.postDelayed(this::connect, 300);
  }

  private void fail(String words) {
    synchronized (VideoDescriber.class) {
      if (instance == this) {
        end(words);
      }
    }
  }

  private void handleMessage(WebSocket ws, String text, boolean audioMode) {
    if (!running || ws != socket) {
      return;
    }
    try {
      JSONObject m = new JSONObject(text);
      if (m.has("setupComplete")) {
        ready = true;
        connectFails = 0;
        lastTurnEnd = System.currentTimeMillis();
        return;
      }
      if (m.has("goAway")) {
        ws.close(1000, "refresh");
        return;
      }
      JSONObject c = m.optJSONObject("serverContent");
      if (c == null) {
        return;
      }
      JSONObject turn = c.optJSONObject("modelTurn");
      if (!audioMode && turn != null) {
        org.json.JSONArray parts = turn.optJSONArray("parts");
        for (int i = 0; parts != null && i < parts.length(); i++) {
          JSONObject p = parts.getJSONObject(i);
          if (p.has("text") && !p.optBoolean("thought", false)) {
            feed(p.getString("text"));
          }
        }
      }
      JSONObject out = c.optJSONObject("outputTranscription");
      if (out != null && out.has("text")) {
        feed(out.getString("text"));
      }
      if (c.optBoolean("turnComplete", false)) {
        flushSentence();
        turnActive = false;
        lastTurnEnd = System.currentTimeMillis();
      }
    } catch (Exception ignored) {
    }
  }

  // ------------------------------------------------------------------------------------ speech

  private void feed(String piece) {
    sentence.append(piece);
    while (true) {
      String s = sentence.toString();
      int cut = -1;
      for (int i = 0; i < s.length(); i++) {
        char ch = s.charAt(i);
        if ((ch == '.' || ch == '!' || ch == '?' || ch == '\u0964' || ch == '\n') && i >= 24) {
          cut = i + 1;
          break;
        }
      }
      if (cut < 0) {
        return;
      }
      String piece1 = s.substring(0, cut);
      sentence.delete(0, cut);
      emit(piece1);
    }
  }

  private void flushSentence() {
    if (sentence.length() > 0) {
      String s = sentence.toString();
      sentence.setLength(0);
      emit(s);
    }
  }

  private void emit(String raw) {
    String t = raw.replaceAll("[*_#`~>]", " ").replaceAll("\\s+", " ").trim();
    if (t.isEmpty() || t.matches("[\\s.\\-\u2013\u2026]*")) {
      return;
    }
    speak(t, false);
  }

  private void speak(final String text, final boolean interrupt) {
    main.post(
        () -> {
          pendingSpeech++;
          speechSince = System.currentTimeMillis();
          SpeakOptions options =
              SpeakOptions.create()
                  .setQueueMode(
                      interrupt
                          ? SpeechController.QUEUE_MODE_INTERRUPT
                          : SpeechController.QUEUE_MODE_QUEUE)
                  .setCompletedAction(
                      status ->
                          main.post(
                              () -> {
                                if (pendingSpeech > 0) {
                                  pendingSpeech--;
                                }
                              }));
          pipeline.returnFeedback(
              EVENT_ID_UNTRACKED, Feedback.speech(withLanguage(text), options));
        });
  }

  private static void speakPlain(AccessibilityService s, Pipeline.FeedbackReturner p, String t) {
    p.returnFeedback(EVENT_ID_UNTRACKED, Feedback.speech(t));
  }

  /** Marks Hindi (Devanagari) and English parts so the voice switches language correctly. */
  private static CharSequence withLanguage(String text) {
    SpannableString out = new SpannableString(text);
    int n = text.length();
    int i = 0;
    while (i < n) {
      Boolean hindi = letterIsHindi(text.charAt(i));
      if (hindi == null) {
        i++;
        continue;
      }
      int j = i + 1;
      while (j < n) {
        Boolean other = letterIsHindi(text.charAt(j));
        if (other != null && !other.equals(hindi)) {
          break;
        }
        j++;
      }
      Locale locale = hindi ? new Locale("hi", "IN") : new Locale("en", "IN");
      out.setSpan(new LocaleSpan(locale), i, j, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
      i = j;
    }
    return out;
  }

  private static Boolean letterIsHindi(char ch) {
    if (ch >= 0x0900 && ch <= 0x097F) {
      return Boolean.TRUE;
    }
    return Character.isLetter(ch) ? Boolean.FALSE : null;
  }

  // -------------------------------------------------------------------------------- the loops

  private final Runnable frameLoop =
      new Runnable() {
        @Override
        public void run() {
          if (!running) {
            return;
          }
          if (ready && !shot) {
            shot = true;
            try {
              ScreenshotCapture.takeScreenshot(
                  service, (bitmap, supported) -> onShot(bitmap), WORKER);
            } catch (RuntimeException e) {
              shot = false;
            }
          }
          main.postDelayed(this, FRAME_EVERY_MS);
        }
      };

  private final Runnable tickLoop =
      new Runnable() {
        @Override
        public void run() {
          if (!running) {
            return;
          }
          long now = System.currentTimeMillis();
          if (pendingSpeech > 0 && now - speechSince > SPEAK_STUCK_MS) {
            pendingSpeech = 0;
          }
          if (ready
              && !turnActive
              && pendingSpeech == 0
              && framesSinceTick > 0
              && now - lastTurnEnd > MIN_GAP_AFTER_TURN_MS) {
            framesSinceTick = 0;
            turnActive = true;
            sendText("[TICK] Describe only what is new since your last description.");
          }
          main.postDelayed(this, 500);
        }
      };

  private void sendText(String t) {
    try {
      JSONObject msg =
          new JSONObject().put("realtimeInput", new JSONObject().put("text", t));
      WebSocket s = socket;
      if (s != null) {
        s.send(msg.toString());
      }
    } catch (Exception ignored) {
    }
  }

  /** Runs on the worker thread. */
  private void onShot(Bitmap bitmap) {
    Bitmap small = null;
    try {
      if (!running || bitmap == null || !ready) {
        return;
      }
      int w = bitmap.getWidth();
      int h = bitmap.getHeight();
      float scale = Math.min(1f, (float) MAX_SIDE / Math.max(w, h));
      small =
          Bitmap.createScaledBitmap(
              bitmap, Math.max(1, Math.round(w * scale)), Math.max(1, Math.round(h * scale)), true);
      if (!changed(small)) {
        return;
      }
      ByteArrayOutputStream bos = new ByteArrayOutputStream();
      small.compress(Bitmap.CompressFormat.JPEG, 65, bos);
      String b64 = Base64.encodeToString(bos.toByteArray(), Base64.NO_WRAP);
      JSONObject video = new JSONObject().put("data", b64).put("mimeType", "image/jpeg");
      WebSocket s = socket;
      if (s != null) {
        s.send(
            new JSONObject()
                .put("realtimeInput", new JSONObject().put("video", video))
                .toString());
        framesSinceTick++;
      }
    } catch (Exception ignored) {
    } finally {
      if (small != null && small != bitmap && !small.isRecycled()) {
        small.recycle();
      }
      if (bitmap != null && !bitmap.isRecycled()) {
        bitmap.recycle();
      }
      shot = false;
    }
  }

  /** Cheap check so that an unchanged screen costs no Gemini quota. */
  private boolean changed(Bitmap b) {
    int sw = 24;
    int sh = 24;
    Bitmap tiny = Bitmap.createScaledBitmap(b, sw, sh, true);
    int[] px = new int[sw * sh];
    tiny.getPixels(px, 0, sw, 0, 0, sw, sh);
    if (tiny != b) {
      tiny.recycle();
    }
    int[] gray = new int[px.length];
    for (int i = 0; i < px.length; i++) {
      int c = px[i];
      gray[i] = (((c >> 16) & 0xff) * 3 + ((c >> 8) & 0xff) * 6 + (c & 0xff)) / 10;
    }
    int[] prev = lastSample;
    lastSample = gray;
    if (prev == null) {
      return true;
    }
    long diff = 0;
    for (int i = 0; i < gray.length; i++) {
      diff += Math.abs(gray[i] - prev[i]);
    }
    return diff / (double) gray.length > 1.5;
  }
}
