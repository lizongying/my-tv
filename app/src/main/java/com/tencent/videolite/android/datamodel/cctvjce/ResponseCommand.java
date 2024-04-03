package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class ResponseCommand extends JceStruct {
    static byte[] cache_body;
    public byte[] body;
    public BusinessHead businessHead;
    public ResponseHead head;
    static ResponseHead cache_head = new ResponseHead();
    static BusinessHead cache_businessHead = new BusinessHead();

    static {
        cache_body = new byte[0];
        byte[] bArr = {0};
    }

    public ResponseCommand() {
        this.head = null;
        this.body = null;
        this.businessHead = null;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.head = (ResponseHead) jceInputStream.read((JceStruct) cache_head, 0, true);
        this.body = jceInputStream.read(cache_body, 1, false);
        this.businessHead = (BusinessHead) jceInputStream.read((JceStruct) cache_businessHead, 2, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write((JceStruct) this.head, 0);
        byte[] bArr = this.body;
        if (bArr != null) {
            jceOutputStream.write(bArr, 1);
        }
        BusinessHead businessHead = this.businessHead;
        if (businessHead != null) {
            jceOutputStream.write((JceStruct) businessHead, 2);
        }
    }

    public ResponseCommand(ResponseHead responseHead, byte[] bArr, BusinessHead businessHead) {
        this.head = null;
        this.body = null;
        this.businessHead = null;
        this.head = responseHead;
        this.body = bArr;
        this.businessHead = businessHead;
    }
}
