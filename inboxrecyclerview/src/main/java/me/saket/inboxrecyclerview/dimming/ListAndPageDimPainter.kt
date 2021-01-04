package me.saket.inboxrecyclerview.dimming

import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSING
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDING
import kotlin.math.abs

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
      rv.dimDrawable = AnimatedColorDrawable(rv, listDim.color, page.animationDurationMillis)
    }
    if (page.dimDrawable == null && pageDim != null) {
      page.dimDrawable = AnimatedColorDrawable(page, pageDim.color, page.animationDurationMillis)
    }

    rv.dimDrawable!!.alpha = when (page.currentState) {
      COLLAPSING, COLLAPSED -> 0
      EXPANDING -> listDim.maxAlpha
      EXPANDED -> if (page.isCollapseEligible) 0 else listDim.maxAlpha
    }

    if (pageDim != null) {
      page.dimDrawable!!.alpha = when (page.currentState) {
        COLLAPSING -> 0
        COLLAPSED, EXPANDING -> 0
        EXPANDED -> if (page.isCollapseEligible) pageDim.maxAlpha else 0
      }
    }
  }

  override fun cancelAnimation(
    rv: InboxRecyclerView,
    page: ExpandablePageLayout
  ) {
    (rv.dimDrawable as? AnimatedColorDrawable)?.let {
      it.alpha = 0
      it.cancelAnimation(jumpToOngoingAlpha = true)
    }
    (page.dimDrawable as? AnimatedColorDrawable)?.let {
      it.alpha = 0
      it.cancelAnimation(jumpToOngoingAlpha = true)
    }
  }
}
