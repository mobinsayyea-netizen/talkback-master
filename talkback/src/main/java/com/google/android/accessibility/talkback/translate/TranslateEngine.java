package com.google.android.accessibility.talkback.translate;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Handler;
import android.os.Looper;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.style.LocaleSpan;
import com.google.mlkit.common.model.DownloadConditions;
import com.google.mlkit.common.model.RemoteModelManager;
import com.google.mlkit.nl.languageid.LanguageIdentification;
import com.google.mlkit.nl.translate.TranslateLanguage;
import com.google.mlkit.nl.translate.TranslateRemoteModel;
import com.google.mlkit.nl.translate.Translation;
import com.google.mlkit.nl.translate.Translator;
import com.google.mlkit.nl.translate.TranslatorOptions;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/** On-phone translation used by the Translate reading control. */
public final class TranslateEngine {

  public static final String PREFS = "ms_translate";
  public static final String KEY_TARGET = "target";
  public static final String KEY_SOURCE = "source";
  public static final String KEY_DOWNLOAD_OK = "download_ok";
  public static final String AUTO = "auto";

  /** Receives the text that should be spoken. */
  public interface Callback {
    void speak(CharSequence text);
  }

  private static Runnable pending;

  private TranslateEngine() {}

  public static SharedPreferences prefs(Context context) {
    return context.getApplicationContext().getSharedPreferences(PREFS, Context.MODE_PRIVATE);
  }

  public static String target(Context context) {
    return prefs(context).getString(KEY_TARGET, "hi");
  }

  public static String source(Context context) {
    return prefs(context).getString(KEY_SOURCE, AUTO);
  }

  public static String languageName(String code) {
    String name = Locale.forLanguageTag(code).getDisplayLanguage();
    return (name == null || name.isEmpty()) ? code : name;
  }

  public static void runPending() {
    final Runnable r = pending;
    pending = null;
    if (r != null) {
      new Handler(Looper.getMainLooper()).post(r);
    }
  }

  public static void clearPending() {
    pending = null;
  }

  /** Downloads language packs over any network (mobile data or Wi-Fi). */
  public static void downloadModels(List<String> languages, Runnable onDone, Runnable onFail) {
    DownloadConditions conditions = new DownloadConditions.Builder().build();
    RemoteModelManager manager = RemoteModelManager.getInstance();
    final int[] left = {languages.size()};
    final boolean[] failed = {false};
    if (languages.isEmpty()) {
      onDone.run();
      return;
    }
    for (String language : languages) {
      manager
          .download(new TranslateRemoteModel.Builder(language).build(), conditions)
          .addOnSuccessListener(
              unused -> {
                left[0]--;
                if (left[0] == 0 && !failed[0]) {
                  onDone.run();
                }
              })
          .addOnFailureListener(
              e -> {
                if (!failed[0]) {
                  failed[0] = true;
                  onFail.run();
                }
              });
    }
  }

  /** Translates {@code text} and passes what should be spoken to the callback. */
  public static void translate(Context context, String text, Callback callback) {
    final Context app = context.getApplicationContext();
    final String target = target(app);
    String source = source(app);
    if (AUTO.equals(source)) {
      LanguageIdentification.getClient()
          .identifyLanguage(text)
          .addOnSuccessListener(
              code -> {
                if (code == null || "und".equals(code)) {
                  callback.speak("Could not detect the language");
                } else {
                  continueTranslate(app, text, code, target, callback);
                }
              })
          .addOnFailureListener(e -> callback.speak("Could not detect the language"));
    } else {
      continueTranslate(app, text, source, target, callback);
    }
  }

  private static void continueTranslate(
      Context app, String text, String sourceTag, String targetTag, Callback callback) {
    final String source = TranslateLanguage.fromLanguageTag(sourceTag);
    final String target = TranslateLanguage.fromLanguageTag(targetTag);
    if (source == null || target == null) {
      callback.speak("This language is not supported for translation");
      return;
    }
    if (source.equals(target)) {
      callback.speak(text);
      return;
    }
    RemoteModelManager.getInstance()
        .getDownloadedModels(TranslateRemoteModel.class)
        .addOnSuccessListener(
            models -> {
              Set<String> have = new HashSet<>();
              for (TranslateRemoteModel m : models) {
                have.add(m.getLanguage());
              }
              List<String> need = new ArrayList<>();
              need.add(source);
              need.add(target);
              if (!TranslateLanguage.ENGLISH.equals(source)
                  && !TranslateLanguage.ENGLISH.equals(target)) {
                need.add(TranslateLanguage.ENGLISH);
              }
              final List<String> missing = new ArrayList<>();
              for (String n : need) {
                if (!have.contains(n)) {
                  missing.add(n);
                }
              }
              if (missing.isEmpty()) {
                doTranslate(text, source, target, callback);
              } else if (prefs(app).getBoolean(KEY_DOWNLOAD_OK, false)) {
                callback.speak("Downloading language pack");
                downloadModels(
                    missing,
                    () -> doTranslate(text, source, target, callback),
                    () -> callback.speak("Download failed. Check your internet connection."));
              } else {
                pending = () -> translate(app, text, callback);
                Intent intent = new Intent(app, TranslateSetupActivity.class);
                intent.putExtra("langs", missing.toArray(new String[0]));
                intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                app.startActivity(intent);
              }
            })
        .addOnFailureListener(e -> callback.speak("Translation is not available"));
  }

  private static void doTranslate(
      String text, String source, final String target, final Callback callback) {
    TranslatorOptions options =
        new TranslatorOptions.Builder().setSourceLanguage(source).setTargetLanguage(target).build();
    final Translator translator = Translation.getClient(options);
    translator
        .translate(text)
        .addOnSuccessListener(
            result -> {
              if (result == null || result.trim().isEmpty()) {
                callback.speak("Translation failed");
              } else {
                SpannableString spoken = new SpannableString(result);
                spoken.setSpan(
                    new LocaleSpan(Locale.forLanguageTag(target)),
                    0,
                    spoken.length(),
                    Spanned.SPAN_INCLUSIVE_EXCLUSIVE);
                callback.speak(spoken);
              }
              translator.close();
            })
        .addOnFailureListener(
            e -> {
              callback.speak("Translation failed");
              translator.close();
            });
  }
}
