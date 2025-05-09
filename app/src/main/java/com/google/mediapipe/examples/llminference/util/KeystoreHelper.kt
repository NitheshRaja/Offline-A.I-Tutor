package com.google.mediapipe.examples.llminference.util

import android.content.Context
import android.content.pm.PackageManager
import android.util.Log

/**
 * Helper class to display keystore fingerprints that are needed for Firebase Google Sign-In
 */
object KeystoreHelper {
    private const val TAG = "KeystoreHelper"

    /**
     * Logs the SHA-1 and SHA-256 fingerprints for the app's signing certificate
     * These fingerprints need to be added to the Firebase console for Google Sign-In to work
     */
    fun logKeyHashesForDebug(context: Context) {
        try {
            val info = context.packageManager.getPackageInfo(
                context.packageName,
                PackageManager.GET_SIGNATURES
            )
            for (signature in info.signatures) {
                val md = java.security.MessageDigest.getInstance("SHA")
                md.update(signature.toByteArray())
                val hashKey = android.util.Base64.encodeToString(md.digest(), android.util.Base64.DEFAULT)
                Log.d(TAG, "KeyHash SHA-1: $hashKey")
                
                val md256 = java.security.MessageDigest.getInstance("SHA-256")
                md256.update(signature.toByteArray())
                val hashKey256 = android.util.Base64.encodeToString(md256.digest(), android.util.Base64.DEFAULT)
                Log.d(TAG, "KeyHash SHA-256: $hashKey256")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error getting key hashes", e)
        }
    }
} 