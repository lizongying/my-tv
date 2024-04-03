package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class BucketConfig extends JceStruct {
    public int bucketId;
    public String extra;

    public BucketConfig() {
        this.bucketId = 0;
        this.extra = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.bucketId = jceInputStream.read(this.bucketId, 0, true);
        this.extra = jceInputStream.readString(1, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.bucketId, 0);
        String str = this.extra;
        if (str != null) {
            jceOutputStream.write(str, 1);
        }
    }

    public BucketConfig(int i2, String str) {
        this.bucketId = 0;
        this.extra = "";
        this.bucketId = i2;
        this.extra = str;
    }
}
