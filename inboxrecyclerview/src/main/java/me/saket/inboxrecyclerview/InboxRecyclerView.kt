package me.saket.inboxrecyclerview

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.Window
import androidx.core.view.doOnLayout
import me.saket.inboxrecyclerview.InternalPageCallbacks.NoOp
import me.saket.inboxrecyclerview.animation.ItemExpandAnimator
import me.saket.inboxrecyclerview.dimming.AnimatedVisibilityColorDrawable
import me.saket.inboxrecyclerview.dimming.DimPainter
import me.saket.inboxrecyclerview.expander.AdapterIdBasedItem
import me.saket.inboxrecyclerview.expander.AdapterIdBasedItemExpander
import me.saket.inboxrecyclerview.expander.InboxItemExpander
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * A RecyclerView where items can expand and collapse to and from an [ExpandablePageLayout].
 */
@Suppress("LeakingThis")
open class InboxRecyclerView @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : ScrollSuppressibleRecyclerView(context, attrs), InternalPageCallbacks {

  /** Controls how [InboxRecyclerView] items are animated when the page is moving. */
  var itemExpandAnimator: ItemExpandAnimator = ItemExpandAnimator.split()
    set(value) {
      val old = field
      field = value

      expandablePage?.let { page ->
        old.onDetachRecyclerView()
        value.onAttachRecyclerView(this, page)
      }
    }

  /** Controls how items are dimmed when the page is expanding/collapsing. */
  var dimPainter: DimPainter = DimPainter.none()
    set(value) {
      val old = field
      field = value

      expandablePage?.let { page ->
        old.onDetachRecyclerView(resetDim = false)
        field.onAttachRecyclerView(this, page)
      }
    }

  @Suppress("unused")
  @Deprecated("Use dimPainter instead", ReplaceWith("dimPainter"))
  var tintPainter: DimPainter
    get() = dimPainter
    set(value) { dimPainter = value }

  /** Details about the currently expanded item. */
  var expandedItemLoc: ExpandedItemLocation = ExpandedItemLocation.EMPTY

  @Suppress("unused")
  @Deprecated("Use expandedItemLoc instead", ReplaceWith("expandedItemLoc"))
  var expandedItem: ExpandedItemLocation
    get() = expandedItemLoc
    set(value) { expandedItemLoc = value }

  /** See [InboxItemExpander]. */
  var itemExpander: InboxItemExpander<*> = AdapterIdBasedItemExpander(requireStableIds = true)
    set(value) {
      field = value
      field.recyclerView = this
    }

  /**
   * The expandable page to be used with this list.
   */
  var expandablePage: ExpandablePageLayout? = null
    set(newPage) {
      val oldPage = field
      if (oldPage === newPage) {
        return
      }

      field = newPage

      // The old page may have gotten removed midway a collapse animation,
      // causing this list's layout to be stuck as disabled. Clear it here.
      if (newPage == null || newPage.isCollapsedOrCollapsing) {
        suppressLayout(false)
      }

      if (oldPage != null) {
        dimPainter.onDetachRecyclerView(resetDim = true)
        itemExpandAnimator.onDetachRecyclerView()
        oldPage.internalStateCallbacksForRecyclerView = NoOp()
      }

      if (newPage != null) {
        dimPainter.onAttachRecyclerView(this, newPage)
        itemExpandAnimator.onAttachRecyclerView(this, newPage)
        newPage.internalStateCallbacksForRecyclerView = this
      }
    }

  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var isFullyCoveredByPage: Boolean = false

  /** Used by [DimPainter]. */
  var dimDrawable: AnimatedVisibilityColorDrawable? = null
    internal set

  init {
    // Because setters don't get called for default values.
    itemExpandAnimator = ItemExpandAnimator.split()
    dimPainter = DimPainter.listAndPage()
    itemExpander = itemExpander
  }

  override fun dispatchDraw(canvas: Canvas) {
    itemExpandAnimator.transformRecyclerViewCanvas(this, canvas) {
      super.dispatchDraw(canvas)
    }
    dimDrawable?.setBounds(0, 0, width, height)
    dimDrawable?.draw(canvas)
  }

  override fun onSaveInstanceState(): Parcelable {
    return itemExpander.saveState(super.onSaveInstanceState())
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    val superState = itemExpander.restoreState(state)
    super.onRestoreInstanceState(superState)
  }

  override fun onDetachedFromWindow() {
    expandablePage = null
    super.onDetachedFromWindow()
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    if (expandablePage?.isCollapsed == false) {
      // Don't let the user open another item midway an animation.
      return true
    }

    val dispatched = super.dispatchTouchEvent(ev)
    return if (expandablePage?.isExpanded == true) {
      // Intentionally leak touch events behind just in case the content page has
      // a lower z-index than than this list. This is an ugly hack, but I cannot
      // think of a way to enforce view positions. Fortunately this hack will not
      // have any effect when the page is positioned at a higher z-index, where
      // it'll consume all touch events before they even reach this list.
      false

    } else {
      dispatched
    }
  }

  private fun ensureSetup(page: ExpandablePageLayout?): ExpandablePageLayout {
    requireNotNull(page) { "Did you forget to set InboxRecyclerView#expandablePage?" }
    requireNotNull(adapter) { "Adapter isn't attached yet!" }
    return page
  }

  /**
   * Expand an item by its adapter-ID.
   *
   * InboxRecyclerView uses adapter-IDs by default for expanding/collapsing items,
   * but you can choose to use any [Parcelable] object by using a custom item expander.
   */
  @JvmOverloads
  fun expandItem(adapterId: Long, immediate: Boolean = false) {
    val expander = itemExpander
    check(expander is AdapterIdBasedItemExpander) {
      "Can't expand an item by its adapter ID if a custom InboxItemExpander is set. " +
          "Call expandItem on your InboxItemExpander instead."
    }
    expander.expandItem(AdapterIdBasedItem(adapterId), immediate)
  }

  /**
   * Expand the page from the top.
   *
   * InboxRecyclerView uses adapter-IDs by default for expanding/collapsing items,
   * but you can choose to use any [Parcelable] object by using a custom item expander.
   */
  @JvmOverloads
  fun expandFromTop(immediate: Boolean = false) {
    itemExpander.expandItem(null, immediate)
  }

  internal fun expandOnceLaidOut(immediate: Boolean) {
    val page = ensureSetup(expandablePage)
    doOnLayout {
      page.doOnLayout {
        expandInternal(immediate)
      }
    }
  }

  private fun expandInternal(immediate: Boolean) {
    val page = expandablePage!!
    if (!page.isCollapsed) {
      // Expanding an item while another is already
      // expanding results in unpredictable animation.
      if (!expandedItemLoc.isNotEmpty()) {
        // Useful if the page was expanded immediately as a result of a (manual)
        // state restoration before this RecyclerView could restore its state.
        expandedItemLoc = itemExpander.captureExpandedItemInfo()
      }
      return
    }

    expandedItemLoc = itemExpander.captureExpandedItemInfo()
    if (immediate) {
      page.expandImmediately()
    } else {
      page.expand(expandedItemLoc)
    }
  }

  fun collapse() {
    val page = ensureSetup(expandablePage)

    if (page.isCollapsedOrCollapsing.not()) {
      // List items may have changed while the page was
      // expanded. Find the expanded item's location again.
      expandedItemLoc = itemExpander.captureExpandedItemInfo()
      page.collapse(expandedItemLoc)
    }
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)

    // This is kind of a hack, but I want the layout to be frozen only after this list
    // has processed its initial batch of child Views. Otherwise this list stays empty
    // after a state restoration, until the page is collapsed.
    if (isLaidOut && childCount > 0 && expandablePage?.isExpandedOrExpanding == true) {
      suppressLayout(true)
    }
  }

  override fun onPageAboutToCollapse() {
    onPageBackgroundVisible()
  }

  override fun onPageCollapsed() {
    suppressLayout(false)
    expandedItemLoc = ExpandedItemLocation.EMPTY
    itemExpander.setItem(null)
  }

  override fun onPagePullStarted() {
    // List items may have changed while the page was expanded. Find the expanded item's location again.
    expandedItemLoc = itemExpander.captureExpandedItemInfo()
  }

  override fun onPagePull(deltaY: Float) {
    onPageBackgroundVisible()
  }

  override fun onPageRelease(collapseEligible: Boolean) {
    if (collapseEligible) {
      collapse()
    }
  }

  override fun onPageFullyCovered() {
    val invalidate = !isFullyCoveredByPage
    isFullyCoveredByPage = true
    if (invalidate) {
      invalidate()
    }

    activityWindow?.setBackgroundDrawable(null)
  }

  private fun onPageBackgroundVisible() {
    isFullyCoveredByPage = false
    invalidate()

    activityWindow?.setBackgroundDrawable(activityWindowOrigBackground)
  }

  override fun canScrollProgrammatically(): Boolean {
    val page = expandablePage
    return page == null || page.isCollapsed
  }

  override fun onDrawForeground(canvas: Canvas) {
    // If an item is expanded, its z-index changes from lower than
    // that of the scrollbars to a higher value and looks a bit abrupt.
    maybeHideScrollbarsAndRun {
      super.onDrawForeground(canvas)
    }
  }

  private inline fun maybeHideScrollbarsAndRun(crossinline run: () -> Unit) {
    if (expandablePage?.isCollapsed == true
        || scrollBarStyle != SCROLLBARS_INSIDE_OVERLAY
    ) {
      run()
      return
    }

    val wasVerticalEnabled = isVerticalScrollBarEnabled
    val wasHorizEnabled = isHorizontalScrollBarEnabled
    isVerticalScrollBarEnabled = false
    isHorizontalScrollBarEnabled = false
    run()
    isVerticalScrollBarEnabled = wasVerticalEnabled
    isHorizontalScrollBarEnabled = wasHorizEnabled
  }

  /**
   * Experimental: Reduce overdraw by 1 level by removing the Activity Window's
   * background when the [ExpandablePageLayout] is expanded. No point in drawing
   * it when it's not visible to the user.
   **/
  // TODO: deprecate.
  fun optimizeActivityBackgroundOverdraw(activity: Activity) {
    activityWindow = activity.window
    activityWindowOrigBackground = activity.window.decorView.background
  }

  override fun setAdapter(adapter: Adapter<*>?) {
    val wasLayoutSuppressed = isLayoutSuppressed
    super.setAdapter(adapter)
    suppressLayout(wasLayoutSuppressed) // isLayoutSuppressed is reset when the adapter is changed.
  }

  override fun swapAdapter(
    adapter: Adapter<*>?,
    removeAndRecycleExistingViews: Boolean
  ) {
    val wasLayoutSuppressed = isLayoutSuppressed
    super.swapAdapter(adapter, removeAndRecycleExistingViews)
    suppressLayout(wasLayoutSuppressed) // isLayoutSuppressed is reset when the adapter is changed.
  }

  data class ExpandedItemLocation(
      // Index of the currently expanded item's
      // View. This is not the adapter index.
    val viewIndex: Int,

      // Original location of the currently expanded item.
      // Used for restoring states after collapsing.
    val locationOnScreen: Rect
  ) {

    internal fun isEmpty(): Boolean = this == EMPTY
    internal fun isNotEmpty(): Boolean = !isEmpty()

    companion object {
      internal val EMPTY = ExpandedItemLocation(viewIndex = -1, locationOnScreen = Rect(0, 0, 0, 0))
    }
  }
}

// Only used for debugging.
internal const val ANIMATION_START_DELAY = 0L
