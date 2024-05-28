package com.bscpe.omcmapp

import java.util.Date

data class Readings(
    val dateTime: Date,
    val date: String = "",
    val time: String = "",
    val internalTemperature: Float = 0.0F,
    val externalTemperature: Float = 0.0F,
    val internalHumidity: Float = 0.0F,
    val externalHumidity: Float = 0.0F
)