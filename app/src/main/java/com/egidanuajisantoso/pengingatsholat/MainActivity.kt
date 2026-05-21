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
import com.egidanuajisantoso.pengingatsholat.data.local.dao.PrayerDao.StreakCalculator
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity
import com.egidanuajisantoso.pengingatsholat.data.repository.PrayerRepository
import com.egidanuajisantoso.pengingatsholat.location.GeocoderUtil
import com.egidanuajisantoso.pengingatsholat.location.LocationHelper
import com.egidanuajisantoso.pengingatsholat.ui.adapter.PrayerAdapter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.LocalTime

class MainActivity : AppCompatActivity() {

    private lateinit var prayerAdapter: PrayerAdapter
    private lateinit var rvPrayer: RecyclerView

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

    private fun setupDashboardDefaults() {
        findViewById<TextView>(R.id.tvLocationStatus).text = getString(R.string.location_loading)
        findViewById<TextView>(R.id.tvNextPrayer).text = getString(R.string.next_prayer_loading)
        findViewById<TextView>(R.id.tvSchedulerStatus).text = getString(R.string.scheduler_ready)
        findViewById<TextView>(R.id.tvStreak).text = getString(R.string.streak_empty)
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

                        val streak = StreakCalculator.calculate(prayerDao.getPerfectDays())

                        withContext(Dispatchers.Main) {
                            prayerAdapter.updateData(schedules)
                            updateDashboard(finalCityLabel, schedules, streak)
                        }
                    } catch (e: Exception) {
                        Log.e("MAIN", "Setup failed", e)

                        val cachedSchedules = prayerDao.getScheduleByDate(today)
                        val streak = StreakCalculator.calculate(prayerDao.getPerfectDays())

                        withContext(Dispatchers.Main) {
                            prayerAdapter.updateData(cachedSchedules)
                            updateDashboard(finalCityLabel, cachedSchedules, streak)
                            findViewById<TextView>(R.id.tvSchedulerStatus).text = getString(R.string.sync_failed)
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
        schedules: List<PrayerScheduleEntity>,
        streak: Int
    ) {
        findViewById<TextView>(R.id.tvLocationStatus).text =
            getString(R.string.location_status_format, cityLabel)
        findViewById<TextView>(R.id.tvNextPrayer).text = resolveNextPrayerText(schedules)
        findViewById<TextView>(R.id.tvSchedulerStatus).text = getString(R.string.scheduler_ready)
        findViewById<TextView>(R.id.tvStreak).text = getString(R.string.streak_format, streak)
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

    // simple vertical spacing decoration
    class VerticalSpaceItemDecoration(private val verticalSpaceHeight: Int) : RecyclerView.ItemDecoration() {
        override fun getItemOffsets(outRect: android.graphics.Rect, view: android.view.View, parent: RecyclerView, state: RecyclerView.State) {
            val position = parent.getChildAdapterPosition(view)
            if (position == RecyclerView.NO_POSITION) return
            outRect.top = if (position == 0) verticalSpaceHeight else 0
            outRect.bottom = verticalSpaceHeight
        }
    }

    private fun resolveNextPrayerText(schedules: List<PrayerScheduleEntity>): String {
        if (schedules.isEmpty()) return getString(R.string.next_prayer_loading)

        val now = LocalTime.now()
        val nextPrayer = schedules.asSequence()
            .mapNotNull { schedule ->
                runCatching { LocalTime.parse(schedule.time) }
                    .getOrNull()
                    ?.let { parsedTime -> schedule to parsedTime }
            }
            .firstOrNull { (_, parsedTime) -> !parsedTime.isBefore(now) }

        return if (nextPrayer != null) {
            val (schedule, _) = nextPrayer
            val prayerName = schedule.prayerName.replaceFirstChar { it.uppercase() }
            getString(R.string.next_prayer_value, prayerName, schedule.time)
        } else {
            getString(R.string.next_prayer_finished)
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
