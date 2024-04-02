package com.qq.taf.jce;

import java.nio.ByteBuffer;

public interface OnIllegalArgumentException {
    void onException(IllegalArgumentException illegalArgumentException, ByteBuffer byteBuffer, int i2, int i3);
}
