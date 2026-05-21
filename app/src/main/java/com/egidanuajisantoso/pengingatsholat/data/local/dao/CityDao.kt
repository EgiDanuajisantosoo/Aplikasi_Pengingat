package com.egidanuajisantoso.pengingatsholat.data.local.dao

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.Query
import com.egidanuajisantoso.pengingatsholat.data.local.entity.CityEntity

@Dao
interface CityDao {

    @Query("SELECT * FROM city ORDER BY name ASC")
    suspend fun getAllCities(): List<CityEntity>

    @Query("SELECT * FROM city WHERE name = :name LIMIT 1")
    suspend fun findByName(name: String): CityEntity?

    @Query("SELECT * FROM city WHERE name = :name LIMIT 1")
    suspend fun findCityByName(name: String): CityEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertCities(data: List<CityEntity>)
}