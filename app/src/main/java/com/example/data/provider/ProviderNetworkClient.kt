package com.example.data.provider

import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit

/**
 * Shared, defensive network client with:
 * 1. Strict concurrency throttle: Maximum 4 simultaneous requests across all sports providers.
 * 2. In-memory caching with TTL (Time To Live).
 * 3. Safe response parsing and detailed error mapping into [DataResult].
 */
object ProviderNetworkClient {

    // Maximum 4 concurrent requests as required by specification
    private val concurrencySemaphore = Semaphore(4)

    val okHttpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(8, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    private val cache = ConcurrentHashMap<String, CacheEntry>()

    private data class CacheEntry(
        val body: String,
        val headers: Map<String, String>,
        val expiresAt: Long
    )

    fun getCached(key: String): Pair<String, Map<String, String>>? {
        val entry = cache[key] ?: return null
        if (System.currentTimeMillis() > entry.expiresAt) {
            cache.remove(key)
            return null
        }
        return entry.body to entry.headers
    }

    fun putCache(key: String, body: String, headers: Map<String, String> = emptyMap(), ttlMillis: Long) {
        cache[key] = CacheEntry(
            body = body,
            headers = headers,
            expiresAt = System.currentTimeMillis() + ttlMillis
        )
    }

    suspend fun executeSafe(
        request: Request,
        sourceName: String,
        cacheKey: String? = null,
        cacheTtlMillis: Long = 0L
    ): DataResult<Pair<String, Map<String, String>>> = withContext(Dispatchers.IO) {
        // 1. Check cache first
        if (cacheKey != null && cacheTtlMillis > 0L) {
            val hit = getCached(cacheKey)
            if (hit != null) {
                return@withContext DataResult.Success(hit, source = sourceName, cached = true)
            }
        }

        // 2. Throttle concurrency to max 4 concurrent requests
        concurrencySemaphore.withPermit {
            try {
                okHttpClient.newCall(request).execute().use { response: Response ->
                    val code = response.code
                    val headersMap = mutableMapOf<String, String>()
                    for (name in response.headers.names()) {
                        headersMap[name.lowercase()] = response.header(name) ?: ""
                    }

                    val body = response.body?.string().orEmpty()

                    when (code) {
                        in 200..299 -> {
                            if (body.isBlank() || body == "[]" || body == "{}") {
                                DataResult.Empty(source = sourceName)
                            } else {
                                if (cacheKey != null && cacheTtlMillis > 0L) {
                                    putCache(cacheKey, body, headersMap, cacheTtlMillis)
                                }
                                DataResult.Success(
                                    data = body to headersMap,
                                    source = sourceName,
                                    cached = false,
                                    quotaRemaining = headersMap["x-ratelimit-requests-remaining"]?.toIntOrNull()
                                        ?: headersMap["x-requests-available"]?.toIntOrNull()
                                )
                            }
                        }
                        401, 403 -> {
                            DataResult.Error(
                                errorType = ErrorType.MISSING_OR_INVALID_KEY,
                                message = "مفتاح API غير صالح أو غير مصرح له بالوصول لهذا المورد ($code)",
                                source = sourceName,
                                httpCode = code
                            )
                        }
                        404 -> {
                            DataResult.Error(
                                errorType = ErrorType.NOT_FOUND,
                                message = "المورد المطلوب غير موجود (404)",
                                source = sourceName,
                                httpCode = code
                            )
                        }
                        429 -> {
                            val retryAfter = response.header("Retry-After")
                            val reset = response.header("X-RequestCounter-Reset")
                            val msg = "تم تجاوز حد الطلبات المسموح (Rate Limit 429). يرجى الانتظار ${retryAfter ?: reset ?: "قليلاً"}."
                            DataResult.Error(
                                errorType = ErrorType.RATE_LIMITED,
                                message = msg,
                                source = sourceName,
                                httpCode = code
                            )
                        }
                        else -> {
                            DataResult.Error(
                                errorType = ErrorType.UNKNOWN,
                                message = "فشل الطلب مع كود HTTP: $code",
                                source = sourceName,
                                httpCode = code
                            )
                        }
                    }
                }
            } catch (e: Exception) {
                DataResult.Error(
                    errorType = ErrorType.NETWORK_ERROR,
                    message = "تعذر الاتصال بـ $sourceName: ${e.localizedMessage ?: "خطأ في الشبكة"}",
                    source = sourceName,
                    throwable = e
                )
            }
        }
    }
}
