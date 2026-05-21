package com.egidanuajisantoso.pengingatsholat.data.remote

import retrofit2.http.GET

interface CityApiService {

    @GET("v3/sholat/kabkota/semua")
    suspend fun getAllCities(): CityApiResponse
}