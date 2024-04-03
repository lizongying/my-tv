package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class LogReport extends JceStruct {
    public String callType;
    public String channelId;
    public String extent;
    public String from;
    public int isAuto;
    public String mid;
    public String pageId;
    public int pageStep;
    public String pid;
    public String refPageId;
    public String vid;

    public LogReport() {
        this.pageId = "";
        this.refPageId = "";
        this.pageStep = 0;
        this.callType = "";
        this.isAuto = 0;
        this.vid = "";
        this.pid = "";
        this.from = "";
        this.channelId = "";
        this.mid = "";
        this.extent = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.pageId = jceInputStream.readString(0, false);
        this.refPageId = jceInputStream.readString(1, false);
        this.pageStep = jceInputStream.read(this.pageStep, 2, false);
        this.callType = jceInputStream.readString(3, false);
        this.isAuto = jceInputStream.read(this.isAuto, 4, false);
        this.vid = jceInputStream.readString(5, false);
        this.pid = jceInputStream.readString(6, false);
        this.from = jceInputStream.readString(7, false);
        this.channelId = jceInputStream.readString(8, false);
        this.mid = jceInputStream.readString(9, false);
        this.extent = jceInputStream.readString(10, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        String str = this.pageId;
        if (str != null) {
            jceOutputStream.write(str, 0);
        }
        String str2 = this.refPageId;
        if (str2 != null) {
            jceOutputStream.write(str2, 1);
        }
        jceOutputStream.write(this.pageStep, 2);
        String str3 = this.callType;
        if (str3 != null) {
            jceOutputStream.write(str3, 3);
        }
        jceOutputStream.write(this.isAuto, 4);
        String str4 = this.vid;
        if (str4 != null) {
            jceOutputStream.write(str4, 5);
        }
        String str5 = this.pid;
        if (str5 != null) {
            jceOutputStream.write(str5, 6);
        }
        String str6 = this.from;
        if (str6 != null) {
            jceOutputStream.write(str6, 7);
        }
        String str7 = this.channelId;
        if (str7 != null) {
            jceOutputStream.write(str7, 8);
        }
        String str8 = this.mid;
        if (str8 != null) {
            jceOutputStream.write(str8, 9);
        }
        String str9 = this.extent;
        if (str9 != null) {
            jceOutputStream.write(str9, 10);
        }
    }

    public LogReport(String str, String str2, int i2, String str3, int i3, String str4, String str5, String str6, String str7, String str8, String str9) {
        this.pageId = "";
        this.refPageId = "";
        this.pageStep = 0;
        this.callType = "";
        this.isAuto = 0;
        this.vid = "";
        this.pid = "";
        this.from = "";
        this.channelId = "";
        this.mid = "";
        this.extent = "";
        this.pageId = str;
        this.refPageId = str2;
        this.pageStep = i2;
        this.callType = str3;
        this.isAuto = i3;
        this.vid = str4;
        this.pid = str5;
        this.from = str6;
        this.channelId = str7;
        this.mid = str8;
        this.extent = str9;
    }
}
