package com.lizongying.mytv.agent

/** Read complete, single-packet PSI only. Missing or fragmented tables remain unknown. */
object TransportTracks {
    enum class Evidence { VIDEO, AUDIO_ONLY, UNKNOWN }
    private val video=setOf(1,2,0x10,0x1b,0x24,0x42)
    private val audio=setOf(3,4,0x0f,0x11,0x81,0x87)
    fun evidence(bytes: ByteArray): Evidence {
        fun byte(i: Int)=bytes[i].toInt() and 255
        val aligned=(0 until minOf(188,bytes.size)).firstOrNull { it+188<bytes.size && byte(it)==0x47 && byte(it+188)==0x47 } ?: return Evidence.UNKNOWN
        val programs=mutableSetOf<Int>();val tables=mutableMapOf<Int,List<Int>>()
        for(start in aligned..bytes.size-188 step 188) {
            if(byte(start)!=0x47 || byte(start+1) and 0x80!=0 || byte(start+1) and 0x40==0 || byte(start+3) and 0xc0!=0) continue
            val mode=(byte(start+3) shr 4) and 3
            if(mode!=1 && mode!=3) continue
            var pos=start+4+if(mode==3) byte(start+4)+1 else 0
            if(pos>=start+188) continue
            pos+=byte(pos)+1
            if(pos+8>start+188 || byte(pos+1) and 0xb0!=0xb0 || byte(pos+5) and 1==0) continue
            val end=pos+3+(((byte(pos+1) and 15) shl 8) or byte(pos+2))-4
            if(end>start+188 || end<pos+8) continue
            val pid=((byte(start+1) and 31) shl 8) or byte(start+2)
            if(pid==0 && byte(pos)==0) {
                var at=pos+8
                while(at+4<=end) {
                    if(byte(at)!=0 || byte(at+1)!=0) programs.add(((byte(at+2) and 31) shl 8) or byte(at+3))
                    at+=4
                }
            } else if(byte(pos)==2 && pos+12<=end) {
                var at=pos+12+(((byte(pos+10) and 15) shl 8) or byte(pos+11))
                val types=mutableListOf<Int>()
                while(at+5<=end) { types.add(byte(at));at+=5+(((byte(at+3) and 15) shl 8) or byte(at+4)) }
                if(at==end && types.isNotEmpty()) tables[pid]=types
            }
        }
        if(tables.values.any { types -> types.any { it in video } }) return Evidence.VIDEO
        return if(programs.isNotEmpty() && programs.all { pid -> tables[pid]?.let { types -> types.isNotEmpty() && types.all { it in audio } }==true }) Evidence.AUDIO_ONLY else Evidence.UNKNOWN
    }
}
