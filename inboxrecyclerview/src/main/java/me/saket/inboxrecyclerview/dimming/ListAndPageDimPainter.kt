package me.saket.inboxrecyclerview.dimming

import androidx.core.graphics.ColorUtils
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSING
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDING

/**
 * Draws dimming over [InboxRecyclerView] while the page is expanding/collapsing.
 * The dimming is shifted to the page when it's pulled past the collapse threshold,
 * as a visual indication that the page can now be released.
 */
internal class ListAndPageDimPainter(
  private val listDim: Dim,
  private val pageDim: Dim?
) : DimPainter() {

  override fun onPageMove(rv: InboxRecyclerView, page: ExpandablePageLayout) {
    if (rv.dimDrawable == null) {
      rv.dimDrawable = AnimatedVisibilityColorDrawable(
        color = listDim.color.withAlpha(listDim.maxAlpha),
        animDuration = page.animationDurationMillis,
        onInvalidate = rv::invalidate
      )
    }
    if (page.dimDrawable == null && pageDim != null) {
      page.dimDrawable = AnimatedVisibilityColorDrawable(
        color = pageDim.color.withAlpha(pageDim.maxAlpha),
        animDuration = page.animationDurationMillis,
        onInvalidate = page::invalidate
      )
    }

    rv.dimDrawable!!.setShown(
      when (page.currentState) {
        COLLAPSING, COLLAPSED -> false
        EXPANDING -> true
        EXPANDED -> !page.isCollapseEligible
      }
    )

    if (pageDim != null) {
      page.dimDrawable!!.setShown(
        when (page.currentState) {
          COLLAPSING -> false
          COLLAPSED, EXPANDING -> false
          EXPANDED -> page.isCollapseEligible
        }
      )
    }
  }

  override fun cancelAnimation(
    rv: InboxRecyclerView,
    page: ExpandablePageLayout,
    resetDim: Boolean
  ) {
    rv.dimDrawable?.cancelAnimation(setAlphaTo = if (resetDim) 0 else null)
    page.dimDrawable?.cancelAnimation(setAlphaTo = if (resetDim) 0 else null)
  }
}

private fun Int.withAlpha(alpha: Int): Int {
  return ColorUtils.setAlphaComponent(this, alpha)
}
