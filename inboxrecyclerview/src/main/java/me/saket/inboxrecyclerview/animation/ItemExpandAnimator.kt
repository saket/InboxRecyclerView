package me.saket.inboxrecyclerview.animation

import android.view.View
import android.view.View.INVISIBLE
import android.view.View.VISIBLE
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Controls how [InboxRecyclerView] items are animated when its page is moving.
 * To create a custom animator, extend this and override [onPageMove].
 */
abstract class ItemExpandAnimator {

  private lateinit var onDetach: () -> Unit
  private var anchorViewOverlay: View? = null
  private var onRemoveOverlay: () -> Unit = {}

  fun onAttachRecyclerView(recyclerView: InboxRecyclerView, page: ExpandablePageLayout) {
    val changeDetector = PageLocationChangeDetector(page) {
      val anchorView = maybeUpdateAnchorOverlay(page, recyclerView)
      onPageMove(recyclerView, page, anchorView)
    }

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

  private fun maybeUpdateAnchorOverlay(
    page: ExpandablePageLayout,
    recyclerView: InboxRecyclerView
  ): View? {
    val anchorIndex = recyclerView.expandedItem.viewIndex

    if (page.isExpandingOrCollapsing && anchorIndex != -1 && anchorViewOverlay == null) {
      val anchorView = recyclerView.getChildAt(anchorIndex)!!
      anchorViewOverlay = anchorView.captureImage(forOverlayOf = page).also {
        // Revert the layout position because
        // ScaleExpandAnimator may have modified the RV's scale.
        it.layout(0, 0, anchorView.width, anchorView.height)
      }
      page.overlay.add(anchorViewOverlay!!)
      anchorView.visibility = INVISIBLE
      onRemoveOverlay = { anchorView.visibility = VISIBLE }
    }

    if (page.isExpandedOrCollapsed && anchorViewOverlay != null) {
      page.overlay.remove(anchorViewOverlay!!)
      anchorViewOverlay = null
      onRemoveOverlay()
    }
    return anchorViewOverlay
  }

  /**
   * Called when the page changes its position and/or dimensions. This can
   * happen when the page is expanding, collapsing or being pulled vertically.
   *
   * Override this to animate [InboxRecyclerView] items with the page's movement.
   *
   * @param anchorViewOverlay An overlay of the item View that is expanding/collapsing.
   * It'll be null if the page is collapsed or if the page was expanded using
   * [InboxRecyclerView.expandFromTop].
   */
  abstract fun onPageMove(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout,
    anchorViewOverlay: View?
  )

  companion object {
    /**
     * See [SplitExpandAnimator].
     */
    @JvmStatic
    fun split() = SplitExpandAnimator()

    /**
     * See [ScaleExpandAnimator].
     */
    @JvmStatic
    fun scale() = ScaleExpandAnimator()

    /**
     * See [NoneAnimator].
     */
    @JvmStatic
    fun none() = NoneAnimator()
  }
}
