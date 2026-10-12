package com.lizongying.mytv

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

    ) : Serializable {

    override fun toString(): String {
        return "TV{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", logo='" + logo + '\'' +
                ", pid='" + pid + '\'' +
                ", sid='" + sid + '\'' +
                ", programType='" + programType + '\'' +
                '}'
    }
}