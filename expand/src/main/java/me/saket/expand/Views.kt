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
  fun View.executeOnNextLayout(listener: () -> Unit) {
    viewTreeObserver.addOnGlobalLayoutListener(object : ViewTreeObserver.OnGlobalLayoutListener {
      override fun onGlobalLayout() {
        viewTreeObserver.removeOnGlobalLayoutListener(this)
        listener()
      }
    })
  }

  /**
   * Execute a runnable when a [view]'s dimensions get measured and is laid out on the screen.
   */
  @SuppressLint("LogNotTimber")
  fun View.executeOnMeasure(listener: () -> Unit) {
    if (isInEditMode || isLaidOut) {
      listener()
      return
    }

    viewTreeObserver.addOnPreDrawListener(object : ViewTreeObserver.OnPreDrawListener {
      override fun onPreDraw(): Boolean {
        if (isLaidOut) {
          viewTreeObserver.removeOnPreDrawListener(this)
          listener()

        } else if (visibility == View.GONE) {
          Log.w("Views", "View's visibility is set to Gone. It'll never be measured: ${resources.getResourceEntryName(id)}")
          viewTreeObserver.removeOnPreDrawListener(this)
        }
        return true
      }
    })
  }
}
