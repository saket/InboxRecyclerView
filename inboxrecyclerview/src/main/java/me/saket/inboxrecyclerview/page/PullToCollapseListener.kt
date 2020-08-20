package me.saket.inboxrecyclerview.page

import android.annotation.SuppressLint
import android.content.Context
import android.view.MotionEvent
import android.view.View
import android.view.ViewConfiguration
import me.saket.inboxrecyclerview.Views
import java.util.ArrayList
import kotlin.math.abs

class PullToCollapseListener(private val expandablePage: ExpandablePageLayout) : View.OnTouchListener {

  @Deprecated(message = "Passing context is no longer needed")
  constructor(context: Context, expandablePage: ExpandablePageLayout) : this(expandablePage)

  /** Minimum Y-distance the page has to be pulled before it's eligible for collapse. */
  var collapseDistanceThreshold: Int = (Views.toolbarHeight(expandablePage.context) * 0.85F).toInt()

  private val touchSlop: Int = ViewConfiguration.get(expandablePage.context).scaledTouchSlop
  private val onPullListeners = ArrayList<OnPullListener>(3)
  private var downX: Float = 0F
  private var downY: Float = 0F
  private var lastMoveY: Float = 0F
  private var eligibleForCollapse: Boolean = false
  private var horizontalSwipingConfirmed: Boolean? = null
  private var interceptedUntilNextGesture: Boolean? = null

  interface OnPullListener {

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
  override fun onTouch(v: View, event: MotionEvent): Boolean {
    when (event.actionMasked) {
      MotionEvent.ACTION_DOWN -> {
        downX = event.rawX
        downY = event.rawY
        lastMoveY = downY

        eligibleForCollapse = false
        horizontalSwipingConfirmed = null
        interceptedUntilNextGesture = horizontalSwipingConfirmed

        // Page may have been touched during an animation. Stop ship!
        cancelAnyOngoingAnimations()
        return false
      }

      MotionEvent.ACTION_MOVE -> {
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

        // When it's confirmed that the movement distance is > touchSlop and this is
        // indeed a gesture, two checks are made:
        //
        // 1. Whether this is a horizontal swipe.
        // 2. Whether an interceptor wants to intercept this gesture.
        //
        // These two checks should only happen once per gesture, just when the gesture
        // starts. The flags will reset when the finger is lifted.

        // Ignore horizontal swipes (Step 1).
        if (horizontalSwipingConfirmed == null) {
          horizontalSwipingConfirmed = totalSwipeDistanceXAbs > totalSwipeDistanceYAbs

          // Any ongoing release animation must also be canceled at the starting of a gesture.
          if (expandablePage.isExpanded) {
            cancelAnyOngoingAnimations()
          }

          if (horizontalSwipingConfirmed!!) {
            return false
          }
        }

        val deltaUpwardSwipe = deltaY < 0

        if (interceptedUntilNextGesture == null) {
          val interceptResult = expandablePage.handleOnPullToCollapseIntercept(event, downX, downY, deltaUpwardSwipe)
          interceptedUntilNextGesture = interceptResult == InterceptResult.INTERCEPTED

          if (interceptedUntilNextGesture!!) {
            return false
          } else {
            expandablePage.parent.requestDisallowInterceptTouchEvent(true)
          }
        }

        val upwardSwipe = totalSwipeDistanceY < 0F

        eligibleForCollapse = when {
          upwardSwipe -> expandablePage.translationY <= -collapseDistanceThreshold
          else -> expandablePage.translationY >= collapseDistanceThreshold
        }

        // The page isn't moved with the same speed as the finger. Some friction is applied
        // to make the gesture feel nice. This friction is increased further once the page
        // is eligible for collapse as a visual indicator that the page can now be released.
        val frictionFactor = 4F
        var deltaYWithFriction = deltaY / frictionFactor

        if (eligibleForCollapse) {
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
      onPullListener.onRelease(eligibleForCollapse)
    }
  }

  private fun dispatchPulledCallback(deltaY: Float, upwardPull: Boolean, deltaUpwardPull: Boolean) {
    val translationY = expandablePage.translationY
    for (i in onPullListeners.indices) {
      val onPullListener = onPullListeners[i]
      onPullListener.onPull(deltaY, translationY, upwardPull, deltaUpwardPull, eligibleForCollapse)
    }
  }

  private fun cancelAnyOngoingAnimations() {
    expandablePage.stopAnyOngoingAnimation()
  }
}
