package me.saket.inboxrecyclerview.page

import android.view.MotionEvent
import android.view.View
import androidx.core.view.ViewCompat.TYPE_NON_TOUCH
import androidx.core.view.ViewCompat.TYPE_TOUCH
import me.saket.inboxrecyclerview.Views
import me.saket.inboxrecyclerview.page.InterceptResult.INTERCEPTED
import java.util.ArrayList
import kotlin.math.abs

internal class PullToCollapseNestedScroller(private val page: ExpandablePageLayout) {

  /** Minimum Y-distance the page has to be dragged before it's eligible for collapse. */
  internal var collapseDistanceThreshold: Int = (Views.toolbarHeight(page.context))
  private var elasticScrollingFactor = 3.5f

  internal val isNestedScrolling get() = interceptedUntilNextScroll == false

  private val onPullListeners = ArrayList<OnExpandablePagePullListener>(3)
  private var interceptedUntilNextScroll: Boolean? = null
  private lateinit var actionDownEvent: MotionEvent

  fun storeTouchEvent(ev: MotionEvent) {
    if (ev.action == MotionEvent.ACTION_DOWN) {
      actionDownEvent = MotionEvent.obtainNoHistory(ev)
    }
  }

  fun onStartNestedScroll(dy: Int, type: Int) {
    check(type == TYPE_TOUCH)
    check(interceptedUntilNextScroll == null)

    if (page.isExpanded) {
      page.stopAnyOngoingAnimation()
    }

    interceptedUntilNextScroll = INTERCEPTED == page.handleOnPullToCollapseIntercept(
      event = actionDownEvent,
      downX = actionDownEvent.x,
      downY = actionDownEvent.y,
      deltaUpwardSwipe = dy >= 0f // i.e., finger is moving from top to bottom.
    )

    if (interceptedUntilNextScroll == false) {
      dispatchPullStartedCallback()
    }
  }

  /**
   * It is recommended to handle both onPreScroll() and onScroll(), but the
   * latter doesn't hide overscroll glow if a nested scroll was consumed.
   */
  fun onNestedPreScroll(target: View?, dy: Int, consumed: IntArray, type: Int) {
    // Ignore flings.
    if (type == TYPE_NON_TOUCH) {
      // Avoid letting the content fling if the page is
      // collapsing, preventing overscroll glows to show up.
      consumed[1] = if (page.isCollapsing) dy else 0
      return
    }

    if (interceptedUntilNextScroll == null) {
      // Note to self: this must happen only for scrolls (not flings) otherwise
      // isNestedScrolling will become true when a fling is started.
      onStartNestedScroll(dy, type)
    }

    if (interceptedUntilNextScroll == true) {
      return
    }

    val deltaDraggingDown = dy > 0f
    val canPageScroll = when {
      deltaDraggingDown -> page.translationY > 0f
      else -> page.translationY < 0f
    }
    val canContentScroll = {
      target != null && target.canScrollVertically(if (deltaDraggingDown) +1 else -1)
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
    if (type == TYPE_TOUCH) {
      if (isNestedScrolling && abs(page.translationY) > 0f) {
        // The page is responsible for animating back into position if the page
        // wasn't eligible for collapse. I no longer remember why I did this.
        dispatchReleaseCallback()
      }
      interceptedUntilNextScroll = null
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
