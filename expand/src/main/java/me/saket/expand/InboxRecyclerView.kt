package me.saket.expand

import android.app.Activity
import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Bundle
import android.os.Parcelable
import android.support.v7.widget.LinearLayoutManager
import android.support.v7.widget.RecyclerView
import android.support.v7.widget.Toolbar
import android.util.AttributeSet
import android.view.View
import android.view.Window
import kotlinx.android.parcel.Parcelize

/**
 * Mimics the expandable layout in the Inbox app by Google. #AcheDin.
 */
class InboxRecyclerView(context: Context, attrs: AttributeSet) : RecyclerView(context, attrs), InternalPageCallbacks {

  var page: ExpandablePageLayout? = null
    private set

  private var expandInfo: ExpandInfo? = null             // Details about the currently expanded Item
  private val dimPaint: Paint
  private var activityWindow: Window? = null
  private var activityWindowOrigBackground: Drawable? = null
  private var pendingItemsOutOfTheWindowAnimation: Boolean = false
  private var isFullyCoveredByPage: Boolean = false
  private var layoutManagerCreated: Boolean = false

  init {
    // For drawing an overlay shadow while the expandable page is fully expanded.
    setWillNotDraw(false)
    dimPaint = Paint(Paint.ANTI_ALIAS_FLAG)
    dimPaint.color = Color.BLACK
    dimPaint.alpha = MAX_DIM
  }

  fun createLayoutManager(): RecyclerView.LayoutManager {
    layoutManagerCreated = true

    // TODO: Create a custom LayoutManager and assert it in setLayoutManager().
    return object : LinearLayoutManager(context) {
      override fun scrollVerticallyBy(dy: Int, recycler: RecyclerView.Recycler?, state: RecyclerView.State?): Int {
        return if (!canScroll()) 0 else super.scrollVerticallyBy(dy, recycler, state)
      }
    }
  }

  fun saveExpandableState(outState: Bundle) {
    if (page != null) {
      outState.putBoolean(KEY_IS_EXPANDED, page!!.isExpanded)
    }
  }

  /** Letting Activities handle restoration manually so that the setup can happen before onRestore gets called.  */
  fun restoreExpandableState(savedInstance: Bundle) {
    val wasExpanded = savedInstance.getBoolean(KEY_IS_EXPANDED)
    if (wasExpanded) {
      if (page == null) {
        throw NullPointerException("setExpandablePage() must be called before handleOnRetainInstance()")
      }
      page!!.expandImmediately()
    }
  }

  /**
   * TODO: Use page's setter instead.
   *
   * Sets the [ExpandablePageLayout] and [Toolbar] to be used with this list. The toolbar
   * gets pushed up when the page is expanding. It is also safe to call this method again and replace
   * the ExpandablePage or Toolbar.
   */
  @JvmOverloads
  fun setExpandablePage(expandablePage: ExpandablePageLayout, toolbar: View? = null) {
    page = expandablePage
    expandablePage.setup(toolbar)
    expandablePage.setInternalStateCallbacksForList(this)
  }

  override fun onLayout(changed: Boolean, l: Int, t: Int, r: Int, b: Int) {
    super.onLayout(changed, l, t, r, b)

    // In case any expand() call was made before this list and its child Views were measured, perform it now.
    if (pendingItemsOutOfTheWindowAnimation) {
      pendingItemsOutOfTheWindowAnimation = false
      animateItemsOutOfTheWindow(true)
    }
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    // Must have gotten called because the keyboard was called / hidden. The items must maintain their
    // positions, relative to the new bounds. Wait for Android to draw the child Views. Calling
    // getChildCount() right now will return old values (that is, no. of children that were present
    // before this height change happened.
    if (page == null) {
      return
    }

    Views.executeOnNextLayout(this) {
      // Fix list items.
      if (page!!.isExpandedOrExpanding) {
        animateItemsOutOfTheWindow(page!!.isExpanded)
      } else {
        animateItemsBackToPosition(page!!.isCollapsed)
      }

      // Fix expandable page (or else it gets stuck in the middle since it doesn't know of the size change).
      if (page!!.currentState === ExpandablePageLayout.PageState.EXPANDING) {
        page!!.animatePageExpandCollapse(true, width, height, getExpandInfo())

      } else if (page!!.currentState === ExpandablePageLayout.PageState.EXPANDED) {
        page!!.alignPageToCoverScreen()
      }
    }
  }

  // ======== EXPAND / COLLAPSE ======== //

  /**
   * @param itemViewPosition Item's position in the RecyclerView. This is not the same as adapter position.
   */
  fun expandItem(itemViewPosition: Int, itemId: Long) {
    if (page == null) {
      throw IllegalAccessError("Did you forget to call InboxRecyclerView.setup(ExpandablePage, Toolbar)")
    }
    if (!layoutManagerCreated) {
      throw IllegalAccessError("LayoutManager isn't set. #Use createLayoutManager()")
    }

    if (page!!.isExpandedOrExpanding) {
      return
    }

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

  /**
   * Expands from arbitrary location. Presently the top.
   */
  fun expandFromTop() {
    // TODO: This will possibly crash in expandItem(). Test before using.
    expandItem(-1, -1)
  }

  /**
   * Expands the page right away and pushes the items out of the list without animations.
   */
  fun expandImmediately() {
    page!!.expandImmediately()

    // This will push all the list items to the bottom, as if the item
    // above the 0th position was expanded
    animateItemsOutOfTheWindow(true)
  }

  fun collapse() {
    if (page == null) {
      throw IllegalStateException("No page attached. Cannot collapse. ListId: $id")
    }

    // Ignore if already collapsed
    if (page!!.isCollapsedOrCollapsing) {
      return
    }
    pendingItemsOutOfTheWindowAnimation = false

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
    if (!isLaidOut) {
      // Neither this list has been drawn yet nor its child views.
      pendingItemsOutOfTheWindowAnimation = true
      return
    }

    val anchorPosition = getExpandInfo().expandedItemPosition
    val listHeight = height

    for (i in 0 until childCount) {
      val view = getChildAt(i)

      // 1. Anchor view to the top edge
      // 2. Views above it out of the top edge
      // 3. Views below it out of the bottom edge
      val moveY: Float
      val above = i <= anchorPosition

      if (anchorPosition == -1 || view.height <= 0) {
        // Item to expand not present in the list. Send all Views outside the bottom edge
        moveY = (listHeight - paddingTop).toFloat()

      } else {
        val positionOffset = i - anchorPosition
        moveY = (if (above)
          -view.top + positionOffset * view.height
        else
          listHeight - view.top + view.height * (positionOffset - 1)).toFloat()
      }

      view.animate().cancel()
      if (!immediate) {
        view.animate()
            .translationY(moveY)
            .setDuration(page!!.animationDurationMillis)
            .setInterpolator(page!!.animationInterpolator).startDelay = animationStartDelay.toLong()

        if (anchorPosition == i) {
          view.animate().alpha(0f).withLayer()
        }

      } else {

        view.translationY = moveY
        if (anchorPosition == i) {
          view.alpha = 0f
        }
      }
    }
  }

  /**
   * Reverses animateItemsOutOfTheWindow() by moving all items back to their actual positions.
   */
  private fun animateItemsBackToPosition(immediate: Boolean) {
    val childCount = childCount
    for (i in 0 until childCount) {
      val view = getChildAt(i) ?: continue

      // Strangely, both the sections (above and below) are getting restored at the same time even when
      // the animation duration is same. :O
      // Update: Oh god. I confused time with speed. Not deleting this so that this comment always
      // reminds me how stupid I can be at times.
      view.animate().cancel()

      if (!immediate) {
        view.animate()
            .alpha(1f)
            .translationY(0f)
            .setDuration(page!!.animationDurationMillis)
            .setInterpolator(page!!.animationInterpolator).startDelay = animationStartDelay.toLong()

      } else {
        view.translationY = 0f
        view.alpha = 1f
      }
    }
  }

  override fun onPageAboutToCollapse() {
    onPageBackgroundVisible()
    postInvalidate()
  }

  override fun onPageFullyCollapsed() {
    expandInfo = null
  }

  override fun onPagePull(deltaY: Float) {
    for (i in 0 until childCount) {
      val itemView = getChildAt(i)

      // Stop any ongoing animation in case the user started pulling
      // while the list items were still animating (out of the window).
      itemView.animate().cancel()
      itemView.translationY = itemView.translationY + deltaY
    }
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

  fun onPageBackgroundVisible() {
    val invalidate = isFullyCoveredByPage
    isFullyCoveredByPage = false
    if (invalidate) {
      postInvalidate()
    }

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

    if (page != null && page!!.isExpanded) {
      // Dimming behind the expandable page.
      canvas.drawRect(0f, 0f, right.toFloat(), bottom.toFloat(), dimPaint)
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

  /**
   * Contains details of the currently expanded item.
   */
  @Parcelize
  data class ExpandInfo(
      // Position of the currently expanded item.
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
    private const val KEY_IS_EXPANDED = "isExpanded"
    private const val MAX_DIM_FACTOR = 0.2f                       // [0..1]
    private const val MAX_DIM = (255 * MAX_DIM_FACTOR).toInt()    // [0..255]
    const val animationStartDelay: Int = 0
  }
}
