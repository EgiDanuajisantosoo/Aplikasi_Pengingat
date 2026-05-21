package com.egidanuajisantoso.pengingatsholat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerLogEntity
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity
import java.time.LocalDate

@Dao
interface PrayerDao {

    @Query("""
        SELECT * FROM prayer_schedule
        WHERE date = :date
        ORDER BY time ASC
    """)
    suspend fun getScheduleByDate(date: String): List<PrayerScheduleEntity>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertSchedules(data: List<PrayerScheduleEntity>)

    @Query("DELETE FROM prayer_schedule WHERE date < :today")
    suspend fun deleteOldSchedules(today: String)


    // ===== Log =====

    @Query("""
        SELECT * FROM prayer_log
        WHERE date = :date AND prayerName = :prayerName
        LIMIT 1
    """)
    suspend fun getPrayerLog(date: String, prayerName: String): PrayerLogEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertPrayerLog(log: PrayerLogEntity)

    @Query("""
    SELECT COUNT(*) FROM prayer_log
    WHERE date = :date AND isDone = 1
""")
    suspend fun countDoneByDate(date: String): Int

    @Query("""
    SELECT date FROM prayer_log
    WHERE isDone = 1
    GROUP BY date
    HAVING COUNT(prayerName) = 5
    ORDER BY date DESC
""")
    suspend fun getPerfectDays(): List<String>


    object StreakCalculator {

        fun calculate(dates: List<String>): Int {

            if (dates.isEmpty()) return 0

            var streak = 0
            var current = LocalDate.now()

            dates.forEach { dateStr ->
                val date = LocalDate.parse(dateStr)

                if (date == current) {
                    streak++
                    current = current.minusDays(1)
                } else {
                    return streak
                }
            }

            return streak
        }
    }


}
