package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.animation.DecelerateInterpolator
import androidx.core.animation.doOnStart

internal open class AnimatedColorDrawable(
  private val view: View,
  color: Int,
  private val animDuration: Long
) : ColorDrawable(color) {
  private var alphaAnimator = ValueAnimator()
  private var animatingToAlpha = 0

  init {
    super.setAlpha(0)
  }

  override fun setAlpha(alpha: Int) {
    if (super.getAlpha() == alpha) return
    if (alphaAnimator.isRunning && animatingToAlpha == alpha) return

    cancelAnimation(jumpToOngoingAlpha = false)
    alphaAnimator = ObjectAnimator.ofInt(super.getAlpha(), alpha).apply {
      startDelay = 0
      duration = animDuration
      interpolator = DecelerateInterpolator()
      doOnStart {
        animatingToAlpha = alpha
      }
      addUpdateListener {
        super.setAlpha(it.animatedValue as Int)
        view.invalidate()
      }
      start()
    }
  }

  fun cancelAnimation(jumpToOngoingAlpha: Boolean) {
    alphaAnimator.cancel()

    if (jumpToOngoingAlpha) {
      super.setAlpha(animatingToAlpha)
    }
  }
}
