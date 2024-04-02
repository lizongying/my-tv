package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class TVProgram extends JceStruct {
    public int copyRight;
    public int duration;
    public String epg_time;
    public String id;
    public String name;
    public int picScreenFlag;
    public long start_time_stamp;
    public int timeShiftFlag;
    public int videoScreenFlag;

    public TVProgram() {
        this.id = "";
        this.start_time_stamp = 0L;
        this.epg_time = "";
        this.name = "";
        this.copyRight = 0;
        this.timeShiftFlag = 0;
        this.duration = 0;
        this.picScreenFlag = 0;
        this.videoScreenFlag = 0;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.id = jceInputStream.readString(0, false);
        this.start_time_stamp = jceInputStream.read(this.start_time_stamp, 1, false);
        this.epg_time = jceInputStream.readString(2, false);
        this.name = jceInputStream.readString(3, false);
        this.copyRight = jceInputStream.read(this.copyRight, 4, false);
        this.timeShiftFlag = jceInputStream.read(this.timeShiftFlag, 5, false);
        this.duration = jceInputStream.read(this.duration, 6, false);
        this.picScreenFlag = jceInputStream.read(this.picScreenFlag, 7, false);
        this.videoScreenFlag = jceInputStream.read(this.videoScreenFlag, 8, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        String str = this.id;
        if (str != null) {
            jceOutputStream.write(str, 0);
        }
        jceOutputStream.write(this.start_time_stamp, 1);
        String str2 = this.epg_time;
        if (str2 != null) {
            jceOutputStream.write(str2, 2);
        }
        String str3 = this.name;
        if (str3 != null) {
            jceOutputStream.write(str3, 3);
        }
        jceOutputStream.write(this.copyRight, 4);
        jceOutputStream.write(this.timeShiftFlag, 5);
        jceOutputStream.write(this.duration, 6);
        jceOutputStream.write(this.picScreenFlag, 7);
        jceOutputStream.write(this.videoScreenFlag, 8);
    }

    public TVProgram(String str, long j, String str2, String str3, int i2, int i3, int i4, int i5, int i6) {
        this.id = "";
        this.start_time_stamp = 0L;
        this.epg_time = "";
        this.name = "";
        this.copyRight = 0;
        this.timeShiftFlag = 0;
        this.duration = 0;
        this.picScreenFlag = 0;
        this.videoScreenFlag = 0;
        this.id = str;
        this.start_time_stamp = j;
        this.epg_time = str2;
        this.name = str3;
        this.copyRight = i2;
        this.timeShiftFlag = i3;
        this.duration = i4;
        this.picScreenFlag = i5;
        this.videoScreenFlag = i6;
    }
}
