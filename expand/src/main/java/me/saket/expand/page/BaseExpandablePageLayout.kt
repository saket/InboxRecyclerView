package me.saket.expand.page

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
import androidx.interpolator.view.animation.FastOutSlowInInterpolator
import me.saket.expand.InboxRecyclerView

/**
 * Animating change in dimensions by changing the actual width and height is expensive.
 * This layout animates change in dimensions by clipping visible bounds instead.
 */
abstract class BaseExpandablePageLayout @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : RelativeLayout(context, attrs) {

  /** The visible portion of this layout. */
  val clippedRect = Rect()

  private var dimensionAnimator: ValueAnimator = ObjectAnimator()
  private var isFullyVisible: Boolean = false

  var animationDurationMillis = DEFAULT_ANIM_DURATION
  var animationInterpolator: TimeInterpolator = DEFAULT_ANIM_INTERPOLATOR

  init {
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRect(0, 0, clippedRect.width(), clippedRect.height())
        outline.alpha = clippedRect.height().toFloat() / height
      }
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    if (isFullyVisible) {
      setClippedDimensions(w, h)
    }
  }

  fun animateDimensions(toWidth: Int, toHeight: Int) {
    dimensionAnimator.cancel()

    dimensionAnimator = ObjectAnimator.ofFloat(0F, 1F).apply {
      duration = animationDurationMillis
      interpolator = animationInterpolator
      startDelay = InboxRecyclerView.animationStartDelay.toLong()

      val fromWidth = clippedRect.width()
      val fromHeight = clippedRect.height()

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
    isFullyVisible = newClippedWidth > 0 && newClippedHeight > 0 && newClippedWidth == width && newClippedHeight == height

    clippedRect.right = newClippedWidth
    clippedRect.bottom = newClippedHeight

    clipBounds = Rect(clippedRect.left, clippedRect.top, clippedRect.right, clippedRect.bottom)
    invalidateOutline()
  }

  /** Immediately reset the clipping so that this layout is fully visible. */
  fun resetClipping() {
    setClippedDimensions(width, height)
  }

  companion object {
    private const val DEFAULT_ANIM_DURATION = 250L
    private val DEFAULT_ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
  }
}
