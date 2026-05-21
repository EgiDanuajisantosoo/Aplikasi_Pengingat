package com.egidanuajisantoso.pengingatsholat.location

import android.content.Context
import android.location.Geocoder
import android.location.Location
import java.util.Locale

object GeocoderUtil {

    fun getCityName(context: Context, location: Location): String? {
        val geocoder = Geocoder(context, Locale("id", "ID"))
        val addresses = geocoder.getFromLocation(
            location.latitude,
            location.longitude,
            1
        )
        return addresses?.firstOrNull()?.subAdminArea
            ?: addresses?.firstOrNull()?.locality
    }
}
