package com.qq.taf.jce;

import java.io.UnsupportedEncodingException;
import java.nio.ByteBuffer;
import java.util.Collection;
import java.util.List;
import java.util.Map;

public class JceOutputStream {
    private ByteBuffer bs;
    private OnIllegalArgumentException exceptionHandler;
    protected String sServerEncoding;

    public JceOutputStream(ByteBuffer byteBuffer) {
        this.sServerEncoding = "GBK";
        this.bs = byteBuffer;
    }

    private void writeArray(Object[] objArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(objArr.length, 0);
        for (Object obj : objArr) {
            write(obj, 0);
        }
    }

    public ByteBuffer getByteBuffer() {
        return this.bs;
    }

    public OnIllegalArgumentException getExceptionHandler() {
        return this.exceptionHandler;
    }

    public void reserve(int i2) {
        if (this.bs.remaining() < i2) {
            int capacity = (this.bs.capacity() + i2) * 2;
            try {
                ByteBuffer allocate = ByteBuffer.allocate(capacity);
                allocate.put(this.bs.array(), 0, this.bs.position());
                this.bs = allocate;
            } catch (IllegalArgumentException e2) {
                OnIllegalArgumentException onIllegalArgumentException = this.exceptionHandler;
                if (onIllegalArgumentException != null) {
                    onIllegalArgumentException.onException(e2, this.bs, i2, capacity);
                }
                throw e2;
            }
        }
    }

    public void setExceptionHandler(OnIllegalArgumentException onIllegalArgumentException) {
        this.exceptionHandler = onIllegalArgumentException;
    }

    public int setServerEncoding(String str) {
        this.sServerEncoding = str;
        return 0;
    }

    public byte[] toByteArray() {
        byte[] bArr = new byte[this.bs.position()];
        System.arraycopy(this.bs.array(), 0, bArr, 0, this.bs.position());
        return bArr;
    }

    public void write(boolean z, int i2) {
        write(z ? (byte) 1 : (byte) 0, i2);
    }

    public void writeByteString(String str, int i2) {
        reserve(str.length() + 10);
        byte[] hexStr2Bytes = HexUtil.hexStr2Bytes(str);
        if (hexStr2Bytes.length > 255) {
            writeHead((byte) 7, i2);
            this.bs.putInt(hexStr2Bytes.length);
            this.bs.put(hexStr2Bytes);
            return;
        }
        writeHead((byte) 6, i2);
        this.bs.put((byte) hexStr2Bytes.length);
        this.bs.put(hexStr2Bytes);
    }

    public void writeHead(byte b2, int i2) {
        if (i2 < 15) {
            this.bs.put((byte) (b2 | (i2 << 4)));
        } else if (i2 < 256) {
            this.bs.put((byte) (b2 | 240));
            this.bs.put((byte) i2);
        } else {
            throw new JceEncodeException("tag is too large: " + i2);
        }
    }

    public void writeStringByte(String str, int i2) {
        byte[] hexStr2Bytes = HexUtil.hexStr2Bytes(str);
        reserve(hexStr2Bytes.length + 10);
        if (hexStr2Bytes.length > 255) {
            writeHead((byte) 7, i2);
            this.bs.putInt(hexStr2Bytes.length);
            this.bs.put(hexStr2Bytes);
            return;
        }
        writeHead((byte) 6, i2);
        this.bs.put((byte) hexStr2Bytes.length);
        this.bs.put(hexStr2Bytes);
    }

    public void write(byte b2, int i2) {
        reserve(3);
        if (b2 == 0) {
            writeHead((byte) 12, i2);
            return;
        }
        writeHead((byte) 0, i2);
        this.bs.put(b2);
    }

    public JceOutputStream(int i2) {
        this.sServerEncoding = "GBK";
        this.bs = ByteBuffer.allocate(i2);
    }

    public void write(short s, int i2) {
        reserve(4);
        if (s >= -128 && s <= 127) {
            write((byte) s, i2);
            return;
        }
        writeHead((byte) 1, i2);
        this.bs.putShort(s);
    }

    public JceOutputStream() {
        this(128);
    }

    public void write(int i2, int i3) {
        reserve(6);
        if (i2 >= -32768 && i2 <= 32767) {
            write((short) i2, i3);
            return;
        }
        writeHead((byte) 2, i3);
        this.bs.putInt(i2);
    }

    public void write(long j, int i2) {
        reserve(10);
        if (j >= -2147483648L && j <= 2147483647L) {
            write((int) j, i2);
            return;
        }
        writeHead((byte) 3, i2);
        this.bs.putLong(j);
    }

    public void write(float f2, int i2) {
        reserve(6);
        writeHead((byte) 4, i2);
        this.bs.putFloat(f2);
    }

    public void write(double d2, int i2) {
        reserve(10);
        writeHead((byte) 5, i2);
        this.bs.putDouble(d2);
    }

    public void write(String str, int i2) {
        byte[] bytes;
        try {
            bytes = str.getBytes(this.sServerEncoding);
        } catch (UnsupportedEncodingException unused) {
            bytes = str.getBytes();
        }
        reserve(bytes.length + 10);
        if (bytes.length > 255) {
            writeHead((byte) 7, i2);
            this.bs.putInt(bytes.length);
            this.bs.put(bytes);
            return;
        }
        writeHead((byte) 6, i2);
        this.bs.put((byte) bytes.length);
        this.bs.put(bytes);
    }

    public <K, V> void write(Map<K, V> map, int i2) {
        reserve(8);
        writeHead((byte) 8, i2);
        write(map == null ? 0 : map.size(), 0);
        if (map != null) {
            for (Map.Entry<K, V> entry : map.entrySet()) {
                write(entry.getKey(), 0);
                write(entry.getValue(), 1);
            }
        }
    }

    public void write(boolean[] zArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(zArr.length, 0);
        for (boolean z : zArr) {
            write(z, 0);
        }
    }

    public void write(byte[] bArr, int i2) {
        reserve(bArr.length + 8);
        writeHead((byte) 13, i2);
        writeHead((byte) 0, 0);
        write(bArr.length, 0);
        this.bs.put(bArr);
    }

    public void write(short[] sArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(sArr.length, 0);
        for (short s : sArr) {
            write(s, 0);
        }
    }

    public void write(int[] iArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(iArr.length, 0);
        for (int i3 : iArr) {
            write(i3, 0);
        }
    }

    public void write(long[] jArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(jArr.length, 0);
        for (long j : jArr) {
            write(j, 0);
        }
    }

    public void write(float[] fArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(fArr.length, 0);
        for (float f2 : fArr) {
            write(f2, 0);
        }
    }

    public void write(double[] dArr, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(dArr.length, 0);
        for (double d2 : dArr) {
            write(d2, 0);
        }
    }

    public <T> void write(T[] tArr, int i2) {
        writeArray(tArr, i2);
    }

    public <T> void write(Collection<T> collection, int i2) {
        reserve(8);
        writeHead((byte) 9, i2);
        write(collection == null ? 0 : collection.size(), 0);
        if (collection != null) {
            for (T t : collection) {
                write(t, 0);
            }
        }
    }

    public void write(JceStruct jceStruct, int i2) {
        reserve(2);
        writeHead((byte) 10, i2);
        jceStruct.writeTo(this);
        reserve(2);
        writeHead((byte) 11, 0);
    }

    public void write(Byte b2, int i2) {
        write(b2.byteValue(), i2);
    }

    public void write(Boolean bool, int i2) {
        write(bool.booleanValue(), i2);
    }

    public void write(Short sh, int i2) {
        write(sh.shortValue(), i2);
    }

    public void write(Integer num, int i2) {
        write(num.intValue(), i2);
    }

    public void write(Long l, int i2) {
        write(l.longValue(), i2);
    }

    public void write(Float f2, int i2) {
        write(f2.floatValue(), i2);
    }

    public void write(Double d2, int i2) {
        write(d2.doubleValue(), i2);
    }

    public void write(Object obj, int i2) {
        if (obj instanceof Byte) {
            write(((Byte) obj).byteValue(), i2);
        } else if (obj instanceof Boolean) {
            write(((Boolean) obj).booleanValue(), i2);
        } else if (obj instanceof Short) {
            write(((Short) obj).shortValue(), i2);
        } else if (obj instanceof Integer) {
            write(((Integer) obj).intValue(), i2);
        } else if (obj instanceof Long) {
            write(((Long) obj).longValue(), i2);
        } else if (obj instanceof Float) {
            write(((Float) obj).floatValue(), i2);
        } else if (obj instanceof Double) {
            write(((Double) obj).doubleValue(), i2);
        } else if (obj instanceof String) {
            write((String) obj, i2);
        } else if (obj instanceof Map) {
            write((Map) obj, i2);
        } else if (obj instanceof List) {
            write((Collection) ((List) obj), i2);
        } else if (obj instanceof JceStruct) {
            write((JceStruct) obj, i2);
        } else if (obj instanceof byte[]) {
            write((byte[]) obj, i2);
        } else if (obj instanceof boolean[]) {
            write((boolean[]) obj, i2);
        } else if (obj instanceof short[]) {
            write((short[]) obj, i2);
        } else if (obj instanceof int[]) {
            write((int[]) obj, i2);
        } else if (obj instanceof long[]) {
            write((long[]) obj, i2);
        } else if (obj instanceof float[]) {
            write((float[]) obj, i2);
        } else if (obj instanceof double[]) {
            write((double[]) obj, i2);
        } else if (obj.getClass().isArray()) {
            writeArray((Object[]) obj, i2);
        } else if (obj instanceof Collection) {
            write((Collection) obj, i2);
        } else {
            throw new JceEncodeException("write object error: unsupport type. " + obj.getClass());
        }
    }
}
