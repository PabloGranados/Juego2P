package com.example.buscaminas.ui.screens

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Done
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.buscaminas.data.preferences.GamePreferences
import com.example.buscaminas.model.GameStatus
import com.example.buscaminas.model.SavedGame
import com.example.buscaminas.ui.components.GameBoard
import com.example.buscaminas.ui.components.PlayerInfo
import com.example.buscaminas.utils.FileManager
import com.example.buscaminas.viewmodel.GameViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Pantalla principal del juego de Buscaminas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameScreen(
    viewModel: GameViewModel,
    onNavigateToStats: () -> Unit,
    onNavigateBack: () -> Unit = {},
    modifier: Modifier = Modifier
) {
    val gameState by viewModel.gameState.collectAsState()
    val lastAction by viewModel.lastAction.collectAsState()
    val isBluetoothMode by viewModel.isBluetoothMode.collectAsState()
    // connectionState is available in viewModel if needed; currently unused
    val isHost by viewModel.isHost.collectAsState()
    val elapsedTime by viewModel.elapsedTime.collectAsState()
    
    // Contexto y coroutine scope
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val preferences = remember { GamePreferences(context) }
    val fileFormat by preferences.fileFormat.collectAsState(initial = com.example.buscaminas.model.FileFormat.TXT)
    
    // Estado para el diálogo de guardar partida
    var showSaveDialog by remember { mutableStateOf(false) }
    var gameName by remember { mutableStateOf("") }
    var saveMessage by remember { mutableStateOf<String?>(null) }
    var showSaveResult by remember { mutableStateOf(false) }
    
    // Limpiar la animación después de un tiempo
    LaunchedEffect(lastAction) {
        if (lastAction != null) {
            delay(500)
            viewModel.clearLastAction()
        }
    }
    
    // Mostrar diálogo de fin de juego
    if (gameState.isGameOver()) {
        GameOverDialog(
            gameStatus = gameState.gameStatus,
            player1 = gameState.player1,
            player2 = gameState.player2,
            onNewGame = { viewModel.resetGame() }
        )
    }
    
    // Diálogo para guardar partida
    if (showSaveDialog) {
        AlertDialog(
            onDismissRequest = { showSaveDialog = false },
            title = { Text("Guardar Partida") },
            text = {
                Column {
                    Text("Ingresa un nombre para la partida:")
                    Spacer(modifier = Modifier.height(8.dp))
                    OutlinedTextField(
                        value = gameName,
                        onValueChange = { gameName = it },
                        label = { Text("Nombre") },
                        singleLine = true,
                        placeholder = { Text("Mi partida") }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Formato: ${fileFormat.name}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        scope.launch {
                            try {
                                val fileManager = FileManager(context)
                                
                                // Calcular duración
                                val startTime = viewModel.repository.getGameStartTime()
                                val duration = if (startTime > 0) {
                                    (System.currentTimeMillis() - startTime) / 1000
                                } else {
                                    0L
                                }
                                
                                val savedGame = SavedGame(
                                    name = gameName.ifBlank { "partida_${System.currentTimeMillis()}" },
                                    format = fileFormat,
                                    gameState = gameState,
                                    duration = duration
                                )
                                
                                val success = fileManager.saveGame(savedGame)
                                
                                if (success) {
                                    saveMessage = "✅ Partida guardada correctamente"
                                } else {
                                    saveMessage = "❌ Error al guardar la partida"
                                }
                                
                                gameName = ""
                                showSaveDialog = false
                                showSaveResult = true
                            } catch (e: Exception) {
                                saveMessage = "❌ Error al guardar: ${e.message}"
                                showSaveDialog = false
                                showSaveResult = true
                            }
                        }
                    },
                    enabled = gameName.isNotBlank()
                ) {
                    Text("Guardar")
                }
            },
            dismissButton = {
                TextButton(onClick = { showSaveDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Diálogo de resultado de guardado
    if (showSaveResult && saveMessage != null) {
        AlertDialog(
            onDismissRequest = { 
                showSaveResult = false
                saveMessage = null
            },
            title = { Text("Resultado") },
            text = { Text(saveMessage ?: "") },
            confirmButton = {
                TextButton(onClick = { 
                    showSaveResult = false
                    saveMessage = null
                }) {
                    Text("OK")
                }
            }
        )
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { 
                    Text(
                        text = if (isBluetoothMode) {
                            "Buscaminas - Bluetooth ${if (isHost) "(Anfitrión)" else "(Invitado)"}"
                        } else {
                            "Buscaminas - Local"
                        }
                    )
                },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        },
        floatingActionButton = {
            // Solo mostrar el botón si el juego ha empezado y no ha terminado
            if (!gameState.isFirstMove && !gameState.isGameOver()) {
                FloatingActionButton(
                    onClick = { showSaveDialog = true },
                    containerColor = MaterialTheme.colorScheme.primary,
                    contentColor = Color.White
                ) {
                    Icon(
                        imageVector = Icons.Default.Done,
                        contentDescription = "Guardar partida"
                    )
                }
            }
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Padding superior
            Spacer(modifier = Modifier.height(4.dp))
            
            // Título del juego
            Text(
                text = "💣 Buscaminas 💣",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2),
                modifier = Modifier.padding(horizontal = 16.dp)
            )
        
        // Información de los jugadores
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            PlayerInfo(
                player = gameState.player1,
                isCurrentPlayer = gameState.currentPlayer == 1 && !gameState.isGameOver(),
                modifier = Modifier.weight(1f)
            )
            
            PlayerInfo(
                player = gameState.player2,
                isCurrentPlayer = gameState.currentPlayer == 2 && !gameState.isGameOver(),
                modifier = Modifier.weight(1f)
            )
        }
        
        // Información del juego
        GameInfo(
            remainingCells = gameState.remainingCells,
            placedFlags = gameState.placedFlags,
            totalFlags = gameState.totalFlags,
            elapsedTime = elapsedTime,
            modifier = Modifier.padding(horizontal = 16.dp)
        )
        
        // Instrucciones
        if (gameState.isFirstMove) {
            FirstMoveHint(modifier = Modifier.padding(horizontal = 16.dp))
        } else {
            GameInstructions(modifier = Modifier.padding(horizontal = 16.dp))
        }
        
        // Tablero del juego
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight()
                .padding(horizontal = 16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                // Key para forzar recomposición cuando cambia el tablero
                key(gameState.board.hashCode(), gameState.currentPlayer) {
                    GameBoard(
                        board = gameState.board,
                        onCellClick = viewModel::onCellClick,
                        onCellLongClick = viewModel::onCellLongClick,
                        lastAction = lastAction
                    )
                }
            }
        }
        
        // Botón de reinicio
        Button(
            onClick = { viewModel.resetGame() },
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🔄 Nueva Partida",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Botón de estadísticas
        OutlinedButton(
            onClick = onNavigateToStats,
            modifier = Modifier
                .fillMaxWidth()
                .height(48.dp)
                .padding(horizontal = 16.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF1976D2)
            )
        ) {
            Text(
                text = "📊 Ver Estadísticas",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(8.dp))
        }
    }
}

/**
 * Muestra información sobre el estado del juego
 */
@Composable
private fun GameInfo(
    remainingCells: Int,
    placedFlags: Int,
    totalFlags: Int,
    elapsedTime: Long,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        InfoItem(
            label = "Tiempo",
            value = formatTime(elapsedTime),
            icon = "⏱️"
        )
        
        HorizontalDivider(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp),
            color = Color.LightGray
        )
        
        InfoItem(
            label = "Celdas",
            value = remainingCells.toString(),
            icon = "📦"
        )
        
        HorizontalDivider(
            modifier = Modifier
                .width(1.dp)
                .height(40.dp),
            color = Color.LightGray
        )
        
        InfoItem(
            label = "Banderas",
            value = "$placedFlags/$totalFlags",
            icon = "🚩"
        )
    }
}

/**
 * Formatea el tiempo en formato MM:SS
 */
private fun formatTime(seconds: Long): String {
    val minutes = seconds / 60
    val secs = seconds % 60
    return String.format("%02d:%02d", minutes, secs)
}

/**
 * Item de información individual
 */
@Composable
private fun InfoItem(
    label: String,
    value: String,
    icon: String
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = icon,
            fontSize = 20.sp
        )
        Text(
            text = label,
            fontSize = 11.sp,
            color = Color.Gray,
            textAlign = TextAlign.Center
        )
        Text(
            text = value,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = Color.Black
        )
    }
}

/**
 * Mensaje de primer movimiento
 */
@Composable
private fun FirstMoveHint(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💡", fontSize = 20.sp)
            Text(
                text = "¡Haz clic en cualquier celda para comenzar! Tu primera jugada siempre es segura.",
                fontSize = 13.sp,
                color = Color(0xFF1976D2)
            )
        }
    }
}

/**
 * Instrucciones del juego
 */
@Composable
private fun GameInstructions(modifier: Modifier = Modifier) {
    Card(
        modifier = modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Text(
                text = "📖 Instrucciones:",
                fontSize = 13.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
            Text(
                text = "• Toca una celda para revelarla (+10 pts)",
                fontSize = 12.sp,
                color = Color(0xFFE65100)
            )
            Text(
                text = "• Mantén presionado para poner bandera (+5 pts)",
                fontSize = 12.sp,
                color = Color(0xFFE65100)
            )
            Text(
                text = "• ¡Cuidado! Pisar una mina te quita 30 puntos",
                fontSize = 12.sp,
                color = Color(0xFFE65100),
                fontWeight = FontWeight.Bold
            )
        }
    }
}

/**
 * Diálogo que muestra el resultado del juego
 */
@Composable
private fun GameOverDialog(
    gameStatus: GameStatus,
    player1: com.example.buscaminas.model.Player,
    player2: com.example.buscaminas.model.Player,
    onNewGame: () -> Unit
) {
    Dialog(onDismissRequest = { }) {
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            shape = RoundedCornerShape(16.dp),
            colors = CardDefaults.cardColors(
                containerColor = Color.White
            )
        ) {
            Column(
                modifier = Modifier.padding(24.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Título y emoji según el resultado
                val (title, emoji, description) = when (gameStatus) {
                    GameStatus.PLAYER1_WON -> Triple(
                        "¡${player1.name} Gana!",
                        "🎉",
                        "Puntos finales: ${player1.points} vs ${player2.points}"
                    )
                    GameStatus.PLAYER2_WON -> Triple(
                        "¡${player2.name} Gana!",
                        "🎉",
                        "Puntos finales: ${player2.points} vs ${player1.points}"
                    )
                    GameStatus.DRAW -> Triple(
                        "¡Empate!",
                        "🤝",
                        "Ambos jugadores: ${player1.points} puntos"
                    )
                    GameStatus.GAME_OVER -> Triple(
                        "💥 ¡Boom!",
                        "💣",
                        "Pisaste una mina"
                    )
                    else -> Triple("", "", "")
                }
                
                Text(
                    text = emoji,
                    fontSize = 64.sp
                )
                
                Text(
                    text = title,
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF1976D2),
                    textAlign = TextAlign.Center
                )
                
                Text(
                    text = description,
                    fontSize = 16.sp,
                    color = Color.Gray,
                    textAlign = TextAlign.Center
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Tabla de victorias
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(8.dp))
                        .background(Color(0xFFF5F5F5))
                        .padding(12.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = player1.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = player1.color
                        )
                        Text(
                            text = "${player1.wins} victorias",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    
                    Text(
                        text = "VS",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = player2.name,
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = player2.color
                        )
                        Text(
                            text = "${player2.wins} victorias",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                }
                
                // Botón de nueva partida
                Button(
                    onClick = onNewGame,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF4CAF50)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "🎮 Jugar de Nuevo",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        }
    }
}
