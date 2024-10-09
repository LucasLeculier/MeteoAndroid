package com.example.m2_2

import androidx.compose.foundation.Image
import androidx.compose.foundation.background
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.m2_2.Data.City
import com.example.m2_2.Data.Meteo
import com.example.m2_2.Data.MeteoRemoteDataSource
import com.example.m2_2.Data.getWeatherIcon
import com.example.m2_2.ui.CityState
import kotlinx.coroutines.launch
import kotlinx.serialization.json.Json

@Composable
fun SecondScreen(navController: NavController,cityState: CityState, cityId: Int) {
    val city = cityState.cities.find { it.id == cityId }
    Column {

        topAppBarWithReturn(navController)

        Body(city)

    }



}

@Composable
fun MeteoBody(meteo: Meteo) {
    val backgroundColor = when (meteo.weatherCode) {
        0 -> Color(0xFF87CEEB) // Ciel dégagé
        1 -> Color(0xFFB0E0E6) // Partiellement nuageux
        2 -> Color(0xFF778899) // Nuage
        3 -> Color(0xFF4682B4) // Nuage avec pluie
        61, 63 -> Color(0xFF1E90FF) // Averse
        80 -> Color(0xFFFFA07A) // Orage avec pluie
        95, 96, 99 -> Color(0xFFFF4500) // Orage
        else -> Color.White // Défaut
    }

    Box(modifier = Modifier
        .fillMaxSize()
        .background(backgroundColor)
        .padding(16.dp)) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            CurrentWeather(meteo)
            Spacer(modifier = Modifier.height(20.dp))
            AdditionalDetails(meteo)
        }
    }
}


@Composable
fun ErrorBody() {
    Text(
        text = "Erreur lors du chargement des données",
        style = MaterialTheme.typography.bodyLarge
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun topAppBarWithReturn(navController: NavController){
    TopAppBar(
        title = {Text(text = "Détails météo")},
        navigationIcon = {
            IconButton(onClick = { navController.popBackStack() }) {
                Icon(Icons.Filled.ArrowBack, "backIcon")
            }
        },
    )
}

@Composable
fun Body(city: City?) {
    val coroutineScope = rememberCoroutineScope()
    val meteoDataSource = MeteoRemoteDataSource()



    val meteo = remember { mutableStateOf<Meteo?>(null) }

    LaunchedEffect(city) {
        if (city != null) {
            coroutineScope.launch {
                meteo.value = meteoDataSource.fetchMeteo(city)
            }
        }
    }

    if (meteo.value != null) {
        MeteoBody(meteo.value!!)
    } else {
        ErrorBody()
    }
}




// Composable pour afficher les données météorologiques actuelles
@Composable
fun CurrentWeather(meteo: Meteo) {
    Column(horizontalAlignment = Alignment.CenterHorizontally) {
        WeatherIcon(meteo.weatherCode, Modifier.size(120.dp))
        Text(
            text = meteo.ville.name,
            style = MaterialTheme.typography.headlineLarge,
            textAlign = TextAlign.Center,
            fontSize = 28.sp,
            color = Color.White
        )
        Text(
            text = "${meteo.ville.country}, ${meteo.ville.admin1}",
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
            fontSize = 16.sp,
            color = Color.White
        )
        Text(
            text = "${meteo.TemperatureActuelle}°C",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White
        )
        Text(
            text = "Temp. Min: ${meteo.TemperatureMin}°C  |  Temp. Max: ${meteo.TemperatureMax}°C",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
    }
}

// Composable pour afficher l'icône météo
@Composable
fun WeatherIcon(weatherCode: Int, modifier: Modifier) {
    Image(
        painter = painterResource(id = getWeatherIcon(weatherCode)),
        contentDescription = null,
        modifier = modifier
    )
}

// Composable pour afficher les détails supplémentaires de la météo
@Composable
fun AdditionalDetails(meteo: Meteo) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = "Humidité: ${meteo.Humidite}%",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Text(
            text = "Vitesse du Vent: ${meteo.VitesseVent} km/h",
            style = MaterialTheme.typography.bodyMedium,
            color = Color.White
        )
        Text(
            text = "Heure: ${meteo.HeureActuelle}",
            style = MaterialTheme.typography.bodySmall,
            color = Color.White
        )
    }
}