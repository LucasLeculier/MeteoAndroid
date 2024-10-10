package com.example.m2_2.Data

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import android.net.ConnectivityManager
import android.util.Log
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.tasks.await
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import retrofit2.HttpException
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query
import java.io.IOException
import java.util.concurrent.TimeUnit

data class CityResponse(
    val results: List<CityApiModel>
)

data class CityApiModel(
    val id: Int,
    val name: String,
    val longitude: Double,
    val latitude: Double,
    val country: String,
    val admin1: String
)

// Interface pour la source de données des villes
interface ICityDataSource {
    suspend fun fetchCities(query: String,context: Context): List<City>
    suspend fun fetchPositionActuelle(context: Context): City
}

class CityRemoteDataSource : ICityDataSource {

    // Configuration de l'OkHttpClient avec des timeouts
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS) // Timeout de connexion
        .readTimeout(30, TimeUnit.SECONDS)    // Timeout de lecture
        .writeTimeout(15, TimeUnit.SECONDS)   // Timeout d'écriture
        .build()

    private val retrofit = Retrofit.Builder()
        .baseUrl("https://geocoding-api.open-meteo.com/")
        .client(client) // Ajout du client configuré
        .addConverterFactory(GsonConverterFactory.create())
        .build()

    private val api = retrofit.create(CityApiService::class.java)

    override suspend fun fetchCities(query: String, context: Context): List<City> {
        return withContext(Dispatchers.IO) {
            if (!isConnectedToInternet(context)) {
                // Retourner une liste vide si hors connexion
                Log.e("Network Error", "Pas de connexion Internet")
                return@withContext emptyList()
            }

            try {
                // Appel de l'API via Retrofit
                val response = api.getCities(query)
                Log.d("API Response", response.toString()) // Vérifiez la réponse

                // Transformez la réponse en une liste d'objets City
                response.results.take(6).map { cityApiModel ->
                    City(
                        id = cityApiModel.id,
                        name = cityApiModel.name,
                        longitude = cityApiModel.longitude,
                        latitude = cityApiModel.latitude,
                        country = cityApiModel.country,
                        admin1 = cityApiModel.admin1
                    )
                }
            } catch (e: HttpException) {
                // Gérer les erreurs HTTP (par exemple, 404, 500)
                Log.e("API Error", "Erreur lors de l'appel API: ${e.code()}: ${e.message()}")
                return@withContext emptyList() // Retourne une liste vide en cas d'erreur
            } catch (e: IOException) {
                // Gérer les erreurs de connexion (problèmes de réseau)
                Log.e("Network Error", "Erreur de connexion: ${e.message}")
                return@withContext emptyList() // Retourne une liste vide en cas d'erreur
            } catch (e: Exception) {
                // Gérer les autres exceptions
                Log.e("Unknown Error", "Erreur inconnue: ${e.message}")
                return@withContext emptyList() // Retourne une liste vide en cas d'erreur
            }
        }
    }

    override suspend fun fetchPositionActuelle(context: Context): City {
        val fusedLocationClient: FusedLocationProviderClient =
            LocationServices.getFusedLocationProviderClient(context)

        // Vérification des permissions
        if (ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED &&
            ContextCompat.checkSelfPermission(
                context,
                Manifest.permission.ACCESS_COARSE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            // Retourner une localisation par défaut si les permissions ne sont pas accordées
            return City(
                id = 0,
                name = "Permissions non accordées",
                longitude = 0.0,
                latitude = 0.0,
                country = "",
                admin1 = ""
            )
        }

        // Vérifier la connectivité avant d'essayer de récupérer la localisation
        if (!isConnectedToInternet(context)) {
            return City(
                id = 0,
                name = "Hors connexion",
                longitude = 0.0,
                latitude = 0.0,
                country = "",
                admin1 = ""
            )
        }

        // Récupérer la dernière position connue
        val location: Location? = fusedLocationClient.lastLocation.await()

        // Si location est null, retourne un City avec latitude et longitude à 0
        return location?.let {
            City(
                id = 0,
                name = "Ma position actuelle",
                longitude = it.longitude,
                latitude = it.latitude,
                country = "",
                admin1 = ""
            )
        } ?: City(
            id = 0,
            name = "Localisation inconnue",
            longitude = 0.0,
            latitude = 0.0,
            country = "",
            admin1 = ""
        )
    }

    // Fonction utilitaire pour vérifier la connectivité réseau
    private fun isConnectedToInternet(context: Context): Boolean {
        val connectivityManager =
            context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val activeNetwork = connectivityManager.activeNetworkInfo
        return activeNetwork != null && activeNetwork.isConnected
    }
}

// Interface pour l'API de gestion des villes
interface CityApiService {
    @GET("v1/search")
    suspend fun getCities(
        @Query("name") query: String
    ): CityResponse
}

object CityRetrofitInstance {
    private const val BASE_URL = "https://geocoding-api.open-meteo.com/"
    val api: CityApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CityApiService::class.java)
    }
}
