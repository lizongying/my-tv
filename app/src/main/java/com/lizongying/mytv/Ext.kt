@file:Suppress("DEPRECATION")

package com.lizongying.mytv

import android.content.Context
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import java.security.MessageDigest

private const val TAG = "Extensions"

private val Context.packageInfo: PackageInfo
    get() {
        val flag = if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            PackageManager.GET_SIGNATURES
        } else {
            PackageManager.GET_SIGNING_CERTIFICATES
        }
        return if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) {
            packageManager.getPackageInfo(packageName, flag)
        } else {
            packageManager.getPackageInfo(
                packageName,
                PackageManager.PackageInfoFlags.of(PackageManager.GET_SIGNING_CERTIFICATES.toLong())
            )
        }
    }

/**
 * Return the version code of the app which is defined in build.gradle.
 * eg:100
 */
val Context.appVersionCode: Long
    get() {
        val packageInfo = this.packageInfo
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
    }

/**
 * Return the version name of the app which is defined in build.gradle.
 * eg:1.0.0
 */
val Context.appVersionName: String get() = packageInfo.versionName

val Context.appSignature: String
    get() {
        val packageInfo = this.packageInfo
        var sign: Signature? = null
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.P) {
            val signatures: Array<out Signature>? = packageInfo.signatures
            if (signatures != null) {
                sign = signatures[0]
            }
        } else {
            val signingInfo: SigningInfo? = packageInfo.signingInfo
            if (signingInfo != null) {
                sign = signingInfo.apkContentsSigners[0]
            }
        }
        if (sign == null) {
            return ""
        }
        return hashSignature(sign)
    }


val Context.isNetworkConnected: Boolean
    get() {
        val connectivityManager =
            this.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            val network = connectivityManager.activeNetwork
            val networkCapabilities = connectivityManager.getNetworkCapabilities(network)
            return networkCapabilities != null &&
                    (networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) ||
                            networkCapabilities.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET))
        } else {
            val networkInfo = connectivityManager.activeNetworkInfo
            return networkInfo != null && networkInfo.isConnectedOrConnecting
        }
    }

private fun hashSignature(signature: Signature): String {
    return try {
        val md = MessageDigest.getInstance("MD5")
        md.update(signature.toByteArray())
        val digest = md.digest()
        digest.let { it -> it.joinToString("") { "%02x".format(it) } }
    } catch (e: Exception) {
        Log.e(TAG, "Error hashing signature", e)
        ""
    }
}