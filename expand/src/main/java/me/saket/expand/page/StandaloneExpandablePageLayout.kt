package me.saket.expand.page

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import me.saket.expand.InboxRecyclerView
import me.saket.expand.InternalPageCallbacks

/** Standalone because this page can live without an [InboxRecyclerView]. */
class StandaloneExpandablePageLayout(
    context: Context,
    attrs: AttributeSet? = null
) : ExpandablePageLayout(context, attrs) {

  internal interface Callbacks {
    /**
     * Called when this page has fully collapsed and is no longer visible.
     */
    fun onPageFullyCollapsed()

    /**
     * Called when this page was released while being pulled.
     *
     * @param collapseEligible Whether the page was pulled enough for collapsing it.
     */
    fun onPageRelease(collapseEligible: Boolean)
  }

  init {
    setCollapsedAlpha(1f)
    animationDurationMillis = ANIMATION_DURATION_MILLIS
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)

    if (isInEditMode) {
      expandImmediately()
      setClippedDimensions(r, b)
    }
  }

  internal fun setCallbacks(callbacks: Callbacks) {
    super.setInternalStateCallbacksForList(object : InternalPageCallbacks {
      override fun onPageAboutToExpand() {}

      override fun onPageFullyCovered() {}

      override fun onPageAboutToCollapse() {}

      override fun onPageFullyCollapsed() {
        callbacks.onPageFullyCollapsed()
      }

      override fun onPagePull(deltaY: Float) {

      }

      override fun onPageRelease(collapseEligible: Boolean) {
        callbacks.onPageRelease(collapseEligible)
      }
    })
  }

  /**
   * Expands this page with animation so that it fills the whole screen.
   *
   * @param fromShapeRect Initial dimensions of this page.
   */
  internal fun expandFrom(fromShapeRect: Rect) {
    setClippedDimensions(width, 0)
    expand(InboxRecyclerView.ExpandedItem(-1, -1, fromShapeRect))
  }

  /**
   * @param toShapeRect Final dimensions of this page, when it fully collapses.
   */
  internal fun collapseTo(toShapeRect: Rect) {
    collapse(InboxRecyclerView.ExpandedItem(-1, -1, toShapeRect))
  }

  companion object {

    val ANIMATION_DURATION_MILLIS: Long = 300
  }
}
