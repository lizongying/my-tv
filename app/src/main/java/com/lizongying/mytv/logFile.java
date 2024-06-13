package com.lizongying.mytv;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import android.util.Log;

public class logFile {
    private static final String TAG = "logFile";  // 日志标签定义

    public static void startCapturingToFile(File outputFile) {
        new Thread(() -> {
            Process clearProcess = null;
            Process logcatProcess = null;

            try {
                // 清除当前logcat缓存
                clearProcess = Runtime.getRuntime().exec("logcat -c");

                logcatProcess = Runtime.getRuntime().exec("logcat");
                BufferedReader reader = new BufferedReader(new InputStreamReader(logcatProcess.getInputStream()));
                BufferedWriter writer =new BufferedWriter(new FileWriter(outputFile, true));

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.append(line).append("\n");
                }

                writer.flush();
                writer.close();
                reader.close();

            } catch (Exception e) {
                Log.e(TAG, "Error capturing log to file", e);
            } finally {
                if (clearProcess != null) clearProcess.destroy();
                if (logcatProcess != null) logcatProcess.destroy();
            }
        }).start();
    }
}
