package com.egidanuajisantoso.pengingatsholat.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.egidanuajisantoso.pengingatsholat.notification.NotificationHelper

class PrayerAlarmReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: return
        val date = intent.getStringExtra("DATE") ?: return

        NotificationHelper.showPrayerNotification(
            context = context,
            prayerName = prayerName,
            date = date
        )
    }
}
