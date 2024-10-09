package com.example.m2_2

import android.os.Bundle
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.sharp.Info
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Divider
import androidx.compose.material3.DividerDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.m2_2.Data.AddCityToFavorites
import com.example.m2_2.Data.City
import com.example.m2_2.Data.CityCache
import com.example.m2_2.Data.CityRemoteDataSource
import com.example.m2_2.Data.ICityDataSource
import com.example.m2_2.Data.IMeteoDataSource
import com.example.m2_2.Data.Meteo
import com.example.m2_2.Data.MeteoRemoteDataSource
import com.example.m2_2.Data.RemoveCityFromFavorites
import com.example.m2_2.Data.getWeatherIcon
import com.example.m2_2.ui.CityState
import com.example.m2_2.ui.CityViewModel
import com.example.m2_2.ui.theme.M22Theme
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Objects

class MainActivity : ComponentActivity() {
    //private var favoriteCities by mutableStateOf<List<City>>(emptyList())
    private val cityViewModel: CityViewModel by viewModels()


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cityViewModel.loadCache(this)
        setContent {
            val cityState by cityViewModel.CityState.collectAsStateWithLifecycle()
            M22Theme {
                val navController = rememberNavController()
                NavHost(navController = navController, startDestination = "home") {
                    composable("home") { HomeScreen(navController, cityState) }
                    composable(
                        route = "second_screen/{cityId}",
                        arguments = listOf(navArgument("cityId") { type = NavType.IntType }) // Changez le type à IntType
                    ) { backStackEntry ->
                        val cityId = backStackEntry.arguments?.getInt("cityId") ?: -1 // Utilisez getInt pour récupérer l'ID
                        SecondScreen(navController = navController,cityState, cityId = cityId) // Passez cityId à SecondScreen
                    }
                }

            }
        }
    }


    @Composable
    fun HomeScreen(navController: NavController, cityState : CityState) {
        Column {
            TopBarWithSearch(cityState.searchText) { query ->
                var searchText = query
                if (query.length >= 1) { // Lancer la recherche après 3 caractères
                    cityViewModel.performSearch(query)
                } else {
                    cityViewModel.clearList()
                }
            }

            if (cityState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                if (cityState.searchResults.isNotEmpty()) {
                    ResultatsRecherche(cityState.searchResults)
                } else {
                    // Afficher la liste des favoris si pas de résultats
                    ListingFavoris(navController,cityState.cities,cityViewModel)
                }
            }
            PositionActuelle()
        }

    }

    @Composable
    fun PositionActuelle() {
        Text(
            text = "Position Actuelle",
            style = MaterialTheme.typography.bodyLarge
        )
        }


    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBarWithSearch(
        searchtext: TextFieldValue,
        modifier: Modifier = Modifier,
        onSearch: (String) -> Unit

    ) {
        TopAppBar(
            title = {
                SearchBar(
                    onSearch = onSearch,
                    modifier = Modifier.fillMaxWidth(),
                    searchText = searchtext
                )
            },
            colors = TopAppBarDefaults.smallTopAppBarColors(
                containerColor = MaterialTheme.colorScheme.primary,
                titleContentColor = MaterialTheme.colorScheme.onPrimary
            ),
            modifier = modifier
        )
    }


    @Composable
    fun ResultatsRecherche(searchResults : List<City>) {
        LazyColumn {
            items(searchResults) { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                cityViewModel.addCityToFavorites(this@MainActivity, city)
                            }
                        )
                        .padding(8.dp)
                ) {
                    Text(
                        text = city.name,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = city.admin1,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = city.country,
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Add Icon"
                    )
                }

                HorizontalDivider()
            }
        }
    }


    @Composable
    fun SearchBar(
        modifier: Modifier = Modifier,
        onSearch: (String) -> Unit,
        searchText: TextFieldValue
    ) {

        Row(
            modifier = modifier
                .background(MaterialTheme.colorScheme.primary)
                .fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            BasicTextField(
                value = searchText,
                onValueChange = { newValue ->
                    cityViewModel.setSearchText(newValue)
                    onSearch(newValue.text) // Appeler le callback pour gérer la recherche
                },
                modifier = Modifier
                    .weight(1f)
                    .padding(8.dp),
                decorationBox = { innerTextField ->
                    if (searchText.text.isEmpty()) {
                        Text(
                            text = "Ajouter une ville à mes favoris ${searchText.text}",
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color.Gray,
                            textAlign = TextAlign.Start
                        )
                    }
                    innerTextField()
                }
            )
            Icon(
                imageVector = Icons.Filled.Search,
                contentDescription = "Search Icon"
            )
        }
    }
}


    @Composable
    fun VilleFavoris(
        navController: NavController,
        city: City,
        onCityClick: () -> Unit, // Callback pour la navigation vers la page de la ville
        onDeleteClick: () -> Unit, // Callback pour supprimer la ligne
        meteoDataSource: IMeteoDataSource = MeteoRemoteDataSource()
    ) {
        val cityId = city.id
        var meteo by remember { mutableStateOf<Meteo?>(null) }
        val coroutineScope = rememberCoroutineScope()

        LaunchedEffect(city) {
            coroutineScope.launch {
                meteo = meteoDataSource.fetchMeteo(city)
            }
        }

        if (meteo != null) {
            Row(
                modifier = Modifier

                    .fillMaxWidth()
                    .clickable(

                        onClick = { navController.navigate("second_screen/$cityId") }
                    )
                    .padding(16.dp)
            ) {
                // Utiliser Box pour contenir le nom de la ville
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(text = city.name, style = MaterialTheme.typography.bodyLarge)
                }
                Spacer(modifier = Modifier.width(32.dp))
                Text(
                    text = "${meteo?.TemperatureActuelle}°C",
                    style = MaterialTheme.typography.bodyLarge
                )
                Spacer(modifier = Modifier.width(16.dp))

                // Icône d'information
                Icon(
                    modifier = Modifier
                        .size(32.dp) // Définir la taille de l'icône
                        .padding(4.dp), // Ajouter un espacement si nécessaire
                    painter = painterResource(id = getWeatherIcon(meteo?.weatherCode ?: -1)),
                    contentDescription = "Weather Icon"
                )
                Spacer(modifier = Modifier.weight(1f))
                // Icône de suppression
                Icon(
                    imageVector = Icons.Filled.Delete,
                    contentDescription = "Delete Icon",
                    modifier = Modifier
                        .clickable(onClick = {
                            onDeleteClick() // Appeler le callback pour supprimer la ligne
                        })
                )
            }
            // Ajoutez un séparateur horizontal
            HorizontalDivider()
        } else {
            Text(
                text = "Chargement...",
                modifier = Modifier.padding(16.dp)
            ) // Affichage pendant le chargement
        }
    }


    @Composable
    fun ListingFavoris(navController: NavController,cityList: List<City>, cityViewModel: CityViewModel) {
        val context = LocalContext.current


        if (cityList.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth() // Ajuste la largeur en fonction du contenu
                    .padding(16.dp), // Ajouter un espacement autour de la Box
                contentAlignment = Alignment.Center // Centrer le contenu horizontalement
            ) {
                Text(text = "Aucun favoris")
            }
            HorizontalDivider()
        } else {
            for (city in cityList) {
                Column {
                    VilleFavoris(
                        navController,
                        city,
                        onCityClick = { /* Handle city click */ },
                        onDeleteClick = { cityViewModel.removeCityFromFavorites(context,city) }
                    )
                }
            }
        }
    }

    @Composable
    fun Greeting(name: String, modifier: Modifier = Modifier) {
        Text(
            text = "Hello $name!",
            modifier = modifier
        )
    }


    @Preview(showBackground = true)
    @Composable
    fun GreetingPreview() {
        M22Theme {
            Greeting("Android")
        }
    }
