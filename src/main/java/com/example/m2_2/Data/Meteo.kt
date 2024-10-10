package com.example.m2_2.Data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.m2_2.R
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.OffsetDateTime
import java.time.format.DateTimeFormatter
import java.time.ZoneId
import java.time.format.TextStyle
import java.util.Locale

data class Meteo(
    val ville : City,
    val TemperatureActuelle : Double,
    val HeureActuelle : String,
    val Humidite : Double,
    val VitesseVent : Double,
    val weatherCode : Int,
    val TemperatureMaxParJOur : List<Double>,
    val TemperatureMinParJOur : List<Double>
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

fun meteoAJour(city : City,meteos : List<Meteo>): Boolean {
    val heureActuelle = OffsetDateTime.now()

    // Formater l'heure actuelle au format "HH:mm"
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")

    // Calculer le dernier quart d'heure
    val dernierQuart = heureActuelle.minusMinutes(heureActuelle.minute % 15L)
    val dernierQuartFormate = dernierQuart.format(timeFormatter)

    val meteo = meteos.find { it.ville.id == city.id }
    return meteo?.HeureActuelle == dernierQuartFormate
}

fun prochaineSemaine(): List<String> {
    // Obtenir la date actuelle
    val aujourdHui = LocalDate.now()

    // Créer une liste des 7 jours à partir d'aujourd'hui
    val joursDeLaSemaine = mutableListOf<String>()

    // Boucler pour les 7 prochains jours
    for (i in 0 until 7) {
        val jour = aujourdHui.plusDays(i.toLong())
        val nomDuJour = jour.dayOfWeek.getDisplayName(TextStyle.FULL, Locale("fr"))
        joursDeLaSemaine.add(nomDuJour)
    }

    return joursDeLaSemaine
}


fun conversionHeure(dateTime: String): String {
    // Parse la chaîne avec un format spécifique
    val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm")

    // Convertir en LocalDateTime (qui n'a pas d'info de fuseau horaire)
    val localDateTime = LocalDateTime.parse(dateTime, formatter)

    // Obtenir l'heure locale dans le fuseau horaire de l'utilisateur
    val zonedDateTime = localDateTime.atZone(ZoneId.of("UTC"))
        .withZoneSameInstant(ZoneId.systemDefault())

    // Extraire uniquement l'heure et les minutes
    val timeFormatter = DateTimeFormatter.ofPattern("HH:mm")
    return timeFormatter.format(zonedDateTime)
}



private val Context.dataStore by preferencesDataStore(name = "meteo_preferences")

object MeteoCache {

    private const val METEO_KEY = "weather_data"
    private val gson = Gson()
    private val meteoKey = stringPreferencesKey(METEO_KEY)

    // Méthode pour sauvegarder la liste de données météo
    suspend fun saveWeatherData(context: Context, meteoList: List<Meteo>) {
        val json = gson.toJson(meteoList)
        context.dataStore.edit { preferences ->
            preferences[meteoKey] = json
        }
    }

    // Méthode pour charger la liste de données météo
    fun loadWeatherData(context: Context): Flow<List<Meteo>> {
        return context.dataStore.data
            .map { preferences ->
                val json = preferences[meteoKey]
                val type = object : TypeToken<List<Meteo>>() {}.type

                json?.let {
                    gson.fromJson(it, type)
                } ?: emptyList()
            }
    }
}