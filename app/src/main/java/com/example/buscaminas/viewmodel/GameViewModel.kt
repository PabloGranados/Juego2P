package com.example.buscaminas.viewmodel

import android.app.Application
import android.bluetooth.BluetoothDevice
import android.util.Log
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
import kotlinx.coroutines.Dispatchers
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
    
    // Flag para saber si el tablero ya fue sincronizado
    private val _boardSynced = MutableStateFlow(false)
    val boardSynced: StateFlow<Boolean> = _boardSynced.asStateFlow()
    
    private val _pairedDevices = MutableStateFlow<List<BluetoothDevice>>(emptyList())
    val pairedDevices: StateFlow<List<BluetoothDevice>> = _pairedDevices.asStateFlow()
    
    val discoveredDevices: StateFlow<List<BluetoothDevice>> = bluetoothManager.discoveredDevices
    val isDiscovering: StateFlow<Boolean> = bluetoothManager.isDiscovering
    
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
            
            Log.d("GameViewModel", "onCellClick(row=$row, col=$col) - isHost=${bluetoothManager.isHost.value}, " +
                    "currentPlayer=${currentState.currentPlayer}, isMyTurn=$isMyTurn, boardSynced=${_boardSynced.value}")
            
            if (!isMyTurn) {
                Log.w("GameViewModel", "No es tu turno. Esperando al oponente.")
                return
            }
            
            // Si es el cliente y el tablero no está sincronizado Y es el primer movimiento, no permitir
            if (!bluetoothManager.isHost.value && !_boardSynced.value && currentState.isFirstMove) {
                Log.w("GameViewModel", "Tablero aún no sincronizado en el cliente (primer movimiento)")
                return
            }
        }
        
        // Verificar que la celda pueda ser revelada
        val cell = currentState.board[row][col]
        if (!cell.canBeRevealed()) return
        
        // NO enviar mensaje antes de procesar. Procesamos primero y luego enviamos el resultado
        
        viewModelScope.launch {
            // Obtener el estado actual al inicio de la coroutine
            var workingState = _gameState.value
            
            // Si es el primer movimiento, generar las minas y guardar tiempo de inicio
            if (workingState.isFirstMove) {
                Log.d("GameViewModel", ">>> PRIMER MOVIMIENTO - Generando minas en (row=$row, col=$col)")
                
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
                
                // Si es el host en modo Bluetooth, enviar el tablero al cliente ANTES de revelar
                if (_isBluetoothMode.value && bluetoothManager.isHost.value && 
                    connectionState.value == ConnectionState.CONNECTED) {
                    Log.d("GameViewModel", ">>> HOST enviando BOARD_SYNC al cliente")
                    val boardData = serializeBoardMines()
                    Log.d("GameViewModel", ">>> BoardData: $boardData")
                    bluetoothManager.sendMessage(
                        BluetoothMessage(MessageType.BOARD_SYNC, boardData)
                    )
                    // Esperar a que el mensaje se envíe
                    delay(200)
                    Log.d("GameViewModel", ">>> BOARD_SYNC enviado, continuando...")
                }
                
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
                    
                    // Enviar el estado actualizado al oponente después de pisar una mina
                    if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
                        Log.d("GameViewModel", "Enviando estado después de pisar mina: turno=${_gameState.value.currentPlayer}")
                        val stateData = serializeGameState(_gameState.value)
                        bluetoothManager.sendMessage(
                            BluetoothMessage(MessageType.GAME_STATE_UPDATE, stateData)
                        )
                    }
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
                    
                    // IMPORTANTE: Enviar el estado completo actualizado al oponente DESPUÉS de cambiar el turno
                    if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
                        val currentBoardState = _gameState.value.board
                        var revealedInState = 0
                        currentBoardState.forEach { row ->
                            row.forEach { cell ->
                                if (cell.isRevealed) revealedInState++
                            }
                        }
                        
                        Log.d("GameViewModel", ">>> Enviando GAME_STATE_UPDATE después de revelar celdas")
                        Log.d("GameViewModel", "    - Turno ahora: ${_gameState.value.currentPlayer}")
                        Log.d("GameViewModel", "    - P1: ${_gameState.value.player1.points}pts, P2: ${_gameState.value.player2.points}pts")
                        Log.d("GameViewModel", "    - Celdas reveladas en este turno: ${revealedCells.size}")
                        Log.d("GameViewModel", "    - Total celdas reveladas en el estado: $revealedInState")
                        
                        val stateData = serializeGameState(_gameState.value)
                        Log.d("GameViewModel", ">>> StateData length: ${stateData.length}")
                        
                        bluetoothManager.sendMessage(
                            BluetoothMessage(MessageType.GAME_STATE_UPDATE, stateData)
                        )
                        
                        Log.d("GameViewModel", ">>> GAME_STATE_UPDATE enviado exitosamente")
                    }
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
            
            Log.d("GameViewModel", "onCellLongClick - isHost=${bluetoothManager.isHost.value}, " +
                    "currentPlayer=${currentState.currentPlayer}, isMyTurn=$isMyTurn")
            
            if (!isMyTurn) {
                Log.w("GameViewModel", "No es tu turno para poner banderas. Esperando al oponente.")
                return
            }
        }
        
        // Verificar que la celda pueda tener bandera
        val cell = currentState.board[row][col]
        if (!cell.canBeFlagged()) return
        
        // NO enviar mensaje antes de procesar. Procesamos primero y luego enviamos el resultado
        
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
                
                // DESPUÉS de actualizar el estado localmente, enviar el estado completo al oponente
                if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
                    Log.d("GameViewModel", "Enviando estado después de colocar bandera: turno=${_gameState.value.currentPlayer}")
                    val stateData = serializeGameState(_gameState.value)
                    bluetoothManager.sendMessage(
                        BluetoothMessage(MessageType.GAME_STATE_UPDATE, stateData)
                    )
                }
            } else {
                // Decrementar el contador cuando se quita una bandera
                repository.decrementFlagsPlaced()
                
                // Solo actualizar el tablero si se quitó una bandera (sin cambiar turno ni puntos)
                _gameState.value = currentState.copy(
                    board = updatedBoard,
                    placedFlags = placedFlags
                )
                
                // También enviar el estado cuando se quita una bandera
                if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
                    Log.d("GameViewModel", "Enviando estado después de quitar bandera")
                    val stateData = serializeGameState(_gameState.value)
                    bluetoothManager.sendMessage(
                        BluetoothMessage(MessageType.GAME_STATE_UPDATE, stateData)
                    )
                }
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
            
            // Resetear el flag de sincronización si está en modo Bluetooth
            if (_isBluetoothMode.value) {
                _boardSynced.value = false
            }
            
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
     * Exportar todas las partidas como JSON (devuelve el string vía callback)
     */
    fun exportAllGamesJson(onResult: (String) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val json = repository.exportAllGamesToJsonString()
                onResult(json)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }
    }

    /**
     * Importar partidas desde un JSON string (devuelve número insertado)
     */
    fun importGamesFromJsonString(jsonString: String, onComplete: (Int) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val inserted = repository.importGamesFromJsonString(jsonString)
                // Refrescar estadísticas
                loadStatistics()
                onComplete(inserted)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }
    }

    /**
     * Exporta todas las partidas a un archivo interno de la app
     */
    fun exportAllGamesToInternalFile(fileName: String, onResult: (Boolean) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val ok = repository.exportAllGamesToInternalFile(fileName)
                onResult(ok)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
            }
        }
    }

    /**
     * Importa partidas desde un archivo interno de la app
     */
    fun importGamesFromInternalFile(fileName: String, onComplete: (Int) -> Unit, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            try {
                val inserted = repository.importGamesFromInternalFile(fileName)
                loadStatistics()
                onComplete(inserted)
            } catch (e: Exception) {
                e.printStackTrace()
                onError(e)
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
     * Inicia búsqueda de dispositivos
     */
    fun startDiscovery() {
        bluetoothManager.startDiscovery()
    }
    
    /**
     * Detiene búsqueda de dispositivos
     */
    fun stopDiscovery() {
        bluetoothManager.stopDiscovery()
    }
    
    /**
     * Agrega un dispositivo descubierto
     */
    fun addDiscoveredDevice(device: BluetoothDevice) {
        bluetoothManager.addDiscoveredDevice(device)
    }
    
    /**
     * Notifica que la búsqueda finalizó
     */
    fun onDiscoveryFinished() {
        bluetoothManager.onDiscoveryFinished()
    }
    
    /**
     * Inicia el servidor Bluetooth (anfitrión)
     */
    fun startBluetoothServer() {
        _isBluetoothMode.value = true
        _boardSynced.value = false
        viewModelScope.launch {
            bluetoothManager.startServer()
        }
    }
    
    /**
     * Conecta a un dispositivo Bluetooth (invitado)
     */
    fun connectToDevice(device: BluetoothDevice) {
        _isBluetoothMode.value = true
        _boardSynced.value = false
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
        viewModelScope.launch(Dispatchers.Main) {  // IMPORTANTE: Ejecutar en Main thread
            when (message.type) {
                MessageType.BOARD_SYNC -> {
                    // Solo el cliente debe procesar este mensaje
                    if (!bluetoothManager.isHost.value) {
                        Log.d("GameViewModel", ">>> CLIENTE recibió BOARD_SYNC")
                        Log.d("GameViewModel", ">>> Data recibida: ${message.data}")
                        
                        deserializeBoardMines(message.data)
                        _boardSynced.value = true
                        
                        // Actualizar el estado con el tablero sincronizado
                        val currentState = _gameState.value
                        val updatedState = currentState.copy(
                            board = board.getBoard(),
                            isFirstMove = false
                        )
                        _gameState.value = updatedState
                        
                        Log.d("GameViewModel", ">>> Tablero del cliente actualizado - isFirstMove ahora: ${updatedState.isFirstMove}")
                        
                        // Guardar el estado
                        repository.saveCurrentGameToPreferences(updatedState)
                        
                        // Iniciar el temporizador solo si no se ha iniciado
                        val startTime = repository.getGameStartTime()
                        if (startTime <= 0) {
                            repository.saveGameStartTime()
                            startTimer()
                        }
                        
                        Log.d("GameViewModel", ">>> Tablero sincronizado exitosamente. Cliente listo. Turno=${updatedState.currentPlayer}")
                    } else {
                        Log.d("GameViewModel", ">>> HOST recibió BOARD_SYNC (ignorando - solo para cliente)")
                    }
                }
                MessageType.GAME_STATE_UPDATE -> {
                    Log.d("GameViewModel", ">>> Recibido GAME_STATE_UPDATE")
                    Log.d("GameViewModel", ">>> isHost: ${bluetoothManager.isHost.value}")
                    
                    // Aplicar el estado completo recibido del oponente directamente
                    deserializeGameState(message.data)?.let { receivedState ->
                        Log.d("GameViewModel", ">>> Estado deserializado:")
                        Log.d("GameViewModel", "    - Jugador actual: ${receivedState.currentPlayer}")
                        Log.d("GameViewModel", "    - P1: ${receivedState.player1.points}pts")
                        Log.d("GameViewModel", "    - P2: ${receivedState.player2.points}pts")
                        Log.d("GameViewModel", "    - isFirstMove: ${receivedState.isFirstMove}")
                        
                        // IMPORTANTE: Recrear el tablero interno completamente con el estado recibido
                        board = Board(boardRows, boardCols, minesCount)
                        
                        // Restaurar el tablero completo desde el estado recibido
                        board.restoreBoard(receivedState.board)
                        
                        // Asegurar que el tablero esté sincronizado si no es el primer movimiento
                        if (!receivedState.isFirstMove && !bluetoothManager.isHost.value) {
                            _boardSynced.value = true
                            Log.d("GameViewModel", ">>> boardSynced establecido a true")
                        }
                        
                        // Aplicar el estado completo recibido con el tablero actualizado
                        val finalState = receivedState.copy(
                            board = board.getBoard()
                        )
                        
                        // IMPORTANTE: Forzar actualización del StateFlow en el hilo principal
                        _gameState.value = finalState
                        
                        Log.d("GameViewModel", ">>> Estado local actualizado completamente")
                        Log.d("GameViewModel", ">>> Verificando tablero: ${finalState.board.size}x${finalState.board.firstOrNull()?.size ?: 0}")
                        
                        // Contar celdas reveladas para verificar
                        var revealedCount = 0
                        finalState.board.forEach { row ->
                            row.forEach { cell ->
                                if (cell.isRevealed) revealedCount++
                            }
                        }
                        Log.d("GameViewModel", ">>> Celdas reveladas en el estado: $revealedCount")
                        
                        // Guardar el estado sincronizado
                        repository.saveCurrentGameToPreferences(finalState)
                        
                        Log.d("GameViewModel", ">>> ACTUALIZACIÓN COMPLETA - Turno=${_gameState.value.currentPlayer}, boardSynced=${_boardSynced.value}")
                    } ?: run {
                        Log.e("GameViewModel", ">>> ERROR: No se pudo deserializar el estado recibido")
                    }
                }
                MessageType.CELL_CLICK -> {
                    // DEPRECATED: Ya no se usa, todo se maneja con GAME_STATE_UPDATE
                    // Se mantiene para compatibilidad pero no procesa nada
                }
                MessageType.CELL_LONG_CLICK -> {
                    // DEPRECATED: Ya no se usa, todo se maneja con GAME_STATE_UPDATE
                    // Se mantiene para compatibilidad pero no procesa nada
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
            
            // Si es el host en modo Bluetooth, enviar el tablero al cliente
            if (_isBluetoothMode.value && bluetoothManager.isHost.value && 
                connectionState.value == ConnectionState.CONNECTED) {
                val boardData = serializeBoardMines()
                bluetoothManager.sendMessage(
                    BluetoothMessage(MessageType.BOARD_SYNC, boardData)
                )
            }
            
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
            
            // DESPUÉS de actualizar el estado localmente, enviar el estado completo al oponente
            if (_isBluetoothMode.value && connectionState.value == ConnectionState.CONNECTED) {
                val stateData = serializeGameState(_gameState.value)
                bluetoothManager.sendMessage(
                    BluetoothMessage(MessageType.GAME_STATE_UPDATE, stateData)
                )
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
     * Activa el modo Bluetooth y resetea el juego para evitar usar el tablero offline
     */
    fun enableBluetoothMode() {
        _isBluetoothMode.value = true
        // Resetear el juego para empezar limpio en modo Bluetooth
        viewModelScope.launch {
            // Resetear el temporizador
            resetTimer()
            
            // Resetear el flag de sincronización
            _boardSynced.value = false
            
            val currentState = _gameState.value
            
            // Crear nuevo tablero limpio
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
     * Serializa las posiciones de las minas del tablero a un string
     * Formato: "row1,col1;row2,col2;..."
     */
    private fun serializeBoardMines(): String {
        val currentBoard = board.getBoard()
        val minePositions = mutableListOf<String>()
        
        for (row in currentBoard.indices) {
            for (col in currentBoard[row].indices) {
                if (currentBoard[row][col].isMine) {
                    minePositions.add("$row,$col")
                }
            }
        }
        
        return minePositions.joinToString(";")
    }
    
    /**
     * Deserializa las posiciones de las minas y las aplica al tablero
     */
    private fun deserializeBoardMines(data: String) {
        // Limpiar el tablero actual
        board = Board(boardRows, boardCols, minesCount)
        val cleanBoard = board.initialize()
        
        // Parsear las posiciones de las minas
        val minePositions = data.split(";").mapNotNull { position ->
            val coords = position.split(",")
            if (coords.size == 2) {
                val row = coords[0].toIntOrNull()
                val col = coords[1].toIntOrNull()
                if (row != null && col != null) row to col else null
            } else null
        }
        
        // Aplicar las minas manualmente al tablero
        val mutableBoard = cleanBoard.map { it.toMutableList() }.toMutableList()
        
        for ((row, col) in minePositions) {
            if (row in 0 until boardRows && col in 0 until boardCols) {
                mutableBoard[row][col] = mutableBoard[row][col].copy(isMine = true)
            }
        }
        
        // Calcular números adyacentes
        for (row in 0 until boardRows) {
            for (col in 0 until boardCols) {
                if (!mutableBoard[row][col].isMine) {
                    var adjacentCount = 0
                    for (dr in -1..1) {
                        for (dc in -1..1) {
                            if (dr == 0 && dc == 0) continue
                            val newRow = row + dr
                            val newCol = col + dc
                            if (newRow in 0 until boardRows && newCol in 0 until boardCols) {
                                if (mutableBoard[newRow][newCol].isMine) {
                                    adjacentCount++
                                }
                            }
                        }
                    }
                    mutableBoard[row][col] = mutableBoard[row][col].copy(adjacentMines = adjacentCount)
                }
            }
        }
        
        // Restaurar el tablero con las minas
        board.restoreBoard(mutableBoard.map { it.toList() })
    }
    
    /**
     * Serializa el estado completo del juego a JSON string
     * Incluye: tablero, jugadores, turno actual, puntos, flags, etc.
     */
    private fun serializeGameState(state: GameState): String {
        val boardData = state.board.map { row ->
            row.map { cell ->
                "${cell.isMine.toInt()},${cell.isRevealed.toInt()},${cell.isFlagged.toInt()},${cell.adjacentMines}"
            }.joinToString("|")
        }.joinToString(";")
        
        return listOf(
            boardData,
            "${state.player1.name}:${state.player1.points}:${state.player1.wins}",
            "${state.player2.name}:${state.player2.points}:${state.player2.wins}",
            state.currentPlayer.toString(),
            state.gameStatus.name,
            state.remainingCells.toString(),
            state.totalFlags.toString(),
            state.placedFlags.toString(),
            state.isFirstMove.toInt().toString(),
            state.lastRevealedBy.toString()
        ).joinToString("###")
    }
    
    /**
     * Deserializa el estado completo del juego desde JSON string
     */
    private fun deserializeGameState(data: String): GameState? {
        return try {
            val parts = data.split("###")
            if (parts.size < 10) return null
            
            // Parsear tablero
            val boardRows = parts[0].split(";").map { rowData ->
                rowData.split("|").map { cellData ->
                    val cellParts = cellData.split(",")
                    if (cellParts.size == 4) {
                        com.example.buscaminas.model.Cell(
                            isMine = cellParts[0].toInt() == 1,
                            isRevealed = cellParts[1].toInt() == 1,
                            isFlagged = cellParts[2].toInt() == 1,
                            adjacentMines = cellParts[3].toInt()
                        )
                    } else {
                        com.example.buscaminas.model.Cell()
                    }
                }
            }
            
            // Parsear jugador 1
            val player1Parts = parts[1].split(":")
            val player1 = Player(
                id = 1,
                name = player1Parts[0],
                points = player1Parts.getOrNull(1)?.toIntOrNull() ?: 0,
                wins = player1Parts.getOrNull(2)?.toIntOrNull() ?: 0,
                color = Color(0xFF2196F3)
            )
            
            // Parsear jugador 2
            val player2Parts = parts[2].split(":")
            val player2 = Player(
                id = 2,
                name = player2Parts[0],
                points = player2Parts.getOrNull(1)?.toIntOrNull() ?: 0,
                wins = player2Parts.getOrNull(2)?.toIntOrNull() ?: 0,
                color = Color(0xFFF44336)
            )
            
            GameState(
                board = boardRows,
                player1 = player1,
                player2 = player2,
                currentPlayer = parts[3].toIntOrNull() ?: 1,
                gameStatus = try { GameStatus.valueOf(parts[4]) } catch (e: Exception) { GameStatus.PLAYING },
                remainingCells = parts[5].toIntOrNull() ?: 0,
                totalFlags = parts[6].toIntOrNull() ?: 15,
                placedFlags = parts[7].toIntOrNull() ?: 0,
                isFirstMove = parts[8].toIntOrNull() == 1,
                lastRevealedBy = parts[9].toIntOrNull() ?: 0
            )
        } catch (e: Exception) {
            Log.e("GameViewModel", "Error deserializando estado del juego", e)
            null
        }
    }
    
    /**
     * Extensión para convertir Boolean a Int
     */
    private fun Boolean.toInt() = if (this) 1 else 0
    
    /**
     * Limpia recursos al destruir el ViewModel
     */
    override fun onCleared() {
        super.onCleared()
        bluetoothManager.disconnect()
    }
}
