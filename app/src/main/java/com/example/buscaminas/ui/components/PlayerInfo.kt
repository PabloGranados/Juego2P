package com.example.buscaminas.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.buscaminas.model.Player

/**
 * Componente que muestra la información de un jugador
 */
@Composable
fun PlayerInfo(
    player: Player,
    isCurrentPlayer: Boolean,
    modifier: Modifier = Modifier
) {
    val borderWidth = if (isCurrentPlayer) 3.dp else 1.dp
    val borderColor = if (isCurrentPlayer) player.color else Color.Gray
    
    Column(
        modifier = modifier
            .clip(RoundedCornerShape(12.dp))
            .background(Color.White)
            .border(
                width = borderWidth,
                color = borderColor,
                shape = RoundedCornerShape(12.dp)
            )
            .padding(12.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        // Nombre del jugador
        Text(
            text = player.name,
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = player.color
        )
        
        // Indicador de turno
        if (isCurrentPlayer) {
            Text(
                text = "🎯 Tu turno",
                fontSize = 12.sp,
                color = Color(0xFF4CAF50),
                fontWeight = FontWeight.SemiBold
            )
        }
        
        Spacer(modifier = Modifier.height(4.dp))
        
        // Puntos actuales
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Puntos:",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "${player.points}",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = player.color
            )
        }
        
        // Victorias
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "Victorias:",
                fontSize = 14.sp,
                color = Color.Gray
            )
            Text(
                text = "${player.wins}",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = Color.Black
            )
        }
    }
}
