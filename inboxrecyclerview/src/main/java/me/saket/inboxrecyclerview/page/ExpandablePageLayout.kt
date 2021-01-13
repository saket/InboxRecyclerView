package me.saket.inboxrecyclerview.page

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.graphics.Canvas
import android.graphics.Outline
import android.graphics.Rect
import android.graphics.drawable.Drawable
import android.os.Parcelable
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import android.view.ViewOutlineProvider
import androidx.annotation.FloatRange
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.ANIMATION_START_DELAY
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItemLocation
import me.saket.inboxrecyclerview.InternalPageCallbacks
import me.saket.inboxrecyclerview.InternalPageCallbacks.NoOp
import me.saket.inboxrecyclerview.executeOnMeasure
import me.saket.inboxrecyclerview.locationOnScreen
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDED
import me.saket.inboxrecyclerview.page.ExpandablePageLayout.PageState.EXPANDING
import me.saket.inboxrecyclerview.withEndAction
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.DeprecationLevel.ERROR
import kotlin.math.abs
import kotlin.math.max

/**
 * An expandable / collapsible layout for use with a [InboxRecyclerView].
 */
@Suppress("LeakingThis")
open class ExpandablePageLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : BaseExpandablePageLayout(context, attrs), PullToCollapseListener.OnPullListener, SimpleNestedScrollingParent3 {

  /** See [pushParentToolbarOnExpand]. */
  private var parentToolbar: View? = null

  /**
   * When this page is expanding, the content is smoothly faded in.
   * This value controls the max opacity of the content.
   */
  @FloatRange(from = 0.0, to = 1.0)
  var contentOpacityWhenCollapsed = 0f

  /**
   * When this page is collapsing, the content is smoothly faded out.
   * This value controls the min opacity of the content.
   */
  @FloatRange(from = 0.0, to = 1.0)
  val contentOpacityWhenExpanded = 1f

  /**
   * Opacity of content while the page is expanding/collapsing.
   */
  internal var contentOpacity: Float = contentOpacityWhenCollapsed
    set(value) {
      field = value
      invalidate()
      invalidateOutline()
    }

  /**
   * See [OnPullToCollapseInterceptor].
   */
  var pullToCollapseInterceptor: OnPullToCollapseInterceptor? = null

  /**
   * Minimum Y-distance the page has to be pulled before it's eligible for collapse.
   * Defaults to around 56dp which is a Toolbar's height.
   */
  var pullToCollapseThresholdDistance: Int
    get() = nestedScroller.collapseDistanceThreshold
    set(value) {
      nestedScroller.collapseDistanceThreshold = value
    }

  /**
   * Whether pulling/dragging this page vertically beyond [pullToCollapseThresholdDistance]
   * will trigger a collapse.
   * */
  var pullToCollapseEnabled = true

  @Deprecated(message = "InboxRecyclerView now uses nested scrolling.", level = ERROR)
  val pullToCollapseListener = Unit

  lateinit var currentState: PageState

  internal var internalStateCallbacksForRecyclerView: InternalPageCallbacks = NoOp()
  private var internalStateCallbacksForNestedPage: InternalPageCallbacks = NoOp()
  private var stateChangeCallbacks = CopyOnWriteArrayList<PageStateChangeCallbacks>()

  private val nestedScroller = PullToCollapseNestedScroller(this)
  private var nestedPage: ExpandablePageLayout? = null
  private var toolbarAnimator: ValueAnimator = ObjectAnimator()
  private var contentCoverAnimator: ValueAnimator = ObjectAnimator()
  private var isFullyCoveredByNestedPage = false
  internal var dimDrawable: Drawable? = null

  val isExpanded: Boolean
    get() = currentState == PageState.EXPANDED

  val isCollapsing: Boolean
    get() = currentState == PageState.COLLAPSING

  val isCollapsed: Boolean
    get() = currentState == PageState.COLLAPSED

  val isExpanding: Boolean
    get() = currentState == PageState.EXPANDING

  val isExpandingOrCollapsing: Boolean
    get() = currentState == PageState.EXPANDING || currentState == PageState.COLLAPSING

  val isExpandedOrCollapsed: Boolean
    get() = currentState == PageState.EXPANDED || currentState == PageState.COLLAPSED

  val isExpandedOrExpanding: Boolean
    get() = currentState == PageState.EXPANDED || currentState == PageState.EXPANDING

  val isCollapsedOrCollapsing: Boolean
    get() = currentState == PageState.COLLAPSING || currentState == PageState.COLLAPSED

  /** Whether the page will collapse if the touched is released right now. */
  val isCollapseEligible
    get() = abs(translationY) >= pullToCollapseThresholdDistance

  /** Whether the page is expanding/collapsing or if it's being pulled to collapse. */
  val isMoving: Boolean
    get() = isExpandingOrCollapsing || isExpanded && translationY != 0f

  enum class PageState {
    COLLAPSING,
    COLLAPSED,
    EXPANDING,
    EXPANDED
  }

  init {
    // Hidden on start.
    visibility = INVISIBLE
    contentOpacity = contentOpacityWhenCollapsed
    changeState(PageState.COLLAPSED)

    pullToCollapseEnabled = true
    nestedScroller.addOnPullListener(this)

    outlineProvider = object : ViewOutlineProvider() {
      override fun getOutline(view: View, outline: Outline) {
        BACKGROUND.getOutline(view, outline)
        outline.alpha = contentOpacity
      }
    }
  }

  override fun onAttachedToWindow() {
    super.onAttachedToWindow()

    // Cache before-hand.
    Thread {
      if (suppressLayoutMethod == null) {
        setSuppressLayoutMethodUsingReflection(this, false)
      }
    }.start()
  }

  override fun onDetachedFromWindow() {
    stopAnyOngoingAnimation()
    pushParentToolbarOnExpand(toolbar = null)
    nestedPage = null
    pullToCollapseInterceptor = null
    nestedScroller.removeAllOnPullListeners()
    internalStateCallbacksForNestedPage = NoOp()
    internalStateCallbacksForRecyclerView = NoOp()
    stateChangeCallbacks.clear()
    super.onDetachedFromWindow()
  }

  override fun onSaveInstanceState(): Parcelable {
    return ExpandablePageSavedState(
        superState = super.onSaveInstanceState(),
        state = currentState
    )
  }

  override fun onRestoreInstanceState(state: Parcelable) {
    require(state is ExpandablePageSavedState)
    super.onRestoreInstanceState(state.superState)

    if (state.state == EXPANDED || state.state == EXPANDING) {
      expandImmediately()
    }
  }

  private fun changeState(newPageState: PageState) {
    currentState = newPageState
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    nestedScroller.storeTouchEvent(ev)
    return isExpandedOrExpanding && super.dispatchTouchEvent(ev)
  }

  override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
    super.onSizeChanged(w, h, oldw, oldh)

    @Suppress("NON_EXHAUSTIVE_WHEN")
    when (currentState) {
      EXPANDING -> animateDimensions(toWidth = width, toHeight = height)
      EXPANDED -> resetClipping()
    }
  }

  override fun onPullStarted() {
    dispatchOnPagePullStartedCallbacks()
  }

  override fun onPull(
    deltaY: Float,
    currentTranslationY: Float,
    upwardPull: Boolean,
    deltaUpwardPull: Boolean,
    collapseEligible: Boolean
  ) {
    // Reveal the toolbar if this page is being pulled down or
    // hide it back if it's being released.
    if (parentToolbar != null) {
      updateToolbarTranslationY(show = currentTranslationY > 0F, currentTranslationY)
    }

    // Sync the positions of the list items with this page.
    dispatchOnPagePullCallbacks(deltaY)
  }

  override fun onRelease(collapseEligible: Boolean) {
    dispatchOnPageReleaseCallback(collapseEligible)

    if (collapseEligible) {
      return
    }

    if (isCollapsedOrCollapsing) {
      // collapse() got called somehow.
      // Let the page collapse in peace.
      return
    }

    // Restore everything to their expanded position.
    // 1. Hide Toolbar again.
    if (parentToolbar != null) {
      animateToolbar(false, targetPageTranslationY = 0F)
    }

    // 2. Expand page again.
    if (translationY != 0F) {
      animateContentCoverAlpha(expand = true)
      animateDimensions(width, height)
      animate()
          .translationY(0F)
          .setDuration(animationDurationMillis)
          .setInterpolator(animationInterpolator)
          .withEndAction { dispatchOnPageFullyCoveredCallback() }
          .start()
    }
  }

  /**
   * Expands this page (with animation) so that it fills the whole screen.
   */
  internal open fun expand(expandedItem: ExpandedItemLocation) {
    if (isLaidOut.not() && visibility != View.GONE) {
      throw IllegalAccessError("Width / Height not available to expand")
    }
    if (isExpandedOrExpanding) {
      return
    }

    // Place the expandable page on top of the expanding item.
    alignPageWithExpandingItem(expandedItem)

    // Callbacks, just before the animation starts.
    dispatchOnPageAboutToExpandCallback(animationDurationMillis)

    // Animate!
    animatePageExpandCollapse(true, width, height, expandedItem)
  }

  /**
   * Expand this page instantly, without any animation. Useful when using this layout
   * as a standalone page, without an associated InboxRecyclerView to manage its lifecycle.
   */
  internal open fun expandImmediately() {
    if (isExpandedOrExpanding) {
      return
    }

    visibility = View.VISIBLE
    contentOpacity = contentOpacityWhenExpanded

    // Hide the toolbar as soon as its height is available.
    parentToolbar?.executeOnMeasure { updateToolbarTranslationY(show = false, 0F) }

    executeOnMeasure {
      // Cover the whole screen right away. Don't need any animations.
      alignPageToCoverScreen()
      dispatchOnPageAboutToExpandCallback(0)
      dispatchOnPageFullyExpandedCallback()
    }
  }

  /**
   * Collapses this page, back to its original state.
   */
  internal fun collapse(expandedItem: ExpandedItemLocation) {
    if (currentState == PageState.COLLAPSED || currentState == PageState.COLLAPSING) {
      return
    }

    // Send state callbacks that the city is going to collapse.
    dispatchOnPageAboutToCollapseCallback()

    var targetWidth = expandedItem.locationOnScreen.width()
    val targetHeight = expandedItem.locationOnScreen.height()
    if (targetWidth == 0) {
      // Page must have expanded immediately after a state restoration.
      targetWidth = width
    }
    animatePageExpandCollapse(false, targetWidth, targetHeight, expandedItem)
  }

  /**
   * Place the expandable page exactly on top of the expanding item.
   */
  private fun alignPageWithExpandingItem(expandedItem: ExpandedItemLocation) {
    // Match height and location.
    setClippedDimensions(
        expandedItem.locationOnScreen.width(),
        expandedItem.locationOnScreen.height()
    )
    translationY = distanceYTo(expandedItem)
    translationX = distanceXTo(expandedItem)
  }

  /**
   * Calculates the distance between a [InboxRecyclerView.ExpandedItemLocation] and this page
   * by using their raw coordinates on the screen. Useful when [InboxRecyclerView] and
   * [ExpandablePageLayout] do not share the same parent or same bounds. For e.g., the
   * [InboxRecyclerView] may be below a toolbar whereas the [ExpandablePageLayout]
   * in front of the toolbar.
   */
  private fun distanceYTo(expandedItem: ExpandedItemLocation): Float {
    val pageYOnScreen = locationOnScreen().top
    val itemYOnScreen = expandedItem.locationOnScreen.top.toFloat()
    return itemYOnScreen - (pageYOnScreen - translationY)
  }

  private fun distanceXTo(expandedItem: ExpandedItemLocation): Float {
    val pageXOnScreen = locationOnScreen().left
    val itemXOnScreen = expandedItem.locationOnScreen.left.toFloat()
    return itemXOnScreen - (pageXOnScreen - translationX)
  }

  private val intArrayBuffer = IntArray(2)
  private val rectBuffer = Rect()
  fun locationOnScreen(): Rect {
    return locationOnScreen(intArrayBuffer, rectBuffer)
  }

  internal fun alignPageToCoverScreen() {
    resetClipping()
    translationY = 0F
  }

  private fun animatePageExpandCollapse(
    expand: Boolean,
    targetWidth: Int,
    targetHeight: Int,
    expandedItem: ExpandedItemLocation
  ) {
    val targetPageTranslationX = if (expand) 0F else distanceXTo(expandedItem)
    var targetPageTranslationY = if (expand) 0F else distanceYTo(expandedItem)

    // If there's no record about the expanded list item (from whose place this page was expanded),
    // collapse just below the toolbar and not the window top to avoid closing the toolbar upon hiding.
    if (!expand && expandedItem.locationOnScreen.height() == 0) {
      val toolbarBottom = if (parentToolbar != null) parentToolbar!!.bottom else 0
      targetPageTranslationY = targetPageTranslationY.coerceAtLeast(toolbarBottom.toFloat())
    }

    if (expand.not()) {
      setSuppressLayoutMethodUsingReflection(this, true)
    }

    if (expand) {
      visibility = View.VISIBLE
    }

    stopAnyOngoingAnimation()
    animateContentCoverAlpha(expand)
    animate()
        .translationY(targetPageTranslationY)
        .translationX(targetPageTranslationX)
        .setDuration(animationDurationMillis)
        .setInterpolator(animationInterpolator)
        .withEndAction { canceled ->
          setSuppressLayoutMethodUsingReflection(this@ExpandablePageLayout, false)

          if (!canceled) {
            if (!expand) {
              visibility = View.INVISIBLE
              dispatchOnPageCollapsedCallback()
            } else {
              dispatchOnPageFullyExpandedCallback()
            }
          }
        }
        .setStartDelay(ANIMATION_START_DELAY)
        .start()

    // Show the toolbar fully even if the page is going to collapse behind it
    var targetPageTranslationYForToolbar = targetPageTranslationY
    if (!expand && parentToolbar != null && targetPageTranslationYForToolbar < parentToolbar!!.bottom) {
      targetPageTranslationYForToolbar = parentToolbar!!.bottom.toFloat()
    }

    if (parentToolbar != null) {
      // Hide / show the toolbar by pushing it up during expand and pulling it down during collapse.
      animateToolbar(
          !expand, // When expand is false, !expand shows the toolbar.
          targetPageTranslationYForToolbar
      )
    }

    animateDimensions(targetWidth, targetHeight)
  }

  protected open fun animateContentCoverAlpha(expand: Boolean) {
    if (contentOpacityWhenCollapsed != contentOpacityWhenExpanded) {
      checkNotNull(background) {
        "A solid background is needed on this page for smoothly fading in/out its content."
      }
    }

    val toAlpha: Float = if (expand) contentOpacityWhenExpanded else contentOpacityWhenCollapsed

    contentCoverAnimator.cancel()
    contentCoverAnimator = ObjectAnimator.ofFloat(contentOpacity, toAlpha).apply {
      duration = animationDurationMillis / 3
      startDelay = ANIMATION_START_DELAY + if (expand) 0 else animationDurationMillis / 3
      addUpdateListener {
        contentOpacity = it.animatedValue as Float
      }
      start()
    }
  }

  private fun animateToolbar(
    show: Boolean,
    targetPageTranslationY: Float
  ) {
    if (translationY == targetPageTranslationY) {
      return
    }

    val toolbarCurrentBottom = when {
      parentToolbar != null -> parentToolbar!!.bottom + parentToolbar!!.translationY
      else -> 0F
    }
    val fromTy = max(toolbarCurrentBottom, translationY)

    // The hide animation happens a bit too quickly if the page has to travel a large
    // distance (when using the current interpolator: EASE). Let's try slowing it down.
    @Suppress("DEPRECATION")
    val speedFactor = when {
      show && abs(targetPageTranslationY - fromTy) > clippedDimens.height() * 2 / 5 -> 2L
      else -> 1L
    }

    toolbarAnimator.cancel()

    // If the page lies behind the toolbar, use toolbar's current bottom position instead
    toolbarAnimator = ObjectAnimator.ofFloat(fromTy, targetPageTranslationY).apply {
      addUpdateListener { animation ->
        updateToolbarTranslationY(show, animation.animatedValue as Float)
      }
      duration = animationDurationMillis * speedFactor
      interpolator = animationInterpolator
      startDelay = ANIMATION_START_DELAY
      start()
    }
  }

  /**
   * Show / hide the toolbar depending upon this page's current translationY.
   */
  private fun updateToolbarTranslationY(
    show: Boolean,
    pageTranslationY: Float
  ) {
    val toolbarHeight = parentToolbar!!.bottom
    var targetTranslationY = pageTranslationY - toolbarHeight

    if (show) {
      if (targetTranslationY > toolbarHeight) {
        targetTranslationY = toolbarHeight.toFloat()
      }
      if (targetTranslationY > 0) {
        targetTranslationY = 0F
      }

    } else if (pageTranslationY >= toolbarHeight || parentToolbar!!.translationY <= -toolbarHeight) {
      // Hide.
      return
    }

    parentToolbar!!.translationY = targetTranslationY
  }

  fun stopAnyOngoingAnimation() {
    stopDimensionAnimation()
    animate().cancel()
    contentCoverAnimator.cancel()
    toolbarAnimator.cancel()
  }

  /**
   * Experimental: To be used when another ExpandablePageLayout is shown inside
   * this page. This page will avoid all draw calls while the nested page is
   * open to minimize overdraw.
   *
   * WARNING: DO NOT USE THIS IF THE NESTED PAGE IS THE ONLY PULL-COLLAPSIBLE
   * PAGE IN AN ACTIVITY.
   */
  fun setNestedExpandablePage(nestedPage: ExpandablePageLayout) {
    val old = this.nestedPage
    if (old != null) {
      old.internalStateCallbacksForNestedPage = NoOp()
    }

    this.nestedPage = nestedPage

    nestedPage.internalStateCallbacksForNestedPage = object : InternalPageCallbacks {
      override fun onPageAboutToExpand() {}

      override fun onPageAboutToCollapse() {
        onPageBackgroundVisible()
      }

      override fun onPageCollapsed() {}

      override fun onPagePull(deltaY: Float) {
        onPageBackgroundVisible()
      }

      override fun onPageRelease(collapseEligible: Boolean) {
        if (collapseEligible) {
          onPageBackgroundVisible()
        }
      }

      override fun onPageFullyCovered() {
        val invalidate = !isFullyCoveredByNestedPage
        isFullyCoveredByNestedPage = true   // Skips draw() until visible again to the
        if (invalidate) {
          postInvalidate()
        }
      }

      fun onPageBackgroundVisible() {
        val invalidate = isFullyCoveredByNestedPage
        isFullyCoveredByNestedPage = false
        if (invalidate) {
          postInvalidate()
        }
      }
    }
  }

  override fun dispatchDraw(canvas: Canvas) {
    if (currentState != PageState.COLLAPSED) {
      super.dispatchDraw(canvas)
    }
    if (background != null) {
      val alphaBackup = background.alpha
      background.alpha = 255 - (255 * contentOpacity).toInt()
      background.draw(canvas)
      background.alpha = alphaBackup
    }

    dimDrawable?.setBounds(0, 0, width, height)
    dimDrawable?.draw(canvas)
  }

  override fun drawChild(
    canvas: Canvas,
    child: View,
    drawingTime: Long
  ): Boolean {
    // When this page is fully covered by a nested ExpandablePage, avoid drawing any other child Views.
    return if (isFullyCoveredByNestedPage && child !is ExpandablePageLayout) {
      false
    } else {
      super.drawChild(canvas, child, drawingTime)
    }
  }

  private fun dispatchOnPagePullStartedCallbacks() {
    internalStateCallbacksForNestedPage.onPagePullStarted()
    internalStateCallbacksForRecyclerView.onPagePullStarted()
  }

  private fun dispatchOnPagePullCallbacks(deltaY: Float) {
    internalStateCallbacksForNestedPage.onPagePull(deltaY)
    internalStateCallbacksForRecyclerView.onPagePull(deltaY)
  }

  protected open fun dispatchOnPageReleaseCallback(collapseEligible: Boolean) {
    internalStateCallbacksForNestedPage.onPageRelease(collapseEligible)
    internalStateCallbacksForRecyclerView.onPageRelease(collapseEligible)
  }

  private fun dispatchOnPageAboutToExpandCallback(expandAnimDuration: Long) {
    internalStateCallbacksForNestedPage.onPageAboutToExpand()
    internalStateCallbacksForRecyclerView.onPageAboutToExpand()

    for (callback in stateChangeCallbacks) {
      callback.onPageAboutToExpand(expandAnimDuration)
    }
    onPageAboutToExpand(animationDurationMillis)

    // The state change must happen after the subscribers have been
    // notified that the page is going to expand.
    changeState(PageState.EXPANDING)
  }

  private fun dispatchOnPageFullyExpandedCallback() {
    changeState(PageState.EXPANDED)
    dispatchOnPageFullyCoveredCallback()

    for (callback in stateChangeCallbacks) {
      callback.onPageExpanded()
    }

    onPageExpanded()
  }

  /**
   * There's a difference between the page fully expanding and fully covering the list.
   * When the page is fully expanded, it may or may not be covering the list. This is
   * usually when the user is pulling the page.
   */
  private fun dispatchOnPageFullyCoveredCallback() {
    internalStateCallbacksForNestedPage.onPageFullyCovered()
    internalStateCallbacksForRecyclerView.onPageFullyCovered()
  }

  private fun dispatchOnPageAboutToCollapseCallback() {
    internalStateCallbacksForNestedPage.onPageAboutToCollapse()
    internalStateCallbacksForRecyclerView.onPageAboutToCollapse()

    for (callback in stateChangeCallbacks) {
      callback.onPageAboutToCollapse(animationDurationMillis)
    }
    onPageAboutToCollapse(animationDurationMillis)

    // The state change must happen after the subscribers have been
    // notified that the page is going to collapse.
    changeState(PageState.COLLAPSING)
  }

  private fun dispatchOnPageCollapsedCallback() {
    changeState(PageState.COLLAPSED)

    internalStateCallbacksForNestedPage.onPageCollapsed()
    internalStateCallbacksForRecyclerView.onPageCollapsed()

    for (callback in stateChangeCallbacks) {
      callback.onPageCollapsed()
    }
    onPageCollapsed()
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected open fun onPageAboutToExpand(expandAnimDuration: Long) {
    // For rent.
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected open fun onPageExpanded() {
    // For rent.
  }

  @Suppress("MemberVisibilityCanBePrivate")
  protected open fun onPageAboutToCollapse(collapseAnimDuration: Long) {
    // For rent.
  }

  /**
   * Page is totally invisible to the user when this is called.
   */
  @Suppress("MemberVisibilityCanBePrivate")
  protected open fun onPageCollapsed() {
    // For rent.
  }

  /**
   * Offer a pull-to-collapse to a listener if it wants to block it. If a nested page is registered
   * and the touch was made on it, block it right away.
   */
  internal fun handleOnPullToCollapseIntercept(
    event: MotionEvent,
    downX: Float,
    downY: Float,
    deltaUpwardSwipe: Boolean
  ): InterceptResult {
    val nestedPageCopy = nestedPage

    @Suppress("DEPRECATION")
    if (nestedPageCopy != null
        && nestedPageCopy.isExpandedOrExpanding
        && nestedPageCopy.clippedDimens.contains(downX.toInt(), downY.toInt())
    ) {
      // Block this pull if it was made inside a nested page. Let the nested
      // page's pull-listener consume this event. I should use nested scrolling
      // in the future to make this smarter.
      // TODO: 20/03/17 Do we even need to call the nested page's listener?
      nestedPageCopy.handleOnPullToCollapseIntercept(event, downX, downY, deltaUpwardSwipe)
      return InterceptResult.INTERCEPTED

    } else {
      val interceptor = pullToCollapseInterceptor

      return when {
        interceptor != null -> interceptor(downX, downY, deltaUpwardSwipe)
        else -> InterceptResult.IGNORED
      }
    }
  }

  /**
   * Push [toolbar] out of the screen during expansion when this page reaches the
   * bottom of the toolbar. When this page is collapsing or being pulled downwards.
   * the toolbar will be animated back to its position.
   */
  fun pushParentToolbarOnExpand(toolbar: View?) {
    if (this.parentToolbar != toolbar) {
      parentToolbar?.translationY = 0f
      toolbarAnimator.cancel()
    }
    this.parentToolbar = toolbar
  }

  fun addStateChangeCallbacks(callbacks: PageStateChangeCallbacks) {
    stateChangeCallbacks.add(callbacks)
  }

  fun removeStateChangeCallbacks(callbacks: PageStateChangeCallbacks) {
    stateChangeCallbacks.remove(callbacks)
  }

  /**
   * Add a listener that gets called when this page is pulled.
   */
  fun addOnPullListener(listener: OnExpandablePagePullListener) {
    nestedScroller.addOnPullListener(listener)
  }

  fun removeOnPullListener(pullListener: OnExpandablePagePullListener) {
    nestedScroller.removeOnPullListener(pullListener)
  }

  override fun onStartNestedScroll(child: View, target: View, axes: Int, type: Int): Boolean {
    return if (pullToCollapseEnabled) {
      // Accept all nested scroll events from the child. The decision of whether
      // or not to actually scroll is calculated inside onNestedPreScroll().
      nestedScroller.onStartNestedScroll(axes, type)
    } else {
      false
    }
  }

  override fun onNestedPreScroll(target: View, dx: Int, dy: Int, consumed: IntArray, type: Int) {
    nestedScroller.onNestedPreScroll(target, dy, consumed, type)
  }

  override fun onStopNestedScroll(target: View, type: Int) {
    nestedScroller.onStopNestedScroll(type)
  }

  companion object {
    private var suppressLayoutMethod: Method? = null

    // TODO: Move to a different class.
    private fun setSuppressLayoutMethodUsingReflection(
      layout: ExpandablePageLayout,
      suppress: Boolean
    ) {
      try {
        if (suppressLayoutMethod == null) {
          suppressLayoutMethod = ViewGroup::class.java
              .getMethod("suppressLayout", Boolean::class.javaPrimitiveType)
        }
        suppressLayoutMethod!!.invoke(layout, suppress)
      } catch (e: Throwable) {
        throw e
      }
    }
  }
}

@Parcelize
private data class ExpandablePageSavedState(
  val superState: Parcelable?,
  val state: ExpandablePageLayout.PageState
) : Parcelable
