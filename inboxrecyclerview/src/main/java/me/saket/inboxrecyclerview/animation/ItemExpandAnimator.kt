package me.saket.inboxrecyclerview.animation

import me.saket.inboxrecyclerview.InboxRecyclerView

/**
 * Controls how [InboxRecyclerView] items are animated when its page is moving.
 * To create a custom animator, extend this and override [onPageMove].
 */
abstract class ItemExpandAnimator {

  protected lateinit var recyclerView: InboxRecyclerView
  private lateinit var changeDetector: PageLocationChangeDetector

  fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {
    this.recyclerView = recyclerView
    this.changeDetector = PageLocationChangeDetector(recyclerView.page, changeListener = ::onPageMove)

    recyclerView.page.viewTreeObserver.addOnGlobalLayoutListener(changeDetector)
    recyclerView.page.viewTreeObserver.addOnPreDrawListener(changeDetector)
  }

  fun onDetachRecyclerView(recyclerView: InboxRecyclerView) {
    recyclerView.page.viewTreeObserver.removeOnGlobalLayoutListener(changeDetector)
    recyclerView.page.viewTreeObserver.removeOnPreDrawListener(changeDetector)
  }

  /**
   * Called when the page changes its position and/or dimensions. This can
   * happen when the page is expanding, collapsing or being pulled vertically.
   *
   * Override this to animate the [InboxRecyclerView] items with the page's movement.
   */
  abstract fun onPageMove()

  companion object {

    /**
     * See [SplitExpandAnimator].
     */
    @JvmStatic
    fun split() = SplitExpandAnimator()
    fun nested() = NestedExpandAnimator()
  }
}
