package com.example.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

data class UserProfile(
    val id: String = "founder_abdelrahman",
    val displayName: String = "عبد الرحمن أحمد عبد الهادي",
    val title: String = "المؤسس • Founder",
    val email: String = "abdelrahman.ahmed@roketro.app",
    val photoUrl: String = "",
    val phoneNumber: String = "+20 100 000 0000",
    val isGoogleUser: Boolean = true,
    val isOnboarded: Boolean = true
)

class AuthManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("roket_ro_auth", Context.MODE_PRIVATE)

    private val _userProfile = MutableStateFlow(loadProfile())
    val userProfile: StateFlow<UserProfile> = _userProfile.asStateFlow()

    private fun loadProfile(): UserProfile {
        val id = prefs.getString("user_id", "founder_abdelrahman") ?: "founder_abdelrahman"
        val name = prefs.getString("user_name", "عبد الرحمن أحمد عبد الهادي") ?: "عبد الرحمن أحمد عبد الهادي"
        val email = prefs.getString("user_email", "abdelrahman.ahmed@roketro.app") ?: "abdelrahman.ahmed@roketro.app"
        val photo = prefs.getString("user_photo", "") ?: ""
        val phone = prefs.getString("user_phone", "") ?: ""
        val isGoogle = prefs.getBoolean("is_google", true)
        val isOnboarded = prefs.getBoolean("is_onboarded", true)

        return UserProfile(
            id = id,
            displayName = name,
            email = email,
            photoUrl = photo,
            phoneNumber = phone,
            isGoogleUser = isGoogle,
            isOnboarded = isOnboarded
        )
    }

    fun signInWithGoogle(displayName: String, email: String, photoUrl: String = "") {
        val updated = UserProfile(
            id = "google_${System.currentTimeMillis()}",
            displayName = displayName,
            email = email,
            photoUrl = photoUrl,
            phoneNumber = _userProfile.value.phoneNumber,
            isGoogleUser = true,
            isOnboarded = true
        )
        saveProfile(updated)
    }

    fun completeOnboarding(name: String, phone: String) {
        val current = _userProfile.value
        val updated = current.copy(
            displayName = if (name.isNotBlank()) name else current.displayName,
            phoneNumber = phone,
            isOnboarded = true
        )
        saveProfile(updated)
    }

    fun signOut() {
        val guest = UserProfile()
        saveProfile(guest)
    }

    private fun saveProfile(profile: UserProfile) {
        prefs.edit()
            .putString("user_id", profile.id)
            .putString("user_name", profile.displayName)
            .putString("user_email", profile.email)
            .putString("user_photo", profile.photoUrl)
            .putString("user_phone", profile.phoneNumber)
            .putBoolean("is_google", profile.isGoogleUser)
            .putBoolean("is_onboarded", profile.isOnboarded)
            .apply()
        _userProfile.value = profile
    }
}
