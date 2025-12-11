package com.supertone.supertonic.tts

import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader

/**
 * Audio encoder configuration
 */
data class AEConfig(
    @SerializedName("sample_rate")
    val sampleRate: Int,
    @SerializedName("base_chunk_size")
    val baseChunkSize: Int
)

/**
 * Text-to-latent configuration
 */
data class TTLConfig(
    @SerializedName("chunk_compress_factor")
    val chunkCompressFactor: Int,
    @SerializedName("latent_dim")
    val latentDim: Int
)

/**
 * Main TTS configuration loaded from tts.json
 */
data class Config(
    val ae: AEConfig,
    val ttl: TTLConfig
) {
    companion object {
        /**
         * Load configuration from assets
         */
        fun loadFromAssets(context: Context, onnxDir: String): Config {
            val inputStream = context.assets.open("$onnxDir/tts.json")
            return InputStreamReader(inputStream).use { reader ->
                Gson().fromJson(reader, Config::class.java)
            }
        }
    }
}

/**
 * TTS synthesis result
 */
data class TTSResult(
    val wav: FloatArray,
    val duration: FloatArray
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TTSResult
        if (!wav.contentEquals(other.wav)) return false
        if (!duration.contentEquals(other.duration)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = wav.contentHashCode()
        result = 31 * result + duration.contentHashCode()
        return result
    }
}

/**
 * Noisy latent result for denoising
 */
data class NoisyLatentResult(
    val noisyLatent: Array<Array<FloatArray>>,
    val latentMask: Array<Array<FloatArray>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as NoisyLatentResult
        if (!noisyLatent.contentDeepEquals(other.noisyLatent)) return false
        if (!latentMask.contentDeepEquals(other.latentMask)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = noisyLatent.contentDeepHashCode()
        result = 31 * result + latentMask.contentDeepHashCode()
        return result
    }
}
