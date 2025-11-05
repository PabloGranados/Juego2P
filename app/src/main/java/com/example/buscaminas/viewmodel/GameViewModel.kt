package com.example.buscaminas.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import androidx.compose.ui.graphics.Color
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.buscaminas.bluetooth.BluetoothManager
import com.example.buscaminas.bluetooth.BluetoothMessage
import com.example.buscaminas.bluetooth.ConnectionState
import com.example.buscaminas.bluetooth.MessageType
import com.example.buscaminas.data.repository.GameRepository
import com.example.buscaminas.game.Board
import com.example.buscaminas.model.GameState
import com.example.buscaminas.model.GameStatistics
import com.example.buscaminas.model.GameStatus
import com.example.buscaminas.model.Player
import com.example.buscaminas.model.PlayerStatistics
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.delay
import kotlinx.coroutines.Job

/**
 * ViewModel que maneja la lógica del juego de Buscaminas
 */
class GameViewModel(application: Application) : AndroidViewModel(application) {
    
    // Repository para persistencia de datos (público para acceso desde UI)
    val repository = GameRepository(application.applicationContext)
    
    // Bluetooth Manager
    private val bluetoothManager = BluetoothManager(application.applicationContext)
    
    // Configuración del juego
    private val boardRows = 10
    private val boardCols = 10
    private val minesCount = 15
    
    // Puntos por acción
    private val pointsPerCell = 10
    private val pointsPerFlag = 5
    private val penaltyPerMine = 30 // Penalización por pisar una mina
    
    // Instancia del tablero
    private var board = Board(boardRows, boardCols, minesCount)
    
    // Estado del juego
    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    // Animación de la última celda revelada
    private val _lastAction = MutableStateFlow<Pair<Int, Int>?>(null)
    val lastAction: StateFlow<Pair<Int, Int>?> = _lastAction.asStateFlow()
    
    // Tiempo transcurrido en segundos
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime: StateFlow<Long> = _elapsedTime.asStateFlow()
    
    // Job para controlar el timer
    private var timerJob: Job? = null
    
    // Estadísticas del juego
    private val _statistics = MutableStateFlow(GameStatistics())
    val statistics: StateFlow<GameStatistics> = _statistics.asStateFlow()
    
    // Estados Bluetooth
    val connectionState: StateFlow<ConnectionState> = bluetoothManager.connectionState
    val isHost: StateFlow<Boolean> = bluetoothManager.isHost
    
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()
    
    private val _isBluetoothMode = MutableStateFlow(false)
    val isBluetoothMode: StateFlow<Boolean> = _isBluetoothMode.asStateFlow()
    
    init {
        // Intentar cargar partida guardada
        loadSavedGame()
        
        // Cargar estadísticas al iniciar
        loadStatistics()
        
        // Observar mensajes Bluetooth
        viewModelScope.launch {
            bluetoothManager.receivedMessage.collect { message ->
                message?.let { handleBluetoothMessage(it) }
            }
        }
    }
    
    /**
     * Intenta cargar una partida guardada desde SharedPreferences
     */
    private fun loadSavedGame() {
        val savedData = repository.loadGameState()
        
        if (savedData != null) {
            val (gameState, boardData) = savedData
            
            // Restaurar el tablero con las posiciones de las minas
            board.restoreBoard(boardData)
            
            // Restaurar el estado del juego
            _gameState.value = gameState
            
            // Si el juego está activo y no es el primer movimiento, iniciar el temporizador
            if (!gameState.isGameOver() && !gameState.isFirstMove) {
                startTimer()
            }
        } else {
            // No hay partida guardada, crear una nueva
            _gameState.value = createInitialGameState()
        }
    }
    
    /**
     * Crea el estado inicial del juego
     */
    private fun createInitialGameState(): GameState {
        val initialBoard = board.initialize()
        
        val player1 = Player(
            id = 1,
            name = "Jugador 1",
            color = Color(0xFF2196F3) // Azul
        )
        
        val player2 = Player(
            id = 2,
            name = "Jugador 2",
            color = Color(0xFFF44336) // Rojo
        )
        
        return GameState(
            board = initialBoard,
            player1 = player1,
            player2 = player2,
            currentPlayer = 1,
            gameStatus = GameStatus.PLAYING,
            remainingCells = boardRows * boardCols - minesCount,
            totalFlags = minesCount,
            placedFlags = 0,
            isFirstMove = true
        )
    }
    
    /**
     * Maneja el clic en una celda para revelarla
     */
    fun onCellClick(row: Int, col: Int) {
        val currentState = _gameState.value
        
        // Verificar que el juego esté activo
        if (currentState.isGameOver()) return
        
        // En modo Bluetooth, verificar que sea el turno del jugador local
        if (_isBluetoothMode.value) {
            val isMyTurn = (bluetoothManager.isHost.value && currentState.currentPlayer == 1) ||
                          (!bluetoothManager.isHost.value && currentState.currentPlayer == 2)
            if (!isMyTurn) return
        }
        
        // Verificar que la celda pueda ser revelada
        val cell = currentState.board[row][col]
        if (!cell.canBeRevealed()) return
        
        // Enviar mensaje Bluetooth si está en modo multijugador
        if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
            viewModelScope.launch {
                bluetoothManager.sendMessage(
                    BluetoothMessage(MessageType.CELL_CLICK, "$row,$col")
                )
            }
        }
        
        viewModelScope.launch {
            // Obtener el estado actual al inicio de la coroutine
            var workingState = _gameState.value
            
            // Si es el primer movimiento, generar las minas y guardar tiempo de inicio
            if (workingState.isFirstMove) {
                // Generar minas (la función ya limpia las minas existentes)
                board.generateMines(row, col)
                
                // Guardar tiempo de inicio y resetear contadores
                repository.saveGameStartTime()
                repository.resetCurrentGameCounters()
                
                // Actualizar el estado marcando que ya no es el primer movimiento
                val updatedState = workingState.copy(
                    board = board.getBoard(),
                    isFirstMove = false
                )
                _gameState.value = updatedState
                workingState = updatedState  // IMPORTANTE: actualizar la variable de trabajo
                
                // Guardar el estado con las minas generadas
                repository.saveCurrentGameToPreferences(updatedState)
                
                // Iniciar el temporizador
                startTimer()
                
                // Pequeña pausa para asegurar que todo esté sincronizado
                delay(50)
            }
            
            // Revelar la celda usando el estado actualizado
            val (revealedCells, hitMine) = board.revealCell(row, col)
            
            if (revealedCells.isEmpty()) return@launch
            
            // Guardar en SharedPreferences el número de celdas reveladas
            repository.incrementCellsRevealed(revealedCells.size)
            
            // Actualizar el estado del tablero
            val updatedBoard = board.getBoard()
            val remainingCells = board.countRemainingCells()
            
            // Animación de la última acción
            _lastAction.value = row to col
            
            if (hitMine) {
                // MECÁNICA MEJORADA PARA 2 JUGADORES:
                // En lugar de terminar el juego, el jugador pierde puntos y pierde su turno
                val currentPlayer = workingState.getCurrentPlayerData()
                val penalizedPlayer = currentPlayer.subtractPoints(penaltyPerMine)
                
                val newState = if (workingState.currentPlayer == 1) {
                    workingState.copy(
                        board = updatedBoard,
                        player1 = penalizedPlayer,
                        remainingCells = remainingCells,
                        lastRevealedBy = workingState.currentPlayer
                    )
                } else {
                    workingState.copy(
                        board = updatedBoard,
                        player2 = penalizedPlayer,
                        remainingCells = remainingCells,
                        lastRevealedBy = workingState.currentPlayer
                    )
                }
                
                // Guardar estado
                repository.saveCurrentGameToPreferences(newState)
                
                // Verificar si se revelaron todas las celdas seguras (victoria)
                if (remainingCells == 0) {
                    handleVictory(newState)
                } else {
                    // Cambiar de turno después de pisar una mina
                    _gameState.value = newState.switchTurn()
                }
            } else {
                // Calcular puntos por las celdas reveladas
                val earnedPoints = revealedCells.size * pointsPerCell
                val currentPlayer = workingState.getCurrentPlayerData()
                val updatedPlayer = currentPlayer.addPoints(earnedPoints)
                
                // Actualizar el estado
                val newState = if (workingState.currentPlayer == 1) {
                    workingState.copy(
                        board = updatedBoard,
                        player1 = updatedPlayer,
                        remainingCells = remainingCells,
                        lastRevealedBy = workingState.currentPlayer
                    )
                } else {
                    workingState.copy(
                        board = updatedBoard,
                        player2 = updatedPlayer,
                        remainingCells = remainingCells,
                        lastRevealedBy = workingState.currentPlayer
                    )
                }
                
                // Guardar estado en SharedPreferences
                repository.saveCurrentGameToPreferences(newState)
                
                // Verificar victoria
                if (remainingCells == 0) {
                    handleVictory(newState)
                } else {
                    // Cambiar de turno
                    _gameState.value = newState.switchTurn()
                }
            }
        }
    }
    
    /**
     * Maneja el clic largo en una celda para colocar/quitar bandera
     */
    fun onCellLongClick(row: Int, col: Int) {
        val currentState = _gameState.value
        
        // Verificar que el juego esté activo
        if (currentState.isGameOver()) return
        
        // En modo Bluetooth, verificar que sea el turno del jugador local
        if (_isBluetoothMode.value) {
            val isMyTurn = (bluetoothManager.isHost.value && currentState.currentPlayer == 1) ||
                          (!bluetoothManager.isHost.value && currentState.currentPlayer == 2)
            if (!isMyTurn) return
        }
        
        // Verificar que la celda pueda tener bandera
        val cell = currentState.board[row][col]
        if (!cell.canBeFlagged()) return
        
        // Enviar mensaje Bluetooth si está en modo multijugador
        if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
            viewModelScope.launch {
                bluetoothManager.sendMessage(
                    BluetoothMessage(MessageType.CELL_LONG_CLICK, "$row,$col")
                )
            }
        }
        
        viewModelScope.launch {
            // Alternar bandera
            val flagPlaced = board.toggleFlag(row, col)
            
            if (flagPlaced == null) return@launch
            
            val updatedBoard = board.getBoard()
            val placedFlags = board.countPlacedFlags()
            
            // Animación de la última acción
            _lastAction.value = row to col
            
            // Actualizar puntos si se colocó una bandera
            if (flagPlaced) {
                // Guardar en SharedPreferences
                repository.incrementFlagsPlaced()
                
                val currentPlayer = currentState.getCurrentPlayerData()
                val updatedPlayer = currentPlayer.addPoints(pointsPerFlag)
                
                val newState = if (currentState.currentPlayer == 1) {
                    currentState.copy(
                        board = updatedBoard,
                        player1 = updatedPlayer,
                        placedFlags = placedFlags
                    )
                } else {
                    currentState.copy(
                        board = updatedBoard,
                        player2 = updatedPlayer,
                        placedFlags = placedFlags
                    )
                }
                
                // Guardar estado en SharedPreferences
                repository.saveCurrentGameToPreferences(newState)
                
                // Cambiar de turno después de colocar una bandera
                _gameState.value = newState.switchTurn()
            } else {
                // Decrementar el contador cuando se quita una bandera
                repository.decrementFlagsPlaced()
                
                // Solo actualizar el tablero si se quitó una bandera (sin cambiar turno ni puntos)
                _gameState.value = currentState.copy(
                    board = updatedBoard,
                    placedFlags = placedFlags
                )
            }
        }
    }
    
    /**
     * Maneja el fin del juego cuando se toca una mina
     */
    private fun handleGameOver() {
        viewModelScope.launch {
            // Detener el temporizador
            stopTimer()
            
            val currentState = _gameState.value
            val updatedBoard = board.getBoard()
            
            // El jugador contrario gana
            val winner = currentState.getOpponentPlayerData()
            val updatedWinner = winner.addWin()
            
            val gameStatus = if (currentState.currentPlayer == 1) {
                GameStatus.PLAYER2_WON
            } else {
                GameStatus.PLAYER1_WON
            }
            
            val finalState = if (currentState.currentPlayer == 1) {
                currentState.copy(
                    board = updatedBoard,
                    player2 = updatedWinner,
                    gameStatus = gameStatus
                )
            } else {
                currentState.copy(
                    board = updatedBoard,
                    player1 = updatedWinner,
                    gameStatus = gameStatus
                )
            }
            
            _gameState.value = finalState
            
            // Guardar partida en la base de datos
            saveGameToDatabase(finalState)
        }
    }
    
    /**
     * Maneja la victoria cuando se revelan todas las celdas seguras
     */
    private fun handleVictory(state: GameState) {
        viewModelScope.launch {
            // Detener el temporizador
            stopTimer()
            
            // Comparar puntos para determinar el ganador
            val gameStatus = when {
                state.player1.points > state.player2.points -> GameStatus.PLAYER1_WON
                state.player2.points > state.player1.points -> GameStatus.PLAYER2_WON
                else -> GameStatus.DRAW
            }
            
            // Actualizar victorias
            val updatedPlayer1 = if (gameStatus == GameStatus.PLAYER1_WON) {
                state.player1.addWin()
            } else {
                state.player1
            }
            
            val updatedPlayer2 = if (gameStatus == GameStatus.PLAYER2_WON) {
                state.player2.addWin()
            } else {
                state.player2
            }
            
            val finalState = state.copy(
                player1 = updatedPlayer1,
                player2 = updatedPlayer2,
                gameStatus = gameStatus
            )
            
            _gameState.value = finalState
            
            // Guardar partida en la base de datos
            saveGameToDatabase(finalState)
        }
    }
    
    /**
     * Guarda la partida completada en la base de datos
     */
    private suspend fun saveGameToDatabase(finalState: GameState) {
        val totalCellsRevealed = repository.getCurrentCellsRevealed()
        val totalFlagsPlaced = repository.getCurrentFlagsPlaced()
        
        repository.saveGameRecord(
            gameState = finalState,
            totalCellsRevealed = totalCellsRevealed,
            totalFlagsPlaced = totalFlagsPlaced,
            boardRows = boardRows,
            boardCols = boardCols,
            totalMines = minesCount
        )
        
        // Actualizar victorias en SharedPreferences
        repository.saveTotalWins(finalState.player1.wins, finalState.player2.wins)
        
        // Recargar estadísticas
        loadStatistics()
    }
    
    /**
     * Reinicia el juego manteniendo las victorias
     */
    fun resetGame() {
        viewModelScope.launch {
            // Resetear el temporizador
            resetTimer()
            
            val currentState = _gameState.value
            
            // Crear nuevo tablero
            board = Board(boardRows, boardCols, minesCount)
            val initialBoard = board.initialize()
            
            // Resetear contadores
            repository.resetCurrentGameCounters()
            
            // Resetear puntos pero mantener victorias
            val resetPlayer1 = currentState.player1.resetPoints()
            val resetPlayer2 = currentState.player2.resetPoints()
            
            val newState = GameState(
                board = initialBoard,
                player1 = resetPlayer1,
                player2 = resetPlayer2,
                currentPlayer = 1,
                gameStatus = GameStatus.PLAYING,
                remainingCells = boardRows * boardCols - minesCount,
                totalFlags = minesCount,
                placedFlags = 0,
                isFirstMove = true
            )
            
            _gameState.value = newState
            
            // Guardar el nuevo estado limpio
            repository.saveCurrentGameToPreferences(newState)
            
            _lastAction.value = null
        }
    }
    
    /**
     * Limpia la última acción (para animaciones)
     */
    fun clearLastAction() {
        _lastAction.value = null
    }
    
    /**
     * Inicia el temporizador del juego
     */
    private fun startTimer() {
        // Cancelar cualquier timer previo
        timerJob?.cancel()
        
        timerJob = viewModelScope.launch {
            // Pequeña pausa para asegurar que se haya guardado el tiempo de inicio
            delay(100)
            
            val startTime = repository.getGameStartTime()
            if (startTime <= 0) {
                // Si no hay tiempo guardado, guardar ahora
                repository.saveGameStartTime()
                delay(50)
            }
            
            while (true) {
                val currentStartTime = repository.getGameStartTime()
                if (currentStartTime > 0) {
                    val elapsed = (System.currentTimeMillis() - currentStartTime) / 1000
                    _elapsedTime.value = elapsed
                }
                delay(1000) // Actualizar cada segundo
            }
        }
    }
    
    /**
     * Detiene el temporizador del juego
     */
    private fun stopTimer() {
        timerJob?.cancel()
        timerJob = null
    }
    
    /**
     * Resetea el temporizador
     */
    private fun resetTimer() {
        stopTimer()
        _elapsedTime.value = 0L
    }
    
    /**
     * Carga las estadísticas desde la base de datos
     */
    fun loadStatistics() {
        viewModelScope.launch {
            try {
                val totalGames = repository.getTotalGames()
                val totalCompleted = repository.getTotalCompletedGames()
                val totalGameOvers = repository.getTotalGameOvers()
                val totalDraws = repository.getTotalDraws()
                
                val player1Wins = repository.getPlayer1Wins()
                val player2Wins = repository.getPlayer2Wins()
                
                val avgPlayer1Points = repository.getAveragePlayer1Points()
                val avgPlayer2Points = repository.getAveragePlayer2Points()
                
                val maxPlayer1Points = repository.getMaxPlayer1Points()
                val maxPlayer2Points = repository.getMaxPlayer2Points()
                
                val avgDuration = repository.getAverageDuration()
                
                val longestGame = repository.getLongestGame()
                val shortestGame = repository.getShortestGame()
                
                val totalCellsRevealed = repository.getTotalCellsRevealed()
                val totalFlagsPlaced = repository.getTotalFlagsPlaced()
                
                val allGames = repository.getAllGamesList()
                val lastGame = allGames.firstOrNull()
                
                // Calcular tasas de victoria
                val player1WinRate = if (totalGames > 0) {
                    (player1Wins.toFloat() / totalGames.toFloat()) * 100
                } else 0f
                
                val player2WinRate = if (totalGames > 0) {
                    (player2Wins.toFloat() / totalGames.toFloat()) * 100
                } else 0f
                
                val stats = GameStatistics(
                    totalGames = totalGames,
                    totalCompletedGames = totalCompleted,
                    totalGameOvers = totalGameOvers,
                    totalDraws = totalDraws,
                    player1Stats = PlayerStatistics(
                        name = "Jugador 1",
                        totalWins = player1Wins,
                        averagePoints = avgPlayer1Points,
                        maxPoints = maxPlayer1Points,
                        winRate = player1WinRate
                    ),
                    player2Stats = PlayerStatistics(
                        name = "Jugador 2",
                        totalWins = player2Wins,
                        averagePoints = avgPlayer2Points,
                        maxPoints = maxPlayer2Points,
                        winRate = player2WinRate
                    ),
                    averageDuration = avgDuration,
                    longestGame = longestGame?.getFormattedDuration() ?: "00:00",
                    shortestGame = shortestGame?.getFormattedDuration() ?: "00:00",
                    totalCellsRevealed = totalCellsRevealed,
                    totalFlagsPlaced = totalFlagsPlaced,
                    lastGameDate = lastGame?.getFormattedDate() ?: "Sin partidas",
                    lastGameWinner = lastGame?.getWinnerName() ?: "N/A"
                )
                
                _statistics.value = stats
            } catch (e: Exception) {
                // Manejar error
                e.printStackTrace()
            }
        }
    }
    
    /**
     * Elimina todas las estadísticas
     */
    fun clearAllStatistics() {
        viewModelScope.launch {
            repository.deleteAllGames()
            repository.clearPreferences()
            loadStatistics()
        }
    }
    
    // ==================== MÉTODOS BLUETOOTH ====================
    
    /**
     * Verifica si Bluetooth está disponible
     */
    fun isBluetoothAvailable(): Boolean {
        return bluetoothManager.isBluetoothAvailable()
    }
    
    /**
     * Verifica si Bluetooth está habilitado
     */
    fun isBluetoothEnabled(): Boolean {
        return bluetoothManager.isBluetoothEnabled()
    }
    
    /**
     * Verifica permisos Bluetooth
     */
    fun hasBluetoothPermissions(): Boolean {
        return bluetoothManager.hasBluetoothPermissions()
    }
    
    /**
     * Obtiene dispositivos vinculados
     */
    fun refreshPairedDevices() {
        _pairedDevices.value = bluetoothManager.getPairedDevices()
    }
    
    /**
     * Inicia el servidor Bluetooth (anfitrión)
     */
    fun startBluetoothServer() {
        _isBluetoothMode.value = true
        viewModelScope.launch {
            bluetoothManager.startServer()
        }
    }
    
    /**
     * Conecta a un dispositivo Bluetooth (invitado)
     */
    fun connectToDevice(device: BluetoothDevice) {
        _isBluetoothMode.value = true
        viewModelScope.launch {
            bluetoothManager.connectToDevice(device)
        }
    }
    
    /**
     * Desconecta Bluetooth
     */
    fun disconnectBluetooth() {
        bluetoothManager.disconnect()
        _isBluetoothMode.value = false
    }
    
    /**
     * Maneja mensajes recibidos por Bluetooth
     */
    private fun handleBluetoothMessage(message: BluetoothMessage) {
        viewModelScope.launch {
            when (message.type) {
                MessageType.CELL_CLICK -> {
                    val coords = message.data.split(",")
                    if (coords.size == 2) {
                        val row = coords[0].toIntOrNull()
                        val col = coords[1].toIntOrNull()
                        if (row != null && col != null) {
                            // Procesar el clic de celda del oponente
                            processCellClick(row, col)
                        }
                    }
                }
                MessageType.CELL_LONG_CLICK -> {
                    val coords = message.data.split(",")
                    if (coords.size == 2) {
                        val row = coords[0].toIntOrNull()
                        val col = coords[1].toIntOrNull()
                        if (row != null && col != null) {
                            // Procesar el clic largo de celda del oponente
                            processCellLongClick(row, col)
                        }
                    }
                }
                MessageType.RESET_GAME -> {
                    resetGame()
                }
                MessageType.GAME_STATE -> {
                    // Sincronizar estado completo del juego si es necesario
                    // (opcional, para casos de desincronización)
                }
                MessageType.PLAYER_INFO -> {
                    // Actualizar información del jugador si es necesario
                }
            }
            bluetoothManager.clearReceivedMessage()
        }
    }
    
    /**
     * Procesa un clic de celda (tanto local como remoto)
     */
    private suspend fun processCellClick(row: Int, col: Int) {
        var workingState = _gameState.value
        
        if (workingState.isGameOver()) return
        
        val cell = workingState.board[row][col]
        if (!cell.canBeRevealed()) return
        
        if (workingState.isFirstMove) {
            board.generateMines(row, col)
            repository.saveGameStartTime()
            repository.resetCurrentGameCounters()
            
            val updatedState = workingState.copy(
                board = board.getBoard(),
                isFirstMove = false
            )
            _gameState.value = updatedState
            workingState = updatedState  // IMPORTANTE: actualizar la variable de trabajo
            
            // Guardar el estado con las minas generadas
            repository.saveCurrentGameToPreferences(updatedState)
            
            // Iniciar el temporizador
            startTimer()
        }
        
        val (revealedCells, hitMine) = board.revealCell(row, col)
        
        if (revealedCells.isEmpty()) return
        
        repository.incrementCellsRevealed(revealedCells.size)
        
        val updatedBoard = board.getBoard()
        val remainingCells = board.countRemainingCells()
        
        _lastAction.value = row to col
        
        if (hitMine) {
            // MECÁNICA MEJORADA: Penalizar al jugador pero continuar el juego
            val currentPlayer = workingState.getCurrentPlayerData()
            val penalizedPlayer = currentPlayer.subtractPoints(penaltyPerMine)
            
            val newState = if (workingState.currentPlayer == 1) {
                workingState.copy(
                    board = updatedBoard,
                    player1 = penalizedPlayer,
                    remainingCells = remainingCells,
                    lastRevealedBy = workingState.currentPlayer
                )
            } else {
                workingState.copy(
                    board = updatedBoard,
                    player2 = penalizedPlayer,
                    remainingCells = remainingCells,
                    lastRevealedBy = workingState.currentPlayer
                )
            }
            
            repository.saveCurrentGameToPreferences(newState)
            
            if (remainingCells == 0) {
                handleVictory(newState)
            } else {
                _gameState.value = newState.switchTurn()
            }
        } else {
            val earnedPoints = revealedCells.size * pointsPerCell
            val currentPlayer = workingState.getCurrentPlayerData()
            val updatedPlayer = currentPlayer.addPoints(earnedPoints)
            
            val newState = if (workingState.currentPlayer == 1) {
                workingState.copy(
                    board = updatedBoard,
                    player1 = updatedPlayer,
                    remainingCells = remainingCells,
                    lastRevealedBy = workingState.currentPlayer
                )
            } else {
                workingState.copy(
                    board = updatedBoard,
                    player2 = updatedPlayer,
                    remainingCells = remainingCells,
                    lastRevealedBy = workingState.currentPlayer
                )
            }
            
            repository.saveCurrentGameToPreferences(newState)
            
            if (remainingCells == 0) {
                handleVictory(newState)
            } else {
                _gameState.value = newState.switchTurn()
            }
        }
    }
    
    /**
     * Procesa un clic largo de celda (tanto local como remoto)
     */
    private suspend fun processCellLongClick(row: Int, col: Int) {
        val currentState = _gameState.value
        
        if (currentState.isGameOver()) return
        
        val cell = currentState.board[row][col]
        if (!cell.canBeFlagged()) return
        
        val flagPlaced = board.toggleFlag(row, col)
        
        if (flagPlaced == null) return
        
        val updatedBoard = board.getBoard()
        val placedFlags = board.countPlacedFlags()
        
        _lastAction.value = row to col
        
        if (flagPlaced) {
            repository.incrementFlagsPlaced()
            
            val currentPlayer = currentState.getCurrentPlayerData()
            val updatedPlayer = currentPlayer.addPoints(pointsPerFlag)
            
            val newState = if (currentState.currentPlayer == 1) {
                currentState.copy(
                    board = updatedBoard,
                    player1 = updatedPlayer,
                    placedFlags = placedFlags
                )
            } else {
                currentState.copy(
                    board = updatedBoard,
                    player2 = updatedPlayer,
                    placedFlags = placedFlags
                )
            }
            
            repository.saveCurrentGameToPreferences(newState)
            
            _gameState.value = newState.switchTurn()
        } else {
            // Decrementar el contador cuando se quita una bandera
            repository.decrementFlagsPlaced()
            
            _gameState.value = currentState.copy(
                board = updatedBoard,
                placedFlags = placedFlags
            )
        }
    }
    
    /**
     * Activa el modo Bluetooth
     */
    fun enableBluetoothMode() {
        _isBluetoothMode.value = true
    }
    
    /**
     * Desactiva el modo Bluetooth
     */
    fun disableBluetoothMode() {
        _isBluetoothMode.value = false
        disconnectBluetooth()
    }
    
    /**
     * Carga una partida guardada desde un archivo
     */
    fun loadSavedGame(fileName: String): Boolean {
        return try {
            viewModelScope.launch {
                // Detener el temporizador actual
                stopTimer()
                
                // Cargar la partida desde el archivo
                val savedGame = repository.loadGame(fileName)
                
                if (savedGame != null) {
                    val loadedState = savedGame.gameState
                    
                    println("🔍 DEBUG - Cargando partida:")
                    println("  - Nombre: ${savedGame.name}")
                    println("  - Formato: ${savedGame.format}")
                    println("  - Jugador 1: ${loadedState.player1.name} (${loadedState.player1.points} pts)")
                    println("  - Jugador 2: ${loadedState.player2.name} (${loadedState.player2.points} pts)")
                    println("  - Turno: Jugador ${loadedState.currentPlayer}")
                    println("  - Estado: ${loadedState.gameStatus}")
                    println("  - Celdas restantes: ${loadedState.remainingCells}")
                    println("  - Banderas: ${loadedState.placedFlags}/${loadedState.totalFlags}")
                    
                    // Verificar el tablero cargado
                    var minesCount = 0
                    var revealedCount = 0
                    var flaggedCount = 0
                    loadedState.board.forEach { row ->
                        row.forEach { cell ->
                            if (cell.isMine) minesCount++
                            if (cell.isRevealed) revealedCount++
                            if (cell.isFlagged) flaggedCount++
                        }
                    }
                    println("  - Tablero: ${minesCount} minas, ${revealedCount} reveladas, ${flaggedCount} banderas")
                    
                    // Crear un nuevo tablero y restaurar el estado
                    board = Board(boardRows, boardCols, minesCount)
                    val restoredBoard = board.restoreFromState(loadedState.board)
                    
                    // IMPORTANTE: Actualizar el estado con el tablero restaurado
                    val restoredState = loadedState.copy(board = restoredBoard)
                    
                    // Actualizar el estado del juego
                    _gameState.value = restoredState
                    
                    // Guardar en preferencias para continuidad
                    repository.saveCurrentGameToPreferences(restoredState)
                    
                    // Restaurar contadores
                    repository.resetCurrentGameCounters()
                    
                    // Restaurar el tiempo de juego
                    val startTime = System.currentTimeMillis() - (savedGame.duration * 1000)
                    repository.saveGameStartTime(startTime)
                    
                    // Actualizar el tiempo transcurrido
                    _elapsedTime.value = savedGame.duration
                    
                    // Reiniciar el temporizador si el juego sigue en curso
                    if (loadedState.gameStatus == GameStatus.PLAYING) {
                        startTimer()
                    }
                    
                    println("✅ Partida cargada exitosamente: ${savedGame.name}")
                } else {
                    println("❌ Error: No se pudo cargar la partida $fileName")
                }
            }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Error al cargar la partida: ${e.message}")
            false
        }
    }
    
    /**
     * Limpia recursos al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
    }
}
