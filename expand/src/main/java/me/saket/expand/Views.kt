package me.saket.expand

import android.annotation.SuppressLint
import android.util.Log
import android.view.View
import android.view.ViewTreeObserver

// TODO: Convert to extension functions.
internal object Views {

  /**
   * Execute a runnable when the next global layout happens for a `View`. Example usage includes
   * waiting for a list to draw its children just after you have updated its adapter's data-set.
   */
  fun executeOnNextLayout(view: View, listener: () -> Unit) {
    view.viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        view.viewTreeObserver.removeOnGlobalLayoutListener(this)
        listener()
      }
    })
  }

  /**
   * Execute a runnable when a [view]'s dimensions get measured and is laid out on the screen.
   */
  @SuppressLint("LogNotTimber")
  fun executeOnMeasure(view: View, listener: () -> Unit) {
    if (view.isInEditMode || view.isLaidOut) {
      listener()
      return
    }

    view.viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
      override fun onPreDraw(): Boolean {
        if (view.isLaidOut) {
          view.viewTreeObserver.removeOnPreDrawListener(this)
          listener()

        } else if (view.visibility == View.GONE) {
          Log.w("Views", "View's visibility is set to Gone. It'll never be measured: $view")
          view.viewTreeObserver.removeOnPreDrawListener(this)
        }
        return true
      }
    })
  }
}
