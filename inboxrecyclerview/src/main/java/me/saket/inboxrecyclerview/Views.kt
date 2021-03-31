package me.saket.inboxrecyclerview

import android.animation.Animator
import android.animation.AnimatorListenerAdapter
import android.content.Context
import android.graphics.Rect
import android.view.View
import android.view.ViewPropertyAnimator

internal object Views {
  internal fun toolbarHeight(context: Context): Int {
    val typedArray = context.obtainStyledAttributes(intArrayOf(android.R.attr.actionBarSize))
    val standardToolbarHeight = typedArray.getDimensionPixelSize(0, 0)
    typedArray.recycle()
    return standardToolbarHeight
  }
}

internal fun ViewPropertyAnimator.withEndAction(action: (Boolean) -> Unit): ViewPropertyAnimator {
  return setListener(object : AnimatorListenerAdapter() {
    var canceled = false

    override fun onAnimationStart(animation: Animator) {
      canceled = false
    }

    override fun onAnimationCancel(animation: Animator) {
      canceled = true
    }

    override fun onAnimationEnd(animation: Animator) {
      action(canceled)
    }
  })
}

internal fun View.locationOnScreen(
  intBuffer: IntArray = IntArray(2),
  rectBuffer: Rect = Rect(),
  ignoreTranslations: Boolean = false
): Rect {
  getLocationOnScreen(intBuffer)
  if (ignoreTranslations) {
    intBuffer[0] -= translationX.toInt()
    intBuffer[1] -= translationY.toInt()
  }
  rectBuffer.set(intBuffer[0], intBuffer[1], intBuffer[0] + width, intBuffer[1] + height)
  return rectBuffer
}

/**
 * View#doOnLayout() has a terrible gotcha that it gets called _during_ a layout
 * when isLaidOut resolves to true, causing nested onLayouts to possibly never execute.
 */
internal inline fun View.doOnLayout2(crossinline action: () -> Unit) {
  if (isInEditMode || isLaidOut || (width > 0 || height > 0)) {
    action()
    return
  }

  addOnLayoutChangeListener(object : View.OnLayoutChangeListener {
    override fun onLayoutChange(
      view: View,
      left: Int,
      top: Int,
      right: Int,
      bottom: Int,
      oldLeft: Int,
      oldTop: Int,
      oldRight: Int,
      oldBottom: Int
    ) {
      view.removeOnLayoutChangeListener(this)
      action()
    }
  })
}
