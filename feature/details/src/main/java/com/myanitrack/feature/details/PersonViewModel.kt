package com.myanitrack.feature.details

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope

import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.PersonRepository
import com.myanitrack.core.model.PersonDetails
import com.myanitrack.feature.details.navigation.PersonDestination
import dagger.hilt.android.lifecycle.HiltViewModel
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

data class PersonUiState(val loading: Boolean = true, val person: PersonDetails? = null, val error: AppError? = null)

@HiltViewModel
class PersonViewModel @Inject constructor(private val repository: PersonRepository, savedStateHandle: SavedStateHandle) : ViewModel() {
    private val args = PersonDestination(id = checkNotNull(savedStateHandle.get<Int>("id")), isCharacter = checkNotNull(savedStateHandle.get<Boolean>("isCharacter")))
    private val state = MutableStateFlow(PersonUiState())
    val uiState = state.asStateFlow()
    private var job: kotlinx.coroutines.Job? = null
    init { refresh() }
    fun refresh() {
        if (job?.isActive == true) return
        job = viewModelScope.launch {
            state.value = PersonUiState()
            val result = repository.getPerson(args.id, args.isCharacter)
            state.value = PersonUiState(loading = false, person = (result as? AppResult.Success)?.data, error = (result as? AppResult.Failure)?.error)
        }
    }
}
