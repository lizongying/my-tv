package com.lizongying.mytv

import android.content.Context
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.View
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView

class GrayOverlayItemDecoration(private val context: Context) : RecyclerView.ItemDecoration() {

    private val grayOverlayPaint = Paint().apply {
        color = ContextCompat.getColor(context, R.color.gray_overlay)
        style = Paint.Style.FILL
    }

    override fun onDrawOver(c: Canvas, parent: RecyclerView, state: RecyclerView.State) {
        super.onDrawOver(c, parent, state)

        val childCount = parent.childCount
        for (i in 0 until childCount) {
            val child = parent.getChildAt(i)
            if (!child.hasFocus()) {
                // 计算遮罩层的大小
                val overlayRect = Rect(
                    child.left,
                    child.top,
                    child.right,
                    child.bottom
                )
                // 绘制灰色遮罩层
                c.drawRect(overlayRect, grayOverlayPaint)
            }
        }
    }

    override fun getItemOffsets(
        outRect: Rect,
        view: View,
        parent: RecyclerView,
        state: RecyclerView.State
    ) {
        super.getItemOffsets(outRect, view, parent, state)
        // 在此处设置偏移量为0，以防止遮罩层影响项的布局
        outRect.setEmpty()
    }
}