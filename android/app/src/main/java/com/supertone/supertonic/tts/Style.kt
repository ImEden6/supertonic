package com.supertone.supertonic.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import android.content.Context
import com.google.gson.Gson
import com.google.gson.annotations.SerializedName
import java.io.InputStreamReader
import java.nio.FloatBuffer

/**
 * Voice style data from JSON file
 */
data class VoiceStyleData(
    @SerializedName("style_ttl")
    val styleTtl: StyleTensor,
    @SerializedName("style_dp")
    val styleDp: StyleTensor
)

data class StyleTensor(
    val data: List<List<List<Float>>>,
    val dims: List<Long>,
    val type: String
)

/**
 * Voice style holder with ONNX tensors
 */
class Style(
    val ttlTensor: OnnxTensor,
    val dpTensor: OnnxTensor
) : AutoCloseable {
    
    override fun close() {
        ttlTensor.close()
        dpTensor.close()
    }
    
    companion object {
        /**
         * Available voice styles
         */
        val VOICE_STYLES = listOf(
            "M1" to "Male Voice 1",
            "M2" to "Male Voice 2",
            "M3" to "Male Voice 3",
            "M4" to "Male Voice 4",
            "M5" to "Male Voice 5",
            "F1" to "Female Voice 1",
            "F2" to "Female Voice 2",
            "F3" to "Female Voice 3",
            "F4" to "Female Voice 4",
            "F5" to "Female Voice 5"
        )
        
        /**
         * Load voice style from assets
         */
        fun loadFromAssets(
            context: Context,
            voiceStylePaths: List<String>,
            env: OrtEnvironment
        ): Style {
            val bsz = voiceStylePaths.size
            val gson = Gson()
            
            // Read first file to get dimensions
            val firstData = context.assets.open(voiceStylePaths[0]).use { inputStream ->
                InputStreamReader(inputStream).use { reader ->
                    gson.fromJson(reader, VoiceStyleData::class.java)
                }
            }
            
            val ttlDims = firstData.styleTtl.dims
            val dpDims = firstData.styleDp.dims
            
            val ttlDim1 = ttlDims[1]
            val ttlDim2 = ttlDims[2]
            val dpDim1 = dpDims[1]
            val dpDim2 = dpDims[2]
            
            // Pre-allocate arrays with full batch size
            val ttlSize = (bsz * ttlDim1 * ttlDim2).toInt()
            val dpSize = (bsz * dpDim1 * dpDim2).toInt()
            val ttlFlat = FloatArray(ttlSize)
            val dpFlat = FloatArray(dpSize)
            
            // Fill in the data
            for (i in 0 until bsz) {
                val data = context.assets.open(voiceStylePaths[i]).use { inputStream ->
                    InputStreamReader(inputStream).use { reader ->
                        gson.fromJson(reader, VoiceStyleData::class.java)
                    }
                }
                
                // Flatten TTL data
                val ttlOffset = (i * ttlDim1 * ttlDim2).toInt()
                var idx = 0
                for (batch in data.styleTtl.data) {
                    for (row in batch) {
                        for (value in row) {
                            ttlFlat[ttlOffset + idx++] = value
                        }
                    }
                }
                
                // Flatten DP data
                val dpOffset = (i * dpDim1 * dpDim2).toInt()
                idx = 0
                for (batch in data.styleDp.data) {
                    for (row in batch) {
                        for (value in row) {
                            dpFlat[dpOffset + idx++] = value
                        }
                    }
                }
            }
            
            val ttlShape = longArrayOf(bsz.toLong(), ttlDim1, ttlDim2)
            val dpShape = longArrayOf(bsz.toLong(), dpDim1, dpDim2)
            
            val ttlTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(ttlFlat), ttlShape)
            val dpTensor = OnnxTensor.createTensor(env, FloatBuffer.wrap(dpFlat), dpShape)
            
            return Style(ttlTensor, dpTensor)
        }
    }
}
