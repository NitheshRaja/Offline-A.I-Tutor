package com.google.mediapipe.examples.llminference

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

object SecureStorage {
  private const val PREF_NAME = "secure_storage"
  private const val KEY_USERNAME = "username"
  private const val KEY_EMAIL = "email"
  private const val KEY_ACCOUNT_TYPE = "account_type"

  private fun getPrefs(context: Context): SharedPreferences {
    return context.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE)
  }

  fun getUserInfo(context: Context): UserInfo? {
    val prefs = getPrefs(context)
    val username = prefs.getString(KEY_USERNAME, null)
    val email = prefs.getString(KEY_EMAIL, null)
    val accountType = prefs.getString(KEY_ACCOUNT_TYPE, null)

    return if (username != null && email != null && accountType != null) {
      UserInfo(username, email, accountType)
    } else {
      null
    }
  }

  fun saveUserInfo(context: Context, userInfo: UserInfo) {
    getPrefs(context).edit().apply {
      putString(KEY_USERNAME, userInfo.username)
      putString(KEY_EMAIL, userInfo.email)
      putString(KEY_ACCOUNT_TYPE, userInfo.accountType)
      apply()
    }
  }

  fun clear(context: Context) {
    getPrefs(context).edit().clear().apply()
  }

  private const val PREFS_NAME = "secure_prefs"
  private const val KEY_ACCESS_TOKEN = "access_token"
  private const val KEY_CODE_VERIFIER = "code_verifier"

  fun saveCodeVerifier(context: Context, codeVerifier: String) {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    prefs.edit().putString(KEY_CODE_VERIFIER, codeVerifier).apply()
  }

  fun getCodeVerifier(context: Context): String? {
    val prefs = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    return prefs.getString(KEY_CODE_VERIFIER, null)
  }

  fun saveToken(context: Context, token: String) {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val sharedPreferences = EncryptedSharedPreferences.create(
      PREFS_NAME,
      masterKeyAlias,
      context,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    sharedPreferences.edit().putString(KEY_ACCESS_TOKEN, token).apply()
  }

  fun getToken(context: Context): String? {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val sharedPreferences = EncryptedSharedPreferences.create(
      PREFS_NAME,
      masterKeyAlias,
      context,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    return sharedPreferences.getString(KEY_ACCESS_TOKEN, null)
  }

  fun removeToken(context: Context) {
    val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
    val sharedPreferences = EncryptedSharedPreferences.create(
      PREFS_NAME,
      masterKeyAlias,
      context,
      EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
      EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
    )
    sharedPreferences.edit().remove(KEY_ACCESS_TOKEN).apply()
  }
}
