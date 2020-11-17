package me.saket.inboxrecyclerview

/** Used internally, by [InboxRecyclerView]. */
internal interface InternalPageCallbacks {

  fun onPageAboutToExpand()

  /**
   * Called when this page has fully covered the list. This can happen in two situations:
   * 1. when the page has fully expanded.
   * 2. when the page has moved back to its position after being pulled.
   */
  fun onPageFullyCovered()

  fun onPageAboutToCollapse()

  /** Page is no longer visible at this point. */
  fun onPageCollapsed()

  /** Page will start getting pulled. */
  fun onPagePullStarted() = Unit

  /** Page is being pulled. Sync the scroll with the list. */
  fun onPagePull(deltaY: Float)

  /** Called when this page was released after being pulled. */
  fun onPageRelease(collapseEligible: Boolean)

  class NoOp : InternalPageCallbacks {

    override fun onPageAboutToExpand() {}

    override fun onPageFullyCovered() {}

    override fun onPageAboutToCollapse() {}

    override fun onPageCollapsed() {}

    override fun onPagePull(deltaY: Float) {}

    override fun onPageRelease(collapseEligible: Boolean) {}
  }
}
