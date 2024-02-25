package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences
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
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.lifecycleScope
import com.lizongying.mytv.models.TVViewModel
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.launch
import java.security.MessageDigest


class MainActivity : FragmentActivity() {

    private var ready = 0
    private var playerFragment = PlayerFragment()
    private var mainFragment = MainFragment()
    private var infoFragment = InfoFragment()
    private var channelFragment = ChannelFragment()
    private lateinit var settingFragment: SettingFragment

    private var doubleBackToExitPressedOnce = false

    private lateinit var gestureDetector: GestureDetector

    private val handler = Handler()
    private val delayHideMain: Long = 5000
    private val delayHideSetting: Long = 10000

    lateinit var sharedPref: SharedPreferences
    private var channelReversal = false
    private var channelNum = true
    private var bootStartup = true

    init {
        lifecycleScope.launch(Dispatchers.IO) {
            val utilsJob = async(start = CoroutineStart.LAZY) { Utils.init() }

            utilsJob.start()

            utilsJob.await()
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        Log.i(TAG, "onCreate")
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        window.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        window.addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN)
        window.decorView.systemUiVisibility = SYSTEM_UI_FLAG_HIDE_NAVIGATION

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, playerFragment)
                .add(R.id.main_browse_fragment, infoFragment)
                .add(R.id.main_browse_fragment, channelFragment)
                .add(R.id.main_browse_fragment, mainFragment)
                .hide(mainFragment)
                .commit()
        }
        gestureDetector = GestureDetector(this, GestureListener())

        sharedPref = getPreferences(Context.MODE_PRIVATE)
        channelReversal = sharedPref.getBoolean(CHANNEL_REVERSAL, channelReversal)
        channelNum = sharedPref.getBoolean(CHANNEL_NUM, channelNum)
        bootStartup = sharedPref.getBoolean(BOOT_STARTUP, bootStartup)

        val packageInfo = getPackageInfo()
        val versionName = packageInfo.versionName
        val versionCode = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
            packageInfo.longVersionCode
        } else {
            packageInfo.versionCode.toLong()
        }
        settingFragment =
            SettingFragment(versionName, versionCode, channelReversal, channelNum, bootStartup)
    }

    fun showInfoFragment(tvViewModel: TVViewModel) {
        infoFragment.show(tvViewModel)
        if (channelNum) {
            channelFragment.show(tvViewModel)
        }
    }

    private fun showChannel(channel: String) {
        if (!mainFragment.isHidden) {
            return
        }

        if (settingFragment.isVisible) {
            return
        }

        if (channelNum) {
            channelFragment.show(channel)
        }
    }

    fun play(tvViewModel: TVViewModel) {
        playerFragment.play(tvViewModel)
        mainFragment.view?.requestFocus()
    }

    fun play(itemPosition: Int) {
        mainFragment.play(itemPosition)
    }

    fun prev() {
        mainFragment.prev()
    }

    fun next() {
        mainFragment.next()
    }

    private fun prevSource() {
//        mainFragment.prevSource()
    }

    private fun nextSource() {
//        mainFragment.nextSource()
    }

    fun switchMainFragment() {
        val transaction = supportFragmentManager.beginTransaction()

        if (mainFragment.isHidden) {
            transaction.show(mainFragment)
            mainActive()
        } else {
            transaction.hide(mainFragment)
        }

        transaction.commit()
    }

    fun mainActive() {
        handler.removeCallbacks(hideMain)
        handler.postDelayed(hideMain, delayHideMain)
    }

    fun settingActive() {
        handler.removeCallbacks(hideSetting)
        handler.postDelayed(hideSetting, delayHideSetting)
    }

    private val hideMain = Runnable {
        if (!mainFragment.isHidden) {
            supportFragmentManager.beginTransaction().hide(mainFragment).commit()
        }
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

    fun fragmentReady() {
        ready++
        Log.i(TAG, "ready $ready")
        if (ready == 4) {
            mainFragment.fragmentReady()
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
            e1: MotionEvent?,
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

    fun saveChannelReversal(channelReversal: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(CHANNEL_REVERSAL, channelReversal)
            apply()
        }
        this.channelReversal = channelReversal
    }

    fun saveChannelNum(channelNum: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(CHANNEL_NUM, channelNum)
            apply()
        }
        this.channelNum = channelNum
    }

    fun saveBootStartup(bootStartup: Boolean) {
        with(sharedPref.edit()) {
            putBoolean(BOOT_STARTUP, bootStartup)
            apply()
        }
        this.bootStartup = bootStartup
    }

    private fun showSetting() {
        if (!mainFragment.isHidden) {
            return
        }

        Log.i(TAG, "settingFragment ${settingFragment.isVisible}")
        if (!settingFragment.isVisible) {
            settingFragment.show(supportFragmentManager, "setting")
            settingActive()
        } else {
            handler.removeCallbacks(hideSetting)
            settingFragment.dismiss()
        }
    }

    private val hideSetting = Runnable {
        if (settingFragment.isVisible) {
            settingFragment.dismiss()
        }
    }

    private fun channelUp() {
        if (mainFragment.isHidden) {
            if (channelReversal) {
                next()
                return
            }
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

    private fun channelDown() {
        if (mainFragment.isHidden) {
            if (channelReversal) {
                prev()
                return
            }
            next()
        } else {
//                    if (mainFragment.selectedPosition == mainFragment.tvListViewModel.maxNum.size - 1) {
////                        mainFragment.setSelectedPosition(0, false)
//                        hideMainFragment()
//                        return false
//                    }
        }
    }

    private fun back() {
        if (!mainFragmentIsHidden()) {
            hideMainFragment()
            return
        }

        if (doubleBackToExitPressedOnce) {
            super.onBackPressed()
            return
        }

        doubleBackToExitPressedOnce = true
        Toast.makeText(this, "再按一次退出", Toast.LENGTH_SHORT).show()

        Handler(Looper.getMainLooper()).postDelayed({
            doubleBackToExitPressedOnce = false
        }, 2000)
    }

    override fun onKeyDown(keyCode: Int, event: KeyEvent?): Boolean {
        when (keyCode) {
            KeyEvent.KEYCODE_0 -> {
                showChannel("0")
                return true
            }

            KeyEvent.KEYCODE_1 -> {
                showChannel("1")
                return true
            }

            KeyEvent.KEYCODE_2 -> {
                showChannel("2")
                return true
            }

            KeyEvent.KEYCODE_3 -> {
                showChannel("3")
                return true
            }

            KeyEvent.KEYCODE_4 -> {
                showChannel("4")
                return true
            }

            KeyEvent.KEYCODE_5 -> {
                showChannel("5")
                return true
            }

            KeyEvent.KEYCODE_6 -> {
                showChannel("6")
                return true
            }

            KeyEvent.KEYCODE_7 -> {
                showChannel("7")
                return true
            }

            KeyEvent.KEYCODE_8 -> {
                showChannel("8")
                return true
            }

            KeyEvent.KEYCODE_9 -> {
                showChannel("9")
                return true
            }

            KeyEvent.KEYCODE_ESCAPE -> {
                back()
                return true
            }

            KeyEvent.KEYCODE_BACK -> {
                back()
                return true
            }

            KeyEvent.KEYCODE_BOOKMARK -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_UNKNOWN -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_HELP -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_SETTINGS -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_MENU -> {
                showSetting()
                return true
            }

            KeyEvent.KEYCODE_ENTER -> {
                switchMainFragment()
            }

            KeyEvent.KEYCODE_DPAD_CENTER -> {
                switchMainFragment()
            }

            KeyEvent.KEYCODE_DPAD_UP -> {
                channelUp()
            }

            KeyEvent.KEYCODE_CHANNEL_UP -> {
                channelUp()
            }

            KeyEvent.KEYCODE_DPAD_DOWN -> {
                channelDown()
            }

            KeyEvent.KEYCODE_CHANNEL_DOWN -> {
                channelDown()
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
            val md = MessageDigest.getInstance("MD5")
            md.update(signature.toByteArray())
            val digest = md.digest()
            digest.let { it -> it.joinToString("") { "%02x".format(it) } }
        } catch (e: Exception) {
            Log.e(TAG, "Error hashing signature", e)
            ""
        }
    }

    override fun onStart() {
        Log.i(TAG, "onStart")
        super.onStart()
    }

    override fun onResume() {
        Log.i(TAG, "onResume")
        super.onResume()
        if (!mainFragment.isHidden) {
            handler.postDelayed(hideMain, delayHideMain)
        }
    }

    override fun onPause() {
        Log.i(TAG, "onPause")
        super.onPause()
        handler.removeCallbacks(hideMain)
    }

    companion object {
        private const val TAG = "MainActivity"
        private const val CHANNEL_REVERSAL = "channel_reversal"
        private const val CHANNEL_NUM = "channel_num"
        const val BOOT_STARTUP = "boot_startup"
    }
}