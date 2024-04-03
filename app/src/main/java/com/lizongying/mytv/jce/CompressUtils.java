package com.lizongying.mytv.jce;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import java.util.zip.GZIPOutputStream;

public class CompressUtils {
    private static final String TAG = "CompressUtils";

    public static byte[] compressGZIP(byte[] bArr) {
        GZIPOutputStream gZIPOutputStream;
        Throwable th;
        if (bArr != null && bArr.length != 0) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                gZIPOutputStream = new GZIPOutputStream(byteArrayOutputStream);
            } catch (IOException unused) {
                gZIPOutputStream = null;
            } catch (Throwable th2) {
                gZIPOutputStream = null;
                th = th2;
            }
            try {
                byte[] bArr2 = new byte[4096];
                while (true) {
                    int read = byteArrayInputStream.read(bArr2);
                    if (read <= 0) {
                        break;
                    }
                    gZIPOutputStream.write(bArr2, 0, read);
                }
                gZIPOutputStream.finish();
                byte[] byteArray = byteArrayOutputStream.toByteArray();
                try {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    gZIPOutputStream.close();
                } catch (IOException e2) {
                    System.out.println(e2);
                }
                return byteArray;
            } catch (IOException unused2) {
                try {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    if (gZIPOutputStream != null) {
                        gZIPOutputStream.close();
                    }
                } catch (IOException e3) {
                    System.out.println(e3);
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                try {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    if (gZIPOutputStream != null) {
                        gZIPOutputStream.close();
                    }
                } catch (IOException e4) {
                    System.out.println(e4);
                }
            }
        }
        return null;
    }

    public static byte[] decompressGZIP(byte[] bArr) {
        GZIPInputStream gZIPInputStream;
        Throwable th;
        if (bArr != null && bArr.length != 0) {
            ByteArrayInputStream byteArrayInputStream = new ByteArrayInputStream(bArr);
            ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();
            try {
                gZIPInputStream = new GZIPInputStream(byteArrayInputStream);
            } catch (IOException unused) {
                gZIPInputStream = null;
            } catch (Throwable th2) {
                gZIPInputStream = null;
                th = th2;
            }

            try {
                byte[] bArr2 = new byte[4096];
                while (true) {
                    int read = gZIPInputStream.read(bArr2);
                    if (read <= 0) {
                        break;
                    }
                    byteArrayOutputStream.write(bArr2, 0, read);
                }
                byteArrayOutputStream.flush();
                byte[] byteArray = byteArrayOutputStream.toByteArray();

                try {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    gZIPInputStream.close();
                } catch (IOException e2) {
                    System.out.println(e2);
                }
                return byteArray;
            } catch (IOException unused2) {
                try {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    if (gZIPInputStream != null) {
                        gZIPInputStream.close();
                    }
                } catch (IOException e3) {
                    System.out.println(e3);
                }
                return null;
            } catch (Throwable th3) {
                th = th3;
                try {
                    byteArrayInputStream.close();
                    byteArrayOutputStream.close();
                    if (gZIPInputStream != null) {
                        gZIPInputStream.close();
                    }
                } catch (IOException e4) {
                    System.out.println(e4);
                }
            }
        }
        return null;
    }
}
