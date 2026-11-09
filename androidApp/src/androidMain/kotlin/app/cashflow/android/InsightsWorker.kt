package app.cashflow.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Once a day: habits, summaries and reminders. All numbers come from the local ledger. */
class InsightsWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        (applicationContext as CashflowApplication).store.runDailyNudges()
        return Result.success()
    }
}
