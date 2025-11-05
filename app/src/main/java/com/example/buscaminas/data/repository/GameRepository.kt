package com.example.buscaminas.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.example.buscaminas.data.dao.GameDao
import com.example.buscaminas.data.database.GameDatabase
import com.example.buscaminas.data.entity.GameRecord
import com.example.buscaminas.model.GameState
import com.example.buscaminas.model.GameStatus
import kotlinx.coroutines.flow.Flow
import org.json.JSONObject

/**
 * Repository que maneja la persistencia de datos del juego
 * Utiliza SharedPreferences para datos de la partida actual (XML)
 * y Room Database para estadísticas históricas (SQLite)
 */
class GameRepository(context: Context) {
    
    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("game_prefs", Context.MODE_PRIVATE)
    
    private val gameDao: GameDao = GameDatabase.getDatabase(context).gameDao()
    
    private val fileManager = com.example.buscaminas.utils.FileManager(context)
    
    // Constantes para SharedPreferences
    companion object {
        private const val PREF_LAST_GAME = "last_game_data"
        private const val PREF_PLAYER1_TOTAL_WINS = "player1_total_wins"
        private const val PREF_PLAYER2_TOTAL_WINS = "player2_total_wins"
        private const val PREF_GAME_START_TIME = "game_start_time"
        private const val PREF_TOTAL_CELLS_REVEALED = "total_cells_revealed"
        private const val PREF_TOTAL_FLAGS_PLACED = "total_flags_placed"
    }
    
    // ==================== SharedPreferences (XML) ====================
    
    /**
     * Guarda la información de la partida actual en SharedPreferences (XML)
     */
    fun saveCurrentGameToPreferences(gameState: GameState) {
        val json = JSONObject().apply {
            put("player1_name", gameState.player1.name)
            put("player2_name", gameState.player2.name)
            put("player1_points", gameState.player1.points)
            put("player2_points", gameState.player2.points)
            put("player1_wins", gameState.player1.wins)
            put("player2_wins", gameState.player2.wins)
            put("current_player", gameState.currentPlayer)
            put("game_status", gameState.gameStatus.name)
            put("remaining_cells", gameState.remainingCells)
            put("placed_flags", gameState.placedFlags)
            put("total_flags", gameState.totalFlags)
            put("is_first_move", gameState.isFirstMove)
            put("timestamp", System.currentTimeMillis())
            
            // Guardar el tablero completo
            val boardArray = org.json.JSONArray()
            gameState.board.forEach { row ->
                val rowArray = org.json.JSONArray()
                row.forEach { cell ->
                    val cellJson = org.json.JSONObject().apply {
                        put("is_mine", cell.isMine)
                        put("is_revealed", cell.isRevealed)
                        put("is_flagged", cell.isFlagged)
                        put("adjacent_mines", cell.adjacentMines)
                    }
                    rowArray.put(cellJson)
                }
                boardArray.put(rowArray)
            }
            put("board", boardArray)
        }
        
        sharedPreferences.edit()
            .putString(PREF_LAST_GAME, json.toString())
            .apply()
    }
    
    /**
     * Obtiene la información de la última partida desde SharedPreferences
     */
    fun getLastGameFromPreferences(): String? {
        return sharedPreferences.getString(PREF_LAST_GAME, null)
    }
    
    /**
     * Carga el estado completo del juego desde SharedPreferences
     * @return Pair<GameState, List<List<Cell>>> o null si no hay partida guardada
     */
    fun loadGameState(): Pair<com.example.buscaminas.model.GameState, List<List<com.example.buscaminas.model.Cell>>>? {
        val jsonString = sharedPreferences.getString(PREF_LAST_GAME, null) ?: return null
        
        return try {
            val json = JSONObject(jsonString)
            
            // Parsear el tablero
            val boardJson = json.getJSONArray("board")
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
            
            // Crear el GameState
            val player1 = com.example.buscaminas.model.Player(
                id = 1,
                name = json.getString("player1_name"),
                points = json.getInt("player1_points"),
                wins = json.getInt("player1_wins"),
                color = androidx.compose.ui.graphics.Color(0xFF2196F3)
            )
            
            val player2 = com.example.buscaminas.model.Player(
                id = 2,
                name = json.getString("player2_name"),
                points = json.getInt("player2_points"),
                wins = json.getInt("player2_wins"),
                color = androidx.compose.ui.graphics.Color(0xFFF44336)
            )
            
            val gameState = com.example.buscaminas.model.GameState(
                board = board,
                player1 = player1,
                player2 = player2,
                currentPlayer = json.getInt("current_player"),
                gameStatus = com.example.buscaminas.model.GameStatus.valueOf(json.getString("game_status")),
                remainingCells = json.getInt("remaining_cells"),
                totalFlags = json.getInt("total_flags"),
                placedFlags = json.getInt("placed_flags"),
                isFirstMove = json.optBoolean("is_first_move", true)
            )
            
            gameState to board
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
    
    /**
     * Guarda el tiempo de inicio del juego
     */
    fun saveGameStartTime(timestamp: Long = System.currentTimeMillis()) {
        sharedPreferences.edit()
            .putLong(PREF_GAME_START_TIME, timestamp)
            .apply()
    }
    
    /**
     * Obtiene el tiempo de inicio del juego
     */
    fun getGameStartTime(): Long {
        return sharedPreferences.getLong(PREF_GAME_START_TIME, 0L)
    }
    
    /**
     * Incrementa el contador de celdas reveladas
     */
    fun incrementCellsRevealed(count: Int) {
        val current = sharedPreferences.getInt(PREF_TOTAL_CELLS_REVEALED, 0)
        sharedPreferences.edit()
            .putInt(PREF_TOTAL_CELLS_REVEALED, current + count)
            .apply()
    }
    
    /**
     * Incrementa el contador de banderas colocadas
     */
    fun incrementFlagsPlaced() {
        val current = sharedPreferences.getInt(PREF_TOTAL_FLAGS_PLACED, 0)
        sharedPreferences.edit()
            .putInt(PREF_TOTAL_FLAGS_PLACED, current + 1)
            .apply()
    }
    
    /**
     * Decrementa el contador de banderas colocadas (cuando se quita una bandera)
     */
    fun decrementFlagsPlaced() {
        val current = sharedPreferences.getInt(PREF_TOTAL_FLAGS_PLACED, 0)
        if (current > 0) {
            sharedPreferences.edit()
                .putInt(PREF_TOTAL_FLAGS_PLACED, current - 1)
                .apply()
        }
    }
    
    /**
     * Obtiene el total de celdas reveladas en la partida actual
     */
    fun getCurrentCellsRevealed(): Int {
        return sharedPreferences.getInt(PREF_TOTAL_CELLS_REVEALED, 0)
    }
    
    /**
     * Obtiene el total de banderas colocadas en la partida actual
     */
    fun getCurrentFlagsPlaced(): Int {
        return sharedPreferences.getInt(PREF_TOTAL_FLAGS_PLACED, 0)
    }
    
    /**
     * Resetea los contadores de la partida actual
     */
    fun resetCurrentGameCounters() {
        sharedPreferences.edit()
            .putInt(PREF_TOTAL_CELLS_REVEALED, 0)
            .putInt(PREF_TOTAL_FLAGS_PLACED, 0)
            .putLong(PREF_GAME_START_TIME, 0L)
            .apply()
    }
    
    /**
     * Guarda las victorias totales en SharedPreferences
     */
    fun saveTotalWins(player1Wins: Int, player2Wins: Int) {
        sharedPreferences.edit()
            .putInt(PREF_PLAYER1_TOTAL_WINS, player1Wins)
            .putInt(PREF_PLAYER2_TOTAL_WINS, player2Wins)
            .apply()
    }
    
    /**
     * Limpia todos los datos de SharedPreferences
     */
    fun clearPreferences() {
        sharedPreferences.edit().clear().apply()
    }
    
    // ==================== Room Database (SQLite) ====================
    
    /**
     * Guarda una partida completa en la base de datos
     */
    suspend fun saveGameRecord(
        gameState: GameState,
        totalCellsRevealed: Int,
        totalFlagsPlaced: Int,
        boardRows: Int,
        boardCols: Int,
        totalMines: Int
    ): Long {
        val startTime = getGameStartTime()
        val duration = if (startTime > 0) {
            (System.currentTimeMillis() - startTime) / 1000
        } else {
            0L
        }
        
        val winnerId = when (gameState.gameStatus) {
            GameStatus.PLAYER1_WON -> 1
            GameStatus.PLAYER2_WON -> 2
            GameStatus.DRAW -> 0
            GameStatus.GAME_OVER -> -1
            else -> -1
        }
        
        val record = GameRecord(
            timestamp = System.currentTimeMillis(),
            duration = duration,
            player1Name = gameState.player1.name,
            player2Name = gameState.player2.name,
            player1Points = gameState.player1.points,
            player2Points = gameState.player2.points,
            winnerId = winnerId,
            gameStatus = gameState.gameStatus.name,
            totalCellsRevealed = totalCellsRevealed,
            totalFlagsPlaced = totalFlagsPlaced,
            totalMines = totalMines,
            boardRows = boardRows,
            boardCols = boardCols,
            hitMine = gameState.gameStatus == GameStatus.GAME_OVER,
            completedBoard = gameState.remainingCells == 0
        )
        
        return gameDao.insertGame(record)
    }
    
    /**
     * Obtiene todas las partidas
     */
    fun getAllGames(): Flow<List<GameRecord>> = gameDao.getAllGames()
    
    /**
     * Obtiene las últimas N partidas
     */
    fun getRecentGames(limit: Int): Flow<List<GameRecord>> = gameDao.getRecentGames(limit)
    
    /**
     * Obtiene el total de partidas jugadas
     */
    suspend fun getTotalGames(): Int = gameDao.getTotalGames()
    
    /**
     * Obtiene las victorias del Jugador 1
     */
    suspend fun getPlayer1Wins(): Int = gameDao.getPlayer1Wins()
    
    /**
     * Obtiene las victorias del Jugador 2
     */
    suspend fun getPlayer2Wins(): Int = gameDao.getPlayer2Wins()
    
    /**
     * Obtiene el total de empates
     */
    suspend fun getTotalDraws(): Int = gameDao.getTotalDraws()
    
    /**
     * Obtiene el total de game overs
     */
    suspend fun getTotalGameOvers(): Int = gameDao.getTotalGameOvers()
    
    /**
     * Obtiene el promedio de puntos del Jugador 1
     */
    suspend fun getAveragePlayer1Points(): Double = gameDao.getAveragePlayer1Points()
    
    /**
     * Obtiene el promedio de puntos del Jugador 2
     */
    suspend fun getAveragePlayer2Points(): Double = gameDao.getAveragePlayer2Points()
    
    /**
     * Obtiene la puntuación máxima del Jugador 1
     */
    suspend fun getMaxPlayer1Points(): Int = gameDao.getMaxPlayer1Points()
    
    /**
     * Obtiene la puntuación máxima del Jugador 2
     */
    suspend fun getMaxPlayer2Points(): Int = gameDao.getMaxPlayer2Points()
    
    /**
     * Obtiene el promedio de duración de partidas
     */
    suspend fun getAverageDuration(): Double = gameDao.getAverageDuration()
    
    /**
     * Obtiene la partida más larga
     */
    suspend fun getLongestGame(): GameRecord? = gameDao.getLongestGame()
    
    /**
     * Obtiene la partida más corta
     */
    suspend fun getShortestGame(): GameRecord? = gameDao.getShortestGame()
    
    /**
     * Obtiene el total de celdas reveladas en todas las partidas
     */
    suspend fun getTotalCellsRevealed(): Int = gameDao.getTotalCellsRevealed()
    
    /**
     * Obtiene el total de banderas colocadas en todas las partidas
     */
    suspend fun getTotalFlagsPlaced(): Int = gameDao.getTotalFlagsPlaced()
    
    /**
     * Obtiene el total de partidas completadas
     */
    suspend fun getTotalCompletedGames(): Int = gameDao.getTotalCompletedGames()
    
    /**
     * Elimina todas las partidas de la base de datos
     */
    suspend fun deleteAllGames() = gameDao.deleteAllGames()
    
    /**
     * Obtiene todas las partidas como lista
     */
    suspend fun getAllGamesList(): List<GameRecord> = gameDao.getAllGamesList()
    
    // ==================== File Manager ====================
    
    /**
     * Carga una partida guardada desde un archivo
     */
    suspend fun loadGame(fileName: String): com.example.buscaminas.model.SavedGame? {
        return try {
            fileManager.loadGame(fileName)
        } catch (e: Exception) {
            e.printStackTrace()
            null
        }
    }
}
