package com.lizongying.mytv.models

import android.net.Uri
import android.util.Log
import androidx.annotation.OptIn
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.media3.common.MediaItem
import androidx.media3.common.util.UnstableApi
import androidx.media3.datasource.DefaultHttpDataSource
import androidx.media3.exoplayer.hls.HlsMediaSource
import com.lizongying.mytv.TV
import com.lizongying.mytv.proto.Ysp.cn.yangshipin.omstv.common.proto.programModel.Program
import java.util.Date

class TVViewModel(private var tv: TV) : ViewModel() {
    private var mapping = mapOf(
        "CCTV4K" to "CCTV4K 超高清",
        "CCTV1" to "CCTV1 综合",
        "CCTV2" to "CCTV2 财经",
        "CCTV4" to "CCTV4 中文国际",
        "CCTV5" to "CCTV5 体育",
        "CCTV5+" to "CCTV5+ 体育赛事",
        "CCTV7" to "CCTV7 国防军事",
        "CCTV9" to "CCTV9 记录",
        "CCTV10" to "CCTV10 科教",
        "CCTV11" to "CCTV11 戏曲",
        "CCTV12" to "CCTV12 社会与法",
        "CCTV13" to "CCTV13 新闻",
        "CCTV14" to "CCTV14 少儿",
        "CCTV15" to "CCTV15 音乐",
        "CCTV16-HD" to "CCTV16 奥林匹克",
        "CCTV17" to "CCTV17 农业农村",
        "CGTN" to "CGTN",
        "CGTN法语频道" to "CGTN 法语频道",
        "CGTN俄语频道" to "CGTN 俄语频道",
        "CGTN阿拉伯语频道" to "CGTN 阿拉伯语频道",
        "CGTN西班牙语频道" to "CGTN 西班牙语频道",
        "CGTN外语纪录频道" to "CGTN 纪录频道",

        "东方卫视" to "东方卫视",
        "湖南卫视" to "湖南卫视",
        "湖北卫视" to "湖北卫视",
        "辽宁卫视" to "辽宁卫视",
        "江苏卫视" to "江苏卫视",
        "江西卫视" to "江西卫视",
        "山东卫视" to "山东卫视",
        "广东卫视" to "广东卫视",
        "广西卫视" to "广西卫视",
        "重庆卫视" to "重庆卫视",
        "河南卫视" to "河南卫视",
        "河北卫视" to "河北卫视",
        "贵州卫视" to "贵州卫视",
        "北京卫视" to "北京卫视",
        "黑龙江卫视" to "黑龙江卫视",
        "浙江卫视" to "浙江卫视",
        "安徽卫视" to "安徽卫视",
        "深圳卫视" to "深圳卫视",
        "四川卫视" to "四川卫视",
        "福建东南卫视" to "东南卫视",
        "海南卫视" to "海南卫视",
    ).entries.associate { (key, value) -> value to key }

    private var mappingLogo = mapOf(
        "CCTV4K" to "https://resources.yangshipin.cn/assets/oms/image/202306/3e9d06fd7244d950df5838750f1c6ac3456e172b51caca2c16d2282125b111e8.png?imageMogr2/format/webp",
        "CCTV4K 超高清" to "https://resources.yangshipin.cn/assets/oms/image/202306/3e9d06fd7244d950df5838750f1c6ac3456e172b51caca2c16d2282125b111e8.png?imageMogr2/format/webp",
        "CCTV1" to "https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp",
        "CCTV1 综合" to "https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp",
        "CCTV2" to "https://resources.yangshipin.cn/assets/oms/image/202306/20115388de0207131af17eac86c33049b95d69eaff064e55653a1b941810a006.png?imageMogr2/format/webp",
        "CCTV2 财经" to "https://resources.yangshipin.cn/assets/oms/image/202306/20115388de0207131af17eac86c33049b95d69eaff064e55653a1b941810a006.png?imageMogr2/format/webp",
        "CCTV3" to "https://resources.yangshipin.cn/assets/oms/image/202306/7b7a65c712450da3deb6ca66fbacf4f9aee00d3f20bd80eafb5ada01ec63eb3a.png?imageMogr2/format/webp",
        "CCTV3 综艺" to "https://resources.yangshipin.cn/assets/oms/image/202306/7b7a65c712450da3deb6ca66fbacf4f9aee00d3f20bd80eafb5ada01ec63eb3a.png?imageMogr2/format/webp",
        "CCTV4" to "https://resources.yangshipin.cn/assets/oms/image/202306/f357e58fdbcc076a3d65e1f958c942b2e14f14342c60736ceed98b092d35356a.png?imageMogr2/format/webp",
        "CCTV4 中文国际" to "https://resources.yangshipin.cn/assets/oms/image/202306/f357e58fdbcc076a3d65e1f958c942b2e14f14342c60736ceed98b092d35356a.png?imageMogr2/format/webp",
        "CCTV5" to "https://resources.yangshipin.cn/assets/oms/image/202306/0a6a7138952675983a3d854df7688557b286d59aa06166edae51506f9204d655.png?imageMogr2/format/webp",
        "CCTV5 体育" to "https://resources.yangshipin.cn/assets/oms/image/202306/0a6a7138952675983a3d854df7688557b286d59aa06166edae51506f9204d655.png?imageMogr2/format/webp",
        "CCTV5+" to "https://resources.yangshipin.cn/assets/oms/image/202306/649ad76a90bfef55b05db9fe52e006487280f619089099d5dc971e387fc6eff0.png?imageMogr2/format/webp",
        "CCTV5+ 体育赛事" to "https://resources.yangshipin.cn/assets/oms/image/202306/649ad76a90bfef55b05db9fe52e006487280f619089099d5dc971e387fc6eff0.png?imageMogr2/format/webp",
        "CCTV6" to "https://resources.yangshipin.cn/assets/oms/image/202306/741515efda91f03f455df8a7da4ee11fa9329139c276435cf0a9e2af398d5bf2.png?imageMogr2/format/webp",
        "CCTV6 电影" to "https://resources.yangshipin.cn/assets/oms/image/202306/741515efda91f03f455df8a7da4ee11fa9329139c276435cf0a9e2af398d5bf2.png?imageMogr2/format/webp",
        "CCTV7" to "https://resources.yangshipin.cn/assets/oms/image/202306/b29af94e295ebdf646cefb68122c429b9cd921f498ca20d2d8070252536f9ff9.png?imageMogr2/format/webp",
        "CCTV7 国防军事" to "https://resources.yangshipin.cn/assets/oms/image/202306/b29af94e295ebdf646cefb68122c429b9cd921f498ca20d2d8070252536f9ff9.png?imageMogr2/format/webp",
        "CCTV8" to "https://resources.yangshipin.cn/assets/oms/image/202306/ad51de94426a0ba039e6dd6a8534ea98ecc813a6176bde87b4f18cc34d6d7590.png?imageMogr2/format/webp",
        "CCTV8 电视剧" to "https://resources.yangshipin.cn/assets/oms/image/202306/ad51de94426a0ba039e6dd6a8534ea98ecc813a6176bde87b4f18cc34d6d7590.png?imageMogr2/format/webp",
        "CCTV9" to "https://resources.yangshipin.cn/assets/oms/image/202306/2ed1b4deeca179d5db806bb941790f82eb92a1b7299c1c38fe027f95a5caee5e.png?imageMogr2/format/webp",
        "CCTV9 记录" to "https://resources.yangshipin.cn/assets/oms/image/202306/2ed1b4deeca179d5db806bb941790f82eb92a1b7299c1c38fe027f95a5caee5e.png?imageMogr2/format/webp",
        "CCTV10" to "https://resources.yangshipin.cn/assets/oms/image/202306/aa6157ec65188cd41826e5a2f088c3d6d153205f5f6428258d12c59999e221aa.png?imageMogr2/format/webp",
        "CCTV10 科教" to "https://resources.yangshipin.cn/assets/oms/image/202306/aa6157ec65188cd41826e5a2f088c3d6d153205f5f6428258d12c59999e221aa.png?imageMogr2/format/webp",
        "CCTV11" to "https://resources.yangshipin.cn/assets/oms/image/202306/ed12ed7c7a1034dae4350011fe039284c5d5a836506b28c9e32e3c75299625c0.png?imageMogr2/format/webp",
        "CCTV11 戏曲" to "https://resources.yangshipin.cn/assets/oms/image/202306/ed12ed7c7a1034dae4350011fe039284c5d5a836506b28c9e32e3c75299625c0.png?imageMogr2/format/webp",
        "CCTV12" to "https://resources.yangshipin.cn/assets/oms/image/202306/484083cffaa40df7e659565e8cb4d1cc740158a185512114167aa21fa0c59240.png?imageMogr2/format/webp",
        "CCTV12 社会与法" to "https://resources.yangshipin.cn/assets/oms/image/202306/484083cffaa40df7e659565e8cb4d1cc740158a185512114167aa21fa0c59240.png?imageMogr2/format/webp",
        "CCTV13" to "https://resources.yangshipin.cn/assets/oms/image/202306/266da7b43c03e2312186b4a999e0f060e8f15b10d2cc2c9aa32171819254cf1a.png?imageMogr2/format/webp",
        "CCTV13 新闻" to "https://resources.yangshipin.cn/assets/oms/image/202306/266da7b43c03e2312186b4a999e0f060e8f15b10d2cc2c9aa32171819254cf1a.png?imageMogr2/format/webp",
        "CCTV14" to "https://resources.yangshipin.cn/assets/oms/image/202306/af6b603896938dc346fbb16abfc63c12cba54b0ec9d18770a15d347d115f12d5.png?imageMogr2/format/webp",
        "CCTV14 少儿" to "https://resources.yangshipin.cn/assets/oms/image/202306/af6b603896938dc346fbb16abfc63c12cba54b0ec9d18770a15d347d115f12d5.png?imageMogr2/format/webp",
        "CCTV15" to "https://resources.yangshipin.cn/assets/oms/image/202306/2ceee92188ef684efe0d8b90839c4f3ad450d179dc64d59beff417059453af47.png?imageMogr2/format/webp",
        "CCTV15 音乐" to "https://resources.yangshipin.cn/assets/oms/image/202306/2ceee92188ef684efe0d8b90839c4f3ad450d179dc64d59beff417059453af47.png?imageMogr2/format/webp",
        "CCTV16" to "https://resources.yangshipin.cn/assets/oms/image/202306/53793fa7bacd3a93ff6dc5d2758418985e1f952a316c335d663b572d8bdcd74d.png?imageMogr2/format/webp",
        "CCTV16 奥林匹克" to "https://resources.yangshipin.cn/assets/oms/image/202306/53793fa7bacd3a93ff6dc5d2758418985e1f952a316c335d663b572d8bdcd74d.png?imageMogr2/format/webp",
        "CCTV17" to "https://resources.yangshipin.cn/assets/oms/image/202306/ddef563072f8bad2bea5b9e52674cb7b4ed50efb20c26e61994dfbdf05c1e3c0.png?imageMogr2/format/webp",
        "CCTV17 农业农村" to "https://resources.yangshipin.cn/assets/oms/image/202306/ddef563072f8bad2bea5b9e52674cb7b4ed50efb20c26e61994dfbdf05c1e3c0.png?imageMogr2/format/webp",
        "CGTN" to "https://resources.yangshipin.cn/assets/oms/image/202306/a72dff758ca1c17cd0ecc8cedc11b893d208f409d5e6302faa0e9d298848abc3.png?imageMogr2/format/webp",
        "CGTN 法语频道" to "https://resources.yangshipin.cn/assets/oms/image/202306/a8d0046a47433d952bf6ed17062deb8bd2184ba9aec0f7781df6bf9487a3ffcf.png?imageMogr2/format/webp",
        "CGTN 俄语频道" to "https://resources.yangshipin.cn/assets/oms/image/202306/bf0a820893cbaf20dd0333e27042e1ef9c8806e5b602b6a8c95af399db0bc77a.png?imageMogr2/format/webp",
        "CGTN 阿拉伯语频道" to "https://resources.yangshipin.cn/assets/oms/image/202306/2e44e2aa3e7a1cedf07fd0ae59fe69e86a60a2632660a006e3e9e7397b2d107e.png?imageMogr2/format/webp",
        "CGTN 西班牙语频道" to "https://resources.yangshipin.cn/assets/oms/image/202309/7c337e3dbe64402ec7e4678a619a4a6d95144e42f35161181ff78e143b7cf67a.png?imageMogr2/format/webp",
        "CGTN 纪录频道" to "https://resources.yangshipin.cn/assets/oms/image/202309/74d3ac436a7e374879578de1d87a941fbf566d39d5632b027c5097891ed32bd5.png?imageMogr2/format/webp",

        "风云剧场" to "https://resources.yangshipin.cn/assets/oms/image/202306/4d549e53e6d0f632d5a633d1945280797b153e588f919221a07faa869812cc89.png?imageMogr2/format/webp",
        "第一剧场" to "https://resources.yangshipin.cn/assets/oms/image/202306/a556bd7d93ce65e18f243a8892b5604f4faa994a4897315914216a710a706208.png?imageMogr2/format/webp",
        "怀旧剧场" to "https://resources.yangshipin.cn/assets/oms/image/202306/5661bd04fecdb6e899f801147a22ab5d3a475bf2b62e30aec2c0023190ebc9b1.png?imageMogr2/format/webp",
        "风云音乐" to "https://resources.yangshipin.cn/assets/oms/image/202306/bbf1d024c5228b8dd128b0e3cb1717d173fab4ee84c3a4c8a57b1a215362ca3b.png?imageMogr2/format/webp",
        "兵器科技" to "https://resources.yangshipin.cn/assets/oms/image/202306/4c6b6a6d3839889f34d33db3c2f80233b26b74d3489b393487635f8704e70796.png?imageMogr2/format/webp",
        "风云足球" to "https://resources.yangshipin.cn/assets/oms/image/202306/cd1e2bb52b06a991de168733e5ff0f1d85adc8042d40c8f393f723543e5dd08a.png?imageMogr2/format/webp",
        "高尔夫网球" to "https://resources.yangshipin.cn/assets/oms/image/202306/cdd1b31ede7a5ad049ed53d9a072422f829e72dd062ed2c19e077fdd01699071.png?imageMogr2/format/webp",
        "女性时尚" to "https://resources.yangshipin.cn/assets/oms/image/202306/fa28955ce8b2539d728bf4c6a13a46ff57ad76eae46627f7bcfb1ed8a613d3fc.png?imageMogr2/format/webp",
        "央视文化精品" to "https://resources.yangshipin.cn/assets/oms/image/202306/14ac5ce40482cacd3d4b37435222bfe86af2b452a2f04ecbfc1d13d76edd7c57.png?imageMogr2/format/webp",
        "央视台球" to "https://resources.yangshipin.cn/assets/oms/image/202306/10e14a92478011aa6c3c8562e62127f3b1908e29fcd78e4b2b24b9e6d3ec2fbc.png?imageMogr2/format/webp",
        "电视指南" to "https://resources.yangshipin.cn/assets/oms/image/202306/244d72c0eb1615ed7d51c2f5db5a67f306aa3f58c05bc2d34de3aa7e956dc8c9.png?imageMogr2/format/webp",
        "卫生健康" to "https://resources.yangshipin.cn/assets/oms/image/202306/54a6863656fdfd8f803be193ddf22441c5000a108833889816fd2d8911715ce8.png?imageMogr2/format/webp",

        "东方卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/9bd372ca292a82ce3aa08772b07efc4af1f85c21d1f268ea33440c49e9a0a488.png?imageMogr2/format/webp",
        "湖南卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/4120e89d3079d08aa17d382f69a2308ec70839b278367763c34a34666c75cb88.png?imageMogr2/format/webp",
        "湖北卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/7a6be5a2bb1dc53a945c016ff1f525dc4a84c51db371c15c89aa55404b0ba784.png?imageMogr2/format/webp",
        "辽宁卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/ac4ed6058a87c101ae7147ebc38905d0cae047fb73fd277ee5049b84f52bda36.png?imageMogr2/format/webp",
        "江苏卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/380ad685c0c1d5b2c902246b8d2df6d3f9b45e2837abcfe493075bbded597a31.png?imageMogr2/format/webp",
        "江西卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/3c760d0d00463855890e8a1864ea4a6b6dd66b90c29b4ac714a4b17c16519871.png?imageMogr2/format/webp",
        "山东卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/22d403f07a7cf5410b3ad3ddb65a11aa229a32475fac213f5344c9f0ec330ca1.png?imageMogr2/format/webp",
        "广东卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/28886880a4dc0f06fb7e0a528a1def0591d61a65870e29176ede0cc92033bbfd.png?imageMogr2/format/webp",
        "广西卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/54b7e97cb816bb223fe05f3fc44da2c7820eb66e8550c19d23100f2c414ecc38.png?imageMogr2/format/webp",
        "重庆卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/657651f411de2673d1770d9a78b44c1265704f7468cc41d4be7f51d630768494.png?imageMogr2/format/webp",
        "河南卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/74925962148a6d31c85808b6cd4e444c2a54bab393d2c5fc85e960b50e22fa86.png?imageMogr2/format/webp",
        "河北卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/d545becdc81c60197b08c7f47380705e4665ed3fe55efc8b855e486f6e655378.png?imageMogr2/format/webp",
        "贵州卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/4eb45f4781d33d872af027dc01c941559aab55667dd99cc5c22bef7037807b13.png?imageMogr2/format/webp",
        "北京卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/f4f23633c578beea49a3841d88d3490100f029ee349059fa532869db889872c5.png?imageMogr2/format/webp",
        "黑龙江卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/d8273ae9be698ce2db21f5b886ecac95a73429593f93713c60ed8c12c38bf0d3.png?imageMogr2/format/webp",
        "浙江卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/a66c836bd98ba3e41a2e9a570d4b9c50dedc6839e9de333e2e78212ad505f37e.png?imageMogr2/format/webp",
        "安徽卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/f35fa04b51b1ee4984b03578b65403570868ebca03c6c01e11b097f999a58d9b.png?imageMogr2/format/webp",
        "深圳卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/d59fec04c902e3581c617136d02d4b9b8c4cbe64272781ddd3525e80c823edb7.png?imageMogr2/format/webp",
        "四川卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/3276a414ae0eaa0f116f2045cd913367967d0c7c1e978e8621ac3879436c6ed7.png?imageMogr2/format/webp",
        "东南卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/3208fe6564a293c21b711333fb3edb05bb5b406cff840573c9a8d839680a1579.png?imageMogr2/format/webp",
        "海南卫视" to "https://resources.yangshipin.cn/assets/oms/image/202306/6e060391fde0469801fc3d84dbf204b4f8d650d251f17d7595a6964c0bb99e81.png?imageMogr2/format/webp",
    )

    private var mappingEPG = mapOf(
        "CCTV4K" to "600002264",
        "CCTV4K 超高清" to "600002264",
        "CCTV1" to "600001859",
        "CCTV1 综合" to "600001859",
        "CCTV2" to "600001800",
        "CCTV2 财经" to "600001800",
        "CCTV3" to "600001801",
        "CCTV3 综艺" to "600001801",
        "CCTV4" to "600001814",
        "CCTV4 中文国际" to "600001814",
        "CCTV5" to "600001818",
        "CCTV5 体育" to "600001818",
        "CCTV5+" to "600001817",
        "CCTV5+ 体育赛事" to "600001817",
        "CCTV6" to "600001802",
        "CCTV6 电影" to "600001802",
        "CCTV7" to "600004092",
        "CCTV7 国防军事" to "600004092",
        "CCTV8" to "600001803",
        "CCTV8 电视剧" to "600001803",
        "CCTV9" to "600004078",
        "CCTV9 记录" to "600004078",
        "CCTV10" to "600001805",
        "CCTV10 科教" to "600001805",
        "CCTV11" to "600001806",
        "CCTV11 戏曲" to "600001806",
        "CCTV12" to "600001807",
        "CCTV12 社会与法" to "600001807",
        "CCTV13" to "600001811",
        "CCTV13 新闻" to "600001811",
        "CCTV14" to "600001809",
        "CCTV14 少儿" to "600001809",
        "CCTV15" to "600001815",
        "CCTV15 音乐" to "600001815",
        "CCTV16" to "600098637",
        "CCTV16 奥林匹克" to "600098637",
        "CCTV17" to "600001810",
        "CCTV17 农业农村" to "600001810",
        "CGTN" to "600014550",
        "CGTN 法语频道" to "600084704",
        "CGTN 俄语频道" to "600084758",
        "CGTN 阿拉伯语频道" to "600084782",
        "CGTN 西班牙语频道" to "600084744",
        "CGTN 记录频道" to "600084781",

        "风云剧场" to "600099658",
        "第一剧场" to "600099655",
        "怀旧剧场" to "600099620",
        "风云音乐" to "600099660",
        "兵器科技" to "600099649",
        "风云足球" to "600099636",
        "高尔夫网球" to "600099659",
        "女性时尚" to "600099650",
        "央视文化精品" to "600099653",
        "央视台球" to "600099652",
        "电视指南" to "600099656",
        "卫生健康" to "600099651",

        "东方卫视" to "600002483",
        "湖南卫视" to "600002475",
        "湖北卫视" to "600002508",
        "辽宁卫视" to "600002505",
        "江苏卫视" to "600002521",
        "江西卫视" to "600002503",
        "山东卫视" to "600002513",
        "广东卫视" to "600002485",
        "广西卫视" to "600002509",
        "重庆卫视" to "600002531",
        "河南卫视" to "600002525",
        "河北卫视" to "600002493",
        "贵州卫视" to "600002490",
        "北京卫视" to "600002309",
        "黑龙江卫视" to "600002498",
        "浙江卫视" to "600002520",
        "安徽卫视" to "600002532",
        "深圳卫视" to "600002481",
        "四川卫视" to "600002516",
        "东南卫视" to "600002484",
        "海南卫视" to "600002506",
    )

    private var isFirstTime = true

    private var rowPosition: Int = 0
    private var itemPosition: Int = 0

    private val _programId = MutableLiveData<String>()
    val programId: LiveData<String>
        get() = _programId

    private var _program = MutableLiveData<MutableList<Program>>()
    val program: LiveData<MutableList<Program>>
        get() = _program

    private val _id = MutableLiveData<Int>()
    val id: LiveData<Int>
        get() = _id

    private val _title = MutableLiveData<String>()
    val title: LiveData<String>
        get() = _title

    private val _videoUrl = MutableLiveData<List<String>>()
    val videoUrl: LiveData<List<String>>
        get() = _videoUrl

    private val _videoIndex = MutableLiveData<Int>()
    val videoIndex: LiveData<Int>
        get() = _videoIndex

    private val _logo = MutableLiveData<String>()
    val logo: LiveData<String>
        get() = _logo

    private val _pid = MutableLiveData<String>()
    val pid: LiveData<String>
        get() = _pid

    private val _sid = MutableLiveData<String>()
    val sid: LiveData<String>
        get() = _sid

    private val _change = MutableLiveData<Boolean>()
    val change: LiveData<Boolean>
        get() = _change

    private val _ready = MutableLiveData<Boolean>()
    val ready: LiveData<Boolean>
        get() = _ready

    private var mMinimumLoadableRetryCount = 3

    var seq = 0

    fun addVideoUrl(url: String) {
        if (_videoUrl.value?.isNotEmpty() == true) {
            if (_videoUrl.value!!.last().contains("cctv.cn")) {
                tv.videoUrl = tv.videoUrl.subList(0, tv.videoUrl.lastIndex) + listOf(url)
            } else {
                tv.videoUrl = tv.videoUrl + listOf(url)
            }
        } else {
            tv.videoUrl = tv.videoUrl + listOf(url)
        }
        tv.videoIndex = tv.videoUrl.lastIndex
        _videoUrl.value = tv.videoUrl
        _videoIndex.value = tv.videoIndex
    }

    fun firstSource() {
        if (_videoUrl.value!!.isNotEmpty()) {
            setVideoIndex(0)
            allReady()
        } else {
            Log.e(TAG, "no first")
        }
    }

    fun changed() {
        _change.value = true
    }

    fun allReady() {
        _ready.value = true
    }

    fun setVideoIndex(videoIndex: Int) {
        _videoIndex.value = videoIndex
    }

    fun setLogo(url: String) {
        _logo.value = url
    }

    init {
        _id.value = tv.id
        _title.value = tv.title
        _videoUrl.value = tv.videoUrl
        _videoIndex.value = tv.videoIndex
        Log.d(TAG, "${tv.title} ${mappingLogo[tv.title]}")
        if (mappingLogo[tv.title] != null) {
            _logo.value = mappingLogo[tv.title]
        }
        _pid.value = tv.pid
        _sid.value = tv.sid
        _program.value = mutableListOf()

        if (mappingEPG[tv.title] != null) {
            _programId.value = mappingEPG[tv.title]
        }
    }

    fun getIsFirstTime(): Boolean {
        return isFirstTime
    }

    fun isFirstTime(firstTime: Boolean) {
        isFirstTime = firstTime
    }

    fun getRowPosition(): Int {
        return rowPosition
    }

    fun getItemPosition(): Int {
        return itemPosition
    }

    fun setRowPosition(position: Int) {
        rowPosition = position
    }

    fun setItemPosition(position: Int) {
        itemPosition = position
    }

    fun update(t: TV) {
        tv = t
    }

    fun getTV(): TV {
        return tv
    }

    fun ysp(): String? {
        if (tv.pid == "") {
            return null
        }
        return mapping[tv.title]
    }

    fun getProgram(): MutableList<Program>? {
        _program.value = (_program.value?.filter { it.et > (Date().time / 1000) })?.toMutableList()
        return _program.value?.subList(0, 2)
    }

    fun getProgramOne(): Program? {
        val programNew = (_program.value?.filter { it.et > (Date().time / 1000) })?.toMutableList()
        if (programNew != null && _program.value != programNew) {
            _program.value = programNew
        }
        if (_program.value!!.isEmpty()) {
            return null
        }
        return _program.value?.first()
    }

    fun addProgram(p: MutableList<Program>) {
        val p1 = (p.filter { it.et > (Date().time / 1000) }).toMutableList()
        if (p1.isEmpty() || _program.value == p1) {
            return
        }

        if (_program.value!!.isEmpty()) {
            _program.value = p1
        } else {
            _program.value =
                ((_program.value?.filter { it.et > (Date().time / 1000) && it.st < p1.first().st })?.plus(
                    p1
                ))?.toMutableList()
        }
    }


    private var mHeaders: Map<String, String>? = mapOf()

    fun setHeaders(headers: Map<String, String>) {
        mHeaders = headers
    }


    fun setMinimumLoadableRetryCount(minimumLoadableRetryCount: Int) {
        mMinimumLoadableRetryCount = minimumLoadableRetryCount
    }

    @OptIn(UnstableApi::class)
    fun buildSource(videoUrl: String, mHeaders: Map<String, String>?): HlsMediaSource {
        val httpDataSource = DefaultHttpDataSource.Factory()
        mHeaders?.let { httpDataSource.setDefaultRequestProperties(it) }

        return HlsMediaSource.Factory(httpDataSource).createMediaSource(
            MediaItem.fromUri(
                Uri.parse(videoUrl)
            )
        )
    }

    companion object {
        private const val TAG = "TVViewModel"
    }
}