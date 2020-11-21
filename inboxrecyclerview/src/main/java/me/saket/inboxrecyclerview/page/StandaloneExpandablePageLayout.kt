package me.saket.inboxrecyclerview.page

import android.content.Context
import android.graphics.Rect
import android.util.AttributeSet
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem
import me.saket.inboxrecyclerview.PullCollapsibleActivity

/**
 * An expandable page that can live without an accompanying [InboxRecyclerView].
 * Can be used for making pull-collapsible screens where using [PullCollapsibleActivity]
 * isn't an option.
 *
 * Usage:
 *
 * ```
 * val pageLayout = findViewById<StandaloneExpandablePageLayout>(...)
 * pageLayout.expandImmediately()
 * pageLayout.onPageRelease = { collapseEligible ->
 *   if (collapseEligible) {
 *     exitWithAnimation()
 *   }
 * }
 * ```
 *
 * where `exitWithAnimation()` can be used for playing your own exit
 * animation, or for playing the page collapse animation.
 *
 * ```
 * pageLayout.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
 *   override fun onPageCollapsed() {
 *     exit()
 *   }
 * })
 * pageLayout.collapseTo(...)
 * ```
 */
open class StandaloneExpandablePageLayout(
    context: Context,
    attrs: AttributeSet? = null
) : ExpandablePageLayout(context, attrs) {

  /**
   * Called when the page was pulled and released.
   *
   * @param collapseEligible Whether the page was pulled enough for collapsing it.
   */
  lateinit var onPageRelease: (collapseEligible: Boolean) -> Unit

  init {
    collapsedContentCoverAlpha = 0F

    addOnPullListener(object : SimpleOnPullListener() {
      override fun onRelease(collapseEligible: Boolean) {
        onPageRelease(collapseEligible)
      }
    })
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    if (::onPageRelease.isInitialized.not()) {
      throw AssertionError("Did you forget to set onPageRelease?")
    }
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)

    if (isInEditMode) {
      expandImmediately()
      setClippedDimensions(r, b)
    }
  }

  /**
   * Expand this page immediately.
   */
  public override fun expandImmediately() {
    super.expandImmediately()
  }

  fun expandFromTop() {
    if (isLaidOut.not()) {
      post { expandFromTop() }
      return
    }

    expand(
        ExpandedItem(
            id = null,
            viewIndex = -1,
            locationOnScreen = Rect(0, 0, width, 0)
        )
    )
  }

  fun collapseToTop() {
    collapse(
        ExpandedItem(
            id = null,
            viewIndex = -1,
            locationOnScreen = Rect(0, 0, width, 0)
        )
    )
  }

  /**
   * Expand this page with animation with `fromShapeRect` as its initial dimensions.
   */
  fun expandFrom(fromShapeRect: Rect) {
    setClippedDimensions(width, 0)
    expand(ExpandedItem(viewIndex = -1, id = null, locationOnScreen = fromShapeRect))
  }

  /**
   * @param toShapeRect Final dimensions of this page, when it fully collapses.
   */
  fun collapseTo(toShapeRect: Rect) {
    collapse(ExpandedItem(viewIndex = -1, id = null, locationOnScreen = toShapeRect))
  }
}
