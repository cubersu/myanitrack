package com.myanitrack.feature.calendar

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.ScheduleRepository
import com.myanitrack.core.model.ScheduleEntry
import com.myanitrack.core.model.WeeklySchedule
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.DayOfWeek
import java.time.ZoneId
import java.time.ZonedDateTime
import javax.inject.Inject
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class CalendarUiState(
    val schedule: WeeklySchedule = WeeklySchedule(),
    val selectedDay: DayOfWeek = ZonedDateTime.now().dayOfWeek,
    val onlyMyList: Boolean = false,
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val error: AppError? = null,
) {
    /** Secili gunun, "yalnizca listem" filtresine gore suzulmus kayitlari. */
    val visibleEntries: List<ScheduleEntry>
        get() = schedule[selectedDay]
            .filter { !onlyMyList || it.isInMyList }
            .sortedWith(
                // Yayin saati bilinenler once, kendi aralarinda saate gore.
                compareBy<ScheduleEntry> { it.nextAiringAt == null }
                    .thenBy { it.nextAiringAt }
                    .thenBy(String.CASE_INSENSITIVE_ORDER) { it.node.title },
            )

    val isEmpty: Boolean get() = visibleEntries.isEmpty() && !isLoading
}

@HiltViewModel
class CalendarViewModel @Inject constructor(
    private val scheduleRepository: ScheduleRepository,
) : ViewModel() {

    private val _uiState = MutableStateFlow(
        CalendarUiState(selectedDay = ZonedDateTime.now(ZoneId.systemDefault()).dayOfWeek),
    )
    val uiState: StateFlow<CalendarUiState> = _uiState.asStateFlow()

    init {
        load(forceRefresh = false)
    }

    fun selectDay(day: DayOfWeek) {
        _uiState.update { it.copy(selectedDay = day) }
    }

    fun toggleOnlyMyList() {
        _uiState.update { it.copy(onlyMyList = !it.onlyMyList) }
    }

    fun refresh() = load(forceRefresh = true)

    private fun load(forceRefresh: Boolean) {
        _uiState.update {
            it.copy(
                isLoading = it.schedule.isEmpty,
                isRefreshing = !it.schedule.isEmpty,
                error = null,
            )
        }
        viewModelScope.launch {
            when (val result = scheduleRepository.getWeek(forceRefresh)) {
                is AppResult.Success -> _uiState.update {
                    it.copy(schedule = result.data, isLoading = false, isRefreshing = false)
                }

                is AppResult.Failure -> _uiState.update {
                    it.copy(isLoading = false, isRefreshing = false, error = result.error)
                }
            }
        }
    }
}
