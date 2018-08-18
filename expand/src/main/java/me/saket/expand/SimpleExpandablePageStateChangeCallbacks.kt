package me.saket.expand

/**
 * Empty implementations of [PageStateChangeCallbacks]. This way, any custom listener that
 * cares only about a subset of the methods of this listener can subclass this adapter
 * class instead of implementing the interface directly.
 */
abstract class SimpleExpandablePageStateChangeCallbacks : PageStateChangeCallbacks {

  override fun onPageAboutToExpand(expandAnimDuration: Long) {
    // For rent. Broker free.
  }

  override fun onPageExpanded() {
    // For rent. Broker free.
  }

  override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
    // For rent. Broker free.
  }

  override fun onPageCollapsed() {
    // For rent. Broker free.
  }
}
