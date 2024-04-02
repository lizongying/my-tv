package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import java.util.ArrayList;
import java.util.Collection;

public final class RequestHead extends JceStruct {
    static BusinessExtent cache_busiExtent;
    static ArrayList<ExtentAccount> cache_extentAccountList;
    static LogReport cache_logReport;
    static SafeInfo cache_safeInfo;
    public String appId;
    public BusinessExtent busiExtent;
    public int cmdId;
    public int contentType;
    public ArrayList<ExtentAccount> extentAccountList;
    public String guid;
    public int isSupportDolby;
    public LogReport logReport;
    public int oemPlatform;
    public QUA qua;
    public int requestId;
    public SafeInfo safeInfo;
    public ArrayList<LoginToken> token;
    static QUA cache_qua = new QUA();
    static ArrayList<LoginToken> cache_token = new ArrayList<>();

    static {
        cache_token.add(new LoginToken());
        cache_logReport = new LogReport();
        cache_extentAccountList = new ArrayList<>();
        cache_extentAccountList.add(new ExtentAccount());
        cache_safeInfo = new SafeInfo();
        cache_busiExtent = new BusinessExtent();
    }

    public RequestHead() {
        this.requestId = 0;
        this.cmdId = 0;
        this.qua = null;
        this.appId = "";
        this.guid = "";
        this.token = null;
        this.logReport = null;
        this.extentAccountList = null;
        this.oemPlatform = 0;
        this.isSupportDolby = 0;
        this.contentType = 0;
        this.safeInfo = null;
        this.busiExtent = null;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.requestId = jceInputStream.read(this.requestId, 0, true);
        this.cmdId = jceInputStream.read(this.cmdId, 1, true);
        this.qua = (QUA) jceInputStream.read((JceStruct) cache_qua, 2, false);
        this.appId = jceInputStream.readString(3, false);
        this.guid = jceInputStream.readString(4, false);
        this.token = (ArrayList) jceInputStream.read(cache_token, 5, false);
        this.logReport = (LogReport) jceInputStream.read((JceStruct) cache_logReport, 6, false);
        this.extentAccountList = (ArrayList) jceInputStream.read( cache_extentAccountList, 7, false);
        this.oemPlatform = jceInputStream.read(this.oemPlatform, 8, false);
        this.isSupportDolby = jceInputStream.read(this.isSupportDolby, 9, false);
        this.contentType = jceInputStream.read(this.contentType, 10, false);
        this.safeInfo = (SafeInfo) jceInputStream.read((JceStruct) cache_safeInfo, 11, false);
        this.busiExtent = (BusinessExtent) jceInputStream.read((JceStruct) cache_busiExtent, 12, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.requestId, 0);
        jceOutputStream.write(this.cmdId, 1);
        QUA qua = this.qua;
        if (qua != null) {
            jceOutputStream.write((JceStruct) qua, 2);
        }
        String str = this.appId;
        if (str != null) {
            jceOutputStream.write(str, 3);
        }
        String str2 = this.guid;
        if (str2 != null) {
            jceOutputStream.write(str2, 4);
        }
        ArrayList<LoginToken> arrayList = this.token;
        if (arrayList != null) {
            jceOutputStream.write((Collection) arrayList, 5);
        }
        LogReport logReport = this.logReport;
        if (logReport != null) {
            jceOutputStream.write((JceStruct) logReport, 6);
        }
        ArrayList<ExtentAccount> arrayList2 = this.extentAccountList;
        if (arrayList2 != null) {
            jceOutputStream.write((Collection) arrayList2, 7);
        }
        jceOutputStream.write(this.oemPlatform, 8);
        jceOutputStream.write(this.isSupportDolby, 9);
        jceOutputStream.write(this.contentType, 10);
        SafeInfo safeInfo = this.safeInfo;
        if (safeInfo != null) {
            jceOutputStream.write((JceStruct) safeInfo, 11);
        }
        BusinessExtent businessExtent = this.busiExtent;
        if (businessExtent != null) {
            jceOutputStream.write((JceStruct) businessExtent, 12);
        }
    }

    public RequestHead(int i2, int i3, QUA qua, String str, String str2, ArrayList<LoginToken> arrayList, LogReport logReport, ArrayList<ExtentAccount> arrayList2, int i4, int i5, int i6, SafeInfo safeInfo, BusinessExtent businessExtent) {
        this.requestId = 0;
        this.cmdId = 0;
        this.qua = null;
        this.appId = "";
        this.guid = "";
        this.token = null;
        this.logReport = null;
        this.extentAccountList = null;
        this.oemPlatform = 0;
        this.isSupportDolby = 0;
        this.contentType = 0;
        this.safeInfo = null;
        this.busiExtent = null;
        this.requestId = i2;
        this.cmdId = i3;
        this.qua = qua;
        this.appId = str;
        this.guid = str2;
        this.token = arrayList;
        this.logReport = logReport;
        this.extentAccountList = arrayList2;
        this.oemPlatform = i4;
        this.isSupportDolby = i5;
        this.contentType = i6;
        this.safeInfo = safeInfo;
        this.busiExtent = businessExtent;
    }
}
