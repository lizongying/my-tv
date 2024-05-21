package com.lizongying.mytv

import android.content.Context
import android.graphics.Rect
import android.view.View
import androidx.recyclerview.widget.RecyclerView

class ItemDecoration(context: Context) : RecyclerView.ItemDecoration() {

    private var application: MyTVApplication

    init {
        application = context.applicationContext as MyTVApplication
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        val space = application.dp2Px(5)
        outRect.top = space
        outRect.bottom = space
        outRect.left = space
        outRect.right = space
    }
}