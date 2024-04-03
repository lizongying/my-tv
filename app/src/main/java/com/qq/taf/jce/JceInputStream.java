package com.qq.taf.jce;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Array;
import java.nio.BufferUnderflowException;
import java.nio.ByteBuffer;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public final class JceInputStream {
    private ByteBuffer bs;
    protected String sServerEncoding = "GBK";


    public static class HeadData {
        public int tag;
        public byte type;

        public void clear() {
            this.type = (byte) 0;
            this.tag = 0;
        }
    }

    public JceInputStream() {
    }

    private int peakHead(HeadData headData) {
        return readHead(headData, this.bs.duplicate());
    }

    private <T> T[] readArrayImpl(T t, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                T[] tArr = (T[]) ((Object[]) Array.newInstance(t.getClass(), read));
                for (int i3 = 0; i3 < read; i3++) {
                    tArr[i3] =  (T) read( t, 0, true);
                }
                return tArr;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public static int readHead(HeadData headData, ByteBuffer byteBuffer) {
        byte b2 = byteBuffer.get();
        headData.type = (byte) (b2 & 15);
        int i2 = (b2 & 240) >> 4;
        headData.tag = i2;
        if (i2 == 15) {
            headData.tag = byteBuffer.get() & 255;
            return 2;
        }
        return 1;
    }

    private void skip(int i2) {
        ByteBuffer byteBuffer = this.bs;
        byteBuffer.position(byteBuffer.position() + i2);
    }

    private void skipField() {
        HeadData headData = new HeadData();
        readHead(headData);
        skipField(headData.type);
    }

    public JceStruct directRead(JceStruct jceStruct, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        try {
            JceStruct newInit = jceStruct.newInit();
            HeadData headData = new HeadData();
            readHead(headData);
            if (headData.type == 10) {
                newInit.readFrom(this);
                skipToStructEnd();
                return newInit;
            }
            throw new JceDecodeException("type mismatch.");
        } catch (Exception e2) {
            throw new JceDecodeException(e2.getMessage());
        }
    }

    public ByteBuffer getBs() {
        return this.bs;
    }

    public boolean read(boolean z, int i2, boolean z2) {
        return read((byte) 0, i2, z2) != 0;
    }

    public <T> T[] readArray(T[] tArr, int i2, boolean z) {
        if (tArr != null && tArr.length != 0) {
            return (T[]) readArrayImpl(tArr[0], i2, z);
        }
        throw new JceDecodeException("unable to get type of key and value.");
    }

    public String readByteString(String str, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return str;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 == 6) {
            int i3 = this.bs.get();
            if (i3 < 0) {
                i3 += 256;
            }
            byte[] bArr = new byte[i3];
            this.bs.get(bArr);
            return HexUtil.bytes2HexStr(bArr);
        } else if (b2 == 7) {
            int i4 = this.bs.getInt();
            if (i4 <= 104857600 && i4 >= 0 && i4 <= this.bs.capacity()) {
                byte[] bArr2 = new byte[i4];
                this.bs.get(bArr2);
                return HexUtil.bytes2HexStr(bArr2);
            }
            throw new JceDecodeException("String too long: " + i4);
        } else {
            throw new JceDecodeException("type mismatch.");
        }
    }

    public List readList(int i2, boolean z) {
        ArrayList arrayList = new ArrayList();
        if (skipToTag(i2)) {
            HeadData headData = new HeadData();
            readHead(headData);
            if (headData.type == 9) {
                int read = read(0, 0, true);
                if (read < 0) {
                    throw new JceDecodeException("size invalid: " + read);
                }
                for (int i3 = 0; i3 < read; i3++) {
                    HeadData headData2 = new HeadData();
                    readHead(headData2);
                    switch (headData2.type) {
                        case 0:
                            skip(1);
                            break;
                        case 1:
                            skip(2);
                            break;
                        case 2:
                            skip(4);
                            break;
                        case 3:
                            skip(8);
                            break;
                        case 4:
                            skip(4);
                            break;
                        case 5:
                            skip(8);
                            break;
                        case 6:
                            int i4 = this.bs.get();
                            if (i4 < 0) {
                                i4 += 256;
                            }
                            skip(i4);
                            break;
                        case 7:
                            skip(this.bs.getInt());
                            break;
                        case 8:
                        case 9:
                            break;
                        case 10:
                            try {
                                JceStruct jceStruct = (JceStruct) Class.forName(JceStruct.class.getName()).getConstructor(new Class[0]).newInstance(new Object[0]);
                                jceStruct.readFrom(this);
                                skipToStructEnd();
                                arrayList.add(jceStruct);
                                break;
                            } catch (Exception e2) {
                                e2.printStackTrace();
                                throw new JceDecodeException("type mismatch." + e2);
                            }
                        case 11:
                        default:
                            throw new JceDecodeException("type mismatch.");
                        case 12:
                            arrayList.add(Integer.valueOf(0));
                            break;
                    }
                }
            } else {
                throw new JceDecodeException("type mismatch.");
            }
        } else if (z) {
            throw new JceDecodeException("require field not exist.");
        }
        return arrayList;
    }

    public <K, V> HashMap<K, V> readMap(Map<K, V> map, int i2, boolean z) {
        return (HashMap) readMap(new HashMap(), map, i2, z);
    }

    public String readString(int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 == 6) {
            int i3 = this.bs.get();
            if (i3 < 0) {
                i3 += 256;
            }
            byte[] bArr = new byte[i3];
            this.bs.get(bArr);
            try {
                return new String(bArr, this.sServerEncoding);
            } catch (UnsupportedEncodingException unused) {
                return new String(bArr);
            }
        } else if (b2 == 7) {
            int i4 = this.bs.getInt();
            if (i4 <= 104857600 && i4 >= 0 && i4 <= this.bs.capacity()) {
                byte[] bArr2 = new byte[i4];
                this.bs.get(bArr2);
                try {
                    return new String(bArr2, this.sServerEncoding);
                } catch (UnsupportedEncodingException unused2) {
                    return new String(bArr2);
                }
            }
            throw new JceDecodeException("String too long: " + i4);
        } else {
            throw new JceDecodeException("type mismatch.");
        }
    }

    public Map<String, String> readStringMap(int i2, boolean z) {
        HashMap hashMap = new HashMap();
        if (skipToTag(i2)) {
            HeadData headData = new HeadData();
            readHead(headData);
            if (headData.type == 8) {
                int read = read(0, 0, true);
                if (read < 0) {
                    throw new JceDecodeException("size invalid: " + read);
                }
                for (int i3 = 0; i3 < read; i3++) {
                    hashMap.put(readString(0, true), readString(1, true));
                }
            } else {
                throw new JceDecodeException("type mismatch.");
            }
        } else if (z) {
            throw new JceDecodeException("require field not exist.");
        }
        return hashMap;
    }

    public int setServerEncoding(String str) {
        this.sServerEncoding = str;
        return 0;
    }

    public void skipToStructEnd() {
        HeadData headData = new HeadData();
        do {
            readHead(headData);
            skipField(headData.type);
        } while (headData.type != 11);
    }

    public boolean skipToTag(int i2) {
        try {
            HeadData headData = new HeadData();
            while (true) {
                int peakHead = peakHead(headData);
                if (i2 <= headData.tag || headData.type == 11) {
                    break;
                }
                skip(peakHead);
                skipField(headData.type);
            }
            if (headData.type == 11) {
                return false;
            }
            return i2 == headData.tag;
        } catch (JceDecodeException | BufferUnderflowException unused) {
            return false;
        }
    }

    public void warp(byte[] bArr) {
        wrap(bArr);
    }

    public void wrap(byte[] bArr) {
        this.bs = ByteBuffer.wrap(bArr);
    }


    private <K, V> Map<K, V> readMap(Map<K, V> map, Map<K, V> map2, int i2, boolean z) {
        if (map2 != null && !map2.isEmpty()) {
            Map.Entry<K, V> next = map2.entrySet().iterator().next();
            K key = next.getKey();
            V value = next.getValue();
            if (skipToTag(i2)) {
                HeadData headData = new HeadData();
                readHead(headData);
                if (headData.type == 8) {
                    int read = read(0, 0, true);
                    if (read < 0) {
                        throw new JceDecodeException("size invalid: " + read);
                    }
                    for (int i3 = 0; i3 < read; i3++) {
                        map.put((K)read( key, 0, true), (V)read(value, 1, true));
                    }
                } else {
                    throw new JceDecodeException("type mismatch.");
                }
            } else if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return map;
        }
        return new HashMap();
    }

    public byte read(byte b2, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return b2;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b3 = headData.type;
        if (b3 != 0) {
            if (b3 == 12) {
                return (byte) 0;
            }
            throw new JceDecodeException("type mismatch.");
        }
        return this.bs.get();
    }

    public JceInputStream(ByteBuffer byteBuffer) {
        this.bs = byteBuffer;
    }

    private void skipField(byte b2) {
        int i2 = 0;
        switch (b2) {
            case 0:
                skip(1);
                return;
            case 1:
                skip(2);
                return;
            case 2:
                skip(4);
                return;
            case 3:
                skip(8);
                return;
            case 4:
                skip(4);
                return;
            case 5:
                skip(8);
                return;
            case 6:
                int i3 = this.bs.get();
                if (i3 < 0) {
                    i3 += 256;
                }
                skip(i3);
                return;
            case 7:
                skip(this.bs.getInt());
                return;
            case 8:
                int read = read(0, 0, true);
                while (i2 < read * 2) {
                    skipField();
                    i2++;
                }
                return;
            case 9:
                int read2 = read(0, 0, true);
                while (i2 < read2) {
                    skipField();
                    i2++;
                }
                return;
            case 10:
                skipToStructEnd();
                return;
            case 11:
            case 12:
                return;
            case 13:
                HeadData headData = new HeadData();
                readHead(headData);
                if (headData.type == 0) {
                    skip(read(0, 0, true));
                    return;
                }
                throw new JceDecodeException("skipField with invalid type, type value: " + ((int) b2) + ", " + ((int) headData.type));
            default:
                throw new JceDecodeException("invalid type.");
        }
    }

    public <T> List<T> readArray(List<T> list, int i2, boolean z) {
        if (list != null && !list.isEmpty()) {
            Object[] readArrayImpl = readArrayImpl(list.get(0), i2, z);
            if (readArrayImpl == null) {
                return null;
            }
            ArrayList arrayList = new ArrayList();
            for (Object obj : readArrayImpl) {
                arrayList.add(obj);
            }
            return arrayList;
        }
        return new ArrayList();
    }

    public void readHead(HeadData headData) {
        readHead(headData, this.bs);
    }

    public JceInputStream(byte[] bArr) {
        this.bs = ByteBuffer.wrap(bArr);
    }

    public JceInputStream(byte[] bArr, int i2) {
        ByteBuffer wrap = ByteBuffer.wrap(bArr);
        this.bs = wrap;
        wrap.position(i2);
    }

    public short read(short s, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return s;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 != 0) {
            if (b2 != 1) {
                if (b2 == 12) {
                    return (short) 0;
                }
                throw new JceDecodeException("type mismatch.");
            }
            return this.bs.getShort();
        }
        return this.bs.get();
    }

    public int read(int i2, int i3, boolean z) {
        if (!skipToTag(i3)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return i2;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 != 0) {
            if (b2 != 1) {
                if (b2 != 2) {
                    if (b2 == 12) {
                        return 0;
                    }
                    throw new JceDecodeException("type mismatch.");
                }
                return this.bs.getInt();
            }
            return this.bs.getShort();
        }
        return this.bs.get();
    }

    public long read(long j, int i2, boolean z) {
        int i3;
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return j;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 == 0) {
            i3 = this.bs.get();
        } else if (b2 == 1) {
            i3 = this.bs.getShort();
        } else if (b2 != 2) {
            if (b2 != 3) {
                if (b2 == 12) {
                    return 0L;
                }
                throw new JceDecodeException("type mismatch.");
            }
            return this.bs.getLong();
        } else {
            i3 = this.bs.getInt();
        }
        return i3;
    }

    public float read(float f2, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return f2;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 != 4) {
            if (b2 == 12) {
                return 0.0f;
            }
            throw new JceDecodeException("type mismatch.");
        }
        return this.bs.getFloat();
    }

    public double read(double d2, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return d2;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 != 4) {
            if (b2 != 5) {
                if (b2 == 12) {
                    return 0.0d;
                }
                throw new JceDecodeException("type mismatch.");
            }
            return this.bs.getDouble();
        }
        return this.bs.getFloat();
    }

    public String read(String str, int i2, boolean z) {
        String str2;
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return str;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 == 6) {
            int i3 = this.bs.get();
            if (i3 < 0) {
                i3 += 256;
            }
            byte[] bArr = new byte[i3];
            this.bs.get(bArr);
            try {
                str2 = new String(bArr, this.sServerEncoding);
            } catch (UnsupportedEncodingException unused) {
                str2 = new String(bArr);
            }
        } else if (b2 == 7) {
            int i4 = this.bs.getInt();
            if (i4 <= 104857600 && i4 >= 0 && i4 <= this.bs.capacity()) {
                byte[] bArr2 = new byte[i4];
                this.bs.get(bArr2);
                try {
                    str2 = new String(bArr2, this.sServerEncoding);
                } catch (UnsupportedEncodingException unused2) {
                    str2 = new String(bArr2);
                }
            } else {
                throw new JceDecodeException("String too long: " + i4);
            }
        } else {
            throw new JceDecodeException("type mismatch.");
        }
        return str2;
    }

    public String[] read(String[] strArr, int i2, boolean z) {
        return (String[]) readArray(strArr, i2, z);
    }

    public boolean[] read(boolean[] zArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                boolean[] zArr2 = new boolean[read];
                for (int i3 = 0; i3 < read; i3++) {
                    zArr2[i3] = read(zArr2[0], 0, true);
                }
                return zArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public byte[] read(byte[] bArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        byte b2 = headData.type;
        if (b2 == 9) {
            int read = read(0, 0, true);
            if (read >= 0 && read <= this.bs.capacity()) {
                byte[] bArr2 = new byte[read];
                for (int i3 = 0; i3 < read; i3++) {
                    bArr2[i3] = read(bArr2[0], 0, true);
                }
                return bArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        } else if (b2 == 13) {
            HeadData headData2 = new HeadData();
            readHead(headData2);
            if (headData2.type == 0) {
                int read2 = read(0, 0, true);
                if (read2 >= 0 && read2 <= this.bs.capacity()) {
                    byte[] bArr3 = new byte[read2];
                    this.bs.get(bArr3);
                    return bArr3;
                }
                throw new JceDecodeException("invalid size, tag: " + i2 + ", type: " + ((int) headData.type) + ", " + ((int) headData2.type) + ", size: " + read2);
            }
            throw new JceDecodeException("type mismatch, tag: " + i2 + ", type: " + ((int) headData.type) + ", " + ((int) headData2.type));
        } else {
            throw new JceDecodeException("type mismatch.");
        }
    }

    public short[] read(short[] sArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                short[] sArr2 = new short[read];
                for (int i3 = 0; i3 < read; i3++) {
                    sArr2[i3] = read(sArr2[0], 0, true);
                }
                return sArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public int[] read(int[] iArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                int[] iArr2 = new int[read];
                for (int i3 = 0; i3 < read; i3++) {
                    iArr2[i3] = read(iArr2[0], 0, true);
                }
                return iArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public long[] read(long[] jArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                long[] jArr2 = new long[read];
                for (int i3 = 0; i3 < read; i3++) {
                    jArr2[i3] = read(jArr2[0], 0, true);
                }
                return jArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public float[] read(float[] fArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                float[] fArr2 = new float[read];
                for (int i3 = 0; i3 < read; i3++) {
                    fArr2[i3] = read(fArr2[0], 0, true);
                }
                return fArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public double[] read(double[] dArr, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        HeadData headData = new HeadData();
        readHead(headData);
        if (headData.type == 9) {
            int read = read(0, 0, true);
            if (read >= 0) {
                double[] dArr2 = new double[read];
                for (int i3 = 0; i3 < read; i3++) {
                    dArr2[i3] = read(dArr2[0], 0, true);
                }
                return dArr2;
            }
            throw new JceDecodeException("size invalid: " + read);
        }
        throw new JceDecodeException("type mismatch.");
    }

    public JceStruct read(JceStruct jceStruct, int i2, boolean z) {
        if (!skipToTag(i2)) {
            if (z) {
                throw new JceDecodeException("require field not exist.");
            }
            return null;
        }
        try {
            JceStruct jceStruct2 = (JceStruct) jceStruct.getClass().newInstance();
            HeadData headData = new HeadData();
            readHead(headData);
            if (headData.type == 10) {
                jceStruct2.readFrom(this);
                skipToStructEnd();
                return jceStruct2;
            }
            throw new JceDecodeException("type mismatch.");
        } catch (Exception e2) {
            throw new JceDecodeException(e2.getMessage());
        }
    }

    public JceStruct[] read(JceStruct[] jceStructArr, int i2, boolean z) {
        return (JceStruct[]) readArray(jceStructArr, i2, z);
    }

    public <T> Object read(T t, int i2, boolean z) {
        if (t instanceof Byte) {
            return Byte.valueOf(read((byte) 0, i2, z));
        }
        if (t instanceof Boolean) {
            return Boolean.valueOf(read(false, i2, z));
        }
        if (t instanceof Short) {
            return Short.valueOf(read((short) 0, i2, z));
        }
        if (t instanceof Integer) {
            return Integer.valueOf(read(0, i2, z));
        }
        if (t instanceof Long) {
            return Long.valueOf(read(0L, i2, z));
        }
        if (t instanceof Float) {
            return Float.valueOf(read(0.0f, i2, z));
        }
        if (t instanceof Double) {
            return Double.valueOf(read(0.0d, i2, z));
        }
        if (t instanceof String) {
            return readString(i2, z);
        }
        if (t instanceof Map) {
            return readMap((Map) t, i2, z);
        }
        if (t instanceof List) {
            return readArray((List) t, i2, z);
        }
        if (t instanceof JceStruct) {
            return read((JceStruct) t, i2, z);
        }
        if (t.getClass().isArray()) {
            if (!(t instanceof byte[]) && !(t instanceof Byte[])) {
                if (t instanceof boolean[]) {
                    return read((boolean[]) null, i2, z);
                }
                if (t instanceof short[]) {
                    return read((short[]) null, i2, z);
                }
                if (t instanceof int[]) {
                    return read((int[]) null, i2, z);
                }
                if (t instanceof long[]) {
                    return read((long[]) null, i2, z);
                }
                if (t instanceof float[]) {
                    return read((float[]) null, i2, z);
                }
                if (t instanceof double[]) {
                    return read((double[]) null, i2, z);
                }
                return readArray((Object[]) t, i2, z);
            }
            return read((byte[]) null, i2, z);
        }
        throw new JceDecodeException("read object error: unsupport type.");
    }
}
