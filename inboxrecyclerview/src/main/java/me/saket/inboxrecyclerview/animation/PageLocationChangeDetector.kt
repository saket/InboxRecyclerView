package me.saket.inboxrecyclerview.animation

import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.PageStateChangeCallbacks
import me.saket.inboxrecyclerview.page.SimplePageStateChangeCallbacks

/**
 * Gives a callback everytime [ExpandablePageLayout]'s size or location changes.
 * Can be used for, say, synchronizing animations with the page's expansion/collapse.
 */
class PageLocationChangeDetector(
  private val page: ExpandablePageLayout,
  private val changeListener: () -> Unit
) : ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener, SimplePageStateChangeCallbacks() {

  private var lastTranslationX = 0F
  private var lastTranslationY = 0F
  private var lastWidth = 0
  private var lastHeight = 0
  private var lastClippedDimens = Rect()
  private var lastState = page.currentState

  override fun onPreDraw(): Boolean {
    dispatchCallbackIfNeeded()
    return true
  }

  override fun onGlobalLayout() {
    // Changes in the page's dimensions will get handled here.
    dispatchCallbackIfNeeded()
  }

  override fun onPageCollapsed() {
    // The page may get removed once it's collapsed.
    dispatchCallbackIfNeeded()
  }

  private fun dispatchCallbackIfNeeded() {
    val moved = lastTranslationX != page.translationX || lastTranslationY != page.translationY
    val stateChanged = lastState != page.currentState
    val dimensionsChanged = lastWidth != page.width
        || lastHeight != page.height
        || lastClippedDimens != page.clippedDimens

    if (moved || dimensionsChanged || stateChanged) {
      changeListener()
    }

    lastTranslationX = page.translationX
    lastTranslationY = page.translationY
    lastWidth = page.width
    lastHeight = page.height
    lastClippedDimens.set(page.clippedDimens)
    lastState = page.currentState
  }

  fun start() {
    page.viewTreeObserver.addOnGlobalLayoutListener(this)
    page.viewTreeObserver.addOnPreDrawListener(this)
    page.addStateChangeCallbacks(this)
  }

  fun stop() {
    page.viewTreeObserver.removeOnGlobalLayoutListener(this)
    page.viewTreeObserver.removeOnPreDrawListener(this)
    page.removeStateChangeCallbacks(this)
  }
}
