package com.egidanuajisantoso.pengingatsholat.data.remote

data class PrayerApiResponse(
    val status: Boolean? = null,
    val message: String? = null,
    val data: JadwalData
)

data class JadwalData(
    val id: String,
    val kabko: String,
    val prov: String,
    val jadwal: Map<String, JadwalEntry>
)

data class JadwalEntry(
    val tanggal: String,
    val imsak: String,
    val subuh: String,
    val terbit: String,
    val dhuha: String,
    val dzuhur: String,
    val ashar: String,
    val maghrib: String,
    val isya: String
)

