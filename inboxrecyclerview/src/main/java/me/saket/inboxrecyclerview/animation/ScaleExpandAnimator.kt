package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * [https://github.com/saket/InboxRecyclerView/tree/master/images/animators/animator_scale.mp4]
 */
internal class ScaleExpandAnimator : ItemExpandAnimator() {

  override fun onPageMove(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout,
    anchorViewOverlay: View?
  ) {
    if (page.isCollapsed) {
      // Reset everything. This is also useful when the content size
      // changes, say as a result of the soft-keyboard getting dismissed.
      recyclerView.apply {
        unClippedScale = 1f
      }
      return
    }

    val anchorY = recyclerView.expandedItem.locationOnScreen.top
    val pageLocationOnScreen = page.locationOnScreen()
    val pageYBound = pageLocationOnScreen[1] - page.translationY
    val pageY = pageLocationOnScreen[1]

    val expandRatio = (anchorY - pageY) / (anchorY - pageYBound)
    recyclerView.unClippedScale = 1f - (expandRatio * .10f)

    // Fade in the anchor row with the expanding/collapsing page.
    anchorViewOverlay?.alpha = page.contentCoverAlpha
  }
}
