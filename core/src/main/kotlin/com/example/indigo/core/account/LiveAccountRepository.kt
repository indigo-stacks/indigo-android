package com.example.indigo.core.account

import com.example.indigo.core.network.apiCall
import javax.inject.Inject
import kotlinx.serialization.json.Json
import retrofit2.http.GET

interface AccountApi {
    @GET("api/v1/account/profile") suspend fun profile(): UserProfile
}

class LiveAccountRepository
@Inject
constructor(private val api: AccountApi, private val json: Json) : AccountRepository {
    override suspend fun profile(): UserProfile = apiCall(json) { api.profile() }
}
