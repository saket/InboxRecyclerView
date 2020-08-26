package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * [https://www.youtube.com/watch?v=87ZbSFgwn8s]
 */
internal class NoneAnimator : ItemExpandAnimator() {
  override fun onPageMove(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout,
    anchorViewOverlay: View?
  ) {
    anchorViewOverlay?.alpha = page.contentCoverAlpha
  }
}
