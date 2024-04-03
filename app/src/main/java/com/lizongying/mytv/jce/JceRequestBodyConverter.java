package com.lizongying.mytv.jce;

import androidx.annotation.NonNull;

import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;
import com.tencent.videolite.android.datamodel.cctvjce.BusinessExtent;
import com.tencent.videolite.android.datamodel.cctvjce.BusinessHead;
import com.tencent.videolite.android.datamodel.cctvjce.QUA;
import com.tencent.videolite.android.datamodel.cctvjce.RequestCommand;
import com.tencent.videolite.android.datamodel.cctvjce.RequestHead;

import okhttp3.MediaType;
import okhttp3.RequestBody;
import retrofit2.Converter;

public class JceRequestBodyConverter<T extends JceStruct> implements Converter<T, RequestBody> {
    private static final MediaType MEDIA_TYPE = MediaType.get("application/octet-stream");

    @Override
    public RequestBody convert(@NonNull T value) {
        BusinessHead businessHead = new BusinessHead();
        RequestHead requestHead = new RequestHead();
        requestHead.appId = "1200013";
        requestHead.busiExtent = new BusinessExtent(1);
        requestHead.cmdId = 24897;
        requestHead.contentType = 0;
        requestHead.guid = "093e7e5989684fd986c44f07542d8dc8";
        requestHead.isSupportDolby = 0;
        requestHead.oemPlatform = 0;
        QUA qua = new QUA();
        qua.appSubVersion = "";
        qua.areaMode = 1;
        qua.channelId = "50001";
        qua.clientKey = "A41CD6B6-6A56-46DC-9AE0-4ECF2E9118DF";
        qua.countryCode = 0;
        qua.densityDpi = 0;
        qua.deviceId = "5EE46760-11B1-5FE7-949A-FF232DAE1823";
        qua.deviceModel = "iPad Pro (12.9 inch) 3G";
        qua.deviceType = 1;
        qua.extent = "";
        qua.idfa = "";
        qua.imei = "";
        qua.langCode = 0;
        qua.mac = "";
        qua.markerId = 1;
        qua.mobileISP = 0;
        qua.networkMode = 0;
        qua.omgId = "78d6ac7572afb5461e9a590f089f893d2abc0010119007";
        qua.platform = 5;
        qua.platformVersion = "sysver=ios17.2&device=iPad&modify_time=&lang=zh_CN";
        qua.qimei = "1c0fc6ed8a53584ae722b915200014317601";
        qua.screenHeight = 2360;
        qua.screenWidth = 1640;
        qua.serverid = "";
        qua.v4ip = "";
        qua.versionCode = "23399";
        qua.versionName = "2.9.0.23399";
        requestHead.qua = qua;
        requestHead.requestId = 1;

        JceOutputStream jceOutputStream = new JceOutputStream();
        jceOutputStream.setServerEncoding("utf-8");
        value.writeTo(jceOutputStream);
        byte[] bytes = jceOutputStream.toByteArray();
        RequestCommand requestCommand = new RequestCommand(requestHead, bytes, businessHead);
        bytes = b.b(requestCommand, requestHead.requestId);
        return RequestBody.create(MEDIA_TYPE, bytes);
    }
}