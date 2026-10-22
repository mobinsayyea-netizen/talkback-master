package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.webkit.JavascriptInterface;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import com.google.android.accessibility.talkback.actor.gemini.GeminiKeyStore;

/**
 * "Create Android APK": the user's own APK Builder (chat first: the AI makes the repo, files and
 * workflow, builds and gives the direct APK link). The page is assets/apk_builder.html. The Gemini
 * key saved in MS Screen Reader is handed to the page, so it does not have to be typed again.
 */
public class ApkBuilderActivity extends Activity {
  private static final int FILE_CHOOSER_REQ = 77;
  private ValueCallback<Uri[]> filePathCallback;
  private WebView web;

  /** Only the local asset page can call this. */
  private final class Host {
    @JavascriptInterface
    public String geminiKey() {
      return GeminiKeyStore.key(ApkBuilderActivity.this);
    }

    @JavascriptInterface
    public String geminiModel() {
      return GeminiKeyStore.model(ApkBuilderActivity.this);
    }
  }

  @Override
  protected void onCreate(Bundle b) {
    super.onCreate(b);
    setTitle("Create Android APK");
    web = new WebView(this);
    WebSettings st = web.getSettings();
    st.setJavaScriptEnabled(true);
    st.setDomStorageEnabled(true);
    st.setAllowFileAccess(true);
    st.setAllowFileAccessFromFileURLs(false);
    st.setAllowUniversalAccessFromFileURLs(false);
    web.addJavascriptInterface(new Host(), "MsHost");
    web.setWebViewClient(
        new WebViewClient() {
          @Override
          public boolean shouldOverrideUrlLoading(WebView v, String url) {
            if (url != null && (url.startsWith("http://") || url.startsWith("https://"))) {
              openOutside(url);
              return true;
            }
            return false;
          }
        });
    web.setDownloadListener(
        (url, ua, cd, mime, len) -> openOutside(url));
    web.setWebChromeClient(
        new WebChromeClient() {
          @Override
          public boolean onShowFileChooser(
              WebView view, ValueCallback<Uri[]> callback, FileChooserParams params) {
            if (filePathCallback != null) {
              filePathCallback.onReceiveValue(null);
            }
            filePathCallback = callback;
            try {
              startActivityForResult(params.createIntent(), FILE_CHOOSER_REQ);
            } catch (ActivityNotFoundException e) {
              filePathCallback = null;
              callback.onReceiveValue(null);
              return false;
            }
            return true;
          }
        });
    web.loadUrl("file:///android_asset/apk_builder.html");
    setContentView(web);
  }

  private void openOutside(String url) {
    try {
      Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(url));
      startActivity(i);
    } catch (Exception ignored) {
    }
  }

  @Override
  public void onBackPressed() {
    if (web != null && web.canGoBack()) {
      web.goBack();
    } else {
      super.onBackPressed();
    }
  }

  @Override
  protected void onActivityResult(int requestCode, int resultCode, Intent data) {
    if (requestCode == FILE_CHOOSER_REQ) {
      if (filePathCallback != null) {
        filePathCallback.onReceiveValue(
            WebChromeClient.FileChooserParams.parseResult(resultCode, data));
        filePathCallback = null;
      }
      return;
    }
    super.onActivityResult(requestCode, resultCode, data);
  }
}
