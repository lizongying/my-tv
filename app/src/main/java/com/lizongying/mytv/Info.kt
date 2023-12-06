package com.lizongying.mytv

import java.io.Serializable

data class Info(
    var rowPosition: Int = 0,
    var itemPosition: Int = 0,
    var item: TV? = null,
) : Serializable