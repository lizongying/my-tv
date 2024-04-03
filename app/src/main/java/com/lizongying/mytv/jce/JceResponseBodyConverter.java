package com.lizongying.mytv.jce;

import androidx.annotation.NonNull;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceStruct;
import com.tencent.videolite.android.datamodel.cctvjce.QQVideoJCECmd;
import com.tencent.videolite.android.datamodel.cctvjce.ResponseCommand;

import java.io.IOException;

import okhttp3.ResponseBody;
import retrofit2.Converter;

public class JceResponseBodyConverter<T extends JceStruct> implements Converter<ResponseBody, T> {

    JceResponseBodyConverter() {
    }

    @Override
    public T convert(@NonNull ResponseBody value) throws IOException {
        return parseFrom(value.bytes());
    }

    public T parseFrom(byte[] aa) {
        byte[] bArr;

        int[] iArr = new int[1];
        try {
            bArr = b.a(aa, iArr);
        } catch (Throwable unused) {
            bArr = null;
        }

        ResponseCommand responseCommand = a.g(bArr, ResponseCommand.class);
        if (responseCommand == null) {
            return null;
        }

        QQVideoJCECmd convert = QQVideoJCECmd.convert(responseCommand.head.cmdId);
        assert convert != null;

        JceInputStream jceInputStream = new JceInputStream(responseCommand.body);
        jceInputStream.setServerEncoding("UTF-8");

        JceStruct instance = null;
        try {
            instance = (JceStruct) Class.forName("com.tencent.videolite.android.datamodel.cctvjce." + convert + "Response").newInstance();
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (ClassNotFoundException e) {
            e.printStackTrace();
        }

        if (instance != null) {
            instance.readFrom(jceInputStream);
            return (T) instance;
        } else {
            return null;
        }
    }
}
