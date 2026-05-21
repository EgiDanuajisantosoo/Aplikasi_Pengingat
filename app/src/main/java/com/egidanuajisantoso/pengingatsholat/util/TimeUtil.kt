package com.egidanuajisantoso.pengingatsholat.util

import java.time.LocalDateTime
import java.time.ZoneId
import java.time.format.DateTimeFormatter

object TimeUtil {

    fun toMillis(date: String, time: String): Long {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm")
        val dateTime = LocalDateTime.parse("$date $time", formatter)
        return dateTime.atZone(ZoneId.systemDefault()).toInstant().toEpochMilli()
    }
}
