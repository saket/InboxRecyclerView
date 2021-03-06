package me.saket.inboxrecyclerview.dimming

import android.graphics.Color.BLACK
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.animation.PageLocationChangeDetector
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Draws dimming on [InboxRecyclerView] rows when they're covered by [ExpandablePageLayout].
 * See [listAndPage].
 */
abstract class DimPainter {
  private var onDetach: ((resetDim: Boolean) -> Unit)? = null

  fun onAttachRecyclerView(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout
  ) {
    val changeDetector = PageLocationChangeDetector(page) {
      onPageMove(recyclerView, page)
    }

    changeDetector.start()
    onDetach = { resetDim ->
      cancelAnimation(recyclerView, page, resetDim)
      changeDetector.stop()
    }
  }

  fun onDetachRecyclerView(resetDim: Boolean) {
    onDetach?.invoke(/* resetDim = */ resetDim )
  }

  abstract fun cancelAnimation(
    rv: InboxRecyclerView,
    page: ExpandablePageLayout,
    resetDim: Boolean
  )

  abstract fun onPageMove(
    rv: InboxRecyclerView,
    page: ExpandablePageLayout
  )

  internal data class Dim(
    @ColorInt val color: Int,
    @IntRange(from = 0, to = 255) val maxAlpha: Int
  )

  companion object {

    /** See [ListAndPageDimPainter]. */
    @JvmStatic
    fun listAndPage(
      @ColorInt listColor: Int = BLACK,
      @FloatRange(from = 0.0, to = 1.0) listAlpha: Float = 0.15F,
      @ColorInt pageColor: Int = BLACK,
      @FloatRange(from = 0.0, to = 1.0) pageAlpha: Float = 0.15F
    ): DimPainter =
      ListAndPageDimPainter(
          listDim = Dim(listColor, (listAlpha * 255).toInt()),
          pageDim = Dim(pageColor, (pageAlpha * 255).toInt())
      )

    /** See [ListAndPageDimPainter]. */
    @JvmStatic
    @JvmOverloads
    fun listAndPage(
      @ColorInt color: Int = BLACK,
      @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.15F
    ): DimPainter = listAndPage(color, alpha, color, alpha)

    /** See [ListAndPageDimPainter]. */
    @JvmStatic
    @JvmOverloads
    fun listOnly(
      @ColorInt color: Int = BLACK,
      @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.15F
    ): DimPainter = ListAndPageDimPainter(
        listDim = Dim(color, (alpha * 255).toInt()),
        pageDim = null
    )

    @JvmStatic
    fun none(): DimPainter {
      return object : DimPainter() {
        override fun cancelAnimation(
          rv: InboxRecyclerView,
          page: ExpandablePageLayout,
          resetDim: Boolean
        ) = Unit

        override fun onPageMove(rv: InboxRecyclerView, page: ExpandablePageLayout) = Unit
      }
    }
  }
}
