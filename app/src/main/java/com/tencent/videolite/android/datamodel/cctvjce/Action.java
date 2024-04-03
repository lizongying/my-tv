package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class Action extends JceStruct {
    public String reportEventId;
    public String reportKey;
    public String reportParams;
    public String url;

    public Action() {
        this.url = "";
        this.reportEventId = "";
        this.reportKey = "";
        this.reportParams = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.url = jceInputStream.readString(0, true);
        this.reportEventId = jceInputStream.readString(1, false);
        this.reportKey = jceInputStream.readString(2, false);
        this.reportParams = jceInputStream.readString(3, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.url, 0);
        String str = this.reportEventId;
        if (str != null) {
            jceOutputStream.write(str, 1);
        }
        String str2 = this.reportKey;
        if (str2 != null) {
            jceOutputStream.write(str2, 2);
        }
        String str3 = this.reportParams;
        if (str3 != null) {
            jceOutputStream.write(str3, 3);
        }
    }

    public Action(String str, String str2, String str3, String str4) {
        this.url = "";
        this.reportEventId = "";
        this.reportKey = "";
        this.reportParams = "";
        this.url = str;
        this.reportEventId = str2;
        this.reportKey = str3;
        this.reportParams = str4;
    }
}
