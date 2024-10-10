package com.example.m2_2.Data

import android.content.Context
import android.net.ConnectivityManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

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

class MeteoRemoteDataSource(private val context: Context) : IMeteoDataSource {

    // Configuration de l'OkHttpClient avec des timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) // Timeout de connexion
        .readTimeout(30, TimeUnit.SECONDS)    // Timeout de lecture
        .writeTimeout(15, TimeUnit.SECONDS)   // Timeout d'écriture
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://api.open-meteo.com/")
        .client(client) // Ajout du client configuré
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(MeteoApiService::class.java)

    override suspend fun fetchMeteo(city: City): Meteo {
        return withContext(Dispatchers.IO) {
            try {
                val response = api.getMeteo(city.latitude, city.longitude)

                // Transformez la réponse en un objet Meteo
                val currentWeather = response.current_weather

                val heureActuelle = conversionHeure(currentWeather.time)

                Meteo(
                    ville = city,
                    TemperatureActuelle = currentWeather.temperature,
                    HeureActuelle = heureActuelle,
                    Humidite = 0.0, // Remplacez par la valeur correcte si disponible
                    VitesseVent = currentWeather.windspeed,
                    weatherCode = response.daily.weathercode[0],
                    TemperatureMaxParJOur = response.daily.temperature_2m_max,
                    TemperatureMinParJOur = response.daily.temperature_2m_min
                )
            } catch (e: HttpException) {
                // Gérer les erreurs HTTP (par exemple, 404, 500)
                throw Exception("Erreur de réseau: ${e.code()}: ${e.message()}")
            } catch (e: IOException) {
                // Gérer les erreurs de connexion (problèmes de réseau)
                throw Exception("Erreur de connexion: ${e.message}")
            } catch (e: Exception) {
                // Gérer les autres exceptions
                throw Exception("Erreur inconnue: ${e.message}")
            }
        }
    }
}