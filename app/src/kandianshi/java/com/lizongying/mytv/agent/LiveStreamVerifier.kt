package com.lizongying.mytv.agent

import kotlinx.coroutines.delay
import kotlinx.coroutines.ensureActive
import kotlin.coroutines.coroutineContext

object LiveStreamVerifier {
    private fun probe(url: String,fetch: (String)->HttpFetch.Reply): HttpFetch.Check = try { HttpFetch.Check(url,true,true,HttpFetch.probe(url,fetch)) }
        catch(e: kotlinx.coroutines.CancellationException) { throw e }
        catch(e: ScanDeferred) { throw e }
        catch(_: HttpFetch.UnverifiedMediaException) { HttpFetch.Check(url,false,false,0) }
        catch(_: Exception) { HttpFetch.Check(url,false,true,0) }
    data class Result(val passed: Boolean,val hls: Boolean,val reason: String,val conclusive: Boolean=false)
    suspend fun check(url: String, initial: HttpFetch.Reply, allowed: () -> Unit,
        fetch: (String) -> HttpFetch.Reply = { HttpFetch.get(it,128*1024,sample=true) },
        wait: suspend (Long) -> Unit = { delay(it) }): Result {
        var reply=initial; var current=url
        repeat(4) {
            allowed(); coroutineContext.ensureActive()
            val snapshot=HlsInspection.parse(reply.url,reply.bytes.toString(Charsets.UTF_8))
            if(snapshot==null) {
                if(reply.bytes.toString(Charsets.UTF_8).trimStart().startsWith("#EXTM3U")) return Result(false,false,"节目单不是直播媒体流",true)
                val media=probe(current,fetch)
                return Result(media.passed,false,if(media.passed) "媒体数据有效，画面待巡检" else "媒体未验证",media.conclusive)
            }
            val variant=snapshot.variant
            if(variant!=null) { current=variant; reply=fetch(current) }
            else {
                if(snapshot.ended) return Result(false,true,"已结束 / 点播清单，不收录为直播",true)
                if(snapshot.encrypted || snapshot.unsupported || snapshot.segments.isEmpty()) return Result(false,true,"加密或特殊清单，尚未验证")
                if(snapshot.targetSeconds !in 1..15) return Result(false,true,"分片周期较长，留待复查")
                // Verify a recent segment, then observe the same media playlist advancing.
                val segment=probe(snapshot.segments.last(),fetch)
                if(!segment.passed) return Result(false,true,"最新视频分片未验证",segment.conclusive)
                repeat(2) {
                    allowed(); wait(snapshot.targetSeconds*1000L); coroutineContext.ensureActive(); allowed()
                    val refreshed=fetch(current)
                    val after=HlsInspection.parse(refreshed.url,refreshed.bytes.toString(Charsets.UTF_8))
                        ?: return Result(false,true,"清单刷新无效",true)
                    if(after.ended) return Result(false,true,"直播已结束",true)
                    if(HlsInspection.progressed(snapshot,after)) {
                        if(after.encrypted || after.unsupported) return Result(false,true,"加密或特殊清单，尚未验证")
                        val recent=probe(after.segments.last(),fetch)
                        if(!recent.passed) return Result(false,true,"新增视频分片未验证",recent.conclusive)
                        return Result(true,true,"清单持续更新且视频分片有效，画面待巡检")
                    }
                }
                return Result(false,true,"两轮清单未更新，暂不收录",true)
            }
        }
        return Result(false,true,"清单嵌套超过上限")
    }
}
