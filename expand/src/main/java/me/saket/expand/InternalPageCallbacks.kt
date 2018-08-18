package me.saket.expand

/** Used internally, by [InboxRecyclerView]. */
internal interface InternalPageCallbacks {

  /**
   * Called when this page has fully covered the list. This can happen in two situations: when the page has
   * fully expanded and when the page has moved back to its position after being pulled.
   */
  fun onPageFullyCovered()

  /**
   * Called when this page is going to be collapsed.
   */
  fun onPageAboutToCollapse()

  /**
   * Called when this page has fully collapsed and is no longer visible.
   */
  fun onPageFullyCollapsed()

  /**
   * Page is being pulled. Sync the scroll with the list.
   */
  fun onPagePull(deltaY: Float)

  /**
   * Called when this page was released while being pulled.
   *
   * @param collapseEligible Whether the page was pulled enough for collapsing it.
   */
  fun onPageRelease(collapseEligible: Boolean)
}
