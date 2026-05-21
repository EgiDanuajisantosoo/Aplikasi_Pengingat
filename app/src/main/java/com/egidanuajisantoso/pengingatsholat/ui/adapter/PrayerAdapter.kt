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
import java.time.LocalTime
import java.time.Duration

class PrayerAdapter(
    private var data: List<PrayerScheduleEntity>,
    private val dao: PrayerDao
) : RecyclerView.Adapter<PrayerAdapter.VH>() {
    private enum class TimeStatus {
        PAST,
        NOW,
        WAITING
    }

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
            val status = resolveStatus(i)

            // Card background follows 3-status model
            val backgroundResource = when (status) {
                TimeStatus.NOW -> R.drawable.bg_next_prayer
                TimeStatus.WAITING -> R.drawable.bg_upcoming_prayer
                TimeStatus.PAST -> R.drawable.bg_past_prayer
            }
            h.container.background = ContextCompat.getDrawable(h.container.context, backgroundResource)

            when (status) {
                TimeStatus.NOW -> {
                    h.status.text = "Sekarang"
                    h.status.setTextColor(ContextCompat.getColor(h.status.context, R.color.chip_now_border))
                    h.status.background = ContextCompat.getDrawable(h.status.context, R.drawable.chip_now)
                }
                TimeStatus.WAITING -> {
                    h.status.text = "Menunggu"
                    h.status.setTextColor(ContextCompat.getColor(h.status.context, android.R.color.darker_gray))
                    h.status.background = ContextCompat.getDrawable(h.status.context, R.drawable.chip_wait)
                }
                TimeStatus.PAST -> {
                    h.status.text = "Telah terlewat"
                    h.status.setTextColor(ContextCompat.getColor(h.status.context, android.R.color.darker_gray))
                    h.status.background = ContextCompat.getDrawable(h.status.context, R.drawable.chip_wait)
                }
            }

            val subTextView = h.itemView.findViewById<TextView>(R.id.tvPrayerSub)
            when (status) {
                TimeStatus.NOW -> subTextView.text = "Sedang berlangsung"
                TimeStatus.PAST -> subTextView.text = "Telah terlewat"
                TimeStatus.WAITING -> {
                    val now = LocalTime.now()
                    val target = runCatching { LocalTime.parse(item.time) }.getOrNull()
                    if (target != null && target.isAfter(now)) {
                        val dur = Duration.between(now, target)
                        val hours = dur.toHours()
                        val minutes = dur.minusHours(hours).toMinutes()
                        val parts = mutableListOf<String>()
                        if (hours > 0) parts.add("${hours} jam")
                        if (minutes > 0) parts.add("${minutes} menit")
                        subTextView.text = if (parts.isNotEmpty()) "Dalam ${parts.joinToString(" ")}" else "Dalam beberapa menit"
                    } else {
                        subTextView.text = "Menunggu"
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
    
    // Determine status by prayer window: the latest prayer time <= now is "NOW".
    private fun resolveStatus(position: Int): TimeStatus {
        val now = LocalTime.now()
        val currentIndex = data.indexOfLast { schedule ->
            runCatching { LocalTime.parse(schedule.time) }.getOrNull()?.let { !it.isAfter(now) } ?: false
        }

        return when {
            currentIndex == -1 -> TimeStatus.WAITING
            position < currentIndex -> TimeStatus.PAST
            position == currentIndex -> TimeStatus.NOW
            else -> TimeStatus.WAITING
        }
    }
}
