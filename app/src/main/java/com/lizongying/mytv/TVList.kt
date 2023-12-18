package com.lizongying.mytv

object TVList {
    val list: Map<String, Map<String, TV>> by lazy {
        setupTV()
    }

    private var count: Int = 0

    private fun setupTV(): Map<String, Map<String, TV>> {
        val tvs = """
央视频道
CCTV4K,,https://resources.yangshipin.cn/assets/oms/image/202306/3e9d06fd7244d950df5838750f1c6ac3456e172b51caca2c16d2282125b111e8.png?imageMogr2/format/webp,600002264,2000266303
CCTV1 综合,http://tvpull.dxhmt.cn/tv/11481-4.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/d57905b93540bd15f0c48230dbbbff7ee0d645ff539e38866e2d15c8b9f7dfcd.png?imageMogr2/format/webp,600001859,2000210103
CCTV2 财经,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226195/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/20115388de0207131af17eac86c33049b95d69eaff064e55653a1b941810a006.png?imageMogr2/format/webp,600001800,2000203603
CCTV3 综艺,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226397/index.m3u8
CCTV4 中文国际,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226191/index.m3u8;http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv4hd/1500/index.m3u8?&encrypt=,https://resources.yangshipin.cn/assets/oms/image/202306/f357e58fdbcc076a3d65e1f958c942b2e14f14342c60736ceed98b092d35356a.png?imageMogr2/format/webp,600001814,2000204803
CCTV5 体育,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226395/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/0a6a7138952675983a3d854df7688557b286d59aa06166edae51506f9204d655.png?imageMogr2/format/webp,600001818,2000205103
CCTV5+ 体育赛事,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226221/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/649ad76a90bfef55b05db9fe52e006487280f619089099d5dc971e387fc6eff0.png?imageMogr2/format/webp,600001817,2000204503
CCTV6 电影,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226393/index.m3u8
CCTV7 国防军事,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226192/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/b29af94e295ebdf646cefb68122c429b9cd921f498ca20d2d8070252536f9ff9.png?imageMogr2/format/webp,600004092,2000510003
CCTV8 电视剧,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226391/index.m3u8
CCTV9 记录,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226197/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/2ed1b4deeca179d5db806bb941790f82eb92a1b7299c1c38fe027f95a5caee5e.png?imageMogr2/format/webp,600004078,2000499403
CCTV10 科教,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226189/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/aa6157ec65188cd41826e5a2f088c3d6d153205f5f6428258d12c59999e221aa.png?imageMogr2/format/webp,600001805,2000203503
CCTV11 戏曲,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226240/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/ed12ed7c7a1034dae4350011fe039284c5d5a836506b28c9e32e3c75299625c0.png?imageMogr2/format/webp,600001806,2000204103
CCTV12 社会与法,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226190/index.m3u8, https://resources.yangshipin.cn/assets/oms/image/202306/484083cffaa40df7e659565e8cb4d1cc740158a185512114167aa21fa0c59240.png?imageMogr2/format/webp,600001807,2000202603
CCTV13 新闻,https://live-play.cctvnews.cctv.com/cctv/merge_cctv13.m3u8
CCTV14 少儿,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226193/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/af6b603896938dc346fbb16abfc63c12cba54b0ec9d18770a15d347d115f12d5.png?imageMogr2/format/webp,600001809,2000204403
CCTV15 音乐,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225785/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/2ceee92188ef684efe0d8b90839c4f3ad450d179dc64d59beff417059453af47.png?imageMogr2/format/webp,600001815,2000205003
CCTV16 奥林匹克,http://39.134.24.162/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226921/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/53793fa7bacd3a93ff6dc5d2758418985e1f952a316c335d663b572d8bdcd74d.png?imageMogr2/format/webp,600098637,2012375003
CCTV17 农业农村,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226198/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/ddef563072f8bad2bea5b9e52674cb7b4ed50efb20c26e61994dfbdf05c1e3c0.png?imageMogr2/format/webp,600001810,2000204203
CGTN,,https://resources.yangshipin.cn/assets/oms/image/202306/a72dff758ca1c17cd0ecc8cedc11b893d208f409d5e6302faa0e9d298848abc3.png?imageMogr2/format/webp,600014550,2001656803
CGTN 新闻频道,http://live.cgtn.com/1000/prog_index.m3u8
CGTN 纪录频道,https://livedoc.cgtn.com/500d/prog_index.m3u8
CGTN 法语频道,https://livefr.cgtn.com/1000f/prog_index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/a8d0046a47433d952bf6ed17062deb8bd2184ba9aec0f7781df6bf9487a3ffcf.png?imageMogr2/format/webp,600084704,2010153503
CGTN 俄语频道,http://liveru.cgtn.com/1000r/prog_index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/bf0a820893cbaf20dd0333e27042e1ef9c8806e5b602b6a8c95af399db0bc77a.png?imageMogr2/format/webp,600084758,2010152603
CGTN 西班牙语频道,http://livees.cgtn.com/500e/prog_index.m3u8;http://livees.cgtn.com/1000e/prog_index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202309/7c337e3dbe64402ec7e4678a619a4a6d95144e42f35161181ff78e143b7cf67a.png?imageMogr2/format/webp,600084744,2010152503
CGTN 阿拉伯语频道,http://livear.cgtn.com/1000a/prog_index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/2e44e2aa3e7a1cedf07fd0ae59fe69e86a60a2632660a006e3e9e7397b2d107e.png?imageMogr2/format/webp,600084782,2010155203
书画频道,http://211.103.180.178:8234/live_hls/hdmi.m3u8

地方频道
东方卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226217/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/9bd372ca292a82ce3aa08772b07efc4af1f85c21d1f268ea33440c49e9a0a488.png?imageMogr2/format/webp,600002483,2000292403
内蒙古卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226389/index.m3u8
湖南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226307/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/4120e89d3079d08aa17d382f69a2308ec70839b278367763c34a34666c75cb88.png?imageMogr2/format/webp,600002475,2000296203
湖北卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226477/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/7a6be5a2bb1dc53a945c016ff1f525dc4a84c51db371c15c89aa55404b0ba784.png?imageMogr2/format/webp,600002508,2000294503
辽宁卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226546/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/ac4ed6058a87c101ae7147ebc38905d0cae047fb73fd277ee5049b84f52bda36.png?imageMogr2/format/webp,600002505,2000281303
江苏卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226200/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/380ad685c0c1d5b2c902246b8d2df6d3f9b45e2837abcfe493075bbded597a31.png?imageMogr2/format/webp,600002521,2000295603
江西卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226344/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/3c760d0d00463855890e8a1864ea4a6b6dd66b90c29b4ac714a4b17c16519871.png?imageMogr2/format/webp,600002503,2000294103
西藏卫视,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226212/index.m3u8
山东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226209/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/22d403f07a7cf5410b3ad3ddb65a11aa229a32475fac213f5344c9f0ec330ca1.png?imageMogr2/format/webp,600002513,2000294803
山西卫视,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225763/index.m3u8
广东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226216/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/28886880a4dc0f06fb7e0a528a1def0591d61a65870e29176ede0cc92033bbfd.png?imageMogr2/format/webp,600002485,2000292703
广西卫视,http://live.gxrb.com.cn/tv/gxtvlive03/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/54b7e97cb816bb223fe05f3fc44da2c7820eb66e8550c19d23100f2c414ecc38.png?imageMogr2/format/webp,600002509,2000294203
重庆卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226409/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/657651f411de2673d1770d9a78b44c1265704f7468cc41d4be7f51d630768494.png?imageMogr2/format/webp,600002531,2000297803
甘肃卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225633/index.m3u8
青海卫视,http://stream.qhbtv.com/qhws/sd/live.m3u8
陕西卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226457/index.m3u8
河南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226480/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/74925962148a6d31c85808b6cd4e444c2a54bab393d2c5fc85e960b50e22fa86.png?imageMogr2/format/webp,600002525,2000296103
河北卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226406/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/d545becdc81c60197b08c7f47380705e4665ed3fe55efc8b855e486f6e655378.png?imageMogr2/format/webp,600002493,2000293403
云南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226444/index.m3u8
贵州卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226474/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/4eb45f4781d33d872af027dc01c941559aab55667dd99cc5c22bef7037807b13.png?imageMogr2/format/webp,600002490,2000293303
新疆卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226460/index.m3u8
北京卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225728/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/f4f23633c578beea49a3841d88d3490100f029ee349059fa532869db889872c5.png?imageMogr2/format/webp,600002309,2000272103
天津卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225740/index.m3u8
黑龙江卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226327/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/d8273ae9be698ce2db21f5b886ecac95a73429593f93713c60ed8c12c38bf0d3.png?imageMogr2/format/webp,600002498,2000293903
吉林卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226397/index.m3u8
浙江卫视,http://hw-m-l.cztv.com/channels/lantian/channel01/1080p.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/a66c836bd98ba3e41a2e9a570d4b9c50dedc6839e9de333e2e78212ad505f37e.png?imageMogr2/format/webp,600002520,2000295503
安徽卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226391/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/f35fa04b51b1ee4984b03578b65403570868ebca03c6c01e11b097f999a58d9b.png?imageMogr2/format/webp,600002532,2000298003
深圳卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226205/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/d59fec04c902e3581c617136d02d4b9b8c4cbe64272781ddd3525e80c823edb7.png?imageMogr2/format/webp,600002481,2000292203
四川卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225768/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/3276a414ae0eaa0f116f2045cd913367967d0c7c1e978e8621ac3879436c6ed7.png?imageMogr2/format/webp,600002516,2000295003
东南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226341/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/3208fe6564a293c21b711333fb3edb05bb5b406cff840573c9a8d839680a1579.png?imageMogr2/format/webp,600002484,2000292503
海南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226465/index.m3u8,https://resources.yangshipin.cn/assets/oms/image/202306/6e060391fde0469801fc3d84dbf204b4f8d650d251f17d7595a6964c0bb99e81.png?imageMogr2/format/webp,600002506,2000291503
宁夏卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225748/index.m3u8
安多卫视,http://stream.qhbtv.com/adws/sd/live.m3u8
三沙卫视,https://pullsstv90080111.ssws.tv/live/SSTV20220729.m3u8
延边卫视,http://live.ybtvyun.com/video/s10006-44f040627ca1/index.m3u8
浙江少儿,http://hw-m-l.cztv.com/channels/lantian/channel008/1080p.m3u8
南京少儿,http://live.nbs.cn/channels/njtv/sepd/500k.m3u8

移动专区
CCTV1,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226231/index.m3u8
CCTV2,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226195/index.m3u8
CCTV3,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226397/index.m3u8
CCTV4,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226191/index.m3u8
CCTV5,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226395/index.m3u8
CCTV5+,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226221/index.m3u8
CCTV6,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226393/index.m3u8
CCTV7,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226192/index.m3u8
CCTV8,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226391/index.m3u8
CCTV9,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226197/index.m3u8
CCTV10,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226189/index.m3u8
CCTV11,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226240/index.m3u8
CCTV12,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226190/index.m3u8
CCTV13,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226233/index.m3u8
CCTV14,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226193/index.m3u8
CCTV15,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225785/index.m3u8
CCTV16,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226921/index.m3u8
CCTV17,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226198/index.m3u8
CETV1,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225753/index.m3u8
CETV2,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225756/index.m3u8
CETV3,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226226/index.m3u8
CETV4,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226225/index.m3u8
浙江卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226199/index.m3u8
四川卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225768/index.m3u8
重庆卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226202/index.m3u8
安徽卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226203/index.m3u8
天津卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226204/index.m3u8
山西卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225763/index.m3u8
山东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226209/index.m3u8
山东教育,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226238/index.m3u8
东南卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225766/index.m3u8
海南卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225769/index.m3u8
厦门卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226219/index.m3u8
河南卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225767/index.m3u8
湖北卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226194/index.m3u8
河北卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225750/index.m3u8
湖南卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226211/index.m3u8
金鹰卡通,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225744/index.m3u8
东方卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226217/index.m3u8
哈哈炫动,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226213/index.m3u8
辽宁卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226201/index.m3u8
黑龙江卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226215/index.m3u8
吉林卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225792/index.m3u8
广西卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225770/index.m3u8
江西卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225764/index.m3u8
江苏卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226200/index.m3u8
优漫卡通,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225795/index.m3u8
深圳卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226205/index.m3u8
广东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226216/index.m3u8
大湾区卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226218/index.m3u8
北京卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226222/index.m3u8
北京卡酷,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225743/index.m3u8
冬奥纪实,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226232/index.m3u8
嘉佳卡通,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226227/index.m3u8
云南卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225751/index.m3u8
贵州卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225793/index.m3u8
宁夏卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225748/index.m3u8
甘肃卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225754/index.m3u8
西藏卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226212/index.m3u8
安多卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226228/index.m3u8
康巴卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226234/index.m3u8
新疆卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225747/index.m3u8
兵团卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226214/index.m3u8
延边卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226220/index.m3u8
内蒙古卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225786/index.m3u8
康巴卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226234/index.m3u8
电视指南,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226987/index.m3u8
风云足球,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226984/index.m3u8
风云剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226950/index.m3u8
风云音乐,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226953/index.m3u8
央视台球,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226956/index.m3u8
第一剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226959/index.m3u8
女性时尚,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226969/index.m3u8
怀旧剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226972/index.m3u8
兵器科技,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226975/index.m3u8
高尔夫网球,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226978/index.m3u8
央视文化精品,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226981/index.m3u8
哒啵电竞,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226951/index.m3u8
哒啵赛事,http://dbiptv.sn.chinamobile.com/PLTV/88888893/224/3221226954/index.m3u8
CHC高清电影,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226463/index.m3u8
CHC家庭影院,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226462/index.m3u8
CHC动作电影,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226465/index.m3u8
陕西卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225821/index.m3u8
农林卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226229/index.m3u8
陕西一套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226357/1.m3u8
陕西二套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226358/1.m3u8
陕西三套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226359/1.m3u8
陕西四套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226360/1.m3u8
陕西五套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226361/1.m3u8
陕西六套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226362/1.m3u8
陕西七套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226363/1.m3u8
陕西八套,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226364/1.m3u8
西安新闻综合,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226366/1.m3u8
西安都市,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226367/1.m3u8
西安商务资讯,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226368/1.m3u8
西安影视,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226369/1.m3u8
西安丝路,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226370/1.m3u8
西安教育,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226371/index.m3u8
咸阳-1,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226372/index.m3u8
杨凌-1,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226373/index.m3u8
延安-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226374/1.m3u8
延安-2,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226375/1.m3u8
铜川-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226379/1.m3u8
铜川-2,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226380/1.m3u8
宝鸡-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226383/1.m3u8
宝鸡-2,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226384/1.m3u8
宁强-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226390/1.m3u8
宁强-2,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226389/1.m3u8
汉中-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226331/1.m3u8
汉中-3,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225991/index.m3u8
佛坪-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226454/1.m3u8
镇巴-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226457/1.m3u8
略阳-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226322/1.m3u8
西乡-1,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226556/index.m3u8
榆林-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226377/1.m3u8
商洛-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226378/1.m3u8
渭南-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226376/1.m3u8
安康-1,http://dbiptv.sn.chinamobile.com/PLTV/88888888/224/3221226385/1.m3u8
纪实人文,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226230/index.m3u8
山东教育卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226238/index.m3u8
置业频道,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226241/index.m3u8
京视剧场,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226242/index.m3u8
家庭理财,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226244/index.m3u8
奕坦春秋,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226245/index.m3u8
发现之旅,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226235/index.m3u8
老故事,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226236/index.m3u8
        """.trimIndent()

        val map: MutableMap<String, MutableMap<String, TV>> = mutableMapOf()

        var channel = ""
        for (i in tvs.split("\n")) {
            if (i.trim() == "") {
                continue
            }
            if (!i.contains(",")) {
                channel = i.trim()
                continue
            }
            val p = i.split(",")
            val titleMap = map[channel] ?: mutableMapOf()

            val tv = titleMap[p[0]] ?: TV(
                count,
                p[0],
                p[1].split(";").map { it.trim() }
            )
            count++

            if (p.size > 2) {
                tv.logo = p[2]
            }
            if (p.size > 3) {
                tv.pid = p[3]
            }
            if (p.size > 4) {
                tv.sid = p[4]
            }

            titleMap[p[0]] = tv
            map[channel] = titleMap
        }

        return map
    }
}