package me.saket.inboxrecyclerview.animation

import android.view.View

/**
 * When the page is expanding, this pushes all RecyclerView items out of the Window.
 * The expanding item is pushed to align with the top edge, while the items above it
 * are pushed out of the window towards the top and the rest towards the bottom.
 *
 * Vice versa when the page is collapsing.
 */
open class SplitExpandAnimator : ItemExpandAnimator() {

  override fun onPageMove() {
    val page = recyclerView.page
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

    val (anchorIndex) = recyclerView.expandedItem
    val anchorView: View? = recyclerView.getChildAt(anchorIndex)

    val pageTop = page.translationY
    val pageBottom = page.translationY + page.clippedDimens.height()

    // Move the RecyclerView rows with the page.
    if (anchorView != null) {
      val distanceExpandedTowardsTop = pageTop - anchorView.top
      val distanceExpandedTowardsBottom = pageBottom - anchorView.bottom
      moveListItems(anchorIndex, distanceExpandedTowardsTop, distanceExpandedTowardsBottom)

    } else {
      // Anchor View can be null when the page was expanded from
      // an arbitrary location. See InboxRecyclerView#expandFromTop().
      moveListItems(anchorIndex, 0F, pageBottom)
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

  open fun moveListItems(anchorIndex: Int, distanceExpandedTowardsTop: Float, distanceExpandedTowardsBottom: Float) {
    recyclerView.apply {
      for (childIndex in 0 until childCount) {
        getChildAt(childIndex).translationY = when {
          childIndex <= anchorIndex -> distanceExpandedTowardsTop
          else -> distanceExpandedTowardsBottom
        }
      }
    }
  }
}
