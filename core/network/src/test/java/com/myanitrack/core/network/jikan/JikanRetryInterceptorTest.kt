package com.myanitrack.core.network.jikan

import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import java.io.IOException
import okhttp3.Interceptor
import okhttp3.Protocol
import okhttp3.Request
import okhttp3.Response
import okhttp3.ResponseBody.Companion.toResponseBody
import org.junit.jupiter.api.Assertions.*
import org.junit.jupiter.api.Test

class JikanRetryInterceptorTest {
    private val request = Request.Builder().url("https://api.jikan.moe/v4/anime/1/full").build()
    private val chain = mockk<Interceptor.Chain>(relaxed = true).also {
        every { it.request() } returns request
        every { it.call().isCanceled() } returns false
    }
    private fun response(code: Int) = Response.Builder().request(request)
        .protocol(Protocol.HTTP_1_1).code(code).message("test").body("{}".toResponseBody()).build()

    @Test
    fun `upstream timeout is returned without another scrape`() {
        every { chain.proceed(request) } returns response(504)
        val sleeps = mutableListOf<Long>()
        JikanRetryInterceptor { sleeps += it }.intercept(chain).use { assertEquals(504, it.code) }
        verify(exactly = 1) { chain.proceed(request) }
        assertTrue(sleeps.isEmpty())
    }

    @Test
    fun `rate limited request can recover on one retry`() {
        every { chain.proceed(request) } returnsMany listOf(response(429), response(200))
        JikanRetryInterceptor { }.intercept(chain).use { assertEquals(200, it.code) }
        verify(exactly = 2) { chain.proceed(request) }
    }

    @Test
    fun `cancellation during backoff prevents another attempt`() {
        every { chain.proceed(request) } returns response(429)
        val subject = JikanRetryInterceptor { every { chain.call().isCanceled() } returns true }
        assertThrows(IOException::class.java) { subject.intercept(chain) }
        verify(exactly = 1) { chain.proceed(request) }
    }
}
