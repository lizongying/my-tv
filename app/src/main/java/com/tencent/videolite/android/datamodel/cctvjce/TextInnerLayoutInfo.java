package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
public final class TextInnerLayoutInfo extends JceStruct {
    public int bottomPending;
    public int leftPending;
    public int rightPending;
    public int topPending;

    public TextInnerLayoutInfo() {
        this.leftPending = 0;
        this.topPending = 0;
        this.rightPending = 0;
        this.bottomPending = 0;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.leftPending = jceInputStream.read(this.leftPending, 0, false);
        this.topPending = jceInputStream.read(this.topPending, 1, false);
        this.rightPending = jceInputStream.read(this.rightPending, 2, false);
        this.bottomPending = jceInputStream.read(this.bottomPending, 3, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.leftPending, 0);
        jceOutputStream.write(this.topPending, 1);
        jceOutputStream.write(this.rightPending, 2);
        jceOutputStream.write(this.bottomPending, 3);
    }

    public TextInnerLayoutInfo(int i2, int i3, int i4, int i5) {
        this.leftPending = 0;
        this.topPending = 0;
        this.rightPending = 0;
        this.bottomPending = 0;
        this.leftPending = i2;
        this.topPending = i3;
        this.rightPending = i4;
        this.bottomPending = i5;
    }
}
