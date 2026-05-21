package com.egidanuajisantoso.pengingatsholat.data.repository

import com.egidanuajisantoso.pengingatsholat.data.local.dao.CityDao
import com.egidanuajisantoso.pengingatsholat.data.local.entity.CityEntity
import com.egidanuajisantoso.pengingatsholat.data.repository.ApiClient.cityApi

class CityRepository(
    private val dao: CityDao
) {

    suspend fun fetchAndSaveCities() {
        val response = cityApi.getAllCities()

        val entities = response.data.map { city ->
            CityEntity(
                id = city.id,
                name = city.lokasi
            )
        }

        dao.insertCities(entities)
    }
}