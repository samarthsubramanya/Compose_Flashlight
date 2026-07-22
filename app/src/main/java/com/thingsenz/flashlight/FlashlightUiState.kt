package com.thingsenz.flashlight

enum class FlashMode { NONE, SOS, STROBE }

data class FlashlightUiState(
    val isTorchOn: Boolean = false,
    val hasFlash: Boolean = true,
    val maxLevel: Int = 1,
    val level: Int = 1,
    val activeMode: FlashMode = FlashMode.NONE,
    val showInfoDialog: Boolean = false,
)
