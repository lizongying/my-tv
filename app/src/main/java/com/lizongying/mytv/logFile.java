package com.lizongying.mytv;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileWriter;
import java.io.InputStreamReader;
import android.util.Log;

public class logFile {
    public static void startCapturingToFile(File outputFile) {
        new Thread(() -> {
            try {
                Process process = Runtime.getRuntime().exec("logcat -c"); // 清除当前的日志缓存
                process = Runtime.getRuntime().exec("logcat");
                BufferedReader reader = new BufferedReader(new InputStreamReader(process.getInputStream()));
                FileWriter writer = new FileWriter(outputFile, true);

                String line;
                while ((line = reader.readLine()) != null) {
                    writer.append(line).append("\n");
                }

                writer.flush();
                writer.close();
                reader.close();

            } catch (Exception e) {
                Log.e("log", "Error startCapturingToFile", e);
            }
        }).start();
    }
}
