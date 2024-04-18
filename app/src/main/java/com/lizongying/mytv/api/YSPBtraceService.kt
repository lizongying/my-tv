package com.lizongying.mytv.api

import retrofit2.Call
import retrofit2.http.Body
import retrofit2.http.Field
import retrofit2.http.FormUrlEncoded
import retrofit2.http.Headers
import retrofit2.http.POST
import retrofit2.http.Query


interface YSPBtraceService {
    @FormUrlEncoded
    @POST("kvcollect")
    @Headers(
        "content-type: application/x-www-form-urlencoded",
        "referer: https://www.yangshipin.cn/",
    )
    fun kvcollect(
        @Query("BossId") BossId: String = "2727",
        @Query("c_timestamp") c_timestamp: String = "",
        @Field("Pwd") Pwd: String = "1424084450",
        @Field("fpid") fpid: String = "",
        @Field("livepid") livepid: String = "",
        @Field("prd") prd: String = "60000",
        @Field("ftime") ftime: String = "",
        @Field("prog") prog: String = "",
        @Field("playno") playno: String = "",
        @Field("guid") guid: String = "",
        @Field("hh_ua") hh_ua: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        @Field("cdn") cdn: String = "waibao",
        @Field("platform") platform: String = "5910204",
        @Field("errcode") errcode: String = "-",
        @Field("sUrl") sUrl: String = "",
        @Field("seq") seq: String = "",
        @Field("login_type") login_type: String = "undefined",
        @Field("open_id") open_id: String = "undefined",
        @Field("openid") openid: String = "undefined",
        @Field("defn") defn: String = "fhd",
        @Field("durl") durl: String = "-",
        @Field("sdtfrom") sdtfrom: String = "ysp_pc_01",
        @Field("firstreport") firstreport: String = "0",
        @Field("fplayerver") fplayerver: String = "89",
        @Field("cmd") cmd: String = "263",
        @Field("fact1") fact1: String = "ysp_pc_live_b",
        @Field("sRef") sRef: String = "-",
        @Field("viewid") viewid: String = "",
        @Field("geturltime") geturltime: String = "0",
        @Field("hc_openid") hc_openid: String = "undefined",
        @Field("downspeed") downspeed: String = "10",
        @Field("c_host") c_host: String = "www.yangshipin.cn",
        @Field("c_pathname") c_pathname: String = "www.yangshipin.cn/",
        @Field("c_url") c_url: String = "www.yangshipin.cn/",
        @Field("c_channel") c_channel: String = "-",
        @Field("c_referrer") c_referrer: String = "-",
        @Field("c_ssize") c_ssize: String = "618",
        @Field("c_ua") c_ua: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/119.0.0.0 Safari/537.36",
        @Field("c_os") c_os: String = "mac os",
        @Field("c_osv") c_osv: String = "os10.15.7",
        @Field("c_browser") c_browser: String = "chrome",
        @Field("c_browserv") c_browserv: String = "chrome119",
        @Field("c_dvendor") c_dvendor: String = "apple",
        @Field("c_dmodel") c_dmodel: String = "macintosh",
        @Field("c_dtype") c_dtype: String = "unkown",
        @Field("c_city") c_city: String = "disabled",
        @Field("c_nation") c_nation: String = "disabled",
        @Field("c_province") c_province: String = "disabled",
        @Field("c_guid") c_guid: String = "",
        @Field("c_vuid") c_vuid: String = "-",
    ): Call<Void>

    @POST("kvcollect")
    @Headers(
        "content-type: application/json",
        "referer: https://www.yangshipin.cn/",
        "Cookie: guid=1; versionName=99.99.99; versionCode=999999; vplatform=109; platformVersion=Chrome; deviceModel=123",
        "User-Agent: Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
    )
    fun kvcollect2(
        @Body request: KvcollectRequest,
    ): Call<Void>

    @FormUrlEncoded
    @POST("kvcollect")
    @Headers(
        "content-type: application/x-www-form-urlencoded",
        "referer: https://www.yangshipin.cn/",
    )
    fun kvcollect3(
        @Field("BossId") BossId: String = "9141",
        @Field("Pwd") Pwd: String = "1137850982",
        @Field("prog") prog: String = "", //
        @Field("playno") playno: String = "", //
        @Field("guid") guid: String = "", //
        @Field("hh_ua") hh_ua: String = "Mozilla/5.0 (Macintosh; Intel Mac OS X 10_15_7) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/123.0.0.0 Safari/537.36",
        @Field("cdn") cdn: String = "waibao",
        @Field("sdtfrom") sdtfrom: String = "ysp_pc_01",
        @Field("prd") prd: String = "60000",
        @Field("platform") platform: String = "5910204",
        @Field("errcode") errcode: String = "-",
        @Field("durl") durl: String = "", //
        @Field("firstreport") firstreport: String = "-",
        @Field("sUrl") sUrl: String = "", //
        @Field("sRef") sRef: String = "-",
        @Field("fplayerver") fplayerver: String = "100",
        @Field("livepid") livepid: String = "",
        @Field("viewid") viewid: String = "",
        @Field("seq") seq: String = "",
        @Field("cmd") cmd: String = "263",
        @Field("login_type") login_type: String = "-",
        @Field("geturltime") geturltime: String = "-",
        @Field("downspeed") downspeed: String = "10",
        @Field("hc_openid") hc_openid: String = "-",
        @Field("open_id") open_id: String = "-",
        @Field("defn") defn: String = "fhd",
        @Field("fact1") fact1: String = "ysp_pc_live_b",
        @Field("openid") openid: String = "-",
        @Field("_dc") _dc: String = "",
        @Field("live_type") live_type: String = "-",
        @Field("ftime") ftime: String = "",
        @Field("url") url: String = "",
        @Field("rand_str") rand_str: String = "", //
        @Field("signature") signature: String = "",
    ): Call<Void>
}