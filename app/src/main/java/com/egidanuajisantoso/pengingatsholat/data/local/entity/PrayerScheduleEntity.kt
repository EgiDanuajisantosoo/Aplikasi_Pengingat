package com.egidanuajisantoso.pengingatsholat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_schedule",
    indices = [Index(value = ["date", "prayerName"], unique = true)]
)
data class PrayerScheduleEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val date: String,       // yyyy-MM-dd
    val prayerName: String, // subuh, dzuhur, ashar, maghrib, isya
    val time: String        // HH:mm
)
