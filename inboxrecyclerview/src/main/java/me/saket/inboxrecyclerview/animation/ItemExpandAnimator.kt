package me.saket.inboxrecyclerview.animation

import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Controls how [InboxRecyclerView] items are animated when its page is moving.
 * To create a custom animator, extend this and override [onPageMove].
 */
abstract class ItemExpandAnimator {

  private lateinit var onDetach: () -> Unit

  fun onAttachRecyclerView(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    val changeDetector = PageLocationChangeDetector(page) { onPageMove(recyclerView, page) }

    page.viewTreeObserver.addOnGlobalLayoutListener(changeDetector)
    page.viewTreeObserver.addOnPreDrawListener(changeDetector)

    onDetach = {
      page.viewTreeObserver.removeOnGlobalLayoutListener(changeDetector)
      page.viewTreeObserver.removeOnPreDrawListener(changeDetector)
    }
  }

  fun onDetachRecyclerView() {
    onDetach()
  }

  /**
   * Called when the page changes its position and/or dimensions. This can
   * happen when the page is expanding, collapsing or being pulled vertically.
   *
   * Override this to animate the [InboxRecyclerView] items with the page's movement.
   */
  abstract fun onPageMove(recyclerView: InboxRecyclerView, page: ExpandablePageLayout)

  companion object {

    /**
     * See [SplitExpandAnimator].
     */
    @JvmStatic
    fun split() = SplitExpandAnimator()
  }
}
