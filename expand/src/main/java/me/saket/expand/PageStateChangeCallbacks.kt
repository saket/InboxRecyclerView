package me.saket.expand

/**
 * TODO: Improve doc.
 *
 * Implement this to receive state callbacks about an ExpandingPage. Use with
 * [ExpandablePageLayout.addStateChangeCallbacks]. If you use a Fragment
 * inside your `ExpandablePage`, it's best to make your Fragment implement this interface.
 */
interface PageStateChangeCallbacks {

  /**
   * Called when the user has selected an item and the `ExpandablePage` is going to be expand.
   */
  fun onPageAboutToExpand(expandAnimDuration: Long)

  /**
   * Called when either the `ExpandablePage`'s expand animation is complete or if the
   * `ExpandablePage` was expanded immediately. At this time, the page is fully covering the list.
   */
  fun onPageExpanded()

  /**
   * Called when the user has chosen to close the expanded item and the `ExpandablePage` is going to
   * be collapse.
   */
  fun onPageAboutToCollapse(collapseAnimDuration: Long)

  /**
   * Called when the page's collapse animation is complete. At this time, it's totally invisible to the user.
   */
  fun onPageCollapsed()
}
