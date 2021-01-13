package me.saket.inboxrecyclerview.page

class PullToCollapseListener {
  @Deprecated(
      message = "Moved to an upper level interface.",
      replaceWith = ReplaceWith(
          "OnExpandablePagePullListener",
          "me.saket.inboxrecyclerview.page.OnExpandablePagePullListener"
      )
  )
  interface OnPullListener : OnExpandablePagePullListener
}
