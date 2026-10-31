package com.tigerworkshop.sms2telegram.data

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.work.*
import java.util.concurrent.TimeUnit

class RecoveryReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) { recover(context) }
    companion object {
        fun recover(context: Context) {
            if (PendingMessageOutbox(context).pendingCount() > 0) TelegramDeliveryWorker.enqueue(context)
            WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                "outbox_recovery", ExistingPeriodicWorkPolicy.KEEP,
                PeriodicWorkRequestBuilder<OutboxRecoveryWorker>(15, TimeUnit.MINUTES).build()
            )
        }
    }
}

class OutboxRecoveryWorker(context: Context, params: WorkerParameters) : Worker(context, params) {
    override fun doWork(): Result {
        if (PendingMessageOutbox(applicationContext).pendingCount() > 0)
            TelegramDeliveryWorker.enqueue(applicationContext)
        return Result.success()
    }
}
