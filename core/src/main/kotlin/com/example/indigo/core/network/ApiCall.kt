package com.example.indigo.core.network

import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.serialization.json.Json
import retrofit2.HttpException

suspend fun <T> apiCall(json: Json, block: suspend () -> T): T =
    try {
        block()
    } catch (error: HttpException) {
        val body = error.response()?.errorBody()?.use { it.string() }
        throw ApiException(
            error.code(),
            body?.let { runCatching { json.decodeFromString<ApiErrorBody>(it) }.getOrNull() },
        )
    } catch (error: CancellationException) {
        throw error
    } catch (error: IOException) {
        throw error
    } catch (error: Exception) {
        throw IOException("Could not process API response", error)
    }
