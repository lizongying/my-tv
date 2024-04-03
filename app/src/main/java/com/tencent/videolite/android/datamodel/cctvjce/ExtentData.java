package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class ExtentData extends JceStruct {
    static BucketConfig cache_bucketInfo = new BucketConfig();
    public BucketConfig bucketInfo;
    public int checkFlag;
    public String extra;
    public byte flagByte;

    public ExtentData() {
        this.checkFlag = 0;
        this.flagByte = (byte) 0;
        this.extra = "";
        this.bucketInfo = null;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.checkFlag = jceInputStream.read(this.checkFlag, 0, false);
        this.flagByte = jceInputStream.read(this.flagByte, 1, false);
        this.extra = jceInputStream.readString(2, false);
        this.bucketInfo = (BucketConfig) jceInputStream.read((JceStruct) cache_bucketInfo, 3, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.checkFlag, 0);
        jceOutputStream.write(this.flagByte, 1);
        String str = this.extra;
        if (str != null) {
            jceOutputStream.write(str, 2);
        }
        BucketConfig bucketConfig = this.bucketInfo;
        if (bucketConfig != null) {
            jceOutputStream.write((JceStruct) bucketConfig, 3);
        }
    }

    public ExtentData(int i2, byte b2, String str, BucketConfig bucketConfig) {
        this.checkFlag = 0;
        this.flagByte = (byte) 0;
        this.extra = "";
        this.bucketInfo = null;
        this.checkFlag = i2;
        this.flagByte = b2;
        this.extra = str;
        this.bucketInfo = bucketConfig;
    }
}
