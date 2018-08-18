package me.saket.expand

import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration

import java.util.ArrayList

class PullToCollapseListener(context: Context, private val expandablePage: ExpandablePageLayout) : View.OnTouchListener {

  private val touchSlop: Int = ViewConfiguration.get(context).scaledTouchSlop
  private val onPullListeners = ArrayList<OnPullListener>(3)
  private var collapseDistanceThreshold: Int = 0
  private var downX: Float = 0.toFloat()
  private var downY: Float = 0.toFloat()
  private var lastMoveY: Float = 0.toFloat()
  private var eligibleForCollapse: Boolean = false
  private var horizontalSwipingConfirmed: Boolean? = null
  private var interceptedUntilNextGesture: Boolean? = null

  interface OnPullListener {

    /**
     * Called when the user is pulling down / up the expandable page or the list.
     *
     * @param deltaY              Delta translation-Y since the last onPull call.
     * @param currentTranslationY Current translation-Y of the page.
     * @param upwardPull          Whether or not the page is being pulled in the upward direction.
     * @param deltaUpwardPull     Whether or not the last delta-pull was made in the upward direction.
     * @param collapseEligible    Whether or not the pull distance was enough to trigger a collapse.
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

  /**
   * The distance after which the page can collapse when pulled.
   */
  fun setCollapseDistanceThreshold(threshold: Int) {
    collapseDistanceThreshold = threshold
  }

  override fun onTouch(v: View, event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.rawX
        downY = event.rawY
        lastMoveY = downY

        eligibleForCollapse = false
        horizontalSwipingConfirmed = null
        interceptedUntilNextGesture = horizontalSwipingConfirmed
        return false
      }

      MotionEvent.ACTION_MOVE -> {
        // Keep ignoring horizontal swipes.
        if (horizontalSwipingConfirmed != null && horizontalSwipingConfirmed!!) {
          return false
        }
        if (interceptedUntilNextGesture != null && interceptedUntilNextGesture!!) {
          return false
        }

        val deltaY = event.rawY - lastMoveY
        val totalSwipeDistanceX = event.rawX - downX
        val totalSwipeDistanceY = event.rawY - downY
        val totalSwipeDistanceXAbs = Math.abs(totalSwipeDistanceX)
        val totalSwipeDistanceYAbs = Math.abs(totalSwipeDistanceY)

        if (totalSwipeDistanceYAbs < touchSlop && totalSwipeDistanceXAbs < touchSlop) {
          return false
        }

        // When it's confirmed that the movement distance is > touchSlop and this is indeed a gesture,
        // we check for two things:
        // 1. Whether or not this is a horizontal swipe.
        // 2. Whether a registered intercepter wants to intercept this gesture.
        // These two checks should only happen once per gesture, just when the gesture starts. The
        // flags will reset when the finger is lifted.

        // Ignore horizontal swipes (Step 1)
        if (horizontalSwipingConfirmed == null) {
          horizontalSwipingConfirmed = totalSwipeDistanceXAbs > totalSwipeDistanceYAbs

          // Lazy hack: We must also cancel any ongoing release animation. But it should only be performed
          // once, at the starting of a gesture. Let's just do it here.
          if (expandablePage.isExpanded) {
            cancelAnyOngoingAnimations()
          }

          if (horizontalSwipingConfirmed!!) {
            return false
          }
        }

        val deltaUpwardSwipe = deltaY < 0

        // Avoid registering this gesture if the page doesn't want us to. Mostly used when the page also
        // has a scrollable child.
        if (interceptedUntilNextGesture == null) {
          interceptedUntilNextGesture = expandablePage.handleOnPullToCollapseIntercept(event, downX, downY, deltaUpwardSwipe)
          if (interceptedUntilNextGesture!!) {
            return false
          } else {
            expandablePage.parent.requestDisallowInterceptTouchEvent(true)
          }
        }

        val upwardSwipe = totalSwipeDistanceY < 0f
        val resistanceFactor = 4f

        // If the gesture has covered a distance >= the toolbar height, mark this gesture eligible
        // for collapsible when the finger is lifted
        val collapseThresholdDistance = (collapseDistanceThreshold * COLLAPSE_THRESHOLD_DISTANCE_FACTOR).toInt()
        eligibleForCollapse = if (upwardSwipe)
          expandablePage.translationY <= -collapseThresholdDistance
        else
          expandablePage.translationY >= collapseThresholdDistance
        var resistedDeltaY = deltaY / resistanceFactor

        // Once it's eligible, start resisting more as an indicator that the
        // page / list is being overscrolled. This will also prevent the user
        // to swipe all the way down or up
        if (eligibleForCollapse && expandablePage.translationY != 0f) {
          val extraResistance = collapseDistanceThreshold / Math.abs(expandablePage.translationY)
          resistedDeltaY *= extraResistance / 2
        }

        // Move page
        val translationY = expandablePage.translationY + resistedDeltaY
        expandablePage.translationY = translationY
        dispatchPulledCallback(resistedDeltaY, translationY, upwardSwipe, deltaUpwardSwipe)

        lastMoveY = event.rawY
        return true
      }

      MotionEvent.ACTION_CANCEL, MotionEvent.ACTION_UP -> {
        // Collapse or restore the page when the finger is lifted, depending on
        // the pull distance
        val totalSwipeDistanceY = event.rawY - downY
        if (Math.abs(totalSwipeDistanceY) >= touchSlop) {
          dispatchReleaseCallback()
        }
      }
    }

    return false
  }

  private fun dispatchReleaseCallback() {
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onRelease(eligibleForCollapse)
    }
  }

  private fun dispatchPulledCallback(deltaY: Float, translationY: Float, upwardPull: Boolean, deltaUpwardPull: Boolean) {
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onPull(deltaY, translationY, upwardPull, deltaUpwardPull, eligibleForCollapse)
    }
  }

  private fun cancelAnyOngoingAnimations() {
    expandablePage.stopAnyOngoingPageAnimation()
  }

  companion object {
    private const val COLLAPSE_THRESHOLD_DISTANCE_FACTOR = 0.85f   // This gets multiplied with the toolbar height
  }
}
