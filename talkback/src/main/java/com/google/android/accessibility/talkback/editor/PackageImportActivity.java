/*
 * Copyright 2026 MS Screen Reader. Licensed under the Apache License, Version 2.0.
 */
package com.google.android.accessibility.talkback.editor;

import android.app.Activity;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;

/** Opens when a .ppk or .xpk file is opened from a file manager, WhatsApp or Telegram. */
public class PackageImportActivity extends Activity {
  @Override
  protected void onCreate(Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);
    Intent i = getIntent();
    Uri uri = i == null ? null : i.getData();
    if (uri == null && i != null && Intent.ACTION_SEND.equals(i.getAction())) {
      uri = i.getParcelableExtra(Intent.EXTRA_STREAM);
    }
    if (uri == null) {
      finish();
      return;
    }
    PackageImporter.importUri(this, uri, null, this::finish);
  }
}
