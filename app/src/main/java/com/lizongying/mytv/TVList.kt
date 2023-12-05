package com.lizongying.mytv

object TVList {
    val list: Map<String, List<TV>> by lazy {
        setupTV()
    }
    private var count: Int = 0

    private fun setupTV(): Map<String, List<TV>> {
        val tv = arrayOf(
            "央视频道,CCTV1,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv1hd265/57/20191230/index.m3u8?&encrypt=",
            "央视频道,CCTV2,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv2hd265/55/20200407/index.m3u8?&encrypt=",
            "央视频道,CCTV3,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv3hd/3000/index.m3u8?&encrypt=",
            "央视频道,CCTV4,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv4hd/1500/index.m3u8?&encrypt=",
            "央视频道,CCTV4美洲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4meihd/57/index.m3u8?&encrypt=",
            "央视频道,CCTV4欧洲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4ouhd/51/index.m3u8?&encrypt=",
            "央视频道,CCTV5,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv5hd265/57/20191230/index.m3u8?&encrypt=",
            "央视频道,CCTV5+,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv5plusnew/2500/index.m3u8?&encrypt=",
            "央视频道,CCTV6,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv6hd/3000/index.m3u8?&encrypt=",
            "央视频道,CCTV7,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv7hd/51/index.m3u8?&encrypt=",
            "央视频道,CCTV8,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv8hd/3000/index.m3u8?&encrypt=",
            "央视频道,CCTV9,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv9hd/57/index.m3u8?&encrypt=",
            "央视频道,CCTV10,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/cctv10hd/2000/index.m3u8?&encrypt=",
            "央视频道,CCTV11,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv11hd/57/20200103/index.m3u8?&encrypt=",
            "央视频道,CCTV12,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv12hd/57/index.m3u8?&encrypt=",
            "央视频道,CCTV13,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/cctv13/2000/index.m3u8?&encrypt=",
            "央视频道,CCTV14,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv14hd/3000/index.m3u8?&encrypt=",
            "央视频道,CCTV15,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv15hd/51/index.m3u8?&encrypt=",
            "央视频道,CCTV17,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv17hd/57/index.m3u8?&encrypt=",
            "央视频道,CGTN,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/cctvnews/1000/index.m3u8?&encrypt=",
            "央视频道,CCTV2,https://iptv.luas.edu.cn/liverespath/6b13fe5368d391761312a985ace065c0ecad2f5e/877097d2fa-0-0-b7736e6941fd5cb71f45ef9397b68092/index.m3u8",
            "央视频道,CCTV3,https://iptv.luas.edu.cn/liverespath/f76f9947c68be18d7a456e25aa59a08c5747e6a5/0df24da9ec-0-0-dca40ddadd2a051ce1a83536d9310820/index.m3u8",
            "央视频道,CCTV4,http://cyz32.livehbindex.hbcatv.cn/live/5b0dff5235e94ba8bff8731427145289.m3u8?bitrate=2300&pt=5",
            "央视频道,CCTV6,https://iptv.luas.edu.cn/liverespath/f2f39ee2105c85c32df375728a51b5d89d3afab4/113ed89f48-0-0-85216e55861329ec31ba1437a2ff37c9/index.m3u8",
            "央视频道,CCTV7,https://iptv.luas.edu.cn/liverespath/f116a0a5035935a3435155998163d8eaa60554c3/4ba629f762-0-0-0a66b18805ff859ce68ab1137157079e/index.m3u8",
            "央视频道,CCTV8,https://iptv.luas.edu.cn/liverespath/9e6e3b618b5dc902d992949f0c669bb674f6cde8/cae4471c68-0-0-316245c664c3311072c7279ec29672fe/index.m3u8",
            "央视频道,CCTV9,http://39.134.24.162/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226197/index.m3u8",
            "央视频道,CCTV10,https://iptv.luas.edu.cn/liverespath/924e6292a0e6f7440e8380075908fed9915b3c00/780499a193-0-0-c50d57fd93cf32a5144ec8d889eb3ed9/index.m3u8",
            "央视频道,CCTV11,https://iptv.luas.edu.cn/liverespath/3d0aa0f1604f13f0ae90c2dc0590ca22a1bcaaa2/646e868153-0-0-a53121a1df5b93a74d1980270e652878/index.m3u8",
            "央视频道,CCTV12,https://iptv.luas.edu.cn/liverespath/fb84bde1de15cdb0308a7910cebc0497594ae94e/e2d8885f42-0-0-6e1f55986652ba786606a116c5cc0775/index.m3u8",
            "央视频道,CCTV13,https://live-play.cctvnews.cctv.com/cctv/merge_cctv13.m3u8",
            "央视频道,CCTV14,https://iptv.luas.edu.cn/liverespath/0e0973e58d4835f4b872548164462930003f77b4/0ef01acfdb-0-0-3c10e95bcde07ba14f0f93a8d831b684/index.m3u8",
            "央视频道,CCTV15,http://cyz32.livehbindex.hbcatv.cn/live/50000020f44f4f4b8aa33925dba216df.m3u8?bitrate=2300&pt=5",
            "地方频道,东方卫视,http://hlsbkmgsplive.miguvideo.com/wd_r4/dfl/dongfangwshd/3000/index.m3u8?&encrypt=",
            "地方频道,内蒙古卫视,http://hlsbkmgsplive.miguvideo.com/envivo_w/2018/SD/neimeng/1000/index.m3u8?&encrypt=",
            "地方频道,湖南卫视,http://hlsbkmgsplive.miguvideo.com/wd-hunanhd-2500/index.m3u8?&encrypt=",
            "地方频道,湖北卫视,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/hubeiwshd/2000/index.m3u8?&encrypt=",
            "地方频道,辽宁卫视,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/liaoningwshd/2000/index.m3u8?&encrypt=",
            "地方频道,江苏卫视,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/jiangsuhd/2500/index.m3u8?&encrypt=",
            "地方频道,江西卫视,http://hlsbkmgsplive.miguvideo.com/migu/kailu/jxwshd/57/20190820/index.m3u8?&encrypt=",
            "地方频道,西藏卫视,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/xizang/1000/index.m3u8?&encrypt=",
            "地方频道,山东卫视,http://hlsbkmgsplive.miguvideo.com/wd-shandonghd-1200/index.m3u8?&encrypt=",
            "地方频道,广东卫视,http://hlsbkmgsplive.miguvideo.com/ws_w/2018/gdws/gdws2000/2000/index.m3u8?&encrypt=",
            "地方频道,广西卫视,http://hlsbkmgsplive.miguvideo.com/wd-guangxiwssd-600/index.m3u8?&encrypt=",
            "地方频道,重庆卫视,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/chongqing/1000/index.m3u8?&encrypt=",
            "地方频道,甘肃卫视,http://hlsbkmgsplive.miguvideo.com/envivo_v/2018/SD/gansu/1000/index.m3u8?&encrypt=",
            "地方频道,青海卫视,http://hlsbkmgsplive.miguvideo.com/envivo_w/2018/SD/qinghai/1000/index.m3u8?&encrypt=",
            "地方频道,陕西卫视,http://hlsbkmgsplive.miguvideo.com/envivo_w/2018/SD/shan3xi/1000/index.m3u8?&encrypt=",
            "地方频道,河南卫视,http://hlsbkmgsplive.miguvideo.com/wd-henanwssd-600/index.m3u8?&encrypt=",
            "地方频道,大湾区卫视,http://hlsbkmgsplive.miguvideo.com/wd_r3/2018/nfmedia/nfws/1000/index.m3u8?&encrypt=",
            "地方频道,云南卫视,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/yunnan/1000/index.m3u8?&encrypt=",
            "地方频道,贵州卫视,http://hlsbkmgsplive.miguvideo.com/wd-guizhouwssd-600/index.m3u8?&encrypt=",
            "地方频道,新疆卫视,http://hlsbkmgsplive.miguvideo.com/wd-xinjiangwssd-600/index.m3u8?&encrypt=",
            "地方频道,北京卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020539e48f392fbcbba5cf7f245.m3u8?bitrate=2300&pt=5",
            "地方频道,重庆卫视,https://sjlivecdn9.cbg.cn/202312010019/app_2/_definst_/ls_2.stream/chunklist.m3u8",
            "地方频道,天津卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020bbc84ee1afd7177a073393d7.m3u8?bitrate=2300&pt=5",
            "地方频道,黑龙江卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020281749ed995f2824759051fa.m3u8?bitrate=2300&pt=5",
            "地方频道,辽宁卫视,http://cyz32.livehbindex.hbcatv.cn/live/5000002019d84b0ba201007677bbf28c.m3u8?bitrate=2300&pt=5",
            "地方频道,吉林卫视,http://cyz32.livehbindex.hbcatv.cn/live/500000205c15461b956f19f9b6896ad6.m3u8?bitrate=2300&pt=5",
            "地方频道,河北卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020d6be4c4ba70a4e3261b14544.m3u8?bitrate=2300&pt=5",
            "地方频道,河北农民,http://cyz32.livehbindex.hbcatv.cn/live/500000202fa64c9bac01e09eaf06afd8.m3u8?bitrate=2300&pt=5",
            "地方频道,河南卫视,https://1gaanhqjqgezdgco.ourdvsss.com/tvcdn.stream3.hndt.com/tv/65c4a6d5017e1000b2b6ea2500000000_transios/playlist.m3u8?wsSecret=e9e7400c1a48039c4975cbfa69c33ba5&wsTime=1701372036&wsSession=eb0bfe9b75597f4505b2d9ba-170135801075815&wsIPSercert=5b0db1aecf3122e1ca8cb9bb5b4ee144&wsiphost=local&wsBindIP=1",
            "地方频道,山东卫视,http://39.134.24.162/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226012/index.m3u8",
            "地方频道,山西卫视,https://livehhhttps.sxrtv.com/lsdream/q8RVWgs/1000/n4sh8sV.m3u8",
            "地方频道,湖北卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020273747628129718a5717b2dd.m3u8?bitrate=2300&pt=5",
            "地方频道,湖南卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020c9ea4ce4bf3cfbbb7f8b8bb7.m3u8?bitrate=2300&pt=5",
            "地方频道,浙江卫视,http://cyz32.livehbindex.hbcatv.cn/live/5000002045414da0bcca7fb08fd34c8a.m3u8?bitrate=2300&pt=5",
            "地方频道,江苏卫视,http://cyz32.livehbindex.hbcatv.cn/live/500000201708473e9f4e1dbb0361de6b.m3u8?bitrate=2300&pt=5",
            "地方频道,江西卫视,https://yun-live.jxtvcn.com.cn/live-jxtv/tv_jxtv1.m3u8?source=pc&t=170136577478931&token=6f10e030312c8c458b3ac4e9f908712c",
            "地方频道,安徽卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020d0ab40578faaa8a023ce4d5c.m3u8?bitrate=2300&pt=5",
            "地方频道,广东卫视,http://ci.emdoor.tech:8080/touchtv.php?pk=1182",
            "地方频道,广西卫视,http://live.gxrb.com.cn/tv/gxtvlive03/index.m3u8",
            "地方频道,深圳卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020289041678b7585eb32637331.m3u8?bitrate=2300&pt=5",
            "地方频道,四川卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020407a4a109f5dfc6f26dbf5eb.m3u8?bitrate=2300&pt=5",
            "地方频道,贵州卫视,https://9bwaz8y2.gzstv.com/live/CH01_lo.m3u8?txSecret=6fa8934c1ce61d48a6f746ec2982b887&txTime=6568D143",
            "地方频道,云南卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020a2f74b03928347fb0b02768b.m3u8?bitrate=2300&pt=5",
            "地方频道,东南卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020c18c4f03a2107aa78ced8fff.m3u8?bitrate=2300&pt=5",
            "地方频道,海南卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020741c4046820cd89d5ecbd401.m3u8?bitrate=2300&pt=5",
            "地方频道,内蒙古卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020eeba44449d4f4913a2897e4e.m3u8?bitrate=2300&pt=5",
            "地方频道,陕西卫视,http://cyz32.livehbindex.hbcatv.cn/live/5000002019984c0fa29ae0d57380da3a.m3u8?bitrate=2300&pt=5",
            "地方频道,甘肃卫视,http://cyz32.livehbindex.hbcatv.cn/live/500000206da44fde810a98a3d7848f82.m3u8?bitrate=2300&pt=5",
            "地方频道,宁夏卫视,http://cyz32.livehbindex.hbcatv.cn/live/500000205dfa4e06bf7243e30e5182c8.m3u8?bitrate=2300&pt=5",
            "地方频道,新疆卫视,http://cyz32.livehbindex.hbcatv.cn/live/500000201e484cbeb064a31201e3dd05.m3u8?bitrate=2300&pt=5",
            "地方频道,青海卫视,http://stream.qhbtv.com/qhws/sd/live.m3u8",
            "地方频道,安多卫视,http://stream.qhbtv.com/adws/sd/live.m3u8",
            "地方频道,西藏卫视,http://cyz32.livehbindex.hbcatv.cn/live/50000020816449349b10593cfdde8311.m3u8?bitrate=2300&pt=5",
            "地方频道,延边卫视,http://live.ybtvyun.com/video/s10006-44f040627ca1/index.m3u8",
            "地方频道,兵团卫视,http://cyz32.livehbindex.hbcatv.cn/live/500000200a7e4caf973012bf85f33406.m3u8?bitrate=2300&pt=5",
            "地方频道,香港卫视,http://zhibo.hkstv.tv/livestream/mutfysrq/playlist.m3u8",
            "地方频道,河北农民,http://cyz32.livehbindex.hbcatv.cn/live/500000202fa64c9bac01e09eaf06afd8.m3u8?bitrate=2300&pt=5",
            "地方频道,河北影视剧,http://cyz32.livehbindex.hbcatv.cn/live/50000020b87d48a78fc68b1f51e8a926.m3u8?bitrate=2300&pt=5",
            "地方频道,河北公共,http://cyz32.livehbindex.hbcatv.cn/live/50000020f9834d34b527dec2d5ad8bb5.m3u8?bitrate=2300&pt=5",
            "地方频道,河北都市,http://cyz32.livehbindex.hbcatv.cn/live/50000020072541118d32f3491df3fd31.m3u8?bitrate=2300&pt=5",
            "地方频道,河北少儿科教,http://cyz32.livehbindex.hbcatv.cn/live/500000206980450095b990d63f377cea.m3u8?bitrate=2300&pt=5",
            "地方频道,河北杂技,http://cyz32.livehbindex.hbcatv.cn/live/50000020344549cc94c638aab1e2cd4c.m3u8?bitrate=2300&pt=5",
            "地方频道,东方影视,http://hlsbkmgsplive.miguvideo.com/wd_r4/dfl/dianshijuhd/3000/index.m3u8?&encrypt=",
        )

        val map: MutableMap<String, MutableList<TV>> = mutableMapOf()

        for (i in tv) {
            val (channel, title, videoUrl) = i.split(",")
            val videoList = map[channel] ?: mutableListOf()
            videoList.add(buildTV(title, videoUrl))
            map[channel] = videoList
        }

        return map
    }

    private fun buildTV(
        title: String,
        videoUrl: String,
    ): TV {
        val tv = TV()
        tv.id = count++
        tv.title = title
        tv.videoUrl = videoUrl
        return tv
    }
}