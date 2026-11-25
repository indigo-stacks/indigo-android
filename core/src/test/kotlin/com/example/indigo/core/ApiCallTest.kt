package com.example.indigo.core

import com.example.indigo.core.network.apiCall
import java.io.IOException
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.SerializationException
import kotlinx.serialization.json.Json
import org.junit.Assert.assertSame
import org.junit.Assert.fail
import org.junit.Test

class ApiCallTest {
    @Test
    fun `unexpected API failures become IO errors with the original cause`() = runTest {
        for (failure in
            listOf(
                SerializationException("Malformed response"),
                IllegalStateException("Invalid response"),
            )) {
            try {
                apiCall<Unit>(Json) { throw failure }
                fail("Expected IOException")
            } catch (error: IOException) {
                assertSame(failure, error.cause)
            }
        }
    }

    @Test
    fun `cancellation and transport errors propagate unchanged`() = runTest {
        for (failure in listOf(CancellationException("Cancelled"), IOException("Offline"))) {
            try {
                apiCall<Unit>(Json) { throw failure }
                fail("Expected original failure")
            } catch (error: Exception) {
                assertSame(failure, error)
            }
        }
    }
}
