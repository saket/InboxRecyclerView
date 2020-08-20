package me.saket.inboxrecyclerview.dimming

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.graphics.Canvas
import android.graphics.ColorFilter
import android.graphics.Paint
import android.graphics.Paint.ANTI_ALIAS_FLAG
import android.graphics.PixelFormat
import android.graphics.drawable.Drawable
import android.os.Build.VERSION_CODES
import androidx.annotation.RequiresApi
import androidx.recyclerview.widget.RecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSING
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDING
import kotlin.math.abs

/**
 * TODO: doc.
 * TODO: migration guide
 */
@Suppress("LeakingThis")
@RequiresApi(VERSION_CODES.M)
internal class CollapseEligibleDimPainter(
  override val color: Int,
  override val opacity: Float
) : TintPainter() {

  private var listDimAnimator = ValueAnimator()
  private var pageDimAnimator = ValueAnimator()

  class DimDrawable(private val page: ExpandablePageLayout, color: Int) : Drawable() {
    private val paint = Paint(ANTI_ALIAS_FLAG).also { it.color = color }

    override fun draw(canvas: Canvas) {
      val pageTop = page.translationY
      val pageBottom = page.translationY + page.clippedDimens.height()

      // Dim above the page.
      canvas.drawRect(0f, 0f, page.width.toFloat(), pageTop, paint)

      // Dim below the page.
      //canvas.drawRect(0f, pageBottom, page.width.toFloat(), page.height.toFloat(), paint)
    }

    override fun setAlpha(alpha: Int) {
      paint.alpha = alpha
    }

    override fun getOpacity() = when (paint.alpha) {
      255, 0 -> PixelFormat.OPAQUE
      else -> PixelFormat.TRANSLUCENT
    }

    override fun setColorFilter(colorFilter: ColorFilter?): Unit = TODO()
  }

  private var lastListDimAlpha = -1f

  override fun onPageMove(rv: RecyclerView, page: ExpandablePageLayout) {
    if (page.foreground == null) {
      page.foreground = DimDrawable(page, color)
    }

    val listDimAlpha = when (page.currentState) {
      COLLAPSING, COLLAPSED -> 0f
      EXPANDING -> opacity
      EXPANDED -> if (page.isCollapseEligible) 0f else opacity
    }
    if (lastListDimAlpha != listDimAlpha) {
      ObjectAnimator.ofFloat(lastListDimAlpha, listDimAlpha).apply {
        duration = page.animationDurationMillis
        addUpdateListener {
          page.foreground.alpha = (255 * it.animatedValue as Float).toInt()
          page.foreground.invalidateSelf()
          println("Animating to ${page.foreground.alpha}")
        }
        start()
      }
      lastListDimAlpha = listDimAlpha
    }
  }

  override fun cancelAnimation() {
    listDimAnimator.cancel()
    pageDimAnimator.cancel()
  }

  private val ExpandablePageLayout.isCollapseEligible
    get() = abs(translationY) >= pullToCollapseThresholdDistance
}
