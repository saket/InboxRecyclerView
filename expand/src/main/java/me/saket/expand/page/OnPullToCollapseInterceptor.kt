package me.saket.expand.page

import android.view.MotionEvent

interface OnPullToCollapseInterceptor {

  /**
   * TODO: Improve doc.
   *
   * Called when the user makes a vertical swipe gesture, which is registered as a pull-to-collapse gesture.
   * This is called once per gesture, when the user's finger touches this page. `ExpandablePage`
   * starts accepting this gesture if this method returns false.
   *
   * So, if you have other vertically scrollable Views in your layout (RecyclerView, ListView, ScrollView, etc.),
   * you can return true to consume a gesture after verifying that the touch event lies on one of those Views.
   * This will block `ExpandablePage` from processing the gesture until the finger is lifted.
   *
   * @param downX          X-location from where the gesture started.
   * @param downY          Y-location from where the gesture started.
   * @param upwardPagePull True if the PAGE is being pulled upwards. Remember that upward swipe == downward
   * scroll and vice versa.
   * @return True to consume this touch event. False otherwise.
   */
  fun onInterceptPullToCollapseGesture(event: MotionEvent, downX: Float, downY: Float, upwardPagePull: Boolean): Boolean
}
