package com.egidanuajisantoso.pengingatsholat.data.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.egidanuajisantoso.pengingatsholat.data.local.dao.CityDao
import com.egidanuajisantoso.pengingatsholat.data.local.dao.PrayerDao
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerLogEntity
import com.egidanuajisantoso.pengingatsholat.data.local.entity.CityEntity
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity

@Database(
    entities = [
        PrayerScheduleEntity::class,
        PrayerLogEntity::class,
        CityEntity::class // ⬅️ WAJIB ADA
    ],
    version = 4, // ⬅️ NAIKKAN VERSION
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {

    abstract fun prayerDao(): PrayerDao
    abstract fun cityDao(): CityDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "prayer_db"
                )
                    .fallbackToDestructiveMigration() // ⬅️ PENTING UNTUK DEV
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
