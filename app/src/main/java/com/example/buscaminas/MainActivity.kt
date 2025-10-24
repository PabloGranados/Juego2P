package com.example.buscaminas

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.buscaminas.ui.screens.GameScreen
import com.example.buscaminas.ui.screens.StatsScreen
import com.example.buscaminas.ui.theme.BuscaminasTheme
import com.example.buscaminas.viewmodel.GameViewModel

/**
 * Actividad principal del juego de Buscaminas
 */
class MainActivity : ComponentActivity() {
    
    private val gameViewModel: GameViewModel by viewModels()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        setContent {
            BuscaminasTheme {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "game",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        composable("game") {
                            GameScreen(
                                viewModel = gameViewModel,
                                onNavigateToStats = {
                                    navController.navigate("stats")
                                }
                            )
                        }
                        
                        composable("stats") {
                            StatsScreen(
                                viewModel = gameViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}