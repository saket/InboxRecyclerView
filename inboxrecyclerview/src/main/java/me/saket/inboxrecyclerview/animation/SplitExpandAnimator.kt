package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * When the page is expanding, this pushes all RecyclerView items out of the Window.
 * The expanding item is pushed to align with the top edge, while the items above it
 * are pushed out of the window towards the top and the rest towards the bottom.
 *
 * Vice versa when the page is collapsing.
 */
open class SplitExpandAnimator : ItemExpandAnimator() {

  override fun onPageMove(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    if (page.isCollapsed) {
      // Reset everything. This is also useful when the content size
      // changes, say as a result of the soft-keyboard getting dismissed.
      recyclerView.apply {
        for (childIndex in 0 until childCount) {
          val childView = getChildAt(childIndex)
          childView.translationY = 0F
          childView.alpha = 1F
        }
      }
      return
    }

    val anchorIndex = recyclerView.expandedItem.viewIndex
    val anchorView: View? = recyclerView.getChildAt(anchorIndex)
    val anchorViewLocation = recyclerView.expandedItem.locationOnScreen

    val pageLocationOnScreen = page.locationOnScreen()
    val pageTop = pageLocationOnScreen[1]
    val pageBottom = pageTop + page.clippedDimens.height()

    // Move the RecyclerView rows with the page.
    if (anchorView != null) {
      val distanceExpandedTowardsTop = pageTop - anchorViewLocation.top
      val distanceExpandedTowardsBottom = pageBottom - anchorViewLocation.bottom
      recyclerView.moveListItems(anchorIndex, distanceExpandedTowardsTop, distanceExpandedTowardsBottom)

    } else {
      // Anchor View can be null when the page was expanded from
      // an arbitrary location. See InboxRecyclerView#expandFromTop().
      recyclerView.moveListItems(anchorIndex, 0, pageBottom)
    }

    // Fade in the anchor row with the expanding/collapsing page.
    anchorView?.apply {
      val minPageHeight = anchorView.height
      val maxPageHeight = page.height
      val expandRatio = (page.clippedDimens.height() - minPageHeight).toFloat() / (maxPageHeight - minPageHeight)
      applyAlphaOnAnchorView(this, expandRatio)
    }
  }

  open fun applyAlphaOnAnchorView(anchorView: View, expandRatio: Float) {
    anchorView.alpha = 1F - expandRatio
  }

  open fun InboxRecyclerView.moveListItems(
    anchorIndex: Int,
    distanceExpandedTowardsTop: Int,
    distanceExpandedTowardsBottom: Int
  ) {
    for (childIndex in 0 until childCount) {
      getChildAt(childIndex).translationY = when {
        childIndex <= anchorIndex -> distanceExpandedTowardsTop.toFloat()
        else -> distanceExpandedTowardsBottom.toFloat()
      }
    }
  }
}
