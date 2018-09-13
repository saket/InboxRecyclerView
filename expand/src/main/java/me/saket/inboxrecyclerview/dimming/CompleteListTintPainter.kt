package me.saket.inboxrecyclerview.dimming

import android.graphics.Canvas
import me.saket.inboxrecyclerview.InboxRecyclerView

/**
 * Draws a tint on the entire [InboxRecyclerView]. Unlike [UncoveredAreaTintPainter],
 * this requires the page to have its own background, but is a safer option because
 * it does not involve any coordinate calculations. Maths is hard.
 */
class CompleteListTintPainter(color: Int, opacity: Float) : UncoveredAreaTintPainter(color, opacity) {

  override fun drawTint(canvas: Canvas) {
    recyclerView.apply {
      canvas.drawRect(0F, 0F, right.toFloat(), bottom.toFloat(), tintPaint)
    }
  }
}
