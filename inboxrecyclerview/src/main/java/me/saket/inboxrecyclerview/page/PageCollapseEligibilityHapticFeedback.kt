package me.saket.inboxrecyclerview.page

import android.view.HapticFeedbackConstants
import me.saket.inboxrecyclerview.dimming.DimPainter

/**
 * Plays haptic feedback during a pull-to-collapse gesture to indicate when the page can be
 * released to collapse. 
 *
 * @param feedbackConstantForPull Haptic feedback played when the page is dragged enough to be
 * eligible for a collapse (if released). This is played again if the page is dragged back up.
 *
 * @param feedbackConstantForRelease Haptic feedback played when the page is released to collapse.
 */
class PageCollapseEligibilityHapticFeedback(
  private val page: ExpandablePageLayout,
  private val feedbackConstantForPull: Int = HapticFeedbackConstants.LONG_PRESS,
  private val feedbackConstantForRelease: Int = HapticFeedbackConstants.LONG_PRESS,
  private val minimumMillisBetweenPlays: Long = 100
) : OnExpandablePagePullListener {

  private var lastPlayedAt = 0L

  private val isCollapseEligible = OnChange(false) {
    lastPlayedAt = System.currentTimeMillis()
    page.performHapticFeedback(feedbackConstantForPull)
  }

  override fun onPull(
    deltaY: Float,
    currentTranslationY: Float,
    upwardPull: Boolean,
    deltaUpwardPull: Boolean,
    collapseEligible: Boolean
  ) {
    if (page.isExpanded) {  // Page can be pulled while it's expanding.
      isCollapseEligible.value = collapseEligible
    }
  }

  override fun onRelease(collapseEligible: Boolean) {
    // Avoid playing overlapping haptic feedbacks, in case the page was pulled and collapsed quickly.
    if (collapseEligible && (System.currentTimeMillis() - lastPlayedAt) > minimumMillisBetweenPlays) {
      page.performHapticFeedback(feedbackConstantForRelease)
      isCollapseEligible.value = false
    }
  }
}

private class OnChange<T>(defaultValue: T, val onChange: (value: T) -> Unit) {
  var value: T = defaultValue
    set(value) {
      if (field != value) {
        field = value
        onChange(value)
      }
    }
}
