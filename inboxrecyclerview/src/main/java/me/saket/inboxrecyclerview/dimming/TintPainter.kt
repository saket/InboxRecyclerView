package me.saket.inboxrecyclerview.dimming

import android.graphics.Canvas
import android.graphics.Color
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Draws a tint on [InboxRecyclerView] rows while it's covered by [ExpandablePageLayout].
 */
abstract class TintPainter {

  abstract fun onAttachRecyclerView(recyclerView: InboxRecyclerView, page: ExpandablePageLayout)

  abstract fun onDetachRecyclerView()

  abstract fun drawTint(canvas: Canvas)

  companion object {

    /**
     * See [UncoveredAreaTintPainter].
     */
    @JvmStatic
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
    @JvmStatic
    @JvmOverloads
    fun completeList(
      @ColorInt color: Int = Color.BLACK,
      @FloatRange(from = 0.0, to = 1.0) opacity: Float = 0.15F
    ): TintPainter {
      return CompleteListTintPainter(color, opacity)
    }

    @JvmStatic
    fun noOp(): TintPainter {
      return object : TintPainter() {
        override fun onAttachRecyclerView(
          recyclerView: InboxRecyclerView,
          page: ExpandablePageLayout
        ) = Unit

        override fun onDetachRecyclerView() = Unit

        override fun drawTint(canvas: Canvas) = Unit
      }
    }
  }
}
