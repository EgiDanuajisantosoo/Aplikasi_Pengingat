package com.egidanuajisantoso.pengingatsholat

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import android.os.Bundle
import android.util.Log
import android.widget.TextView
import androidx.appcompat.app.AppCompatActivity
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.egidanuajisantoso.pengingatsholat.data.local.AppDatabase
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity
import com.egidanuajisantoso.pengingatsholat.data.repository.PrayerRepository
import com.egidanuajisantoso.pengingatsholat.location.GeocoderUtil
import com.egidanuajisantoso.pengingatsholat.location.LocationHelper
import com.egidanuajisantoso.pengingatsholat.ui.adapter.PrayerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime
import java.time.format.DateTimeFormatter
import java.util.Locale

class MainActivity : AppCompatActivity() {

    private lateinit var prayerAdapter: PrayerAdapter
    private lateinit var rvPrayer: RecyclerView
    private lateinit var tvCityName: TextView
    private lateinit var tvGregorianDate: TextView
    private lateinit var tvHijriDate: TextView
    private lateinit var tvHeaderPrayerName: TextView
    private lateinit var tvHeaderTimeBadge: TextView
    private lateinit var tvHeaderCountdown: TextView
    private lateinit var tvHeaderCountdownSub: TextView
    private lateinit var tvCalendarLink: TextView
    private var latestSchedules: List<PrayerScheduleEntity> = emptyList()
    private var countdownJob: Job? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        setupPrayerList()
        setupDashboardDefaults()

        when {
            hasLocationPermission() -> setupTodaySchedule()
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU -> requestNotificationPermission()
            else -> requestLocationPermission()
        }
    }

    override fun onStart() {
        super.onStart()
        startRealtimeCountdownTicker()
    }

    override fun onStop() {
        super.onStop()
        countdownJob?.cancel()
        countdownJob = null
    }

    private fun setupDashboardDefaults() {
        tvCityName = findViewById(R.id.tvCityName)
        tvGregorianDate = findViewById(R.id.tvGregorianDate)
        tvHijriDate = findViewById(R.id.tvHijriDate)
        tvHeaderPrayerName = findViewById(R.id.tvHeaderPrayerName)
        tvHeaderTimeBadge = findViewById(R.id.tvHeaderTimeBadge)
        tvHeaderCountdown = findViewById(R.id.tvHeaderCountdown)
        tvHeaderCountdownSub = findViewById(R.id.tvHeaderCountdownSub)
        tvCalendarLink = findViewById(R.id.tvCalendarLink)

        tvCityName.text = getString(R.string.location_loading)
        tvGregorianDate.text = formatGregorianDate(LocalDate.now())
        tvHijriDate.text = getString(R.string.dashboard_subtitle)
        tvHeaderPrayerName.text = getString(R.string.next_prayer_loading)
        tvHeaderTimeBadge.text = "--:-- WIB"
        tvHeaderCountdown.text = "--:--:--"
        tvHeaderCountdownSub.text = getString(R.string.next_prayer_loading)
        tvCalendarLink.setOnClickListener {
            rvPrayer.smoothScrollToPosition(0)
        }
    }

    private fun setupPrayerList() {
        rvPrayer = findViewById(R.id.rvPrayer)
        rvPrayer.layoutManager = LinearLayoutManager(this)
        // Add spacing between items
        val spacingDp = 8
        val scale = resources.displayMetrics.density
        val spacingPx = (spacingDp * scale + 0.5f).toInt()
        rvPrayer.addItemDecoration(VerticalSpaceItemDecoration(spacingPx))
        // Allow items to have outer padding visible
        rvPrayer.clipToPadding = false
        rvPrayer.setPadding(0, spacingPx, 0, spacingPx)

        prayerAdapter = PrayerAdapter(
            emptyList(),
            AppDatabase.getInstance(this).prayerDao()
        )
        rvPrayer.adapter = prayerAdapter
    }

    private fun setupTodaySchedule() {
        val db = AppDatabase.getInstance(this)
        val prayerDao = db.prayerDao()
        val repo = PrayerRepository(prayerDao)
        val today = LocalDate.now().toString()
        val locationHelper = LocationHelper(this)

        locationHelper.getLastLocation { lastLocation ->
            val locationCallback: (Location?) -> Unit = { location ->
                lifecycleScope.launch(Dispatchers.IO) {
                    var finalCityLabel = getString(R.string.location_unknown)

                    try {
                        val detectedCityName = location?.let {
                            GeocoderUtil.getCityName(this@MainActivity, it)
                        }?.trim().orEmpty()

                        val schedules = if (detectedCityName.isNotBlank()) {
                            finalCityLabel = detectedCityName
                            try {
                                repo.fetchScheduleByCityName(detectedCityName, LocalDate.parse(today))
                            } catch (scheduleError: Exception) {
                                Log.e("MAIN", "Schedule search failed for city=$detectedCityName", scheduleError)
                                prayerDao.getScheduleByDate(today)
                            }
                        } else {
                            finalCityLabel = getString(R.string.fallback_city)
                            repo.fetchAndSaveDailySchedule("1301", today)
                            prayerDao.getScheduleByDate(today)
                        }

                        withContext(Dispatchers.Main) {
                            prayerAdapter.updateData(schedules)
                            updateDashboard(finalCityLabel, schedules)
                        }
                    } catch (e: Exception) {
                        Log.e("MAIN", "Setup failed", e)

                        val cachedSchedules = prayerDao.getScheduleByDate(today)

                        withContext(Dispatchers.Main) {
                            prayerAdapter.updateData(cachedSchedules)
                            updateDashboard(finalCityLabel, cachedSchedules)
                        }
                    }
                }
            }

            if (lastLocation != null) {
                locationCallback(lastLocation)
            } else {
                locationHelper.requestSingleUpdate { newLoc ->
                    locationCallback(newLoc)
                }
            }
        }
    }

    private fun updateDashboard(
        cityLabel: String,
        schedules: List<PrayerScheduleEntity>
    ) {
        latestSchedules = schedules
        tvCityName.text = cityLabel
        tvGregorianDate.text = formatGregorianDate(LocalDate.now())
        tvHijriDate.text = getString(R.string.dashboard_subtitle)

        updateHeaderRealtime()

        // Scroll to next upcoming prayer for better UX
        try {
            val now = LocalTime.now()
            val nextIndex = schedules.indexOfFirst { sch ->
                runCatching { LocalTime.parse(sch.time) }.getOrNull()?.isAfter(now) ?: false
            }
            if (nextIndex >= 0) {
                rvPrayer.post { rvPrayer.smoothScrollToPosition(nextIndex) }
            }
        } catch (ex: Exception) {
            // ignore
        }

    }

    private fun formatGregorianDate(date: LocalDate): String {
        val formatter = DateTimeFormatter.ofPattern("EEEE, d MMMM yyyy", Locale("id", "ID"))
        return date.format(formatter).uppercase(Locale("id", "ID"))
    }

    private fun startRealtimeCountdownTicker() {
        if (countdownJob?.isActive == true) return
        countdownJob = lifecycleScope.launch {
            while (isActive) {
                updateHeaderRealtime()
                delay(1000)
            }
        }
    }

    private fun updateHeaderRealtime() {
        val now = LocalTime.now()
        val next = latestSchedules.asSequence()
            .mapNotNull { s -> runCatching { LocalTime.parse(s.time) }.getOrNull()?.let { s to it } }
            .firstOrNull { (_, t) -> !t.isBefore(now) }

        if (next != null) {
            val (sch, t) = next
            tvHeaderPrayerName.text = sch.prayerName.replaceFirstChar { it.uppercase() }
            tvHeaderTimeBadge.text = "${sch.time} WIB"

            val dur = java.time.Duration.between(now, t)
            val hours = dur.toHours().coerceAtLeast(0)
            val minutes = dur.minusHours(hours).toMinutes().coerceAtLeast(0)
            val seconds = dur.minusHours(hours).minusMinutes(minutes).seconds.coerceAtLeast(0)
            tvHeaderCountdown.text = String.format("-%02d:%02d:%02d", hours, minutes, seconds)
            tvHeaderCountdownSub.text = "Menuju waktu adzan"
        } else {
            tvHeaderPrayerName.text = getString(R.string.next_prayer_finished)
            tvHeaderTimeBadge.text = "--:-- WIB"
            tvHeaderCountdown.text = "--:--:--"
            tvHeaderCountdownSub.text = "Tidak ada jadwal"
        }
    }

    // simple vertical spacing decoration
    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: android.graphics.Rect, view: android.view.View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            outRect.top = if (position == 0) verticalSpaceHeight else 0
            outRect.bottom = verticalSpaceHeight
        }
    }


    private fun hasLocationPermission(): Boolean {
        return ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED || ActivityCompat.checkSelfPermission(
            this,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }

    private fun hasNotificationPermission(): Boolean {
        return Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) == PackageManager.PERMISSION_GRANTED
    }

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU && !hasNotificationPermission()) {
            requestPermissions(
                arrayOf(Manifest.permission.POST_NOTIFICATIONS),
                100
            )
        } else {
            requestLocationPermission()
        }
    }

    private fun requestLocationPermission() {
        if (!hasLocationPermission()) {
            ActivityCompat.requestPermissions(
                this,
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ),
                101
            )
        } else {
            setupTodaySchedule()
        }
    }

    override fun onRequestPermissionsResult(
        requestCode: Int,
        permissions: Array<out String>,
        grantResults: IntArray
    ) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults)

        when (requestCode) {
            100 -> requestLocationPermission()
            101 -> setupTodaySchedule()
        }
    }
}
