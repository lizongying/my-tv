package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class Coordinates extends JceStruct {
    public double accuracy;
    public float latitude;
    public float longitude;
    public int type;

    public Coordinates() {
        this.type = 0;
        this.latitude = 0.0f;
        this.longitude = 0.0f;
        this.accuracy = 0.0d;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.type = jceInputStream.read(this.type, 0, false);
        this.latitude = jceInputStream.read(this.latitude, 1, false);
        this.longitude = jceInputStream.read(this.longitude, 2, false);
        this.accuracy = jceInputStream.read(this.accuracy, 3, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.type, 0);
        jceOutputStream.write(this.latitude, 1);
        jceOutputStream.write(this.longitude, 2);
        jceOutputStream.write(this.accuracy, 3);
    }

    public Coordinates(int i2, float f2, float f3, double d2) {
        this.type = 0;
        this.latitude = 0.0f;
        this.longitude = 0.0f;
        this.accuracy = 0.0d;
        this.type = i2;
        this.latitude = f2;
        this.longitude = f3;
        this.accuracy = d2;
    }
}
