package app.cashflow.android

import android.Manifest
import android.app.*
import android.content.*
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import app.cashflow.core.*
import app.cashflow.db.LedgerDatabase
import java.io.File
import java.time.OffsetDateTime
import java.util.UUID
import java.util.concurrent.TimeUnit
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager

class CashflowApplication : Application(), PlatformHooks {
    lateinit var store: LedgerStore
        private set
    lateinit var subscriptions: SubscriptionService
        private set
    var activity: MainActivity? = null
    override fun onCreate() {
        super.onCreate()
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(NotificationChannel("recorded", "Recorded transactions", NotificationManager.IMPORTANCE_DEFAULT))
        manager.createNotificationChannel(NotificationChannel("insights", "Goals and summaries", NotificationManager.IMPORTANCE_LOW))
        store = LedgerStore(AndroidSqliteDriver(LedgerDatabase.Schema, this, "cashflow.db"), this)
        store.nudgeSink = NudgeSink(::showNudge)
        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            "insights", ExistingPeriodicWorkPolicy.KEEP,
            PeriodicWorkRequestBuilder<InsightsWorker>(24, TimeUnit.HOURS).build(),
        )
        subscriptions = SubscriptionService(this).also { it.start() }
    }
    override fun nowMillis(): Long = System.currentTimeMillis()
    override fun refreshSubscription() = subscriptions.refresh()
    override fun purchase(offerId: String) = subscriptions.buy(activity, offerId)
    override fun manageSubscription() = subscriptions.manage()
    override fun analyseAlert(item: ReviewItem, automatic: Boolean) = subscriptions.analyse(item, automatic)
    override fun now(): String = OffsetDateTime.now().toString()
    override fun id(): String = UUID.randomUUID().toString()
    override fun captureState(selected: Set<String>): CaptureState {
        val intent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val apps = packageManager.queryIntentActivities(intent, 0)
            .filter { it.activityInfo.packageName != packageName }
            .distinctBy { it.activityInfo.packageName }
            .map { SourceApp(it.activityInfo.packageName, it.loadLabel(packageManager).toString(), it.activityInfo.packageName in selected) }
            .sortedBy { it.name.lowercase() }
        val enabled = Settings.Secure.getString(contentResolver, "enabled_notification_listeners")
            ?.split(':')?.mapNotNull(ComponentName::unflattenFromString)
            ?.any { it.packageName == packageName } == true
        // ADB's notification command is a safe synthetic source for debug-build capture checks.
        val debugSources = if (applicationInfo.flags and android.content.pm.ApplicationInfo.FLAG_DEBUGGABLE != 0) {
            listOf(SourceApp("com.android.shell", "Android test notifications", "com.android.shell" in selected))
        } else emptyList()
        val available = (apps + debugSources).map { it.id }.toSet()
        val unavailable = (selected - available).sorted().map { SourceApp(it, "$it (unavailable)", true) }
        return CaptureState("Android", accessGranted = enabled, sources = apps + debugSources + unavailable)
    }
    override fun openCaptureSettings() {
        startActivity(Intent(Settings.ACTION_NOTIFICATION_LISTENER_SETTINGS).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
    }
    override fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= 33) activity?.requestPermissions(arrayOf(Manifest.permission.POST_NOTIFICATIONS), 101)
    }
    override fun exportCsv(contents: String): String? {
        val directory = File(cacheDir, "exports").also { it.mkdirs() }
        val file = File(directory, "cashflow.csv").also { it.writeText(contents) }
        val uri = FileProvider.getUriForFile(this, "$packageName.files", file)
        val send = Intent(Intent.ACTION_SEND).setType("text/csv").putExtra(Intent.EXTRA_STREAM, uri)
            .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        startActivity(Intent.createChooser(send, "Export recorded activity").addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
        return null
    }
    private fun showNudge(nudge: Nudge) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) {
            throw IllegalStateException("Notifications are not allowed") // Not counted as sent; retried later.
        }
        val open = PendingIntent.getActivity(this, nudge.id.hashCode(), Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "insights")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle(nudge.title).setContentText(nudge.text)
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open).setAutoCancel(true).build()
        getSystemService(NotificationManager::class.java).notify(nudge.id.hashCode(), notification)
    }
    override fun recorded(entry: Entry) {
        if (Build.VERSION.SDK_INT >= 33 && checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED) return
        val openIntent = Intent(this, MainActivity::class.java).putExtra("entryId", entry.id)
        val open = PendingIntent.getActivity(this, entry.id.hashCode(), openIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val undoIntent = Intent(this, UndoReceiver::class.java).putExtra("entryId", entry.id).putExtra("revision", entry.revision)
        val undo = PendingIntent.getBroadcast(this, entry.id.hashCode(), undoIntent, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        val notification = NotificationCompat.Builder(this, "recorded")
            .setSmallIcon(android.R.drawable.stat_notify_more)
            .setContentTitle("Transaction recorded")
            .setContentText(store.creditDetail(entry) ?: "Open Cashflow to see the entry.")
            .setVisibility(NotificationCompat.VISIBILITY_PRIVATE)
            .setContentIntent(open).setAutoCancel(true)
            .addAction(android.R.drawable.ic_menu_revert, "Undo", undo)
            .addAction(android.R.drawable.ic_menu_edit, "Edit", open).build()
        getSystemService(NotificationManager::class.java).notify(entry.id.hashCode(), notification)
    }
}

class UndoReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val store = (context.applicationContext as CashflowApplication).store
        val id = intent.getStringExtra("entryId") ?: return
        val entry = store.state.value.entries.firstOrNull { it.id == id } ?: return
        if (entry.revision != intent.getLongExtra("revision", -1)) {
            context.startActivity(Intent(context, MainActivity::class.java).putExtra("entryId", id).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
            return
        }
        if (store.delete(id) == null) context.getSystemService(NotificationManager::class.java).cancel(id.hashCode())
    }
}
