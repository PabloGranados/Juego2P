package com.example.buscaminas.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.model.Cell

/**
 * Componente que representa una celda individual del tablero
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun CellView(
    cell: Cell,
    row: Int,
    col: Int,
    onCellClick: (Int, Int) -> Unit,
    onCellLongClick: (Int, Int) -> Unit,
    isAnimating: Boolean = false,
    modifier: Modifier = Modifier
) {
    // Animación de escala
    val scale by animateFloatAsState(
        targetValue = if (isAnimating) 1.2f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "cell_scale"
    )
    
    Box(
        modifier = modifier
            .size(35.dp)
            .scale(scale)
            .clip(RoundedCornerShape(4.dp))
            .background(getCellBackgroundColor(cell))
            .border(
                width = 1.dp,
                color = getCellBorderColor(cell),
                shape = RoundedCornerShape(4.dp)
            )
            .combinedClickable(
                onClick = { onCellClick(row, col) },
                onLongClick = { onCellLongClick(row, col) }
            ),
        contentAlignment = Alignment.Center
    ) {
        CellContent(cell)
    }
}

/**
 * Contenido de la celda según su estado
 */
@Composable
private fun CellContent(cell: Cell) {
    when {
        cell.isFlagged -> {
            // Mostrar bandera
            Text(
                text = "🚩",
                fontSize = 20.sp
            )
        }
        cell.isRevealed -> {
            if (cell.isMine) {
                // Mostrar mina
                Text(
                    text = "💣",
                    fontSize = 20.sp
                )
            } else if (cell.adjacentMines > 0) {
                // Mostrar número de minas adyacentes
                Text(
                    text = cell.adjacentMines.toString(),
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = getNumberColor(cell.adjacentMines)
                )
            }
            // Si es 0, no mostrar nada
        }
    }
}

/**
 * Obtiene el color de fondo de la celda según su estado
 */
private fun getCellBackgroundColor(cell: Cell): Color {
    return when {
        cell.isRevealed && cell.isMine -> Color(0xFFE57373) // Rojo claro para mina
        cell.isRevealed -> Color(0xFFE0E0E0) // Gris claro para revelada
        else -> Color(0xFF90CAF9) // Azul claro para no revelada
    }
}

/**
 * Obtiene el color del borde de la celda
 */
private fun getCellBorderColor(cell: Cell): Color {
    return if (cell.isRevealed) {
        Color(0xFFBDBDBD) // Gris medio
    } else {
        Color(0xFF1976D2) // Azul oscuro
    }
}

/**
 * Obtiene el color del número según la cantidad de minas adyacentes
 */
private fun getNumberColor(number: Int): Color {
    return when (number) {
        1 -> Color(0xFF2196F3) // Azul
        2 -> Color(0xFF4CAF50) // Verde
        3 -> Color(0xFFF44336) // Rojo
        4 -> Color(0xFF9C27B0) // Púrpura
        5 -> Color(0xFFFF9800) // Naranja
        6 -> Color(0xFF00BCD4) // Cian
        7 -> Color(0xFF000000) // Negro
        8 -> Color(0xFF795548) // Marrón
        else -> Color.Black
    }
}
