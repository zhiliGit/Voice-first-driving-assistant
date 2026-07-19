package com.zhiligit.voicefirstdrivingassistant

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.zhiligit.voicefirstdrivingassistant.data.AgentRepository
import com.zhiligit.voicefirstdrivingassistant.model.ActionPlan
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class MainUiState(
    val request: String = "Create a note titled Project Idea containing Voice-first driving assistant.",
    val plan: ActionPlan? = null,
    val isLoading: Boolean = false,
    val result: String? = null,
    val error: String? = null,
    val isMockMode: Boolean = true
)

class MainViewModel : ViewModel() {
    private val repository = AgentRepository(BuildConfig.AGENT_BASE_URL)
    private val _state = MutableStateFlow(MainUiState(isMockMode = repository.isMockMode))
    val state: StateFlow<MainUiState> = _state.asStateFlow()

    fun updateRequest(value: String) {
        _state.value = _state.value.copy(request = value, plan = null, result = null, error = null)
    }

    fun createPlan() {
        val request = _state.value.request.trim()
        if (request.isEmpty()) return
        viewModelScope.launch {
            _state.value = _state.value.copy(isLoading = true, plan = null, result = null, error = null)
            runCatching { repository.createPlan(request) }
                .onSuccess { plan -> _state.value = _state.value.copy(isLoading = false, plan = plan) }
                .onFailure { error ->
                    _state.value = _state.value.copy(
                        isLoading = false,
                        error = error.message ?: "Unable to create plan"
                    )
                }
        }
    }

    fun confirm() {
        val count = _state.value.plan?.actions?.size ?: return
        _state.value = _state.value.copy(plan = null, result = "$count action(s) confirmed")
    }

    fun cancel() {
        _state.value = _state.value.copy(plan = null, result = "Actions cancelled")
    }
}
