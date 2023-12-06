package com.lizongying.mytv

import android.app.AlertDialog
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.view.Gravity
import android.view.KeyEvent
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.Toast
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class MainActivity : FragmentActivity() {

    private val playbackFragment = PlaybackFragment()
    private val mainFragment = MainFragment()

    private var doubleBackToExitPressedOnce = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)

        if (savedInstanceState == null) {
            supportFragmentManager.beginTransaction()
                .add(R.id.main_browse_fragment, playbackFragment)
                .add(R.id.main_browse_fragment, mainFragment)
                .commit()
        }
    }

    fun play(tv: TV) {
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
                val imageView = ImageView(this)
                val drawable = ContextCompat.getDrawable(this, R.drawable.appreciate)
                imageView.setImageDrawable(drawable)

                val parent = imageView.parent as? ViewGroup
                parent?.removeView(imageView)

                val linearLayout = LinearLayout(this)
                linearLayout.addView(imageView)

                val layoutParams = LinearLayout.LayoutParams(
                    LinearLayout.LayoutParams.MATCH_PARENT,
                    LinearLayout.LayoutParams.WRAP_CONTENT
                )
                layoutParams.gravity = Gravity.BOTTOM
                imageView.layoutParams = layoutParams


                val packageInfo = packageManager.getPackageInfo(packageName, 0)

                val versionName = packageInfo.versionName

                val builder: AlertDialog.Builder = AlertDialog.Builder(this)
                builder
                    .setTitle("当前版本: $versionName, 获取最新: https://github.com/lizongying/my-tv/releases/")
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

    companion object {
        private const val TAG = "MainActivity"
    }
}