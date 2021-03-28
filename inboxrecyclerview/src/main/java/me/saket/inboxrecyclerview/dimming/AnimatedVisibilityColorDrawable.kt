package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.animation.DecelerateInterpolator
import androidx.annotation.IntRange

/**
 * A Drawable that smoothly toggles its alpha between 0 and [maxAlpha].
 *
 * @param onInvalidate Useful when this drawable isn't set as the background or the foreground
 * of a View because the View will otherwise not auto-invalidate itself when this Drawable's
 * alpha is updated.
 */
class AnimatedVisibilityColorDrawable(
  color: Int,
  @IntRange(from = 0, to = 255) private val maxAlpha: Int,
  private val animDuration: Long,
  private val onInvalidate: (() -> Unit)? = null
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

  // This name sounds a bit stupid, but I couldn't use setVisible(),
  // whose lifecycle is managed by the host View of this drawable.
  fun setShown(show: Boolean, immediately: Boolean = false) {
    val targetAlpha = if (show) maxAlpha else 0

    if (this.alpha == targetAlpha) return
    if (alphaAnimator.isRunning && isShown == show) return

    isShown = show

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
