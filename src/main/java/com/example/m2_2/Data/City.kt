package com.example.m2_2.Data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.m2_2.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.Locale
import kotlinx.coroutines.tasks.await

data class City(
    val id: Int,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val country: String,
    val admin1: String,
    var lastTemperature: Double? = null,
    var lastWeatherCode: Int? = null
) {
    // Méthode pour mettre à jour les données météo
    fun setLastMeteoData(meteo: Meteo) {
        lastTemperature = meteo.TemperatureActuelle
        lastWeatherCode = meteo.weatherCode
    }
}

private val Context.dataStore by preferencesDataStore(name = "city_preferences")

object CityCache {

    private const val FAVORITES_KEY = "favorite_cities"
    private val gson = Gson()

    private val favoritesKey = stringPreferencesKey(FAVORITES_KEY)

    // Méthode pour sauvegarder la liste de favoris
    suspend fun saveFavorites(context: Context, favorites: List<City>) {
        val json = gson.toJson(favorites)
        context.dataStore.edit { preferences ->
            preferences[favoritesKey] = json
        }
    }

    // Méthode pour charger la liste de favoris
    fun loadFavorites(context: Context): Flow<List<City>> {
        return context.dataStore.data
            .map { preferences ->
                val json = preferences[favoritesKey]
                val type = object : TypeToken<List<City>>() {}.type

                json?.let {
                    gson.fromJson(it, type)
                } ?: emptyList()
            }
    }
}