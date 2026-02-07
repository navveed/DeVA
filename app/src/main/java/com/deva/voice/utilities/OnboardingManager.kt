package com.deva.voice.utilities

import android.content.Context
import android.content.SharedPreferences

class OnboardingManager(context: Context) {

    private val sharedPreferences: SharedPreferences =
        context.getSharedPreferences("onboarding_prefs", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_ONBOARDING_COMPLETED = "onboarding_completed"
    }

    fun isOnboardingCompleted(): Boolean {
        return sharedPreferences.getBoolean(KEY_ONBOARDING_COMPLETED, false)
    }

    fun setOnboardingCompleted(completed: Boolean) {
        sharedPreferences.edit().putBoolean(KEY_ONBOARDING_COMPLETED, completed).apply()
    }
}




