package me.saket.inboxrecyclerview.page

import android.view.MotionEvent
import android.view.View
import android.widget.RelativeLayout
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import me.saket.inboxrecyclerview.Views
import me.saket.inboxrecyclerview.page.InterceptResult.INTERCEPTED
import me.saket.inboxrecyclerview.page.PullToCollapseListener.OnPullListener
import java.util.ArrayList
import kotlin.math.abs

class PullToCollapseNestedScroller(private val page: ExpandablePageLayout) {

  /** Minimum Y-distance the page has to be dragged before it's eligible for collapse. */
  var collapseDistanceThreshold: Int = (Views.toolbarHeight(page.context))
  var elasticScrollingFactor = 3.5f

  private val onPullListeners = ArrayList<OnPullListener>(3)
  private var isPageMoving = false

  private lateinit var lastTouchEvent: MotionEvent
  private var interceptedUntilNextScroll: Boolean? = null

  fun storeTouchEvent(ev: MotionEvent) {
    lastTouchEvent = ev
  }

  fun onStartNestedScroll(axes: Int, type: Int): Boolean {
    // Accept all nested scroll events from the child. The decision of whether
    // or not to actually scroll is calculated inside onNestedPreScroll().

    if (type == TYPE_TOUCH) { // A fling can start after a scroll finishes.
      if (page.isExpanded) {
        page.stopAnyOngoingAnimation()
      }
      dispatchPullStartedCallback()
    }
    return axes and RelativeLayout.SCROLL_AXIS_VERTICAL != 0
  }

  fun onNestedPreScroll(target: View, dy: Int, consumed: IntArray, type: Int) {
    val downwardScroll = (page.translationY - dy) < 0f
    val deltaDownwardScroll = dy < 0f // i.e., finger is moving from top to bottom.

    if (type == TYPE_NON_TOUCH) {
      // Ignore flings.
      return
    }
    if (isPageMoving.not() && interceptedUntilNextScroll == null) {
      val interceptResult = page.handleOnPullToCollapseIntercept(
          event = lastTouchEvent,
          downX = lastTouchEvent.x,
          downY = lastTouchEvent.y,
          deltaUpwardSwipe = !deltaDownwardScroll
      )
      interceptedUntilNextScroll = interceptResult == INTERCEPTED
    }
    if (interceptedUntilNextScroll == true) {
      return
    }

    val canPageScroll = when {
      deltaDownwardScroll -> page.translationY < 0f
      else -> page.translationY > 0f
    }
    val canContentScroll = {
      target.canScrollVertically(if (deltaDownwardScroll) -1 else +1)
    }

    if (canPageScroll || !canContentScroll()) {
      consumed[1] = dy
      isPageMoving = true
    }

    // The page isn't moved with the same speed as the finger. Some elasticity is applied
    // to make the gesture feel nice. This elasticity is increased further once the page
    // is eligible for collapse as a visual indicator that the page can now be released.
    var elasticDy = consumed[1] / elasticScrollingFactor
    if (page.isCollapseEligible) {
      val extraElasticity = collapseDistanceThreshold / (2F * abs(page.translationY))
      elasticDy *= extraElasticity
    }

    page.translationY += -elasticDy // dy is negative when the scroll is downwards.

    if (abs(consumed[1]) > 0f) {
      dispatchPulledCallback(elasticDy, downwardScroll, deltaDownwardScroll)
    }
  }

  fun onStopNestedScroll(type: Int) {
    if (type == TYPE_TOUCH) {
      interceptedUntilNextScroll = null
      isPageMoving = false

      if (abs(page.translationY) > 0f) {
        // The page is responsible for animating back into position if the page
        // wasn't eligible for collapse. I no longer remember why I did this.
        dispatchReleaseCallback()
      }
    }
  }

// === Pull listeners. === //

  fun addOnPullListener(listener: OnPullListener) {
    onPullListeners.add(listener)
  }

  fun removeOnPullListener(listener: OnPullListener) {
    onPullListeners.remove(listener)
  }

  fun removeAllOnPullListeners() {
    onPullListeners.clear()
  }

  private fun dispatchReleaseCallback() {
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onRelease(page.isCollapseEligible)
    }
  }

  private fun dispatchPullStartedCallback() {
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onPullStarted()
    }
  }

  private fun dispatchPulledCallback(deltaY: Float, upwardPull: Boolean, deltaUpwardPull: Boolean) {
    val translationY = page.translationY
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onPull(deltaY, translationY, upwardPull, deltaUpwardPull, page.isCollapseEligible)
    }
  }
}
