package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * @param scaling whether to scale items or not, if set to false this animator actually
 * looks like a simple shared element transition.
 */
class ScaleExpandAnimator(private val scaling: Boolean = true) : ItemExpandAnimator() {

  override fun onPageMove(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    if (page.isCollapsed) {
      recyclerView.apply {
        for (childIndex in 0 until childCount) {
          val childView = getChildAt(childIndex)
          childView.scaleX = 1F
          childView.scaleY = 1F
          childView.alpha = 1F
          childView.translationY = 0F
          childView.translationX = 0F
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

    if (anchorView != null) {
      val distanceExpandedTowardsTop = pageTop - anchorViewLocation.top
      val distanceExpandedTowardsCorner = pageLocationOnScreen[0] - anchorView.left
      anchorView.apply {
        val minPageHeight = anchorView.height
        val maxPageHeight = page.height
        val expandRatio = (page.clippedDimens.height() - minPageHeight).toFloat() / (maxPageHeight - minPageHeight)
        if (scaling) {
          // 1 to 0.90
          val scale = 1f - (expandRatio * .10f)
          recyclerView.scaleListItems(anchorIndex, scale)
        }
        alpha = 1F - expandRatio
        translationY = distanceExpandedTowardsTop.toFloat()
        // Just in case we're not using a LinearLayoutManager
        translationX = distanceExpandedTowardsCorner.toFloat()
      }
    } else {
      // Anchor View can be null when the page was expanded from
      // an arbitrary location. See InboxRecyclerView#expandFromTop().
      recyclerView.moveListItems(anchorIndex, 0, pageBottom)
    }
  }

  private fun InboxRecyclerView.scaleListItems(
    anchorIndex: Int,
    scale: Float
  ) {
    for (childIndex in 0 until childCount) {
      if (childIndex != anchorIndex) {
        val child = getChildAt(childIndex)
        child.scaleX = scale
        child.scaleY = scale
      }
    }
  }

  private fun InboxRecyclerView.moveListItems(
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