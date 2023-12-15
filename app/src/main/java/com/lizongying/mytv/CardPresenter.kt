package com.lizongying.mytv

import android.content.Context
import android.graphics.Bitmap
import android.media.MediaMetadataRetriever
import android.util.Log
import android.view.ContextThemeWrapper
import android.view.ViewGroup
import androidx.leanback.widget.ImageCardView
import androidx.leanback.widget.Presenter
import androidx.lifecycle.LifecycleCoroutineScope
import androidx.lifecycle.LifecycleOwner
import com.bumptech.glide.Glide
import com.lizongying.mytv.models.TVViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

class CardPresenter(
    private val lifecycleScope: LifecycleCoroutineScope,
//    private val owner: LifecycleOwner
) : Presenter() {

    private var tvViewModel: TVViewModel? = null
    fun bindData(viewModel: TVViewModel, fragment: Context?, viewHolder: ViewHolder) {
    }

    fun setViewModel(model: TVViewModel) {
        tvViewModel = model
    }

    fun unbind() {
//        viewModel?.backgroundImage?.removeObservers(fragment)
    }

    override fun onCreateViewHolder(parent: ViewGroup): ViewHolder {
        val cardView = object :
            ImageCardView(ContextThemeWrapper(parent.context, R.style.CustomImageCardTheme)) {
            override fun setSelected(selected: Boolean) {
                updateCardBackgroundColor(this)
                super.setSelected(selected)
            }
        }

        cardView.isFocusable = true
        cardView.isFocusableInTouchMode = true
        return ViewHolder(cardView)
    }

    override fun onBindViewHolder(viewHolder: ViewHolder, item: Any) {
        val tv = item as TV
        val cardView = viewHolder.view as ImageCardView

        cardView.titleText = tv.title
        cardView.setMainImageDimensions(CARD_WIDTH, CARD_HEIGHT)
        cardView.tag = tv.videoUrl

//            lifecycleScope.launch(Dispatchers.IO) {
//                val videoThumbnail = tv.videoUrl?.let { getVideoThumbnail(it) }
//
//                withContext(Dispatchers.Main) {
//                    cardView.mainImageView.setImageBitmap(videoThumbnail)
//                }
//            }

//        tvViewModel?.update(tv)
//        tvViewModel?.backgroundImage?.observe(owner) { imageUrl ->
//            if (imageUrl != "") {
//                Glide.with(viewHolder.view.context)
//                    .load(imageUrl)
//                    .centerCrop()
//                    .into(cardView.mainImageView)
//            }
//        }
    }

    override fun onUnbindViewHolder(viewHolder: ViewHolder) {
        val cardView = viewHolder.view as ImageCardView
        cardView.badgeImage = null
        cardView.mainImage = null
    }

    private fun updateCardBackgroundColor(view: ImageCardView) {
        val currentTag = view.tag
        lifecycleScope.launch(Dispatchers.IO) {
            delay(1000)
            if (view.isSelected && view.tag != null && currentTag == view.tag) {
                val videoThumbnail = view.tag.toString().let { getVideoThumbnail(it) }
                withContext(Dispatchers.Main) {
                    if (view.isSelected && currentTag == view.tag) {
                        view.mainImageView.setImageBitmap(videoThumbnail)
                    }
                }
            }
        }
    }

    private fun getVideoThumbnail(url: String): Bitmap? {
        val mediaMetadataRetriever = MediaMetadataRetriever()
        try {
            val map = HashMap<String, String>()
            mediaMetadataRetriever.setDataSource(url, map)
            return mediaMetadataRetriever.frameAtTime
        } catch (e: Exception) {
            e.printStackTrace()
        } finally {
            mediaMetadataRetriever.release()
        }
        return null
    }

    companion object {
        private const val TAG = "CardPresenter"
        private const val CARD_WIDTH = 313
        private const val CARD_HEIGHT = 176
    }
}