package com.example.service

import android.content.ComponentName
import android.content.Intent
import android.os.Build
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log

class BankNotificationListenerService : NotificationListenerService() {

    override fun onListenerConnected() {
        super.onListenerConnected()
        BankNotificationManager.setServiceActive(true)
        Log.d("BankNotifService", "Notification Listener connected successfully")
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        BankNotificationManager.setServiceActive(false)
        Log.d("BankNotifService", "Notification Listener disconnected - requesting rebind")
        tryRebind()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        super.onStartCommand(intent, flags, startId)
        tryRebind()
        return START_STICKY
    }

    private fun tryRebind() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            try {
                requestRebind(ComponentName(this, BankNotificationListenerService::class.java))
            } catch (e: Exception) {
                Log.e("BankNotifService", "Failed to request rebind", e)
            }
        }
    }

    override fun onNotificationPosted(sbn: StatusBarNotification?) {
        super.onNotificationPosted(sbn)
        if (sbn == null) return

        val packageName = sbn.packageName ?: ""
        // Do NOT process notifications originating from our own app
        if (packageName == applicationContext.packageName) return

        val extras = sbn.notification?.extras ?: return

        val title = extras.getCharSequence("android.title")?.toString() ?: ""
        val text = extras.getCharSequence("android.text")?.toString() ?: ""

        if (title.isBlank() && text.isBlank()) return

        if (BankNotificationParser.isBankOrSmsNotification(packageName, title, text)) {
            val parsed = BankNotificationParser.parse(title = title, text = text, packageName = packageName)
            if (parsed != null) {
                Log.d("BankNotifService", "Captured bank transaction: $parsed")
                BankNotificationManager.postTransaction(parsed, applicationContext)
            }
        }
    }
}
