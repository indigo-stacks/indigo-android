package com.example.indigo.core.network

import java.io.IOException
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

class ApiException(val statusCode: Int, val body: ApiErrorBody?) :
    IOException(body?.message ?: "HTTP $statusCode")

/** Backend errors use `error`, not `message`; JWT middleware can also return `code`. */
@Serializable
data class ApiErrorBody(@SerialName("error") val message: String? = null, val code: String? = null)
