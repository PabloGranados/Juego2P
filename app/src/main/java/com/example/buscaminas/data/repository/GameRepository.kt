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
            put("timestamp", System.currentTimeMillis())
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
}
