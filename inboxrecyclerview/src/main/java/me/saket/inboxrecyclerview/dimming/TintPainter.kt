package me.saket.inboxrecyclerview.dimming

import android.graphics.Color.BLACK
import androidx.annotation.ColorInt
import androidx.annotation.FloatRange
import androidx.annotation.IntRange
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.animation.PageLocationChangeDetector
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * TODO: rename to DimPainter.
 * Draws a tint on [InboxRecyclerView] rows while it's covered by [ExpandablePageLayout].
 */
abstract class TintPainter {
  private lateinit var onDetach: () -> Unit

  fun onAttachRecyclerView(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout
  ) {
    val changeDetector = PageLocationChangeDetector(page) {
      onPageMove(recyclerView, page)
    }

    page.viewTreeObserver.addOnGlobalLayoutListener(changeDetector)
    page.viewTreeObserver.addOnPreDrawListener(changeDetector)

    onDetach = {
      cancelAnimation(recyclerView, page)
      page.viewTreeObserver.removeOnGlobalLayoutListener(changeDetector)
      page.viewTreeObserver.removeOnPreDrawListener(changeDetector)
    }
  }

  fun onDetachRecyclerView() {
    onDetach()
  }

  abstract fun cancelAnimation(
    rv: InboxRecyclerView,
    page: ExpandablePageLayout
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
    ): TintPainter =
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
    ): TintPainter = listAndPage(color, alpha, color, alpha)

    /** See [ListAndPageDimPainter]. */
    @JvmStatic
    @JvmOverloads
    fun listOnly(
      @ColorInt color: Int = BLACK,
      @FloatRange(from = 0.0, to = 1.0) alpha: Float = 0.15F
    ): TintPainter = ListAndPageDimPainter(
        listDim = Dim(color, (alpha * 255).toInt()),
        pageDim = null
    )

    @JvmStatic
    @JvmOverloads
    @Deprecated(
        "Use listAndPage() instead",
        ReplaceWith("TintPainter.listAndPage(color, opacity)")
    )
    fun uncoveredArea(color: Int = BLACK, opacity: Float = 0.15F) =
      listAndPage(color, opacity)

    @JvmStatic
    @JvmOverloads
    @Deprecated(
        "No longer supported. Use listAndPage() instead",
        ReplaceWith("TintPainter.listAndPage(color, opacity)")
    )
    fun completeList(color: Int = BLACK, opacity: Float = 0.15F) =
      listAndPage(color, opacity)

    @JvmStatic
    fun none(): TintPainter {
      return object : TintPainter() {
        override fun cancelAnimation(
          rv: InboxRecyclerView,
          page: ExpandablePageLayout
        ) = Unit

        override fun onPageMove(rv: InboxRecyclerView, page: ExpandablePageLayout) = Unit
      }
    }

    @JvmStatic
    @Deprecated("Use none() instead", ReplaceWith("TintPainter.none()"))
    fun noOp(): TintPainter {
      return none()
    }
  }
}
