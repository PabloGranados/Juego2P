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
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.buscaminas.model.GameStatus
import com.example.buscaminas.ui.components.GameBoard
import com.example.buscaminas.ui.components.PlayerInfo
import com.example.buscaminas.viewmodel.GameViewModel
import kotlinx.coroutines.delay

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
    val connectionState by viewModel.connectionState.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    
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
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
                .padding(16.dp)
            .verticalScroll(rememberScrollState()),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Título del juego
        Text(
            text = "💣 Buscaminas 💣",
            fontSize = 28.sp,
            fontWeight = FontWeight.Bold,
            color = Color(0xFF1976D2),
            modifier = Modifier.padding(top = 8.dp)
        )
        
        // Información de los jugadores
        Row(
            modifier = Modifier.fillMaxWidth(),
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
            totalFlags = gameState.totalFlags
        )
        
        // Instrucciones
        if (gameState.isFirstMove) {
            FirstMoveHint()
        } else {
            GameInstructions()
        }
        
        // Tablero del juego
        Card(
            modifier = Modifier
                .fillMaxWidth()
                .wrapContentHeight(),
            elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
            colors = CardDefaults.cardColors(containerColor = Color.White)
        ) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                contentAlignment = Alignment.Center
            ) {
                GameBoard(
                    board = gameState.board,
                    onCellClick = viewModel::onCellClick,
                    onCellLongClick = viewModel::onCellLongClick,
                    lastAction = lastAction
                )
            }
        }
        
        // Botón de reinicio
        Button(
            onClick = { viewModel.resetGame() },
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            colors = ButtonDefaults.buttonColors(
                containerColor = Color(0xFF4CAF50)
            ),
            shape = RoundedCornerShape(12.dp)
        ) {
            Text(
                text = "🔄 Nueva Partida",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        // Botón de estadísticas
        OutlinedButton(
            onClick = onNavigateToStats,
            modifier = Modifier
                .fillMaxWidth()
                .height(50.dp),
            shape = RoundedCornerShape(12.dp),
            colors = ButtonDefaults.outlinedButtonColors(
                contentColor = Color(0xFF1976D2)
            )
        ) {
            Text(
                text = "📊 Ver Estadísticas",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold
            )
        }
        
        Spacer(modifier = Modifier.height(16.dp))
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
    totalFlags: Int
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .padding(12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        InfoItem(
            label = "Celdas restantes",
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
            value = "$placedFlags / $totalFlags",
            icon = "🚩"
        )
    }
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
            fontSize = 24.sp
        )
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

/**
 * Mensaje de primer movimiento
 */
@Composable
private fun FirstMoveHint() {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Row(
            modifier = Modifier.padding(12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = "💡", fontSize = 24.sp)
            Text(
                text = "¡Haz clic en cualquier celda para comenzar! Tu primera jugada siempre es segura.",
                fontSize = 14.sp,
                color = Color(0xFF1976D2)
            )
        }
    }
}

/**
 * Instrucciones del juego
 */
@Composable
private fun GameInstructions() {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
                fontSize = 14.sp,
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
