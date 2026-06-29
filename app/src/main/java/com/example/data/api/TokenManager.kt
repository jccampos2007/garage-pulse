package com.example.data.api

import android.content.Context
import android.content.SharedPreferences

/**
 * Manages JWT token persistence for API authentication.
 * Stores the token in SharedPreferences for offline access.
 */
object TokenManager {
    private const val PREFS_NAME = "garage_pulse_auth"
    private const val KEY_TOKEN = "jwt_token"
    private const val KEY_USER_ID = "api_user_id"

    private var prefs: SharedPreferences? = null

    fun init(context: Context) {
        prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    }

    fun saveToken(token: String) {
        prefs?.edit()?.putString(KEY_TOKEN, token)?.apply()
    }

    fun getToken(): String? {
        return prefs?.getString(KEY_TOKEN, null)
    }

    fun saveUserId(userId: Int) {
        prefs?.edit()?.putInt(KEY_USER_ID, userId)?.apply()
    }

    fun getUserId(): Int {
        return prefs?.getInt(KEY_USER_ID, -1) ?: -1
    }

    fun clearToken() {
        prefs?.edit()
            ?.remove(KEY_TOKEN)
            ?.remove(KEY_USER_ID)
            ?.apply()
    }

    fun isAuthenticated(): Boolean {
        return getToken() != null
    }
}
