package com.example.buscaminas.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.utils.FileManager

/**
 * Pantalla para gestionar partidas guardadas
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SavedGamesScreen(
    onNavigateBack: () -> Unit,
    onLoadGame: ((String) -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val fileManager = remember { FileManager(context) }
    
    var savedGames by remember { mutableStateOf(fileManager.listSavedGames()) }
    var selectedGame by remember { mutableStateOf<String?>(null) }
    var showDeleteDialog by remember { mutableStateOf(false) }
    var gameToDelete by remember { mutableStateOf<String?>(null) }
    var showContentDialog by remember { mutableStateOf(false) }
    var showExportDialog by remember { mutableStateOf(false) }
    var gameToExport by remember { mutableStateOf<String?>(null) }
    var contentToShow by remember { mutableStateOf("") }
    var exportMessage by remember { mutableStateOf("") }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Partidas Guardadas") },
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
        Box(
            modifier = modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .padding(paddingValues)
        ) {
            if (savedGames.isEmpty()) {
                // Estado vacío
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(32.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Text(
                        text = "📂",
                        fontSize = 64.sp
                    )
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "No hay partidas guardadas",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = "Guarda una partida desde el juego para verla aquí",
                        fontSize = 14.sp,
                        color = Color.Gray
                    )
                }
            } else {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item {
                        Text(
                            text = "${savedGames.size} partida${if (savedGames.size != 1) "s" else ""} guardada${if (savedGames.size != 1) "s" else ""}",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                    
                    items(savedGames) { fileName ->
                        SavedGameCard(
                            fileName = fileName,
                            onLoad = if (onLoadGame != null) {
                                { onLoadGame(fileName) }
                            } else null,
                            onView = {
                                contentToShow = fileManager.getFileContent(fileName)
                                showContentDialog = true
                            },
                            onDelete = {
                                gameToDelete = fileName
                                showDeleteDialog = true
                            },
                            onExport = {
                                gameToExport = fileName
                                showExportDialog = true
                            }
                        )
                    }
                }
            }
        }
    }
    
    // Diálogo de confirmación de eliminación
    if (showDeleteDialog && gameToDelete != null) {
        AlertDialog(
            onDismissRequest = { showDeleteDialog = false },
            title = { Text("Eliminar partida") },
            text = { Text("¿Estás seguro de que quieres eliminar '$gameToDelete'?") },
            confirmButton = {
                TextButton(
                    onClick = {
                        fileManager.deleteGame(gameToDelete!!)
                        savedGames = fileManager.listSavedGames()
                        showDeleteDialog = false
                        gameToDelete = null
                    }
                ) {
                    Text("Eliminar", color = MaterialTheme.colorScheme.error)
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
    
    // Diálogo para mostrar contenido
    if (showContentDialog) {
        AlertDialog(
            onDismissRequest = { showContentDialog = false },
            title = { Text("Contenido del archivo") },
            text = {
                LazyColumn {
                    item {
                        Text(
                            text = contentToShow,
                            fontSize = 12.sp,
                            fontFamily = androidx.compose.ui.text.font.FontFamily.Monospace
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showContentDialog = false }) {
                    Text("Cerrar")
                }
            }
        )
    }
    
    // Diálogo de exportación
    if (showExportDialog && gameToExport != null) {
        AlertDialog(
            onDismissRequest = { showExportDialog = false },
            title = { Text("Exportar partida") },
            text = { 
                Column {
                    Text("¿Deseas exportar '$gameToExport' a la carpeta de Descargas?")
                    if (exportMessage.isNotEmpty()) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = exportMessage,
                            color = if (exportMessage.contains("éxito")) Color.Green else Color.Red,
                            fontSize = 12.sp
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val downloadsDir = android.os.Environment.getExternalStoragePublicDirectory(
                            android.os.Environment.DIRECTORY_DOWNLOADS
                        )
                        val destFile = java.io.File(downloadsDir, gameToExport!!)
                        val success = fileManager.exportGame(gameToExport!!, destFile)
                        exportMessage = if (success) {
                            "✅ Exportado con éxito a: ${destFile.absolutePath}"
                        } else {
                            "❌ Error al exportar el archivo"
                        }
                    }
                ) {
                    Text("Exportar")
                }
            },
            dismissButton = {
                TextButton(onClick = { 
                    showExportDialog = false
                    exportMessage = ""
                    gameToExport = null
                }) {
                    Text("Cerrar")
                }
            }
        )
    }
}

@Composable
private fun SavedGameCard(
    fileName: String,
    onLoad: (() -> Unit)?,
    onView: () -> Unit,
    onDelete: () -> Unit,
    onExport: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = fileName,
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    
                    val extension = fileName.substringAfterLast(".", "")
                    val formatIcon = when (extension.lowercase()) {
                        "txt" -> "📄"
                        "xml" -> "📋"
                        "json" -> "📊"
                        else -> "📁"
                    }
                    
                    Text(
                        text = "$formatIcon Formato: ${extension.uppercase()}",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(12.dp))
            
            // Primera fila de botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                // Botón de cargar (solo si onLoad no es null)
                if (onLoad != null) {
                    Button(
                        onClick = onLoad,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary
                        )
                    ) {
                        Text("▶️ Cargar")
                    }
                }
                
                OutlinedButton(
                    onClick = onView,
                    modifier = Modifier.weight(1f)
                ) {
                    Text("👁️ Ver")
                }
            }
            
            Spacer(modifier = Modifier.height(8.dp))
            
            // Segunda fila de botones
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                OutlinedButton(
                    onClick = onExport,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.tertiary
                    )
                ) {
                    Text("📤 Exportar")
                }
                
                OutlinedButton(
                    onClick = onDelete,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = MaterialTheme.colorScheme.error
                    )
                ) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = "Eliminar",
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
        }
    }
}
