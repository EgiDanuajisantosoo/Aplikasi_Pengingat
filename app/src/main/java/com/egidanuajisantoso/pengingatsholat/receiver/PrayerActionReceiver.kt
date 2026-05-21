package com.egidanuajisantoso.pengingatsholat.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationManagerCompat
import com.egidanuajisantoso.pengingatsholat.data.local.AppDatabase
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerLogEntity
import com.egidanuajisantoso.pengingatsholat.scheduler.PrayerAlarmScheduler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

class PrayerActionReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {

        val prayerName = intent.getStringExtra("PRAYER_NAME") ?: return
        val date = intent.getStringExtra("DATE") ?: return
        val action = intent.action ?: return

        val dao = AppDatabase
            .getInstance(context)
            .prayerDao()

        CoroutineScope(Dispatchers.IO).launch {

            when (action) {
                ACTION_DONE -> {
                    dao.insertPrayerLog(
                        PrayerLogEntity(
                            date = date,
                            prayerName = prayerName,
                            isDone = true,
                            confirmedAt = System.currentTimeMillis()
                        )
                    )
                }

                ACTION_LATER -> {

                    val existing = dao.getPrayerLog(date, prayerName)

                    val count = existing?.postponeCount ?: 0

                    if (count < 2) {

                        dao.insertPrayerLog(
                            PrayerLogEntity(
                                date = date,
                                prayerName = prayerName,
                                isDone = null,
                                confirmedAt = null,
                                postponeCount = count + 1
                            )
                        )

                        // reschedule +15 menit
                        val nextMillis = System.currentTimeMillis() + (15 * 60 * 1000)

                        PrayerAlarmScheduler.schedule(
                            context = context,
                            prayerName = prayerName,
                            triggerAtMillis = nextMillis,
                            date = date
                        )
                    }
                }

            }
        }

        NotificationManagerCompat.from(context)
            .cancel(prayerName.hashCode())
    }

    companion object {
        const val ACTION_DONE = "ACTION_PRAYER_DONE"
        const val ACTION_LATER = "ACTION_PRAYER_LATER"
    }
}
