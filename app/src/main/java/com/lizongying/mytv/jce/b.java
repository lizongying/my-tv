package com.lizongying.mytv.jce;

import com.tencent.videolite.android.datamodel.cctvjce.RequestCommand;

import java.io.ByteArrayOutputStream;
import java.io.DataOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ByteBuffer;

public class b {

    /* renamed from: a  reason: collision with root package name */
    private static final String f33241a = "Net_UnifiedProtocol";

    b() {
    }

    public static byte[] a(byte[] bArr, int[] iArr) {
        boolean z;
        if (bArr == null || bArr.length == 0) {
            return null;
        }
        ByteBuffer wrap = ByteBuffer.wrap(bArr);
        if (wrap.get() == 19 && wrap.getInt() == bArr.length) {
            wrap.getShort();
            if ((wrap.getShort() & 65535) != 65281) {

                return null;
            }
            wrap.getShort();
            int i2 = wrap.getShort() & 65535;
            if (i2 != 0) {
                iArr[0] = i2;

                return null;
            }
            wrap.getLong();
            int i3 = wrap.getInt();
            if ((i3 & 2) <= 0) {
                z = false;
            } else if ((i3 & 16) <= 0) {
                iArr[0] = -867;
                return null;
            } else {
                z = true;
            }
            wrap.getInt();
            wrap.getLong();
            wrap.position(wrap.position() + 32);
            wrap.get();
            wrap.position(wrap.position() + 10);
            wrap.get();
            wrap.position(wrap.position() + (wrap.getShort() & 65535));
            int i4 = 65535 & wrap.getShort();
            wrap.position(wrap.position() + i4);
            int i5 = 83 + i4 + 2;
            int i6 = wrap.getInt();
            int i7 = i5 + 4;
            if (wrap.get(bArr.length - 1) != 3) {
                iArr[0] = -869;
                return null;
            }
            int length = (bArr.length - i7) - 1;
            if (length <= 0) {
                iArr[0] = -868;
                return null;
            }
            byte[] bArr2 = new byte[length];
            System.arraycopy(bArr, i7, bArr2, 0, length);
            return c(bArr2, z, i6, iArr);
        }
        return null;
    }

    public static byte[] b(RequestCommand requestCommand, long j) {
        byte[] d2 = a.d(requestCommand);
        if (d2 == null) {
            return null;
        }
        byte[] d3 = d(d2);
        ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream(d3.length + 50);
        DataOutputStream dataOutputStream = new DataOutputStream(byteArrayOutputStream);
        try {
            dataOutputStream.writeByte(19);
            dataOutputStream.writeInt(0);
            dataOutputStream.writeShort(2);
            dataOutputStream.writeShort(65281);
            dataOutputStream.writeShort(requestCommand.head.cmdId);
            dataOutputStream.writeShort(0);
            dataOutputStream.writeLong(j);
            dataOutputStream.writeInt(531);
            dataOutputStream.writeInt(0);
            dataOutputStream.writeLong(0);
            e(dataOutputStream, requestCommand.head.guid, 32);
            dataOutputStream.writeByte(0);
//            VersionCode
            dataOutputStream.writeInt(0);
            e(dataOutputStream, null, 6);
            dataOutputStream.writeByte(0);
            dataOutputStream.writeShort(0);
            dataOutputStream.writeShort(0);
            dataOutputStream.writeInt(d3.length);
            byte[] compressGZIP = CompressUtils.compressGZIP(d3);
            if (compressGZIP == null) {
                return null;
            }
            dataOutputStream.write(compressGZIP);
            dataOutputStream.writeByte(3);
            dataOutputStream.close();
            byte[] byteArray = byteArrayOutputStream.toByteArray();
            ByteBuffer wrap = ByteBuffer.wrap(byteArray);
            wrap.putInt(1, byteArray.length);
            return wrap.array();
        } catch (IOException unused) {
            return null;
        }
    }

    private static byte[] c(byte[] bArr, boolean z, int i2, int[] iArr) {
        if (bArr == null) {
            iArr[0] = -871;
            return null;
//        } else if (z && ((bArr = CompressUtils.decompressGZIP(bArr)) == null || bArr.length != i2)) {
        } else if (z && ((bArr = CompressUtils.decompressGZIP(bArr)) == null)) {
            iArr[0] = -871;

            return null;
        } else {
            ByteBuffer wrap = ByteBuffer.wrap(bArr);
            int position = wrap.position() + 16;
            if (wrap.get() != 38) {
                iArr[0] = -869;
                return null;
            } else if (wrap.get(bArr.length - 1) != 40) {
                iArr[0] = -869;
                return null;
            } else {
                int length = (bArr.length - position) - 1;
                if (length <= 0) {
                    iArr[0] = -869;
                    return null;
                }
                byte[] bArr2 = new byte[length];
                System.arraycopy(bArr, position, bArr2, 0, length);
                return bArr2;
            }
        }
    }

    private static byte[] d(byte[] bArr) {
        int length = bArr.length + 17;
        ByteBuffer allocate = ByteBuffer.allocate(length);
        allocate.put((byte) 38);
        allocate.putInt(length);
        allocate.put((byte) 1);
        allocate.position(allocate.position() + 10);
        allocate.put(bArr);
        allocate.put((byte) 40);
        return allocate.array();
    }

    private static void e(OutputStream outputStream, String str, int i2) throws IOException {
        byte[] bArr;
        int i3;
        if (i2 > 0) {
            if (str != null) {
                bArr = str.getBytes("UTF-8");
                i3 = bArr.length;
            } else {
                bArr = null;
                i3 = 0;
            }
            if (i3 >= i2) {
                outputStream.write(bArr, 0, i2);
                return;
            }
            if (i3 > 0) {
                outputStream.write(bArr, 0, i3);
            }
            while (i3 < i2) {
                outputStream.write(0);
                i3++;
            }
        }
    }
}
