package me.saket.inboxrecyclerview.page

import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Canvas
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View
import android.view.ViewGroup
import me.saket.inboxrecyclerview.ANIMATION_START_DELAY
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem
import me.saket.inboxrecyclerview.InternalPageCallbacks
import me.saket.inboxrecyclerview.InternalPageCallbacks.NoOp
import me.saket.inboxrecyclerview.executeOnMeasure
import me.saket.inboxrecyclerview.withEndAction
import java.lang.reflect.Method
import java.util.concurrent.CopyOnWriteArrayList
import kotlin.math.abs
import kotlin.math.max

/**
 * An expandable / collapsible layout for use with a [InboxRecyclerView].
 */
open class ExpandablePageLayout @JvmOverloads constructor(
  context: Context,
  attrs: AttributeSet? = null
) : BaseExpandablePageLayout(context, attrs), PullToCollapseListener.OnPullListener {

  /** See [pushParentToolbarOnExpand]. */
  private var parentToolbar: View? = null

  /** Alpha of this page when it's collapsed. */
  internal var collapsedAlpha = 0F

  var pullToCollapseInterceptor: OnPullToCollapseInterceptor? = null

  /** Minimum Y-distance the page has to be pulled before it's eligible for collapse. */
  var pullToCollapseThresholdDistance: Int
    get() = pullToCollapseListener.collapseDistanceThreshold
    set(value) {
      pullToCollapseListener.collapseDistanceThreshold = value
    }

  var pullToCollapseEnabled = false
  val pullToCollapseListener: PullToCollapseListener
  lateinit var currentState: PageState

  internal var internalStateCallbacksForRecyclerView: InternalPageCallbacks = NoOp()
  private var internalStateCallbacksForNestedPage: InternalPageCallbacks = NoOp()
  private var stateChangeCallbacks = CopyOnWriteArrayList<PageStateChangeCallbacks>()

  private var nestedPage: ExpandablePageLayout? = null
  private var toolbarAnimator: ValueAnimator = ObjectAnimator()
  private val expandedAlpha = 1F
  private var isFullyCoveredByNestedPage = false
  private val locationOnScreenBuffer = IntArray(2)

  val isExpanded: Boolean
    get() = currentState == PageState.EXPANDED

  val isExpandingOrCollapsing: Boolean
    get() = currentState == PageState.EXPANDING || currentState == PageState.COLLAPSING

  val isCollapsing: Boolean
    get() = currentState == PageState.COLLAPSING

  val isCollapsed: Boolean
    get() = currentState == PageState.COLLAPSED

  val isExpanding: Boolean
    get() = currentState == PageState.EXPANDING

  val isExpandedOrExpanding: Boolean
    get() = currentState == PageState.EXPANDED || currentState == PageState.EXPANDING

  val isCollapsedOrCollapsing: Boolean
    get() = currentState == PageState.COLLAPSING || currentState == PageState.COLLAPSED

  enum class PageState {
    COLLAPSING,
    COLLAPSED,
    EXPANDING,
    EXPANDED
  }

  init {
    // Hidden on start.
    alpha = expandedAlpha
    visibility = View.INVISIBLE
    changeState(PageState.COLLAPSED)

    pullToCollapseEnabled = true
    pullToCollapseListener = PullToCollapseListener(this)
    pullToCollapseListener.addOnPullListener(this)
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
    parentToolbar = null
    nestedPage = null
    pullToCollapseInterceptor = null
    pullToCollapseListener.removeAllOnPullListeners()
    internalStateCallbacksForNestedPage = NoOp()
    internalStateCallbacksForRecyclerView = NoOp()
    stateChangeCallbacks.clear()
    stopAnyOngoingPageAnimation()
    super.onDetachedFromWindow()
  }

  private fun changeState(newPageState: PageState) {
    currentState = newPageState
  }

  override fun hasOverlappingRendering(): Boolean {
    // This should help improve performance when animating alpha.
    // Source: https://www.youtube.com/watch?v=wIy8g8yNhNk.
    return false
  }

  override fun dispatchTouchEvent(ev: MotionEvent): Boolean {
    // Ignore touch events until the page is
    // fully expanded for avoiding accidental taps.
    if (isExpanded) {
      super.dispatchTouchEvent(ev)
    }

    // Consume all touch events to avoid them leaking behind.
    return true
  }

  override fun onInterceptTouchEvent(event: MotionEvent): Boolean {
    var intercepted = false
    if (pullToCollapseEnabled && visibility == View.VISIBLE) {
      intercepted = pullToCollapseListener.onTouch(this, event)
    }

    return intercepted || super.onInterceptTouchEvent(event)
  }

  @SuppressLint("ClickableViewAccessibility")
  override fun onTouchEvent(event: MotionEvent): Boolean {
    val handled = pullToCollapseEnabled && pullToCollapseListener.onTouch(this, event)
    return handled || super.onTouchEvent(event)
  }

  override fun onPull(
    deltaY: Float,
    currentTranslationY: Float,
    upwardPull: Boolean,
    deltaUpwardPull: Boolean,
    collapseEligible: Boolean
  ) {
    // In case the user pulled the page before it could fully
    // open (and while the toolbar was still hiding).
    stopToolbarAnimation()

    // Reveal the toolbar if this page is being pulled down or
    // hide it back if it's being released.
    if (parentToolbar != null) {
      updateToolbarTranslationY(currentTranslationY > 0F, currentTranslationY)
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

    changeState(PageState.EXPANDED)
    stopAnyOngoingPageAnimation()

    // Restore everything to their expanded position.
    // 1. Hide Toolbar again.
    if (parentToolbar != null) {
      animateToolbar(false, targetPageTranslationY = 0F)
    }

    // 2. Expand page again.
    if (translationY != 0F) {
      animate()
          .withLayer()
          .translationY(0F)
          .alpha(expandedAlpha)
          .setDuration(animationDurationMillis)
          .setInterpolator(animationInterpolator)
          .withEndAction { dispatchOnPageFullyCoveredCallback() }
          .start()
    }
  }

  /**
   * Expands this page (with animation) so that it fills the whole screen.
   */
  internal fun expand(expandedItem: ExpandedItem) {
    if (isLaidOut.not() && visibility != View.GONE) {
      throw IllegalAccessError("Width / Height not available to expand")
    }

    // Ignore if already expanded.
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
   * Expand this page instantly, without any animation.
   */
  internal fun expandImmediately() {
    if (currentState == PageState.EXPANDING || currentState == PageState.EXPANDED) {
      return
    }

    visibility = View.VISIBLE
    alpha = expandedAlpha

    // Hide the toolbar as soon as its height is available.
    parentToolbar?.executeOnMeasure { updateToolbarTranslationY(false, 0F) }

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
  internal fun collapse(expandedItem: ExpandedItem) {
    if (currentState == PageState.COLLAPSED || currentState == PageState.COLLAPSING) {
      return
    }

    var targetWidth = expandedItem.locationOnScreen.width()
    val targetHeight = expandedItem.locationOnScreen.height()
    if (targetWidth == 0) {
      // Page must have expanded immediately after a state restoration.
      targetWidth = width
    }
    animatePageExpandCollapse(false, targetWidth, targetHeight, expandedItem)

    // Send state callbacks that the city is going to collapse.
    dispatchOnPageAboutToCollapseCallback()
  }

  /**
   * Place the expandable page exactly on top of the expanding item.
   */
  private fun alignPageWithExpandingItem(expandedItem: ExpandedItem) {
    // Match height and location.
    setClippedDimensions(
        expandedItem.locationOnScreen.width(),
        expandedItem.locationOnScreen.height()
    )
    translationY = distanceYTo(expandedItem)
  }

  /**
   * Calculates the distance between a [InboxRecyclerView.ExpandedItem] and this page
   * by using their raw coordinates on the screen. Useful when [InboxRecyclerView] and
   * [ExpandablePageLayout] do not share the same parent or same bounds. For e.g., the
   * [InboxRecyclerView] may be below a toolbar whereas the [ExpandablePageLayout]
   * in front of the toolbar.
   */
  private fun distanceYTo(expandedItem: ExpandedItem): Float {
    val pageYOnScreen = locationOnScreen()[1]
    val itemYOnScreen = expandedItem.locationOnScreen.top.toFloat()
    return itemYOnScreen - (pageYOnScreen - translationY)
  }

  private fun distanceXTo(expandedItem: ExpandedItem): Float {
    val pageXOnScreen = locationOnScreen()[0]
    val itemXOnScreen = expandedItem.locationOnScreen.left.toFloat()
    return itemXOnScreen - (pageXOnScreen - translationX)
  }

  fun locationOnScreen(): IntArray {
    getLocationOnScreen(locationOnScreenBuffer)
    return locationOnScreenBuffer
  }

  internal fun alignPageToCoverScreen() {
    resetClipping()
    translationY = 0F
  }

  internal fun animatePageExpandCollapse(
    expand: Boolean,
    targetWidth: Int,
    targetHeight: Int,
    expandedItem: ExpandedItem
  ) {
    val targetPageTranslationX = if (expand) 0F else distanceXTo(expandedItem)
    var targetPageTranslationY = if (expand) 0F else distanceYTo(expandedItem)

    // If there's no record about the expanded list item (from whose place this page was expanded),
    // collapse just below the toolbar and not the window top to avoid closing the toolbar upon hiding.
    if (!expand && expandedItem.locationOnScreen.height() == 0) {
      val toolbarBottom = if (parentToolbar != null) parentToolbar!!.bottom else 0
      targetPageTranslationY = Math.max(targetPageTranslationY, toolbarBottom.toFloat())
    }

    if (expand.not()) {
      setSuppressLayoutMethodUsingReflection(this, true)
    }

    if (expand) {
      visibility = View.VISIBLE
    }

    alpha = if (expand) collapsedAlpha else expandedAlpha
    stopAnyOngoingPageAnimation()
    animate()
        .withLayer()
        .alpha(if (expand) expandedAlpha else collapsedAlpha)
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
    val speedFactor = when {
      show && abs(targetPageTranslationY - fromTy) > clippedDimens.height() * 2 / 5 -> 2L
      else -> 1L
    }

    stopToolbarAnimation()

    // If the page lies behind the toolbar, use toolbar's current bottom position instead
    toolbarAnimator = ObjectAnimator.ofFloat(fromTy, targetPageTranslationY)
    toolbarAnimator.addUpdateListener { animation ->
      updateToolbarTranslationY(show, animation.animatedValue as Float)
    }
    toolbarAnimator.duration = animationDurationMillis * speedFactor
    toolbarAnimator.interpolator = animationInterpolator
    toolbarAnimator.startDelay = ANIMATION_START_DELAY
    toolbarAnimator.start()
  }

  /**
   * Helper method for showing / hiding the toolbar depending upon this page's current translationY.
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

  internal fun stopAnyOngoingPageAnimation() {
    animate().cancel()
    stopToolbarAnimation()
  }

  private fun stopToolbarAnimation() {
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

  override fun draw(canvas: Canvas) {
    if (currentState == PageState.COLLAPSED) {
      return
    }
    super.draw(canvas)
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

  private fun dispatchOnPagePullCallbacks(deltaY: Float) {
    internalStateCallbacksForNestedPage.onPagePull(deltaY)
    internalStateCallbacksForRecyclerView.onPagePull(deltaY)
  }

  private fun dispatchOnPageReleaseCallback(collapseEligible: Boolean) {
    internalStateCallbacksForNestedPage.onPageRelease(collapseEligible)
    internalStateCallbacksForRecyclerView.onPageRelease(collapseEligible)
  }

  private fun dispatchOnPageAboutToExpandCallback(expandAnimDuration: Long) {
    internalStateCallbacksForNestedPage.onPageAboutToExpand()
    internalStateCallbacksForRecyclerView.onPageAboutToExpand()

    for (callback in stateChangeCallbacks) {
      callback.onPageAboutToExpand(this, expandAnimDuration)
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
      callback.onPageExpanded(this)
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
      callback.onPageAboutToCollapse(this, animationDurationMillis)
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
      callback.onPageCollapsed(this)
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
  fun pushParentToolbarOnExpand(toolbar: View) {
    this.parentToolbar = toolbar
  }

  fun addStateChangeCallbacks(callbacks: PageStateChangeCallbacks) {
    stateChangeCallbacks.add(callbacks)
  }

  fun removeStateChangeCallbacks(callbacks: PageStateChangeCallbacks) {
    stateChangeCallbacks.remove(callbacks)
  }

  /**
   * Listener that gets called when this page is being pulled.
   */
  fun addOnPullListener(listener: PullToCollapseListener.OnPullListener) {
    pullToCollapseListener.addOnPullListener(listener)
  }

  fun removeOnPullListener(pullListener: PullToCollapseListener.OnPullListener) {
    pullToCollapseListener.removeOnPullListener(pullListener)
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
