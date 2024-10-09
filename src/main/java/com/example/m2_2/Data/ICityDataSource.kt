package com.example.m2_2.Data

import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.http.GET
import retrofit2.http.Query

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
    suspend fun fetchCities(query: String): List<City>
}

class CityRemoteDataSource : ICityDataSource {
    override suspend fun fetchCities(query: String): List<City> {
        return withContext(Dispatchers.IO) {
            try {
                // Appel de l'API via Retrofit
                val response = CityRetrofitInstance.api.getCities(query)
                Log.d("API Response", response.toString()) // Vérifiez la réponse

                // Transformez la réponse en une liste d'objets City
                response.results.take(6).map { cityApiModel -> // Utilisez "results" ici
                    City(
                        id = cityApiModel.id,
                        name = cityApiModel.name, // Remplacez "title" par "name"
                        longitude = cityApiModel.longitude,
                        latitude = cityApiModel.latitude,
                        country = cityApiModel.country,
                        admin1 = cityApiModel.admin1
                    )
                }
            } catch (e: Exception) {
                Log.e("API Error", "Erreur lors de l'appel API", e)
                emptyList() // Ou gérez l'erreur comme nécessaire
            }
        }
    }
}



// Interface pour l'API de gestion des villes
interface CityApiService {
    @GET("v1/search")
    suspend fun getCities(
        @Query("name") query: String
    ): CityResponse  // Remplacez CityResponse par votre classe de réponse appropriée
}

object CityRetrofitInstance {
    private const val BASE_URL = "https://geocoding-api.open-meteo.com/" // Changez ceci par votre URL de base
    val api: CityApiService by lazy {
        Retrofit.Builder()
            .baseUrl(BASE_URL)
            .addConverterFactory(GsonConverterFactory.create())
            .build()
            .create(CityApiService::class.java)
    }
}

