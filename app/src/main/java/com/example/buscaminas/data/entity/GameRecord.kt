package com.example.buscaminas.data.entity

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.util.Date

/**
 * Entidad que representa un registro de partida en la base de datos
 */
@Entity(tableName = "game_records")
data class GameRecord(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    
    // Información de la partida
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long = 0, // Duración en segundos
    
    // Información de los jugadores
    val player1Name: String,
    val player2Name: String,
    
    // Puntuación
    val player1Points: Int,
    val player2Points: Int,
    
    // Resultado
    val winnerId: Int, // 1 = Jugador 1, 2 = Jugador 2, 0 = Empate, -1 = Game Over
    val gameStatus: String, // PLAYER1_WON, PLAYER2_WON, DRAW, GAME_OVER
    
    // Estadísticas del juego
    val totalCellsRevealed: Int,
    val totalFlagsPlaced: Int,
    val totalMines: Int,
    val boardRows: Int,
    val boardCols: Int,
    
    // Detalles adicionales
    val hitMine: Boolean = false, // Si alguien tocó una mina
    val completedBoard: Boolean = false // Si se completó el tablero
) {
    /**
     * Obtiene la fecha de la partida formateada
     */
    fun getFormattedDate(): String {
        val sdf = java.text.SimpleDateFormat("dd/MM/yyyy HH:mm:ss", java.util.Locale.getDefault())
        return sdf.format(Date(timestamp))
    }
    
    /**
     * Obtiene la duración formateada
     */
    fun getFormattedDuration(): String {
        val minutes = duration / 60
        val seconds = duration % 60
        return String.format("%02d:%02d", minutes, seconds)
    }
    
    /**
     * Obtiene el nombre del ganador
     */
    fun getWinnerName(): String {
        return when (winnerId) {
            1 -> player1Name
            2 -> player2Name
            0 -> "Empate"
            else -> "Sin ganador"
        }
    }
}
