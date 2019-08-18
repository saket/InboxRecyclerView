package me.saket.inboxrecyclerview.dimming

import android.graphics.Canvas
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Draws a tint on the entire [InboxRecyclerView]. Unlike [UncoveredAreaTintPainter],
 * this requires the page to have its own background, but is a safer option because
 * it does not involve any coordinate calculations. Maths can be hard.
 */
class CompleteListTintPainter(
  private val color: Int,
  private val opacity: Float
) : UncoveredAreaTintPainter(color, opacity) {

  override fun createCallbacks(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) =
    object : StateChangeCallbacks(recyclerView, page, color, opacity) {
      override fun drawTint(canvas: Canvas) {
        recyclerView.apply {
          canvas.drawRect(0F, 0F, right.toFloat(), bottom.toFloat(), tintPaint)
        }
      }
    }
}
