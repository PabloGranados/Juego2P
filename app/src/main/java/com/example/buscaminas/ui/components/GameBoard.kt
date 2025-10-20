package com.example.buscaminas.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.example.buscaminas.model.Cell

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
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        board.forEachIndexed { rowIndex, row ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                modifier = Modifier.padding(vertical = 1.dp)
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
                        isAnimating = isAnimating
                    )
                }
            }
        }
    }
}
