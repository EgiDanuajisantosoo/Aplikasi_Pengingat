package com.egidanuajisantoso.pengingatsholat.data.repository

import com.egidanuajisantoso.pengingatsholat.data.remote.CityApiService
import com.egidanuajisantoso.pengingatsholat.data.remote.PrayerApiService
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory

object ApiClient {

    private const val BASE_URL = "https://api.myquran.com/"

    val api: PrayerApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(PrayerApiService::class.java)
    }

    val cityApi: CityApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CityApiService::class.java)
    }
}
