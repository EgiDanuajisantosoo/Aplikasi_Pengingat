package com.egidanuajisantoso.pengingatsholat.data.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "prayer_log",
    indices = [Index(value = ["date", "prayerName"], unique = true)]
)
data class PrayerLogEntity(

    @PrimaryKey(autoGenerate = true)
    val id: Int = 0,

    val date: String,
    val prayerName: String,

    val isDone: Boolean?,     // true = sudah, false = tidak, null = pending
    val confirmedAt: Long?,

    val postponeCount: Int = 0
)
