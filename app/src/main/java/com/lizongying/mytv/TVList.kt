package com.lizongying.mytv

object TVList {
    val list: Map<String, Map<String, Set<String>>> by lazy {
        setupTV()
    }

    private fun setupTV(): Map<String, Map<String, Set<String>>> {
        val tvs = """
央视频道,CCTV1 综合,http://tvpull.dxhmt.cn/tv/11481-4.m3u8
央视频道,CCTV1 综合,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv1hd265/57/20191230/index.m3u8?&encrypt=
央视频道,CCTV2 财经,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv2hd265/55/20200407/index.m3u8?&encrypt=
央视频道,CCTV3 综艺,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv3hd/3000/index.m3u8?&encrypt=
央视频道,CCTV4 中文国际,http://39.134.24.161/dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221226191/index.m3u8
央视频道,CCTV4 中文国际,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv4hd/1500/index.m3u8?&encrypt=
央视频道,CCTV4 中文国际美洲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4meihd/57/index.m3u8?&encrypt=
央视频道,CCTV4 中文国际欧洲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4ouhd/51/index.m3u8?&encrypt=
央视频道,CCTV5 体育,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv5hd265/57/20191230/index.m3u8?&encrypt=
央视频道,CCTV5+ 体育赛事,http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv5plusnew/2500/index.m3u8?&encrypt=
央视频道,CCTV6 电影,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv6hd/3000/index.m3u8?&encrypt=
央视频道,CCTV7 国防军事,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv7hd/51/index.m3u8?&encrypt=
央视频道,CCTV8 电视剧,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv8hd/3000/index.m3u8?&encrypt=
央视频道,CCTV9 记录,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv9hd/57/index.m3u8?&encrypt=
央视频道,CCTV10 科教,http://hlsbkmgsplive.miguvideo.com/wd_r2/2018/ocn/cctv10hd/2000/index.m3u8?&encrypt=
央视频道,CCTV11 戏曲,http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv11hd/57/20200103/index.m3u8?&encrypt=
央视频道,CCTV12 社会与法,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv12hd/57/index.m3u8?&encrypt=
央视频道,CCTV13 新闻,https://live-play.cctvnews.cctv.com/cctv/merge_cctv13.m3u8
央视频道,CCTV13 新闻,http://hlsbkmgsplive.miguvideo.com/envivo_x/2018/SD/cctv13/2000/index.m3u8?&encrypt=
央视频道,CCTV14 少儿,http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv14hd/3000/index.m3u8?&encrypt=
央视频道,CCTV15 音乐,http://dbiptv.sn.chinamobile.com/PLTV/88888890/224/3221225785/index.m3u8
央视频道,CCTV15 音乐,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv15hd/51/index.m3u8?&encrypt=
央视频道,CCTV17 农业农村,http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv17hd/57/index.m3u8?&encrypt=
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