package com.supertone.supertonic.tts

import ai.onnxruntime.OnnxTensor
import ai.onnxruntime.OrtEnvironment
import ai.onnxruntime.OrtSession
import android.content.Context
import android.util.Log
import java.nio.FloatBuffer
import java.nio.LongBuffer
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.ln
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt
import kotlin.random.Random

/**
 * Text-to-Speech inference engine using ONNX Runtime
 */
class TextToSpeech private constructor(
    private val config: Config,
    private val textProcessor: UnicodeProcessor,
    private val dpSession: OrtSession,
    private val textEncSession: OrtSession,
    private val vectorEstSession: OrtSession,
    private val vocoderSession: OrtSession,
    private val env: OrtEnvironment
) : AutoCloseable {
    
    val sampleRate: Int = config.ae.sampleRate
    private val baseChunkSize: Int = config.ae.baseChunkSize
    private val chunkCompress: Int = config.ttl.chunkCompressFactor
    private val ldim: Int = config.ttl.latentDim
    
    companion object {
        private const val TAG = "TextToSpeech"
        
        /**
         * Load TTS engine from assets
         */
        suspend fun loadFromAssets(
            context: Context,
            onnxDir: String = "onnx",
            useGpu: Boolean = false,
            onProgress: ((String) -> Unit)? = null
        ): TextToSpeech {
            if (useGpu) {
                throw RuntimeException("GPU mode is not supported yet")
            }
            
            onProgress?.invoke("Loading configuration...")
            val config = Config.loadFromAssets(context, onnxDir)
            
            onProgress?.invoke("Loading text processor...")
            val textProcessor = UnicodeProcessor.loadFromAssets(context, onnxDir)
            
            val env = OrtEnvironment.getEnvironment()
            val opts = OrtSession.SessionOptions()
            
            onProgress?.invoke("Loading duration predictor model...")
            val dpBytes = context.assets.open("$onnxDir/duration_predictor.onnx").use { it.readBytes() }
            val dpSession = env.createSession(dpBytes, opts)
            
            onProgress?.invoke("Loading text encoder model...")
            val textEncBytes = context.assets.open("$onnxDir/text_encoder.onnx").use { it.readBytes() }
            val textEncSession = env.createSession(textEncBytes, opts)
            
            onProgress?.invoke("Loading vector estimator model...")
            val vectorEstBytes = context.assets.open("$onnxDir/vector_estimator.onnx").use { it.readBytes() }
            val vectorEstSession = env.createSession(vectorEstBytes, opts)
            
            onProgress?.invoke("Loading vocoder model...")
            val vocoderBytes = context.assets.open("$onnxDir/vocoder.onnx").use { it.readBytes() }
            val vocoderSession = env.createSession(vocoderBytes, opts)
            
            onProgress?.invoke("Models loaded successfully!")
            Log.i(TAG, "Using CPU for inference")
            
            return TextToSpeech(
                config, textProcessor, dpSession, textEncSession, 
                vectorEstSession, vocoderSession, env
            )
        }
    }
    
    /**
     * Synthesize speech from a single text with automatic chunking
     */
    fun call(
        text: String,
        style: Style,
        totalStep: Int,
        speed: Float = 1.05f,
        silenceDuration: Float = 0.3f
    ): TTSResult {
        val chunks = TextChunker.chunkText(text)
        
        val wavCat = mutableListOf<Float>()
        var durCat = 0.0f
        
        for (i in chunks.indices) {
            val result = infer(listOf(chunks[i]), style, totalStep, speed)
            
            val dur = result.duration[0]
            val wavLen = (sampleRate * dur).toInt()
            val wavChunk = FloatArray(wavLen)
            System.arraycopy(result.wav, 0, wavChunk, 0, min(wavLen, result.wav.size))
            
            if (i == 0) {
                wavChunk.forEach { wavCat.add(it) }
                durCat = dur
            } else {
                val silenceLen = (silenceDuration * sampleRate).toInt()
                repeat(silenceLen) { wavCat.add(0.0f) }
                wavChunk.forEach { wavCat.add(it) }
                durCat += silenceDuration + dur
            }
        }
        
        return TTSResult(wavCat.toFloatArray(), floatArrayOf(durCat))
    }
    
    /**
     * Batch synthesize speech from multiple texts
     */
    fun batch(
        textList: List<String>,
        style: Style,
        totalStep: Int,
        speed: Float = 1.05f
    ): TTSResult {
        return infer(textList, style, totalStep, speed)
    }
    
    private fun infer(
        textList: List<String>,
        style: Style,
        totalStep: Int,
        speed: Float
    ): TTSResult {
        val bsz = textList.size
        
        // Process text
        val textResult = textProcessor.call(textList)
        val textIds = textResult.textIds
        val textMask = textResult.textMask
        
        // Create tensors
        val textIdsTensor = createLongTensor(textIds)
        val textMaskTensor = createFloatTensor3D(textMask)
        
        // Predict duration
        val dpInputs = mapOf(
            "text_ids" to textIdsTensor,
            "style_dp" to style.dpTensor,
            "text_mask" to textMaskTensor
        )
        
        val dpResult = dpSession.run(dpInputs)
        val dpValue = dpResult[0].value
        val duration = when (dpValue) {
            is Array<*> -> (dpValue[0] as FloatArray)
            is FloatArray -> dpValue
            else -> throw RuntimeException("Unexpected duration predictor output type")
        }
        
        // Apply speed factor to duration
        for (i in duration.indices) {
            duration[i] /= speed
        }
        
        // Encode text
        val textEncInputs = mapOf(
            "text_ids" to textIdsTensor,
            "style_ttl" to style.ttlTensor,
            "text_mask" to textMaskTensor
        )
        
        val textEncResult = textEncSession.run(textEncInputs)
        val textEmbTensor = textEncResult[0] as OnnxTensor
        
        // Sample noisy latent
        val noisyLatentResult = sampleNoisyLatent(duration)
        var xt = noisyLatentResult.noisyLatent
        val latentMask = noisyLatentResult.latentMask
        
        // Prepare constant tensors
        val totalStepArray = FloatArray(bsz) { totalStep.toFloat() }
        val totalStepTensor = OnnxTensor.createTensor(env, totalStepArray)
        
        // Denoising loop
        for (step in 0 until totalStep) {
            val currentStepArray = FloatArray(bsz) { step.toFloat() }
            val currentStepTensor = OnnxTensor.createTensor(env, currentStepArray)
            val noisyLatentTensor = createFloatTensor3D(xt)
            val latentMaskTensor = createFloatTensor3D(latentMask)
            val textMaskTensor2 = createFloatTensor3D(textMask)
            
            val vectorEstInputs = mapOf(
                "noisy_latent" to noisyLatentTensor,
                "text_emb" to textEmbTensor,
                "style_ttl" to style.ttlTensor,
                "latent_mask" to latentMaskTensor,
                "text_mask" to textMaskTensor2,
                "current_step" to currentStepTensor,
                "total_step" to totalStepTensor
            )
            
            val vectorEstResult = vectorEstSession.run(vectorEstInputs)
            @Suppress("UNCHECKED_CAST")
            val denoised = vectorEstResult[0].value as Array<Array<FloatArray>>
            
            // Update latent
            xt = denoised
            
            // Clean up
            currentStepTensor.close()
            noisyLatentTensor.close()
            latentMaskTensor.close()
            textMaskTensor2.close()
            vectorEstResult.close()
        }
        
        // Generate waveform
        val finalLatentTensor = createFloatTensor3D(xt)
        val vocoderInputs = mapOf("latent" to finalLatentTensor)
        
        val vocoderResult = vocoderSession.run(vocoderInputs)
        @Suppress("UNCHECKED_CAST")
        val wavBatch = vocoderResult[0].value as Array<FloatArray>
        val wav = wavBatch[0]
        
        // Clean up
        textIdsTensor.close()
        textMaskTensor.close()
        dpResult.close()
        textEncResult.close()
        totalStepTensor.close()
        finalLatentTensor.close()
        vocoderResult.close()
        
        return TTSResult(wav, duration)
    }
    
    private fun sampleNoisyLatent(duration: FloatArray): NoisyLatentResult {
        val bsz = duration.size
        var maxDur = 0f
        for (d in duration) {
            maxDur = max(maxDur, d)
        }
        
        val wavLenMax = (maxDur * sampleRate).toLong()
        val wavLengths = LongArray(bsz) { (duration[it] * sampleRate).toLong() }
        
        val chunkSize = baseChunkSize * chunkCompress
        val latentLen = ((wavLenMax + chunkSize - 1) / chunkSize).toInt()
        val latentDim = ldim * chunkCompress
        
        val noisyLatent = Array(bsz) { Array(latentDim) { FloatArray(latentLen) } }
        for (b in 0 until bsz) {
            for (d in 0 until latentDim) {
                for (t in 0 until latentLen) {
                    // Box-Muller transform
                    val u1 = max(1e-10, Random.nextDouble())
                    val u2 = Random.nextDouble()
                    noisyLatent[b][d][t] = (sqrt(-2.0 * ln(u1)) * cos(2.0 * PI * u2)).toFloat()
                }
            }
        }
        
        val latentMask = getLatentMask(wavLengths)
        
        // Apply mask
        for (b in 0 until bsz) {
            for (d in 0 until latentDim) {
                for (t in 0 until latentLen) {
                    noisyLatent[b][d][t] *= latentMask[b][0][t]
                }
            }
        }
        
        return NoisyLatentResult(noisyLatent, latentMask)
    }
    
    private fun getLatentMask(wavLengths: LongArray): Array<Array<FloatArray>> {
        val baseChunkSizeL = baseChunkSize.toLong()
        val chunkCompressL = chunkCompress.toLong()
        val latentSize = baseChunkSizeL * chunkCompressL
        
        val latentLengths = LongArray(wavLengths.size) { (wavLengths[it] + latentSize - 1) / latentSize }
        var maxLen = 0L
        for (len in latentLengths) {
            maxLen = max(maxLen, len)
        }
        
        return Array(wavLengths.size) { i ->
            Array(1) {
                FloatArray(maxLen.toInt()) { j ->
                    if (j < latentLengths[i]) 1.0f else 0.0f
                }
            }
        }
    }
    
    private fun createFloatTensor3D(array: Array<Array<FloatArray>>): OnnxTensor {
        val dim0 = array.size
        val dim1 = array[0].size
        val dim2 = array[0][0].size
        
        val flat = FloatArray(dim0 * dim1 * dim2)
        var idx = 0
        for (i in 0 until dim0) {
            for (j in 0 until dim1) {
                for (k in 0 until dim2) {
                    flat[idx++] = array[i][j][k]
                }
            }
        }
        
        val shape = longArrayOf(dim0.toLong(), dim1.toLong(), dim2.toLong())
        return OnnxTensor.createTensor(env, FloatBuffer.wrap(flat), shape)
    }
    
    private fun createLongTensor(array: Array<LongArray>): OnnxTensor {
        val dim0 = array.size
        val dim1 = array[0].size
        
        val flat = LongArray(dim0 * dim1)
        var idx = 0
        for (i in 0 until dim0) {
            for (j in 0 until dim1) {
                flat[idx++] = array[i][j]
            }
        }
        
        val shape = longArrayOf(dim0.toLong(), dim1.toLong())
        return OnnxTensor.createTensor(env, LongBuffer.wrap(flat), shape)
    }
    
    override fun close() {
        dpSession.close()
        textEncSession.close()
        vectorEstSession.close()
        vocoderSession.close()
    }
}
