package com.egidanuajisantoso.pengingatsholat.notification

import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import com.egidanuajisantoso.pengingatsholat.R
import com.egidanuajisantoso.pengingatsholat.receiver.PrayerActionReceiver
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.app.ActivityCompat

object NotificationHelper {

    private const val CHANNEL_ID = "prayer_channel"

    fun showPrayerNotification(
        context: Context,
        prayerName: String,
        date: String
    ) {

        createChannel(context)

        // ===== ACTION: SUDAH =====
        val doneIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            action = PrayerActionReceiver.ACTION_DONE
            putExtra("PRAYER_NAME", prayerName)
            putExtra("DATE", date)
        }

        val donePendingIntent = PendingIntent.getBroadcast(
            context,
            (prayerName + "DONE").hashCode(),
            doneIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        // ===== ACTION: NANTI =====
        val laterIntent = Intent(context, PrayerActionReceiver::class.java).apply {
            action = PrayerActionReceiver.ACTION_LATER
            putExtra("PRAYER_NAME", prayerName)
            putExtra("DATE", date)
        }

        val laterPendingIntent = PendingIntent.getBroadcast(
            context,
            (prayerName + "LATER").hashCode(),
            laterIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_mosque)
            .setContentTitle("Waktu Sholat")
            .setContentText("Saatnya sholat $prayerName")
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setAutoCancel(true)
            .addAction(
                R.drawable.ic_check,
                "Sudah",
                donePendingIntent
            )
            .addAction(
                R.drawable.ic_later,
                "Nanti",
                laterPendingIntent
            )
            .build()

        if (ActivityCompat.checkSelfPermission(
                context,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Permission tidak ada → jangan crash, jangan tampilkan
            return
        }
        NotificationManagerCompat.from(context)
            .notify(prayerName.hashCode(), notification)

    }

    private fun createChannel(context: Context) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {

            val channel = NotificationChannel(
                CHANNEL_ID,
                "Pengingat Sholat",
                NotificationManager.IMPORTANCE_HIGH
            )

            val manager =
                context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            manager.createNotificationChannel(channel)
        }
    }

}

