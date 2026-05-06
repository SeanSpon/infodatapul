package com.fitnessdatapuller.mobile

import android.content.Context
import androidx.core.content.edit

private const val DEFAULT_API_BASE_URL = "https://fitnessdatapuller.vercel.app"

data class SyncSettings(
    val apiBaseUrl: String = DEFAULT_API_BASE_URL,
    val syncApiKey: String = "",
)

class SettingsStore(context: Context) {
    private val preferences = context.getSharedPreferences("fitnessdatapuller_settings", Context.MODE_PRIVATE)

    fun load(): SyncSettings = SyncSettings(
        apiBaseUrl = preferences.getString(KEY_API_BASE_URL, DEFAULT_API_BASE_URL) ?: DEFAULT_API_BASE_URL,
        syncApiKey = preferences.getString(KEY_SYNC_API_KEY, "") ?: "",
    )

    fun save(settings: SyncSettings) {
        preferences.edit {
            putString(KEY_API_BASE_URL, settings.apiBaseUrl.trim().trimEnd('/'))
            putString(KEY_SYNC_API_KEY, settings.syncApiKey.trim())
        }
    }

    private companion object {
        const val KEY_API_BASE_URL = "api_base_url"
        const val KEY_SYNC_API_KEY = "sync_api_key"
    }
}
