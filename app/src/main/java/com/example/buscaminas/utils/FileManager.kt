package com.example.buscaminas.utils

import android.content.Context
import com.example.buscaminas.model.FileFormat
import com.example.buscaminas.model.GameState
import com.example.buscaminas.model.SavedGame
import org.json.JSONObject
import org.json.JSONArray
import java.io.File
import java.io.FileInputStream
import java.io.FileOutputStream
import java.util.UUID
import javax.xml.parsers.DocumentBuilderFactory
import javax.xml.transform.TransformerFactory
import javax.xml.transform.dom.DOMSource
import javax.xml.transform.stream.StreamResult
import org.w3c.dom.Document
import org.w3c.dom.Element

/**
 * Gestor de archivos para guardar y cargar partidas en diferentes formatos
 */
class FileManager(private val context: Context) {
    
    private val filesDir = context.filesDir
    
    /**
     * Guarda una partida en el formato especificado
     */
    fun saveGame(savedGame: SavedGame): Boolean {
        return try {
            val fileName = savedGame.getFileName()
            val file = File(filesDir, fileName)
            
            val content = when (savedGame.format) {
                FileFormat.TXT -> savedGameToText(savedGame)
                FileFormat.XML -> savedGameToXml(savedGame)
                FileFormat.JSON -> savedGameToJson(savedGame)
            }
            
            FileOutputStream(file).use { it.write(content.toByteArray()) }
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Carga una partida desde un archivo
     */
    fun loadGame(fileName: String): SavedGame? {
        return try {
            val file = File(filesDir, fileName)
            if (!file.exists()) return null
            
            val extension = file.extension
            val format = FileFormat.fromExtension(extension) ?: return null
            
            val content = FileInputStream(file).bufferedReader().use { it.readText() }
            
            when (format) {
                FileFormat.TXT -> textToSavedGame(content, fileName)
                FileFormat.XML -> xmlToSavedGame(content, fileName)
                FileFormat.JSON -> jsonToSavedGame(content, fileName)
            }
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Lista todos los archivos guardados
     */
    fun listSavedGames(): List<String> {
        return filesDir.listFiles()
            ?.filter { file ->
                val ext = file.extension
                ext == "txt" || ext == "xml" || ext == "json"
            }
            ?.map { it.name }
            ?: emptyList()
    }
    
    /**
     * Elimina un archivo guardado
     */
    fun deleteGame(fileName: String): Boolean {
        return try {
            val file = File(filesDir, fileName)
            file.delete()
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    /**
     * Obtiene el contenido de un archivo como texto
     */
    fun getFileContent(fileName: String): String {
        return try {
            val file = File(filesDir, fileName)
            FileInputStream(file).bufferedReader().use { it.readText() }
        } catch (e: Exception) {
            e.printStackTrace()
            "Error al leer archivo"
        }
    }
    
    /**
     * Exporta una partida al almacenamiento público
     */
    fun exportGame(fileName: String, destFile: File): Boolean {
        return try {
            val sourceFile = File(filesDir, fileName)
            sourceFile.copyTo(destFile, overwrite = true)
            true
        } catch (e: Exception) {
            e.printStackTrace()
            false
        }
    }
    
    // ==================== Conversiones a TEXTO PLANO ====================
    
    private fun savedGameToText(savedGame: SavedGame): String {
        val sb = StringBuilder()
        val game = savedGame.gameState
        
        sb.appendLine("=== PARTIDA GUARDADA ===")
        sb.appendLine("Nombre: ${savedGame.name}")
        sb.appendLine("Fecha: ${savedGame.getFormattedDate()}")
        sb.appendLine("Duración: ${savedGame.getFormattedDuration()}")
        sb.appendLine("Descripción: ${savedGame.description}")
        sb.appendLine("Etiquetas: ${savedGame.tags.joinToString(", ")}")
        sb.appendLine()
        sb.appendLine("=== ESTADO DEL JUEGO ===")
        sb.appendLine("Jugador 1: ${game.player1.name}")
        sb.appendLine("Puntos J1: ${game.player1.points}")
        sb.appendLine("Victorias J1: ${game.player1.wins}")
        sb.appendLine()
        sb.appendLine("Jugador 2: ${game.player2.name}")
        sb.appendLine("Puntos J2: ${game.player2.points}")
        sb.appendLine("Victorias J2: ${game.player2.wins}")
        sb.appendLine()
        sb.appendLine("Turno actual: Jugador ${game.currentPlayer}")
        sb.appendLine("Estado: ${game.gameStatus.name}")
        sb.appendLine("Celdas restantes: ${game.remainingCells}")
        sb.appendLine("Banderas colocadas: ${game.placedFlags} / ${game.totalFlags}")
        sb.appendLine("Primer movimiento: ${if (game.isFirstMove) "Sí" else "No"}")
        sb.appendLine()
        sb.appendLine("=== TABLERO ===")
        game.board.forEachIndexed { rowIndex, row ->
            row.forEachIndexed { colIndex, cell ->
                val symbol = when {
                    cell.isFlagged -> "F"
                    !cell.isRevealed -> "·"
                    cell.isMine -> "X"
                    cell.adjacentMines > 0 -> cell.adjacentMines.toString()
                    else -> " "
                }
                sb.append(symbol)
            }
            sb.appendLine()
        }
        
        return sb.toString()
    }
    
    private fun textToSavedGame(content: String, fileName: String): SavedGame {
        try {
            val lines = content.lines().filter { it.isNotBlank() }
            
            // Parsear metadatos
            val name = lines.find { it.startsWith("Nombre:") }
                ?.substringAfter("Nombre:")?.trim() ?: fileName.substringBeforeLast(".")
            
            val dateLine = lines.find { it.startsWith("Fecha:") }
                ?.substringAfter("Fecha:")?.trim() ?: ""
            val timestamp = System.currentTimeMillis()
            
            val durationLine = lines.find { it.startsWith("Duración:") }
                ?.substringAfter("Duración:")?.trim() ?: "0:00"
            val parts = durationLine.split(":")
            val duration = if (parts.size == 2) {
                val minutes = parts[0].replace(" min", "").trim().toLongOrNull() ?: 0L
                val seconds = parts[1].replace(" seg", "").trim().toLongOrNull() ?: 0L
                (minutes * 60) + seconds
            } else 0L
            
            val description = lines.find { it.startsWith("Descripción:") }
                ?.substringAfter("Descripción:")?.trim() ?: ""
            
            // Parsear tags
            val tagsLine = lines.find { it.startsWith("Etiquetas:") }
                ?.substringAfter("Etiquetas:")?.trim() ?: ""
            val tags = if (tagsLine.isNotEmpty()) {
                tagsLine.split(",").map { it.trim() }.filter { it.isNotEmpty() }
            } else emptyList()
            
            // Parsear jugador 1
            val player1Name = lines.find { it.startsWith("Jugador 1:") }
                ?.substringAfter("Jugador 1:")?.trim() ?: "Jugador 1"
            
            val player1Points = lines.find { it.startsWith("Puntos J1:") }
                ?.substringAfter("Puntos J1:")?.trim()?.toIntOrNull() ?: 0
            
            val player1Wins = lines.find { it.startsWith("Victorias J1:") }
                ?.substringAfter("Victorias J1:")?.trim()?.toIntOrNull() ?: 0
            
            // Parsear jugador 2
            val player2Name = lines.find { it.startsWith("Jugador 2:") }
                ?.substringAfter("Jugador 2:")?.trim() ?: "Jugador 2"
            
            val player2Points = lines.find { it.startsWith("Puntos J2:") }
                ?.substringAfter("Puntos J2:")?.trim()?.toIntOrNull() ?: 0
            
            val player2Wins = lines.find { it.startsWith("Victorias J2:") }
                ?.substringAfter("Victorias J2:")?.trim()?.toIntOrNull() ?: 0
            
            val player1 = com.example.buscaminas.model.Player(
                id = 1,
                name = player1Name,
                points = player1Points,
                wins = player1Wins,
                color = androidx.compose.ui.graphics.Color(0xFF2196F3)
            )
            
            val player2 = com.example.buscaminas.model.Player(
                id = 2,
                name = player2Name,
                points = player2Points,
                wins = player2Wins,
                color = androidx.compose.ui.graphics.Color(0xFFF44336)
            )
            
            // Parsear tablero
            val boardStartIndex = lines.indexOfFirst { it.startsWith("=== TABLERO ===") }
            val board = if (boardStartIndex >= 0 && boardStartIndex + 1 < lines.size) {
                parseTextBoardSimple(lines.subList(boardStartIndex + 1, lines.size))
            } else {
                createEmptyBoard()
            }
            
            // Parsear información del juego
            val currentPlayerLine = lines.find { it.startsWith("Turno actual:") }
                ?.substringAfter("Turno actual:")?.trim() ?: "Jugador 1"
            val currentPlayer = if (currentPlayerLine.contains("2")) 2 else 1
            
            val gameStatus = try {
                val statusLine = lines.find { it.startsWith("Estado:") }
                    ?.substringAfter("Estado:")?.trim() ?: "PLAYING"
                com.example.buscaminas.model.GameStatus.valueOf(statusLine)
            } catch (e: Exception) {
                com.example.buscaminas.model.GameStatus.PLAYING
            }
            
            val remainingCells = lines.find { it.startsWith("Celdas restantes:") }
                ?.substringAfter("Celdas restantes:")?.trim()?.toIntOrNull() ?: 0
            
            val flagsLine = lines.find { it.startsWith("Banderas colocadas:") }
                ?.substringAfter("Banderas colocadas:")?.trim() ?: "0 / 15"
            val flagsParts = flagsLine.split("/")
            val placedFlags = flagsParts.getOrNull(0)?.trim()?.toIntOrNull() ?: 0
            val totalFlags = flagsParts.getOrNull(1)?.trim()?.toIntOrNull() ?: 15
            
            val isFirstMove = lines.find { it.startsWith("Primer movimiento:") }
                ?.substringAfter("Primer movimiento:")?.trim()
                ?.equals("Sí", ignoreCase = true) ?: true
            
            val gameState = GameState(
                board = board,
                player1 = player1,
                player2 = player2,
                currentPlayer = currentPlayer,
                gameStatus = gameStatus,
                remainingCells = remainingCells,
                totalFlags = totalFlags,
                placedFlags = placedFlags,
                isFirstMove = isFirstMove
            )
            
            return SavedGame(
                id = java.util.UUID.randomUUID().toString(),
                name = name,
                format = FileFormat.TXT,
                gameState = gameState,
                timestamp = timestamp,
                duration = duration,
                tags = tags,
                description = description
            )
        } catch (e: Exception) {
            e.printStackTrace()
            println("❌ Error parseando TXT: ${e.message}")
            return SavedGame(
                name = fileName.substringBeforeLast("."),
                format = FileFormat.TXT,
                gameState = createEmptyGameState(),
                timestamp = System.currentTimeMillis(),
                duration = 0L
            )
        }
    }
    
    /**
     * Parsea el tablero desde formato texto simple (sin espacios ni corchetes)
     * Formato: cada línea es una fila, cada carácter es una celda
     * F = bandera, X = mina revelada, · = sin revelar, número = minas adyacentes, espacio = 0 minas
     */
    private fun parseTextBoardSimple(boardLines: List<String>): List<List<com.example.buscaminas.model.Cell>> {
        val board = mutableListOf<List<com.example.buscaminas.model.Cell>>()
        
        for (line in boardLines) {
            if (line.trim().isEmpty() || line.startsWith("===")) continue
            
            val row = line.map { char ->
                when (char) {
                    'F' -> com.example.buscaminas.model.Cell(
                        isMine = false,
                        isRevealed = false,
                        isFlagged = true,
                        adjacentMines = 0
                    )
                    'X' -> com.example.buscaminas.model.Cell(
                        isMine = true,
                        isRevealed = true,
                        isFlagged = false,
                        adjacentMines = 0
                    )
                    '·' -> com.example.buscaminas.model.Cell(
                        isMine = false,
                        isRevealed = false,
                        isFlagged = false,
                        adjacentMines = 0
                    )
                    ' ' -> com.example.buscaminas.model.Cell(
                        isMine = false,
                        isRevealed = true,
                        isFlagged = false,
                        adjacentMines = 0
                    )
                    in '0'..'8' -> com.example.buscaminas.model.Cell(
                        isMine = false,
                        isRevealed = true,
                        isFlagged = false,
                        adjacentMines = char.toString().toInt()
                    )
                    else -> com.example.buscaminas.model.Cell(
                        isMine = false,
                        isRevealed = false,
                        isFlagged = false,
                        adjacentMines = 0
                    )
                }
            }
            
            if (row.size == 10) { // Validar que tenga el tamaño correcto
                board.add(row)
            }
        }
        
        return if (board.size == 10) board else createEmptyBoard()
    }
    
    
    /**
     * Crea un tablero vacío de 10x10
     */
    private fun createEmptyBoard(): List<List<com.example.buscaminas.model.Cell>> {
        return List(10) { List(10) { com.example.buscaminas.model.Cell() } }
    }
    
    // ==================== Conversiones a XML ====================
    
    private fun savedGameToXml(savedGame: SavedGame): String {
        val docBuilder = DocumentBuilderFactory.newInstance().newDocumentBuilder()
        val doc: Document = docBuilder.newDocument()
        
        // Elemento raíz
        val root = doc.createElement("saved_game")
        doc.appendChild(root)
        
        // Metadatos
        root.appendChild(createElement(doc, "id", savedGame.id))
        root.appendChild(createElement(doc, "name", savedGame.name))
        root.appendChild(createElement(doc, "timestamp", savedGame.timestamp.toString()))
        root.appendChild(createElement(doc, "duration", savedGame.duration.toString()))
        root.appendChild(createElement(doc, "description", savedGame.description))
        
        // Etiquetas
        val tagsElement = doc.createElement("tags")
        savedGame.tags.forEach { tag ->
            tagsElement.appendChild(createElement(doc, "tag", tag))
        }
        root.appendChild(tagsElement)
        
        // Estado del juego
        val gameStateElement = doc.createElement("game_state")
        val game = savedGame.gameState
        
        // Jugador 1
        val player1Element = doc.createElement("player1")
        player1Element.appendChild(createElement(doc, "name", game.player1.name))
        player1Element.appendChild(createElement(doc, "points", game.player1.points.toString()))
        player1Element.appendChild(createElement(doc, "wins", game.player1.wins.toString()))
        gameStateElement.appendChild(player1Element)
        
        // Jugador 2
        val player2Element = doc.createElement("player2")
        player2Element.appendChild(createElement(doc, "name", game.player2.name))
        player2Element.appendChild(createElement(doc, "points", game.player2.points.toString()))
        player2Element.appendChild(createElement(doc, "wins", game.player2.wins.toString()))
        gameStateElement.appendChild(player2Element)
        
        // Info del juego
        gameStateElement.appendChild(createElement(doc, "current_player", game.currentPlayer.toString()))
        gameStateElement.appendChild(createElement(doc, "game_status", game.gameStatus.name))
        gameStateElement.appendChild(createElement(doc, "remaining_cells", game.remainingCells.toString()))
        gameStateElement.appendChild(createElement(doc, "placed_flags", game.placedFlags.toString()))
        gameStateElement.appendChild(createElement(doc, "total_flags", game.totalFlags.toString()))
        gameStateElement.appendChild(createElement(doc, "is_first_move", game.isFirstMove.toString()))
        
        // Tablero
        val boardElement = doc.createElement("board")
        game.board.forEachIndexed { rowIndex, row ->
            val rowElement = doc.createElement("row")
            rowElement.setAttribute("index", rowIndex.toString())
            
            row.forEachIndexed { colIndex, cell ->
                val cellElement = doc.createElement("cell")
                cellElement.setAttribute("col", colIndex.toString())
                cellElement.setAttribute("is_mine", cell.isMine.toString())
                cellElement.setAttribute("is_revealed", cell.isRevealed.toString())
                cellElement.setAttribute("is_flagged", cell.isFlagged.toString())
                cellElement.setAttribute("adjacent_mines", cell.adjacentMines.toString())
                rowElement.appendChild(cellElement)
            }
            
            boardElement.appendChild(rowElement)
        }
        gameStateElement.appendChild(boardElement)
        
        root.appendChild(gameStateElement)
        
        // Convertir a String
        val transformer = TransformerFactory.newInstance().newTransformer()
        transformer.setOutputProperty(javax.xml.transform.OutputKeys.INDENT, "yes")
        transformer.setOutputProperty("{http://xml.apache.org/xslt}indent-amount", "2")
        
        val writer = java.io.StringWriter()
        transformer.transform(DOMSource(doc), StreamResult(writer))
        return writer.toString()
    }
    
    private fun createElement(doc: Document, name: String, value: String): Element {
        val element = doc.createElement(name)
        element.appendChild(doc.createTextNode(value))
        return element
    }
    
    private fun xmlToSavedGame(content: String, fileName: String): SavedGame {
        try {
            val factory = DocumentBuilderFactory.newInstance()
            val builder = factory.newDocumentBuilder()
            val doc = builder.parse(java.io.ByteArrayInputStream(content.toByteArray()))
            doc.documentElement.normalize()
            
            val root = doc.documentElement
            
            // Parsear metadatos
            val id = getElementText(root, "id") ?: java.util.UUID.randomUUID().toString()
            val name = getElementText(root, "name") ?: fileName.substringBeforeLast(".")
            val timestamp = getElementText(root, "timestamp")?.toLongOrNull() ?: System.currentTimeMillis()
            val duration = getElementText(root, "duration")?.toLongOrNull() ?: 0L
            val description = getElementText(root, "description") ?: ""
            
            // Parsear etiquetas
            val tags = mutableListOf<String>()
            val tagsElement = root.getElementsByTagName("tags").item(0) as? Element
            if (tagsElement != null) {
                val tagNodes = tagsElement.getElementsByTagName("tag")
                for (i in 0 until tagNodes.length) {
                    val tagNode = tagNodes.item(i)
                    tags.add(tagNode.textContent)
                }
            }
            
            // Parsear estado del juego
            val gameStateElement = root.getElementsByTagName("game_state").item(0) as? Element
            val gameState = if (gameStateElement != null) {
                parseGameStateFromXml(gameStateElement)
            } else {
                createEmptyGameState()
            }
            
            return SavedGame(
                id = id,
                name = name,
                format = FileFormat.XML,
                gameState = gameState,
                timestamp = timestamp,
                duration = duration,
                tags = tags,
                description = description
            )
        } catch (e: Exception) {
            e.printStackTrace()
            return SavedGame(
                name = fileName.substringBeforeLast("."),
                format = FileFormat.XML,
                gameState = createEmptyGameState(),
                timestamp = System.currentTimeMillis(),
                duration = 0L
            )
        }
    }
    
    /**
     * Obtiene el texto de un elemento XML
     */
    private fun getElementText(parent: Element, tagName: String): String? {
        val nodeList = parent.getElementsByTagName(tagName)
        return if (nodeList.length > 0) {
            nodeList.item(0).textContent
        } else null
    }
    
    /**
     * Parsea el estado del juego desde XML
     */
    private fun parseGameStateFromXml(gameStateElement: Element): GameState {
        // Parsear jugador 1
        val player1Element = gameStateElement.getElementsByTagName("player1").item(0) as Element
        val player1 = com.example.buscaminas.model.Player(
            id = 1,
            name = getElementText(player1Element, "name") ?: "Jugador 1",
            points = getElementText(player1Element, "points")?.toIntOrNull() ?: 0,
            wins = getElementText(player1Element, "wins")?.toIntOrNull() ?: 0,
            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
        )
        
        // Parsear jugador 2
        val player2Element = gameStateElement.getElementsByTagName("player2").item(0) as Element
        val player2 = com.example.buscaminas.model.Player(
            id = 2,
            name = getElementText(player2Element, "name") ?: "Jugador 2",
            points = getElementText(player2Element, "points")?.toIntOrNull() ?: 0,
            wins = getElementText(player2Element, "wins")?.toIntOrNull() ?: 0,
            color = androidx.compose.ui.graphics.Color(0xFFF44336)
        )
        
        // Parsear tablero
        val boardElement = gameStateElement.getElementsByTagName("board").item(0) as Element
        val rowNodes = boardElement.getElementsByTagName("row")
        val board = mutableListOf<List<com.example.buscaminas.model.Cell>>()
        
        for (i in 0 until rowNodes.length) {
            val rowElement = rowNodes.item(i) as Element
            val cellNodes = rowElement.getElementsByTagName("cell")
            val row = mutableListOf<com.example.buscaminas.model.Cell>()
            
            for (j in 0 until cellNodes.length) {
                val cellElement = cellNodes.item(j) as Element
                val cell = com.example.buscaminas.model.Cell(
                    isMine = cellElement.getAttribute("is_mine").toBoolean(),
                    isRevealed = cellElement.getAttribute("is_revealed").toBoolean(),
                    isFlagged = cellElement.getAttribute("is_flagged").toBoolean(),
                    adjacentMines = cellElement.getAttribute("adjacent_mines").toIntOrNull() ?: 0
                )
                row.add(cell)
            }
            
            board.add(row)
        }
        
        // Parsear información del juego
        val currentPlayer = getElementText(gameStateElement, "current_player")?.toIntOrNull() ?: 1
        val gameStatus = try {
            com.example.buscaminas.model.GameStatus.valueOf(
                getElementText(gameStateElement, "game_status") ?: "PLAYING"
            )
        } catch (e: Exception) {
            com.example.buscaminas.model.GameStatus.PLAYING
        }
        val remainingCells = getElementText(gameStateElement, "remaining_cells")?.toIntOrNull() ?: 0
        val placedFlags = getElementText(gameStateElement, "placed_flags")?.toIntOrNull() ?: 0
        val totalFlags = getElementText(gameStateElement, "total_flags")?.toIntOrNull() ?: 0
        val isFirstMove = getElementText(gameStateElement, "is_first_move")?.toBoolean() ?: true
        
        return GameState(
            board = board,
            player1 = player1,
            player2 = player2,
            currentPlayer = currentPlayer,
            gameStatus = gameStatus,
            remainingCells = remainingCells,
            totalFlags = totalFlags,
            placedFlags = placedFlags,
            isFirstMove = isFirstMove
        )
    }
    
    // ==================== Conversiones a JSON ====================
    
    private fun savedGameToJson(savedGame: SavedGame): String {
        val json = JSONObject()
        val game = savedGame.gameState
        
        // Metadatos
        json.put("id", savedGame.id)
        json.put("name", savedGame.name)
        json.put("timestamp", savedGame.timestamp)
        json.put("duration", savedGame.duration)
        json.put("description", savedGame.description)
        json.put("tags", JSONArray(savedGame.tags))
        
        // Estado del juego
        val gameStateJson = JSONObject()
        
        // Jugadores
        val player1Json = JSONObject().apply {
            put("name", game.player1.name)
            put("points", game.player1.points)
            put("wins", game.player1.wins)
        }
        val player2Json = JSONObject().apply {
            put("name", game.player2.name)
            put("points", game.player2.points)
            put("wins", game.player2.wins)
        }
        
        gameStateJson.put("player1", player1Json)
        gameStateJson.put("player2", player2Json)
        gameStateJson.put("current_player", game.currentPlayer)
        gameStateJson.put("game_status", game.gameStatus.name)
        gameStateJson.put("remaining_cells", game.remainingCells)
        gameStateJson.put("placed_flags", game.placedFlags)
        gameStateJson.put("total_flags", game.totalFlags)
        gameStateJson.put("is_first_move", game.isFirstMove)
        
        // Tablero
        val boardJson = JSONArray()
        game.board.forEach { row ->
            val rowJson = JSONArray()
            row.forEach { cell ->
                val cellJson = JSONObject().apply {
                    put("is_mine", cell.isMine)
                    put("is_revealed", cell.isRevealed)
                    put("is_flagged", cell.isFlagged)
                    put("adjacent_mines", cell.adjacentMines)
                }
                rowJson.put(cellJson)
            }
            boardJson.put(rowJson)
        }
        gameStateJson.put("board", boardJson)
        
        json.put("game_state", gameStateJson)
        
        return json.toString(2) // Formato con indentación
    }
    
    private fun jsonToSavedGame(content: String, fileName: String): SavedGame {
        val json = JSONObject(content)
        
        val id = json.optString("id", java.util.UUID.randomUUID().toString())
        val name = json.optString("name", fileName.substringBeforeLast("."))
        val timestamp = json.optLong("timestamp", System.currentTimeMillis())
        val duration = json.optLong("duration", 0L)
        val description = json.optString("description", "")
        
        val tagsArray = json.optJSONArray("tags")
        val tags = mutableListOf<String>()
        if (tagsArray != null) {
            for (i in 0 until tagsArray.length()) {
                tags.add(tagsArray.getString(i))
            }
        }
        
        // Parsear el estado del juego completo
        val gameStateJson = json.optJSONObject("game_state")
        val gameState = if (gameStateJson != null) {
            parseGameStateFromJson(gameStateJson)
        } else {
            createEmptyGameState()
        }
        
        return SavedGame(
            id = id,
            name = name,
            format = FileFormat.JSON,
            gameState = gameState,
            timestamp = timestamp,
            duration = duration,
            tags = tags,
            description = description
        )
    }
    
    /**
     * Parsea el estado del juego desde JSON
     */
    private fun parseGameStateFromJson(gameStateJson: JSONObject): GameState {
        // Parsear jugadores
        val player1Json = gameStateJson.getJSONObject("player1")
        val player1 = com.example.buscaminas.model.Player(
            id = 1,
            name = player1Json.getString("name"),
            points = player1Json.getInt("points"),
            wins = player1Json.getInt("wins"),
            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
        )
        
        val player2Json = gameStateJson.getJSONObject("player2")
        val player2 = com.example.buscaminas.model.Player(
            id = 2,
            name = player2Json.getString("name"),
            points = player2Json.getInt("points"),
            wins = player2Json.getInt("wins"),
            color = androidx.compose.ui.graphics.Color(0xFFF44336)
        )
        
        // Parsear tablero
        val boardJson = gameStateJson.getJSONArray("board")
        val board = mutableListOf<List<com.example.buscaminas.model.Cell>>()
        
        for (i in 0 until boardJson.length()) {
            val rowJson = boardJson.getJSONArray(i)
            val row = mutableListOf<com.example.buscaminas.model.Cell>()
            
            for (j in 0 until rowJson.length()) {
                val cellJson = rowJson.getJSONObject(j)
                val cell = com.example.buscaminas.model.Cell(
                    isMine = cellJson.getBoolean("is_mine"),
                    isRevealed = cellJson.getBoolean("is_revealed"),
                    isFlagged = cellJson.getBoolean("is_flagged"),
                    adjacentMines = cellJson.getInt("adjacent_mines")
                )
                row.add(cell)
            }
            
            board.add(row)
        }
        
        // Parsear información del juego
        val currentPlayer = gameStateJson.getInt("current_player")
        val gameStatus = try {
            com.example.buscaminas.model.GameStatus.valueOf(gameStateJson.getString("game_status"))
        } catch (e: Exception) {
            com.example.buscaminas.model.GameStatus.PLAYING
        }
        val remainingCells = gameStateJson.getInt("remaining_cells")
        val placedFlags = gameStateJson.getInt("placed_flags")
        val totalFlags = gameStateJson.getInt("total_flags")
        val isFirstMove = gameStateJson.getBoolean("is_first_move")
        
        return GameState(
            board = board,
            player1 = player1,
            player2 = player2,
            currentPlayer = currentPlayer,
            gameStatus = gameStatus,
            remainingCells = remainingCells,
            totalFlags = totalFlags,
            placedFlags = placedFlags,
            isFirstMove = isFirstMove
        )
    }
    
    // ==================== Helpers ====================
    
    private fun createEmptyGameState(): GameState {
        // Importar las clases necesarias
        val player1 = com.example.buscaminas.model.Player(
            id = 1,
            name = "Jugador 1",
            color = androidx.compose.ui.graphics.Color(0xFF2196F3)
        )
        
        val player2 = com.example.buscaminas.model.Player(
            id = 2,
            name = "Jugador 2",
            color = androidx.compose.ui.graphics.Color(0xFFF44336)
        )
        
        return GameState(
            board = emptyList(),
            player1 = player1,
            player2 = player2,
            currentPlayer = 1,
            gameStatus = com.example.buscaminas.model.GameStatus.PLAYING,
            remainingCells = 0,
            totalFlags = 0,
            placedFlags = 0,
            isFirstMove = true
        )
    }
}
