package me.saket.inboxrecyclerview.expander

import android.graphics.Rect
import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem
import me.saket.inboxrecyclerview.locationOnScreen
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Convenience function for treating [ItemExpander] like a fun interface.
 */
fun <T : Parcelable> ItemExpander(identify: (parent: RecyclerView, item: T) -> View?) =
  object : ItemExpander<T>() {
    override fun identifyExpandingView(parent: RecyclerView, item: T) = identify(parent, item)
  }

/**
 * Identifies expanding list items so that [InboxRecyclerView] can animate between the item
 * and its [ExpandablePageLayout].
 *
 * The default implementation is [AdapterIdBasedItemExpander] that uses adapter IDs, but apps
 * can implement their own expanders if using adapter IDs isn't desired because it's not 2020
 * anymore.
 */
// TODO: rename to InboxItemExpander.
abstract class ItemExpander<T : Parcelable> {
  private var expandedItem: T? = null
  lateinit var recyclerView: InboxRecyclerView

  /**
   * Called when [expandItem] is called and [InboxRecyclerView] needs to find the item's
   * corresponding View. The View is only used for capturing its location on screen. This
   * may be called multiple times while the page is visible if [InboxRecyclerView] detects
   * that the list item may have moved.
   *
   * @param item Item passed to [expandItem].
   *
   * @return When null, the [ExpandablePageLayout] will be expanded from the top of the list.
   */
  abstract fun identifyExpandingView(parent: RecyclerView, item: T): View?

  /**
   * Expand a list item. The item's View will be captured using [identifyExpandingView].
   */
  fun expandItem(item: T?, immediate: Boolean = false) {
    setItem(item)
    recyclerView.expandInternal(immediate = immediate)
  }

  /**
   * Expand the page from the top.
   */
  fun expandFromTop(immediate: Boolean = false) {
    expandItem(item = null, immediate = immediate)
  }

  fun collapse() {
    recyclerView.collapse()
  }

  /**
   * Update the currently expanded item. It's preferred that the item is updated through
   * [expandItem], but this may be used if the expanded item needs to be force-updated
   * while the page is already expanded.
   */
  fun setItem(item: T?) {
    expandedItem = item
  }

  internal fun saveState(outState: Parcelable?): Parcelable {
    return ExpandedItemSavedState(outState, expandedItem)
  }

  @Suppress("UNCHECKED_CAST")
  internal fun restoreState(inState: Parcelable): Parcelable? {
    val savedState = inState as ExpandedItemSavedState<T>
    setItem(savedState.expandedItem)
    return savedState.superState
  }

  internal fun captureExpandedItemInfo(): ExpandedItem {
    val itemView = expandedItem?.let { identifyExpandingView(recyclerView, it) }

    return if (itemView != null) {
      ExpandedItem(
          viewIndex = recyclerView.indexOfChild(itemView),
          // Ignore translations done by the item expand animator.
          locationOnScreen = itemView.locationOnScreen(ignoreTranslations = true)
      )

    } else {
      val locationOnScreen = recyclerView.locationOnScreen()
      val paddedY = locationOnScreen.top + recyclerView.paddingTop // This is where list items will be laid out from.
      ExpandedItem(
          viewIndex = -1,
          locationOnScreen = Rect(locationOnScreen.left, paddedY, locationOnScreen.right, paddedY)
      )
    }
  }
}

@Parcelize
internal data class ExpandedItemSavedState<T : Parcelable>(
  val superState: Parcelable?,
  val expandedItem: T?
) : Parcelable
