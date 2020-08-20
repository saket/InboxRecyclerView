package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * looks like a simple shared element transition.
 * @see <a href="https://github.com/saket/InboxRecyclerView/wiki/Item-animations#2-scale">Watch the examples</a>
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
        alpha = 1f
        scaleX = 1f
        scaleY = 1f
      }
      return
    }

    val anchorY = recyclerView.expandedItem.locationOnScreen.top
    val pageLocationOnScreen = page.locationOnScreen()
    val pageYBound = pageLocationOnScreen[1] - page.translationY
    val pageY = pageLocationOnScreen[1]

    val expandRatio = (anchorY - pageY) / (anchorY - pageYBound)
    val expandScale = 1f - (expandRatio * .10f)

    recyclerView.apply {
      alpha = 1f - (expandRatio * .70f)
      scaleX = expandScale
      scaleY = expandScale
    }

    // Fade in the anchor row with the expanding/collapsing page.
    anchorViewOverlay?.alpha = page.contentCoverAlpha
  }
}
