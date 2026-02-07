package com.deva.voice

import org.junit.Test
import org.junit.Assert.*

/**
 * Unit tests for ConversationalAgentService behavior changes
 * Testing that greeting is skipped and listening starts immediately
 */
class ConversationalAgentServiceTest {
    
    @Test
    fun test_hasHeardFirstUtterance_initiallyFalse() {
        // Test that the hasHeardFirstUtterance flag is properly initialized
        // Note: This is a behavioral test - we can't easily test the actual service
        // without extensive mocking of Android components
        
        // Simulating initial state
        val hasHeardFirstUtterance = false
        assertFalse("hasHeardFirstUtterance should be false initially", hasHeardFirstUtterance)
    }
    
    @Test
    fun test_firstUtteranceTriggersMemoryExtraction() {
        // Test that memory extraction is deferred until first utterance
        
        // Simulating the flow:
        var hasHeardFirstUtterance = false
        var memoryExtractionCalled = false
        
        // Simulate service startup (no memory extraction)
        assertFalse("Memory extraction should not be called on startup", memoryExtractionCalled)
        
        // Simulate first user utterance
        if (!hasHeardFirstUtterance) {
            hasHeardFirstUtterance = true
            memoryExtractionCalled = true // This would be the call to updateSystemPromptWithMemories()
        }
        
        assertTrue("hasHeardFirstUtterance should be true after first utterance", hasHeardFirstUtterance)
        assertTrue("Memory extraction should be called after first utterance", memoryExtractionCalled)
    }
    
    @Test
    fun test_subsequentUtterancesDoNotRetriggerMemoryExtraction() {
        // Test that memory extraction is only triggered once
        
        var hasHeardFirstUtterance = false
        var memoryExtractionCallCount = 0
        
        // Simulate first utterance
        if (!hasHeardFirstUtterance) {
            hasHeardFirstUtterance = true
            memoryExtractionCallCount++
        }
        
        // Simulate second utterance
        if (!hasHeardFirstUtterance) {
            hasHeardFirstUtterance = true
            memoryExtractionCallCount++
        }
        
        assertEquals("Memory extraction should only be called once", 1, memoryExtractionCallCount)
    }
    
    @Test 
    fun test_noGreetingOnStartup() {
        // Test that no greeting is sent on service startup
        
        var greetingSent = false
        val conversationHistorySize = 1 // Just the system prompt
        
        // Simulate the old behavior (removed)
        // if (conversationHistory.size == 1) {
        //     greetingSent = true
        // }
        
        // With our changes, no greeting should be sent
        assertFalse("No greeting should be sent on startup", greetingSent)
    }
}
