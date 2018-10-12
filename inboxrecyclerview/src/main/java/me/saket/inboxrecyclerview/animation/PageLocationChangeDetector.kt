package me.saket.inboxrecyclerview.animation

import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

internal class PageLocationChangeDetector(
    private val page: ExpandablePageLayout,
    private val changeListener: () -> Unit
) : ViewTreeObserver.OnPreDrawListener, ViewTreeObserver.OnGlobalLayoutListener {

  private var lastTranslationY = 0F
  private var lastClippedDimens = Rect()
  private var lastState = ExpandablePageLayout.PageState.COLLAPSED

  override fun onPreDraw(): Boolean {
    dispatchCallbackIfNeeded()
    return true
  }

  override fun onGlobalLayout() {
    // Changes in the page's dimensions will get handled here.
    dispatchCallbackIfNeeded()
  }

  private fun dispatchCallbackIfNeeded() {
    val moved = lastTranslationY != page.translationY
    val dimensionsChanged = lastClippedDimens != page.clippedDimens
    val stateChanged = lastState != page.currentState

    if (page.isCollapsed.not() && (moved || dimensionsChanged || stateChanged)) {
      changeListener()
    }

    lastTranslationY = page.translationY
    lastClippedDimens = page.clippedDimens
    lastState = page.currentState
  }
}
