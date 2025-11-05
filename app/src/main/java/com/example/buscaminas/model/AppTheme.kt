package com.example.buscaminas.model

/**
 * Temas disponibles para la aplicación
 */
enum class AppTheme(val displayName: String) {
    GUINDA_IPN("Guinda IPN"),
    AZUL_ESCOM("Azul ESCOM");
    
    companion object {
        fun fromString(name: String): AppTheme {
            return values().find { it.name == name } ?: GUINDA_IPN
        }
    }
}
