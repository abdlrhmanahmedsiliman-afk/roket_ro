package com.example.service

import android.content.Context
import android.content.SharedPreferences
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

class PreferencesManager(context: Context) {
    private val prefs: SharedPreferences =
        context.getSharedPreferences("roket_ro_prefs", Context.MODE_PRIVATE)

    private val _favoriteTeams = MutableStateFlow<Set<String>>(loadFavoriteTeams())
    val favoriteTeams: StateFlow<Set<String>> = _favoriteTeams.asStateFlow()

    private val _favoriteMatches = MutableStateFlow<Set<String>>(loadFavoriteMatches())
    val favoriteMatches: StateFlow<Set<String>> = _favoriteMatches.asStateFlow()

    private val _selectedSport = MutableStateFlow(prefs.getString(KEY_SPORT, "all") ?: "all")
    val selectedSport: StateFlow<String> = _selectedSport.asStateFlow()

    private val _language = MutableStateFlow(prefs.getString(KEY_LANG, "ar") ?: "ar")
    val language: StateFlow<String> = _language.asStateFlow()

    private fun loadFavoriteTeams(): Set<String> {
        return prefs.getStringSet(KEY_FAV_TEAMS, setOf("ريال مدريد", "مانشستر سيتي", "الأهلي")) ?: emptySet()
    }

    private fun loadFavoriteMatches(): Set<String> {
        return prefs.getStringSet(KEY_FAV_MATCHES, emptySet()) ?: emptySet()
    }

    fun toggleFavoriteTeam(teamName: String) {
        val current = _favoriteTeams.value.toMutableSet()
        if (current.contains(teamName)) {
            current.remove(teamName)
        } else {
            current.add(teamName)
        }
        prefs.edit().putStringSet(KEY_FAV_TEAMS, current).apply()
        _favoriteTeams.value = current
    }

    fun isTeamFavorite(teamName: String): Boolean {
        return _favoriteTeams.value.contains(teamName)
    }

    fun toggleFavoriteMatch(matchId: String) {
        val current = _favoriteMatches.value.toMutableSet()
        if (current.contains(matchId)) {
            current.remove(matchId)
        } else {
            current.add(matchId)
        }
        prefs.edit().putStringSet(KEY_FAV_MATCHES, current).apply()
        _favoriteMatches.value = current
    }

    fun isMatchFavorite(matchId: String): Boolean {
        return _favoriteMatches.value.contains(matchId)
    }

    fun setSelectedSport(sportId: String) {
        prefs.edit().putString(KEY_SPORT, sportId).apply()
        _selectedSport.value = sportId
    }

    fun setLanguage(lang: String) {
        prefs.edit().putString(KEY_LANG, lang).apply()
        _language.value = lang
    }

    companion object {
        private const val KEY_FAV_TEAMS = "fav_teams"
        private const val KEY_FAV_MATCHES = "fav_matches"
        private const val KEY_SPORT = "selected_sport"
        private const val KEY_LANG = "language"
    }
}
