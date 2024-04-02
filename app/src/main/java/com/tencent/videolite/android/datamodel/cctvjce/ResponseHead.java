package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class ResponseHead extends JceStruct {
    public int cmdId;
    public int errCode;
    public int requestId;
    public String sUserid;

    public ResponseHead() {
        this.requestId = 0;
        this.cmdId = 0;
        this.errCode = 0;
        this.sUserid = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.requestId = jceInputStream.read(this.requestId, 0, true);
        this.cmdId = jceInputStream.read(this.cmdId, 1, true);
        this.errCode = jceInputStream.read(this.errCode, 2, true);
        this.sUserid = jceInputStream.readString(3, true);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.requestId, 0);
        jceOutputStream.write(this.cmdId, 1);
        jceOutputStream.write(this.errCode, 2);
        jceOutputStream.write(this.sUserid, 3);
    }

    public ResponseHead(int i2, int i3, int i4, String str) {
        this.requestId = 0;
        this.cmdId = 0;
        this.errCode = 0;
        this.sUserid = "";
        this.requestId = i2;
        this.cmdId = i3;
        this.errCode = i4;
        this.sUserid = str;
    }
}
