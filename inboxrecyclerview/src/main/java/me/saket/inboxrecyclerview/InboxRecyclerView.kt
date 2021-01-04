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
import androidx.core.view.doOnDetach
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.InternalPageCallbacks.NoOp
import me.saket.inboxrecyclerview.animation.ItemExpandAnimator
import me.saket.inboxrecyclerview.dimming.DimPainter
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
        old.onDetachRecyclerView()
        field.onAttachRecyclerView(this, page)
      }
    }

  @Deprecated("Use dimPainter instead", ReplaceWith("dimPainter"))
  var tintPainter: DimPainter
    get() = dimPainter
    set(value) {
      dimPainter = value
    }

  /** Details about the currently expanded item. */
  var expandedItem: ExpandedItem = ExpandedItem.EMPTY

  /** See [ExpandedItemFinder]. */
  var expandedItemFinder: ExpandedItemFinder? = DefaultExpandedItemFinder(requireStableIds = true)

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

      if (oldPage == null) {
        restorer.restoreIfPossible()
      }

      // The old page may have gotten removed midway a collapse animation,
      // causing this list's layout to be stuck as disabled. Clear it here.
      if (newPage == null || newPage.isCollapsedOrCollapsing) {
        suppressLayout(false)
      }

      if (oldPage != null) {
        dimPainter.onDetachRecyclerView()
        itemExpandAnimator.onDetachRecyclerView()
        oldPage.internalStateCallbacksForRecyclerView = NoOp()
      }

      if (newPage != null) {
        dimPainter.onAttachRecyclerView(this, newPage)
        itemExpandAnimator.onAttachRecyclerView(this, newPage)
        newPage.internalStateCallbacksForRecyclerView = this

        newPage.doOnDetach {
          this.expandablePage = null
        }
      }
    }

  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var isFullyCoveredByPage: Boolean = false
  private val restorer = StateRestorer(this)

  /** Used by [DimPainter]. */
  var dimDrawable: Drawable? = null

  init {
    // Because setters don't get called for default values.
    itemExpandAnimator = ItemExpandAnimator.split()
    dimPainter = DimPainter.listAndPage()
  }

  override fun dispatchDraw(canvas: Canvas) {
    itemExpandAnimator.transformRecyclerViewCanvas(this, canvas) {
      super.dispatchDraw(canvas)
    }
    dimDrawable?.setBounds(0, 0, width, height)
    dimDrawable?.draw(canvas)
  }

  override fun onSaveInstanceState(): Parcelable {
    return restorer.save(super.onSaveInstanceState())
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    val superState = restorer.restore(state)
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
   * @param adapterId Adapter ID of the item to expand.
   */
  @JvmOverloads
  fun expandItem(adapterId: Long, immediate: Boolean = false) {
    expandInternal(itemId = DefaultExpandedItemId(adapterId = adapterId), immediate = immediate)
  }

  /**
   * @param itemId Identifier of the expanding item that can be used for finding the expanding
   * item's location on screen (across state restorations) using [expandedItemFinder].
   */
  @JvmOverloads
  fun expandItem(itemId: Parcelable, immediate: Boolean = false) {
    expandInternal(itemId, immediate)
  }

  /**
   * Expand from the top, pushing all items out of the window towards the bottom.
   */
  @JvmOverloads
  fun expandFromTop(immediate: Boolean = false) {
    expandInternal(itemId = null, immediate = immediate)
  }

  private fun expandInternal(itemId: Parcelable?, immediate: Boolean) {
    val page = ensureSetup(expandablePage)

    if (isLaidOut.not() || page.isLaidOut.not()) {
      post { expandInternal(itemId, immediate) }
      return
    }

    if (!page.isCollapsed) {
      // Expanding an item while another is already
      // expanding results in unpredictable animation.
      if (!expandedItem.isNotEmpty()) {
        // Useful if the page was expanded immediately as a result of a (manual)
        // state restoration before this RecyclerView could restore its state.
        expandedItem = captureExpandedItemInfo(itemId)
      }
      return
    }

    expandedItem = captureExpandedItemInfo(itemId)
    if (immediate) {
      page.expandImmediately()
    } else {
      page.expand(expandedItem)
    }
  }

  /**
   * InboxRecyclerView rejects duplicate calls to [expandItem] if the page is already expanded.
   * If the expanded item still needs to be updated for some reason (for eg., if the page was
   * immediately expanded from an arbitrary location earlier but can now collapse to an actual
   * list item), this can be used.
   */
  fun forceUpdateExpandedItem(itemId: Parcelable) {
    expandedItem = captureExpandedItemInfo(itemId)
  }

  fun collapse() {
    val page = ensureSetup(expandablePage)

    // List items may have changed while the page was
    // expanded. Find the expanded item's location again.
    expandedItem = captureExpandedItemInfo(itemId = expandedItem.id)

    if (page.isCollapsedOrCollapsing.not()) {
      page.collapse(expandedItem)
    }
  }

  private fun captureExpandedItemInfo(itemId: Parcelable?): ExpandedItem {
    itemId?.let(::checkHasSupportingItemFinder)

    val findResult = itemId?.let { expandedItemFinder?.findExpandedItem(this, it) }
    val itemView = findResult?.itemView

    return if (itemView != null) {
      ExpandedItem(
          id = itemId,
          viewIndex = indexOfChild(itemView),
          // Ignore translations done by the item expand animator.
          locationOnScreen = itemView.locationOnScreen(ignoreTranslations = true)
      )

    } else {
      val locationOnScreen = locationOnScreen()
      val paddedY = locationOnScreen.top + paddingTop // This is where list items will be laid out from.
      ExpandedItem(
          id = itemId,
          viewIndex = -1,
          locationOnScreen = Rect(locationOnScreen.left, paddedY, locationOnScreen.right, paddedY)
      )
    }
  }

  private fun checkHasSupportingItemFinder(itemId: Parcelable) {
    if (itemId !is DefaultExpandedItemId && expandedItemFinder is DefaultExpandedItemFinder) {
      "A custom InboxRecyclerView#expandedItemFinder must be set that's capable of identifying expanded " +
          "item ID of type ${itemId::class.java}."
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
    expandedItem = ExpandedItem.EMPTY
  }

  override fun onPagePullStarted() {
    // List items may have changed while the page was expanded. Find the expanded item's location again.
    expandedItem = captureExpandedItemInfo(itemId = expandedItem.id)
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
  fun optimizeActivityBackgroundOverdraw(activity: Activity) {
    activityWindow = activity.window
    activityWindowOrigBackground = activity.window.decorView.background
  }

  override fun setAdapter(adapter: Adapter<*>?) {
    val wasLayoutSuppressed = isLayoutSuppressed
    super.setAdapter(adapter)
    suppressLayout(wasLayoutSuppressed) // isLayoutSuppressed is reset when the adapter is changed.

    restorer.restoreIfPossible()
  }

  override fun swapAdapter(
    adapter: Adapter<*>?,
    removeAndRecycleExistingViews: Boolean
  ) {
    val wasLayoutSuppressed = isLayoutSuppressed
    super.swapAdapter(adapter, removeAndRecycleExistingViews)
    suppressLayout(wasLayoutSuppressed) // isLayoutSuppressed is reset when the adapter is changed.

    restorer.restoreIfPossible()
  }

  @Parcelize
  data class ExpandedItem(
    // Index of the currently expanded item's
    // View. This is not the adapter index.
    val viewIndex: Int,

    // Adapter ID of the currently expanded item.
    val id: Parcelable?,

    // Original location of the currently expanded item.
    // Used for restoring states after collapsing.
    val locationOnScreen: Rect
  ) : Parcelable {

    internal fun isNotEmpty(): Boolean {
      return viewIndex != -1
          && id != null
          && locationOnScreen.width() != 0
          && locationOnScreen.height() != 0
    }

    companion object {
      internal val EMPTY =
        ExpandedItem(id = null, viewIndex = -1, locationOnScreen = Rect(0, 0, 0, 0))
    }
  }
}

// Only used for debugging.
internal const val ANIMATION_START_DELAY = 0L
