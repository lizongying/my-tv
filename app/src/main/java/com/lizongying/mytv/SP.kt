package com.lizongying.mytv

import android.content.Context
import android.content.SharedPreferences

object SP {
    // Name of the sp file TODO Should use a meaningful name and do migrations
    private const val SP_FILE_NAME = "MainActivity"
    // if Change channel with up and down in reversed order or not
    private const val KEY_CHANNEL_REVERSAL = "channel_reversal"
    // If use channel num to select channel or not
    private const val KEY_CHANNEL_NUM = "channel_num"
    // If start app on device boot or not
    private const val KEY_BOOT_STARTUP = "boot_startup"
    // Position in list of the selected channel item
    private const val KEY_POSITION = "position"
    // guid
    private const val KEY_GUID = "guid"
    // guid
    private lateinit var sp: SharedPreferences

    /**
     * The method must be invoked as early as possible(At least before using the keys)
     */
    fun init(context: Context) {
        sp = context.getSharedPreferences(SP_FILE_NAME, Context.MODE_PRIVATE)
    }

    var channelReversal: Boolean
        get() = sp.getBoolean(KEY_CHANNEL_REVERSAL, false)
        set(value) = sp.edit().putBoolean(KEY_CHANNEL_REVERSAL, value).apply()

    var channelNum: Boolean
        get() = sp.getBoolean(KEY_CHANNEL_NUM, true)
        set(value) = sp.edit().putBoolean(KEY_CHANNEL_NUM, value).apply()

    var bootStartup: Boolean
        // TODO It‘s more friendly to change the default value to false
        get() = sp.getBoolean(KEY_BOOT_STARTUP, true)
        set(value) = sp.edit().putBoolean(KEY_BOOT_STARTUP, value).apply()

    var itemPosition: Int
        get() = sp.getInt(KEY_POSITION, 0)
        set(value) = sp.edit().putInt(KEY_POSITION, value).apply()

    var guid: String
        get() = sp.getString(KEY_GUID, "")  ?: ""
        set(value) = sp.edit().putString(KEY_GUID, value).apply()
}