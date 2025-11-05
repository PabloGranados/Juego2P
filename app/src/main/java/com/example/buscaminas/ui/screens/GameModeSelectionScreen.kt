package com.example.buscaminas.ui.screens

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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.model.GameConfig
import com.example.buscaminas.model.GameDifficulty
import com.example.buscaminas.model.GameMode

/**
 * Pantalla de selección de modo y dificultad del juego
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun GameModeSelectionScreen(
    onNavigateBack: () -> Unit,
    onStartGame: (GameConfig) -> Unit,
    modifier: Modifier = Modifier
) {
    var selectedMode by remember { mutableStateOf(GameMode.TWO_PLAYERS) }
    var selectedDifficulty by remember { mutableStateOf(GameDifficulty.MEDIUM) }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Seleccionar Modo") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
                .padding(16.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "🎮 Configura tu partida",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(vertical = 16.dp)
            )
            
            // Sección de Modo de Juego
            Text(
                text = "Modo de Juego",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            GameModeCard(
                title = "👥 Dos Jugadores",
                description = "Juega con un amigo localmente",
                isSelected = selectedMode == GameMode.TWO_PLAYERS,
                onClick = { selectedMode = GameMode.TWO_PLAYERS }
            )
            
            GameModeCard(
                title = "🤖 Contra IA",
                description = "Próximamente - Juega contra la computadora",
                isSelected = selectedMode == GameMode.VS_AI,
                onClick = { /* selectedMode = GameMode.VS_AI */ },
                enabled = false
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Sección de Dificultad
            Text(
                text = "Dificultad / Tamaño del Tablero",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.align(Alignment.Start)
            )
            
            GameDifficulty.values().forEach { difficulty ->
                DifficultyCard(
                    difficulty = difficulty,
                    isSelected = selectedDifficulty == difficulty,
                    onClick = { selectedDifficulty = difficulty }
                )
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Botón de inicio
            Button(
                onClick = {
                    val config = GameConfig(
                        mode = selectedMode,
                        difficulty = selectedDifficulty
                    )
                    onStartGame(config)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Text(
                    text = "▶️ Iniciar Partida",
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

@Composable
private fun GameModeCard(
    title: String,
    description: String,
    isSelected: Boolean,
    onClick: () -> Unit,
    enabled: Boolean = true
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        enabled = enabled,
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = when {
                !enabled -> Color.LightGray.copy(alpha = 0.5f)
                isSelected -> MaterialTheme.colorScheme.primaryContainer
                else -> Color.White
            },
            contentColor = when {
                !enabled -> Color.Gray
                isSelected -> MaterialTheme.colorScheme.onPrimaryContainer
                else -> MaterialTheme.colorScheme.onSurface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = description,
                    fontSize = 12.sp,
                    color = if (enabled) Color.Gray else Color.DarkGray
                )
            }
            
            if (isSelected) {
                Text(
                    text = "✓",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
        }
    }
}

@Composable
private fun DifficultyCard(
    difficulty: GameDifficulty,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        onClick = onClick,
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(
            defaultElevation = if (isSelected) 8.dp else 2.dp
        ),
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.secondaryContainer
            } else {
                Color.White
            },
            contentColor = if (isSelected) {
                MaterialTheme.colorScheme.onSecondaryContainer
            } else {
                MaterialTheme.colorScheme.onSurface
            }
        ),
        shape = RoundedCornerShape(12.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = difficulty.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    text = "${difficulty.rows}x${difficulty.cols} - ${difficulty.mines} minas",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            
            if (isSelected) {
                Text(
                    text = "✓",
                    fontSize = 24.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.secondary
                )
            }
        }
    }
}
