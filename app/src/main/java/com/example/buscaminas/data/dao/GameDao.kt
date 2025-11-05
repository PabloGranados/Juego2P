package com.example.buscaminas.data.dao

import androidx.room.*
import com.example.buscaminas.data.entity.GameRecord
import kotlinx.coroutines.flow.Flow

/**
 * DAO para operaciones de base de datos con GameRecord
 */
@Dao
interface GameDao {
    
    /**
     * Inserta un nuevo registro de partida
     */
    @Insert
    suspend fun insertGame(game: GameRecord): Long
    
    /**
     * Obtiene todas las partidas ordenadas por fecha descendente
     */
    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    fun getAllGames(): Flow<List<GameRecord>>
    
    /**
     * Obtiene todas las partidas como lista (no Flow)
     */
    @Query("SELECT * FROM game_records ORDER BY timestamp DESC")
    suspend fun getAllGamesList(): List<GameRecord>
    
    /**
     * Obtiene las últimas N partidas
     */
    @Query("SELECT * FROM game_records ORDER BY timestamp DESC LIMIT :limit")
    fun getRecentGames(limit: Int): Flow<List<GameRecord>>
    
    /**
     * Obtiene el total de partidas jugadas
     */
    @Query("SELECT COUNT(*) FROM game_records")
    suspend fun getTotalGames(): Int
    
    /**
     * Obtiene el total de victorias del Jugador 1
     */
    @Query("SELECT COUNT(*) FROM game_records WHERE winnerId = 1")
    suspend fun getPlayer1Wins(): Int
    
    /**
     * Obtiene el total de victorias del Jugador 2
     */
    @Query("SELECT COUNT(*) FROM game_records WHERE winnerId = 2")
    suspend fun getPlayer2Wins(): Int
    
    /**
     * Obtiene el total de empates
     */
    @Query("SELECT COUNT(*) FROM game_records WHERE winnerId = 0")
    suspend fun getTotalDraws(): Int
    
    /**
     * Obtiene el total de game overs (minas tocadas)
     */
    @Query("SELECT COUNT(*) FROM game_records WHERE hitMine = 1")
    suspend fun getTotalGameOvers(): Int
    
    /**
     * Obtiene el promedio de puntos del Jugador 1
     */
    @Query("SELECT COALESCE(AVG(player1Points), 0) FROM game_records")
    suspend fun getAveragePlayer1Points(): Double
    
    /**
     * Obtiene el promedio de puntos del Jugador 2
     */
    @Query("SELECT COALESCE(AVG(player2Points), 0) FROM game_records")
    suspend fun getAveragePlayer2Points(): Double
    
    /**
     * Obtiene la puntuación máxima del Jugador 1
     */
    @Query("SELECT COALESCE(MAX(player1Points), 0) FROM game_records")
    suspend fun getMaxPlayer1Points(): Int
    
    /**
     * Obtiene la puntuación máxima del Jugador 2
     */
    @Query("SELECT COALESCE(MAX(player2Points), 0) FROM game_records")
    suspend fun getMaxPlayer2Points(): Int
    
    /**
     * Obtiene el promedio de duración de partidas
     */
    @Query("SELECT COALESCE(AVG(duration), 0) FROM game_records")
    suspend fun getAverageDuration(): Double
    
    /**
     * Obtiene la partida con mayor duración
     */
    @Query("SELECT * FROM game_records ORDER BY duration DESC LIMIT 1")
    suspend fun getLongestGame(): GameRecord?
    
    /**
     * Obtiene la partida con menor duración (excluyendo duraciones de 0)
     */
    @Query("SELECT * FROM game_records WHERE duration > 0 ORDER BY duration ASC LIMIT 1")
    suspend fun getShortestGame(): GameRecord?
    
    /**
     * Obtiene el total de celdas reveladas en todas las partidas
     */
    @Query("SELECT COALESCE(SUM(totalCellsRevealed), 0) FROM game_records")
    suspend fun getTotalCellsRevealed(): Int
    
    /**
     * Obtiene el total de banderas colocadas en todas las partidas
     */
    @Query("SELECT COALESCE(SUM(totalFlagsPlaced), 0) FROM game_records")
    suspend fun getTotalFlagsPlaced(): Int
    
    /**
     * Elimina todas las partidas
     */
    @Query("DELETE FROM game_records")
    suspend fun deleteAllGames()
    
    /**
     * Elimina una partida específica
     */
    @Delete
    suspend fun deleteGame(game: GameRecord)
    
    /**
     * Obtiene partidas por estado del juego
     */
    @Query("SELECT * FROM game_records WHERE gameStatus = :status ORDER BY timestamp DESC")
    fun getGamesByStatus(status: String): Flow<List<GameRecord>>
    
    /**
     * Obtiene el total de partidas completadas (sin tocar minas)
     */
    @Query("SELECT COUNT(*) FROM game_records WHERE completedBoard = 1")
    suspend fun getTotalCompletedGames(): Int
}
