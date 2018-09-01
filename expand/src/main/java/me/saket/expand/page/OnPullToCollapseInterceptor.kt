package me.saket.expand.page

import android.view.MotionEvent

interface OnPullToCollapseInterceptor {

  /**
   * Called once every time a vertical scroll gesture is registered on [ExpandablePageLayout].
   * When intercepted, all touch events until the finger is lifted will be ignored. This is
   * useful when nested (vertically) scrollable layouts are also present inside the page.
   *
   * @param downX      X-coordinate from where the gesture started, relative to the screen window.
   * @param downY      Y-coordinate from where the gesture started, relative to the screen window.
   * @param upwardPull Upward pull == downward scroll and vice versa.
   *
   * @return True to consume this touch event. False otherwise.
   */
  fun onInterceptPullToCollapseGesture(event: MotionEvent, downX: Float, downY: Float, upwardPull: Boolean): Boolean
}
