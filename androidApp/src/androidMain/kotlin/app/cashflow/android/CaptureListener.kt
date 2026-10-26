package app.cashflow.android

import android.app.Notification
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.os.Handler
import android.os.Looper
import java.security.MessageDigest

class CaptureListener : NotificationListenerService() {
    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (sbn.packageName == packageName || sbn.notification.flags and Notification.FLAG_GROUP_SUMMARY != 0) return
        Handler(Looper.getMainLooper()).post {
            val app = application as CashflowApplication
            if (!app.store.isSelected(sbn.packageName) || app.store.state.value.capturePaused) return@post
            val extras = sbn.notification.extras
            val title = extras.getCharSequence(Notification.EXTRA_TITLE)?.toString().orEmpty()
            val body = (extras.getCharSequence(Notification.EXTRA_BIG_TEXT) ?: extras.getCharSequence(Notification.EXTRA_TEXT))?.toString().orEmpty()
            val text = listOf(title, body).filter { it.isNotBlank() }.distinct().joinToString("\n")
            if (text.isBlank()) return@post
            val hash = MessageDigest.getInstance("SHA-256").digest(text.toByteArray()).joinToString("") { "%02x".format(it) }
            val deliveryKey = "${sbn.packageName}:${sbn.key}:${sbn.postTime}:$hash"
            app.store.ingest(text, sbn.packageName, deliveryKey)
        }
    }
    override fun onListenerConnected() { (application as CashflowApplication).store.refreshCapture() }
}
