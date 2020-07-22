package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * @param scaling whether to scale items or not, if set to false this animator actually
 * looks like a simple shared element transition.
 * @see <a href="https://imgur.com/a/mG0S94t">Watch the examples</a>
 */
class ScaleExpandAnimator(private val scaleBackground: Boolean = true) : ItemExpandAnimator() {

  override fun onPageMove(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    val anchorIndex = recyclerView.expandedItem.viewIndex
    val anchorView: View? = recyclerView.getChildAt(anchorIndex)

    if (page.isCollapsed) {
      anchorView?.apply {
        translationX = 0f
        translationY = 0f
        alpha = 1f
      }
      return
    }

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
        alpha = 1F - expandRatio
        translationY = distanceExpandedTowardsTop.toFloat()
        // Just in case we're not using a LinearLayoutManager
        translationX = distanceExpandedTowardsCorner.toFloat()
        if (scaleBackground) {
          // 1 to 0.95
          val expandedScale = 1f - (expandRatio * .10f)
          val expandedAlpha = 1f - (expandRatio * .70f)
          recyclerView.apply {
            scaleX = expandedScale
            scaleY = expandedScale
            alpha = expandedAlpha
          }
        }
      }
    } else {
      // Anchor View can be null when the page was expanded from
      // an arbitrary location. See InboxRecyclerView#expandFromTop().
      recyclerView.moveListItems(anchorIndex, 0, pageBottom)
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