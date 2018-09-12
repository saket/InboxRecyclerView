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
abstract class TintPainter {

  abstract fun onAttachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun onDetachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun drawTint(canvas: Canvas)

  companion object {

    /**
     * See [UncoveredAreaTintPainter].
     */
    @JvmOverloads
    fun uncoveredArea(
        @ColorInt color: Int = Color.BLACK,
        @FloatRange(from = 0.0, to = 1.0) opacity: Float = 0.15F
    ): TintPainter {
      return UncoveredAreaTintPainter(color, opacity)
    }

    /**
     * See [CompleteListTintPainter].
     */
    @JvmOverloads
    fun completeList(
        @ColorInt color: Int = Color.BLACK,
        @FloatRange(from = 0.0, to = 1.0) opacity: Float = 0.15F
    ): TintPainter {
      return CompleteListTintPainter(color, opacity)
    }

    fun noOp(): TintPainter {
      return object : TintPainter() {
        override fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun onDetachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun drawTint(canvas: Canvas) {}
      }
    }
  }
}
