package com.example.buscaminas.model

/**
 * Representa a un jugador en el juego
 *
 * @property id Identificador único del jugador (1 o 2)
 * @property name Nombre del jugador
 * @property color Color asociado al jugador
 * @property wins Número de victorias del jugador
 * @property points Puntos acumulados en la partida actual
 */
data class Player(
    val id: Int,
    val name: String,
    val color: androidx.compose.ui.graphics.Color,
    val wins: Int = 0,
    val points: Int = 0
) {
    /**
     * Crea una copia del jugador con puntos actualizados
     */
    fun addPoints(newPoints: Int): Player = copy(points = points + newPoints)
    
    /**
     * Crea una copia del jugador con victorias actualizadas
     */
    fun addWin(): Player = copy(wins = wins + 1)
    
    /**
     * Resetea los puntos del jugador para una nueva partida
     */
    fun resetPoints(): Player = copy(points = 0)
}
