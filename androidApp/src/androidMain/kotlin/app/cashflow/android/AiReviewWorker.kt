package app.cashflow.android

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters

/** Only the local review ID is persisted in WorkManager. Consent is rechecked before each send. */
class AiReviewWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        val id = inputData.getString("reviewId") ?: return Result.failure()
        val app = applicationContext as CashflowApplication
        val retry = app.subscriptions.runAnalysis(id, inputData.getBoolean("automatic", false), runAttemptCount)
        return if (retry) Result.retry() else Result.success()
    }
}
