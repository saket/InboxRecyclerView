package me.saket.inboxrecyclerview.page

import android.graphics.Rect
import android.view.GestureDetector
import android.view.GestureDetector.SimpleOnGestureListener
import android.view.MotionEvent
import android.view.MotionEvent.ACTION_CANCEL
import android.view.MotionEvent.ACTION_DOWN
import android.view.MotionEvent.ACTION_UP
import android.view.View
import android.view.ViewGroup
import androidx.core.view.ViewCompat.TYPE_TOUCH
import androidx.core.view.children
import kotlin.math.abs

/**
 * Manually forwards events to [PullToCollapseNestedScroller] when [ExpandablePageLayout]'s
 * content isn't nested-scrollable.
 */
internal class PullToCollapseTouchListener(
  val page: ExpandablePageLayout,
  val nestedScroller: PullToCollapseNestedScroller
) : SimpleOnGestureListener() {

  private var canNestedScroll = true
  private val fooArray = intArrayOf(0, 0)

  private val touchDetector = GestureDetector(page.context, object : SimpleOnGestureListener() {
    override fun onScroll(e1: MotionEvent, e2: MotionEvent, distanceX: Float, distanceY: Float): Boolean {
      nestedScroller.onNestedPreScroll(
          target = null,
          dy = distanceY.toInt(),
          consumed = fooArray,
          type = TYPE_TOUCH
      )
      return nestedScroller.isNestedScrolling || abs(distanceY) > abs(distanceX)
    }
  })

  fun onInterceptTouch(event: MotionEvent): Boolean {
    if (event.action == ACTION_DOWN) {
      canNestedScroll = page.findChildUnderTouch(event)?.isNestedScrollingEnabled == true
    }
    return if (canNestedScroll) false else touchDetector.onTouchEvent(event)
  }

  fun onTouch(event: MotionEvent): Boolean {
    touchDetector.onTouchEvent(event)
    if (event.action == ACTION_UP || event.action == ACTION_CANCEL) {
      nestedScroller.onStopNestedScroll(TYPE_TOUCH)
    }
    return true
  }

  private fun ViewGroup.findChildUnderTouch(
    e: MotionEvent,
    hitRect: Rect = Rect(),
  ): View? {
    for (child in children) {
      child.getHitRect(hitRect)
      hitRect.offset(this.left, this.top)
      val contains = hitRect.contains(e.x.toInt() + page.scrollX, e.y.toInt() + page.scrollY)

      if (contains) {
        if (child.isNestedScrollingEnabled) {
          return child
        }
        (child as? ViewGroup)?.findChildUnderTouch(e, hitRect)?.let {
          return it
        }
        return child
      }
    }
    return null
  }
}
