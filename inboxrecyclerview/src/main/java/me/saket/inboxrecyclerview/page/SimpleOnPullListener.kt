package me.saket.inboxrecyclerview.page

/**
 * Empty implementations of [PullToCollapseListener.OnPullListener]. This way, any custom
 * listener that cares only about a subset of the methods of this listener can subclass
 * this adapter class instead of implementing the interface directly.
 */
@Deprecated(
    message = "Not needed anymore, thanks to default functions in interfaces",
    replaceWith = ReplaceWith(
        "OnExpandablePagePullListener",
        "me.saket.inboxrecyclerview.page.OnExpandablePagePullListener"
    )
)
abstract class SimpleOnPullListener : PullToCollapseListener.OnPullListener {
  /**
   * See [PullToCollapseListener.OnPullListener.onPull]
   */
  override fun onPull(
    deltaY: Float,
    currentTranslationY: Float,
    upwardPull: Boolean,
    deltaUpwardPull: Boolean,
    collapseEligible: Boolean
  ) {
    // For rent. Broker free.
  }

  override fun onRelease(collapseEligible: Boolean) {
    // For rent. Broker free.
  }
}
