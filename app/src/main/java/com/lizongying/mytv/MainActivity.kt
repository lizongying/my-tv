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
import android.view.GestureDetector
import android.view.KeyEvent
import android.view.MotionEvent
import android.view.View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
import android.view.WindowManager
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.TextView
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.lizongying.mytv.models.TVViewModel
import java.security.MessageDigest


class MainActivity : FragmentActivity() {

    var playerFragment = PlayerFragment()
    private val mainFragment = MainFragment()
    private val infoFragment = InfoFragment()

    private var doubleBackToExitPressedOnce = false

    private lateinit var gestureDetector: GestureDetector

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_HIDE_NAVIGATION

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, playerFragment)
                .add(R.id.main_browse_fragment, infoFragment)
                .add(R.id.main_browse_fragment, mainFragment)
                .hide(infoFragment)
                .commit()
            mainFragment.view?.requestFocus()
        }
        gestureDetector = GestureDetector(this, GestureListener())
    }

    fun showInfoFragment(tvViewModel: TVViewModel) {
        infoFragment.show(tvViewModel)
    }

    fun play(tvViewModel: TVViewModel) {
        Log.i(TAG, "play: ${tvViewModel.getTV()}")
        playerFragment.play(tvViewModel)
        mainFragment.view?.requestFocus()
    }

    fun prev() {
        mainFragment.prev()
    }

    fun next() {
        mainFragment.next()
    }

    private fun prevSource() {
        mainFragment.prevSource()
    }

    private fun nextSource() {
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

    private fun mainFragmentIsHidden(): Boolean {
        return mainFragment.isHidden
    }

    private fun hideMainFragment() {
        if (!mainFragment.isHidden) {
            supportFragmentManager.beginTransaction()
                .hide(mainFragment)
                .commit()
        }
    }

    override fun onTouchEvent(event: MotionEvent?): Boolean {
        if (event != null) {
            gestureDetector.onTouchEvent(event)
        }
        return super.onTouchEvent(event)
    }

    private inner class GestureListener : GestureDetector.SimpleOnGestureListener() {

        override fun onSingleTapConfirmed(e: MotionEvent): Boolean {
            Log.i(TAG, "onSingleTapConfirmed")
            switchMainFragment()
            return true
        }

        override fun onFling(
            e1: MotionEvent,
            e2: MotionEvent,
            velocityX: Float,
            velocityY: Float
        ): Boolean {
            if (velocityY > 0) {
                if (mainFragment.isHidden) {
                    prev()
                } else {
//                    if (mainFragment.selectedPosition == 0) {
//                        mainFragment.setSelectedPosition(
//                            mainFragment.tvListViewModel.maxNum.size - 1,
//                            false
//                        )
//                    }
                }
            }
            if (velocityY < 0) {
                if (mainFragment.isHidden) {
                    next()
                } else {
//                    if (mainFragment.selectedPosition == mainFragment.tvListViewModel.maxNum.size - 1) {
////                        mainFragment.setSelectedPosition(0, false)
//                        hideMainFragment()
//                        return false
//                    }
                }
            }
            return super.onFling(e1, e2, velocityX, velocityY)
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
                Log.i(TAG, "KEYCODE_DPAD_CENTER")
                switchMainFragment()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                if (mainFragment.isHidden) {
                    prev()
                } else {
//                    if (mainFragment.selectedPosition == 0) {
//                        mainFragment.setSelectedPosition(
//                            mainFragment.tvListViewModel.maxNum.size - 1,
//                            false
//                        )
//                    }
                }
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                if (mainFragment.isHidden) {
                    next()
                } else {
//                    if (mainFragment.selectedPosition == mainFragment.tvListViewModel.maxNum.size - 1) {
////                        mainFragment.setSelectedPosition(0, false)
//                        hideMainFragment()
//                        return false
//                    }
                }
            }

            KeyEvent.KEYCODE_DPAD_LEFT -> {
//                if (mainFragment.isHidden) {
//                    prevSource()
//                } else {
////                    if (mainFragment.tvListViewModel.getTVViewModelCurrent()
////                            ?.getItemPosition() == 0
////                    ) {
//////                        mainFragment.toLastPosition()
////                        hideMainFragment()
////                    }
//                }
            }

            KeyEvent.KEYCODE_DPAD_RIGHT -> {
//                if (mainFragment.isHidden) {
//                    nextSource()
//                } else {
////                    if (mainFragment.tvListViewModel.getTVViewModelCurrent()
////                            ?.getItemPosition() == mainFragment.tvListViewModel.maxNum[mainFragment.selectedPosition] - 1
////                    ) {
////                        mainFragment.toFirstPosition()
////                    }
//                }
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