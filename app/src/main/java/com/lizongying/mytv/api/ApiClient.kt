package com.lizongying.mytv.api


import okhttp3.OkHttpClient
import retrofit2.Retrofit
import retrofit2.converter.gson.GsonConverterFactory
import retrofit2.converter.protobuf.ProtoConverterFactory
import javax.net.ssl.SSLContext
import javax.net.ssl.TrustManager
import javax.net.ssl.X509TrustManager


class ApiClient {
    private val yspUrl = "https://player-api.yangshipin.cn/"
    private val myUrl = "https://lyrics.run/"
    private val devUrl = "http://10.0.2.2:8081/"
    private val protoUrl = "https://capi.yangshipin.cn/"
    private val traceUrl = "https://btrace.yangshipin.cn/"
    private val fUrl = "https://m.fengshows.com/"

    private var okHttpClient = getUnsafeOkHttpClient()

    val yspApiService: YSPApiService by lazy {
        Retrofit.Builder()
            .baseUrl(yspUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPApiService::class.java)
    }

    val yspTokenService: YSPTokenService by lazy {
        Retrofit.Builder()
            .baseUrl(myUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPTokenService::class.java)
    }

    val releaseService: ReleaseService by lazy {
        Retrofit.Builder()
            .baseUrl(myUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(ReleaseService::class.java)
    }

    val yspProtoService: YSPProtoService by lazy {
        Retrofit.Builder()
            .baseUrl(protoUrl)
            .client(okHttpClient)
            .addConverterFactory(ProtoConverterFactory.create())
            .build().create(YSPProtoService::class.java)
    }

    val yspBtraceService: YSPBtraceService by lazy {
        Retrofit.Builder()
            .baseUrl(traceUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(YSPBtraceService::class.java)
    }

    val fAuthService: FAuthService by lazy {
        Retrofit.Builder()
            .baseUrl(fUrl)
            .client(okHttpClient)
            .addConverterFactory(GsonConverterFactory.create())
            .build().create(FAuthService::class.java)
    }

    private fun getUnsafeOkHttpClient(): OkHttpClient {
        try {
            val trustAllCerts: Array<TrustManager> = arrayOf(
                object : X509TrustManager {
                    override fun checkClientTrusted(
                        chain: Array<out java.security.cert.X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun checkServerTrusted(
                        chain: Array<out java.security.cert.X509Certificate>?,
                        authType: String?
                    ) {
                    }

                    override fun getAcceptedIssuers(): Array<java.security.cert.X509Certificate> {
                        return emptyArray()
                    }
                }
            )

            val sslContext = SSLContext.getInstance("SSL")
            sslContext.init(null, trustAllCerts, java.security.SecureRandom())

            return OkHttpClient.Builder()
                .sslSocketFactory(sslContext.socketFactory, trustAllCerts[0] as X509TrustManager)
                .hostnameVerifier { _, _ -> true }
                .dns(DnsCache())
                .build()

        } catch (e: Exception) {
            throw RuntimeException(e)
        }
    }
}