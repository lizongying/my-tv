package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class QUA extends JceStruct {
    public String appSubVersion;
    public int areaMode;
    public String channelId;
    public String clientKey;
    public Coordinates coordinates;
    public int countryCode;
    public int densityDpi;
    public String deviceId;
    public String deviceModel;
    public int deviceType;
    public String extent;
    public ExtentData extentData;
    public String idfa;
    public String imei;
    public String imsi;
    public int langCode;
    public String mac;
    public int markerId;
    public int mobileISP;
    public int networkMode;
    public String omgId;
    public int platform;
    public String platformVersion;
    public String qimei;
    public int screenHeight;
    public int screenWidth;
    public String serverid;
    public String v4ip;
    public String versionCode;
    public String versionName;
    static ExtentData cache_extentData = new ExtentData();
    static Coordinates cache_coordinates = new Coordinates();

    public QUA() {
        this.versionName = "";
        this.versionCode = "";
        this.screenWidth = 0;
        this.screenHeight = 0;
        this.platform = 0;
        this.platformVersion = "";
        this.markerId = 0;
        this.networkMode = 0;
        this.densityDpi = 0;
        this.channelId = "";
        this.imei = "";
        this.imsi = "";
        this.idfa = "";
        this.omgId = "";
        this.extent = "";
        this.extentData = null;
        this.clientKey = "";
        this.mac = "";
        this.serverid = "";
        this.coordinates = null;
        this.deviceId = "";
        this.deviceModel = "";
        this.deviceType = 0;
        this.mobileISP = 0;
        this.areaMode = 0;
        this.countryCode = 0;
        this.langCode = 0;
        this.appSubVersion = "";
        this.v4ip = "";
        this.qimei = "";
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.versionName = jceInputStream.readString(0, true);
        this.versionCode = jceInputStream.readString(1, true);
        this.screenWidth = jceInputStream.read(this.screenWidth, 2, false);
        this.screenHeight = jceInputStream.read(this.screenHeight, 3, false);
        this.platform = jceInputStream.read(this.platform, 4, false);
        this.platformVersion = jceInputStream.readString(5, false);
        this.markerId = jceInputStream.read(this.markerId, 6, false);
        this.networkMode = jceInputStream.read(this.networkMode, 7, false);
        this.densityDpi = jceInputStream.read(this.densityDpi, 8, false);
        this.channelId = jceInputStream.readString(9, false);
        this.imei = jceInputStream.readString(10, false);
        this.imsi = jceInputStream.readString(11, false);
        this.idfa = jceInputStream.readString(12, false);
        this.omgId = jceInputStream.readString(13, false);
        this.extent = jceInputStream.readString(14, false);
        this.extentData = (ExtentData) jceInputStream.read((JceStruct) cache_extentData, 15, false);
        this.clientKey = jceInputStream.readString(16, false);
        this.mac = jceInputStream.readString(17, false);
        this.serverid = jceInputStream.readString(18, false);
        this.coordinates = (Coordinates) jceInputStream.read((JceStruct) cache_coordinates, 19, false);
        this.deviceId = jceInputStream.readString(20, false);
        this.deviceModel = jceInputStream.readString(21, false);
        this.deviceType = jceInputStream.read(this.deviceType, 22, false);
        this.mobileISP = jceInputStream.read(this.mobileISP, 23, false);
        this.areaMode = jceInputStream.read(this.areaMode, 24, false);
        this.countryCode = jceInputStream.read(this.countryCode, 25, false);
        this.langCode = jceInputStream.read(this.langCode, 26, false);
        this.appSubVersion = jceInputStream.readString(27, false);
        this.v4ip = jceInputStream.readString(28, false);
        this.qimei = jceInputStream.readString(29, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.versionName, 0);
        jceOutputStream.write(this.versionCode, 1);
        jceOutputStream.write(this.screenWidth, 2);
        jceOutputStream.write(this.screenHeight, 3);
        jceOutputStream.write(this.platform, 4);
        String str = this.platformVersion;
        if (str != null) {
            jceOutputStream.write(str, 5);
        }
        jceOutputStream.write(this.markerId, 6);
        jceOutputStream.write(this.networkMode, 7);
        jceOutputStream.write(this.densityDpi, 8);
        String str2 = this.channelId;
        if (str2 != null) {
            jceOutputStream.write(str2, 9);
        }
        String str3 = this.imei;
        if (str3 != null) {
            jceOutputStream.write(str3, 10);
        }
        String str4 = this.imsi;
        if (str4 != null) {
            jceOutputStream.write(str4, 11);
        }
        String str5 = this.idfa;
        if (str5 != null) {
            jceOutputStream.write(str5, 12);
        }
        String str6 = this.omgId;
        if (str6 != null) {
            jceOutputStream.write(str6, 13);
        }
        String str7 = this.extent;
        if (str7 != null) {
            jceOutputStream.write(str7, 14);
        }
        ExtentData extentData = this.extentData;
        if (extentData != null) {
            jceOutputStream.write((JceStruct) extentData, 15);
        }
        String str8 = this.clientKey;
        if (str8 != null) {
            jceOutputStream.write(str8, 16);
        }
        String str9 = this.mac;
        if (str9 != null) {
            jceOutputStream.write(str9, 17);
        }
        String str10 = this.serverid;
        if (str10 != null) {
            jceOutputStream.write(str10, 18);
        }
        Coordinates coordinates = this.coordinates;
        if (coordinates != null) {
            jceOutputStream.write((JceStruct) coordinates, 19);
        }
        String str11 = this.deviceId;
        if (str11 != null) {
            jceOutputStream.write(str11, 20);
        }
        String str12 = this.deviceModel;
        if (str12 != null) {
            jceOutputStream.write(str12, 21);
        }
        jceOutputStream.write(this.deviceType, 22);
        jceOutputStream.write(this.mobileISP, 23);
        jceOutputStream.write(this.areaMode, 24);
        jceOutputStream.write(this.countryCode, 25);
        jceOutputStream.write(this.langCode, 26);
        String str13 = this.appSubVersion;
        if (str13 != null) {
            jceOutputStream.write(str13, 27);
        }
        String str14 = this.v4ip;
        if (str14 != null) {
            jceOutputStream.write(str14, 28);
        }
        String str15 = this.qimei;
        if (str15 != null) {
            jceOutputStream.write(str15, 29);
        }
    }

    public QUA(String str, String str2, int i2, int i3, int i4, String str3, int i5, int i6, int i7, String str4, String str5, String str6, String str7, String str8, String str9, ExtentData extentData, String str10, String str11, String str12, Coordinates coordinates, String str13, String str14, int i8, int i9, int i10, int i11, int i12, String str15, String str16, String str17) {
        this.versionName = "";
        this.versionCode = "";
        this.screenWidth = 0;
        this.screenHeight = 0;
        this.platform = 0;
        this.platformVersion = "";
        this.markerId = 0;
        this.networkMode = 0;
        this.densityDpi = 0;
        this.channelId = "";
        this.imei = "";
        this.imsi = "";
        this.idfa = "";
        this.omgId = "";
        this.extent = "";
        this.extentData = null;
        this.clientKey = "";
        this.mac = "";
        this.serverid = "";
        this.coordinates = null;
        this.deviceId = "";
        this.deviceModel = "";
        this.deviceType = 0;
        this.mobileISP = 0;
        this.areaMode = 0;
        this.countryCode = 0;
        this.langCode = 0;
        this.appSubVersion = "";
        this.v4ip = "";
        this.qimei = "";
        this.versionName = str;
        this.versionCode = str2;
        this.screenWidth = i2;
        this.screenHeight = i3;
        this.platform = i4;
        this.platformVersion = str3;
        this.markerId = i5;
        this.networkMode = i6;
        this.densityDpi = i7;
        this.channelId = str4;
        this.imei = str5;
        this.imsi = str6;
        this.idfa = str7;
        this.omgId = str8;
        this.extent = str9;
        this.extentData = extentData;
        this.clientKey = str10;
        this.mac = str11;
        this.serverid = str12;
        this.coordinates = coordinates;
        this.deviceId = str13;
        this.deviceModel = str14;
        this.deviceType = i8;
        this.mobileISP = i9;
        this.areaMode = i10;
        this.countryCode = i11;
        this.langCode = i12;
        this.appSubVersion = str15;
        this.v4ip = str16;
        this.qimei = str17;
    }
}
