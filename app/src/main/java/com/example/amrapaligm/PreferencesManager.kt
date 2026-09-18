package com.example.amrapaligm

import android.content.Context
import android.content.SharedPreferences

object PreferencesManager {
    private const val PREF_NAME = "visitor_management_prefs"
    private const val BASE_URL_KEY = "base_url"
    private const val DEFAULT_BASE_URL = "http://your-server-ip:2222/"

    private lateinit var preferences: SharedPreferences

    fun init(context: Context) {
        preferences = context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
    }

    fun getBaseUrl(): String {
        return preferences.getString(BASE_URL_KEY, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun setBaseUrl(url: String) {
        preferences.edit().putString(BASE_URL_KEY, url).apply()
    }

    fun resetToDefault() {
        preferences.edit().remove(BASE_URL_KEY).apply()
    }
}
