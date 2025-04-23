package com.craxiom.networksurvey.ui.gnss.model

/**
 * A container class to hold a NMEA [message] with a system [timestamp] (coming from
 * https://developer.android.com/reference/android/location/OnNmeaMessageListener)
 */
data class NmeaWithTime(val timestamp: Long, val message: String)