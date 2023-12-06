package com.lizongying.mytv

object TVList {
    val list: Map<String, Map<String, Set<String>>> by lazy {
        setupTV()
    }

    private fun setupTV(): Map<String, Map<String, Set<String>>> {
        val tvs = """
央视频道,CCTV1,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv1hd265/57/20191230/index.m3u8?&encrypt=
央视频道,CCTV2,https://iptv.luas.edu.cn/liverespath/6b13fe5368d391761312a985ace065c0ecad2f5e/877097d2fa-0-0-b7736e6941fd5cb71f45ef9397b68092/index.m3u8
央视频道,CCTV2,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv2hd265/55/20200407/index.m3u8?&encrypt=
央视频道,CCTV3,https://iptv.luas.edu.cn/liverespath/f76f9947c68be18d7a456e25aa59a08c5747e6a5/0df24da9ec-0-0-dca40ddadd2a051ce1a83536d9310820/index.m3u8
央视频道,CCTV3,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv3hd/3000/index.m3u8?&encrypt=
央视频道,CCTV4 中文国际,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226191/index.m3u8
央视频道,CCTV4 中文国际,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv4hd/1500/index.m3u8?&encrypt=
央视频道,CCTV4 美洲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4meihd/57/index.m3u8?&encrypt=
央视频道,CCTV4 欧洲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4ouhd/51/index.m3u8?&encrypt=
央视频道,CCTV5,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv5hd265/57/20191230/index.m3u8?&encrypt=
央视频道,CCTV5+,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv5plusnew/2500/index.m3u8?&encrypt=
央视频道,CCTV6,https://iptv.luas.edu.cn/liverespath/f2f39ee2105c85c32df375728a51b5d89d3afab4/113ed89f48-0-0-85216e55861329ec31ba1437a2ff37c9/index.m3u8
央视频道,CCTV6,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv6hd/3000/index.m3u8?&encrypt=
央视频道,CCTV7,https://iptv.luas.edu.cn/liverespath/f116a0a5035935a3435155998163d8eaa60554c3/4ba629f762-0-0-0a66b18805ff859ce68ab1137157079e/index.m3u8
央视频道,CCTV7,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv7hd/51/index.m3u8?&encrypt=
央视频道,CCTV8,https://iptv.luas.edu.cn/liverespath/9e6e3b618b5dc902d992949f0c669bb674f6cde8/cae4471c68-0-0-316245c664c3311072c7279ec29672fe/index.m3u8
央视频道,CCTV8,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv8hd/3000/index.m3u8?&encrypt=
央视频道,CCTV9,http://39.134.24.162/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226197/index.m3u8
央视频道,CCTV9,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv9hd/57/index.m3u8?&encrypt=
央视频道,CCTV10,https://iptv.luas.edu.cn/liverespath/924e6292a0e6f7440e8380075908fed9915b3c00/780499a193-0-0-c50d57fd93cf32a5144ec8d889eb3ed9/index.m3u8
央视频道,CCTV10,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/cctv10hd/2000/index.m3u8?&encrypt=
央视频道,CCTV11,https://iptv.luas.edu.cn/liverespath/3d0aa0f1604f13f0ae90c2dc0590ca22a1bcaaa2/646e868153-0-0-a53121a1df5b93a74d1980270e652878/index.m3u8
央视频道,CCTV11,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv11hd/57/20200103/index.m3u8?&encrypt=
央视频道,CCTV12,https://iptv.luas.edu.cn/liverespath/fb84bde1de15cdb0308a7910cebc0497594ae94e/e2d8885f42-0-0-6e1f55986652ba786606a116c5cc0775/index.m3u8
央视频道,CCTV12,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv12hd/57/index.m3u8?&encrypt=
央视频道,CCTV13,https://live-play.cctvnews.cctv.com/cctv/merge_cctv13.m3u8
央视频道,CCTV13,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/cctv13/2000/index.m3u8?&encrypt=
央视频道,CCTV14,https://iptv.luas.edu.cn/liverespath/0e0973e58d4835f4b872548164462930003f77b4/0ef01acfdb-0-0-3c10e95bcde07ba14f0f93a8d831b684/index.m3u8
央视频道,CCTV14,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv14hd/3000/index.m3u8?&encrypt=
央视频道,CCTV15 音乐,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225785/index.m3u8
央视频道,CCTV15,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv15hd/51/index.m3u8?&encrypt=
央视频道,CCTV17,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv17hd/57/index.m3u8?&encrypt=
央视频道,CGTN,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/cctvnews/1000/index.m3u8?&encrypt=
央视频道,CGTN 新闻频道,http://live.cgtn.com/1000/prog_index.m3u8
央视频道,CGTN 纪录频道,https://livedoc.cgtn.com/500d/prog_index.m3u8
央视频道,CGTN 法语频道,https://livefr.cgtn.com/1000f/prog_index.m3u8
央视频道,CGTN 俄语频道,http://liveru.cgtn.com/1000r/prog_index.m3u8
央视频道,CGTN 西班牙语频道,http://livees.cgtn.com/500e/prog_index.m3u8
央视频道,CGTN 阿拉伯语频道,http://livear.cgtn.com/1000a/prog_index.m3u8
央视频道,CGTN 拉丁美洲频道,http://livees.cgtn.com/1000e/prog_index.m3u8
央视频道,书画频道,http://211.103.180.178:8234/live_hls/hdmi.m3u8

地方频道,东方卫视,http://hlsbkmgsplive.miguvideo.com/wd_r4/dfl/dongfangwshd/3000/index.m3u8?&encrypt=
地方频道,内蒙古卫视,http://hlsbkmgsplive.miguvideo.com/envivo_w/2018/SD/neimeng/1000/index.m3u8?&encrypt=
地方频道,湖南卫视,http://hlsbkmgsplive.miguvideo.com/wd-hunanhd-2500/index.m3u8?&encrypt=
地方频道,湖北卫视,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/hubeiwshd/2000/index.m3u8?&encrypt=
地方频道,辽宁卫视,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/liaoningwshd/2000/index.m3u8?&encrypt=
地方频道,江苏卫视,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/jiangsuhd/2500/index.m3u8?&encrypt=
地方频道,江西卫视,http://hlsbkmgsplive.miguvideo.com/migu/kailu/jxwshd/57/20190820/index.m3u8?&encrypt=
地方频道,西藏卫视,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/xizang/1000/index.m3u8?&encrypt=
地方频道,山东卫视,http://hlsbkmgsplive.miguvideo.com/wd-shandonghd-1200/index.m3u8?&encrypt=
地方频道,广东卫视,http://hlsbkmgsplive.miguvideo.com/ws_w/2018/gdws/gdws2000/2000/index.m3u8?&encrypt=
地方频道,广西卫视,http://hlsbkmgsplive.miguvideo.com/wd-guangxiwssd-600/index.m3u8?&encrypt=
地方频道,重庆卫视,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/chongqing/1000/index.m3u8?&encrypt=
地方频道,甘肃卫视,http://hlsbkmgsplive.miguvideo.com/envivo_v/2018/SD/gansu/1000/index.m3u8?&encrypt=
地方频道,青海卫视,http://hlsbkmgsplive.miguvideo.com/envivo_w/2018/SD/qinghai/1000/index.m3u8?&encrypt=
地方频道,陕西卫视,http://hlsbkmgsplive.miguvideo.com/envivo_w/2018/SD/shan3xi/1000/index.m3u8?&encrypt=
地方频道,河南卫视,http://hlsbkmgsplive.miguvideo.com/wd-henanwssd-600/index.m3u8?&encrypt=
地方频道,大湾区卫视,http://hlsbkmgsplive.miguvideo.com/wd_r3/2018/nfmedia/nfws/1000/index.m3u8?&encrypt=
地方频道,云南卫视,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/yunnan/1000/index.m3u8?&encrypt=
地方频道,贵州卫视,http://hlsbkmgsplive.miguvideo.com/wd-guizhouwssd-600/index.m3u8?&encrypt=
地方频道,新疆卫视,http://hlsbkmgsplive.miguvideo.com/wd-xinjiangwssd-600/index.m3u8?&encrypt=
地方频道,北京卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225728/index.m3u8
地方频道,重庆卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226409/index.m3u8
地方频道,天津卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225740/index.m3u8
地方频道,黑龙江卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226327/index.m3u8
地方频道,辽宁卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226546/index.m3u8
地方频道,吉林卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226397/index.m3u8
地方频道,河北卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226406/index.m3u8
地方频道,河南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226480/index.m3u8
地方频道,山东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226209/index.m3u8
地方频道,山西卫视,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225763/index.m3u8
地方频道,湖北卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226477/index.m3u8
地方频道,湖南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226307/index.m3u8
地方频道,浙江卫视,http://hw-m-l.cztv.com/channels/lantian/channel01/1080p.m3u8
地方频道,江苏卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226200/index.m3u8
地方频道,江西卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226344/index.m3u8
地方频道,安徽卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226391/index.m3u8
地方频道,广东卫视,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226216/index.m3u8
地方频道,广西卫视,http://live.gxrb.com.cn/tv/gxtvlive03/index.m3u8
地方频道,深圳卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226205/index.m3u8
地方频道,四川卫视,http://39.134.24.166/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225768/index.m3u8
地方频道,贵州卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226474/index.m3u8
地方频道,云南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226444/index.m3u8
地方频道,东南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226341/index.m3u8
地方频道,海南卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226465/index.m3u8
地方频道,内蒙古卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226389/index.m3u8
地方频道,陕西卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226457/index.m3u8
地方频道,甘肃卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221225633/index.m3u8
地方频道,宁夏卫视,https://hls.ningxiahuangheyun.com/tv/nxws.m3u8
地方频道,新疆卫视,http://ottrrs.hl.chinamobile.com/PLTV/88888888/224/3221226460/index.m3u8
地方频道,青海卫视,http://stream.qhbtv.com/qhws/sd/live.m3u8
地方频道,安多卫视,http://stream.qhbtv.com/adws/sd/live.m3u8
地方频道,西藏卫视,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226212/index.m3u8
地方频道,延边卫视,http://live.ybtvyun.com/video/s10006-44f040627ca1/index.m3u8
        """.trimIndent()

        val map: MutableMap<String, MutableMap<String, LinkedHashSet<String>>> = mutableMapOf()

        for (i in tvs.split("\n")) {
            if (!i.contains(",")) {
                continue
            }
            val (channel, title, url) = i.split(",")
            val titleMap = map[channel] ?: mutableMapOf()

            val urlSet = titleMap[title] ?: LinkedHashSet()
            urlSet.add(url)

            titleMap[title] = urlSet
            map[channel] = titleMap
        }

        return map
    }
}