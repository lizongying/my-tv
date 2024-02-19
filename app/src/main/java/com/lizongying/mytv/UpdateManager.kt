package com.lizongying.mytv

import android.app.DownloadManager
import android.app.DownloadManager.Request
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.database.Cursor
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.lizongying.mytv.api.Release
import com.lizongying.mytv.requests.MyRequest
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class UpdateManager(
    private var context: Context?,
    private var settingFragment: SettingFragment,
    private var versionCode: Long
) :
    ConfirmationDialogFragment.ConfirmationDialogListener {

    private var myRequest = MyRequest()
    private var release: Release? = null

    private var downloadReceiver: DownloadReceiver? = null

    fun checkAndUpdate() {
        CoroutineScope(Dispatchers.Main).launch {
            try {
                release = myRequest.getRelease()
                updateUI(release)
                Log.i(TAG, "versionCode $versionCode ${release?.data?.versionCode}")
                if (release != null) {
                    if (release?.data?.versionCode!! >= versionCode) {
                        val dialog = ConfirmationDialogFragment(this@UpdateManager)
                        dialog.show(settingFragment.fragmentManager, "ConfirmationDialogFragment")
                    } else {
                        Toast.makeText(context, "不需要更新", Toast.LENGTH_LONG)
                            .show()
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error occurred: ${e.message}", e)
            }
        }
    }

    private fun updateUI(release: Release?) {
        if (release?.data?.versionName.isNullOrEmpty()) {
            settingFragment.setVersionName("版本获取失败")
        } else {
            settingFragment.setVersionName("最新版本：${release?.data?.versionName!!}")
        }
    }

    private fun startDownload(release: Release) {
        val apkFileName = "my-tv-${release.data.versionName}.apk"
        Log.i(TAG, "apkFileName $apkFileName")
        val downloadManager =
            context!!.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request = Request(Uri.parse(release.data.downloadUrl))
        Log.i(TAG, "url ${Uri.parse(release.data.downloadUrl)}")
        request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, apkFileName)
        request.setTitle("New Version Download")
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)

        // 获取下载任务的引用
        val downloadReference = downloadManager.enqueue(request)

        downloadReceiver = DownloadReceiver(context!!, apkFileName, downloadReference)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context!!.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context!!.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        getDownloadProgress(context!!, downloadReference) { progress ->
            println("Download progress: $progress%")
        }
    }

    private fun getDownloadProgress(context: Context, downloadId: Long, progressListener: (Int) -> Unit) {
        val downloadManager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val handler = Handler(Looper.getMainLooper())
        val intervalMillis: Long = 1000

        handler.post(object : Runnable {
            override fun run() {
                Log.i(TAG, "search")
                val query = DownloadManager.Query().setFilterById(downloadId)
                val cursor: Cursor = downloadManager.query(query)
                cursor.use {
                    if (it.moveToFirst()) {
                        val bytesDownloadedIndex =
                            it.getColumnIndex(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR)
                        val bytesTotalIndex =
                            it.getColumnIndex(DownloadManager.COLUMN_TOTAL_SIZE_BYTES)

                        // 检查列名是否存在
                        if (bytesDownloadedIndex != -1 && bytesTotalIndex != -1) {
                            val bytesDownloaded = it.getInt(bytesDownloadedIndex)
                            val bytesTotal = it.getInt(bytesTotalIndex)

                            if (bytesTotal != -1) {
                                val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                                progressListener(progress)
                                if (progress == 100) {
                                    return
                                }
                            }
                        }
                    }
                }

//                handler.postDelayed(this, intervalMillis)
            }
        })
    }

    private class DownloadReceiver(
        private val context: Context,
        private val apkFileName: String,
        private val downloadReference: Long
    ) : BroadcastReceiver() {
        override fun onReceive(context: Context, intent: Intent) {
            val reference = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, -1)
            Log.i(TAG, "reference $reference")
            val progress = intent.getIntExtra("progress", 0)
            Log.i(TAG, "progress $progress")

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

    override fun onConfirm() {
        Log.i(TAG, "onConfirm $release")
        release?.let { startDownload(it) }
    }

    override fun onCancel() {
    }


    fun destroy() {
        if (downloadReceiver != null) {
            context!!.unregisterReceiver(downloadReceiver)
            Log.i(TAG, "destroy downloadReceiver")
        }
    }
}