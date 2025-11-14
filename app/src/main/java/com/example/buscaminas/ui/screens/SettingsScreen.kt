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
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.data.preferences.GamePreferences
import com.example.buscaminas.model.AppTheme
import com.example.buscaminas.model.FileFormat
import kotlinx.coroutines.launch

/**
 * Pantalla de configuración de la aplicación
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val preferences = remember { GamePreferences(context) }
    val scope = rememberCoroutineScope()
    
    // Estados
    val currentTheme by preferences.appTheme.collectAsState(initial = AppTheme.GUINDA_IPN)
    val currentFormat by preferences.fileFormat.collectAsState(initial = FileFormat.JSON)
    val soundEnabled by preferences.soundEnabled.collectAsState(initial = true)
    val currentThemeMode by preferences.themeMode.collectAsState(initial = com.example.buscaminas.model.ThemeMode.SYSTEM)
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración") },
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
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Sección de Tema
            SettingsSection(title = "🎨 Apariencia") {
                Text(
                    text = "Tema de la aplicación",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                AppTheme.values().forEach { theme ->
                    ThemeOption(
                        theme = theme,
                        isSelected = theme == currentTheme,
                        onSelect = {
                            scope.launch {
                                preferences.setAppTheme(theme)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }

                Spacer(modifier = Modifier.height(12.dp))
                Text(
                    text = "Modo de tema",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )

                // Opciones de modo: Claro / Oscuro / Sistema
                com.example.buscaminas.model.ThemeMode.values().forEach { mode ->
                    ThemeModeOption(
                        mode = mode,
                        isSelected = mode == currentThemeMode,
                        onSelect = {
                            scope.launch {
                                preferences.setThemeMode(mode)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Sección de Formato de Archivo
            SettingsSection(title = "💾 Guardado de Partidas") {
                Text(
                    text = "Formato de archivo preferido",
                    fontSize = 14.sp,
                    color = Color.Gray,
                    modifier = Modifier.padding(bottom = 8.dp)
                )
                
                FileFormat.values().forEach { format ->
                    FileFormatOption(
                        format = format,
                        isSelected = format == currentFormat,
                        onSelect = {
                            scope.launch {
                                preferences.setFileFormat(format)
                            }
                        }
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                }
            }
            
            // Sección de Sonido
            SettingsSection(title = "🔊 Sonido") {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Efectos de sonido",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = "Preparado para futuros efectos de audio",
                            fontSize = 12.sp,
                            color = Color.Gray
                        )
                    }
                    Switch(
                        checked = soundEnabled,
                        onCheckedChange = { enabled ->
                            scope.launch {
                                preferences.setSoundEnabled(enabled)
                            }
                        }
                    )
                }
                
                Spacer(modifier = Modifier.height(8.dp))
                
                // Información sobre sonidos
                Card(
                    colors = CardDefaults.cardColors(
                        containerColor = Color(0xFFFFF3E0)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        horizontalArrangement = Arrangement.spacedBy(8.dp),
                        verticalAlignment = Alignment.Top
                    ) {
                        Text(text = "ℹ️", fontSize = 16.sp)
                        Text(
                            text = "Los efectos de sonido están disponibles:\n" +
                                    "• Revelar celda\n" +
                                    "• Colocar/quitar bandera\n" +
                                    "• Pisar mina\n" +
                                    "• Victoria\n" +
                                    "• Clics de botón",
                            fontSize = 11.sp,
                            color = Color(0xFFE65100)
                        )
                    }
                }
            }
            
            // Información
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                )
            ) {
                Column(
                    modifier = Modifier.padding(16.dp)
                ) {
                    Text(
                        text = "ℹ️ Información",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        modifier = Modifier.padding(bottom = 8.dp)
                    )
                    Text(
                        text = "• Los cambios se aplican inmediatamente",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "• El formato de archivo se usa al guardar partidas",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                    Text(
                        text = "• Puedes cargar partidas de cualquier formato",
                        fontSize = 12.sp,
                        color = Color.Gray
                    )
                }
            }
        }
    }
}

@Composable
private fun ThemeModeOption(
    mode: com.example.buscaminas.model.ThemeMode,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = mode.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (mode) {
                        com.example.buscaminas.model.ThemeMode.LIGHT -> "Fuerza modo claro"
                        com.example.buscaminas.model.ThemeMode.DARK -> "Fuerza modo oscuro"
                        com.example.buscaminas.model.ThemeMode.SYSTEM -> "Seguir configuración del sistema"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (isSelected) {
                Text(text = "✓", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun SettingsSection(
    title: String,
    content: @Composable ColumnScope.() -> Unit
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
            Text(
                text = title,
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(bottom = 12.dp)
            )
            content()
        }
    }
}

@Composable
private fun ThemeOption(
    theme: AppTheme,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = theme.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = when (theme) {
                        AppTheme.GUINDA_IPN -> "Color guinda característico del IPN"
                        AppTheme.AZUL_ESCOM -> "Color azul característico de ESCOM"
                    },
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (isSelected) {
                Text(text = "✓", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}

@Composable
private fun FileFormatOption(
    format: FileFormat,
    isSelected: Boolean,
    onSelect: () -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        onClick = onSelect,
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) {
                MaterialTheme.colorScheme.primaryContainer
            } else {
                MaterialTheme.colorScheme.surface
            }
        ),
        border = if (isSelected) {
            androidx.compose.foundation.BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
        } else null
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = format.displayName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = ".${format.extension} - ${format.mimeType}",
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            if (isSelected) {
                Text(text = "✓", fontSize = 20.sp, color = MaterialTheme.colorScheme.primary)
            }
        }
    }
}
