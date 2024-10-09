package com.example.m2_2.Data

import android.Manifest
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import com.example.m2_2.MainActivity
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
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

object CityCache {

    private const val PREFS_NAME = "city_preferences"
    private const val FAVORITES_KEY = "favorite_cities"


    private val gson = Gson()

    // Méthode pour sauvegarder la liste de favoris
    fun saveFavorites(context: Context, favorites: List<City>) {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val editor = sharedPreferences.edit()
        val json = gson.toJson(favorites)
        editor.putString(FAVORITES_KEY, json)
        editor.apply()
    }

    // Méthode pour charger la liste de favoris
    fun loadFavorites(context: Context): List<City> {
        val sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        val json = sharedPreferences.getString(FAVORITES_KEY, null)
        val type = object : TypeToken<List<City>>() {}.type
        return if (json != null) {
            gson.fromJson(json, type)
        } else {
            emptyList() // Retourner une liste vide si aucune donnée n'est trouvée
        }
    }
}

fun AddCityToFavorites(context: Context, city: City) {
    val favorites = CityCache.loadFavorites(context).toMutableList()
    if (!favorites.contains(city)) {
        favorites.add(city)
        CityCache.saveFavorites(context, favorites)
    }
    val intent = Intent(context, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

fun RemoveCityFromFavorites(context: Context, city: City) {
    val favorites = CityCache.loadFavorites(context).toMutableList()
    if (favorites.contains(city)) {
        favorites.remove(city)
        CityCache.saveFavorites(context, favorites)
    }

    val intent = Intent(context, MainActivity::class.java)
    intent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_NEW_TASK)
    context.startActivity(intent)
}

// Fonction pour obtenir la ville actuelle
// Fonction pour obtenir la ville actuelle
suspend fun getCurrentCity(context: Context): City? {
    val fusedLocationClient: FusedLocationProviderClient =
        LocationServices.getFusedLocationProviderClient(context)

    return try {
        // Vérifiez les permissions
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_FINE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED &&
                ActivityCompat.checkSelfPermission(
                    context,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                // Permissions non accordées, vous devez les demander
                return null // Ou gérer les permissions selon vos besoins
            }
        }

        // Obtenir la dernière position connue
        val location: Location? = fusedLocationClient.lastLocation.await()

        // Vérifiez si la localisation est disponible
        if (location != null) {
                City(
                    id = 0, // Remplacez par une logique pour générer un ID si nécessaire
                    name = "",
                    longitude = location.longitude,
                    latitude = location.latitude,
                    country ="",
                    admin1 =  ""
                )
            ////

        } else {
            null // Aucune localisation disponible
        }
    } catch (e: Exception) {
        // Gérer les exceptions (par exemple, permissions non accordées)
        e.printStackTrace()
        null
    }
}
/*
object CityMock {
    var CityList = ArrayList<City>().apply {
        add(City(id = 3023506, name = "Corte", 9.14917, 42.30956))
        add(City(id = 2989554, name = "Omessa", 9.19921, 42.37028))
        add(City(id = 3034640, name = "Bastia", 9.45, 42.70278))
    }
}
 */



