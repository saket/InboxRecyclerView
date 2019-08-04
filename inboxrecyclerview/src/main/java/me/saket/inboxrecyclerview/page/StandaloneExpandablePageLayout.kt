package me.saket.inboxrecyclerview.page

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem

/** Standalone because this page can live without an [InboxRecyclerView]. */
class StandaloneExpandablePageLayout(
    context: Context,
    attrs: AttributeSet? = null
) : ExpandablePageLayout(context, attrs) {

  internal interface Callbacks {

    /**
     * Page has fully collapsed and is no longer visible.
     */
    fun onPageCollapsed()

    /**
     * Page was released while being pulled.
     *
     * @param collapseEligible Whether the page was pulled enough for collapsing it.
     */
    fun onPageRelease(collapseEligible: Boolean)
  }

  internal lateinit var callbacks: Callbacks

  init {
    collapsedAlpha = 1F
    animationDurationMillis = 300

    addOnPullListener(object : SimpleOnPullListener() {
      override fun onRelease(collapseEligible: Boolean) {
        callbacks.onPageRelease(collapseEligible)
      }
    })

    addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageCollapsed(page: ExpandablePageLayout) {
        callbacks.onPageCollapsed()
      }
    })
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)

    if (isInEditMode) {
      expandImmediately()
      setClippedDimensions(r, b)
    }
  }

  /**
   * Expands this page with animation so that it fills the whole screen.
   *
   * @param fromShapeRect Initial dimensions of this page.
   */
  internal fun expandFrom(fromShapeRect: Rect) {
    setClippedDimensions(width, 0)
    expand(ExpandedItem(-1, -1, fromShapeRect))
  }

  /**
   * @param toShapeRect Final dimensions of this page, when it fully collapses.
   */
  internal fun collapseTo(toShapeRect: Rect) {
    collapse(ExpandedItem(-1, -1, toShapeRect))
  }
}
