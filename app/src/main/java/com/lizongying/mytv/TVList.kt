package com.lizongying.mytv

object TVList {
    val list: Map<String, List<TV>> by lazy {
        setupTV()
    }
    private var count: Int = 0

    private fun setupTV(): Map<String, List<TV>> {
        val tv = arrayOf(
            arrayOf(
                "央视频道",
                "CCTV1",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv1hd265/57/20191230/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV2",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv2hd265/55/20200407/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV3",
                "http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv3hd/3000/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV4",
                "http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv4hd/1500/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV4美洲",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4meihd/57/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV4欧洲",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4ouhd/51/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV5",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv5hd265/57/20191230/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频道",
                "CCTV5+",
                "http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv5plusnew/2500/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV1",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv1hd265/57/20191230/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV2",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv2hd265/55/20200407/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV3",
                "http://hlsbkmgsplive.miguvideo.com/wd_r2/ocn/cctv3hd/3000/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV4",
                "http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv4hd/1500/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV4美洲",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4meihd/57/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV4欧洲",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/20200324/cctv4ouhd/51/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV5",
                "http://hlsbkmgsplive.miguvideo.com/migu/kailu/cctv5hd265/57/20191230/index.m3u8?&encrypt=",
            ),
            arrayOf(
                "央视频",
                "CCTV5+",
                "http://hlsbkmgsplive.miguvideo.com/wd_r2/cctv/cctv5plusnew/2500/index.m3u8?&encrypt=",
            ),
        )

        val map: MutableMap<String, MutableList<TV>> = mutableMapOf()

        for (i in tv) {
            val channelName = i[0]
            val movieList = map[channelName] ?: mutableListOf()
            movieList.add(buildTV(i[1], i[2]))
            map[channelName] = movieList
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