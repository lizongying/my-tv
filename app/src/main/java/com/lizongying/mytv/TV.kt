package com.lizongying.mytv

import java.io.Serializable

data class TV(
    var id: Int = 0,
    var title: String,
    var videoUrl: List<String>,
    var channel: String = "",
    var logo: Any = "",
    var sourceName: String = "",

    ) : Serializable {

    override fun toString(): String {
        return "TV{" +
                "id=" + id +
                ", title='" + title + '\'' +
                ", videoUrl='" + videoUrl + '\'' +
                ", logo='" + logo + '\'' +
                ", sourceName='" + sourceName + '\'' +
                '}'
    }
}
