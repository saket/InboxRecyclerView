package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.animation.DecelerateInterpolator

/**
 * A Drawable that toggles its alpha between 255 and 0.
 *
 * @param onInvalidate Useful when this drawable isn't set as the background or the foreground
 * of a View because the View will otherwise not auto-invalidate itself when Drawable's alpha is
 * updated.
 */
class AnimatedVisibilityColorDrawable(
  color: Int,
  private val animDuration: Long,
  private val onInvalidate: (() -> Unit)? = null,
) : ColorDrawable(color) {

  private var alphaAnimator = ValueAnimator()
  var isShown: Boolean? = null

  init {
    setShown(false, immediately = true)
  }

  override fun invalidateSelf() {
    super.invalidateSelf()
    onInvalidate?.invoke()
  }

  fun setShown(show: Boolean, immediately: Boolean = false) {
    val changed = show != isShown
    if (!changed) {
      return
    }
    isShown = show

    val targetAlpha = if (show) 255 else 0
    if (immediately) {
      alpha = targetAlpha

    } else {
      alphaAnimator.cancel()
      alphaAnimator = ObjectAnimator.ofInt(super.getAlpha(), targetAlpha).apply {
        startDelay = 0
        duration = animDuration
        interpolator = DecelerateInterpolator()
        addUpdateListener {
          alpha = it.animatedValue as Int
        }
        start()
      }
    }
  }

  fun cancelAnimation(setAlphaTo: Int?) {
    alphaAnimator.cancel()

    if (setAlphaTo != null) {
      alpha = setAlphaTo
    }
  }
}
