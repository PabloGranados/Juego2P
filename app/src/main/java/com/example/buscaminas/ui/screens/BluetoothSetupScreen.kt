package com.example.buscaminas.ui.screens

import android.Manifest
import android.app.Activity
import android.bluetooth.BluetoothAdapter
import android.content.Intent
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.bluetooth.ConnectionState
import com.example.buscaminas.viewmodel.GameViewModel
import kotlinx.coroutines.launch

/**
 * Pantalla de configuración Bluetooth
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BluetoothSetupScreen(
    viewModel: GameViewModel,
    onConnectionEstablished: () -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    
    val connectionState by viewModel.connectionState.collectAsState()
    val pairedDevices by viewModel.pairedDevices.collectAsState()
    val isHost by viewModel.isHost.collectAsState()
    
    var showPermissionRationale by remember { mutableStateOf(false) }
    
    // Launcher para habilitar Bluetooth
    val enableBluetoothLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            viewModel.refreshPairedDevices()
        }
    }
    
    // Launcher para permisos Bluetooth
    val bluetoothPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions: Map<String, Boolean> ->
        val allGranted = permissions.values.all { it }
        if (allGranted) {
            viewModel.refreshPairedDevices()
        } else {
            showPermissionRationale = true
        }
    }
    
    // Verificar si hay conexión establecida
    LaunchedEffect(connectionState) {
        if (connectionState == ConnectionState.CONNECTED) {
            onConnectionEstablished()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Configuración Bluetooth") },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color(0xFF2196F3),
                    titleContentColor = Color.White
                )
            )
        }
    ) { paddingValues ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .background(Color(0xFFF5F5F5))
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Estado de conexión
            ConnectionStatusCard(connectionState, isHost)
            
            // Verificar Bluetooth
            if (!viewModel.isBluetoothAvailable()) {
                ErrorCard("Este dispositivo no tiene Bluetooth")
            } else if (!viewModel.isBluetoothEnabled()) {
                EnableBluetoothCard {
                    val enableBtIntent = Intent(BluetoothAdapter.ACTION_REQUEST_ENABLE)
                    enableBluetoothLauncher.launch(enableBtIntent)
                }
            } else if (!viewModel.hasBluetoothPermissions()) {
                RequestPermissionsCard(
                    onRequestPermissions = {
                        val permissions = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                            arrayOf(
                                Manifest.permission.BLUETOOTH_SCAN,
                                Manifest.permission.BLUETOOTH_CONNECT,
                                Manifest.permission.BLUETOOTH_ADVERTISE
                            )
                        } else {
                            arrayOf(
                                Manifest.permission.BLUETOOTH,
                                Manifest.permission.BLUETOOTH_ADMIN,
                                Manifest.permission.ACCESS_FINE_LOCATION
                            )
                        }
                        bluetoothPermissionLauncher.launch(permissions)
                    }
                )
            } else {
                // Mostrar opciones de conexión
                when (connectionState) {
                    ConnectionState.DISCONNECTED -> {
                        // Botones para elegir rol
                        Text(
                            text = "Elige cómo conectar:",
                            fontSize = 18.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(top = 16.dp)
                        )
                        
                        Button(
                            onClick = {
                                scope.launch {
                                    viewModel.startBluetoothServer()
                                }
                            },
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(60.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFF4CAF50)
                            )
                        ) {
                            Icon(Icons.Default.Person, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text("Crear partida (Anfitrión)", fontSize = 16.sp)
                        }
                        
                        Text(
                            text = "- o -",
                            fontSize = 14.sp,
                            color = Color.Gray,
                            textAlign = TextAlign.Center,
                            modifier = Modifier.padding(vertical = 8.dp)
                        )
                        
                        Text(
                            text = "Unirse a una partida:",
                            fontSize = 16.sp,
                            fontWeight = FontWeight.SemiBold
                        )
                        
                        // Lista de dispositivos vinculados
                        if (pairedDevices.isEmpty()) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFFFFF3E0)
                                )
                            ) {
                                Text(
                                    text = "No hay dispositivos vinculados.\nVincula dispositivos desde Configuración de Android.",
                                    modifier = Modifier.padding(16.dp),
                                    color = Color(0xFFE65100),
                                    textAlign = TextAlign.Center
                                )
                            }
                        } else {
                            LazyColumn(
                                modifier = Modifier.fillMaxWidth(),
                                verticalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                items(pairedDevices) { device ->
                                    DeviceCard(
                                        deviceName = device.name ?: "Dispositivo desconocido",
                                        deviceAddress = device.address,
                                        onClick = {
                                            scope.launch {
                                                viewModel.connectToDevice(device)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    
                    ConnectionState.LISTENING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            color = Color(0xFF2196F3)
                        )
                        Text(
                            text = "Esperando conexión...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { viewModel.disconnectBluetooth() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("Cancelar")
                        }
                    }
                    
                    ConnectionState.CONNECTING -> {
                        CircularProgressIndicator(
                            modifier = Modifier.size(60.dp),
                            color = Color(0xFF2196F3)
                        )
                        Text(
                            text = "Conectando...",
                            fontSize = 16.sp,
                            color = Color.Gray
                        )
                        Button(
                            onClick = { viewModel.disconnectBluetooth() },
                            colors = ButtonDefaults.buttonColors(
                                containerColor = Color(0xFFF44336)
                            )
                        ) {
                            Text("Cancelar")
                        }
                    }
                    
                    ConnectionState.CONNECTED -> {
                        Icon(
                            imageVector = Icons.Default.CheckCircle,
                            contentDescription = null,
                            tint = Color(0xFF4CAF50),
                            modifier = Modifier.size(80.dp)
                        )
                        Text(
                            text = "¡Conectado!",
                            fontSize = 24.sp,
                            fontWeight = FontWeight.Bold,
                            color = Color(0xFF4CAF50)
                        )
                        Text(
                            text = "Redirigiendo al juego...",
                            fontSize = 14.sp,
                            color = Color.Gray
                        )
                    }
                }
            }
        }
    }
    
    // Diálogo de explicación de permisos
    if (showPermissionRationale) {
        AlertDialog(
            onDismissRequest = { showPermissionRationale = false },
            title = { Text("Permisos necesarios") },
            text = {
                Text("La aplicación necesita permisos de Bluetooth para conectar dispositivos y jugar en modo multijugador.")
            },
            confirmButton = {
                TextButton(onClick = { showPermissionRationale = false }) {
                    Text("Entendido")
                }
            }
        )
    }
}

@Composable
private fun ConnectionStatusCard(state: ConnectionState, isHost: Boolean) {
    data class StatusInfo(
        val icon: androidx.compose.ui.graphics.vector.ImageVector,
        val text: String,
        val color: Color
    )
    
    val statusInfo = when (state) {
        ConnectionState.DISCONNECTED -> StatusInfo(Icons.Default.Close, "Desconectado", Color.Gray)
        ConnectionState.LISTENING -> StatusInfo(Icons.Default.Refresh, "Esperando conexión (Anfitrión)", Color(0xFF2196F3))
        ConnectionState.CONNECTING -> StatusInfo(Icons.Default.Search, "Conectando...", Color(0xFFFF9800))
        ConnectionState.CONNECTED -> StatusInfo(
            Icons.Default.Check,
            if (isHost) "Conectado (Anfitrión)" else "Conectado (Invitado)",
            Color(0xFF4CAF50)
        )
    }
    
    val icon = statusInfo.icon
    val text = statusInfo.text
    val color = statusInfo.color
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = color.copy(alpha = 0.1f)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = text,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = color
            )
        }
    }
}

@Composable
private fun ErrorCard(message: String) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFEBEE)
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Warning,
                contentDescription = null,
                tint = Color(0xFFD32F2F),
                modifier = Modifier.size(32.dp)
            )
            Text(
                text = message,
                fontSize = 14.sp,
                color = Color(0xFFD32F2F)
            )
        }
    }
}

@Composable
private fun EnableBluetoothCard(onEnableBluetooth: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFFFF3E0)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Close,
                contentDescription = null,
                tint = Color(0xFFFF9800),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Bluetooth desactivado",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFFE65100)
            )
            Button(
                onClick = onEnableBluetooth,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFFFF9800)
                )
            ) {
                Text("Activar Bluetooth")
            }
        }
    }
}

@Composable
private fun RequestPermissionsCard(onRequestPermissions: () -> Unit) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFFE3F2FD)
        )
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Lock,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Permisos necesarios",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color(0xFF1976D2)
            )
            Text(
                text = "La aplicación necesita permisos para usar Bluetooth",
                fontSize = 14.sp,
                color = Color(0xFF1976D2),
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onRequestPermissions,
                colors = ButtonDefaults.buttonColors(
                    containerColor = Color(0xFF2196F3)
                )
            ) {
                Text("Conceder permisos")
            }
        }
    }
}

@Composable
private fun DeviceCard(
    deviceName: String,
    deviceAddress: String,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(
            containerColor = Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                imageVector = Icons.Default.Phone,
                contentDescription = null,
                tint = Color(0xFF2196F3),
                modifier = Modifier.size(32.dp)
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = deviceName,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.Black
                )
                Text(
                    text = deviceAddress,
                    fontSize = 12.sp,
                    color = Color.Gray
                )
            }
            Icon(
                imageVector = Icons.Default.KeyboardArrowRight,
                contentDescription = null,
                tint = Color.Gray
            )
        }
    }
}
