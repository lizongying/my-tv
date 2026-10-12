package com.lizongying.mytv.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

object LiveStreamVerifier {
    data class Result(val passed: Boolean, val hls: Boolean, val reason: String)
    suspend fun check(url: String, initial: HttpFetch.Reply, allowed: () -> Unit,
        fetch: (String) -> HttpFetch.Reply = { HttpFetch.get(it,128*1024,sample=true) },
        wait: suspend (Long) -> Unit = { delay(it) }): Result {
        var reply=initial; var current=url
        repeat(4) {
            allowed(); coroutineContext.ensureActive()
            val snapshot=HlsInspection.parse(reply.url,reply.bytes.toString(Charsets.UTF_8))
            if(snapshot==null) {
                return if(runCatching { HttpFetch.probe(current,fetch) }.isSuccess) Result(true,false,"媒体数据有效，画面待巡检")
                    else Result(false,false,"媒体未验证")
            }
            val variant=snapshot.variant
            if(variant!=null) { current=variant; reply=fetch(current) }
            else {
                if(snapshot.ended) return Result(false,true,"已结束 / 点播清单，不收录为直播")
                if(snapshot.encrypted || snapshot.unsupported || snapshot.segments.isEmpty()) return Result(false,true,"加密或特殊清单，尚未验证")
                if(snapshot.targetSeconds !in 1..15) return Result(false,true,"分片周期较长，留待复查")
                // Verify a recent segment, then observe the same media playlist advancing.
                if(runCatching { HttpFetch.probe(snapshot.segments.last(),fetch) }.isFailure) return Result(false,true,"最新视频分片未验证")
                repeat(2) {
                    allowed(); wait(snapshot.targetSeconds*1000L); coroutineContext.ensureActive(); allowed()
                    val refreshed=fetch(current)
                    val after=HlsInspection.parse(refreshed.url,refreshed.bytes.toString(Charsets.UTF_8))
                        ?: return Result(false,true,"清单刷新无效")
                    if(after.ended) return Result(false,true,"直播已结束")
                    if(HlsInspection.progressed(snapshot,after)) {
                        if(after.encrypted || after.unsupported || runCatching { HttpFetch.probe(after.segments.last(),fetch) }.isFailure)
                            return Result(false,true,"新增视频分片未验证")
                        return Result(true,true,"清单持续更新且视频分片有效，画面待巡检")
                    }
                }
                return Result(false,true,"两轮清单未更新，暂不收录")
            }
        }
        return Result(false,true,"清单嵌套超过上限")
    }
}
