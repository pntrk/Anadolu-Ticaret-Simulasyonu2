package com.example.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

enum class AppLanguage(
    val code: String,
    val title: String,
    val nativeName: String,
    val flagEmoji: String
) {
    TURKISH("tr", "Türkçe", "Türkçe", "🇹🇷"),
    ENGLISH("en", "English", "English", "🇬🇧");

    companion object {
        fun fromCode(code: String): AppLanguage = values().firstOrNull { it.code == code } ?: TURKISH
    }
}

val LocalAppLanguage = staticCompositionLocalOf { AppLanguage.TURKISH }

var globalCurrentLanguage: AppLanguage = AppLanguage.TURKISH

fun isGlobalEnglishLanguage(): Boolean = globalCurrentLanguage == AppLanguage.ENGLISH
