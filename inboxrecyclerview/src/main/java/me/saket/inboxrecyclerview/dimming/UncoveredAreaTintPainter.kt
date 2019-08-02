package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import me.saket.inboxrecyclerview.ANIMATION_START_DELAY
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.animation.PageLocationChangeDetector
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.PageStateChangeCallbacks
import kotlin.math.abs

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
  private lateinit var changeDetector: PageLocationChangeDetector

  init {
    tintPaint.color = color
    tintPaint.alpha = minIntensity
  }

  override fun onAttachRecyclerView(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    this.recyclerView = recyclerView
    this.changeDetector = PageLocationChangeDetector(page, changeListener = ::onPageMove)

    page.viewTreeObserver.addOnGlobalLayoutListener(changeDetector)
    page.viewTreeObserver.addOnPreDrawListener(changeDetector)
    page.addStateChangeCallbacks(this)
  }

  override fun onDetachRecyclerView(page: ExpandablePageLayout) {
    page.removeStateChangeCallbacks(this)
    page.viewTreeObserver.removeOnGlobalLayoutListener(changeDetector)
    page.viewTreeObserver.removeOnPreDrawListener(changeDetector)
    tintAnimator.cancel()
  }

  private fun onPageMove(page: ExpandablePageLayout) {
    // Remove dimming when the page is being pulled and is eligible for collapse.
    if (page.isExpanded) {
      val collapseThreshold = page.pullToCollapseThresholdDistance
      val translationYAbs = abs(page.translationY)
      val isCollapseEligible = translationYAbs >= collapseThreshold

      if (isCollapseEligible != lastIsCollapseEligible) {
        page.animateDimming(
            toAlpha = if (isCollapseEligible) minIntensity else maxIntensity,
            dimDuration = 300)
      }
      lastIsCollapseEligible = isCollapseEligible

    } else {
      lastIsCollapseEligible = false
    }
  }

  override fun drawTint(canvas: Canvas, page: ExpandablePageLayout) {
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

  override fun onPageAboutToExpand(
    page: ExpandablePageLayout,
    expandAnimDuration: Long
  ) {
    tintAnimator.cancel()
    page.animateDimming(maxIntensity, expandAnimDuration)
  }

  override fun onPageAboutToCollapse(
    page: ExpandablePageLayout,
    collapseAnimDuration: Long
  ) {
    tintAnimator.cancel()
    page.animateDimming(minIntensity, collapseAnimDuration)
  }

  private fun ExpandablePageLayout.animateDimming(toAlpha: Int, dimDuration: Long) {
    tintAnimator = ObjectAnimator.ofInt(tintPaint.alpha, toAlpha).apply {
      duration = dimDuration
      interpolator = animationInterpolator
      startDelay = ANIMATION_START_DELAY
    }
    tintAnimator.addUpdateListener {
      tintPaint.alpha = it.animatedValue as Int
      recyclerView.postInvalidate()
    }
    tintAnimator.start()
  }

  override fun onPageExpanded(page: ExpandablePageLayout) = Unit

  override fun onPageCollapsed(page: ExpandablePageLayout) = Unit
}
