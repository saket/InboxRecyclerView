package me.saket.inboxrecyclerview.animation

import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

internal class PageLocationChangeDetector(
  private val page: ExpandablePageLayout,
  private val changeListener: () -> Unit
) : ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener {

  private var lastTranslationX = 0F
  private var lastTranslationY = 0F
  private var lastWidth = 0
  private var lastHeight = 0
  private var lastClippedDimens = Rect()
  private var lastState = page.currentState

  private val rectBuffer = Rect()

  override fun onPreDraw(): Boolean {
    dispatchCallbackIfNeeded()
    return true
  }

  override fun onGlobalLayout() {
    // Changes in the page's dimensions will get handled here.
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
    lastClippedDimens.set(rectBuffer)
    lastState = page.currentState
  }
}
