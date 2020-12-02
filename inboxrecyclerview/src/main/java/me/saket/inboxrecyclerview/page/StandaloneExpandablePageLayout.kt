package me.saket.inboxrecyclerview.page

import android.content.Context
import android.graphics.Rect
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem
import me.saket.inboxrecyclerview.PullCollapsibleActivity
import me.saket.inboxrecyclerview.executeOnMeasure
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.COLLAPSING
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDING

@Parcelize
data class StandaloneState(
  val superState: Parcelable?,
  val state: ExpandablePageLayout.PageState
): Parcelable

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

  override fun onSaveInstanceState(): Parcelable {
    return StandaloneState(
        superState = super.onSaveInstanceState(),
        state = currentState
    )
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    require(state is StandaloneState)
    super.onRestoreInstanceState(state.superState)

    if (state.state == EXPANDED || state.state == EXPANDING) {
      expandImmediately()
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
            locationOnScreen = Rect(left, top, right, top)
        )
    )
  }

  fun collapseToTop() {
    collapse(
        ExpandedItem(
            id = null,
            viewIndex = -1,
            locationOnScreen = Rect(left, top, right, top)
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
