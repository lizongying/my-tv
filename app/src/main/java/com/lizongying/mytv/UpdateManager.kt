package com.lizongying.mytv

import android.content.Context
import android.util.Log
import androidx.fragment.app.FragmentActivity
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.workDataOf
import com.lizongying.mytv.api.ApiClient
import com.lizongying.mytv.requests.ReleaseRequest
import com.lizongying.mytv.requests.ReleaseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch


class UpdateManager(
    private var context: Context,
    private var versionCode: Long
) :
    ConfirmationFragment.ConfirmationListener {

    private var releaseRequest = ReleaseRequest()
    private var release: ReleaseResponse? = null

    fun checkAndUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            var text = "版本获取失败"
            var update = false
            try {
                release = releaseRequest.getRelease()
                val code = release?.version_code
                if (code != null) {
                    if (code.toLong() > versionCode) {
                        text = "最新版本：${release?.version_name}"
                        update = true
                    } else {
                        text = "已是最新版本，不需要更新"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error occurred: ${e.message}", e)
            }
            updateUI(text, update)
        }
    }

    private fun updateUI(text: String, update: Boolean) {
        val dialog = ConfirmationFragment(this@UpdateManager, text, update)
        dialog.show((context as FragmentActivity).supportFragmentManager, TAG)
    }

    private fun startDownload(context: Context, release: ReleaseResponse) {
        val versionName = release.version_name
        val apkName = "my-tv"
        val apkFileName = "$apkName-${release.version_name}.apk"
        val downloadUrl =
            "${ApiClient.DOWNLOAD_HOST}${release.version_name}/$apkName-${release.version_name}.apk"
        Log.i(TAG, "downloadUrl: $downloadUrl")
        val downloadRequest = OneTimeWorkRequestBuilder<UpdateWorker>()
            .setInputData(
                workDataOf(
                    "VERSION_NAME" to versionName,
                    "APK_FILENAME" to apkFileName,
                    "DOWNLOAD_URL" to downloadUrl
                )
            )
            .build()

        WorkManager.getInstance(context).enqueue(downloadRequest)
    }

    companion object {
        private const val TAG = "UpdateManager"
    }

    override fun onConfirm() {
        Log.i(TAG, "onConfirm $release")
        release?.let { startDownload(context, it) }
    }

    override fun onCancel() {
    }
}