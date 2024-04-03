package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class BusinessExtent extends JceStruct {
    public int launchType;

    public BusinessExtent() {
        this.launchType = 0;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.launchType = jceInputStream.read(this.launchType, 0, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.launchType, 0);
    }

    public BusinessExtent(int i2) {
        this.launchType = 0;
        this.launchType = i2;
    }
}
