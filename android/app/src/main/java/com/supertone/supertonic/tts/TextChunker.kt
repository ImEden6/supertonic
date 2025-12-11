package com.supertone.supertonic.tts

import java.util.regex.Pattern

/**
 * Text chunking utilities for long-form synthesis
 */
object TextChunker {
    
    private const val MAX_CHUNK_LENGTH = 300
    
    private val ABBREVIATIONS = listOf(
        "Dr.", "Mr.", "Mrs.", "Ms.", "Prof.", "Sr.", "Jr.",
        "St.", "Ave.", "Rd.", "Blvd.", "Dept.", "Inc.", "Ltd.",
        "Co.", "Corp.", "etc.", "vs.", "i.e.", "e.g.", "Ph.D."
    )
    
    /**
     * Chunk text into smaller segments based on paragraphs and sentences
     */
    fun chunkText(text: String, maxLen: Int = 0): List<String> {
        val effectiveMaxLen = if (maxLen == 0) MAX_CHUNK_LENGTH else maxLen
        
        val trimmedText = text.trim()
        if (trimmedText.isEmpty()) {
            return listOf("")
        }
        
        // Split by paragraphs
        val paragraphs = trimmedText.split(Regex("\\n\\s*\\n"))
        val chunks = mutableListOf<String>()
        
        for (para in paragraphs) {
            val trimmedPara = para.trim()
            if (trimmedPara.isEmpty()) continue
            
            if (trimmedPara.length <= effectiveMaxLen) {
                chunks.add(trimmedPara)
                continue
            }
            
            // Split by sentences
            val sentences = splitSentences(trimmedPara)
            val current = StringBuilder()
            var currentLen = 0
            
            for (sentence in sentences) {
                val trimmedSentence = sentence.trim()
                if (trimmedSentence.isEmpty()) continue
                
                val sentenceLen = trimmedSentence.length
                
                if (sentenceLen > effectiveMaxLen) {
                    // If sentence is longer than maxLen, split by comma or space
                    if (current.isNotEmpty()) {
                        chunks.add(current.toString().trim())
                        current.clear()
                        currentLen = 0
                    }
                    
                    // Try splitting by comma
                    val parts = trimmedSentence.split(",")
                    for (part in parts) {
                        val trimmedPart = part.trim()
                        if (trimmedPart.isEmpty()) continue
                        
                        val partLen = trimmedPart.length
                        if (partLen > effectiveMaxLen) {
                            // Split by space as last resort
                            val words = trimmedPart.split(Regex("\\s+"))
                            val wordChunk = StringBuilder()
                            var wordChunkLen = 0
                            
                            for (word in words) {
                                val wordLen = word.length
                                if (wordChunkLen + wordLen + 1 > effectiveMaxLen && wordChunk.isNotEmpty()) {
                                    chunks.add(wordChunk.toString().trim())
                                    wordChunk.clear()
                                    wordChunkLen = 0
                                }
                                
                                if (wordChunk.isNotEmpty()) {
                                    wordChunk.append(" ")
                                    wordChunkLen++
                                }
                                wordChunk.append(word)
                                wordChunkLen += wordLen
                            }
                            
                            if (wordChunk.isNotEmpty()) {
                                chunks.add(wordChunk.toString().trim())
                            }
                        } else {
                            if (currentLen + partLen + 1 > effectiveMaxLen && current.isNotEmpty()) {
                                chunks.add(current.toString().trim())
                                current.clear()
                                currentLen = 0
                            }
                            
                            if (current.isNotEmpty()) {
                                current.append(", ")
                                currentLen += 2
                            }
                            current.append(trimmedPart)
                            currentLen += partLen
                        }
                    }
                    continue
                }
                
                if (currentLen + sentenceLen + 1 > effectiveMaxLen && current.isNotEmpty()) {
                    chunks.add(current.toString().trim())
                    current.clear()
                    currentLen = 0
                }
                
                if (current.isNotEmpty()) {
                    current.append(" ")
                    currentLen++
                }
                current.append(trimmedSentence)
                currentLen += sentenceLen
            }
            
            if (current.isNotEmpty()) {
                chunks.add(current.toString().trim())
            }
        }
        
        return if (chunks.isEmpty()) listOf("") else chunks
    }
    
    /**
     * Split text into sentences, avoiding common abbreviations
     */
    private fun splitSentences(text: String): List<String> {
        // Build pattern that avoids abbreviations
        val abbrevPattern = ABBREVIATIONS.joinToString("|") { Pattern.quote(it) }
        
        // Match sentence endings, but not abbreviations
        val patternStr = "(?<!(?:$abbrevPattern))(?<=[.!?])\\s+"
        val pattern = Pattern.compile(patternStr)
        
        return pattern.split(text).toList()
    }
    
    /**
     * Sanitize filename
     */
    fun sanitizeFilename(text: String, maxLen: Int = 20): String {
        val truncated = if (text.length > maxLen) text.substring(0, maxLen) else text
        return truncated.replace(Regex("[^a-zA-Z0-9]"), "_")
    }
}
