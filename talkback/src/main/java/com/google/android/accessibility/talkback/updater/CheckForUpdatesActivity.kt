package com.google.android.accessibility.talkback.updater

import android.app.Activity
import android.os.Bundle

/** A transparent, momentary activity: runs an update check (always telling the user the result,
 *  since they tapped this on purpose) and finishes once any dialog it shows is dismissed. */
class CheckForUpdatesActivity : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        Updater.check(this, manual = true, onDismissed = { finish() })
    }
}
