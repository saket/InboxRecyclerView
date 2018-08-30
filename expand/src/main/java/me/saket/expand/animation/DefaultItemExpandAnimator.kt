package me.saket.expand.animation

/**
 * When the page is expanding, this pushes all RecyclerView items out of the Window.
 * The expanding item is pushed to align with the top edge, while the items above it
 * are pushed out of the window towards the top and the rest towards the bottom.
 *
 * Vice versa when the page is collapsing.
 *
 * TODO: Find a better name.
 */
class DefaultItemExpandAnimator : ItemExpandAnimator() {

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
    val anchorView = recyclerView.getChildAt(anchorIndex)

    val pageTop = page.translationY
    val pageBottom = page.translationY + page.clippedRect.height()

    val distanceExpandedTowardsTop = pageTop - anchorView.top
    val distanceExpandedTowardsBottom = pageBottom - anchorView.bottom

    // Move the RecyclerView rows with the page.
    recyclerView.apply {
      for (childIndex in 0 until childCount) {
        val childView = getChildAt(childIndex)

        if (anchorView == null) {
          // Anchor View can be null when the page was expanded from
          // an arbitrary location. See InboxRecyclerView#expandFromTop().
          childView.translationY = pageBottom

        } else {
          childView.translationY = when {
            childIndex <= anchorIndex -> distanceExpandedTowardsTop
            else -> distanceExpandedTowardsBottom
          }
        }
      }
    }

    // Fade in the anchor row with the expanding/collapsing page.
    val minPageHeight = anchorView.height
    val maxPageHeight = page.height
    val expandRatio = (page.clippedRect.height() - minPageHeight).toFloat() / (maxPageHeight - minPageHeight)
    anchorView.alpha = 1F - expandRatio
  }
}
