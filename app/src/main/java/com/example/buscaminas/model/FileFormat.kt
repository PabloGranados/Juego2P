package com.example.buscaminas.model

/**
 * Formatos de archivo soportados para guardar partidas
 */
enum class FileFormat(val extension: String, val mimeType: String, val displayName: String) {
    TXT("txt", "text/plain", "Texto Plano"),
    XML("xml", "text/xml", "XML"),
    JSON("json", "application/json", "JSON");
    
    companion object {
        fun fromExtension(extension: String): FileFormat? {
            return values().find { it.extension.equals(extension, ignoreCase = true) }
        }
    }
}
