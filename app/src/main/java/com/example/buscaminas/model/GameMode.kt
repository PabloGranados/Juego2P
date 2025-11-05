package com.example.buscaminas.model

/**
 * Enumeración de los modos de juego disponibles
 */
enum class GameMode {
    TWO_PLAYERS,  // Modo 2 jugadores (actual)
    VS_AI         // Modo contra IA
}

/**
 * Enumeración de las dificultades/tamaños de tablero
 */
enum class GameDifficulty(
    val displayName: String,
    val rows: Int,
    val cols: Int,
    val mines: Int
) {
    EASY("Fácil (8x8)", 8, 8, 10),
    MEDIUM("Normal (10x10)", 10, 10, 15),
    HARD("Difícil (12x12)", 12, 12, 20),
    EXPERT("Experto (14x14)", 14, 14, 30)
}

/**
 * Configuración del juego
 */
data class GameConfig(
    val mode: GameMode = GameMode.TWO_PLAYERS,
    val difficulty: GameDifficulty = GameDifficulty.MEDIUM
)
