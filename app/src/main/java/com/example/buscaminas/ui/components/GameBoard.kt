package com.example.buscaminas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.dp
import com.example.buscaminas.model.Cell
import kotlin.math.min

/**
 * Componente que representa el tablero completo del juego
 */
@Composable
fun GameBoard(
    board: List<List<Cell>>,
    onCellClick: (Int, Int) -> Unit,
    onCellLongClick: (Int, Int) -> Unit,
    lastAction: Pair<Int, Int>?,
    modifier: Modifier = Modifier
) {
    // Obtener el tamaño de pantalla disponible
    val configuration = LocalConfiguration.current
    val screenWidth = configuration.screenWidthDp.dp
    
    // Calcular el tamaño de cada celda dinámicamente
    // Restamos el padding y dividimos por el número de columnas
    val rows = board.size
    val cols = if (rows > 0) board[0].size else 0
    
    // Tamaño disponible considerando padding y espacios entre celdas
    val availableWidth = screenWidth - 64.dp // padding lateral
    val spacing = 2.dp
    val totalSpacing = spacing * (cols - 1)
    val cellSize = ((availableWidth - totalSpacing) / cols).coerceIn(28.dp, 45.dp)
    
    BoxWithConstraints(
        modifier = modifier.fillMaxWidth(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing)
        ) {
            board.forEachIndexed { rowIndex, row ->
                Row(
                    horizontalArrangement = Arrangement.spacedBy(spacing)
                ) {
                    row.forEachIndexed { colIndex, cell ->
                        val isAnimating = lastAction?.let { 
                            it.first == rowIndex && it.second == colIndex 
                        } ?: false
                        
                        CellView(
                            cell = cell,
                            row = rowIndex,
                            col = colIndex,
                            onCellClick = onCellClick,
                            onCellLongClick = onCellLongClick,
                            isAnimating = isAnimating,
                            cellSize = cellSize
                        )
                    }
                }
            }
        }
    }
}
