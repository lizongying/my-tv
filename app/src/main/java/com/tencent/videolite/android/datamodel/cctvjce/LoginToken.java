package com.tencent.videolite.android.datamodel.cctvjce;

import com.qq.taf.jce.JceInputStream;
import com.qq.taf.jce.JceOutputStream;
import com.qq.taf.jce.JceStruct;

public final class LoginToken extends JceStruct {
    static byte[] cache_TokenValue;
    public boolean IsMainLogin;
    public String TokenAppID;
    public byte TokenKeyType;
    public String TokenUin;
    public byte[] TokenValue;

    static {
        cache_TokenValue =  new byte[0];
        byte[] bArr = {0};
    }

    public LoginToken() {
        this.TokenAppID = "";
        this.TokenKeyType = (byte) 0;
        this.TokenValue = null;
        this.TokenUin = "";
        this.IsMainLogin = true;
    }

    @Override // com.qq.taf.jce.JceStruct
    public void readFrom(JceInputStream jceInputStream) {
        this.TokenAppID = jceInputStream.readString(0, true);
        this.TokenKeyType = jceInputStream.read(this.TokenKeyType, 1, true);
        this.TokenValue = jceInputStream.read(cache_TokenValue, 2, true);
        this.TokenUin = jceInputStream.readString(3, false);
        this.IsMainLogin = jceInputStream.read(this.IsMainLogin, 4, false);
    }

    @Override // com.qq.taf.jce.JceStruct
    public void writeTo(JceOutputStream jceOutputStream) {
        jceOutputStream.write(this.TokenAppID, 0);
        jceOutputStream.write(this.TokenKeyType, 1);
        jceOutputStream.write(this.TokenValue, 2);
        String str = this.TokenUin;
        if (str != null) {
            jceOutputStream.write(str, 3);
        }
        jceOutputStream.write(this.IsMainLogin, 4);
    }

    public LoginToken(String str, byte b2, byte[] bArr, String str2, boolean z) {
        this.TokenAppID = "";
        this.TokenKeyType = (byte) 0;
        this.TokenValue = null;
        this.TokenUin = "";
        this.IsMainLogin = true;
        this.TokenAppID = str;
        this.TokenKeyType = b2;
        this.TokenValue = bArr;
        this.TokenUin = str2;
        this.IsMainLogin = z;
    }
}
