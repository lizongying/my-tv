package com.lizongying.mytv.jce;

import androidx.annotation.NonNull;

import java.lang.annotation.Annotation;
import java.lang.reflect.Type;

import okhttp3.RequestBody;
import okhttp3.ResponseBody;
import retrofit2.Converter;
import retrofit2.Retrofit;

public class JceConverterFactory extends Converter.Factory {
    public static JceConverterFactory create() {
        return new JceConverterFactory();
    }

    private JceConverterFactory() {
    }

    @Override
    public Converter<ResponseBody, ?> responseBodyConverter(
            @NonNull Type type,
            @NonNull Annotation[] annotations,
            @NonNull Retrofit retrofit) {
        if (!(type instanceof Class<?>)) {
            return null;
        }

        return new JceResponseBodyConverter<>();
    }

    @Override
    public Converter<?, RequestBody> requestBodyConverter(
            @NonNull Type type,
            @NonNull Annotation[] parameterAnnotations,
            @NonNull Annotation[] methodAnnotations,
            @NonNull Retrofit retrofit) {
        if (!(type instanceof Class<?>)) {
            return null;
        }

        return new JceRequestBodyConverter<>();
    }
}
