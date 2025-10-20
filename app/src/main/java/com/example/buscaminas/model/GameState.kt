package com.example.buscaminas.model

/**
 * Representa el estado del juego
 */
enum class GameStatus {
    /** Juego en curso */
    PLAYING,
    
    /** Jugador 1 ganó */
    PLAYER1_WON,
    
    /** Jugador 2 ganó */
    PLAYER2_WON,
    
    /** Empate */
    DRAW,
    
    /** Juego perdido (mina revelada) */
    GAME_OVER
}

/**
 * Representa el estado completo del juego de Buscaminas
 *
 * @property board Matriz del tablero con las celdas
 * @property player1 Información del jugador 1
 * @property player2 Información del jugador 2
 * @property currentPlayer Jugador actual (1 o 2)
 * @property gameStatus Estado actual del juego
 * @property remainingCells Celdas seguras restantes por revelar
 * @property totalFlags Banderas totales disponibles (igual al número de minas)
 * @property placedFlags Número de banderas colocadas actualmente
 * @property isFirstMove Indica si es el primer movimiento del juego
 * @property lastRevealedBy ID del jugador que hizo la última revelación
 */
data class GameState(
    val board: List<List<Cell>> = emptyList(),
    val player1: Player,
    val player2: Player,
    val currentPlayer: Int = 1,
    val gameStatus: GameStatus = GameStatus.PLAYING,
    val remainingCells: Int = 0,
    val totalFlags: Int = 0,
    val placedFlags: Int = 0,
    val isFirstMove: Boolean = true,
    val lastRevealedBy: Int? = null
) {
    /**
     * Obtiene el jugador actual
     */
    fun getCurrentPlayerData(): Player = if (currentPlayer == 1) player1 else player2
    
    /**
     * Obtiene el jugador contrario
     */
    fun getOpponentPlayerData(): Player = if (currentPlayer == 1) player2 else player1
    
    /**
     * Alterna el turno al siguiente jugador
     */
    fun switchTurn(): GameState = copy(currentPlayer = if (currentPlayer == 1) 2 else 1)
    
    /**
     * Verifica si el juego ha terminado
     */
    fun isGameOver(): Boolean = gameStatus != GameStatus.PLAYING
    
    /**
     * Obtiene el ganador del juego
     */
    fun getWinner(): Player? = when (gameStatus) {
        GameStatus.PLAYER1_WON -> player1
        GameStatus.PLAYER2_WON -> player2
        else -> null
    }
}
