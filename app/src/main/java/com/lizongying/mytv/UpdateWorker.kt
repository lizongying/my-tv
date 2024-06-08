package com.lizongying.mytv

import android.app.DownloadManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.lizongying.mytv.models.MyViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File

class UpdateWorker(context: Context, params: WorkerParameters) : CoroutineWorker(context, params) {

    private lateinit var notificationManager: NotificationManager
    private lateinit var notificationBuilder: NotificationCompat.Builder

    private lateinit var myViewModel: MyViewModel

    override suspend fun doWork(): Result {
        val downloadUrl = inputData.getString("DOWNLOAD_URL") ?: return Result.failure()
        val apkFileName = inputData.getString("APK_FILENAME") ?: return Result.failure()
        val versionName = inputData.getString("VERSION_NAME") ?: return Result.failure()

        showNotification(applicationContext)

        val app = applicationContext as MyTVApplication
        myViewModel = app.myViewModel

        return withContext(Dispatchers.IO) {
            try {
                downloadAndInstall(applicationContext, downloadUrl, apkFileName, versionName)
                Result.success()
            } catch (e: Exception) {
                Result.failure()
            }
        }
    }

    private fun showNotification(context: Context) {
        notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationBuilder = NotificationCompat.Builder(context, "download_channel").apply {
            setSmallIcon(android.R.drawable.stat_sys_download)
            setContentTitle("Downloading Update")
            setContentText("Download in progress")
            priority = NotificationCompat.PRIORITY_LOW
            setOngoing(true)
            setProgress(100, 0, false)
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                "download_channel",
                "Download Updates",
                NotificationManager.IMPORTANCE_LOW
            )
            notificationManager.createNotificationChannel(channel)
        }

        notificationManager.notify(1, notificationBuilder.build())
    }

    private fun downloadAndInstall(
        context: Context,
        downloadUrl: String,
        apkFileName: String,
        versionName: String
    ) {
        context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS)?.mkdirs()
        Log.i(TAG, "save dir ${Environment.DIRECTORY_DOWNLOADS}")
        val request = DownloadManager.Request(Uri.parse(downloadUrl)).apply {
            setTitle("${context.resources.getString(R.string.app_name)} $versionName Downloading")
            setDescription("Downloading the latest version of the app")
            setDestinationInExternalFilesDir(context, Environment.DIRECTORY_DOWNLOADS, apkFileName)

            setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
//            setAllowedOverRoaming(false)
            setMimeType("application/vnd.android.package-archive")
        }

        val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
        val downloadId = manager.enqueue(request)

        var downloading = true
        while (downloading) {
            val query = DownloadManager.Query().setFilterById(downloadId)
            val cursor = manager.query(query)
            if (cursor.moveToFirst()) {
                val bytesDownloaded =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_BYTES_DOWNLOADED_SO_FAR))
                val bytesTotal =
                    cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_TOTAL_SIZE_BYTES))

                if (bytesTotal > 0) {
                    val progress = (bytesDownloaded * 100L / bytesTotal).toInt()
                    Log.i(TAG, "progress $progress")
//                    myViewModel.setDownloadProgress(progress)
                    notificationBuilder.setProgress(100, progress, false)
                    notificationManager.notify(1, notificationBuilder.build())
                }

                when (cursor.getInt(cursor.getColumnIndexOrThrow(DownloadManager.COLUMN_STATUS))) {
                    DownloadManager.STATUS_SUCCESSFUL -> downloading = false
                    DownloadManager.STATUS_FAILED -> {
                        downloading = false
                        throw Exception("Download failed")
                    }
                }
            }
            cursor.close()
        }

        notificationBuilder.setContentText("Download complete")
            .setProgress(0, 0, false)
            .setOngoing(false)
        notificationManager.notify(1, notificationBuilder.build())

        val apkFile = File(
            context.getExternalFilesDir(Environment.DIRECTORY_DOWNLOADS),
            apkFileName
        )

        if (apkFile.exists()) {
            val apkUri: Uri = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                FileProvider.getUriForFile(context, "${context.packageName}.provider", apkFile)
            } else {
                Uri.parse("file://${apkFile.absolutePath}")
            }

            Log.i(TAG, "apkUri $apkUri")

            val installIntent = Intent(Intent.ACTION_VIEW).apply {
                setDataAndType(apkUri, "application/vnd.android.package-archive")
                addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
            }

            context.startActivity(installIntent)
        } else {
            Log.e(TAG, "APK file does not exist!")
        }
    }

    companion object {
        private const val TAG = "UpdateWorker"
    }
}
