package me.saket.expand

import android.animation.ObjectAnimator
import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.util.AttributeSet
import android.view.View
import android.view.Window
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize
import me.saket.expand.Views.executeOnNextLayout
import me.saket.expand.page.ExpandablePageLayout

/**
 * Mimics the expandable layout in the Inbox app by Google. #AcheDin.
 */
class InboxRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs), InternalPageCallbacks {

  var page: ExpandablePageLayout? = null
    private set(value) {
      val oldPage = field
      field = value!!

      if (oldPage != null) {
        itemExpandAnimator.onPageDetached(oldPage)
      }
      itemExpandAnimator.page = field!!
      itemExpandAnimator.recyclerView = this
      itemExpandAnimator.onPageAttached()
    }

  private var expandInfo: ExpandInfo? = null             // Details about the currently expanded Item
  private val dimPaint: Paint
  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var isFullyCoveredByPage: Boolean = false
  private var itemExpandAnimator: ItemExpandAnimator = DefaultItemExpandAnimator()

  init {
    // For drawing an overlay shadow while the expandable page is fully expanded.
    setWillNotDraw(false)
    dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    dimPaint.color = Color.BLACK
    dimPaint.alpha = MAX_DIM
  }

  fun saveExpandableState(outState: Bundle) {
    if (page != null) {
      outState.putParcelable(KEY_EXPAND_INFO, expandInfo)
    }
  }

  /** Letting Activities handle restoration manually so that the setup can happen before onRestore gets called.  */
  fun restoreExpandableState(savedInstance: Bundle) {
    expandInfo = savedInstance.getParcelable(KEY_EXPAND_INFO) as ExpandInfo?
    if (expandInfo != null) {
      if (page == null) {
        throw NullPointerException("setExpandablePage() must be called before calling restoreExpandableState()")
      }
      expandImmediately()
    }
  }

  /**
   * TODO: Use page's setter instead.
   *
   * Sets the [ExpandablePageLayout] and [Toolbar] to be used with this list. The toolbar
   * gets pushed up when the page is expanding. It is also safe to call this method again and replace
   * the ExpandablePage or Toolbar.
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
      if (page!!.isExpandedOrExpanding) {
        val immediate = page!!.isExpanded
        animateItemsOutOfTheWindow(immediate)
      } else {
        val immediate = page!!.isCollapsed
        animateItemsBackToPosition(immediate)
      }

      if (page!!.currentState === ExpandablePageLayout.PageState.EXPANDING) {
        page!!.animatePageExpandCollapse(true, width, height, getExpandInfo())

      } else if (page!!.currentState === ExpandablePageLayout.PageState.EXPANDED) {
        page!!.alignPageToCoverScreen()
      }
    }
  }

  private fun ensurePageIsSetup() {
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
    ensurePageIsSetup()

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

    animateItemsOutOfTheWindow()
    if (!isLaidOut && visibility != View.GONE) {
      expandImmediately()
    } else {
      page!!.expand(getExpandInfo())
    }
  }

  /** Expand from the top, pushing all items out of the window towards the bottom. */
  fun expandFromTop() {
    ensurePageIsSetup()

    expandInfo = ExpandInfo(-1, -1, Rect(left, top, right, top))
    animateItemsOutOfTheWindow()
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
    animateItemsOutOfTheWindow(immediate = true)
  }

  fun collapse() {
    if (page == null) {
      throw IllegalStateException("No page attached. Cannot collapse. ListId: $id")
    }

    // Ignore if already collapsed
    if (page!!.isCollapsedOrCollapsing) {
      return
    }

    // This ensures the items were present outside the window before collapse starts
    if (page!!.translationY == 0f) {
      animateItemsOutOfTheWindow(true)
    }

    // Collapse the page and restore the item positions of this list
    if (page != null) {
      val expandInfo = getExpandInfo()
      page!!.collapse(expandInfo)
    }
    animateItemsBackToPosition(false)
  }

  /**
   * Animates all items out of the Window. The item at position `expandInfo.expandedItemPosition`
   * is moved to the top, while the items above it are animated out of the window from the top and the rest
   * from the bottom.
   */
  @JvmOverloads
  internal fun animateItemsOutOfTheWindow(immediate: Boolean = false) {
    // TODO: Move dimming logic into a separate class.
    if (immediate.not()) {
      val dimAnimator = ObjectAnimator.ofInt(dimPaint.alpha, MAX_DIM).apply {
        duration = page!!.animationDurationMillis
        interpolator = page!!.animationInterpolator
        startDelay = animationStartDelay.toLong()
      }
      dimAnimator.addUpdateListener {
        dimPaint.alpha = it.animatedValue as Int
        postInvalidate()
      }
      dimAnimator.start()

    } else {
      dimPaint.alpha = MAX_DIM
      postInvalidate()
    }
  }

  /**
   * Reverses animateItemsOutOfTheWindow() by moving all items back to their actual positions.
   */
  private fun animateItemsBackToPosition(immediate: Boolean) {
    // TODO: Move dimming logic into a separate class.
    if (immediate.not()) {
      val dimAnimator = ObjectAnimator.ofInt(dimPaint.alpha, MIN_DIM).apply {
        duration = page!!.animationDurationMillis
        interpolator = page!!.animationInterpolator
        startDelay = animationStartDelay.toLong()
      }
      dimAnimator.addUpdateListener {
        dimPaint.alpha = it.animatedValue as Int
        postInvalidate()
      }
      dimAnimator.start()

    } else {
      dimPaint.alpha = MIN_DIM
      postInvalidate()
    }
  }

  override fun onPageAboutToExpand() {
    isLayoutFrozen = true
  }

  override fun onPageAboutToCollapse() {
    isLayoutFrozen = false
    onPageBackgroundVisible()
    postInvalidate()
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
      onPageBackgroundVisible()
    } else {
      animateItemsOutOfTheWindow()
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
      val pageCopy = this.page!!
      canvas.drawRect(0F, 0F, right.toFloat(), pageCopy.translationY, dimPaint)

      if (pageCopy.isExpanded) {
        canvas.drawRect(0F, (bottom + pageCopy.translationY), right.toFloat(), bottom.toFloat(), dimPaint)

      } else if (pageCopy.isExpandingOrCollapsing) {
        val pageBottom = pageCopy.translationY + pageCopy.clippedRect.height().toFloat()
        canvas.drawRect(0F, pageBottom, right.toFloat(), bottom.toFloat(), dimPaint)
      }
    }
  }

  private fun canScroll(): Boolean {
    return page == null || page!!.isCollapsed
  }

  override fun scrollToPosition(position: Int) {
    if (!canScroll()) {
      return
    }
    super.scrollToPosition(position)
  }

  override fun smoothScrollToPosition(position: Int) {
    if (!canScroll()) {
      return
    }
    super.smoothScrollToPosition(position)
  }

  override fun smoothScrollBy(dx: Int, dy: Int) {
    if (!canScroll()) {
      return
    }
    super.smoothScrollBy(dx, dy)
  }

  override fun scrollTo(x: Int, y: Int) {
    if (!canScroll()) {
      return
    }
    super.scrollTo(x, y)
  }

  override fun scrollBy(x: Int, y: Int) {
    if (!canScroll()) {
      return
    }
    super.scrollBy(x, y)
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
    private const val MIN_DIM = 0
    private const val MAX_DIM_FACTOR = 0.1F                       // [0..1]
    private const val MAX_DIM = (255 * MAX_DIM_FACTOR).toInt()    // [0..255]
    const val animationStartDelay: Int = 0
  }
}
