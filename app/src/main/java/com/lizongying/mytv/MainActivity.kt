package com.lizongying.mytv

import android.app.AlertDialog
import android.content.pm.PackageInfo
import android.content.pm.PackageManager
import android.content.pm.Signature
import android.content.pm.SigningInfo
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.view.KeyEvent
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import java.security.MessageDigest


class MainActivity : FragmentActivity() {

    private val playbackFragment = PlaybackFragment()
    private val mainFragment = MainFragment()
    private val infoFragment = InfoFragment()

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, playbackFragment)
                .add(R.id.main_browse_fragment, mainFragment)
                .add(R.id.main_browse_fragment, infoFragment)
                .hide(infoFragment)
                .commit()
        }

        Log.i(TAG, "Signature ${getAppSignature()}")
    }

    fun switchInfoFragment(tv: TV) {
        infoFragment.setInfo(tv)

        if (infoFragment.isHidden) {
            supportFragmentManager.beginTransaction().show(infoFragment).commit()
        }
    }

    fun showInfoFragment(tv: TV) {
        infoFragment.setInfo(tv)
        supportFragmentManager.beginTransaction()
            .show(infoFragment)
            .commit()
    }

    fun hideInfoFragment() {
        supportFragmentManager.beginTransaction()
            .hide(infoFragment)
            .commit()
    }

    fun play(tv: TV) {
        Log.i(TAG, "play: $tv")
        playbackFragment.play(tv)
    }

    fun prev() {
        mainFragment.prev()
    }

    fun next() {
        mainFragment.next()
    }

    fun prevSource() {
        mainFragment.prevSource()
    }

    fun nextSource() {
        mainFragment.nextSource()
    }

    fun switchMainFragment() {
        val transaction = supportFragmentManager.beginTransaction()

        if (mainFragment.isHidden) {
            focusMainFragment()
            transaction.show(mainFragment)
        } else {
            transaction.hide(mainFragment)
        }

        transaction.commit()
    }

    private fun focusMainFragment() {
        mainFragment.focus()
    }

    fun mainFragmentIsHidden(): Boolean {
        return mainFragment.isHidden
    }

    private fun hideMainFragment() {
        if (!mainFragment.isHidden) {
            supportFragmentManager.beginTransaction()
                .hide(mainFragment)
                .commit()
        }
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_BACK -> {
                if (!mainFragmentIsHidden()) {
                    hideMainFragment()
                    return true
                }

                if (doubleBackToExitPressedOnce) {
                    super.onBackPressed()
                    return true
                }

                this.doubleBackToExitPressedOnce = true
                Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show()

                Handler(Looper.getMainLooper()).postDelayed({
                    doubleBackToExitPressedOnce = false
                }, 2000)
                return true
            }

            KeyEvent.KEYCODE_MENU -> {
                val versionName = getPackageInfo().versionName

                val textView = TextView(this)
                textView.text =
                    "当前版本: $versionName\n获取最新: https://github.com/lizongying/my-tv/releases/"

                val imageView = ImageView(this)
                val drawable = ContextCompat.getDrawable(this, R.drawable.appreciate)
                imageView.setImageDrawable(drawable)

                val linearLayout = LinearLayout(this)
                linearLayout.orientation = LinearLayout.VERTICAL
                linearLayout.addView(textView)
                linearLayout.addView(imageView)

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                imageView.layoutParams = layoutParams
                textView.layoutParams = layoutParams

                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder
                    .setView(linearLayout)

                val dialog: AlertDialog = builder.create()
                dialog.show()
                return true
            }

            KeyEvent.KEYCODE_DPAD_CENTER -> {
                switchMainFragment()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (mainFragment.isHidden) {
                    prev()
                }
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (mainFragment.isHidden) {
                    next()
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
                if (mainFragment.isHidden) {
                    prevSource()
                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
                if (mainFragment.isHidden) {
                    nextSource()
                }
            }
        }

        return super.onKeyDown(keyCode, event)
    }

    private fun getPackageInfo(): PackageInfo {
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

    private fun getAppSignature(): String {
        val packageInfo = getPackageInfo()

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

    private fun hashSignature(signature: Signature): String {
        return try {
            val md = MessageDigest.getInstance("SHA-256")
            md.update(signature.toByteArray())
            val digest = md.digest()
            digest.let { it -> it.joinToString("") { "%02x".format(it) } }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing signature", e)
            ""
        }
    }

    companion object {
        private const val TAG = "MainActivity"
    }
}