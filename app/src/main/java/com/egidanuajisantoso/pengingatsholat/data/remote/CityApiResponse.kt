package com.egidanuajisantoso.pengingatsholat.data.remote


data class CityApiResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: List<CityDto>
)

data class CityDto(
    val id: String,
    val lokasi: String
)