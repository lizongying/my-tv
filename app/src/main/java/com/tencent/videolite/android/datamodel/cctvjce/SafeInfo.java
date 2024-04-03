package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class SafeInfo extends JceStruct {
    static byte[] cache_SafeResult;
    public String SafeKey;
    public byte[] SafeResult;
    public int type;

    static {
        cache_SafeResult = new byte[0];
        byte[] bArr = {0};
    }

    public SafeInfo() {
        this.type = 0;
        this.SafeKey = "";
        this.SafeResult = null;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.type = jceInputStream.read(this.type, 0, false);
        this.SafeKey = jceInputStream.readString(1, false);
        this.SafeResult = jceInputStream.read(cache_SafeResult, 2, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.type, 0);
        String str = this.SafeKey;
        if (str != null) {
            jceOutputStream.write(str, 1);
        }
        byte[] bArr = this.SafeResult;
        if (bArr != null) {
            jceOutputStream.write(bArr, 2);
        }
    }

    public SafeInfo(int i2, String str, byte[] bArr) {
        this.type = 0;
        this.SafeKey = "";
        this.SafeResult = null;
        this.type = i2;
        this.SafeKey = str;
        this.SafeResult = bArr;
    }
}
