package com.supertone.supertonic.tts

import java.io.File
import java.io.FileOutputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder

/**
 * WAV file writer for Android
 * Writes 16-bit PCM WAV files
 */
object WavWriter {
    
    /**
     * Write audio data to WAV file
     */
    fun writeWavFile(filePath: String, audioData: FloatArray, sampleRate: Int) {
        FileOutputStream(File(filePath)).use { fos ->
            writeWav(fos, audioData, sampleRate)
        }
    }
    
    /**
     * Write audio data to output stream as WAV
     */
    fun writeWav(outputStream: OutputStream, audioData: FloatArray, sampleRate: Int) {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = audioData.size * 2 // 2 bytes per sample (16-bit)
        val fileSize = 36 + dataSize
        
        // Write WAV header
        val header = ByteBuffer.allocate(44).apply {
            order(ByteOrder.LITTLE_ENDIAN)
            
            // RIFF header
            put("RIFF".toByteArray())
            putInt(fileSize)
            put("WAVE".toByteArray())
            
            // fmt subchunk
            put("fmt ".toByteArray())
            putInt(16) // Subchunk1Size for PCM
            putShort(1) // AudioFormat (1 = PCM)
            putShort(numChannels.toShort())
            putInt(sampleRate)
            putInt(byteRate)
            putShort(blockAlign.toShort())
            putShort(bitsPerSample.toShort())
            
            // data subchunk
            put("data".toByteArray())
            putInt(dataSize)
        }
        
        outputStream.write(header.array())
        
        // Write audio data
        val buffer = ByteBuffer.allocate(audioData.size * 2)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        for (sample in audioData) {
            // Clamp and convert to 16-bit
            val clampedSample = sample.coerceIn(-1.0f, 1.0f)
            val shortSample = (clampedSample * 32767).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(shortSample)
        }
        
        outputStream.write(buffer.array())
    }
    
    /**
     * Get WAV data as byte array
     */
    fun getWavBytes(audioData: FloatArray, sampleRate: Int): ByteArray {
        val numChannels = 1
        val bitsPerSample = 16
        val byteRate = sampleRate * numChannels * bitsPerSample / 8
        val blockAlign = numChannels * bitsPerSample / 8
        val dataSize = audioData.size * 2
        val fileSize = 36 + dataSize
        
        val buffer = ByteBuffer.allocate(44 + dataSize)
        buffer.order(ByteOrder.LITTLE_ENDIAN)
        
        // RIFF header
        buffer.put("RIFF".toByteArray())
        buffer.putInt(fileSize)
        buffer.put("WAVE".toByteArray())
        
        // fmt subchunk
        buffer.put("fmt ".toByteArray())
        buffer.putInt(16)
        buffer.putShort(1)
        buffer.putShort(numChannels.toShort())
        buffer.putInt(sampleRate)
        buffer.putInt(byteRate)
        buffer.putShort(blockAlign.toShort())
        buffer.putShort(bitsPerSample.toShort())
        
        // data subchunk
        buffer.put("data".toByteArray())
        buffer.putInt(dataSize)
        
        // Audio data
        for (sample in audioData) {
            val clampedSample = sample.coerceIn(-1.0f, 1.0f)
            val shortSample = (clampedSample * 32767).toInt().coerceIn(-32768, 32767).toShort()
            buffer.putShort(shortSample)
        }
        
        return buffer.array()
    }
}
