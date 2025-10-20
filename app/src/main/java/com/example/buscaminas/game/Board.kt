package com.example.buscaminas.game

import com.example.buscaminas.model.Cell
import kotlin.random.Random

/**
 * Clase que maneja la lógica del tablero de Buscaminas
 *
 * @property rows Número de filas del tablero
 * @property cols Número de columnas del tablero
 * @property minesCount Número de minas en el tablero
 */
class Board(
    private val rows: Int = 10,
    private val cols: Int = 10,
    private val minesCount: Int = 15
) {
    private var board: MutableList<MutableList<Cell>> = mutableListOf()
    private val directions = listOf(
        -1 to -1, -1 to 0, -1 to 1,
        0 to -1,           0 to 1,
        1 to -1,  1 to 0,  1 to 1
    )
    
    /**
     * Inicializa el tablero con celdas vacías
     */
    fun initialize(): List<List<Cell>> {
        board = MutableList(rows) { MutableList(cols) { Cell() } }
        return board.map { it.toList() }
    }
    
    /**
     * Genera las minas en el tablero, evitando la posición del primer clic
     *
     * @param firstClickRow Fila del primer clic
     * @param firstClickCol Columna del primer clic
     */
    fun generateMines(firstClickRow: Int, firstClickCol: Int) {
        val availablePositions = mutableListOf<Pair<Int, Int>>()
        
        // Recolectar todas las posiciones disponibles excepto el primer clic y sus adyacentes
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!isAdjacentToFirstClick(row, col, firstClickRow, firstClickCol)) {
                    availablePositions.add(row to col)
                }
            }
        }
        
        // Colocar minas aleatoriamente
        val selectedPositions = availablePositions.shuffled().take(minesCount)
        for ((row, col) in selectedPositions) {
            board[row][col] = board[row][col].copy(isMine = true)
        }
        
        // Calcular números adyacentes
        calculateAdjacentMines()
    }
    
    /**
     * Verifica si una posición es adyacente al primer clic
     */
    private fun isAdjacentToFirstClick(
        row: Int,
        col: Int,
        firstRow: Int,
        firstCol: Int
    ): Boolean {
        val rowDiff = kotlin.math.abs(row - firstRow)
        val colDiff = kotlin.math.abs(col - firstCol)
        return rowDiff <= 1 && colDiff <= 1
    }
    
    /**
     * Calcula el número de minas adyacentes para cada celda
     */
    private fun calculateAdjacentMines() {
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!board[row][col].isMine) {
                    val count = countAdjacentMines(row, col)
                    board[row][col] = board[row][col].copy(adjacentMines = count)
                }
            }
        }
    }
    
    /**
     * Cuenta el número de minas adyacentes a una celda
     */
    private fun countAdjacentMines(row: Int, col: Int): Int {
        var count = 0
        for ((dr, dc) in directions) {
            val newRow = row + dr
            val newCol = col + dc
            if (isValidPosition(newRow, newCol) && board[newRow][newCol].isMine) {
                count++
            }
        }
        return count
    }
    
    /**
     * Verifica si una posición es válida en el tablero
     */
    private fun isValidPosition(row: Int, col: Int): Boolean {
        return row in 0 until rows && col in 0 until cols
    }
    
    /**
     * Revela una celda y retorna la lista de celdas reveladas
     *
     * @return Pair de (lista de posiciones reveladas, si tocó una mina)
     */
    fun revealCell(row: Int, col: Int): Pair<List<Pair<Int, Int>>, Boolean> {
        if (!isValidPosition(row, col) || !board[row][col].canBeRevealed()) {
            return emptyList<Pair<Int, Int>>() to false
        }
        
        val cell = board[row][col]
        
        // Si es una mina, revelar solo esa celda
        if (cell.isMine) {
            board[row][col] = cell.copy(isRevealed = true)
            return listOf(row to col) to true
        }
        
        // Revelar usando flood fill
        val revealed = mutableListOf<Pair<Int, Int>>()
        floodFill(row, col, revealed)
        return revealed to false
    }
    
    /**
     * Algoritmo de flood fill para revelar celdas vacías adyacentes
     */
    private fun floodFill(row: Int, col: Int, revealed: MutableList<Pair<Int, Int>>) {
        if (!isValidPosition(row, col) || !board[row][col].canBeRevealed()) {
            return
        }
        
        val cell = board[row][col]
        board[row][col] = cell.copy(isRevealed = true)
        revealed.add(row to col)
        
        // Si la celda tiene minas adyacentes, no continuar el flood fill
        if (cell.adjacentMines > 0) {
            return
        }
        
        // Revelar celdas adyacentes recursivamente
        for ((dr, dc) in directions) {
            floodFill(row + dr, col + dc, revealed)
        }
    }
    
    /**
     * Alterna el estado de bandera de una celda
     *
     * @return true si se colocó una bandera, false si se quitó
     */
    fun toggleFlag(row: Int, col: Int): Boolean? {
        if (!isValidPosition(row, col) || !board[row][col].canBeFlagged()) {
            return null
        }
        
        val cell = board[row][col]
        val newFlagState = !cell.isFlagged
        board[row][col] = cell.copy(isFlagged = newFlagState)
        return newFlagState
    }
    
    /**
     * Obtiene el tablero actual
     */
    fun getBoard(): List<List<Cell>> = board.map { it.toList() }
    
    /**
     * Cuenta las celdas seguras restantes (no reveladas y sin minas)
     */
    fun countRemainingCells(): Int {
        var count = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (!board[row][col].isMine && !board[row][col].isRevealed) {
                    count++
                }
            }
        }
        return count
    }
    
    /**
     * Cuenta el número de banderas colocadas
     */
    fun countPlacedFlags(): Int {
        var count = 0
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (board[row][col].isFlagged) {
                    count++
                }
            }
        }
        return count
    }
    
    /**
     * Revela todas las minas (usado al final del juego)
     */
    fun revealAllMines(): List<Pair<Int, Int>> {
        val minePositions = mutableListOf<Pair<Int, Int>>()
        for (row in 0 until rows) {
            for (col in 0 until cols) {
                if (board[row][col].isMine && !board[row][col].isRevealed) {
                    board[row][col] = board[row][col].copy(isRevealed = true)
                    minePositions.add(row to col)
                }
            }
        }
        return minePositions
    }
    
    /**
     * Obtiene el número de filas del tablero
     */
    fun getRows(): Int = rows
    
    /**
     * Obtiene el número de columnas del tablero
     */
    fun getCols(): Int = cols
    
    /**
     * Obtiene el número de minas en el tablero
     */
    fun getMinesCount(): Int = minesCount
}
