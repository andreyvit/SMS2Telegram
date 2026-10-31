package com.tigerworkshop.sms2telegram.sms

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Telephony
import android.telephony.SubscriptionManager
import android.util.Log
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat.getSystemService
import com.tigerworkshop.sms2telegram.data.PendingMessageOutbox
import com.tigerworkshop.sms2telegram.data.SettingsRepository
import com.tigerworkshop.sms2telegram.data.StatusUpdateBus
import com.tigerworkshop.sms2telegram.data.TelegramDeliveryWorker
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale


class SmsReceiver : BroadcastReceiver() {
    private fun getSimCarrierName(context: Context, repository: SettingsRepository, slotIndex: Int): String? {
        // Check if user has enabled "Show SIM Name" feature
        if (!repository.isShowSimNameEnabled()) {
            return null
        }

        // Check if we have READ_PHONE_STATE permission
        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.READ_PHONE_STATE
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            return null
        }

        // Only available on Android 5.1 (API 22) and above
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP_MR1) {
            return null
        }

        try {
            val subscriptionManager = getSystemService(context, SubscriptionManager::class.java)
            if (subscriptionManager != null) {
                val subscriptionInfoList = subscriptionManager.activeSubscriptionInfoList
                if (subscriptionInfoList != null) {
                    for (subscriptionInfo in subscriptionInfoList) {
                        if (subscriptionInfo.simSlotIndex == slotIndex) {
                            val carrierName = listOfNotNull(subscriptionInfo.displayName?.toString(), subscriptionInfo.carrierName?.toString()).filter { it.isNotBlank() }.distinct().joinToString(" / ")
                            if (!carrierName.isNullOrBlank()) {
                                return carrierName
                            }
                        }
                    }
                }
            }
        } catch (e: Exception) {
            Log.e("SmsReceiver", "Error getting SIM carrier name", e)
        }

        return null
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Telephony.Sms.Intents.SMS_RECEIVED_ACTION) return

        val appContext = context.applicationContext
        val repository = SettingsRepository(appContext)
        val outbox = PendingMessageOutbox(appContext)
        val timeFormatter = SimpleDateFormat("yyyy-MM-dd HH:mm:ssZ", Locale.US)

        fun notifyStatusUpdated() {
            StatusUpdateBus.notifyUpdated()
        }

        if (!repository.isForwardingEnabled()) {
            repository.saveLastForwardStatus("${timeFormatter.format(Date())}: Forwarding disabled, SMS ignored")
            notifyStatusUpdated()
            return
        }

        if (repository.loadSettings() == null) {
            repository.saveLastForwardStatus("${timeFormatter.format(Date())}: Incomplete settings: Missing API token or Chat ID")
            notifyStatusUpdated()
            return
        }

        val messages = Telephony.Sms.Intents.getMessagesFromIntent(intent).orEmpty()
        if (messages.isEmpty()) {
            repository.saveLastForwardStatus("No SMS payload detected.")
            notifyStatusUpdated()
            return
        }

        val subscriptionId = listOf("android.telephony.extra.SUBSCRIPTION_INDEX", "subscription")
            .firstNotNullOfOrNull { key -> intent.getIntExtra(key, -1).takeIf { it >= 0 } }
        val subscriptions = try {
            getSystemService(appContext, SubscriptionManager::class.java)?.activeSubscriptionInfoList.orEmpty()
        } catch (_: SecurityException) { emptyList() }
        val slotFromSubscription = subscriptions.firstOrNull { it.subscriptionId == subscriptionId }?.simSlotIndex
        val simSlotIndex = slotFromSubscription ?: listOf("android.telephony.extra.SLOT_INDEX", "slot", "simSlot", "phone")
            .firstNotNullOfOrNull { key -> intent.getIntExtra(key, -1).takeIf { it >= 0 } } ?: -1
        val simLabel = if (simSlotIndex >= 0) "SIM ${simSlotIndex + 1}" else "SIM unknown"
        val simCarrierName = getSimCarrierName(appContext, repository, simSlotIndex)

        val sender = messages.firstOrNull()?.displayOriginatingAddress ?: "Unknown"
        val body = messages.joinToString(separator = "") { it.displayMessageBody ?: "" }
        val formattedMessage = buildString {
            appendLine("From: $sender")
            if (simCarrierName != null) {
                appendLine("$simLabel - $simCarrierName")
            } else {
                appendLine(simLabel)
            }
            appendLine("Time: ${timeFormatter.format(Date())}")
            appendLine()
            append(body)
        }

        messageChunks(formattedMessage).forEach { chunk ->
            outbox.enqueue(sender = sender, message = chunk)
        }
        val pendingCount = outbox.pendingCount()
        repository.saveLastForwardStatus(
            "${timeFormatter.format(Date())} - From $sender - Queued for delivery. $pendingCount pending message(s)."
        )
        notifyStatusUpdated()
        TelegramDeliveryWorker.enqueue(appContext)
    }
}
