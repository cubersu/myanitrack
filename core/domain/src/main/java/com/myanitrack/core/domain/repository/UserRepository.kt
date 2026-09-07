package com.myanitrack.core.domain.repository

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.model.UserProfile

interface UserRepository {

    suspend fun getMyProfile(): AppResult<UserProfile>

    suspend fun getProfile(userName: String): AppResult<UserProfile>
}
