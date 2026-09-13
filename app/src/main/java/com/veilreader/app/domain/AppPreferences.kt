package com.veilreader.app.domain

enum class AppTheme(val label: String) {
    SYSTEM("System"), LIGHT("Light"), DARK("Dark")
}

/** User choices only. Migration flags and device-specific state never travel in backups. */
data class AppPreferences(
    val theme: AppTheme = AppTheme.SYSTEM,
    val gameVisible: Boolean = true,
    val onboardingCompleted: Boolean = false
)
