package me.saket.inboxrecyclerview.animation

import android.graphics.Canvas
import android.view.View
import android.view.View.GONE
import android.view.View.VISIBLE
import androidx.core.view.doOnDetach
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.animation.ItemExpandAnimator.Companion.none
import me.saket.inboxrecyclerview.animation.ItemExpandAnimator.Companion.scale
import me.saket.inboxrecyclerview.animation.ItemExpandAnimator.Companion.split
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Controls how [InboxRecyclerView] items are animated when [ExpandablePageLayout] is moving.
 * See [split], [scale] and [none].
 */
abstract class ItemExpandAnimator {
  private lateinit var onDetach: () -> Unit
  private var anchorViewOverlay: View? = null

  fun onAttachRecyclerView(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout
  ) {
    val changeDetector = PageLocationChangeDetector(page) {
      val anchorView = maybeUpdateAnchorOverlay(recyclerView, page)
      onPageMove(recyclerView, page, anchorView)
    }

    changeDetector.start()
    onDetach = {
      anchorViewOverlay?.let { page.overlay.remove(it) }
      resetAnimation(recyclerView, anchorViewOverlay = null)
      changeDetector.stop()
    }
  }

  fun onDetachRecyclerView() {
    onDetach()
  }

  private fun maybeUpdateAnchorOverlay(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout
  ): View? {
    val anchorIndex = recyclerView.expandedItem.viewIndex

    if (page.isCollapsed.not() && anchorIndex != -1 && anchorViewOverlay == null) {
      recyclerView.getChildAt(anchorIndex)?.let { anchorView ->
        anchorViewOverlay = anchorView.captureImage(forOverlayOf = page).also {
          // Revert the layout position because
          // - ScaleExpandAnimator may have modified the RV's scale.
          // - SplitExpandAnimator may have modified the y-translation.
          it.layout(0, 0, anchorView.width, anchorView.height)
        }
        page.overlay.add(anchorViewOverlay!!)
        anchorView.visibility = GONE

        anchorViewOverlay!!.doOnDetach {
          anchorView.visibility = VISIBLE
          anchorViewOverlay = null
        }
      }
    }

    if (page.isCollapsed && anchorViewOverlay != null) {
      page.overlay.remove(anchorViewOverlay!!)
    }

    return anchorViewOverlay
  }

  /**
   * Called when the page changes its position and/or dimensions. This can
   * happen when the page is expanding, collapsing or being pulled vertically.
   *
   * Override this to animate [InboxRecyclerView] items with the page's movement.
   *
   * @param anchorViewOverlay An overlay of the item View that is expanding/collapsing.
   * It'll be null if the page is collapsed or if the page was expanded using
   * [InboxRecyclerView.expandFromTop].
   */
  abstract fun onPageMove(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout,
    anchorViewOverlay: View?
  )

  /**
   * Called when a page is detached from its [InboxRecyclerView].
   */
  abstract fun resetAnimation(
    recyclerView: InboxRecyclerView,
    anchorViewOverlay: View?
  )

  /** Called before the list items are drawn on the canvas. */
  open fun transformRecyclerViewCanvas(
    recyclerView: InboxRecyclerView,
    canvas: Canvas,
    block: Canvas.() -> Unit
  ) {
    block(canvas)
  }

  /**
   * 0.0 -> fully collapsed.
   * 1.0 -> fully expanded.
   */
  protected fun ExpandablePageLayout.expandRatio(rv: InboxRecyclerView): Float {
    val anchorHeight = rv.expandedItem.locationOnScreen.height()
    val pageHeight = clippedDimens.height()
    return ((anchorHeight - pageHeight) / (anchorHeight - height).toFloat()).coerceIn(0.0f, 1.0f)
  }

  companion object {
    /**
     * [https://github.com/saket/InboxRecyclerView/tree/master/images/animators/animator_split.mp4]
     */
    @JvmStatic
    fun split(): ItemExpandAnimator = SplitExpandAnimator()

    /**
     * [https://github.com/saket/InboxRecyclerView/tree/master/images/animators/animator_scale.mp4]
     */
    @JvmStatic
    fun scale(): ItemExpandAnimator = ScaleExpandAnimator()

    /**
     * [https://github.com/saket/InboxRecyclerView/tree/master/images/animators/animator_none.mp4]
     */
    @JvmStatic
    fun none(): ItemExpandAnimator = NoneAnimator()
  }
}
