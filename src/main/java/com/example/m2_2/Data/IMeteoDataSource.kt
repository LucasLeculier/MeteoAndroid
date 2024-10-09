package com.example.m2_2.Data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

// Modèle de données pour les conditions météorologiques actuelles
data class CurrentWeather(
    val temperature: Double,
    val windspeed: Double,
    val time: String
)

// Modèle de données pour les prévisions journalières
data class DailyWeather(
    val temperature_2m_max: List<Double>,
    val temperature_2m_min: List<Double>,
    val weathercode: List<Int>
)

// Modèle de réponse de l'API
data class MeteoResponse(
    val current_weather: CurrentWeather,
    val daily: DailyWeather
)

// Interface pour l'API météo
interface MeteoApiService {
    @GET("v1/forecast")
    suspend fun getMeteo(
        @Query("latitude") latitude: Double,
        @Query("longitude") longitude: Double,
        @Query("current_weather") currentWeather: Boolean = true,
        @Query("daily") daily: String = "temperature_2m_max,temperature_2m_min,weathercode"
    ): MeteoResponse
}

// Objet pour créer une instance Retrofit
object RetrofitInstance {
    private const val BASE_URL = "https://api.open-meteo.com/"

    val api: MeteoApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(MeteoApiService::class.java)
    }
}

// Interface pour la source de données météo
interface IMeteoDataSource {
    suspend fun fetchMeteo(city: City): Meteo
}

// Classe pour la source de données distante
class MeteoRemoteDataSource : IMeteoDataSource {
    override suspend fun fetchMeteo(city: City): Meteo {
        return withContext(Dispatchers.IO) {
            // Appel de l'API via Retrofit
            val response = RetrofitInstance.api.getMeteo(city.latitude, city.longitude)

            // Transformez la réponse en un objet Meteo
            val currentWeather = response.current_weather
            val dailyMaxTemp = response.daily.temperature_2m_max[0]
            val dailyMinTemp = response.daily.temperature_2m_min[0]

            // Créez un objet Meteo à retourner
            Meteo(
                ville = city,
                TemperatureActuelle = currentWeather.temperature,
                TemperatureMin = dailyMinTemp,
                TemperatureMax = dailyMaxTemp,
                HeureActuelle = currentWeather.time,
                Humidite = 0.0, // Remplacez par la valeur correcte si disponible
                VitesseVent = currentWeather.windspeed,
                weatherCode = response.daily.weathercode[0]
            )
        }
    }
}