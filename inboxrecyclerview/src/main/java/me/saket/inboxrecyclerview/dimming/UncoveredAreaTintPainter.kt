package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.PageStateChangeCallbacks

/**
 * Draws a tint on [InboxRecyclerView] only in the area that's not covered by its page.
 * This allows the page content to not have another background of its own, thus reducing
 * overdraw by a level.
 *
 * If the tinted area appears incorrect, try using [TintPainter.completeList] instead.
 */
open class UncoveredAreaTintPainter(color: Int, opacity: Float) : TintPainter(), PageStateChangeCallbacks {

  private val minIntensity = 0
  private val maxIntensity = (255 * opacity).toInt()    // [0..255]

  private var tintAnimator: ValueAnimator = ObjectAnimator()
  private var lastIsCollapseEligible = false

  protected val tintPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  protected lateinit var recyclerView: InboxRecyclerView

  init {
    tintPaint.color = color
    tintPaint.alpha = minIntensity
  }

  private val pagePreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
    private var lastTranslationY = 0F
    private var lastClippedDimens = Rect()
    private var lastState = ExpandablePageLayout.PageState.COLLAPSED

    override fun onPreDraw(): Boolean {
      val page = recyclerView.page
      if (lastTranslationY != page.translationY || lastClippedDimens != page.clippedDimens || lastState != page.currentState) {
        onPageMove()
      }

      lastTranslationY = page.translationY
      lastClippedDimens = page.clippedDimens
      lastState = page.currentState
      return true
    }
  }

  private val pageLayoutChangeListener = {
    // Changes in the page's dimensions will get handled here.
    onPageMove()
  }

  override fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {
    this.recyclerView = recyclerView
    recyclerView.page.addStateChangeCallbacks(this)
    recyclerView.page.viewTreeObserver.addOnGlobalLayoutListener(pageLayoutChangeListener)
    recyclerView.page.viewTreeObserver.addOnPreDrawListener(pagePreDrawListener)
  }

  override fun onDetachRecyclerView(recyclerView: InboxRecyclerView) {
    recyclerView.page.removeStateChangeCallbacks(this)
    recyclerView.page.viewTreeObserver.removeOnGlobalLayoutListener(pageLayoutChangeListener)
    recyclerView.page.viewTreeObserver.removeOnPreDrawListener(pagePreDrawListener)
  }

  private fun onPageMove() {
    // Remove dimming when the page is being pulled and is eligible for collapse.
    if (recyclerView.page.isExpanded) {
      val collapseThreshold = recyclerView.page.pullToCollapseThresholdDistance
      val translationYAbs = Math.abs(recyclerView.page.translationY)
      val isCollapseEligible = translationYAbs >= collapseThreshold

      if (isCollapseEligible != lastIsCollapseEligible) {
        animateDimming(
            toAlpha = if (isCollapseEligible) minIntensity else maxIntensity,
            dimDuration = 300)
      }
      lastIsCollapseEligible = isCollapseEligible

    } else {
      lastIsCollapseEligible = false
    }
  }

  override fun drawTint(canvas: Canvas) {
    recyclerView.apply {
      // Content above the page.
      canvas.drawRect(0F, 0F, right.toFloat(), page.translationY, tintPaint)

      // Content below the page.
      if (page.isExpanded) {
        canvas.drawRect(0F, (bottom + page.translationY), right.toFloat(), bottom.toFloat(), tintPaint)

      } else if (page.isExpandingOrCollapsing) {
        val pageBottom = page.translationY + page.clippedDimens.height().toFloat()
        canvas.drawRect(0F, pageBottom, right.toFloat(), bottom.toFloat(), tintPaint)
      }
    }
  }

  override fun onPageAboutToExpand(expandAnimDuration: Long) {
    tintAnimator.cancel()
    animateDimming(maxIntensity, expandAnimDuration)
  }

  override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
    tintAnimator.cancel()
    animateDimming(minIntensity, collapseAnimDuration)
  }

  private fun animateDimming(toAlpha: Int, dimDuration: Long) {
    tintAnimator = ObjectAnimator.ofInt(tintPaint.alpha, toAlpha).apply {
      duration = dimDuration
      interpolator = recyclerView.page.animationInterpolator
      startDelay = InboxRecyclerView.animationStartDelay.toLong()
    }
    tintAnimator.addUpdateListener {
      tintPaint.alpha = it.animatedValue as Int
      recyclerView.postInvalidate()
    }
    tintAnimator.start()
  }

  override fun onPageExpanded() {}

  override fun onPageCollapsed() {}
}
