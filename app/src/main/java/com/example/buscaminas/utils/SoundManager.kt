package com.example.buscaminas.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.SoundPool

/**
 * Gestor de sonidos para la aplicación
 */
class SoundManager(private val context: Context) {
    
    private var soundPool: SoundPool? = null
    private val soundIds = mutableMapOf<SoundType, Int>()
    private var isEnabled = true
    
    enum class SoundType {
        CELL_REVEAL,
        FLAG_PLACE,
        FLAG_REMOVE,
        MINE_HIT,
        VICTORY,
        BUTTON_CLICK
    }
    
    init {
        initSoundPool()
    }
    
    private fun initSoundPool() {
        val audioAttributes = AudioAttributes.Builder()
            .setUsage(AudioAttributes.USAGE_GAME)
            .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
            .build()
        
        soundPool = SoundPool.Builder()
            .setMaxStreams(5)
            .setAudioAttributes(audioAttributes)
            .build()
        
        // Nota: Los sonidos se pueden generar mediante código o usar archivos de recursos
        // Por ahora, la funcionalidad está lista pero sin archivos de audio
        // Para agregar sonidos reales, coloca archivos .ogg o .mp3 en res/raw/
    }
    
    /**
     * Reproduce un sonido del tipo especificado
     */
    fun playSound(type: SoundType) {
        if (!isEnabled || soundPool == null) return
        
        soundIds[type]?.let { soundId ->
            soundPool?.play(soundId, 1f, 1f, 0, 0, 1f)
        }
    }
    
    /**
     * Activa o desactiva los sonidos
     */
    fun setEnabled(enabled: Boolean) {
        isEnabled = enabled
    }
    
    /**
     * Verifica si los sonidos están activados
     */
    fun isEnabled(): Boolean = isEnabled
    
    /**
     * Libera los recursos del SoundPool
     */
    fun release() {
        soundPool?.release()
        soundPool = null
        soundIds.clear()
    }
}
