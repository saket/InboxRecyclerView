package me.saket.expand

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import kotlinx.android.parcel.Parcelize
import me.saket.expand.Views.executeOnNextLayout
import me.saket.expand.animation.DefaultItemExpandAnimator
import me.saket.expand.animation.ItemExpandAnimator
import me.saket.expand.dimming.ItemDimmer
import me.saket.expand.page.ExpandablePageLayout

/**
 * A RecyclerView where items can expand and collapse to and from an [ExpandablePageLayout].
 */
class InboxRecyclerView(
    context: Context,
    attrs: AttributeSet
) : ScrollSuppressibleRecyclerView(context, attrs), InternalPageCallbacks {

  /** Controls how [InboxRecyclerView] items are animated when the page is moving. */
  var itemExpandAnimator: ItemExpandAnimator = DefaultItemExpandAnimator()
    set(value) {
      field = value
      if (pageSetupDone) {
        itemDimmer.onAttachRecyclerView(this)
      }
    }

  /** Controls how items are dimmed when the page is expanding/collapsing. */
  var itemDimmer: ItemDimmer = ItemDimmer.uncoveredItems()
    set(value) {
      field = value
      if (pageSetupDone) {
        itemDimmer.onAttachRecyclerView(this)
      }
    }

  /** Details about the currently expanded item. */
  var expandedItem: ExpandedItem = ExpandedItem.EMPTY

  lateinit var page: ExpandablePageLayout
    private set

  private var pageSetupDone: Boolean = false

  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var isFullyCoveredByPage: Boolean = false

  init {
    // For drawing dimming using ItemDimmer.
    setWillNotDraw(false)
  }

  fun saveExpandableState(outState: Bundle) {
    outState.putParcelable(KEY_EXPAND_INFO, expandedItem)
  }

  /**
   * Letting Activities handle restoration manually so that the setup can optionally
   * happen before onRestore gets called.
   * */
  fun restoreExpandableState(savedInstance: Bundle) {
    expandedItem = savedInstance.getParcelable(KEY_EXPAND_INFO) as ExpandedItem
    if (expandedItem.isEmpty().not()) {
      ensureSetup()
      expandImmediately()
    }
  }

  /**
   * Set the [ExpandablePageLayout] and [Toolbar] to be used with this list. The toolbar
   * gets pushed up when the page is expanding.
   *
   * The [toolbar]'s height is also used as the pull-to-collapse distance threshold.
   */
  fun setExpandablePage(expandablePage: ExpandablePageLayout, toolbar: View) {
    setExpandablePageInternal(expandablePage)

    expandablePage.setToolbar(toolbar)
    toolbar.post {
      expandablePage.setPullToCollapseDistanceThreshold((toolbar.height * 0.85F).toInt())
    }
  }

  /**
   * [collapseDistanceThreshold] Minimum Y-distance the page has to be pulled to collapse.
   */
  fun setExpandablePage(expandablePage: ExpandablePageLayout, collapseDistanceThreshold: Int) {
    setExpandablePageInternal(expandablePage)

    expandablePage.setPullToCollapseDistanceThreshold(collapseDistanceThreshold)
  }

  private fun setExpandablePageInternal(expandablePage: ExpandablePageLayout) {
    if (pageSetupDone) {
      throw IllegalStateException("Expandable page is already set.")
    }
    pageSetupDone = true
    page = expandablePage

    expandablePage.setInternalStateCallbacksForList(this)
    itemDimmer.onAttachRecyclerView(this)
    itemExpandAnimator.onAttachRecyclerView(this)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    // The items must maintain their positions, relative to the new bounds. Wait for
    // Android to draw the child Views. Calling getChildCount() right now will return
    // old values (that is, no. of children that were present before this height
    // change happened.
    if (pageSetupDone) {
      executeOnNextLayout {
        if (page.currentState === ExpandablePageLayout.PageState.EXPANDING) {
          page.animatePageExpandCollapse(true, width, height, expandedItem)

        } else if (page.currentState === ExpandablePageLayout.PageState.EXPANDED) {
          page.alignPageToCoverScreen()
        }
      }
    }
  }

  private fun ensureSetup() {
    if (pageSetupDone.not()) {
      throw IllegalAccessError("Did you forget to call InboxRecyclerView.setup()?")
    }
    if (adapter == null) {
      throw AssertionError("Adapter isn't attached yet.")
    }
  }

  /**
   * @param itemPosition Item's position in the adapter.
   */
  fun expandItem(itemPosition: Int, itemId: Long) {
    ensureSetup()

    if (page.isExpandedOrExpanding) {
      return
    }

    val itemView = (layoutManager as LinearLayoutManager).findViewByPosition(itemPosition)
        ?: throw AssertionError("Couldn't find the View for adapter position $itemPosition")

    val itemViewPosition = indexOfChild(itemView)
    val child = getChildAt(itemViewPosition)
    if (child == null) {
      // View got removed right when it was clicked to expand.
    }

    val itemRect = Rect(
        left + child.left,
        top + child.top,
        width - right + child.right,
        top + child.bottom)

    if (itemRect.width() == 0) {
      // Should expand from full width even when expanding from
      // arbitrary location (that is, item to expand is null).
      itemRect.left = left
      itemRect.right = right
    }

    expandedItem = ExpandedItem(itemViewPosition, itemId, itemRect)

    if (!isLaidOut && visibility != View.GONE) {
      expandImmediately()
    } else {
      page.expand(expandedItem)
    }
  }

  /** Expand from the top, pushing all items out of the window towards the bottom. */
  fun expandFromTop() {
    ensureSetup()

    expandedItem = ExpandedItem.EMPTY.copy(expandedItemLocationRect = Rect(left, top, right, top))

    if (!isLaidOut && visibility != View.GONE) {
      expandImmediately()
    } else {
      page.expand(expandedItem)
    }
  }

  /**
   * Expands the page right away and pushes the items out of the list without animations.
   */
  fun expandImmediately() {
    page.expandImmediately()
  }

  fun collapse() {
    ensureSetup()

    if (page.isCollapsedOrCollapsing.not()) {
      val expandInfo = expandedItem
      page.collapse(expandInfo)
    }
  }

  override fun onPageAboutToExpand() {
    isLayoutFrozen = true
  }

  override fun onPageAboutToCollapse() {
    isLayoutFrozen = false
    onPageBackgroundVisible()
  }

  override fun onPageFullyCollapsed() {
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
    isFullyCoveredByPage = true   // Skips draw() until visible again to the user.
    if (invalidate) {
      postInvalidate()
    }

    if (activityWindow != null) {
      activityWindow!!.setBackgroundDrawable(null)
    }
  }

  private fun onPageBackgroundVisible() {
    isFullyCoveredByPage = false
    postInvalidate()

    if (activityWindow != null) {
      activityWindow!!.setBackgroundDrawable(activityWindowOrigBackground)
    }
  }

  override fun draw(canvas: Canvas) {
    // Minimize overdraw by not drawing anything while this
    // list is totally covered by the expandable page.
    if (isFullyCoveredByPage) {
      return
    }

    super.draw(canvas)

    // Dimming behind the expandable page.
    if (pageSetupDone) {
      itemDimmer.drawDimming(canvas)
    }
  }

  override fun canScrollProgrammatically(): Boolean {
    return pageSetupDone.not() || page.isCollapsed
  }

  /**
   * Experimental: Reduce overdraw by 1 level by removing the Activity Window's
   * background when the [ExpandablePageLayout] is expanded. No point in drawing
   * it when it's not visible to the user.
   **/
  fun optimizeActivityBackgroundOverdraw(activity: Activity) {
    activityWindow = activity.window
    activityWindowOrigBackground = activityWindow!!.decorView.background
  }

  override fun setAdapter(adapter: Adapter<*>?) {
    val isLayoutFrozenBak = isLayoutFrozen
    super.setAdapter(adapter)

    // isLayoutFrozen is reset when the adapter is changed.
    isLayoutFrozen = isLayoutFrozenBak
  }

  override fun swapAdapter(adapter: Adapter<*>?, removeAndRecycleExistingViews: Boolean) {
    val isLayoutFrozenBak = isLayoutFrozen
    super.swapAdapter(adapter, removeAndRecycleExistingViews)
    isLayoutFrozen = isLayoutFrozenBak
  }

  /** Details of the currently expanded item. */
  @Parcelize
  data class ExpandedItem(
      // Index of the currently expanded item's
      // View. This is not the adapter index.
      val viewIndex: Int,

      // Adapter ID of the currently expanded item.
      val itemId: Long,

      // Original location of the currently expanded item (that is, when the user
      // selected this item). Can be used for restoring states after collapsing.
      val expandedItemLocationRect: Rect

  ) : Parcelable {

    internal fun isEmpty(): Boolean {
      return viewIndex == -1 || itemId == -1L || expandedItemLocationRect.height() == 0
    }

    companion object {
      internal val EMPTY = ExpandedItem(itemId = -1, viewIndex = -1, expandedItemLocationRect = Rect(0, 0, 0, 0))
    }
  }

  companion object {
    private const val KEY_EXPAND_INFO = "expand_info"
    const val animationStartDelay: Int = 0  // Only used for debugging.
  }
}
