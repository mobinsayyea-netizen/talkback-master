package com.google.android.accessibility.talkback.editor;

import android.content.Context;
import com.google.android.accessibility.talkback.actor.gemini.GeminiKeyStore;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.List;
import org.json.JSONArray;
import org.json.JSONObject;

/** Minimal blocking Gemini text client using the user's own key and model. Worker thread only. */
public final class GeminiClient {
  private GeminiClient() {}

  /** One question, no history. */
  public static String ask(Context ctx, String system, String prompt) throws IOException {
    java.util.ArrayList<String[]> turns = new java.util.ArrayList<>();
    turns.add(new String[] {"user", prompt});
    return chat(ctx, system, turns);
  }

  /** turns: each item is {"user" or "model", text}. Returns the model's text. */
  public static String chat(Context ctx, String system, List<String[]> turns) throws IOException {
    String key = GeminiKeyStore.key(ctx);
    if (key.isEmpty()) {
      throw new IOException("Gemini key is not set");
    }
    String model = GeminiKeyStore.model(ctx);
    try {
      JSONObject body = new JSONObject();
      if (system != null && !system.isEmpty()) {
        body.put(
            "systemInstruction",
            new JSONObject()
                .put("parts", new JSONArray().put(new JSONObject().put("text", system))));
      }
      JSONArray contents = new JSONArray();
      for (String[] t : turns) {
        contents.put(
            new JSONObject()
                .put("role", t[0])
                .put("parts", new JSONArray().put(new JSONObject().put("text", t[1]))));
      }
      body.put("contents", contents);
      body.put("generationConfig", new JSONObject().put("temperature", 0.4));
      URL url =
          new URL(
              "https://generativelanguage.googleapis.com/v1beta/models/"
                  + model
                  + ":generateContent");
      HttpURLConnection c = (HttpURLConnection) url.openConnection();
      c.setConnectTimeout(20000);
      c.setReadTimeout(120000);
      c.setRequestMethod("POST");
      c.setRequestProperty("Content-Type", "application/json; charset=utf-8");
      c.setRequestProperty("x-goog-api-key", key);
      c.setDoOutput(true);
      OutputStream os = c.getOutputStream();
      os.write(body.toString().getBytes("UTF-8"));
      os.close();
      int code = c.getResponseCode();
      InputStream is = code >= 400 ? c.getErrorStream() : c.getInputStream();
      String resp = read(is);
      if (code >= 400) {
        String msg = resp;
        try {
          msg = new JSONObject(resp).getJSONObject("error").getString("message");
        } catch (Exception ignored) {
        }
        if (code == 429) {
          throw new IOException("Gemini quota finished or too many requests. " + msg);
        }
        throw new IOException("Gemini error " + code + ": " + msg);
      }
      JSONArray cands = new JSONObject(resp).optJSONArray("candidates");
      if (cands == null || cands.length() == 0) {
        throw new IOException("Gemini returned no answer");
      }
      JSONArray parts = cands.getJSONObject(0).getJSONObject("content").optJSONArray("parts");
      StringBuilder sb = new StringBuilder();
      if (parts != null) {
        for (int i = 0; i < parts.length(); i++) {
          sb.append(parts.getJSONObject(i).optString("text", ""));
        }
      }
      return sb.toString();
    } catch (IOException e) {
      throw e;
    } catch (Exception e) {
      throw new IOException(String.valueOf(e.getMessage()), e);
    }
  }

  private static String read(InputStream is) throws IOException {
    if (is == null) return "";
    ByteArrayOutputStream bo = new ByteArrayOutputStream();
    byte[] b = new byte[8192];
    int n;
    while ((n = is.read(b)) > 0) {
      bo.write(b, 0, n);
    }
    is.close();
    return bo.toString("UTF-8");
  }
}
