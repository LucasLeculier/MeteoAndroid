package com.example.m2_2

import android.app.Activity
import android.content.Context
import android.os.Bundle
import android.util.Log
import android.view.inputmethod.InputMethodManager
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
import androidx.compose.foundation.layout.height
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
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Warning
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
import androidx.compose.runtime.collectAsState
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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.TextFieldValue
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewModelScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.example.m2_2.Data.City
import com.example.m2_2.Data.CityCache
import com.example.m2_2.Data.CityRemoteDataSource
import com.example.m2_2.Data.ConnectionStateReceiver
import com.example.m2_2.Data.ICityDataSource
import com.example.m2_2.Data.IMeteoDataSource
import com.example.m2_2.Data.Meteo
import com.example.m2_2.Data.MeteoCache
import com.example.m2_2.Data.MeteoRemoteDataSource
import com.example.m2_2.Data.getWeatherIcon
import com.example.m2_2.Data.meteoAJour
import com.example.m2_2.ui.CityState
import com.example.m2_2.ui.CityViewModel
import com.example.m2_2.ui.theme.M22Theme
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.Objects

class MainActivity : ComponentActivity() {
    //private var favoriteCities by mutableStateOf<List<City>>(emptyList())
    private val cityViewModel: CityViewModel by viewModels()
    private var connectionReceiver: ConnectionStateReceiver? = null


    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        cityViewModel.loadCache(this)
        setContent {
            cityViewModel.setHorsConnexion(!ConnectionStateReceiver.isConnectedToInternet(this))
            connectionReceiver = ConnectionStateReceiver.register(this) { isConnected ->
                cityViewModel.setHorsConnexion(!isConnected)
            }

            val cityState by cityViewModel.CityState.collectAsStateWithLifecycle()
            M22Theme {

                Column {
                    if (cityState.horsConnexion) {
                        BanniereHorsConnexion()
                    }

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
    }


    @Composable
    fun HomeScreen(navController: NavController, cityState: CityState) {
        val context = LocalContext.current
        Column {
            TopBarWithSearch(cityState.searchText) { query ->
                if (query.length >= 1) { // Lancer la recherche après 1 caractère
                    cityViewModel.performSearch(query, context)
                } else {
                    cityViewModel.clearList() // Réinitialiser la liste des résultats
                }
            }
            PositionActuelle(navController)

            if (cityState.isLoading) {
                CircularProgressIndicator(modifier = Modifier.padding(16.dp))
            } else {
                if (cityState.searchResults.isNotEmpty()) {
                    ResultatsRecherche(cityState.searchResults)
                } else {
                    // Afficher la liste des favoris si pas de résultats
                    ListingFavoris(cityState,navController, cityState.cities, cityViewModel)
                }
            }
        }
    }

    @Composable
    fun BanniereHorsConnexion() {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .background(Color(0xFFFF5722)) // Couleur vive (orange vif)
                .padding(8.dp) // Espacement autour du texte
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.Center,
                modifier = Modifier.fillMaxWidth()
            ) {
                Icon(
                    imageVector = Icons.Default.Warning,
                    contentDescription = null,
                    tint = Color.White, // Couleur blanche pour l'icône
                    modifier = Modifier.size(12.dp)
                )
                Spacer(modifier = Modifier.width(8.dp)) // Espace entre l'icône et le texte
                Text(
                    text = "Mode hors connexion",
                    color = Color.White, // Texte en blanc pour le contraste
                    style = MaterialTheme.typography.bodySmall,
                    fontWeight = FontWeight.Bold // Texte en gras pour plus de visibilité
                )
            }
        }
    }


    @OptIn(ExperimentalPermissionsApi::class)
    @Composable
    fun PositionActuelle(navController: NavController) {
        // Utilisation de rememberMultiplePermissionsState pour gérer les permissions
        val permissionState = rememberPermissionState(permission = android.Manifest.permission.ACCESS_FINE_LOCATION)

        HorizontalDivider()

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable {
                    // Si l'utilisateur a la permission de localisation, naviguez
                    when {
                        permissionState.status.isGranted -> {
                            navController.navigate("second_screen/0")
                        }

                        permissionState.status.shouldShowRationale -> {

                            permissionState.launchPermissionRequest()
                        }

                        else -> {
                            // Demande la permission si elle n'est pas encore accordée
                            permissionState.launchPermissionRequest()
                        }
                    }
                }
                .padding(16.dp)
        ) {
            // Texte centré pour la position actuelle
            Text(
                text = "Météo de ma Position actuelle",
                style = MaterialTheme.typography.bodyLarge,
                textAlign = TextAlign.Center,
                modifier = Modifier.weight(1f)
            )

            // Icône de localisation
            Icon(
                imageVector = Icons.Filled.LocationOn,
                contentDescription = "LocationOn Icon"
            )
        }

        // Ajoutez un séparateur horizontal
        HorizontalDivider()
    }




    @OptIn(ExperimentalMaterial3Api::class)
    @Composable
    fun TopBarWithSearch(
        searchText: TextFieldValue,
        modifier: Modifier = Modifier,
        onSearch: (String) -> Unit
    ) {
        Column(modifier = modifier.fillMaxWidth()) {
            // Top App Bar
            TopAppBar(
                title = {
                    Text(
                        text = "Météo de Lucas",
                        textAlign = TextAlign.Center,
                        modifier = Modifier.fillMaxWidth(),
                        color = Color.White // Titre en blanc
                    )
                },
                colors = TopAppBarDefaults.smallTopAppBarColors(
                    containerColor = Color.Black, // Couleur noire
                    titleContentColor = Color.White // Couleur du titre en blanc
                ),
                modifier = modifier
            )

            // Search Bar
            SearchBar(
                onSearch = { onSearch(it) },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                searchText = searchText
            )
        }
    }

    @Composable
    fun ResultatsRecherche(searchResults : List<City>) {
        val context = LocalContext.current
        LazyColumn {
            items(searchResults) { city ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(
                            onClick = {
                                cityViewModel.addCityToFavorites(this@MainActivity, city)
                                val imm =
                                    context.getSystemService(Context.INPUT_METHOD_SERVICE) as InputMethodManager
                                imm.hideSoftInputFromWindow(
                                    (context as Activity).currentFocus?.windowToken,
                                    0
                                )
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
        cityViewModel: CityViewModel,
        cityState : CityState,
        navController: NavController,
        city: City,
        onDeleteClick: () -> Unit, // Callback pour supprimer la ligne
        meteoDataSource: IMeteoDataSource = MeteoRemoteDataSource(LocalContext.current)
    ) {
        val cityId = city.id
        if(cityId == 0) return /// On affiche pas la position actuelle comme favoris, elle porte l'id 0
        var meteo by remember { mutableStateOf<Meteo?>(null) }
        val coroutineScope = rememberCoroutineScope()
        val context = LocalContext.current

        LaunchedEffect(city) {
            coroutineScope.launch {


                if (cityState.horsConnexion || meteoAJour(city, cityState.meteos)) {
                    meteo = cityState.meteos.find { it.ville.id == cityId }
                }else{
                    meteo = meteoDataSource.fetchMeteo(city)
                }


            }
        }

        if (meteo != null) {
            cityViewModel.addMeteoToMeteos(context, meteo)
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
    fun ListingFavoris(cityState : CityState,navController: NavController,cityList: List<City>, cityViewModel: CityViewModel) {
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
                        cityViewModel,
                        cityState,
                        navController,
                        city,
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
