package com.example.buscaminas.model

/**
 * Modo de tema: claro, oscuro o seguir sistema
 */
enum class ThemeMode(val displayName: String) {
    LIGHT("Claro"),
    DARK("Oscuro"),
    SYSTEM("Sistema");

    companion object {
        fun fromString(name: String): ThemeMode {
            return values().find { it.name == name } ?: SYSTEM
        }
    }
}
