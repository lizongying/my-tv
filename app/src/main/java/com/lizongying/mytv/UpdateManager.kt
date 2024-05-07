package com.lizongying.mytv

import android.app.Activity
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
import androidx.core.app.ActivityCompat
import androidx.core.content.PermissionChecker
import androidx.core.content.PermissionChecker.checkSelfPermission
import com.lizongying.mytv.api.ApiClient
import com.lizongying.mytv.requests.ReleaseRequest
import com.lizongying.mytv.requests.ReleaseResponse
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import java.io.File


class UpdateManager(
    private var context: Context,
    private var settingFragment: SettingFragment,
    private var versionCode: Long
) :
    ConfirmationFragment.ConfirmationListener {

    private var releaseRequest = ReleaseRequest()
    private var release: ReleaseResponse? = null

    private var downloadReceiver: DownloadReceiver? = null

    fun checkAndUpdate() {
        if (!haveStoragePermission()) {
            return
        }
        CoroutineScope(Dispatchers.Main).launch {
            var text = "版本获取失败"
            try {
                release = releaseRequest.getRelease()
                Log.i(TAG, "versionCode $versionCode ${release?.version_code}")
                if (release?.version_code != null) {
                    text = if (release?.version_code!! >= versionCode) {
                        "最新版本：${release?.version_name}}"
                    } else {
                        "已是最新版本，不需要更新"
                    }
                }
            } catch (e: Exception) {
                Log.e(TAG, "Error occurred: ${e.message}", e)
            }
            updateUI(text)
        }
    }

    private fun updateUI(text: String) {
        val dialog = ConfirmationFragment(this@UpdateManager, text)
        settingFragment.fragmentManager?.let { dialog.show(it, TAG) }
    }

    private fun haveStoragePermission(): Boolean {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(context, android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                === PermissionChecker.PERMISSION_GRANTED
            ) {
                Log.e("Permission error", "You have permission")
                return true
            } else {
                Log.e("Permission error", "You have asked for permission")
                ActivityCompat.requestPermissions(
                    context as Activity, arrayOf(
                        android.Manifest.permission.WRITE_EXTERNAL_STORAGE
                    ), 1
                )
                return false
            }
        } else { //you don't need to worry about these stuff below api level 23
            Log.e("Permission error", "You already have the permission")
            return true
        }
    }


    private fun startDownload(release: ReleaseResponse) {
        val apkFileName = "my-tv-${release.version_name}.apk"
        Log.i(TAG, "apkFileName $apkFileName")
        val downloadManager =
            context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val request =
            Request(Uri.parse("${ApiClient.HOST}/release/download/${release.version_name}/my-tv-${release.version_name}.apk"))
        Log.i(
            TAG,
            "url ${Uri.parse("${ApiClient.HOST}/release/download/${release.version_name}/my-tv-0-${release.version_name}.apk")}"
        )
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.mkdirs()
        request.setDestinationInExternalFilesDir(
            context,
            Environment.DIRECTORY_DOWNLOADS,
            apkFileName
        )
        request.setTitle("${settingFragment.resources.getString(R.string.app_name)} ${release.version_name}")
        request.setNotificationVisibility(Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
        request.setAllowedOverRoaming(false)
        request.setMimeType("application/vnd.android.package-archive")

        // 获取下载任务的引用
        val downloadReference = downloadManager.enqueue(request)

        downloadReceiver = DownloadReceiver(context, apkFileName, downloadReference)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE),
                Context.RECEIVER_NOT_EXPORTED,
            )
        } else {
            context.registerReceiver(
                downloadReceiver,
                IntentFilter(DownloadManager.ACTION_DOWNLOAD_COMPLETE)
            )
        }

        getDownloadProgress(context, downloadReference) { progress ->
            println("Download progress: $progress%")
        }
    }

    private fun getDownloadProgress(
        context: Context,
        downloadId: Long,
        progressListener: (Int) -> Unit
    ) {
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
            context.unregisterReceiver(downloadReceiver)
            Log.i(TAG, "destroy downloadReceiver")
        }
    }
}