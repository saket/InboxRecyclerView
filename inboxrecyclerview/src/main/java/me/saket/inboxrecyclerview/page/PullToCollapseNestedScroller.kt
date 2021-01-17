package me.saket.inboxrecyclerview.page

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import me.saket.inboxrecyclerview.Views
import me.saket.inboxrecyclerview.page.InterceptResult.INTERCEPTED
import java.util.ArrayList
import kotlin.math.abs

class PullToCollapseNestedScroller(private val page: ExpandablePageLayout) {

  /** Minimum Y-distance the page has to be dragged before it's eligible for collapse. */
  internal var collapseDistanceThreshold: Int = (Views.toolbarHeight(page.context))
  private var elasticScrollingFactor = 3.5f

  private val onPullListeners = ArrayList<OnExpandablePagePullListener>(3)

  private var isNestedScrolling = false
  private var interceptedUntilNextScroll = false
  private lateinit var lastTouchEvent: MotionEvent

  fun storeTouchEvent(ev: MotionEvent) {
    lastTouchEvent = ev
  }

  private fun onStartNestedScroll(dy: Int, type: Int) {
    if (type != TYPE_TOUCH) {
      return  // A fling can start after a scroll finishes.
    }

    if (page.isExpanded) {
      page.stopAnyOngoingAnimation()
    }

    interceptedUntilNextScroll = INTERCEPTED == page.handleOnPullToCollapseIntercept(
        event = lastTouchEvent,
        downX = lastTouchEvent.x,
        downY = lastTouchEvent.y,
        deltaUpwardSwipe = dy >= 0f // i.e., finger is moving from top to bottom.
    )

    if (!interceptedUntilNextScroll) {
      dispatchPullStartedCallback()
    }
  }

  /**
   * It is recommended to handle both onPreScroll() and onScroll(), but the
   * latter doesn't hide overscroll glow if a nested scroll was consumed.
   */
  fun onNestedPreScroll(target: View, dy: Int, consumed: IntArray, type: Int) {
    if (!isNestedScrolling) {
      onStartNestedScroll(dy, type)
      isNestedScrolling = true
    }

    if (interceptedUntilNextScroll) {
      return
    }

    // Ignore flings.
    if (type == TYPE_NON_TOUCH) {
      // Avoid letting the content fling if the page is
      // collapsing, preventing overscroll glows to show up.
      consumed[1] = if (page.isCollapsing) dy else 0
      return
    }

    val deltaDraggingDown = dy > 0f
    val canPageScroll = when {
      deltaDraggingDown -> page.translationY > 0f
      else -> page.translationY < 0f
    }
    val canContentScroll = {
      target.canScrollVertically(if (deltaDraggingDown) +1 else -1)
    }

    if (canPageScroll || !canContentScroll()) {
      movePageBy(dy)
      consumed[1] = dy
    }
  }

  private fun movePageBy(dy: Int) {
    var elasticDy = dy / elasticScrollingFactor
    if (page.isCollapseEligible) {
      val extraElasticity = collapseDistanceThreshold / (2F * abs(page.translationY))
      elasticDy *= extraElasticity
    }

    page.translationY += -elasticDy // dy is negative when the scroll is downwards.

    val draggingDown = page.translationY < 0f
    val deltaDraggingDown = dy < 0f
    dispatchPulledCallback(elasticDy, draggingDown, deltaDraggingDown)
  }

  fun onStopNestedScroll(type: Int) {
    if (type == TYPE_TOUCH && isNestedScrolling) {
      isNestedScrolling = false
      interceptedUntilNextScroll = false

      if (abs(page.translationY) > 0f) {
        // The page is responsible for animating back into position if the page
        // wasn't eligible for collapse. I no longer remember why I did this.
        dispatchReleaseCallback()
      }
    }
  }

// === Pull listeners. === //

  fun addOnPullListener(listener: OnExpandablePagePullListener) {
    onPullListeners.add(listener)
  }

  fun removeOnPullListener(listener: OnExpandablePagePullListener) {
    onPullListeners.remove(listener)
  }

  fun removeAllOnPullListeners() {
    onPullListeners.clear()
  }

  private fun dispatchReleaseCallback() {
    for (listener in onPullListeners) {
      listener.onRelease(page.isCollapseEligible)
    }
  }

  private fun dispatchPullStartedCallback() {
    for (listener in onPullListeners) {
      listener.onPullStarted()
    }
  }

  private fun dispatchPulledCallback(deltaY: Float, draggingDown: Boolean, deltaDraggingDown: Boolean) {
    for (i in onPullListeners.indices) {  // Avoids creating an iterator on each callback.
      val onPullListener = onPullListeners[i]
      onPullListener.onPull(
          deltaY = deltaY,
          currentTranslationY = page.translationY,
          upwardPull = draggingDown,
          deltaUpwardPull = deltaDraggingDown,
          collapseEligible = page.isCollapseEligible
      )
    }
  }
}
