package com.deva.voice.utilities

/**
 * Represents the different states of the DeVA app for visual feedback
 */
enum class DeVAState {
    /**
     * App is ready and idle, no active conversation or processing
     */
    IDLE,
    
    /**
     * App is actively listening for user input via STT
     */
    LISTENING,
    
    /**
     * App is processing user request (thinking/reasoning)
     */
    PROCESSING,
    
    /**
     * App is speaking to the user via TTS
     */
    SPEAKING,
    
    /**
     * App encountered an error (STT errors, service issues, etc.)
     */
    ERROR
}




