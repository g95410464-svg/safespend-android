package com.safespend.app.service

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.core.content.ContextCompat
import com.safespend.app.MainActivity
import com.safespend.app.util.toMoney

class NotificationHelper(private val context: Context) {

    init { createChannel() }

    private fun createChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID, "Alertas de límite de gasto", NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Te avisa cuando el saldo del banco cae bajo tu límite de seguridad"
                enableVibration(true)
            }
            context.getSystemService(NotificationManager::class.java).createNotificationChannel(channel)
        }
    }

    fun showLowBalance(bankCents: Long, pendingVaultCents: Long, thresholdCents: Long) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(context, Manifest.permission.POST_NOTIFICATIONS)
            != PackageManager.PERMISSION_GRANTED
        ) return

        val body = if (pendingVaultCents > 0) {
            "Te quedan ${bankCents.toMoney()} en el banco y aún debes " +
                "${pendingVaultCents.toMoney()} en suscripciones"
        } else {
            "Te quedan ${bankCents.toMoney()} en el banco (tu límite es ${thresholdCents.toMoney()}). " +
                "Suelta la tarjeta."
        }

        val tap = PendingIntent.getActivity(
            context, 0, Intent(context, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(android.R.drawable.ic_dialog_alert)
            .setContentTitle("¡No seas pendejo! nos quedamos pobres subnormal 💀")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_ALARM)
            .setAutoCancel(true)
            .setContentIntent(tap)
            .build()

        NotificationManagerCompat.from(context).notify(NOTIFICATION_ID, notification)
    }

    private companion object {
        const val CHANNEL_ID = "safety_limit"
        const val NOTIFICATION_ID = 1001
    }
}
