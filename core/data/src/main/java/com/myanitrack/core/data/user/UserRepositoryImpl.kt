package com.myanitrack.core.data.user

import com.myanitrack.core.common.result.AppResult
import com.myanitrack.core.common.result.map
import com.myanitrack.core.domain.repository.UserRepository
import com.myanitrack.core.model.UserProfile
import com.myanitrack.core.network.mal.MalApiService
import com.myanitrack.core.network.mal.mapper.toDomain
import com.myanitrack.core.network.util.safeApiCall
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UserRepositoryImpl @Inject constructor(
    private val apiService: MalApiService,
) : UserRepository {

    override suspend fun getMyProfile(): AppResult<UserProfile> =
        safeApiCall { apiService.getMyUser() }.map { it.toDomain() }

    override suspend fun getProfile(userName: String): AppResult<UserProfile> =
        safeApiCall { apiService.getUser(userName) }.map { it.toDomain() }
}
