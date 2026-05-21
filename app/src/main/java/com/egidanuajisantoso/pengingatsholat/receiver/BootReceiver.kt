package com.egidanuajisantoso.pengingatsholat.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.egidanuajisantoso.pengingatsholat.data.local.AppDatabase
import com.egidanuajisantoso.pengingatsholat.scheduler.PrayerAlarmScheduler
import com.egidanuajisantoso.pengingatsholat.util.TimeUtil
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.time.LocalDate

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        if (intent.action != Intent.ACTION_BOOT_COMPLETED) return

        CoroutineScope(Dispatchers.IO).launch {

            val dao = AppDatabase
                .getInstance(context)
                .prayerDao()

            val today = LocalDate.now().toString()

            val schedules = dao.getScheduleByDate(today)

            schedules.forEach { schedule ->

                val triggerMillis = TimeUtil.toMillis(
                    date = today,
                    time = schedule.time
                )

                // Jangan schedule alarm yang sudah lewat
                if (triggerMillis > System.currentTimeMillis()) {

                    PrayerAlarmScheduler.schedule(
                        context = context,
                        prayerName = schedule.prayerName,
                        triggerAtMillis = triggerMillis,
                        date = today
                    )
                }
            }
        }
    }
}
