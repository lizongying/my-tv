package com.lizongying.mytv

import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.annotation.RequiresApi
import com.lizongying.mytv.api.Release
import com.lizongying.mytv.requests.MyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class UpdateManager(private var context: Context?) {

    private var myRequest = MyRequest()

    @RequiresApi(Build.VERSION_CODES.O)
    fun checkAndUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                val release = myRequest.getRelease()
                // 在主线程中更新 UI
                updateUI(release)
                if (release?.data?.versionCode!! > 0) {
                    startDownload(release)
                }
            } catch (e: Exception) {
                // 处理异常情况
                Log.e(TAG, "Error occurred: ${e.message}", e)
            }
        }
    }

    private fun updateUI(release: Release?) {
    }

    @RequiresApi(Build.VERSION_CODES.O)
    private fun startDownload(release: Release) {
        val apkFileName = "my-tv-${release.data.versionName}.apk"

        val downloadManager =
            context!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = Request(Uri.parse(release.data.downloadUrl))
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkFileName)
        request.setTitle("New Version Download")
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // 获取下载任务的引用
        val downloadReference = downloadManager.enqueue(request)

        // 注册广播接收器，监听下载完成事件
        context!!.registerReceiver(
            DownloadReceiver(context!!, apkFileName, downloadReference),
            IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE), Context.RECEIVER_NOT_EXPORTED
        )
    }


    private class DownloadReceiver(
        private val context: Context,
        private val apkFileName: String,
        private val downloadReference: Long
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)

            // 检查是否是我们发起的下载
            if (reference == downloadReference) {
                // 下载完成，触发安装
                installNewVersion()
            }
        }

        private fun installNewVersion() {
            val installIntent = Intent(Intent.ACTION_VIEW)
            val apkUri = Uri.fromFile(
                File(
                    Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS),
                    apkFileName
                )
            )
            installIntent.setDataAndType(apkUri, "application/vnd.android.package-archive")
            installIntent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            context.startActivity(installIntent)
        }
    }


    companion object {
        private const val TAG = "UpdateManager"
    }
}