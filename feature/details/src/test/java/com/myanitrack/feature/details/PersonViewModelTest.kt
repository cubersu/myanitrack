package com.myanitrack.feature.details

import androidx.lifecycle.SavedStateHandle
import com.myanitrack.core.common.result.AppError
import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.domain.repository.PersonRepository
import com.myanitrack.core.model.PersonDetails
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.*
import org.junit.jupiter.api.AfterEach
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*

@OptIn(ExperimentalCoroutinesApi::class)
class PersonViewModelTest {
    private val repository = mockk<PersonRepository>()
    private val person = PersonDetails("Edward", null, "Biography", null, 12)
    @BeforeEach fun setup() { Dispatchers.setMain(StandardTestDispatcher()) }
    @AfterEach fun teardown() { Dispatchers.resetMain() }

    @Test fun `character request recovers after failure`() = runTest {
        coEvery { repository.getPerson(11, true) } returnsMany listOf(
            AppResult.Failure(AppError.Serialization("missing data")), AppResult.Success(person),
        )
        val vm = PersonViewModel(repository, SavedStateHandle(mapOf("id" to 11, "isCharacter" to true)))
        advanceUntilIdle()
        assertFalse(vm.uiState.value.loading)
        assertNull(vm.uiState.value.person)
        vm.refresh()
        advanceUntilIdle()
        assertEquals(person, vm.uiState.value.person)
    }

    @Test fun `staff request uses people endpoint and ignores duplicate refresh`() = runTest {
        coEvery { repository.getPerson(22, false) } returns AppResult.Success(person)
        val vm = PersonViewModel(repository, SavedStateHandle(mapOf("id" to 22, "isCharacter" to false)))
        vm.refresh()
        advanceUntilIdle()
        coVerify(exactly = 1) { repository.getPerson(22, false) }
        assertEquals(person, vm.uiState.value.person)
    }
}
