package me.saket.inboxrecyclerview.page

/**
 * Empty implementations of [PageStateChangeCallbacks]. This way, any custom listener that
 * cares only about a subset of the methods of this listener can subclass this adapter
 * class instead of implementing the interface directly.
 */
abstract class SimplePageStateChangeCallbacks : PageStateChangeCallbacks {

  override fun onPageAboutToExpand(
    page: ExpandablePageLayout,
    expandAnimDuration: Long
  ) {
    // For rent. Broker free.
  }

  override fun onPageExpanded(page: ExpandablePageLayout) {
    // For rent. Broker free.
  }

  override fun onPageAboutToCollapse(
    page: ExpandablePageLayout,
    collapseAnimDuration: Long
  ) {
    // For rent. Broker free.
  }

  override fun onPageCollapsed(page: ExpandablePageLayout) {
    // For rent. Broker free.
  }
}
