package com.example.buscaminas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.core.net.toUri
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.model.GameStatistics
import com.example.buscaminas.viewmodel.GameViewModel

/**
 * Pantalla de estadísticas del juego
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun StatsScreen(
    viewModel: GameViewModel,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val statistics by viewModel.statistics.collectAsState()
    var showDeleteDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var exportMessage by remember { mutableStateOf("") }
    var showImportDialog by remember { mutableStateOf(false) }
    var importFileName by remember { mutableStateOf("") }
    var showImportResult by remember { mutableStateOf("") }
    val context = LocalContext.current
    var exportJsonBuffer by remember { mutableStateOf<String?>(null) }

    // Launcher para crear documento (exportar)
    val createDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri == null) {
            exportMessage = "Exportación cancelada"
            showExportDialog = true
            exportJsonBuffer = null
            return@rememberLauncherForActivityResult
        }

        try {
            val json = exportJsonBuffer
            if (!json.isNullOrEmpty()) {
                context.contentResolver.openOutputStream(uri)?.use { out ->
                    out.write(json.toByteArray(Charsets.UTF_8))
                }
                exportMessage = "Exportado correctamente"
            } else {
                exportMessage = "No hay datos para exportar"
            }
        } catch (e: Exception) {
            e.printStackTrace()
            exportMessage = "Error al exportar: ${e.message}"
        } finally {
            showExportDialog = true
            exportJsonBuffer = null
        }
    }

    // Launcher para abrir documento (importar)
    val openDocumentLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri == null) return@rememberLauncherForActivityResult
        try {
            context.contentResolver.openInputStream(uri)?.bufferedReader()?.use { reader ->
                val json = reader.readText()
                viewModel.importGamesFromJsonString(json, onComplete = { inserted ->
                    showImportResult = "Importadas: $inserted partidas"
                }, onError = {
                    showImportResult = "Error: ${it.message}"
                })
            }
        } catch (e: Exception) {
            e.printStackTrace()
            showImportResult = "Error al leer archivo: ${e.message}"
        }
    }
    
    // Diálogo de confirmación para borrar estadísticas
    if (showDeleteDialog) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Borrar Estadísticas") },
            text = { Text("¿Estás seguro de que quieres borrar todas las estadísticas? Esta acción no se puede deshacer.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.clearAllStatistics()
                        showDeleteDialog = false
                    }
                ) {
                    Text("Borrar", color = Color.Red)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Estadísticas") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, "Volver")
                    }
                },
                actions = {
                    IconButton(onClick = { showDeleteDialog = true }) {
                        Icon(Icons.Default.Delete, "Borrar estadísticas")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF1976D2),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(paddingValues)
                .background(Color(0xFFF5F5F5))
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Resumen general
            GeneralStatsCard(statistics)
            
            // Estadísticas de jugadores
            PlayersStatsCard(statistics)
            
            // Estadísticas de tiempo
            TimeStatsCard(statistics)
            
            // Estadísticas de juego
            GameplayStatsCard(statistics)
            
            // Última partida
            LastGameCard(statistics)
            
            Spacer(modifier = Modifier.height(16.dp))

            // Botones de exportar / importar
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Export interno (ya implementado)
                Button(onClick = {
                    // Generar nombre de archivo por defecto
                    val fileName = "buscaminas_export_${System.currentTimeMillis()}.json"
                    viewModel.exportAllGamesToInternalFile(fileName, onResult = { success ->
                        exportMessage = if (success) "Exportado internamente como: $fileName" else "Error al exportar"
                        showExportDialog = true
                    }, onError = {
                        exportMessage = "Error: ${it.message}"
                        showExportDialog = true
                    })
                }) {
                    Text("Exportar (interno)")
                }

                // Export vía SAF (guardar donde el usuario elija)
                Button(onClick = {
                    // Primero obtener el JSON desde ViewModel, luego lanzar el selector CreateDocument
                    val suggestedName = "buscaminas_export_${System.currentTimeMillis()}.json"
                    viewModel.exportAllGamesJson(onResult = { json ->
                        exportJsonBuffer = json
                        createDocumentLauncher.launch(suggestedName)
                    }, onError = { err ->
                        exportMessage = "Error al generar JSON: ${err.message}"
                        showExportDialog = true
                    })
                }) {
                    Text("Exportar (Guardar como...)")
                }

                // Import vía SAF (abrir archivo JSON)
                Button(onClick = {
                    // MIME filter para JSON
                    openDocumentLauncher.launch(arrayOf("application/json", "text/*"))
                }) {
                    Text("Importar (Seleccionar archivo)")
                }
            }

            // Diálogo de resultado de export
            if (showExportDialog) {
                AlertDialog(
                    onDismissRequest = { showExportDialog = false },
                    confirmButton = {
                        TextButton(onClick = { showExportDialog = false }) { Text("OK") }
                    },
                    title = { Text("Exportar") },
                    text = { Text(exportMessage) }
                )
            }

            // Diálogo para indicar el nombre del archivo a importar
            if (showImportDialog) {
                AlertDialog(
                    onDismissRequest = { showImportDialog = false },
                    title = { Text("Importar partidas desde archivo interno") },
                    text = {
                        Column {
                            Text("Ingrese el nombre del archivo dentro del almacenamiento interno de la app:")
                            Spacer(modifier = Modifier.height(8.dp))
                            OutlinedTextField(
                                value = importFileName,
                                onValueChange = { importFileName = it },
                                placeholder = { Text("ej: buscam_export_... .json") },
                                singleLine = true
                            )
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            showImportDialog = false
                            if (importFileName.isNotBlank()) {
                                viewModel.importGamesFromInternalFile(importFileName, onComplete = { inserted ->
                                    showImportResult = "Importadas: $inserted partidas"
                                }, onError = {
                                    showImportResult = "Error: ${it.message}"
                                })
                            } else {
                                showImportResult = "Nombre de archivo vacío"
                            }
                        }) { Text("Importar") }
                    },
                    dismissButton = {
                        TextButton(onClick = { showImportDialog = false }) { Text("Cancelar") }
                    }
                )
            }

            if (showImportResult.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(showImportResult, fontSize = 14.sp, color = Color.Gray)
            }
        }
    }
}

/**
 * Tarjeta con estadísticas generales
 */
@Composable
private fun GeneralStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "📊 Resumen General",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Total de Partidas", stats.totalGames.toString())
                StatItem("Completadas", stats.totalCompletedGames.toString())
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Empates", stats.totalDraws.toString())
                StatItem("Game Overs", stats.totalGameOvers.toString())
            }
            
            // Barra de progreso de tasa de finalización
            Column(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Tasa de Finalización",
                    fontSize = 14.sp,
                    color = Color.Gray
                )
                Spacer(modifier = Modifier.height(4.dp))
                LinearProgressIndicator(
                    progress = { stats.getCompletionRate() / 100f },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(8.dp)
                        .clip(RoundedCornerShape(4.dp)),
                    color = Color(0xFF4CAF50),
                    trackColor = Color(0xFFE0E0E0)
                )
                Text(
                    text = "${String.format("%.1f", stats.getCompletionRate())}%",
                    fontSize = 12.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(top = 2.dp)
                )
            }
        }
    }
}

/**
 * Tarjeta con estadísticas de jugadores
 */
@Composable
private fun PlayersStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "👥 Estadísticas de Jugadores",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            HorizontalDivider()
            
            // Jugador 1
            PlayerStatsSection(
                playerName = stats.player1Stats.name,
                playerColor = Color(0xFF2196F3),
                stats = stats.player1Stats
            )
            
            HorizontalDivider()
            
            // Jugador 2
            PlayerStatsSection(
                playerName = stats.player2Stats.name,
                playerColor = Color(0xFFF44336),
                stats = stats.player2Stats
            )
        }
    }
}

/**
 * Sección de estadísticas de un jugador
 */
@Composable
private fun PlayerStatsSection(
    playerName: String,
    playerColor: Color,
    stats: com.example.buscaminas.model.PlayerStatistics
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Text(
            text = playerName,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = playerColor
        )
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Victorias", stats.totalWins.toString())
            StatItem("Tasa de Victoria", stats.getFormattedWinRate())
        }
        
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            StatItem("Promedio Pts", stats.getFormattedAveragePoints())
            StatItem("Máximo Pts", stats.maxPoints.toString())
        }
    }
}

/**
 * Tarjeta con estadísticas de tiempo
 */
@Composable
private fun TimeStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "⏱️ Estadísticas de Tiempo",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Duración Promedio", stats.getFormattedAverageDuration())
                StatItem("Partida Más Larga", stats.longestGame)
            }
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Partida Más Corta", stats.shortestGame)
            }
        }
    }
}

/**
 * Tarjeta con estadísticas de gameplay
 */
@Composable
private fun GameplayStatsCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.White)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Text(
                text = "🎮 Estadísticas de Juego",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Celdas Reveladas", stats.totalCellsRevealed.toString())
                StatItem("Banderas Colocadas", stats.totalFlagsPlaced.toString())
            }
        }
    }
}

/**
 * Tarjeta con información de la última partida
 */
@Composable
private fun LastGameCard(stats: GameStatistics) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color(0xFFE3F2FD))
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "🏆 Última Partida",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            
            HorizontalDivider()
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                StatItem("Ganador", stats.lastGameWinner)
                StatItem("Fecha", stats.lastGameDate.split(" ").getOrNull(0) ?: "N/A")
            }
        }
    }
}

/**
 * Componente individual de estadística
 */
@Composable
private fun StatItem(label: String, value: String) {
    Column(
        horizontalAlignment = Alignment.Start
    ) {
        Text(
            text = label,
            fontSize = 12.sp,
            color = Color.Gray
        )
        Text(
            text = value,
            fontSize = 18.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}
