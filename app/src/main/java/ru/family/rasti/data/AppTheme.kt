package ru.family.rasti.data

enum class AppTheme {
    LIGHT,
    DARK,
}

internal fun parseAppTheme(value: String?): AppTheme =
    runCatching { AppTheme.valueOf(value.orEmpty()) }.getOrDefault(AppTheme.LIGHT)
