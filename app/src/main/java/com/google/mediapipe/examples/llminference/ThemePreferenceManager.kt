package com.google.mediapipe.examples.llminference

import android.content.Context
import android.content.SharedPreferences
import android.content.res.Configuration
import androidx.appcompat.app.AppCompatDelegate

object ThemePreferenceManager {

    private const val PREFS_NAME = "theme_prefs"
    private const val KEY_THEME = "pref_theme"

    // Define theme constants
    const val THEME_LIGHT = 0
    const val THEME_DARK = 1
    const val THEME_SYSTEM = 2 // Default

    private fun getPreferences(context: Context): SharedPreferences {
        return context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveThemePreference(context: Context, theme: Int) {
        val editor = getPreferences(context).edit()
        editor.putInt(KEY_THEME, theme)
        editor.apply()
        // Apply the theme immediately
        applyTheme(theme)
    }

    fun loadThemePreference(context: Context): Int {
        return getPreferences(context).getInt(KEY_THEME, THEME_SYSTEM) // Default to system
    }

    fun applyTheme(themePreference: Int) {
        when (themePreference) {
            THEME_LIGHT -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_NO)
            THEME_DARK -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_YES)
            else -> AppCompatDelegate.setDefaultNightMode(AppCompatDelegate.MODE_NIGHT_FOLLOW_SYSTEM) // THEME_SYSTEM
        }
    }
    
    // Helper to check if the effective mode is dark (considering system setting if applicable)
    fun isCurrentlyDark(context: Context): Boolean {
        return when (loadThemePreference(context)) {
            THEME_DARK -> true
            THEME_LIGHT -> false
            else -> { // THEME_SYSTEM
                val currentNightMode = context.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK
                currentNightMode == Configuration.UI_MODE_NIGHT_YES
            }
        }
    }
} 