package com.example.data.provider

/**
 * High-integrity tri-state network/data result.
 * Enforces explicit representation of Success with Data, Success with No Data (Empty),
 * and Failure with precise cause (Error).
 */
sealed class DataResult<out T> {
    data class Success<out T>(
        val data: T,
        val source: String,
        val cached: Boolean = false,
        val quotaRemaining: Int? = null
    ) : DataResult<T>()

    data class Empty(
        val source: String,
        val message: String = "غير متاح في المصدر"
    ) : DataResult<Nothing>()

    data class Error(
        val errorType: ErrorType,
        val message: String,
        val source: String,
        val httpCode: Int? = null,
        val throwable: Throwable? = null
    ) : DataResult<Nothing>()

    fun getOrNull(): T? = when (this) {
        is Success -> data
        else -> null
    }

    val isSuccess: Boolean get() = this is Success
    val isEmpty: Boolean get() = this is Empty
    val isError: Boolean get() = this is Error
}

enum class ErrorType(val labelAr: String) {
    NETWORK_ERROR("خطأ في الاتصال بالشبكة"),
    RATE_LIMITED("تم تجاوز حد الطلبات المسموح (Rate Limit 429)"),
    QUOTA_EXHAUSTED("تم استهلاك الحصة اليومية للمزود"),
    MISSING_OR_INVALID_KEY("مفتاح API غير مهيأ أو غير صالح"),
    MALFORMED_JSON("استجابة غير صالحة من المصدر (JSON تالف)"),
    NOT_FOUND("البيانات المطلوبة غير موجودة في المصدر (404)"),
    UNKNOWN("خطأ غير معروف")
}
