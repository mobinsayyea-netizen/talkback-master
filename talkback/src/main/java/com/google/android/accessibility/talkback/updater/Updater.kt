package com.google.android.accessibility.talkback.updater

import android.app.Activity
import android.app.AlertDialog
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageInstaller
import android.net.Uri
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.widget.Toast
import org.json.JSONObject
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

/**
 * Checks GitHub Releases for a newer build than the one installed, and — with the user's
 * confirmation — downloads and installs it via PackageInstaller. Adapted from the same-named
 * helper in the Mobin-Launcher project.
 */
object Updater {

    private const val REPO = "mobinsayyea-netizen/talkback-master"

    class Release(val build: Int, val name: String, val apkUrl: String)

    @Suppress("DEPRECATION")
    fun installedBuild(ctx: Context): Int {
        val pi = ctx.packageManager.getPackageInfo(ctx.packageName, 0)
        return if (Build.VERSION.SDK_INT >= 28) pi.longVersionCode.toInt() else pi.versionCode
    }

    fun installedName(ctx: Context): String {
        return try {
            ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    private fun fetchLatest(): Release? {
        var conn: HttpURLConnection? = null
        try {
            conn = URL("https://api.github.com/repos/$REPO/releases/latest").openConnection() as HttpURLConnection
            conn.setRequestProperty("Accept", "application/vnd.github+json")
            conn.setRequestProperty("User-Agent", "MSScreenReader")
            conn.connectTimeout = 15000
            conn.readTimeout = 15000
            if (conn.responseCode != 200) return null
            val body = conn.inputStream.bufferedReader().use { it.readText() }
            val j = JSONObject(body)
            val tag = j.getString("tag_name")
            // Debug build-log releases are tagged "buildlog-N"; only "build-N" tags are real APK
            // releases.
            if (!tag.startsWith("build-")) return null
            val build = tag.removePrefix("build-").toIntOrNull() ?: return null
            val assets = j.getJSONArray("assets")
            for (i in 0 until assets.length()) {
                val a = assets.getJSONObject(i)
                if (a.getString("name").endsWith(".apk")) {
                    return Release(build, j.optString("name", tag), a.getString("browser_download_url"))
                }
            }
            return null
        } catch (e: Exception) {
            return null
        } finally {
            conn?.disconnect()
        }
    }

    private fun download(ctx: Context, url: String): File? {
        var conn: HttpURLConnection? = null
        try {
            val out = File(ctx.cacheDir, "update.apk")
            conn = URL(url).openConnection() as HttpURLConnection
            conn.instanceFollowRedirects = true
            conn.setRequestProperty("User-Agent", "MSScreenReader")
            conn.connectTimeout = 20000
            conn.readTimeout = 60000
            if (conn.responseCode != 200) return null
            conn.inputStream.use { input ->
                FileOutputStream(out).use { output -> input.copyTo(output) }
            }
            return out
        } catch (e: Exception) {
            return null
        } finally {
            conn?.disconnect()
        }
    }

    private fun startInstall(ctx: Context, apk: File): Boolean {
        return try {
            val installer = ctx.packageManager.packageInstaller
            val params = PackageInstaller.SessionParams(PackageInstaller.SessionParams.MODE_FULL_INSTALL)
            val id = installer.createSession(params)
            val session = installer.openSession(id)
            FileInputStream(apk).use { input ->
                session.openWrite("update.apk", 0, apk.length()).use { out ->
                    input.copyTo(out)
                    session.fsync(out)
                }
            }
            val intent = Intent(ctx, InstallReceiver::class.java)
            var flags = PendingIntent.FLAG_UPDATE_CURRENT
            if (Build.VERSION.SDK_INT >= 31) flags = flags or PendingIntent.FLAG_MUTABLE
            val pending = PendingIntent.getBroadcast(ctx, id, intent, flags)
            session.commit(pending.intentSender)
            session.close()
            true
        } catch (e: Exception) {
            false
        }
    }

    fun canInstall(ctx: Context): Boolean =
        if (Build.VERSION.SDK_INT >= 26) ctx.packageManager.canRequestPackageInstalls() else true

    private fun message(activity: Activity, title: String, text: String, onDismiss: () -> Unit = {}) {
        AlertDialog.Builder(activity)
            .setTitle(title)
            .setMessage(text)
            .setPositiveButton("OK", null)
            .setOnDismissListener { onDismiss() }
            .show()
    }

    /** Checks for a newer build. [manual] = the user tapped "Check for updates" themselves, so
     *  always tell them the result (even "already up to date"); otherwise (an automatic check on
     *  opening Settings) stay silent unless there is an update, and only prompt once per build.
     *  [onDismissed] runs once whatever dialog this shows (if any) has been dismissed — used by a
     *  throwaway "Check for updates" screen to close itself; a persistent screen can ignore it. */
    fun check(activity: Activity, manual: Boolean, onDismissed: () -> Unit = {}) {
        val handler = Handler(Looper.getMainLooper())
        if (manual) Toast.makeText(activity, "Checking for updates", Toast.LENGTH_SHORT).show()
        Thread {
            val latest = fetchLatest()
            handler.post {
                if (activity.isFinishing) return@post
                if (latest == null) {
                    if (manual) {
                        message(
                            activity,
                            "Update check",
                            "Could not check for updates. Please check your internet and try again.",
                            onDismissed)
                    } else {
                        onDismissed()
                    }
                    return@post
                }
                val installed = installedBuild(activity)
                if (latest.build > installed) {
                    val prefs = activity.getSharedPreferences("ms_updater", Context.MODE_PRIVATE)
                    if (manual || prefs.getInt("notified_build", 0) != latest.build) {
                        prefs.edit().putInt("notified_build", latest.build).apply()
                        prompt(activity, latest, onDismissed)
                    } else {
                        onDismissed()
                    }
                } else if (manual) {
                    message(
                        activity,
                        "Update check",
                        "You already have the latest version, ${installedName(activity)}.",
                        onDismissed)
                } else {
                    onDismissed()
                }
            }
        }.start()
    }

    private fun prompt(activity: Activity, r: Release, onDismissed: () -> Unit) {
        // onDismissed only means "the user is done here and nothing else is pending" — so it
        // fires for Later/cancel, but NOT for Update now, which keeps the activity alive through
        // the download and hands off to downloadAndInstall's own onDismissed instead.
        var updating = false
        AlertDialog.Builder(activity)
            .setTitle("Update available")
            .setMessage("New version ${r.name} is available. You have version ${installedName(activity)}. Update now?")
            .setPositiveButton("Update now") { _, _ ->
                updating = true
                downloadAndInstall(activity, r, onDismissed)
            }
            .setNegativeButton("Later", null)
            .setOnDismissListener { if (!updating) onDismissed() }
            .show()
    }

    private fun downloadAndInstall(activity: Activity, r: Release, onDismissed: () -> Unit = {}) {
        if (!canInstall(activity)) {
            AlertDialog.Builder(activity)
                .setTitle("Allow updates")
                .setMessage("To install updates, allow this app to install apps. Settings will open. Turn on the switch, press Back, then tap Check for updates again.")
                .setPositiveButton("Open settings") { _, _ ->
                    try {
                        activity.startActivity(
                            Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${activity.packageName}"))
                        )
                    } catch (e: Exception) {
                    }
                }
                .setNegativeButton("Cancel", null)
                .setOnDismissListener { onDismissed() }
                .show()
            return
        }
        Toast.makeText(activity, "Downloading update", Toast.LENGTH_LONG).show()
        val handler = Handler(Looper.getMainLooper())
        Thread {
            val file = download(activity, r.apkUrl)
            handler.post {
                if (activity.isFinishing) {
                    return@post
                }
                if (file == null) {
                    message(activity, "Update failed", "The download did not finish. Please try again.", onDismissed)
                } else if (!startInstall(activity, file)) {
                    message(activity, "Update failed", "Could not start the installation.", onDismissed)
                } else {
                    // The system's own install confirmation takes over from here.
                    onDismissed()
                }
            }
        }.start()
    }
}

class InstallReceiver : BroadcastReceiver() {
    @Suppress("DEPRECATION")
    override fun onReceive(context: Context, intent: Intent) {
        val status = intent.getIntExtra(PackageInstaller.EXTRA_STATUS, PackageInstaller.STATUS_FAILURE)
        when (status) {
            PackageInstaller.STATUS_PENDING_USER_ACTION -> {
                val confirm = intent.getParcelableExtra<Intent>(Intent.EXTRA_INTENT)
                if (confirm != null) {
                    confirm.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                    context.startActivity(confirm)
                }
            }
            PackageInstaller.STATUS_SUCCESS -> {
            }
            else -> {
                val msg = intent.getStringExtra(PackageInstaller.EXTRA_STATUS_MESSAGE) ?: "unknown error"
                Toast.makeText(context, "Update failed: $msg", Toast.LENGTH_LONG).show()
            }
        }
    }
}
