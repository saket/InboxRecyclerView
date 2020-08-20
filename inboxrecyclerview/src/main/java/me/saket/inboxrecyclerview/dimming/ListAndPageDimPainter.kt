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
 * When [ExpandablePageLayout] is being pulled, the dimming is shifted to the page
 * if the pull distance is sufficient for collapsing the page. The shift acts as a
 * nice indicator that the page can now be released.
 */
internal class ListAndPageDimPainter(
  private val listDim: Dim,
  private val pageDim: Dim?
) : TintPainter() {

  override fun onPageMove(rv: InboxRecyclerView, page: ExpandablePageLayout) {
    if (rv.dimDrawable == null) {
      val animDuration = page.animationDurationMillis
      rv.dimDrawable = AnimatedColorDrawable(rv, listDim.color, animDuration)

      if (pageDim != null) {
        page.dimDrawable = AnimatedColorDrawable(page, pageDim.color, animDuration)
      }
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
    (rv.dimDrawable as? AnimatedColorDrawable)?.cancelAnimation()
    (page.dimDrawable as? AnimatedColorDrawable)?.cancelAnimation()
  }

  private val ExpandablePageLayout.isCollapseEligible
    get() = abs(translationY) >= pullToCollapseThresholdDistance
}
