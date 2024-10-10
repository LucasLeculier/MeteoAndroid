package com.example.m2_2.ui

import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.m2_2.Data.City
import com.example.m2_2.Data.CityCache
import com.example.m2_2.Data.CityRemoteDataSource
import com.example.m2_2.Data.Meteo
import com.example.m2_2.Data.MeteoCache
import com.example.m2_2.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class  CityState(
    var cities: List<City> = emptyList(),
    var meteos: List<Meteo> = emptyList(),
    var isLoading: Boolean = false,
    var searchText: TextFieldValue = TextFieldValue(""),
    var searchResults: List<City> = emptyList(),
    var horsConnexion: Boolean = false
)

class CityViewModel : ViewModel() {

    private val _CityState = MutableStateFlow(CityState())
    val CityState: StateFlow<CityState> = _CityState

    fun setHorsConnexion(isHorsConnexion: Boolean) {
        viewModelScope.launch {
            _CityState.emit(_CityState.value.copy(horsConnexion = isHorsConnexion))
        }
    }


    fun loadCache(context: Context) {
        viewModelScope.launch {

            CityCache.loadFavorites(context).collect { favorites ->
                // Émettre l'état avec les favoris chargés
                val city0: City? = favorites.find { it.id == 0 }


                // Si la ville avec id = 0 n'existe pas encore dans les favoris, on l'ajoute
                if (city0 == null) {
                    val city0Add = City(
                        id = 0,
                        name = "Ma position Actuelle",
                        longitude = 0.0, // Valeur par défaut
                        latitude = 0.0,  // Valeur par défaut
                        country = "",
                        admin1 = ""
                    )
                    addCityToFavorites(context, city0Add)
                }
                _CityState.emit(CityState.value.copy(cities = favorites))
            }

        }
        viewModelScope.launch {
            MeteoCache.loadWeatherData(context).collect { meteos ->
                _CityState.emit(CityState.value.copy(meteos = meteos))
            }
        }
    }



    fun clearList() {
        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(searchResults = emptyList()))
        }
    }
    fun setSearchText(newValue: TextFieldValue) {
        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(searchText = newValue))
        }
    }

    fun addCityToFavorites(context: Context, city: City) {
        var save = false
        val favorites = _CityState.value.cities
        val updatedFavorites = favorites.toMutableList()
        viewModelScope.launch {
            // Charger les favoris une seule fois



            //Ajouter la ville si elle n'existe pas déjà
            if (!updatedFavorites.contains(city)) {
                updatedFavorites.add(city)
                save = true
            }



                // Émettre l'état après l'ajout
                _CityState.emit(
                    CityState.value.copy(
                        searchText = TextFieldValue(""), // Réinitialiser le champ de recherche
                        searchResults = emptyList(),  // Réinitialiser les résultats de recherche
                        cities = updatedFavorites
                    )
                )
        }
        if(save){
            viewModelScope.launch {
                // Sauvegarder la liste mise à jour
                CityCache.saveFavorites(context, updatedFavorites)
            }}
    }

    fun addMeteoToMeteos(context: Context, meteo: Meteo?) {
        if (meteo == null) return
        viewModelScope.launch {
            val meteos = _CityState.value.meteos
            val updatesMeteo = meteos.toMutableList()
            val meteoVille = updatesMeteo.find { it.ville.id == meteo.ville.id }
            if (meteoVille != null) {
                val index = updatesMeteo.indexOf(meteoVille)
                updatesMeteo[index] = meteo // Mettre à jour la météo existante
            }else{
                updatesMeteo.add(meteo)
            }
            MeteoCache.saveWeatherData(context, updatesMeteo)
            //_CityState.emit(CityState.value.copy(meteos = updatesMeteo))



        }
    }


    fun removeCityFromFavorites(context: Context, city: City) {
        viewModelScope.launch {
            // Charger les favoris une seule fois
            val favorites = CityCache.loadFavorites(context).first()
            val updatedFavorites = favorites.toMutableList()

            // Supprimer la ville
            if (updatedFavorites.remove(city)) {
                // Sauvegarder la liste mise à jour
                CityCache.saveFavorites(context, updatedFavorites)

                // Émettre l'état après la suppression
                _CityState.emit(CityState.value.copy(cities = updatedFavorites))
            }
        }
    }



    // Fonction pour effectuer la recherche
    fun performSearch(query: String,context: Context) {

        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(isLoading = true))
        }
        // Lancer la recherche dans un coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Appel à l'API pour obtenir les villes correspondantes
                val results = CityRemoteDataSource().fetchCities(query, context) // Remplacez par votre source de données

                // Mettre à jour l'état sur le thread principal
                withContext(Dispatchers.Main) {
                    viewModelScope.launch {
                        _CityState.emit(CityState.value.copy(searchResults = results))
                    }
                }
            } catch (e: Exception) {
                withContext(Dispatchers.Main) {
                    _CityState.emit(CityState.value.copy(searchResults = emptyList()))
                }
            } finally {
                // Arrêter le chargement
                _CityState.emit(CityState.value.copy(isLoading = false))
            }
        }
    }
}
