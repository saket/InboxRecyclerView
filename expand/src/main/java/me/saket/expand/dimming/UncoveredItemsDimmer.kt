package me.saket.expand.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.expand.InboxRecyclerView
import me.saket.expand.page.ExpandablePageLayout
import me.saket.expand.page.PageStateChangeCallbacks

private const val MIN_DIM = 0
private const val MAX_DIM_FACTOR = 0.1F                       // [0..1]
private const val MAX_DIM = (255 * MAX_DIM_FACTOR).toInt()    // [0..255]

/**
 * Draws dimming on [InboxRecyclerView] only in the area that's not covered by its page.
 * This allows the page content to not have another background of its own, thus reducing
 * overdraw by a level.
 *
 * If the dimming appears incorrect, try using [ItemDimmer.allItems] instead.
 */
open class UncoveredItemsDimmer : ItemDimmer(), PageStateChangeCallbacks {

  private var dimAnimator: ValueAnimator = ObjectAnimator()
  protected val dimPaint: Paint = Paint(Paint.ANTI_ALIAS_FLAG)

  init {
    dimPaint.color = Color.BLACK
    dimPaint.alpha = MIN_DIM
  }

  private val pagePreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
    private var lastTranslationY = 0F
    private var lastClippedRect = Rect()
    private var lastState = ExpandablePageLayout.PageState.COLLAPSED

    override fun onPreDraw(): Boolean {
      if (lastTranslationY != page.translationY || lastClippedRect != page.clippedRect || lastState != page.currentState) {
        onPageMove()
      }

      lastTranslationY = page.translationY
      lastClippedRect = page.clippedRect
      lastState = page.currentState
      return true
    }
  }

  private val pageLayoutChangeListener = {
    // Changes in the page's dimensions will get handled here.
    onPageMove()
  }

  override fun onPageDetached(page: ExpandablePageLayout) {
    dimAnimator.cancel()
    page.removeStateChangeCallbacks(this)
    page.viewTreeObserver.removeOnGlobalLayoutListener(pageLayoutChangeListener)
    page.viewTreeObserver.removeOnPreDrawListener(pagePreDrawListener)
  }

  override fun onPageAttached() {
    page.addStateChangeCallbacks(this)
    page.viewTreeObserver.addOnGlobalLayoutListener(pageLayoutChangeListener)
    page.viewTreeObserver.addOnPreDrawListener(pagePreDrawListener)
  }

  private fun onPageMove() {
    recyclerView.postInvalidate()
  }

  override fun drawDimming(canvas: Canvas) {
    val pageCopy = this.page

    recyclerView.apply {
      // Content above the page.
      canvas.drawRect(0F, 0F, right.toFloat(), pageCopy.translationY, dimPaint)

      // Content below the page.
      if (pageCopy.isExpanded) {
        canvas.drawRect(0F, (bottom + pageCopy.translationY), right.toFloat(), bottom.toFloat(), dimPaint)

      } else if (pageCopy.isExpandingOrCollapsing) {
        val pageBottom = pageCopy.translationY + pageCopy.clippedRect.height().toFloat()
        canvas.drawRect(0F, pageBottom, right.toFloat(), bottom.toFloat(), dimPaint)
      }
    }
  }

  override fun onPageAboutToExpand(expandAnimDuration: Long) {
    dimAnimator.cancel()
    animateDimming(MAX_DIM, expandAnimDuration)
  }

  override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
    dimAnimator.cancel()
    animateDimming(MIN_DIM, collapseAnimDuration)
  }

  private fun animateDimming(toAlpha: Int, dimDuration: Long) {
    dimAnimator = ObjectAnimator.ofInt(dimPaint.alpha, toAlpha).apply {
      duration = dimDuration
      interpolator = page.animationInterpolator
      startDelay = InboxRecyclerView.animationStartDelay.toLong()
    }
    dimAnimator.addUpdateListener {
      dimPaint.alpha = it.animatedValue as Int
    }
    dimAnimator.start()
  }

  override fun onPageExpanded() {}

  override fun onPageCollapsed() {}
}
