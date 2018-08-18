package me.saket.expand

import android.animation.ObjectAnimator
import android.animation.TimeInterpolator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Outline
import android.graphics.Rect
import android.support.v4.view.animation.FastOutSlowInInterpolator
import android.util.AttributeSet
import android.view.View
import android.view.ViewOutlineProvider
import android.widget.RelativeLayout

/** Animates change in dimensions by clipping bounds instead of changing the layout params. */
abstract class BaseExpandablePageLayout : RelativeLayout {

  val clippedRect = Rect()
  private var dimensionAnimator: ValueAnimator? = null
  private var isFullyVisible: Boolean = false

  var animationDurationMillis = DEFAULT_ANIM_DURATION

  private val clippedWidth: Int
    get() = clippedRect.width()

  protected val clippedHeight: Int
    get() = clippedRect.height()

  val animationInterpolator: TimeInterpolator
    get() = ANIM_INTERPOLATOR

  constructor(context: Context) : super(context) {
    init()
  }

  constructor(context: Context, attrs: AttributeSet) : super(context, attrs) {
    init()
  }

  private fun init() {
    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        outline.setRect(0, 0, clippedRect.width(), clippedRect.height())
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
    cancelOngoingClipAnimation()

    dimensionAnimator = ObjectAnimator.ofFloat(0f, 1f).apply {
      duration = animationDurationMillis
      interpolator = animationInterpolator
      startDelay = InboxRecyclerView.animationStartDelay.toLong()

      val fromWidth = clippedWidth
      val fromHeight = clippedHeight

      addUpdateListener {
        val scale = it.animatedValue as Float
        val newWidth = ((toWidth - fromWidth) * scale + fromWidth).toInt()
        val newHeight = ((toHeight - fromHeight) * scale + fromHeight).toInt()
        setClippedDimensions(newWidth, newHeight)
      }
    }
    dimensionAnimator!!.start()
  }

  fun setClippedDimensions(newClippedWidth: Int, newClippedHeight: Int) {
    isFullyVisible = newClippedWidth > 0 && newClippedHeight > 0 && newClippedWidth == width && newClippedHeight == height

    clippedRect.right = newClippedWidth
    clippedRect.bottom = newClippedHeight

    clipBounds = Rect(clippedRect.left, clippedRect.top, clippedRect.right, clippedRect.bottom)
    invalidateOutline()
  }

  /** Immediately reset the clipping so that this layout is visible. */
  fun resetClipping() {
    setClippedDimensions(width, height)
  }

  private fun cancelOngoingClipAnimation() {
    if (dimensionAnimator != null) {
      dimensionAnimator!!.cancel()
    }
  }

  companion object {
    const val DEFAULT_ANIM_DURATION = 250L
    private val ANIM_INTERPOLATOR = FastOutSlowInInterpolator()
  }
}
