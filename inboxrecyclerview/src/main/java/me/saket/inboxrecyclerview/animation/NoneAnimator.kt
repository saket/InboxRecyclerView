package me.saket.inboxrecyclerview.animation

import android.view.View
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * [https://github.com/saket/InboxRecyclerView/tree/master/images/animators/animator_none.mp4]
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
