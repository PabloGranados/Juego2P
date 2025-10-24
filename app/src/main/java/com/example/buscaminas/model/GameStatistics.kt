package com.example.buscaminas.model

/**
 * Representa las estadísticas completas del juego
 */
data class GameStatistics(
    // Estadísticas generales
    val totalGames: Int = 0,
    val totalCompletedGames: Int = 0,
    val totalGameOvers: Int = 0,
    val totalDraws: Int = 0,
    
    // Estadísticas de jugadores
    val player1Stats: PlayerStatistics = PlayerStatistics(),
    val player2Stats: PlayerStatistics = PlayerStatistics(),
    
    // Estadísticas de tiempo
    val averageDuration: Double = 0.0,
    val longestGame: String = "00:00",
    val shortestGame: String = "00:00",
    
    // Estadísticas de juego
    val totalCellsRevealed: Int = 0,
    val totalFlagsPlaced: Int = 0,
    
    // Última partida
    val lastGameDate: String = "Sin partidas",
    val lastGameWinner: String = "N/A"
) {
    /**
     * Obtiene el porcentaje de partidas completadas
     */
    fun getCompletionRate(): Float {
        return if (totalGames > 0) {
            (totalCompletedGames.toFloat() / totalGames.toFloat()) * 100
        } else 0f
    }
    
    /**
     * Obtiene el porcentaje de game overs
     */
    fun getGameOverRate(): Float {
        return if (totalGames > 0) {
            (totalGameOvers.toFloat() / totalGames.toFloat()) * 100
        } else 0f
    }
    
    /**
     * Obtiene la duración promedio formateada
     */
    fun getFormattedAverageDuration(): String {
        val minutes = (averageDuration / 60).toInt()
        val seconds = (averageDuration % 60).toInt()
        return String.format("%02d:%02d", minutes, seconds)
    }
}

/**
 * Representa las estadísticas de un jugador específico
 */
data class PlayerStatistics(
    val name: String = "Jugador",
    val totalWins: Int = 0,
    val averagePoints: Double = 0.0,
    val maxPoints: Int = 0,
    val winRate: Float = 0f
) {
    /**
     * Obtiene el promedio de puntos formateado
     */
    fun getFormattedAveragePoints(): String {
        return String.format("%.1f", averagePoints)
    }
    
    /**
     * Obtiene la tasa de victoria formateada
     */
    fun getFormattedWinRate(): String {
        return String.format("%.1f%%", winRate)
    }
}
