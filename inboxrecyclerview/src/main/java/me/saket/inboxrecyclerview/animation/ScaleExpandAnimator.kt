package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Commented lines would allow to add scaling animation.
 */
class ScaleExpandAnimator : ItemExpandAnimator() {

  override fun onPageMove(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    if (page.isCollapsed) {
      recyclerView.apply {
        for (childIndex in 0 until childCount) {
          val childView = getChildAt(childIndex)
          //childView.scaleX = 1F
          //childView.scaleY = 1F
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

    // Move the RecyclerView rows with the page.
    if (anchorView != null) {
      val distanceExpandedTowardsTop = pageTop - anchorViewLocation.top
      val distanceExpandedTowardsCorner = pageLocationOnScreen[0] - anchorView.left
      anchorView.apply {
        val minPageHeight = anchorView.height
        val maxPageHeight = page.height
        val expandRatio = (page.clippedDimens.height() - minPageHeight).toFloat() / (maxPageHeight - minPageHeight)
        //val scale = 1f - expandRatio / 20
        //recyclerView.scaleListItems(anchorIndex, scale)
        alpha = 1F - expandRatio
        translationY = distanceExpandedTowardsTop.toFloat()
        translationX = distanceExpandedTowardsCorner.toFloat()
      }
    } else {
      recyclerView.moveListItems(anchorIndex, 0, pageBottom)
    }
  }

  /*private fun InboxRecyclerView.scaleListItems(
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
  }*/

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
