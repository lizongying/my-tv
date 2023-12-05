package com.lizongying.mytv

import android.content.Context
import android.view.KeyEvent
import android.view.View
import androidx.leanback.media.MediaPlayerAdapter
import androidx.leanback.media.PlaybackTransportControlGlue

class PlaybackControlGlue(
    context: Context?,
    playerAdapter: MediaPlayerAdapter?,
) :
    PlaybackTransportControlGlue<MediaPlayerAdapter>(context, playerAdapter) {

    override fun onKey(v: View?, keyCode: Int, event: KeyEvent?): Boolean {
        if (event!!.action == KeyEvent.ACTION_DOWN) {
            when (keyCode) {
                KeyEvent.KEYCODE_DPAD_CENTER -> {
                    (context as? MainActivity)?.switchMainFragment()
                }

                KeyEvent.KEYCODE_DPAD_UP -> {
                    if ((context as? MainActivity)?.mainFragmentIsHidden() == true) {
                        (context as? MainActivity)?.prev()
                    }
                }

                KeyEvent.KEYCODE_DPAD_DOWN -> {
                    if ((context as? MainActivity)?.mainFragmentIsHidden() == true) {
                        (context as? MainActivity)?.next()
                    }
                }

                KeyEvent.KEYCODE_DPAD_LEFT -> {
                    if ((context as? MainActivity)?.mainFragmentIsHidden() == true) {
                        (context as? MainActivity)?.prev()
                    }
                }

                KeyEvent.KEYCODE_DPAD_RIGHT -> {
                    if ((context as? MainActivity)?.mainFragmentIsHidden() == true) {
                        (context as? MainActivity)?.next()
                    }
                }
            }
        }

        return super.onKey(v, keyCode, event)
    }

    companion object {
        private const val TAG = "CustomPlaybackControlGlue"
    }
}
