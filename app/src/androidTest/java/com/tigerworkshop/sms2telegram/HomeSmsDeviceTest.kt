package com.tigerworkshop.sms2telegram

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.tigerworkshop.sms2telegram.data.*
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.json.JSONObject
import java.io.File

@RunWith(AndroidJUnit4::class)
class HomeSmsDeviceTest {
    private val context get() = InstrumentationRegistry.getInstrumentation().targetContext
    @Test fun migrateConfiguration() {
        val file = File(context.getExternalFilesDir(null), "migration.json")
        val settings = JSONObject(file.readText())
        context.getSharedPreferences("sms_forwarder_prefs", Context.MODE_PRIVATE).edit()
            .putString("api_token", settings.getString("api_token"))
            .putString("chat_id", settings.getString("chat_id"))
            .putBoolean("first_launch", false)
            .putBoolean("forwarding_enabled", true)
            .putBoolean("show_sim_name", true).commit().also { assertTrue(it) }
        assertTrue(file.delete())
        assertNotNull(SettingsRepository(context).loadSettings())
        RecoveryReceiver.recover(context)
    }
    @Test fun queueOffline() {
        val outbox = PendingMessageOutbox(context)
        assertEquals(0, outbox.pendingCount())
        outbox.enqueue("Home SMS test", "Home SMS offline-recovery test A. No real SMS content.")
        outbox.enqueue("Home SMS test", "Home SMS offline-recovery test B. No real SMS content.")
        TelegramDeliveryWorker.enqueue(context)
        Thread.sleep(3000)
        assertEquals(2, PendingMessageOutbox(context).pendingCount())
    }
    @Test fun verifyPersistedQueue() {
        assertEquals(2, PendingMessageOutbox(context).pendingCount())
    }
    @Test fun deliverRecoveredQueue() {
        RecoveryReceiver.recover(context)
        val deadline = System.currentTimeMillis() + 90000
        while (PendingMessageOutbox(context).pendingCount() > 0 && System.currentTimeMillis() < deadline)
            Thread.sleep(500)
        assertEquals(0, PendingMessageOutbox(context).pendingCount())
        assertTrue(SettingsRepository(context).loadLastForwardStatus().orEmpty().contains("Delivered"))
    }
}
