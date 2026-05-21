package com.egidanuajisantoso.pengingatsholat.ui.confirm
import android.R.attr.id
import android.os.Bundle
import android.widget.Button
import android.widget.TextView
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.egidanuajisantoso.pengingatsholat.R
import com.egidanuajisantoso.pengingatsholat.data.local.AppDatabase
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerLogEntity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class PrayerConfirmActivity : AppCompatActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_confirm_prayer)

        // Menggunakan properti 'intent' yang lebih aman
        val prayer = intent.getStringExtra("PRAYER_NAME") ?: "Sholat"
        val date = intent.getStringExtra("DATE") ?: ""

        findViewById<TextView>(R.id.tvTitle).text = "Sudah sholat $prayer?"

        findViewById<Button>(R.id.btnDone).setOnClickListener {
            // lifecycleScope lebih aman daripada CoroutineScope manual
            lifecycleScope.launch(Dispatchers.IO) {
                val db = AppDatabase.getInstance(this@PrayerConfirmActivity)

                val log = PrayerLogEntity(
                    date = date,
                    prayerName = prayer,
                    isDone = true,
                    confirmedAt = System.currentTimeMillis(), // Mengisi TODO
                    postponeCount = 0 // Mengisi TODO awal
                )

                db.prayerDao().insertPrayerLog(log)

                withContext(Dispatchers.Main) {
                    Toast.makeText(this@PrayerConfirmActivity, "Alhamdulillah!", Toast.LENGTH_SHORT).show()
                    finish()
                }
            }
        }
    }
}