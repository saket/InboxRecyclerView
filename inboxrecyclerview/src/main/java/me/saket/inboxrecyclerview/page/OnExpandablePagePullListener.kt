package me.saket.inboxrecyclerview.page

interface OnExpandablePagePullListener {
  /**
   * Called when a pull starts.
   */
  fun onPullStarted() = Unit

  /**
   * Called when the user is pulling down / up the expandable page or the list.
   *
   * @param deltaY              Delta translation-Y since the last onPull call.
   * @param currentTranslationY Current translation-Y of the page.
   * @param upwardPull          Whether the page is being pulled in the upward direction.
   * @param deltaUpwardPull     Whether the distance pulled since last the onPull() was made in the
   *                            upward direction. Calculated using [deltaY].
   * @param collapseEligible    Whether the pull distance is enough to trigger a collapse.
   */
  fun onPull(
    deltaY: Float,
    currentTranslationY: Float,
    upwardPull: Boolean,
    deltaUpwardPull: Boolean,
    collapseEligible: Boolean
  ) = Unit

  /**
   * Called when the user's finger is lifted.
   *
   * @param collapseEligible Whether or not the pull distance was enough to trigger a collapse.
   */
  fun onRelease(collapseEligible: Boolean) = Unit
}
