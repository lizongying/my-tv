package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class TVTimeShiftProgramRequest extends JceStruct {
    public String pid;

    public TVTimeShiftProgramRequest() {
        this.pid = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.pid = jceInputStream.readString(0, true);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.pid, 0);
    }

    public TVTimeShiftProgramRequest(String pid) {
        this.pid = pid;
    }
}
