package com.example.m2_2.Data

import android.graphics.drawable.Icon
import com.example.m2_2.R

data class Meteo(
    val ville : City,
    val TemperatureActuelle : Double,
    val TemperatureMin : Double,
    val TemperatureMax : Double,
    val HeureActuelle : String,
    val Humidite : Double,
    val VitesseVent : Double,
    val weatherCode : Int
)

fun getWeatherIcon(weatherCode: Int): Int {
    return when (weatherCode) {
        0 -> R.drawable.soleil
        1 -> R.drawable.partiellement_nuageux
        2 -> R.drawable.nuage
        3 -> R.drawable.nuage_pluie
        45 -> R.drawable.brouillard
        48 -> R.drawable.brouillard
        61 -> R.drawable.averse
        63 -> R.drawable.averse
        80 -> R.drawable.orage_pluie
        81 -> R.drawable.orage_pluie
        95 -> R.drawable.orage
        96 -> R.drawable.averse_grele
        99 -> R.drawable.averse_grele
        else -> R.drawable.partiellement_nuageux
    }
}






/*
current_temperature = data['current_weather']['temperature']
current_wind_speed = data['current_weather']['windspeed']
current_time = data['current_weather']['time']
daily_temperature_max = data['daily']['temperature_2m_max'][0]
daily_temperature_min = data['daily']['temperature_2m_min'][0]
weather_code = data['daily']['weathercode'][0]

 */