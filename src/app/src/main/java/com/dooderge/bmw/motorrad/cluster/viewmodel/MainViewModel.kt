package com.dooderge.bmw.motorrad.cluster.viewmodel

import androidx.lifecycle.ViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class MainUiState(
    val isConnected: Boolean = false,
    val receivedData: String = "",
    val error: String? = null
)

class MainViewModel : ViewModel() {
    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    fun updateConnectionState(isConnected: Boolean) {
        _uiState.value = _uiState.value.copy(isConnected = isConnected)
    }

    fun updateReceivedData(data: String) {
        _uiState.value = _uiState.value.copy(receivedData = data)
    }

    fun updateError(error: String?) {
        _uiState.value = _uiState.value.copy(error = error)
    }
} 