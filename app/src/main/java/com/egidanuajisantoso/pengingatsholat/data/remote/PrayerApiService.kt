package com.egidanuajisantoso.pengingatsholat.data.remote

import retrofit2.http.GET
import retrofit2.http.Path

interface PrayerApiService {

    @GET("v3/sholat/kabkota/cari/{keyword}")
    suspend fun searchCity(
        @Path("keyword") cityName: String
    ): CityApiResponse

    @GET("v3/sholat/jadwal/{cityId}/{period}")
    suspend fun getDailySchedule(
        @Path("cityId") cityId: String,
        @Path("period") period: String
    ): PrayerApiResponse
}
