package com.example.buscaminas.viewmodel

import androidx.compose.ui.graphics.Color
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.buscaminas.game.Board
import com.example.buscaminas.model.GameState
import com.example.buscaminas.model.GameStatus
import com.example.buscaminas.model.Player
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

/**
 * ViewModel que maneja la lógica del juego de Buscaminas
 */
class GameViewModel : ViewModel() {
    
    // Configuración del juego
    private val boardRows = 10
    private val boardCols = 10
    private val minesCount = 15
    
    // Puntos por acción
    private val pointsPerCell = 10
    private val pointsPerFlag = 5
    
    // Instancia del tablero
    private var board = Board(boardRows, boardCols, minesCount)
    
    // Estado del juego
    private val _gameState = MutableStateFlow(createInitialGameState())
    val gameState: StateFlow<GameState> = _gameState.asStateFlow()
    
    // Animación de la última celda revelada
    private val _lastAction = MutableStateFlow<Pair<Int, Int>?>(null)
    val lastAction: StateFlow<Pair<Int, Int>?> = _lastAction.asStateFlow()
    
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
        
        // Verificar que la celda pueda ser revelada
        val cell = currentState.board[row][col]
        if (!cell.canBeRevealed()) return
        
        viewModelScope.launch {
            // Si es el primer movimiento, generar las minas
            if (currentState.isFirstMove) {
                board.generateMines(row, col)
                _gameState.value = currentState.copy(
                    board = board.getBoard(),
                    isFirstMove = false
                )
            }
            
            // Revelar la celda
            val (revealedCells, hitMine) = board.revealCell(row, col)
            
            if (revealedCells.isEmpty()) return@launch
            
            // Actualizar el estado del tablero
            val updatedBoard = board.getBoard()
            val remainingCells = board.countRemainingCells()
            
            // Animación de la última acción
            _lastAction.value = row to col
            
            if (hitMine) {
                // El jugador perdió - revelar todas las minas
                board.revealAllMines()
                handleGameOver()
            } else {
                // Calcular puntos por las celdas reveladas
                val earnedPoints = revealedCells.size * pointsPerCell
                val currentPlayer = currentState.getCurrentPlayerData()
                val updatedPlayer = currentPlayer.addPoints(earnedPoints)
                
                // Actualizar el estado
                val newState = if (currentState.currentPlayer == 1) {
                    currentState.copy(
                        board = updatedBoard,
                        player1 = updatedPlayer,
                        remainingCells = remainingCells,
                        lastRevealedBy = currentState.currentPlayer
                    )
                } else {
                    currentState.copy(
                        board = updatedBoard,
                        player2 = updatedPlayer,
                        remainingCells = remainingCells,
                        lastRevealedBy = currentState.currentPlayer
                    )
                }
                
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
        if (currentState.isFirstMove) return
        
        // Verificar que la celda pueda tener bandera
        val cell = currentState.board[row][col]
        if (!cell.canBeFlagged()) return
        
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
                
                // Cambiar de turno
                _gameState.value = newState.switchTurn()
            } else {
                // Solo actualizar el tablero si se quitó una bandera (sin cambiar turno)
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
    }
    
    /**
     * Maneja la victoria cuando se revelan todas las celdas seguras
     */
    private fun handleVictory(state: GameState) {
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
        
        _gameState.value = state.copy(
            player1 = updatedPlayer1,
            player2 = updatedPlayer2,
            gameStatus = gameStatus
        )
    }
    
    /**
     * Reinicia el juego manteniendo las victorias
     */
    fun resetGame() {
        viewModelScope.launch {
            val currentState = _gameState.value
            
            // Crear nuevo tablero
            board = Board(boardRows, boardCols, minesCount)
            val initialBoard = board.initialize()
            
            // Resetear puntos pero mantener victorias
            val resetPlayer1 = currentState.player1.resetPoints()
            val resetPlayer2 = currentState.player2.resetPoints()
            
            _gameState.value = GameState(
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
            
            _lastAction.value = null
        }
    }
    
    /**
     * Limpia la última acción (para animaciones)
     */
    fun clearLastAction() {
        _lastAction.value = null
    }
}
