package com.egidanuajisantoso.pengingatsholat.ui.adapter

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import com.egidanuajisantoso.pengingatsholat.R
import com.egidanuajisantoso.pengingatsholat.data.local.dao.PrayerDao
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrayerAdapter(
    private var data: List<PrayerScheduleEntity>,
    private val dao: PrayerDao
) : RecyclerView.Adapter<PrayerAdapter.VH>() {
    fun updateData(newData: List<PrayerScheduleEntity>) {
        data = newData
        notifyDataSetChanged()
    }
    inner class VH(v: View) : RecyclerView.ViewHolder(v) {
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

        CoroutineScope(Dispatchers.IO).launch {
            val log = dao.getPrayerLog(item.date, item.prayerName)
            withContext(Dispatchers.Main) {
                h.status.text = if (log?.isDone == true) "✔" else "⏳"
            }
        }
    }

    override fun getItemCount() = data.size
}
