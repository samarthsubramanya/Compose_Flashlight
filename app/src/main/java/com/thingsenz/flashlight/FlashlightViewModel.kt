package com.thingsenz.flashlight

import android.app.Application
import android.hardware.camera2.CameraAccessException
import android.hardware.camera2.CameraCharacteristics
import android.hardware.camera2.CameraManager
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

private const val TAG = "FlashlightViewModel"
private const val STROBE_INTERVAL_MS = 70L
private const val SOS_UNIT_MS = 200L

class FlashlightViewModel(application: Application) : AndroidViewModel(application) {

    private val cameraManager: CameraManager? =
        application.getSystemService(CameraManager::class.java)
    private var cameraId: String? = null
    private var modeJob: Job? = null

    private val _uiState = MutableStateFlow(FlashlightUiState())
    val uiState: StateFlow<FlashlightUiState> = _uiState.asStateFlow()

    private val torchCallback = object : CameraManager.TorchCallback() {
        override fun onTorchModeChanged(camId: String, enabled: Boolean) {
            if (camId == cameraId) _uiState.update { it.copy(isTorchOn = enabled) }
        }

        override fun onTorchModeUnavailable(camId: String) {
            if (camId == cameraId) _uiState.update { it.copy(isTorchOn = false) }
        }

        @RequiresApi(Build.VERSION_CODES.TIRAMISU)
        override fun onTorchStrengthLevelChanged(camId: String, newStrengthLevel: Int) {
            if (camId == cameraId) _uiState.update { it.copy(level = newStrengthLevel) }
        }
    }

    init {
        detectFlashCamera()
        cameraManager?.registerTorchCallback(torchCallback, null)
    }

    private fun detectFlashCamera() {
        val manager = cameraManager ?: run {
            _uiState.update { it.copy(hasFlash = false) }
            return
        }
        try {
            val id = manager.cameraIdList.firstOrNull { candidate ->
                manager.getCameraCharacteristics(candidate)
                    .get(CameraCharacteristics.FLASH_INFO_AVAILABLE) == true
            }
            if (id == null) {
                _uiState.update { it.copy(hasFlash = false) }
                return
            }
            cameraId = id
            val characteristics = manager.getCameraCharacteristics(id)
            val maxLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                (characteristics.get(CameraCharacteristics.FLASH_INFO_STRENGTH_MAXIMUM_LEVEL) ?: 1)
                    .coerceAtLeast(1)
            } else 1
            val defaultLevel = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
                manager.getTorchStrengthLevel(id).coerceAtLeast(1)
            } else 1
            _uiState.update { it.copy(hasFlash = true, maxLevel = maxLevel, level = defaultLevel) }
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Unable to access camera", e)
            _uiState.update { it.copy(hasFlash = false) }
        }
    }

    fun onTorchTap() {
        if (!_uiState.value.hasFlash) return
        stopMode()
        setTorch(!_uiState.value.isTorchOn)
    }

    fun setLevel(level: Int) {
        val manager = cameraManager ?: return
        val id = cameraId ?: return
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val clamped = level.coerceIn(1, _uiState.value.maxLevel)
        try {
            manager.turnOnTorchWithStrengthLevel(id, clamped)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Unable to set torch level", e)
        }
    }

    fun onSosTap() = toggleMode(FlashMode.SOS) { runPattern(sosPattern()) }

    fun onStrobeTap() = toggleMode(FlashMode.STROBE) { runStrobe() }

    fun openInfoDialog() = _uiState.update { it.copy(showInfoDialog = true) }

    fun dismissInfoDialog() = _uiState.update { it.copy(showInfoDialog = false) }

    private fun toggleMode(mode: FlashMode, block: suspend () -> Unit) {
        if (!_uiState.value.hasFlash) return
        if (_uiState.value.activeMode == mode) {
            stopMode()
            return
        }
        modeJob?.cancel()
        _uiState.update { it.copy(activeMode = mode) }
        modeJob = viewModelScope.launch { block() }
    }

    private fun stopMode() {
        modeJob?.cancel()
        modeJob = null
        _uiState.update { it.copy(activeMode = FlashMode.NONE) }
        setTorch(false)
    }

    private fun setTorch(on: Boolean) {
        val manager = cameraManager ?: return
        val id = cameraId ?: return
        try {
            manager.setTorchMode(id, on)
        } catch (e: CameraAccessException) {
            Log.e(TAG, "Unable to set torch mode", e)
        }
    }

    private suspend fun runStrobe() {
        while (currentCoroutineContext().isActive) {
            setTorch(true)
            delay(STROBE_INTERVAL_MS)
            setTorch(false)
            delay(STROBE_INTERVAL_MS)
        }
    }

    private suspend fun runPattern(pattern: List<Pair<Boolean, Long>>) {
        while (currentCoroutineContext().isActive) {
            for ((isOn, durationMs) in pattern) {
                setTorch(isOn)
                delay(durationMs)
            }
        }
    }

    override fun onCleared() {
        modeJob?.cancel()
        cameraManager?.unregisterTorchCallback(torchCallback)
        setTorch(false)
        super.onCleared()
    }

    /** Morse S-O-S (dot dot dot / dash dash dash / dot dot dot), looping with a trailing word gap. */
    private fun sosPattern(): List<Pair<Boolean, Long>> {
        val dot = SOS_UNIT_MS
        val dash = SOS_UNIT_MS * 3
        val gap = SOS_UNIT_MS
        val letterGap = SOS_UNIT_MS * 3
        val wordGap = SOS_UNIT_MS * 7
        val pattern = mutableListOf<Pair<Boolean, Long>>()

        fun letter(vararg symbols: Long) {
            symbols.forEachIndexed { index, duration ->
                pattern += true to duration
                if (index != symbols.lastIndex) pattern += false to gap
            }
            pattern += false to letterGap
        }

        letter(dot, dot, dot)
        letter(dash, dash, dash)
        letter(dot, dot, dot)
        pattern[pattern.lastIndex] = false to wordGap
        return pattern
    }
}
