package me.saket.expand.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.expand.InboxRecyclerView
import me.saket.expand.page.ExpandablePageLayout
import me.saket.expand.page.PageStateChangeCallbacks

/**
 * Draws dimming on [InboxRecyclerView] only in the area that's not covered by its page.
 * This allows the page content to not have another background of its own, thus reducing
 * overdraw by a level.
 *
 * If the dimming appears incorrect, try using [ItemDimmer.allItems] instead.
 */
open class UncoveredItemsDimmer(color: Int, intensity: Float) : ItemDimmer(), PageStateChangeCallbacks {

  private val minDim = 0
  private val maxDim = (255 * intensity).toInt()    // [0..255]

  private var dimAnimator: ValueAnimator = ObjectAnimator()
  private var lastIsCollapseEligible = false

  protected val dimPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)
  protected lateinit var recyclerView: InboxRecyclerView

  init {
    dimPaint.color = color
    dimPaint.alpha = minDim
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
            toAlpha = if (isCollapseEligible) minDim else maxDim,
            dimDuration = 300)
      }
      lastIsCollapseEligible = isCollapseEligible

    } else {
      lastIsCollapseEligible = false
    }
  }

  override fun drawDimming(canvas: Canvas) {
    recyclerView.apply {
      // Content above the page.
      canvas.drawRect(0F, 0F, right.toFloat(), page.translationY, dimPaint)

      // Content below the page.
      if (page.isExpanded) {
        canvas.drawRect(0F, (bottom + page.translationY), right.toFloat(), bottom.toFloat(), dimPaint)

      } else if (page.isExpandingOrCollapsing) {
        val pageBottom = page.translationY + page.clippedDimens.height().toFloat()
        canvas.drawRect(0F, pageBottom, right.toFloat(), bottom.toFloat(), dimPaint)
      }
    }
  }

  override fun onPageAboutToExpand(expandAnimDuration: Long) {
    dimAnimator.cancel()
    animateDimming(maxDim, expandAnimDuration)
  }

  override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
    dimAnimator.cancel()
    animateDimming(minDim, collapseAnimDuration)
  }

  private fun animateDimming(toAlpha: Int, dimDuration: Long) {
    dimAnimator = ObjectAnimator.ofInt(dimPaint.alpha, toAlpha).apply {
      duration = dimDuration
      interpolator = recyclerView.page.animationInterpolator
      startDelay = InboxRecyclerView.animationStartDelay.toLong()
    }
    dimAnimator.addUpdateListener {
      dimPaint.alpha = it.animatedValue as Int
      recyclerView.postInvalidate()
    }
    dimAnimator.start()
  }

  override fun onPageExpanded() {}

  override fun onPageCollapsed() {}
}
