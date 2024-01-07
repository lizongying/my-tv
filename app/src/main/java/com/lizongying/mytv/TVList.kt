package com.lizongying.mytv

object TVList {
    val list: Map<String, List<TV>> by lazy {
        setupTV()
    }

    private var mappingLogo = mapOf(
        "CCTV4K 超高清" to "https://resources.yangshipin.cn/assets/oms/image/202306/3e9d06fd7244d950df5838750f1c6ac3456e172b51caca2c16d2282125b111e8.png?imageMogr2/format/webp",
        "CCTV8K 超高清" to "https://static.wikia.nocookie.net/logos/images/6/69/CCTV8K.png/revision/latest/scale-to-width-down/120?cb=20230104110835&path-prefix=vi",
        "CCTV1 综合" to "https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp",
        "CCTV2 财经" to "https://resources.yangshipin.cn/assets/oms/image/202306/20115388de0207131af17eac86c33049b95d69eaff064e55653a1b941810a006.png?imageMogr2/format/webp",
        "CCTV3 综艺" to "https://resources.yangshipin.cn/assets/oms/image/202306/7b7a65c712450da3deb6ca66fbacf4f9aee00d3f20bd80eafb5ada01ec63eb3a.png?imageMogr2/format/webp",
        "CCTV4 中文国际" to "https://resources.yangshipin.cn/assets/oms/image/202306/f357e58fdbcc076a3d65e1f958c942b2e14f14342c60736ceed98b092d35356a.png?imageMogr2/format/webp",
        "CCTV5 体育" to "https://resources.yangshipin.cn/assets/oms/image/202306/0a6a7138952675983a3d854df7688557b286d59aa06166edae51506f9204d655.png?imageMogr2/format/webp",
        "CCTV5+ 体育赛事" to "https://resources.yangshipin.cn/assets/oms/image/202306/649ad76a90bfef55b05db9fe52e006487280f619089099d5dc971e387fc6eff0.png?imageMogr2/format/webp",
        "CCTV6 电影" to "https://resources.yangshipin.cn/assets/oms/image/202306/741515efda91f03f455df8a7da4ee11fa9329139c276435cf0a9e2af398d5bf2.png?imageMogr2/format/webp",
        "CCTV7 国防军事" to "https://resources.yangshipin.cn/assets/oms/image/202306/b29af94e295ebdf646cefb68122c429b9cd921f498ca20d2d8070252536f9ff9.png?imageMogr2/format/webp",
        "CCTV8 电视剧" to "https://resources.yangshipin.cn/assets/oms/image/202306/ad51de94426a0ba039e6dd6a8534ea98ecc813a6176bde87b4f18cc34d6d7590.png?imageMogr2/format/webp",
        "CCTV9 记录" to "https://resources.yangshipin.cn/assets/oms/image/202306/2ed1b4deeca179d5db806bb941790f82eb92a1b7299c1c38fe027f95a5caee5e.png?imageMogr2/format/webp",
        "CCTV10 科教" to "https://resources.yangshipin.cn/assets/oms/image/202306/aa6157ec65188cd41826e5a2f088c3d6d153205f5f6428258d12c59999e221aa.png?imageMogr2/format/webp",
        "CCTV11 戏曲" to "https://resources.yangshipin.cn/assets/oms/image/202306/ed12ed7c7a1034dae4350011fe039284c5d5a836506b28c9e32e3c75299625c0.png?imageMogr2/format/webp",
        "CCTV12 社会与法" to "https://resources.yangshipin.cn/assets/oms/image/202306/484083cffaa40df7e659565e8cb4d1cc740158a185512114167aa21fa0c59240.png?imageMogr2/format/webp",
        "CCTV13 新闻" to "https://resources.yangshipin.cn/assets/oms/image/202306/266da7b43c03e2312186b4a999e0f060e8f15b10d2cc2c9aa32171819254cf1a.png?imageMogr2/format/webp",
        "CCTV14 少儿" to "https://resources.yangshipin.cn/assets/oms/image/202306/af6b603896938dc346fbb16abfc63c12cba54b0ec9d18770a15d347d115f12d5.png?imageMogr2/format/webp",
        "CCTV15 音乐" to "https://resources.yangshipin.cn/assets/oms/image/202306/2ceee92188ef684efe0d8b90839c4f3ad450d179dc64d59beff417059453af47.png?imageMogr2/format/webp",
        "CCTV16 奥林匹克" to "https://resources.yangshipin.cn/assets/oms/image/202306/53793fa7bacd3a93ff6dc5d2758418985e1f952a316c335d663b572d8bdcd74d.png?imageMogr2/format/webp",
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
        "世界地理" to "https://resources.yangshipin.cn/assets/oms/image/202306/bb3c6c9e145d698137f5bb64a582021a01b51344b929003630eb769ea65832a9.png?imageMogr2/format/webp",
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
        "CCTV4K 超高清" to "600002264",
        "CCTV8K 超高清" to "600156816",
        "CCTV1 综合" to "600001859",
        "CCTV2 财经" to "600001800",
        "CCTV3 综艺" to "600001801",
        "CCTV4 中文国际" to "600001814",
        "CCTV5 体育" to "600001818",
        "CCTV5+ 体育赛事" to "600001817",
        "CCTV6 电影" to "600001802",
        "CCTV7 国防军事" to "600004092",
        "CCTV8 电视剧" to "600001803",
        "CCTV9 记录" to "600004078",
        "CCTV10 科教" to "600001805",
        "CCTV11 戏曲" to "600001806",
        "CCTV12 社会与法" to "600001807",
        "CCTV13 新闻" to "600001811",
        "CCTV14 少儿" to "600001809",
        "CCTV15 音乐" to "600001815",
        "CCTV16 奥林匹克" to "600098637",
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
        "世界地理" to "600099637",
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
    private var mappingVideo = mapOf(
        "CCTV4K 超高清" to arrayOf("600002264", "2000266303"),
        "CCTV8K 超高清" to arrayOf("600156816", "2020603421"),
        "CCTV1 综合" to arrayOf("600001859", "2000210103"),
        "CCTV2 财经" to arrayOf("600001800", "2000203603"),
        "CCTV3 综艺" to arrayOf("600001801", "2000203803"),
        "CCTV4 中文国际" to arrayOf("600001814", "2000204803"),
        "CCTV5 体育" to arrayOf("600001818", "2000205103"),
        "CCTV5+ 体育赛事" to arrayOf("600001817", "2000204503"),
        "CCTV6 电影" to arrayOf("600001802", "2000203303"),
        "CCTV7 国防军事" to arrayOf("600004092", "2000510003"),
        "CCTV8 电视剧" to arrayOf("600001803", "2000203903"),
        "CCTV9 记录" to arrayOf("600004078", "2000499403"),
        "CCTV10 科教" to arrayOf("600001805", "2000203503"),
        "CCTV11 戏曲" to arrayOf("600001806", "2000204103"),
        "CCTV12 社会与法" to arrayOf("600001807", "2000202603"),
//        "CCTV13 新闻" to arrayOf("600001811","2000204603"),
        "CCTV14 少儿" to arrayOf("600001809", "2000204403"),
        "CCTV15 音乐" to arrayOf("600001815", "2000205003"),
        "CCTV16 奥林匹克" to arrayOf("600098637", "2012375003"),
        "CCTV17 农业农村" to arrayOf("600001810", "2000204203"),
        "CGTN" to arrayOf("600014550", "2001656803"),
        "CGTN 法语频道" to arrayOf("600084704", "2010153503"),
        "CGTN 俄语频道" to arrayOf("600084758", "2010152603"),
        "CGTN 阿拉伯语频道" to arrayOf("600084782", "2010155203"),
        "CGTN 西班牙语频道" to arrayOf("600084744", "2010152503"),
        "CGTN 记录频道" to arrayOf("600084781", "2010155403"),
        "风云剧场" to arrayOf("600099658", "2012513603"),
        "第一剧场" to arrayOf("600099655", "2012514403"),
        "怀旧剧场" to arrayOf("600099620", "2012511203"),
        "世界地理" to arrayOf("600099637", "2012513303"),
        "风云音乐" to arrayOf("600099660", "2012514103"),
        "兵器科技" to arrayOf("600099649", "2012513403"),
        "风云足球" to arrayOf("600099636", "2012514203"),
        "高尔夫网球" to arrayOf("600099659", "2012512503"),
        "女性时尚" to arrayOf("600099650", "2012513903"),
        "央视文化精品" to arrayOf("600099653", "2012513803"),
        "央视台球" to arrayOf("600099652", "2012513703"),
        "电视指南" to arrayOf("600099656", "2012514003"),
        "卫生健康" to arrayOf("600099651", "2012513503"),
        "东方卫视" to arrayOf("600002483", "2000292403"),
        "湖南卫视" to arrayOf("600002475", "2000296203"),
        "湖北卫视" to arrayOf("600002508", "2000294503"),
        "辽宁卫视" to arrayOf("600002505", "2000281303"),
        "江苏卫视" to arrayOf("600002521", "2000295603"),
        "江西卫视" to arrayOf("600002503", "2000294103"),
        "山东卫视" to arrayOf("600002513", "2000294803"),
        "广东卫视" to arrayOf("600002485", "2000292703"),
        "广西卫视" to arrayOf("600002509", "2000294203"),
        "重庆卫视" to arrayOf("600002531", "2000297803"),
        "河南卫视" to arrayOf("600002525", "2000296103"),
        "河北卫视" to arrayOf("600002493", "2000293403"),
        "贵州卫视" to arrayOf("600002490", "2000293303"),
        "北京卫视" to arrayOf("600002309", "2000272103"),
        "黑龙江卫视" to arrayOf("600002498", "2000293903"),
        "浙江卫视" to arrayOf("600002520", "2000295503"),
        "安徽卫视" to arrayOf("600002532", "2000298003"),
        "深圳卫视" to arrayOf("600002481", "2000292203"),
        "四川卫视" to arrayOf("600002516", "2000295003"),
        "东南卫视" to arrayOf("600002484", "2000292503"),
        "海南卫视" to arrayOf("600002506", "2000291503"),
    )
    private var count: Int = 0

    private fun setupTV(): Map<String, List<TV>> {
        val tvs = """
央视频道
CCTV4K 超高清,
CCTV8K 超高清,
CCTV1 综合,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226231/index.m3u8
CCTV2 财经,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226195/index.m3u8
CCTV3 综艺,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226397/index.m3u8
CCTV4 中文国际,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226191/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226191/index.m3u8
CCTV5 体育,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226395/index.m3u8
CCTV5+ 体育赛事,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226221/index.m3u8
CCTV6 电影,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226393/index.m3u8
CCTV7 国防军事,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226192/index.m3u8
CCTV8 电视剧,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226391/index.m3u8
CCTV9 记录,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226197/index.m3u8
CCTV10 科教,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226189/index.m3u8
CCTV11 戏曲,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226240/index.m3u8
CCTV12 社会与法,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226190/index.m3u8
CCTV13 新闻,https://live-play.cctvnews.cctv.com/cctv/merge_cctv13.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226233/index.m3u8
CCTV14 少儿,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226193/index.m3u8
CCTV15 音乐,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225785/index.m3u8
CCTV16 奥林匹克,http://39.134.24.162/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226921/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226921/index.m3u8
CCTV17 农业农村,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226198/index.m3u8
风云剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226950/index.m3u8
第一剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226959/index.m3u8
怀旧剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226972/index.m3u8
世界地理,
风云音乐,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226953/index.m3u8
兵器科技,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226975/index.m3u8
风云足球,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226984/index.m3u8
高尔夫网球,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226978/index.m3u8
女性时尚,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226969/index.m3u8
央视文化精品,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226981/index.m3u8
央视台球,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226956/index.m3u8
电视指南,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226987/index.m3u8
卫生健康,
地方频道
东方卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226217/index.m3u8
湖南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226307/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226211/index.m3u8
湖北卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226477/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226194/index.m3u8
辽宁卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226546/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226201/index.m3u8
江苏卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226200/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226200/index.m3u8
江西卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226344/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225764/index.m3u8
山东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226209/index.m3u8
广东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226216/index.m3u8
广西卫视,http://live.gxrb.com.cn/tv/gxtvlive03/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225770/index.m3u8
重庆卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226409/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226202/index.m3u8
河南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226480/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225767/index.m3u8
河北卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226406/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225750/index.m3u8
贵州卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226474/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225793/index.m3u8
北京卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225728/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226222/index.m3u8
黑龙江卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226327/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226215/index.m3u8
浙江卫视,http://hw-m-l.cztv.com/channels/lantian/channel01/1080p.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226199/index.m3u8
安徽卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226391/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226203/index.m3u8
深圳卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226205/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226205/index.m3u8
四川卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225768/index.m3u8;http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225768/index.m3u8
东南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226341/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225766/index.m3u8
海南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226465/index.m3u8;http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225769/index.m3u8
国际频道
CGTN,http://live.cgtn.com/1000/prog_index.m3u8
CGTN 法语频道,https://livefr.cgtn.com/1000f/prog_index.m3u8
CGTN 俄语频道,http://liveru.cgtn.com/1000r/prog_index.m3u8
CGTN 阿拉伯语频道,http://livear.cgtn.com/1000a/prog_index.m3u8
CGTN 西班牙语频道,http://livees.cgtn.com/500e/prog_index.m3u8;http://livees.cgtn.com/1000e/prog_index.m3u8
CGTN 纪录频道,https://livedoc.cgtn.com/500d/prog_index.m3u8
        """.trimIndent()

        val map: MutableMap<String, MutableList<TV>> = mutableMapOf()

        var channel = ""
        for (i in tvs.split("\n")) {
            if (i.trim() == "") {
                continue
            }
            if (!i.contains(",")) {
                channel = i.trim()
                if (channel == "移动专区") {
                    break
                }
                continue
            }
            val p = i.split(",")
            val titleMap = map[channel] ?: mutableListOf()

            val tv = TV(
                count,
                p[0],
                p[1].split(";").map { it.trim() }
            )

            val logo = mappingLogo[tv.title]
            if (logo != null && logo != "") {
                tv.logo = logo
            }
            val programId = mappingEPG[tv.title]
            if (programId != null && programId != "") {
                tv.programId = programId
            }

            val video = mappingVideo[tv.title]
            if (video != null) {
                tv.pid = video[0]
                tv.sid = video[1]
            }

            titleMap.add(tv)
            map[channel] = titleMap
            count++
        }

        return map
    }
}