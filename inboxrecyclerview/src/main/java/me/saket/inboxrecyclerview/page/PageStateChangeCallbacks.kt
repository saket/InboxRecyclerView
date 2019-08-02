package me.saket.inboxrecyclerview.page

/**
 * Implement this to receive state callbacks for [ExpandablePageLayout].
 */
interface PageStateChangeCallbacks {

  fun onPageAboutToExpand(
    page: ExpandablePageLayout,
    expandAnimDuration: Long
  )

  fun onPageExpanded(
    page: ExpandablePageLayout
  )

  fun onPageAboutToCollapse(
    page: ExpandablePageLayout,
    collapseAnimDuration: Long
  )

  fun onPageCollapsed(
    page: ExpandablePageLayout
  )
}
