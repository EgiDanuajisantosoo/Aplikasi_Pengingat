package com.egidanuajisantoso.pengingatsholat.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.LinearLayout
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
        val container: LinearLayout = v.findViewById(R.id.itemPrayerContainer)
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
        val item = data[i]
        h.name.text = item.prayerName.replaceFirstChar { it.uppercase() }
        h.time.text = item.time

        // Check if prayer time is upcoming or past
        val isUpcoming = isPrayerUpcoming(item.time)
        
        // Set background based on whether prayer is upcoming or past
        val backgroundResource = if (isUpcoming) {
            R.drawable.bg_upcoming_prayer
        } else {
            R.drawable.bg_past_prayer
        }
        h.container.background = ContextCompat.getDrawable(h.container.context, backgroundResource)

        CoroutineScope(Dispatchers.IO).launch {
            val log = dao.getPrayerLog(item.date, item.prayerName)
            withContext(Dispatchers.Main) {
                h.status.text = if (log?.isDone == true) "✔" else "⏳"
            }
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
