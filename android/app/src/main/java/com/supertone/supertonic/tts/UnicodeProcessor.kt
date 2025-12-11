package com.supertone.supertonic.tts

import android.content.Context
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import java.io.InputStreamReader
import java.text.Normalizer

/**
 * Unicode text processor for TTS input
 */
class UnicodeProcessor(
    private val indexer: LongArray
) {
    
    companion object {
        // Emoji Unicode ranges
        private val EMOJI_RANGES = listOf(
            0x1F600..0x1F64F,  // Emoticons
            0x1F300..0x1F5FF,  // Misc Symbols and Pictographs
            0x1F680..0x1F6FF,  // Transport and Map
            0x1F700..0x1F77F,  // Alchemical Symbols
            0x1F780..0x1F7FF,  // Geometric Shapes Extended
            0x1F800..0x1F8FF,  // Supplemental Arrows-C
            0x1F900..0x1F9FF,  // Supplemental Symbols and Pictographs
            0x1FA00..0x1FA6F,  // Chess Symbols
            0x1FA70..0x1FAFF,  // Symbols and Pictographs Extended-A
            0x2600..0x26FF,    // Misc Symbols
            0x2700..0x27BF,    // Dingbats
            0x1F1E6..0x1F1FF   // Regional Indicator Symbols
        )
        
        // Character replacements
        private val REPLACEMENTS = mapOf(
            "\u2013" to "-",   // en dash
            "\u2011" to "-",   // non-breaking hyphen
            "\u2014" to "-",   // em dash
            "\u00AF" to " ",   // macron
            "_" to " ",
            "\u201C" to "\"",  // left double quote
            "\u201D" to "\"",  // right double quote
            "\u2018" to "'",   // left single quote
            "\u2019" to "'",   // right single quote
            "\u00B4" to "'",   // acute accent
            "`" to "'",
            "[" to " ",
            "]" to " ",
            "|" to " ",
            "/" to " ",
            "#" to " ",
            "\u2192" to " ",   // right arrow
            "\u2190" to " "    // left arrow
        )
        
        // Expression replacements
        private val EXPR_REPLACEMENTS = mapOf(
            "@" to " at ",
            "e.g.," to "for example, ",
            "i.e.," to "that is, "
        )
        
        // Combining diacritics pattern
        private val DIACRITICS_PATTERN = Regex("[\u0302\u0303\u0304\u0305\u0306\u0307\u0308\u030A\u030B\u030C\u0327\u0328\u0329\u032A\u032B\u032C\u032D\u032E\u032F]")
        
        // Special symbols pattern
        private val SPECIAL_SYMBOLS_PATTERN = Regex("[\\u2665\\u2606\\u2661\\u00A9\\\\]")
        
        // Sentence ending pattern
        private val SENTENCE_END_PATTERN = Regex("[.!?;:,'\"\u201C\u201D\u2018\u2019)\\]}\\u2026\\u3002\\u300D\\u300F\\u3011\\u3009\\u300B\\u203A\\u00BB]$")
        
        /**
         * Load processor from assets
         */
        fun loadFromAssets(context: Context, onnxDir: String): UnicodeProcessor {
            val inputStream = context.assets.open("$onnxDir/unicode_indexer.json")
            val indexer = InputStreamReader(inputStream).use { reader ->
                val type = object : TypeToken<List<Long>>() {}.type
                Gson().fromJson<List<Long>>(reader, type).toLongArray()
            }
            return UnicodeProcessor(indexer)
        }
        
        private fun isEmoji(codePoint: Int): Boolean {
            return EMOJI_RANGES.any { codePoint in it }
        }
        
        private fun removeEmojis(text: String): String {
            val result = StringBuilder()
            var i = 0
            while (i < text.length) {
                val codePoint = if (Character.isHighSurrogate(text[i]) && 
                    i + 1 < text.length && 
                    Character.isLowSurrogate(text[i + 1])) {
                    val cp = Character.codePointAt(text, i)
                    i++ // Skip the low surrogate
                    cp
                } else {
                    text[i].code
                }
                
                if (!isEmoji(codePoint)) {
                    if (codePoint > 0xFFFF) {
                        result.append(Character.toChars(codePoint))
                    } else {
                        result.append(codePoint.toChar())
                    }
                }
                i++
            }
            return result.toString()
        }
    }
    
    /**
     * Process text list into IDs and masks
     */
    fun call(textList: List<String>): TextProcessResult {
        val processedTexts = textList.map { preprocessText(it) }
        
        val textIdsLengths = processedTexts.map { it.length }.toIntArray()
        val maxLen = textIdsLengths.maxOrNull() ?: 0
        
        val textIds = Array(processedTexts.size) { LongArray(maxLen) }
        for (i in processedTexts.indices) {
            val unicodeVals = textToUnicodeValues(processedTexts[i])
            for (j in unicodeVals.indices) {
                val unicodeVal = unicodeVals[j]
                textIds[i][j] = if (unicodeVal < indexer.size) indexer[unicodeVal] else 0L
            }
        }
        
        val textMask = getTextMask(textIdsLengths)
        return TextProcessResult(textIds, textMask)
    }
    
    private fun preprocessText(text: String): String {
        var result = Normalizer.normalize(text, Normalizer.Form.NFKD)
        
        // Remove emojis
        result = removeEmojis(result)
        
        // Replace various dashes and symbols
        for ((key, value) in REPLACEMENTS) {
            result = result.replace(key, value)
        }
        
        // Remove combining diacritics
        result = result.replace(DIACRITICS_PATTERN, "")
        
        // Remove special symbols
        result = result.replace(SPECIAL_SYMBOLS_PATTERN, "")
        
        // Replace known expressions
        for ((key, value) in EXPR_REPLACEMENTS) {
            result = result.replace(key, value)
        }
        
        // Fix spacing around punctuation
        result = result.replace(" ,", ",")
        result = result.replace(" .", ".")
        result = result.replace(" !", "!")
        result = result.replace(" ?", "?")
        result = result.replace(" ;", ";")
        result = result.replace(" :", ":")
        result = result.replace(" '", "'")
        
        // Remove duplicate quotes
        while ("\"\"" in result) {
            result = result.replace("\"\"", "\"")
        }
        while ("''" in result) {
            result = result.replace("''", "'")
        }
        while ("``" in result) {
            result = result.replace("``", "`")
        }
        
        // Remove extra spaces
        result = result.replace(Regex("\\s+"), " ").trim()
        
        // If text doesn't end with punctuation, add a period
        if (!SENTENCE_END_PATTERN.containsMatchIn(result)) {
            result += "."
        }
        
        return result
    }
    
    private fun textToUnicodeValues(text: String): IntArray {
        return IntArray(text.length) { text.codePointAt(it) }
    }
    
    private fun getTextMask(lengths: IntArray): Array<Array<FloatArray>> {
        val bsz = lengths.size
        val maxLen = lengths.maxOrNull() ?: 0
        
        return Array(bsz) { i ->
            Array(1) { FloatArray(maxLen) { j ->
                if (j < lengths[i]) 1.0f else 0.0f
            }}
        }
    }
}

/**
 * Result of text processing
 */
data class TextProcessResult(
    val textIds: Array<LongArray>,
    val textMask: Array<Array<FloatArray>>
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (javaClass != other?.javaClass) return false
        other as TextProcessResult
        if (!textIds.contentDeepEquals(other.textIds)) return false
        if (!textMask.contentDeepEquals(other.textMask)) return false
        return true
    }

    override fun hashCode(): Int {
        var result = textIds.contentDeepHashCode()
        result = 31 * result + textMask.contentDeepHashCode()
        return result
    }
}
