package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.PersonDetails

interface PersonRepository {
    suspend fun getPerson(id: Int, isCharacter: Boolean): AppResult<PersonDetails>
}
