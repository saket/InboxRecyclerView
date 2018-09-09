package me.saket.expand.dimming

import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import me.saket.expand.InboxRecyclerView

/**
 * Draws dimming on [InboxRecyclerView] rows while the page is expanded or expanding.
 */
abstract class ItemDimmer {

  abstract fun onAttachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun onDetachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun drawDimming(canvas: Canvas)

  companion object {

    @JvmOverloads
    fun uncoveredItems(
        @ColorInt dimColor: Int = Color.BLACK,
        @FloatRange(from = 0.0, to = 1.0) maxDimRatio: Float = 0.15F
    ): ItemDimmer {
      return UncoveredItemsDimmer(dimColor, maxDimRatio)
    }

    @JvmOverloads
    fun allItems(
        @ColorInt dimColor: Int = Color.BLACK,
        @FloatRange(from = 0.0, to = 1.0) maxDimRatio: Float = 0.15F
    ): ItemDimmer {
      return AllItemsDimmer(dimColor, maxDimRatio)
    }

    fun noOp(): ItemDimmer {
      return object : ItemDimmer() {
        override fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun onDetachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun drawDimming(canvas: Canvas) {}
      }
    }
  }
}
