package com.example.service

import androidx.compose.ui.graphics.Color
import com.example.ui.theme.AppColors
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class AppNotification(
    val id: String,
    val title: String,
    val message: String,
    val timestamp: Long = System.currentTimeMillis(),
    val accentColor: Color = AppColors.NeonCyan,
    val isRead: Boolean = false,
    val matchId: String? = null
)

class NotificationCenter {
    private val _notifications = MutableStateFlow(generateInitialNotifications())
    val notifications: StateFlow<List<AppNotification>> = _notifications.asStateFlow()

    private val _unreadCount = MutableStateFlow(calculateUnread(_notifications.value))
    val unreadCount: StateFlow<Int> = _unreadCount.asStateFlow()

    private fun calculateUnread(list: List<AppNotification>): Int = list.count { !it.isRead }

    fun postNotification(title: String, message: String, color: Color = AppColors.NeonCyan, matchId: String? = null) {
        val newNotification = AppNotification(
            id = "notif_${System.currentTimeMillis()}",
            title = title,
            message = message,
            accentColor = color,
            matchId = matchId,
            isRead = false
        )
        val updated = listOf(newNotification) + _notifications.value
        _notifications.value = updated
        _unreadCount.value = calculateUnread(updated)
    }

    fun markAllAsRead() {
        val updated = _notifications.value.map { it.copy(isRead = true) }
        _notifications.value = updated
        _unreadCount.value = 0
    }

    fun clearAll() {
        _notifications.value = emptyList()
        _unreadCount.value = 0
    }

    private fun generateInitialNotifications(): List<AppNotification> {
        return listOf(
            AppNotification(
                id = "n_1",
                title = "⚽ هدف مباشر لـ ريال مدريد!",
                message = "سجل فينيسيوس جونيور الهدف الثاني لـ ريال مدريد (د68') بتسديدة متقنة.",
                accentColor = AppColors.NeonCyan,
                isRead = false
            ),
            AppNotification(
                id = "n_2",
                title = "⏱️ بداية الشوط الثاني",
                message = "انطلقت أحداث الشوط الثاني لمباراة الهلال والاتحاد في قمة الجولة.",
                accentColor = AppColors.NeonBlue,
                isRead = false
            ),
            AppNotification(
                id = "n_3",
                title = "🔴 بطاقة حمراء!",
                message = "طرد مباشر لمدافع الفريق المنافس بعد تدخل قوي قرب منطقة الجزاء.",
                accentColor = AppColors.NeonRed,
                isRead = true
            ),
            AppNotification(
                id = "n_4",
                title = "🔔 تذكير بمباراة قمة مرتقبة",
                message = "تنطلق بعد 30 دقيقة قمة ليفربول ومانشستر سيتي في الدوري الإنجليزي الممتاز.",
                accentColor = AppColors.Coral,
                isRead = true
            )
        )
    }
}
