package com.lizongying.mytv

import android.graphics.Bitmap
import android.graphics.Color
import android.graphics.drawable.BitmapDrawable
import android.widget.ImageView
import androidx.core.content.ContextCompat
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.DiskCacheStrategy
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.target.Target
import com.lizongying.mytv.agent.*

/** Channel identity images are shown only in the programme UI, never over the live picture. */
object ChannelLogo {
    fun clear(view: ImageView) {
        view.setTag(R.id.channel_logo_token,Any())
        Glide.with(view).clear(view)
        view.setImageDrawable(null)
    }
    fun bind(view: ImageView, title: String, logo: Any?) {
        clear(view)
        val token=Any();view.setTag(R.id.channel_logo_token,token)
        val c=view.context.applicationContext
        val config=AgentConfigStore(c).load()
        val original=(logo as? String)?.trim()?.let(RealLogoPolicy::safeUrl)
        val fallback=ContextCompat.getDrawable(view.context,R.drawable.kan_logo)
        view.contentDescription=title
        view.scaleType=ImageView.ScaleType.FIT_CENTER
        // Many broadcast logos are transparent white artwork; a neutral plate keeps them visible.
        view.setBackgroundColor(0xFF647580.toInt())
        view.setImageDrawable(fallback)
        if(config.logoLookupEnabled) {
            RealLogoStore.bitmap(c,title)?.let {
                view.setImageDrawable(BitmapDrawable(view.resources,it))
                if(config.enabled) RealLogoStore.note(c,title,original)
                return
            }
        }
        val urls=if(config.logoLookupEnabled) RealLogoStore.urls(c,title,original) else listOfNotNull(original)
        val usable=urls.filterNot { RealLogoStore.isFailed(c,it) }
        if(config.enabled && config.logoLookupEnabled) RealLogoStore.note(c,title,original)
        fun load(index: Int) {
            if(view.getTag(R.id.channel_logo_token)!==token || index>=usable.size) return
            val remote=usable[index]
            fun failed() {
                RealLogoStore.note(c,title,original,remote)
                // Glide listeners cannot start another request synchronously.
                view.post { if(view.getTag(R.id.channel_logo_token)===token) load(index+1) }
            }
            Glide.with(view).asBitmap().load(remote).override(400,160).timeout(4000).fitCenter()
                .diskCacheStrategy(DiskCacheStrategy.NONE).placeholder(fallback).error(fallback)
                .listener(object: RequestListener<Bitmap> {
                    override fun onLoadFailed(e: GlideException?, model: Any?, target: Target<Bitmap>?, first: Boolean): Boolean {
                        if(view.getTag(R.id.channel_logo_token)===token) failed()
                        return true
                    }
                    override fun onResourceReady(resource: Bitmap, model: Any?, target: Target<Bitmap>?, source: DataSource?, first: Boolean): Boolean {
                        if(view.getTag(R.id.channel_logo_token)!==token) return true
                        if(!useful(resource)) { failed();return true }
                        return false
                    }
                }).into(view)
        }
        load(0)
    }
    internal fun useful(bitmap: Bitmap): Boolean {
        var first: Int?=null
        for(y in 0..15) for(x in 0..31) {
            val pixel=bitmap.getPixel(x*(bitmap.width-1)/31,y*(bitmap.height-1)/15)
            if(Color.alpha(pixel)>32) {
                if(first==null) first=pixel else if(pixel!=first) return true
            } else if(first!=null) return true
        }
        return first!=null && bitmap.hasAlpha() && (0..15).any { y -> (0..31).any { x ->
            Color.alpha(bitmap.getPixel(x*(bitmap.width-1)/31,y*(bitmap.height-1)/15))<=32
        } }
    }
}
