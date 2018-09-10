package me.saket.expand.dimming

import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import me.saket.expand.InboxRecyclerView
import me.saket.expand.page.ExpandablePageLayout

/**
 * Draws a tint on [InboxRecyclerView] rows while it's covered by [ExpandablePageLayout].
 */
abstract class ItemTintPainter {

  abstract fun onAttachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun onDetachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun drawTint(canvas: Canvas)

  companion object {

    @JvmOverloads
    fun uncoveredItems(
        @ColorInt color: Int = Color.BLACK,
        @FloatRange(from = 0.0, to = 1.0) intensity: Float = 0.15F
    ): ItemTintPainter {
      return UncoveredItemsTintPainter(color, intensity)
    }

    @JvmOverloads
    fun allItems(
        @ColorInt color: Int = Color.BLACK,
        @FloatRange(from = 0.0, to = 1.0) intensity: Float = 0.15F
    ): ItemTintPainter {
      return AllItemsTintPainter(color, intensity)
    }

    fun noOp(): ItemTintPainter {
      return object : ItemTintPainter() {
        override fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun onDetachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun drawTint(canvas: Canvas) {}
      }
    }
  }
}
