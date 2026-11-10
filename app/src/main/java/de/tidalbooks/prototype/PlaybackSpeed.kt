package de.tidalbooks.prototype

internal val playbackSpeeds = listOf(0.5f, 0.75f, 1f, 1.25f, 1.5f, 1.75f, 2f)

internal fun validPlaybackSpeed(value: Float) = value.takeIf { it in playbackSpeeds } ?: 1f

internal fun playbackSpeedLabel(value: Float): String =
    value.toString().removeSuffix(".0").let { if (AppLanguage.english) it else it.replace('.', ',') } + "×"
