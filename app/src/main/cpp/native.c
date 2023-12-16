#include "stdio.h"
#include "jni.h"
#include <openssl/rand.h>
#include <string.h>
#include <openssl/evp.h>
#include <openssl/md5.h>
#include <android/log.h>
#include <stdbool.h>
#include <ctype.h>

#include <stdlib.h>

#define APP_SIGNATURE "f0005d8a185c47e72946053a0af5c0f5620605dd9e301ef0937e477f607ab0d7"
#define APP_SIGNATURE2 "9e58fc98c3967ae80eb25541241895368a900fa488e015b08614575c18fab95e"

#define TAG    "native"
#define LOGD(...)  __android_log_print(ANDROID_LOG_DEBUG, TAG, __VA_ARGS__)
#define LOGI(...)  __android_log_print(ANDROID_LOG_INFO, TAG, __VA_ARGS__)
#define LOGW(...)  __android_log_print(ANDROID_LOG_WARN, TAG, __VA_ARGS__)
#define LOGE(...)  __android_log_print(ANDROID_LOG_ERROR, TAG, __VA_ARGS__)

static bool signatureVerified = false;

static const char key[33] = "48e5918a74ae21c972b90cce8af6c8be";
static const char iv[33] = "9a7e7d23610266b1d9fbf98581384d92";
static const char appendString[15] = "0f$IVHi9Qno?G";

static void toUpperCase(char *str) {
    while (*str) {
        *str = toupper((unsigned char) *str);
        str++;
    }
}

static char *binaryToHex(const unsigned char *data, size_t dataSize) {
    char *hexString = (char *) malloc(dataSize * 2 + 1);  // 每个字节需要两个十六进制字符，再加上字符串结束符
    if (hexString == NULL) {
        // 内存分配失败
        return NULL;
    }

    for (size_t i = 0; i < dataSize; ++i) {
        sprintf(hexString + i * 2, "%02X", data[i]);
    }

    hexString[dataSize * 2] = '\0';  // 添加字符串结束符

    return hexString;
}

static unsigned char *hexStringToBinary(const char *hexString1) {
    size_t inputLength = strlen(hexString1);
    if (inputLength % 2 != 0) {
        return NULL;
    }

    size_t length = inputLength / 2;
    unsigned char *result1 = (unsigned char *) malloc(length);

    for (size_t i = 0; i < length; i++) {
        char byteString[3] = {hexString1[2 * i], hexString1[2 * i + 1], '\0'};
        result1[i] = (unsigned char) strtoul(byteString, NULL, 16);
    }

    return result1;
}

static char *aes(const char *plaintext) {
    EVP_CIPHER_CTX *ctx = EVP_CIPHER_CTX_new();
    if (ctx == NULL) {
        return NULL;
    }

//    const char k[33] = "48e5918a74ae21c972b90cce8af6c8be";
    unsigned char *resultKey = hexStringToBinary(key);

//    const char i[33] = "9a7e7d23610266b1d9fbf98581384d92";
    unsigned char *resultIv = hexStringToBinary(iv);

    if (EVP_EncryptInit_ex(ctx, EVP_aes_128_cbc(), NULL, resultKey, resultIv) != 1) {
        free(resultKey);
        free(resultIv);
        EVP_CIPHER_CTX_free(ctx);
        return NULL;
    }

    EVP_CIPHER_CTX_set_padding(ctx, 1);

    size_t dataLength = strlen(plaintext);

    size_t maxEncryptedLength = dataLength + EVP_CIPHER_block_size(EVP_aes_128_cbc());
    unsigned char *encryptedData = (unsigned char *) malloc(maxEncryptedLength);
    if (encryptedData == NULL) {
        free(resultKey);
        free(resultIv);
        EVP_CIPHER_CTX_free(ctx);
        return NULL;
    }

    int len;
    if (EVP_EncryptUpdate(ctx, encryptedData, &len, (const unsigned char *) plaintext,
                          (int) dataLength) !=
        1) {
        free(resultKey);
        free(resultIv);
        free(encryptedData);
        EVP_CIPHER_CTX_free(ctx);
        return NULL;
    }

    size_t encryptedLength = len;

    if (EVP_EncryptFinal_ex(ctx, encryptedData + len, &len) != 1) {
        free(resultKey);
        free(resultIv);
        free(encryptedData);
        EVP_CIPHER_CTX_free(ctx);
        return NULL;
    }

    // 更新加密数据长度
    encryptedLength += len;

    // 创建一个 C 字符串以容纳加密后的数据
    char *encryptedString = binaryToHex(encryptedData, encryptedLength);
    if (encryptedString == NULL) {
        // 内存分配失败
        free(resultKey);
        free(resultIv);
        free(encryptedData);
        EVP_CIPHER_CTX_free(ctx);
        return NULL;
    }

    // 释放内存和资源
    free(resultKey);
    free(resultIv);
    free(encryptedData);
    EVP_CIPHER_CTX_free(ctx);

    return encryptedString;
}

jbyte *appendStringToEnd(jbyte *data, size_t *dataLength) {
    // 获取原始数据长度
    size_t originalLength = *dataLength;

    size_t appendStringLength = strlen(appendString);

    // 分配新的内存空间，包括原始数据和附加字符串
    data = (jbyte *) realloc(data,
                             originalLength + appendStringLength + 1);

    if (data == NULL) {
        // 内存分配失败的处理
        return NULL;
    }

    // 将附加字符串复制到新的内存空间的末尾
    memcpy(data + originalLength, appendString, appendStringLength);

    // 在新数据的末尾添加 null 终止字符
    data[originalLength + appendStringLength] = '\0';

    // 更新数据长度
    *dataLength = originalLength + appendStringLength + 1;

    return data;
}

JNIEXPORT jbyteArray

JNICALL
Java_com_lizongying_mytv_Encryptor_hash(JNIEnv *env, jobject thiz, jbyteArray data_) {
    if (signatureVerified == false) {
        return NULL;
    }

    jbyte *data = (*env)->GetByteArrayElements(env, data_, NULL);

    // 获取数据长度
    size_t dataLength = (*env)->GetArrayLength(env, data_);

//    LOGI("appendStringLength %s", data);
    data = appendStringToEnd(data, &dataLength);

    dataLength = strlen((const char *) data);
//    LOGI("dataLength %d", dataLength);

    // 分配内存以存储 MD5 值（16 字节）
    unsigned char md5Digest[EVP_MAX_MD_SIZE];
    unsigned int md5DigestLength;

    // 创建 MD5 上下文
    EVP_MD_CTX *md5Context = EVP_MD_CTX_new();
    if (md5Context == NULL) {
        // 处理错误
        (*env)->ReleaseByteArrayElements(env, data_, data, 0);
        return NULL;
    }

    // 初始化 MD5 上下文
    if (EVP_DigestInit_ex(md5Context, EVP_md5(), NULL) != 1) {
        // 处理错误
        EVP_MD_CTX_free(md5Context);
        (*env)->ReleaseByteArrayElements(env, data_, data, 0);
        return NULL;
    }

    // 更新 MD5 上下文
    if (EVP_DigestUpdate(md5Context, data, dataLength) != 1) {
        // 处理错误
        EVP_MD_CTX_free(md5Context);
        (*env)->ReleaseByteArrayElements(env, data_, data, 0);
        return NULL;
    }

    // 获取 MD5 值
    if (EVP_DigestFinal_ex(md5Context, md5Digest, &md5DigestLength) != 1) {
        // 处理错误
        EVP_MD_CTX_free(md5Context);
        (*env)->ReleaseByteArrayElements(env, data_, data, 0);
        return NULL;
    }

    jsize md5Length = (jsize) md5DigestLength;

    // 创建 Java 字节数组以容纳 MD5 值
    jbyteArray md5Array = (*env)->NewByteArray(env, md5Length);
    if (md5Array == NULL) {
        // 处理错误
        EVP_MD_CTX_free(md5Context);
        (*env)->ReleaseByteArrayElements(env, data_, data, 0);
        return NULL;
    }

    // 将 MD5 值复制到 Java 字节数组
    (*env)->SetByteArrayRegion(env, md5Array, 0, md5Length, (jbyte *) md5Digest);

    // 释放内存和资源
    EVP_MD_CTX_free(md5Context);
    (*env)->ReleaseByteArrayElements(env, data_, data, 0);
    return md5Array;
}

JNIEXPORT void JNICALL
Java_com_lizongying_mytv_Encryptor_init(JNIEnv *env, jobject thiz, jobject context) {
    // 获取 Context 类
    jclass contextClass = (*env)->GetObjectClass(env, context);

    // 获取 getAppSignature 方法的 ID
    jmethodID methodId = (*env)->GetMethodID(env, contextClass, "getAppSignature",
                                             "()Ljava/lang/String;");

    // 调用 Kotlin 函数
    jstring result = (jstring) (*env)->CallObjectMethod(env, context, methodId);

    // 检查 result 是否为 NULL
    if (result == NULL) {
        return;
    }

    // 在 C 端处理字符串
    const char *utfChars = (*env)->GetStringUTFChars(env, result, NULL);

    // 比较签名哈希
    signatureVerified = (strcmp(utfChars, APP_SIGNATURE) == 0);

    if (signatureVerified == 0 ) {
        signatureVerified = (strcmp(utfChars, APP_SIGNATURE2) == 0);
    }

    // 释放字符串
    (*env)->ReleaseStringUTFChars(env, result, utfChars);
}

JNIEXPORT jstring JNICALL
Java_com_lizongying_mytv_Encryptor_encrypt(JNIEnv *env, jobject thiz, jstring t, jstring e,
                                           jstring r, jstring n, jstring i) {
    if (signatureVerified == false) {
        return NULL;
    }

    const char *t_str = (*env)->GetStringUTFChars(env, t, NULL);
    const char *e_str = (*env)->GetStringUTFChars(env, e, NULL);
    const char *r_str = (*env)->GetStringUTFChars(env, r, NULL);
    const char *n_str = (*env)->GetStringUTFChars(env, n, NULL);
    const char *i_str = (*env)->GetStringUTFChars(env, i, NULL);

    char El[256];
    snprintf(El, sizeof(El),
             "|%s|%s|%s|%s|%s|%s|https://www.yangshipin.c|mozilla/5.0 (macintosh; ||Mozilla|Netscape|MacIntel|",
             t_str, e_str, "mg3c3b04ba", r_str, n_str, i_str);
//    LOGI("EL %s", El);

    unsigned int xl = 0;
    for (int Ll = 0; Ll < strlen(El); Ll++) {
        xl = (xl << 5) - xl + (unsigned char) El[Ll];
        xl &= xl;
    }

    char plaintext[256];
    snprintf(plaintext, sizeof(plaintext), "|%d%s", xl, El);

    // Encrypt the plaintext
    char *encryptedResult = aes(plaintext);
    toUpperCase(encryptedResult);

    char result[512];
    snprintf(result, sizeof(result), "--01%s", encryptedResult);

//    LOGI("result %s", result);

    // Free the memory allocated in the encrypt function
    free(encryptedResult);

    (*env)->ReleaseStringUTFChars(env, t, t_str);
    (*env)->ReleaseStringUTFChars(env, e, e_str);
    (*env)->ReleaseStringUTFChars(env, r, r_str);
    (*env)->ReleaseStringUTFChars(env, n, n_str);
    (*env)->ReleaseStringUTFChars(env, i, i_str);

    // Create a Java string from the result and return it
    jstring resultString = (*env)->NewStringUTF(env, result);

    return resultString;
}