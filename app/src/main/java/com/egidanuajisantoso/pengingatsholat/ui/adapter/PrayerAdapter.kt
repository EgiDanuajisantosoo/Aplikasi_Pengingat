package com.egidanuajisantoso.pengingatsholat.ui.adapter

import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.View
import android.widget.TextView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.egidanuajisantoso.pengingatsholat.R
import com.egidanuajisantoso.pengingatsholat.data.local.dao.PrayerDao
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalTime

class PrayerAdapter(
    private var data: List<PrayerScheduleEntity>,
    private val dao: PrayerDao
) : RecyclerView.Adapter<PrayerAdapter.VH>() {
    fun updateData(newData: List<PrayerScheduleEntity>) {
        data = newData
        notifyDataSetChanged()
    }
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
        // use a generic View for safety to avoid ClassCastException on some devices
        val container: View = v.findViewById(R.id.itemPrayerContainer)
        val name: TextView = v.findViewById(R.id.tvPrayerName)
        val time: TextView = v.findViewById(R.id.tvPrayerTime)
        val status: TextView = v.findViewById(R.id.tvStatus)
    }

    override fun onCreateViewHolder(p: ViewGroup, v: Int): VH {
        return VH(
            LayoutInflater.from(p.context)
                .inflate(R.layout.item_prayer, p, false)
        )
    }

    override fun onBindViewHolder(h: VH, i: Int) {
        try {
            val item = data[i]
            h.name.text = item.prayerName.replaceFirstChar { it.uppercase() }
            h.time.text = item.time

            // Check if prayer time is upcoming or past
            val isUpcoming = isPrayerUpcoming(item.time)

            // Set background based on whether prayer is upcoming or past (safe fallback)
            val backgroundResource = if (isUpcoming) R.drawable.bg_upcoming_prayer else R.drawable.bg_past_prayer
            try {
                h.container.background = ContextCompat.getDrawable(h.container.context, backgroundResource)
            } catch (_: Exception) {
                // ignore background apply errors
            }

            // Update status and status color based on log and upcoming state
            CoroutineScope(Dispatchers.IO).launch {
                val log = try { dao.getPrayerLog(item.date, item.prayerName) } catch (ex: Exception) { null }
                withContext(Dispatchers.Main) {
                    try {
                        if (log?.isDone == true) {
                            h.status.text = "✔"
                            h.status.setTextColor(ContextCompat.getColor(h.status.context, R.color.teal_700))
                        } else {
                            h.status.text = if (isUpcoming) "⬆" else "⏳"
                            val color = if (isUpcoming) R.color.purple_500 else android.R.color.darker_gray
                            h.status.setTextColor(ContextCompat.getColor(h.status.context, color))
                        }
                    } catch (_: Exception) {
                        h.status.text = ""
                    }
                }
            }
        } catch (e: Exception) {
            // prevent adapter crashes; show safe fallback
            try {
                h.name.text = "-"
                h.time.text = "--:--"
                h.status.text = ""
            } catch (_: Exception) {}
        }
    }

    override fun getItemCount() = data.size
    
    /**
     * Check if a prayer time (HH:mm format) is upcoming compared to current time
     */
    private fun isPrayerUpcoming(prayerTime: String): Boolean {
        return try {
            val time = LocalTime.parse(prayerTime)
            val now = LocalTime.now()
            time.isAfter(now)
        } catch (e: Exception) {
            false
        }
    }
}
