package dev.armaan.pd.widget

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import java.time.Duration

/** SPEC §6.2: redraws the widget every 30 minutes. Plumbing only; the widget reads its own sources. */
class RefreshWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        DashboardWidget.refresh(applicationContext)
        return Result.success()
    }

    companion object {
        const val NAME = "widget-refresh"

        /** Unique periodic work, no constraints, `KEEP`: at app start and when a widget is added. */
        fun enqueue(context: Context) {
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                NAME,
                ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<RefreshWorker>(Duration.ofMinutes(30)).build(),
            )
        }
    }
}
