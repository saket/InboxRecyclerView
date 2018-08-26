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
import me.saket.expand.dimming.ItemDimmer
import me.saket.expand.page.ExpandablePageLayout

/**
 * Mimics the expandable layout in the Inbox app by Google. #AcheDin.
 */
class InboxRecyclerView(
    context: Context,
    attrs: AttributeSet
) : ScrollSuppressibleRecyclerView(context, attrs), InternalPageCallbacks {

  var page: ExpandablePageLayout? = null
    private set(value) {
      val oldPage = field
      field = value!!

      if (oldPage != null) {
        itemExpandAnimator.onPageDetached(oldPage)
        itemDimmer.onPageDetached(oldPage)
      }

      itemExpandAnimator.page = field!!
      itemExpandAnimator.recyclerView = this
      itemExpandAnimator.onPageAttached()

      itemDimmer.page = field!!
      itemDimmer.recyclerView = this
      itemDimmer.onPageAttached()
    }

  // TODO: Doc.
  var itemExpandAnimator: ItemExpandAnimator = DefaultItemExpandAnimator()
    set(value) {
      field.onPageDetached(page!!)
      field = value
    }

  // TODO: Doc.
  var itemDimmer: ItemDimmer = ItemDimmer.uncoveredItems()
    set(value) {
      field.onPageDetached(page!!)
      field = value

      field.page = page!!
      field.recyclerView = this
      field.onPageAttached()
    }

  /** Details about the currently expanded row. */
  private var expandInfo: ExpandInfo? = null

  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var isFullyCoveredByPage: Boolean = false

  init {
    // For drawing an overlay shadow while the expandable page is fully expanded.
    setWillNotDraw(false)
  }

  fun saveExpandableState(outState: Bundle) {
    if (page != null) {
      outState.putParcelable(KEY_EXPAND_INFO, expandInfo)
    }
  }

  /**
   * Letting Activities handle restoration manually so that the setup can optionally
   * happen before onRestore gets called.
   * */
  fun restoreExpandableState(savedInstance: Bundle) {
    expandInfo = savedInstance.getParcelable(KEY_EXPAND_INFO) as ExpandInfo?
    if (expandInfo != null) {
      ensureSetup()
      expandImmediately()
    }
  }

  /**
   * Sets the [ExpandablePageLayout] and [Toolbar] to be used with this list. The toolbar
   * gets pushed up when the page is expanding. It is also safe to call this method again
   * and replace the ExpandablePage or Toolbar.
   *
   * The [toolbar]'s height is also used as the pull-to-collapse distance threshold.
   */
  fun setExpandablePage(expandablePage: ExpandablePageLayout, toolbar: View) {
    page = expandablePage
    expandablePage.setInternalStateCallbacksForList(this)

    expandablePage.setToolbar(toolbar)
    toolbar.post {
      expandablePage.setPullToCollapseDistanceThreshold(toolbar.height)
    }
  }

  /**
   * [collapseDistanceThreshold] Minimum Y-distance the page has to be pulled to collapse.
   */
  fun setExpandablePage(expandablePage: ExpandablePageLayout, collapseDistanceThreshold: Int) {
    page = expandablePage
    expandablePage.setInternalStateCallbacksForList(this)

    expandablePage.setPullToCollapseDistanceThreshold(collapseDistanceThreshold)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    if (page == null) {
      return
    }

    // The items must maintain their positions, relative to the new bounds. Wait for
    // Android to draw the child Views. Calling getChildCount() right now will return
    // old values (that is, no. of children that were present before this height
    // change happened.
    executeOnNextLayout {
      if (page!!.currentState === ExpandablePageLayout.PageState.EXPANDING) {
        page!!.animatePageExpandCollapse(true, width, height, getExpandInfo())

      } else if (page!!.currentState === ExpandablePageLayout.PageState.EXPANDED) {
        page!!.alignPageToCoverScreen()
      }
    }
  }

  private fun ensureSetup() {
    if (page == null) {
      throw IllegalAccessError("Did you forget to call InboxRecyclerView.setup(ExpandablePage, Toolbar)")
    }
    if (adapter == null) {
      throw AssertionError("Adapter isn't attached yet. No items to expand.")
    }
  }

  /**
   * @param itemPosition Item's position in the adapter.
   */
  fun expandItem(itemPosition: Int, itemId: Long) {
    ensureSetup()

    if (page!!.isExpandedOrExpanding) {
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

    expandInfo = ExpandInfo(itemViewPosition, itemId, itemRect)

    if (!isLaidOut && visibility != View.GONE) {
      expandImmediately()
    } else {
      page!!.expand(getExpandInfo())
    }
  }

  /** Expand from the top, pushing all items out of the window towards the bottom. */
  fun expandFromTop() {
    ensureSetup()

    expandInfo = ExpandInfo(-1, -1, Rect(left, top, right, top))

    if (!isLaidOut && visibility != View.GONE) {
      expandImmediately()
    } else {
      page!!.expand(getExpandInfo())
    }
  }

  /**
   * Expands the page right away and pushes the items out of the list without animations.
   */
  fun expandImmediately() {
    page!!.expandImmediately()
  }

  fun collapse() {
    if (page == null) {
      throw IllegalStateException("No page attached. Cannot collapse. ListId: $id")
    }

    // Ignore if already collapsed
    if (page!!.isCollapsedOrCollapsing) {
      return
    }

    // Collapse the page and restore the item positions of this list
    if (page != null) {
      val expandInfo = getExpandInfo()
      page!!.collapse(expandInfo)
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
    expandInfo = null
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
    if (page != null) {
      itemDimmer.drawDimming(canvas)
    }
  }

  override fun canScrollProgrammatically(): Boolean {
    return page == null || page!!.isCollapsed
  }

  /**
   * @return Details of the currently expanded item. Returns an empty ExpandInfo object
   * if all items are collapsed.
   */
  fun getExpandInfo(): ExpandInfo {
    if (expandInfo == null) {
      expandInfo = ExpandInfo.EMPTY
    }
    return expandInfo!!
  }

  /**
   * Reduce overdraw by 1 level by removing the Activity Window's background
   * while the [ExpandablePageLayout] is open. No point in drawing it when
   * it's not visible to the user. This way, there's no extra overdraw while the
   * expandable page is open.
   */
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

  /**
   * Contains details of the currently expanded item.
   */
  @Parcelize
  data class ExpandInfo(
      // Position of the currently expanded item.
      // TODO: Rename to index
      var expandedItemPosition: Int,

      // Adapter ID of the currently expanded item.
      var expandedItemId: Long,

      // Original location of the currently expanded item (that is, when the user
      // selected this item). Can be used for restoring states after collapsing.
      internal var expandedItemLocationRect: Rect
  ) : Parcelable {

    internal fun isEmpty(): Boolean {
      return expandedItemPosition == -1 || expandedItemId == -1L || expandedItemLocationRect.height() == 0
    }

    companion object {
      internal val EMPTY = ExpandInfo(expandedItemId = -1, expandedItemPosition = -1, expandedItemLocationRect = Rect(0, 0, 0, 0))
    }
  }

  companion object {
    private const val KEY_EXPAND_INFO = "expand_info"
    const val animationStartDelay: Int = 0
  }
}
