package me.saket.inboxrecyclerview

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
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

      val page = expandablePage
      if (page != null) {
        old.onDetachRecyclerView()
        value.onAttachRecyclerView(this, page)
      }
    }

  /** Controls how items are dimmed when the page is expanding/collapsing. */
  var dimPainter: DimPainter = DimPainter.none()
    set(value) {
      val old = field
      field = value

      val page = expandablePage
      if (page != null) {
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

  /**
   * The expandable page to be used with this list.
   * Setting it to null will reset the older page's state.
   */
  var expandablePage: ExpandablePageLayout? = null
    set(newPage) {
      val oldPage = field
      field = newPage

      if (oldPage == null) {
        restorer.restoreIfPossible()
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
      }
    }

  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var isFullyCoveredByPage: Boolean = false
  private val restorer = StateRestorer(this)
  private val locationOnScreenBuffer = IntArray(2)
  internal var dimDrawable: Drawable? = null

  init {
    // Because setters don't get called for default values.
    itemExpandAnimator = ItemExpandAnimator.split()
    dimPainter = DimPainter.listAndPage()
  }

  override fun dispatchDraw(canvas: Canvas) {
    itemExpandAnimator.transformRecyclerViewCanvas(this, canvas) {
      super.dispatchDraw(canvas)
    }
    dimDrawable?.setBounds(left, top, right, bottom)
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
    val page = expandablePage
    if (page != null) {
      itemExpandAnimator.onDetachRecyclerView()
      dimPainter.onDetachRecyclerView()
    }
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
   * @param itemId ID of the item to expand.
   */
  @JvmOverloads
  fun expandItem(
    itemId: Long,
    immediate: Boolean = false
  ) {
    val page = ensureSetup(expandablePage)

    if (isLaidOut.not()) {
      post { expandItem(itemId, immediate) }
      return
    }

    if (!page.isCollapsed) {
      // Expanding an item while another is already
      // expanding results in unpredictable animation.
      return
    }

    val adapter = adapter!!
    var itemAdapterPosition: Int = -1
    for (i in 0 until adapter.itemCount) {
      if (adapter.getItemId(i) == itemId) {
        itemAdapterPosition = i
        break
      }
    }

    val itemView: View? =
      (layoutManager as LinearLayoutManager).findViewByPosition(itemAdapterPosition)

    if (itemView == null) {
      // View got removed right when it was clicked to expand.
      expandFromTop(immediate)
      return
    }

    val itemViewPosition = indexOfChild(itemView)
    val itemRect = itemView.locationOnScreen(locationOnScreenBuffer)

    expandedItem = ExpandedItem(itemViewPosition, itemId, itemRect)
    if (immediate) {
      page.expandImmediately()
    } else {
      page.expand(expandedItem)
    }
  }

  /**
   * Expand from the top, pushing all items out of the window towards the bottom.
   */
  @JvmOverloads
  fun expandFromTop(immediate: Boolean = false) {
    val page = ensureSetup(expandablePage)

    if (isLaidOut.not()) {
      post { expandFromTop(immediate) }
      return
    }

    if (page.isExpandedOrExpanding) {
      return
    }

    expandedItem = ExpandedItem.EMPTY.copy(locationOnScreen = Rect(left, top, right, top))

    if (immediate) {
      page.expandImmediately()
    } else {
      page.expand(expandedItem)
    }
  }

  fun collapse() {
    val page = ensureSetup(expandablePage)

    if (page.isCollapsedOrCollapsing.not()) {
      page.collapse(expandedItem)
    }
  }

  override fun onPageAboutToExpand() {
    isLayoutFrozen = true
  }

  override fun onPageAboutToCollapse() {
    isLayoutFrozen = false
    onPageBackgroundVisible()
  }

  override fun onPageCollapsed() {
    expandedItem = ExpandedItem.EMPTY
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
    ensureStableIds(adapter)
    val isLayoutFrozenBak = isLayoutFrozen
    super.setAdapter(adapter)

    // isLayoutFrozen is reset when the adapter is changed.
    isLayoutFrozen = isLayoutFrozenBak

    restorer.restoreIfPossible()
  }

  override fun swapAdapter(
    adapter: Adapter<*>?,
    removeAndRecycleExistingViews: Boolean
  ) {
    ensureStableIds(adapter)
    val isLayoutFrozenBak = isLayoutFrozen
    super.swapAdapter(adapter, removeAndRecycleExistingViews)

    isLayoutFrozen = isLayoutFrozenBak
    restorer.restoreIfPossible()
  }

  private fun ensureStableIds(adapter: Adapter<*>?) {
    adapter?.apply {
      if (isInEditMode.not() && hasStableIds().not()) {
        throw AssertionError(
            "$adapter needs to have stable IDs so that the expanded item can be restored across " +
                "orientation changes. Unlike adapter positions, IDs remain unchanged across " +
                "data-set updates."
        )
      }
    }
  }

  @Parcelize
  data class ExpandedItem(
    // Index of the currently expanded item's
    // View. This is not the adapter index.
    val viewIndex: Int,

    // Adapter ID of the currently expanded item.
    val adapterId: Long,

    // Original location of the currently expanded item.
    // Used for restoring states after collapsing.
    val locationOnScreen: Rect

  ) : Parcelable {

    internal fun isEmpty(): Boolean {
      return viewIndex == -1
          && adapterId == -1L
          && locationOnScreen.width() == 0
          && locationOnScreen.height() == 0
    }

    companion object {
      internal val EMPTY =
        ExpandedItem(adapterId = -1, viewIndex = -1, locationOnScreen = Rect(0, 0, 0, 0))
    }
  }
}

// Only used for debugging.
internal const val ANIMATION_START_DELAY = 0L
