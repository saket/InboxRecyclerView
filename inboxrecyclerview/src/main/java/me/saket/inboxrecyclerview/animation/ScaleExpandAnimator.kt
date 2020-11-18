package me.saket.inboxrecyclerview.animation

import android.graphics.Canvas
import android.view.View
import androidx.core.graphics.withScale
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * [https://www.youtube.com/watch?v=a0U8HcvT4G4]
 */
internal class ScaleExpandAnimator : ItemExpandAnimator() {
  private var unClippedScale: Float = 1f

  override fun onPageMove(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout,
    anchorViewOverlay: View?
  ) {
    if (page.isCollapsed) {
      // Reset everything. This is also useful when the content size
      // changes, say as a result of the soft-keyboard getting dismissed.
      unClippedScale = 1f
      recyclerView.invalidate()
      return
    }

    val anchorY = recyclerView.expandedItem.locationOnScreen.top
    val pageLocationOnScreen = page.locationOnScreen()
    val pageY = pageLocationOnScreen.top
    val pageTopYBound = pageY - page.translationY

    val expandRatio = (anchorY - pageY) / (anchorY - pageTopYBound)
    unClippedScale = 1f - (.10f * expandRatio.coerceIn(0.0f, 1.0f))
    recyclerView.invalidate()

    // Fade in the anchor row with the expanding/collapsing page.
    anchorViewOverlay?.alpha = 1f - page.expandRatio(recyclerView)
  }

  override fun transformRecyclerViewCanvas(
    recyclerView: InboxRecyclerView,
    canvas: Canvas,
    block: Canvas.() -> Unit
  ) {
    // Android clips canvas to the view's scaled bounds so a custom
    // scale property is used for drawing dimDrawable over full bounds.
    canvas.withScale(unClippedScale, unClippedScale, recyclerView.pivotX, recyclerView.pivotY) {
      block(canvas)
    }
  }
}
