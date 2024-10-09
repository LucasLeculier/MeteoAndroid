package com.example.m2_2.ui

import android.content.Context
import android.content.Intent
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
import com.example.m2_2.MainActivity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


data class  CityState(
    var cities: List<City> = emptyList(),
    var isLoading: Boolean = false,
    var searchText: TextFieldValue = TextFieldValue(""),
    var searchResults: List<City> = emptyList()
)

class CityViewModel : ViewModel() {

    private val _CityState = MutableStateFlow(CityState())
    val CityState: StateFlow<CityState> = _CityState


    fun loadCache(context: Context){
        val favorites = CityCache.loadFavorites(context)
        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(cities=favorites))
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
        val favorites = CityCache.loadFavorites(context).toMutableList()
        if (!favorites.contains(city)) {
            favorites.add(city)
            CityCache.saveFavorites(context, favorites)
        }
        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(cities=favorites))
            _CityState.emit(CityState.value.copy(searchResults = emptyList()))
            _CityState.emit(CityState.value.copy(searchText = TextFieldValue("")))
        }
    }

    fun removeCityFromFavorites(context: Context, city: City) {
        val favorites = CityCache.loadFavorites(context).toMutableList()
        if (favorites.contains(city)) {
            favorites.remove(city)
            CityCache.saveFavorites(context, favorites)
        }

        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(cities=favorites))
        }
    }


    // Fonction pour effectuer la recherche
    fun performSearch(query: String) {
        viewModelScope.launch {
            _CityState.emit(CityState.value.copy(isLoading = true))
        }
        // Lancer la recherche dans un coroutine
        CoroutineScope(Dispatchers.IO).launch {
            try {
                // Appel à l'API pour obtenir les villes correspondantes
                val results = CityRemoteDataSource().fetchCities(query) // Remplacez par votre source de données

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
