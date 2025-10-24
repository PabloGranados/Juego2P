package com.example.buscaminas.data.database

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.example.buscaminas.data.dao.GameDao
import com.example.buscaminas.data.entity.GameRecord

/**
 * Base de datos Room para almacenar estadísticas de partidas
 */
@Database(
    entities = [GameRecord::class],
    version = 1,
    exportSchema = false
)
abstract class GameDatabase : RoomDatabase() {
    
    abstract fun gameDao(): GameDao
    
    companion object {
        @Volatile
        private var INSTANCE: GameDatabase? = null
        
        /**
         * Obtiene la instancia singleton de la base de datos
         */
        fun getDatabase(context: Context): GameDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    GameDatabase::class.java,
                    "buscaminas_database"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}
