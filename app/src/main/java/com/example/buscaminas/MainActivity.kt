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
import com.example.buscaminas.data.preferences.GamePreferences
import com.example.buscaminas.ui.screens.BluetoothSetupScreen
import com.example.buscaminas.ui.screens.GameScreen
import com.example.buscaminas.ui.screens.MenuScreen
import com.example.buscaminas.ui.screens.SavedGamesScreen
import com.example.buscaminas.ui.screens.SettingsScreen
import com.example.buscaminas.ui.screens.StatsScreen
import com.example.buscaminas.ui.theme.BuscaminasTheme
import com.example.buscaminas.viewmodel.GameViewModel

/**
 * Actividad principal del juego de Buscaminas
 */
class MainActivity : ComponentActivity() {
    
    private val gameViewModel: GameViewModel by viewModels()
    private lateinit var preferences: GamePreferences
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        preferences = GamePreferences(applicationContext)
        enableEdgeToEdge()
        setContent {
            // Observar el tema seleccionado
            val appTheme by preferences.appTheme.collectAsState(initial = com.example.buscaminas.model.AppTheme.GUINDA_IPN)
            
            BuscaminasTheme(appTheme = appTheme) {
                val navController = rememberNavController()
                
                Scaffold(modifier = Modifier.fillMaxSize()) { innerPadding ->
                    NavHost(
                        navController = navController,
                        startDestination = "menu",
                        modifier = Modifier.padding(innerPadding)
                    ) {
                        // Menú principal
                        composable("menu") {
                            MenuScreen(
                                onPlayLocal = {
                                    gameViewModel.disableBluetoothMode()
                                    navController.navigate("game")
                                },
                                onPlayBluetooth = {
                                    navController.navigate("bluetooth_setup")
                                },
                                onViewStats = {
                                    navController.navigate("stats")
                                },
                                onSettings = {
                                    navController.navigate("settings")
                                },
                                onSavedGames = {
                                    navController.navigate("saved_games")
                                }
                            )
                        }
                        
                        // Configuración Bluetooth
                        composable("bluetooth_setup") {
                            BluetoothSetupScreen(
                                viewModel = gameViewModel,
                                onConnectionEstablished = {
                                    gameViewModel.enableBluetoothMode()
                                    navController.navigate("game") {
                                        popUpTo("menu")
                                    }
                                }
                            )
                        }
                        
                        // Pantalla de juego
                        composable("game") {
                            GameScreen(
                                viewModel = gameViewModel,
                                onNavigateToStats = {
                                    navController.navigate("stats")
                                },
                                onNavigateBack = {
                                    gameViewModel.disconnectBluetooth()
                                    navController.navigate("menu") {
                                        popUpTo("menu") { inclusive = true }
                                    }
                                }
                            )
                        }
                        
                        // Estadísticas
                        composable("stats") {
                            StatsScreen(
                                viewModel = gameViewModel,
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // Configuración
                        composable("settings") {
                            SettingsScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                }
                            )
                        }
                        
                        // Partidas guardadas
                        composable("saved_games") {
                            SavedGamesScreen(
                                onNavigateBack = {
                                    navController.popBackStack()
                                },
                                onLoadGame = { fileName ->
                                    // Cargar la partida
                                    val success = gameViewModel.loadSavedGame(fileName)
                                    if (success) {
                                        // Navegar al juego si la carga fue exitosa
                                        navController.navigate("game") {
                                            popUpTo("menu") { inclusive = false }
                                        }
                                    }
                                }
                            )
                        }
                    }
                }
            }
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        gameViewModel.disconnectBluetooth()
    }
}
