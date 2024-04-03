package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class ExtentAccount extends JceStruct {
    public String accountId;
    public int type;

    public ExtentAccount() {
        this.type = 0;
        this.accountId = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.type = jceInputStream.read(this.type, 0, true);
        this.accountId = jceInputStream.readString(1, true);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.type, 0);
        jceOutputStream.write(this.accountId, 1);
    }

    public ExtentAccount(int i2, String str) {
        this.type = 0;
        this.accountId = "";
        this.type = i2;
        this.accountId = str;
    }
}
