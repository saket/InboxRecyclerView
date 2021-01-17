package me.saket.inboxrecyclerview.page

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout
import androidx.core.widget.NestedScrollView
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import me.saket.inboxrecyclerview.ANIMATION_START_DELAY

/**
 * Animating change in dimensions by changing the actual width and height is expensive.
 * This layout animates change in dimensions by clipping visible bounds instead.
 */
abstract class BaseExpandablePageLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : NestedScrollView(context, attrs) {

  /** The visible portion of this layout. Warning: this is mutable. Use wisely! */
  internal val clippedDimens: Rect = Rect()

  private var dimensionAnimator: ValueAnimator = ObjectAnimator()
  private var isFullyVisible: Boolean = false

  var animationDurationMillis = DEFAULT_ANIM_DURATION
  var animationInterpolator: TimeInterpolator = DEFAULT_ANIM_INTERPOLATOR

  init {
    clipBounds = clippedDimens
    isFillViewport = true
    overScrollMode = OVER_SCROLL_ALWAYS

    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRect(0, 0, clippedDimens.width(), clippedDimens.height())
        outline.alpha = clippedDimens.height().toFloat() / height
      }
    }
  }

  override fun onDetachedFromWindow() {
    dimensionAnimator.cancel()
    super.onDetachedFromWindow()
  }

  protected fun stopDimensionAnimation() {
    dimensionAnimator.cancel()
  }

  fun animateDimensions(toWidth: Int, toHeight: Int) {
    stopDimensionAnimation()

    dimensionAnimator = ObjectAnimator.ofFloat(0F, 1F).apply {
      duration = animationDurationMillis
      interpolator = animationInterpolator
      startDelay = ANIMATION_START_DELAY

      val fromWidth = clippedDimens.width()
      val fromHeight = clippedDimens.height()

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newWidth = ((toWidth - fromWidth) * scale + fromWidth).toInt()
        val newHeight = ((toHeight - fromHeight) * scale + fromHeight).toInt()
        setClippedDimensions(newWidth, newHeight)
      }
    }
    dimensionAnimator.start()
  }

  fun setClippedDimensions(newClippedWidth: Int, newClippedHeight: Int) {
    isFullyVisible = newClippedWidth > 0 && newClippedHeight > 0
        && newClippedWidth == width
        && newClippedHeight == height
    clippedDimens.set(0, 0, newClippedWidth, newClippedHeight)
    clipBounds = clippedDimens
    invalidateOutline()
  }

  /** Immediately reset the clipping so that this layout is fully visible. */
  fun resetClipping() {
    setClippedDimensions(width, height)
  }

  companion object {
    private const val DEFAULT_ANIM_DURATION = 300L
    private val DEFAULT_ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
  }
}
