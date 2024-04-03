package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class BusinessHead extends JceStruct {
    static byte[] cache_head;
    public byte[] head;
    public int type;

    static {
        cache_head = new byte[0];
        byte[] bArr = {0};
    }

    public BusinessHead() {
        this.type = 0;
        this.head = null;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.type = jceInputStream.read(this.type, 0, true);
        this.head = jceInputStream.read(cache_head, 1, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.type, 0);
        byte[] bArr = this.head;
        if (bArr != null) {
            jceOutputStream.write(bArr, 1);
        }
    }

    public BusinessHead(int i2, byte[] bArr) {
        this.type = 0;
        this.head = null;
        this.type = i2;
        this.head = bArr;
    }
}
