package com.lizongying.mytv.models

import com.lizongying.mytv.models.ProgramType
import java.io.Serializable

data class TV(
    var id: Int = 0,
    var title: String,
    var alias: String = "",
    var videoUrl: List<String>,
    var channel: String = "",
    var logo: Any = "",
    var pid: String = "",
    var sid: String = "",
    var programType: ProgramType,
    var needToken: Boolean = false,
    var mustToken: Boolean = false,
    var volume: Float = 0.1F,
) : Serializable {

    override fun toString(): String {
        return "TV{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", alias='" + alias + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", channel='" + channel + '\'' +
                ", logo='" + logo + '\'' +
                ", pid='" + pid + '\'' +
                ", sid='" + sid + '\'' +
                ", programType='" + programType + '\'' +
                ", needToken='" + needToken + '\'' +
                ", mustToken='" + mustToken + '\'' +
                '}'
    }
}