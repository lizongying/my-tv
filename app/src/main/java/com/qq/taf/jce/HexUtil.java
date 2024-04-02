package com.qq.taf.jce;

import java.nio.ByteBuffer;
public class HexUtil {
    private static final char[] digits = {'0', '1', '2', '3', '4', '5', '6', '7', '8', '9', 'A', 'B', 'C', 'D', 'E', 'F'};
    public static final byte[] emptybytes = new byte[0];

    public static String byte2HexStr(byte b2) {
        char[] cArr = digits;
        return new String(new char[]{cArr[((byte) (b2 >>> 4)) & 15], cArr[b2 & 15]});
    }

    public static String bytes2HexStr(ByteBuffer byteBuffer) {
        ByteBuffer duplicate = byteBuffer.duplicate();
        duplicate.flip();
        byte[] bArr = new byte[duplicate.limit()];
        duplicate.get(bArr);
        return bytes2HexStr(bArr);
    }

    public static byte char2Byte(char c2) {
        int i2;
        if (c2 < '0' || c2 > '9') {
            char c3 = 'a';
            if (c2 < 'a' || c2 > 'f') {
                c3 = 'A';
                if (c2 < 'A' || c2 > 'F') {
                    return (byte) 0;
                }
            }
            i2 = (c2 - c3) + 10;
        } else {
            i2 = c2 - '0';
        }
        return (byte) i2;
    }

    public static byte hexStr2Byte(String str) {
        if (str == null || str.length() != 1) {
            return (byte) 0;
        }
        return char2Byte(str.charAt(0));
    }

    public static byte[] hexStr2Bytes(String str) {
        if (str != null && !str.equals("")) {
            int length = str.length() / 2;
            byte[] bArr = new byte[length];
            for (int i2 = 0; i2 < length; i2++) {
                int i3 = i2 * 2;
                bArr[i2] = (byte) ((char2Byte(str.charAt(i3)) * 16) + char2Byte(str.charAt(i3 + 1)));
            }
            return bArr;
        }
        return emptybytes;
    }

    public static String bytes2HexStr(byte[] bArr) {
        if (bArr == null || bArr.length == 0) {
            return null;
        }
        char[] cArr = new char[bArr.length * 2];
        for (int i2 = 0; i2 < bArr.length; i2++) {
            byte b2 = bArr[i2];
            int i3 = i2 * 2;
            char[] cArr2 = digits;
            cArr[i3 + 1] = cArr2[b2 & 15];
            cArr[i3 + 0] = cArr2[((byte) (b2 >>> 4)) & 15];
        }
        return new String(cArr);
    }
}
