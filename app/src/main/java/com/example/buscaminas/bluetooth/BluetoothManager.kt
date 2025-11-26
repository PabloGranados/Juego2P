package com.example.buscaminas.bluetooth

import android.Manifest
import android.annotation.SuppressLint
import android.bluetooth.BluetoothAdapter
import android.bluetooth.BluetoothDevice
import android.bluetooth.BluetoothServerSocket
import android.bluetooth.BluetoothSocket
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.util.Log
import androidx.core.app.ActivityCompat
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.util.UUID

/**
 * Tipos de mensaje que se pueden enviar por Bluetooth
 */
enum class MessageType {
    CELL_CLICK,
    CELL_LONG_CLICK,
    GAME_STATE,
    RESET_GAME,
    PLAYER_INFO,
    BOARD_SYNC  // Sincronización del tablero (posiciones de minas)
}

/**
 * Mensaje que se envía por Bluetooth
 */
data class BluetoothMessage(
    val type: MessageType,
    val data: String
)

/**
 * Estado de la conexión Bluetooth
 */
enum class ConnectionState {
    DISCONNECTED,
    CONNECTING,
    CONNECTED,
    LISTENING
}

/**
 * Gestor de comunicación Bluetooth para el juego
 */
class BluetoothManager(private val context: Context) {
    
    companion object {
        private const val TAG = "BluetoothManager"
        private const val APP_NAME = "Buscaminas"
        // UUID único para esta aplicación
        private val MY_UUID: UUID = UUID.fromString("8ce255c0-200a-11e0-ac64-0800200c9a66")
        private const val MESSAGE_DELIMITER = "\n"
        const val DISCOVERABLE_DURATION = 300 // 5 minutos
    }
    
    private val bluetoothAdapter: BluetoothAdapter? = BluetoothAdapter.getDefaultAdapter()
    
    private var serverSocket: BluetoothServerSocket? = null
    private var clientSocket: BluetoothSocket? = null
    private var inputStream: InputStream? = null
    private var outputStream: OutputStream? = null
    
    private val _connectionState = MutableStateFlow(ConnectionState.DISCONNECTED)
    val connectionState: StateFlow<ConnectionState> = _connectionState.asStateFlow()
    
    private val _receivedMessage = MutableStateFlow<BluetoothMessage?>(null)
    val receivedMessage: StateFlow<BluetoothMessage?> = _receivedMessage.asStateFlow()
    
    private val _isHost = MutableStateFlow(false)
    val isHost: StateFlow<Boolean> = _isHost.asStateFlow()
    
    private val _discoveredDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = _discoveredDevices.asStateFlow()
    
    private val _isDiscovering = MutableStateFlow(false)
    val isDiscovering: StateFlow<Boolean> = _isDiscovering.asStateFlow()
    
    private var isListening = false
    
    /**
     * Verifica si Bluetooth está disponible
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothAdapter != null
    }
    
    /**
     * Verifica si Bluetooth está habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothAdapter?.isEnabled == true
    }
    
    /**
     * Verifica permisos Bluetooth
     */
    fun hasBluetoothPermissions(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_CONNECT) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_SCAN) == PackageManager.PERMISSION_GRANTED
        } else {
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH) == PackageManager.PERMISSION_GRANTED &&
            ActivityCompat.checkSelfPermission(context, Manifest.permission.BLUETOOTH_ADMIN) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    /**
     * Obtiene lista de dispositivos vinculados
     */
    @SuppressLint("MissingPermission")
    fun getPairedDevices(): List<BluetoothDevice> {
        if (!hasBluetoothPermissions()) return emptyList()
        return bluetoothAdapter?.bondedDevices?.toList() ?: emptyList()
    }
    
    /**
     * Inicia el descubrimiento de dispositivos cercanos
     */
    @SuppressLint("MissingPermission")
    fun startDiscovery() {
        if (!hasBluetoothPermissions()) {
            Log.e(TAG, "No hay permisos para buscar dispositivos")
            return
        }
        
        if (bluetoothAdapter?.isDiscovering == true) {
            bluetoothAdapter?.cancelDiscovery()
        }
        
        _discoveredDevices.value = emptyList()
        _isDiscovering.value = true
        
        val started = bluetoothAdapter?.startDiscovery() ?: false
        if (started) {
            Log.d(TAG, "Búsqueda de dispositivos iniciada")
        } else {
            Log.e(TAG, "No se pudo iniciar la búsqueda")
            _isDiscovering.value = false
        }
    }
    
    /**
     * Detiene el descubrimiento de dispositivos
     */
    @SuppressLint("MissingPermission")
    fun stopDiscovery() {
        if (hasBluetoothPermissions()) {
            bluetoothAdapter?.cancelDiscovery()
            _isDiscovering.value = false
            Log.d(TAG, "Búsqueda de dispositivos detenida")
        }
    }
    
    /**
     * Agrega un dispositivo descubierto a la lista
     */
    fun addDiscoveredDevice(device: BluetoothDevice) {
        val currentList = _discoveredDevices.value.toMutableList()
        if (!currentList.any { it.address == device.address }) {
            currentList.add(device)
            _discoveredDevices.value = currentList
            Log.d(TAG, "Dispositivo descubierto agregado: ${device.address}")
        }
    }
    
    /**
     * Finaliza el descubrimiento
     */
    fun onDiscoveryFinished() {
        _isDiscovering.value = false
        Log.d(TAG, "Búsqueda de dispositivos finalizada. Total encontrados: ${_discoveredDevices.value.size}")
    }
    
    /**
     * Inicia modo servidor (anfitrión)
     */
    @SuppressLint("MissingPermission")
    suspend fun startServer() = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "No hay permisos Bluetooth")
                return@withContext
            }
            
            _isHost.value = true
            _connectionState.value = ConnectionState.LISTENING
            
            serverSocket = bluetoothAdapter?.listenUsingRfcommWithServiceRecord(APP_NAME, MY_UUID)
            Log.d(TAG, "Servidor esperando conexión...")
            
            isListening = true
            
            while (isListening) {
                try {
                    val socket = serverSocket?.accept()
                    if (socket != null) {
                        Log.d(TAG, "Cliente conectado")
                        clientSocket = socket
                        inputStream = socket.inputStream
                        outputStream = socket.outputStream
                        _connectionState.value = ConnectionState.CONNECTED
                        startListening()
                        break
                    }
                } catch (e: IOException) {
                    if (isListening) {
                        Log.e(TAG, "Error aceptando conexión", e)
                    }
                    break
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando servidor", e)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * Conecta a un dispositivo como cliente
     */
    @SuppressLint("MissingPermission")
    suspend fun connectToDevice(device: BluetoothDevice) = withContext(Dispatchers.IO) {
        try {
            if (!hasBluetoothPermissions()) {
                Log.e(TAG, "No hay permisos Bluetooth")
                return@withContext
            }
            
            _isHost.value = false
            _connectionState.value = ConnectionState.CONNECTING
            
            val socket = device.createRfcommSocketToServiceRecord(MY_UUID)
            bluetoothAdapter?.cancelDiscovery()
            
            socket.connect()
            Log.d(TAG, "Conectado al servidor")
            
            clientSocket = socket
            inputStream = socket.inputStream
            outputStream = socket.outputStream
            _connectionState.value = ConnectionState.CONNECTED
            
            startListening()
        } catch (e: Exception) {
            Log.e(TAG, "Error conectando al dispositivo", e)
            _connectionState.value = ConnectionState.DISCONNECTED
        }
    }
    
    /**
     * Inicia la escucha de mensajes
     */
    private suspend fun startListening() = withContext(Dispatchers.IO) {
        val buffer = ByteArray(1024)
        var bytes: Int
        var messageBuffer = StringBuilder()
        
        try {
            while (_connectionState.value == ConnectionState.CONNECTED) {
                bytes = inputStream?.read(buffer) ?: -1
                
                if (bytes > 0) {
                    val receivedData = String(buffer, 0, bytes)
                    messageBuffer.append(receivedData)
                    
                    // Procesar mensajes completos (delimitados por \n)
                    val messages = messageBuffer.toString().split(MESSAGE_DELIMITER)
                    
                    for (i in 0 until messages.size - 1) {
                        val message = messages[i]
                        if (message.isNotEmpty()) {
                            parseMessage(message)
                        }
                    }
                    
                    // Mantener el último fragmento incompleto
                    messageBuffer = StringBuilder(messages.last())
                }
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error leyendo datos", e)
            disconnect()
        }
    }
    
    /**
     * Parsea un mensaje recibido
     */
    private fun parseMessage(message: String) {
        try {
            val parts = message.split("|")
            if (parts.size >= 2) {
                val type = MessageType.valueOf(parts[0])
                val data = parts[1]
                _receivedMessage.value = BluetoothMessage(type, data)
                Log.d(TAG, "Mensaje recibido: $type - $data")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando mensaje", e)
        }
    }
    
    /**
     * Envía un mensaje
     */
    suspend fun sendMessage(message: BluetoothMessage) = withContext(Dispatchers.IO) {
        try {
            if (_connectionState.value == ConnectionState.CONNECTED) {
                val data = "${message.type}|${message.data}$MESSAGE_DELIMITER"
                outputStream?.write(data.toByteArray())
                outputStream?.flush()
                Log.d(TAG, "Mensaje enviado: ${message.type} - ${message.data}")
            } else {
                Log.w(TAG, "No conectado, no se puede enviar mensaje")
            }
        } catch (e: IOException) {
            Log.e(TAG, "Error enviando mensaje", e)
            disconnect()
        }
    }
    
    /**
     * Desconecta y limpia recursos
     */
    fun disconnect() {
        isListening = false
        stopDiscovery()
        
        try {
            inputStream?.close()
            outputStream?.close()
            clientSocket?.close()
            serverSocket?.close()
        } catch (e: IOException) {
            Log.e(TAG, "Error cerrando conexión", e)
        }
        
        inputStream = null
        outputStream = null
        clientSocket = null
        serverSocket = null
        
        _connectionState.value = ConnectionState.DISCONNECTED
        _isHost.value = false
        _discoveredDevices.value = emptyList()
        
        Log.d(TAG, "Desconectado")
    }
    
    /**
     * Limpia el último mensaje recibido
     */
    fun clearReceivedMessage() {
        _receivedMessage.value = null
    }
}
