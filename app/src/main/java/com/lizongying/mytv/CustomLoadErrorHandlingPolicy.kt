package com.lizongying.mytv

import androidx.media3.common.C
import androidx.media3.common.util.UnstableApi
import androidx.media3.exoplayer.upstream.DefaultLoadErrorHandlingPolicy


@UnstableApi
class CustomLoadErrorHandlingPolicy(private val minimumLoadableRetryCount: Int) :
    DefaultLoadErrorHandlingPolicy(minimumLoadableRetryCount) {

    override fun getMinimumLoadableRetryCount(dataType: Int): Int {
        return if (minimumLoadableRetryCount == -1) {
            if (dataType == C.DATA_TYPE_MEDIA_PROGRESSIVE_LIVE) {
                DEFAULT_MIN_LOADABLE_RETRY_COUNT_PROGRESSIVE_LIVE
            } else {
                DEFAULT_MIN_LOADABLE_RETRY_COUNT
            }
        } else {
            minimumLoadableRetryCount
        }
    }
}