package me.saket.inboxrecyclerview.page

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Rect
import android.view.MotionEvent
import android.view.ViewConfiguration
import me.saket.inboxrecyclerview.Views
import me.saket.inboxrecyclerview.page.InterceptResult.INTERCEPTED
import java.util.ArrayList
import kotlin.math.abs

class PullToCollapseListener(private val expandablePage: ExpandablePageLayout) {

  @Deprecated(message = "Passing context is no longer needed")
  constructor(context: Context, expandablePage: ExpandablePageLayout) : this(expandablePage)

  /** Minimum Y-distance the page has to be pulled before it's eligible for collapse. */
  var collapseDistanceThreshold: Int = (Views.toolbarHeight(expandablePage.context))

  private val touchSlop: Int = ViewConfiguration.get(expandablePage.context).scaledTouchSlop
  private val onPullListeners = ArrayList<OnPullListener>(3)
  private var downX: Float = 0f
  private var downY: Float = 0f
  private var lastMoveY: Float = 0f
  private var horizontalSwipingConfirmed: Boolean? = null
  private var interceptedUntilNextGesture: Boolean? = null
  var pullFrictionFactor = 3.5f

  /**
   * Letting the user pull down the page while it's expanding results in a very fluid
   * experience because they aren't forced to wait for the expansion animation to finish
   * in case they change their minds and want to go back immediately.
   *
   * An unfortunate drawback of this is that the user can stop the animation midway by
   * holding the page and be able to move it around.
   * */
  var allowPullDuringExpansion = false

  interface OnPullListener {
    /**
     * Called when a pull starts.
     */
    fun onPullStarted() = Unit

    /**
     * Called when the user is pulling down / up the expandable page or the list.
     *
     * @param deltaY              Delta translation-Y since the last onPull call.
     * @param currentTranslationY Current translation-Y of the page.
     * @param upwardPull          Whether the page is being pulled in the upward direction.
     * @param deltaUpwardPull     Whether the last delta-pull was made in the upward direction.
     * @param collapseEligible    Whether the pull distance was enough to trigger a collapse.
     */
    fun onPull(
        deltaY: Float,
        currentTranslationY: Float,
        upwardPull: Boolean,
        deltaUpwardPull: Boolean,
        collapseEligible: Boolean
    )

    /**
     * Called when the user's finger is lifted.
     *
     * @param collapseEligible Whether or not the pull distance was enough to trigger a collapse.
     */
    fun onRelease(collapseEligible: Boolean)
  }

  fun addOnPullListener(listener: OnPullListener) {
    onPullListeners.add(listener)
  }

  fun removeOnPullListener(listener: OnPullListener) {
    onPullListeners.remove(listener)
  }

  fun removeAllOnPullListeners() {
    onPullListeners.clear()
  }

  @SuppressLint("ClickableViewAccessibility")
  fun onTouch(event: MotionEvent, consumeDowns: Boolean): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.rawX
        downY = event.rawY
        lastMoveY = downY

        horizontalSwipingConfirmed = null
        interceptedUntilNextGesture = null
        return consumeDowns
      }

      MotionEvent.ACTION_MOVE -> {
        if (horizontalSwipingConfirmed == true) return false
        if (interceptedUntilNextGesture == true) return false

        val deltaY = event.rawY - lastMoveY
        val totalSwipeDistanceX = event.rawX - downX
        val totalSwipeDistanceY = event.rawY - downY
        val totalSwipeDistanceXAbs = abs(totalSwipeDistanceX)
        val totalSwipeDistanceYAbs = abs(totalSwipeDistanceY)

        // Ignore horizontal swipes.
        if (horizontalSwipingConfirmed == null) {
          horizontalSwipingConfirmed = totalSwipeDistanceXAbs > totalSwipeDistanceYAbs

          // Any ongoing animation must also be canceled at the starting of a gesture.
          if (expandablePage.isExpanded) {
            cancelAnyOngoingAnimations()
          }

          if (horizontalSwipingConfirmed!!) {
            return false
          }
        }

        val deltaUpwardSwipe = deltaY < 0

        // Offer this event to interceptors. Content is allowed to
        // scroll even while the page is expanding for a fluid experience.
        if (interceptedUntilNextGesture == null) {
          val interceptResult = expandablePage.handleOnPullToCollapseIntercept(event, downX, downY, deltaUpwardSwipe)
          interceptedUntilNextGesture = interceptResult == INTERCEPTED
          if (interceptedUntilNextGesture!!) {
            return false
          }
          dispatchPullStartedCallback()
        }
        expandablePage.parent.requestDisallowInterceptTouchEvent(true)

        if (allowPullDuringExpansion
            && expandablePage.isExpanding
            && event in expandablePage.clippedDimens
            && abs(totalSwipeDistanceY) > 0f) {
          // Page swiped during an expansion. The user may want to go back. Don't make
          // them wait until the animation is over and giving a chance to swipe back immediately.
          cancelAnyOngoingAnimations()

        } else if (totalSwipeDistanceYAbs < touchSlop && totalSwipeDistanceXAbs < touchSlop) {
          return false
        }

        val upwardSwipe = totalSwipeDistanceY < 0F

        // The page isn't moved with the same speed as the finger. Some friction is applied
        // to make the gesture feel nice. This friction is increased further once the page
        // is eligible for collapse as a visual indicator that the page can now be released.
        var deltaYWithFriction = deltaY / pullFrictionFactor

        if (expandablePage.isCollapseEligible) {
          val extraFriction = collapseDistanceThreshold / (2F * abs(expandablePage.translationY))
          deltaYWithFriction *= extraFriction
        }

        val translationY = expandablePage.translationY + deltaYWithFriction
        expandablePage.translationY = translationY
        dispatchPulledCallback(deltaYWithFriction, upwardSwipe, deltaUpwardSwipe)

        lastMoveY = event.rawY
        return true
      }

      MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
        // The page is responsible for animating back into position if the page
        // wasn't eligible for collapse. I no longer remember why I did this.
        dispatchReleaseCallback()
      }
    }

    return false
  }

  private fun dispatchReleaseCallback() {
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onRelease(expandablePage.isCollapseEligible)
    }
  }

  private fun dispatchPullStartedCallback() {
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onPullStarted()
    }
  }

  private fun dispatchPulledCallback(deltaY: Float, upwardPull: Boolean, deltaUpwardPull: Boolean) {
    val translationY = expandablePage.translationY
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onPull(deltaY, translationY, upwardPull, deltaUpwardPull, expandablePage.isCollapseEligible)
    }
  }

  private fun cancelAnyOngoingAnimations() {
    expandablePage.stopAnyOngoingAnimation()
  }
}

private operator fun Rect.contains(event: MotionEvent): Boolean {
  return contains(event.x.toInt(), event.y.toInt())
}
