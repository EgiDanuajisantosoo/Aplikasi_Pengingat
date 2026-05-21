package com.egidanuajisantoso.pengingatsholat.data.repository

import com.egidanuajisantoso.pengingatsholat.data.local.dao.PrayerDao
import com.egidanuajisantoso.pengingatsholat.data.local.entity.PrayerScheduleEntity
import com.egidanuajisantoso.pengingatsholat.data.repository.ApiClient.api
import com.egidanuajisantoso.pengingatsholat.data.remote.JadwalEntry
import java.time.LocalDate

class PrayerRepository(
    private val dao: PrayerDao
) {

    suspend fun fetchAndSaveDailySchedule(
        cityId: String,
        date: String
    ) {
        val response = api.getDailySchedule(
            cityId,
            date
        )

        val entities = response.data.jadwal[date]?.toEntities(date)
            ?: response.data.jadwal.values.firstOrNull()?.toEntities(date)
            ?: emptyList()
        dao.insertSchedules(entities)
    }

    suspend fun fetchScheduleByCityName(
        cityName: String,
        date: LocalDate
    ): List<PrayerScheduleEntity> {
        var cityId: String? = null

        for (candidate in buildCitySearchCandidates(cityName)) {
            val response = api.searchCity(candidate)
            val matchedCityId = selectBestCityId(candidate, response.data)

            if (!matchedCityId.isNullOrBlank()) {
                cityId = matchedCityId
                break
            }
        }

        if (cityId.isNullOrBlank()) {
            throw IllegalStateException("Kota tidak ditemukan: $cityName")
        }

        // 2️⃣ Baru ambil jadwal
        val scheduleResponse = api.getDailySchedule(cityId, date.toString())

        val entities = scheduleResponse.data.jadwal[date.toString()]?.toEntities(date.toString())
            ?: scheduleResponse.data.jadwal.values.firstOrNull()?.toEntities(date.toString())
            ?: emptyList()
        dao.insertSchedules(entities)
        return entities
    }


}

private fun buildCitySearchCandidates(cityName: String): List<String> {
    val normalized = cityName.trim()
    val withoutKota = normalized.removePrefix("Kota ").removePrefix("kota ").trim()
    val withoutKabupaten = normalized
        .removePrefix("Kabupaten ")
        .removePrefix("kabupaten ")
        .removePrefix("Kab. ")
        .removePrefix("kab. ")
        .trim()

    return listOf(normalized, withoutKota, withoutKabupaten)
        .map { it.trim() }
        .filter { it.isNotBlank() }
        .distinct()
}

private fun selectBestCityId(candidate: String, cities: List<com.egidanuajisantoso.pengingatsholat.data.remote.CityDto>): String? {
    val exactCandidate = candidate.trim().uppercase()
    val normalizedCandidate = normalizeCityName(candidate)

    return cities.firstOrNull { it.lokasi.trim().uppercase() == exactCandidate }?.id
        ?: cities.firstOrNull { normalizeCityName(it.lokasi) == normalizedCandidate && it.lokasi.contains("KOTA", ignoreCase = true) }?.id
        ?: cities.firstOrNull { normalizeCityName(it.lokasi) == normalizedCandidate }?.id
        ?: cities.firstOrNull { normalizeCityName(it.lokasi).contains(normalizedCandidate) }?.id
        ?: cities.firstOrNull { it.lokasi.contains("KOTA", ignoreCase = true) }?.id
        ?: cities.firstOrNull()?.id
}

private fun normalizeCityName(value: String): String {
    return value
        .trim()
        .uppercase()
        .removePrefix("KOTA ")
        .removePrefix("KAB. ")
        .removePrefix("KABUPATEN ")
        .replace(Regex("[^A-Z0-9 ]"), " ")
        .replace(Regex("\\s+"), " ")
        .trim()
}

private fun JadwalEntry.toEntities(date: String): List<PrayerScheduleEntity> {
    return listOf(
        PrayerScheduleEntity(date = date, prayerName = "subuh", time = subuh),
        PrayerScheduleEntity(date = date, prayerName = "dzuhur", time = dzuhur),
        PrayerScheduleEntity(date = date, prayerName = "ashar", time = ashar),
        PrayerScheduleEntity(date = date, prayerName = "maghrib", time = maghrib),
        PrayerScheduleEntity(date = date, prayerName = "isya", time = isya)
    )
}

