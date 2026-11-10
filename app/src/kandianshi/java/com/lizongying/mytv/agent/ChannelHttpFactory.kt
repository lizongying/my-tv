package com.lizongying.mytv.agent

@androidx.annotation.OptIn(androidx.media3.common.util.UnstableApi::class)
object ChannelHttpFactory {
    fun create(): androidx.media3.datasource.DataSource.Factory {
        val upstream=androidx.media3.datasource.DefaultHttpDataSource.Factory()
            .setUserAgent("Mozilla/5.0 (Linux; Android) KanDianshi/1.3")
            .setConnectTimeoutMs(8000).setReadTimeoutMs(8000).setAllowCrossProtocolRedirects(true)
        return androidx.media3.datasource.ResolvingDataSource.Factory(upstream) { data ->
            val original=data.uri.toString();val resolved=CwjdResolver.resolve(original)
            if(resolved==original) data else data.withUri(android.net.Uri.parse(resolved))
        }
    }
}
