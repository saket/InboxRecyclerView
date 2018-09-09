package me.saket.expand.animation

import android.graphics.Rect
import android.view.ViewTreeObserver
import me.saket.expand.InboxRecyclerView
import me.saket.expand.page.ExpandablePageLayout

/**
 * Controls how [InboxRecyclerView] items are animated when its page is moving.
 * To create a custom animator, extend this and override [onPageMove].
 */
abstract class ItemExpandAnimator {

  private val pagePreDrawListener = object : ViewTreeObserver.OnPreDrawListener {
    private var lastTranslationY = 0F
    private var lastClippedDimens = Rect()
    private var lastState = ExpandablePageLayout.PageState.COLLAPSED

    override fun onPreDraw(): Boolean {
      val page = recyclerView.page
      if (lastTranslationY != page.translationY || lastClippedDimens != page.clippedDimens || lastState != page.currentState) {
        onPageMove()
      }

      lastTranslationY = page.translationY
      lastClippedDimens = page.clippedDimens
      lastState = page.currentState
      return true
    }
  }

  private val pageLayoutChangeListener = {
    // Changes in the page's dimensions will get handled here.
    onPageMove()
  }

  protected lateinit var recyclerView: InboxRecyclerView

  fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {
    this.recyclerView = recyclerView
    recyclerView.page.viewTreeObserver.addOnGlobalLayoutListener(pageLayoutChangeListener)
    recyclerView.page.viewTreeObserver.addOnPreDrawListener(pagePreDrawListener)
  }

  fun onDetachRecyclerView(recyclerView: InboxRecyclerView) {
    recyclerView.page.viewTreeObserver.removeOnGlobalLayoutListener(pageLayoutChangeListener)
    recyclerView.page.viewTreeObserver.removeOnPreDrawListener(pagePreDrawListener)
  }

  /**
   * Called when the page changes its position and/or dimensions. This can
   * happen when the page is expanding, collapsing or being pulled vertically.
   *
   * Override this to animate the [InboxRecyclerView] items with the page's movement.
   */
  abstract fun onPageMove()

  companion object {

    fun default() = DefaultItemExpandAnimator()
  }
}
