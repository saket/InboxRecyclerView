package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
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
open class UncoveredAreaTintPainter(
  private val color: Int,
  private val opacity: Float
) : TintPainter() {

  private lateinit var onDetach: () -> Unit
  private lateinit var onDrawTint: (Canvas) -> Unit

  override fun onAttachRecyclerView(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    val callbacks = createCallbacks(recyclerView, page)
    val changeDetector = PageLocationChangeDetector(page, changeListener = callbacks::onPageMove)

    page.viewTreeObserver.addOnGlobalLayoutListener(changeDetector)
    page.viewTreeObserver.addOnPreDrawListener(changeDetector)
    page.addStateChangeCallbacks(callbacks)

    onDrawTint = { canvas ->
      callbacks.drawTint(canvas)
    }

    onDetach = {
      callbacks.reset()
      page.removeStateChangeCallbacks(callbacks)
      page.viewTreeObserver.removeOnGlobalLayoutListener(changeDetector)
      page.viewTreeObserver.removeOnPreDrawListener(changeDetector)
    }
  }

  override fun onDetachRecyclerView() {
    onDetach()
  }

  override fun drawTint(canvas: Canvas) {
    onDrawTint(canvas)
  }

  protected open fun createCallbacks(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) =
    StateChangeCallbacks(recyclerView, page, color, opacity)

  open class StateChangeCallbacks(
    private val recyclerView: InboxRecyclerView,
    private val page: ExpandablePageLayout,
    private val color: Int,
    opacity: Float
  ) : PageStateChangeCallbacks {

    private val minIntensity = 0
    private val maxIntensity = (255 * opacity).toInt()    // [0..255]

    private var tintAnimator: ValueAnimator = ObjectAnimator()
    private var lastIsCollapseEligible = false

    protected val tintPaint: Paint = Paint().apply {
      flags = ANTI_ALIAS_FLAG
      color = this@StateChangeCallbacks.color
      alpha = minIntensity
    }

    fun onPageMove() {
      // Remove dimming when the page is being pulled and is eligible for collapse.
      if (page.isExpanded) {
        val collapseThreshold = page.pullToCollapseThresholdDistance
        val translationYAbs = abs(page.translationY)
        val isCollapseEligible = translationYAbs >= collapseThreshold

        if (isCollapseEligible != lastIsCollapseEligible) {
          animateDimming(
              toAlpha = if (isCollapseEligible) minIntensity else maxIntensity,
              dimDuration = 300
          )
        }
        lastIsCollapseEligible = isCollapseEligible

      } else {
        lastIsCollapseEligible = false
      }
    }

    open fun drawTint(canvas: Canvas) {
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

    fun reset() {
      tintAnimator.cancel()
    }

    private fun animateDimming(toAlpha: Int, dimDuration: Long) {
      tintAnimator = ObjectAnimator.ofInt(tintPaint.alpha, toAlpha).apply {
        duration = dimDuration
        interpolator = page.animationInterpolator
        startDelay = ANIMATION_START_DELAY
      }
      tintAnimator.addUpdateListener {
        tintPaint.alpha = it.animatedValue as Int
        recyclerView.postInvalidate()
      }
      tintAnimator.start()
    }

    override fun onPageExpanded() = Unit

    override fun onPageCollapsed() = Unit
  }
}
