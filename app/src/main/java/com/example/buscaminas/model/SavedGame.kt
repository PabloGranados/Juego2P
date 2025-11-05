package com.example.buscaminas.model

import java.text.SimpleDateFormat
import java.util.*

/**
 * Representa una partida guardada con sus metadatos
 */
data class SavedGame(
    val id: String = UUID.randomUUID().toString(),
    val name: String,
    val format: FileFormat,
    val gameState: GameState,
    val timestamp: Long = System.currentTimeMillis(),
    val duration: Long, // en segundos
    val tags: List<String> = emptyList(),
    val description: String = ""
) {
    /**
     * Obtiene la fecha formateada
     */
    fun getFormattedDate(): String {
        val sdf = SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault())
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
     * Obtiene el nombre del archivo
     */
    fun getFileName(): String {
        return "${name}.${format.extension}"
    }
}
