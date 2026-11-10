package com.lizongying.mytv.agent
/** Bounded in-memory repair priorities; repeated failures do not flood disk or requests. */
class RecoveryRequests {
 private var lastRefresh: Long?=null
 private val last=linkedMapOf<String,Long>();private val wanted=linkedMapOf<String,Long>()
 @Synchronized fun request(title: String,now: Long): Boolean {
  if(title.isBlank()) return false
  val key=title.take(80);val previous=last[key]
  if(previous!=null && now-previous in 0 until 900000) return false
  last[key]=now;wanted[key]=now
  while(last.size>16) { val old=last.keys.first();last.remove(old);wanted.remove(old) }
  return true
 }
 @Synchronized fun pending(now: Long): Set<String> { wanted.entries.removeAll { now-it.value !in 0..21600000 };return wanted.keys.toSet() }
 @Synchronized fun refreshDue(now: Long): Boolean = pending(now).isNotEmpty() && lastRefresh?.let { now-it in 0 until 900000 }!=true
 @Synchronized fun refreshed(now: Long) { lastRefresh=now }
 @Synchronized fun complete(title: String) { wanted.remove(title) }
 companion object { val shared=RecoveryRequests() }
}
