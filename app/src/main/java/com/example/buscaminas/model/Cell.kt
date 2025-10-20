package com.example.buscaminas.model

/**
 * Representa una celda individual del tablero de Buscaminas
 *
 * @property isMine Indica si la celda contiene una mina
 * @property isRevealed Indica si la celda ha sido revelada
 * @property isFlagged Indica si la celda tiene una bandera
 * @property adjacentMines Número de minas adyacentes a esta celda (0-8)
 */
data class Cell(
    val isMine: Boolean = false,
    val isRevealed: Boolean = false,
    val isFlagged: Boolean = false,
    val adjacentMines: Int = 0
) {
    /**
     * Verifica si la celda puede ser revelada
     */
    fun canBeRevealed(): Boolean = !isRevealed && !isFlagged
    
    /**
     * Verifica si la celda puede tener una bandera
     */
    fun canBeFlagged(): Boolean = !isRevealed
}
