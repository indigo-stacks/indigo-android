package com.example.indigo.core

import com.example.indigo.core.network.ApiErrorBody
import com.example.indigo.core.network.ApiException
import kotlinx.serialization.json.Json
import org.junit.Assert.*
import org.junit.Test

class ApiErrorBodyTest {
    @Test
    fun backendErrorFieldIsExposedAsMessage() {
        val body = Json.decodeFromString<ApiErrorBody>("""{"error":"Email not verified"}""")
        assertEquals("Email not verified", body.message)
        assertNull(body.code)
        assertEquals("Email not verified", ApiException(401, body).message)
    }

    @Test
    fun middlewareCodeIsOptionalAndHttpFallbackIsAvailable() {
        val body =
            Json.decodeFromString<ApiErrorBody>("""{"error":"Unauthorized","code":"JWT_EXPIRED"}""")
        assertEquals("JWT_EXPIRED", body.code)
        assertEquals("HTTP 503", ApiException(503, null).message)
    }
}
