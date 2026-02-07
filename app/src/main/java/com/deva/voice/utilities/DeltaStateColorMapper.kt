package com.deva.voice.utilities

import android.content.Context
import androidx.core.content.ContextCompat
import com.deva.voice.R

/**
 * Utility class for mapping DeVAState values to their corresponding colors
 * and providing state-related information for the delta symbol.
 */
object DeltaStateColorMapper {

    /**
     * Data class representing the visual state of the delta symbol
     */
    data class DeltaVisualState(
        val state: DeVAState,
        val color: Int,
        val statusText: String,
        val colorHex: String
    )

    /**
     * Get the color resource ID for a given DeVAState
     */
    fun getColorResourceId(state: DeVAState): Int {
        return when (state) {
            DeVAState.IDLE -> R.color.delta_idle
            DeVAState.LISTENING -> R.color.delta_listening
            DeVAState.PROCESSING -> R.color.delta_processing
            DeVAState.SPEAKING -> R.color.delta_speaking
            DeVAState.ERROR -> R.color.delta_error
        }
    }

    /**
     * Get the resolved color value for a given DeVAState
     */
    fun getColor(context: Context, state: DeVAState): Int {
        val colorResId = getColorResourceId(state)
        return ContextCompat.getColor(context, colorResId)
    }

    /**
     * Get the status text for a given DeVAState
     */
    fun getStatusText(state: DeVAState): String {
        return when (state) {
            DeVAState.IDLE -> "Ready, tap delta to wake me up!"
            DeVAState.LISTENING -> "Listening..."
            DeVAState.PROCESSING -> "Processing..."
            DeVAState.SPEAKING -> "Speaking..."
            DeVAState.ERROR -> "Error"
        }
    }

    /**
     * Get the hex color string for a given DeVAState (for debugging/logging)
     */
    fun getColorHex(context: Context, state: DeVAState): String {
        val color = getColor(context, state)
        return String.format("#%08X", color)
    }

    /**
     * Get complete visual state information for a given DeVAState
     */
    fun getDeltaVisualState(context: Context, state: DeVAState): DeltaVisualState {
        return DeltaVisualState(
            state = state,
            color = getColor(context, state),
            statusText = getStatusText(state),
            colorHex = getColorHex(context, state)
        )
    }

    /**
     * Get all available states with their visual information
     */
    fun getAllStates(context: Context): List<DeltaVisualState> {
        return DeVAState.values().map { state ->
            getDeltaVisualState(context, state)
        }
    }

    /**
     * Check if a state represents an active operation (not idle or error)
     */
    fun isActiveState(state: DeVAState): Boolean {
        return when (state) {
            DeVAState.LISTENING, DeVAState.PROCESSING, DeVAState.SPEAKING -> true
            DeVAState.IDLE, DeVAState.ERROR -> false
        }
    }

    /**
     * Check if a state represents an error condition
     */
    fun isErrorState(state: DeVAState): Boolean {
        return state == DeVAState.ERROR
    }

    /**
     * Get the priority of a state for determining which state to display
     * when multiple conditions might be true. Higher numbers = higher priority.
     */
    fun getStatePriority(state: DeVAState): Int {
        return when (state) {
            DeVAState.ERROR -> 5      // Highest priority
            DeVAState.SPEAKING -> 4   // High priority
            DeVAState.LISTENING -> 3  // Medium-high priority
            DeVAState.PROCESSING -> 2 // Medium priority
            DeVAState.IDLE -> 1       // Lowest priority
        }
    }
}




