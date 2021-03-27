package me.saket.inboxrecyclerview.expander

import android.graphics.Rect
import android.os.Parcelable
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItemLocation
import me.saket.inboxrecyclerview.locationOnScreen
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Convenience function for treating [InboxItemExpander] like a fun interface.
 */
@Suppress("FunctionName")
fun <T : Parcelable> InboxItemExpander(identifier: ExpandingViewIdentifier<T>): InboxItemExpander<T> {
  return object : InboxItemExpander<T>() {
    override fun identifyExpandingView(expandingItem: T, childViewHolders: Sequence<ViewHolder>) =
      identifier.identifyExpandingView(expandingItem, childViewHolders)
  }
}

/**
 * Identifies expanding list items so that [InboxRecyclerView] can animate between the item
 * and its [ExpandablePageLayout].
 *
 * The default implementation is [AdapterIdBasedItemExpander] that uses adapter IDs, but apps
 * can implement their own expanders if using adapter IDs isn't desired because it's not 2020
 * anymore.
 *
 * Example usage:
 *
 * ```
 * InboxItemExpander { expandingItem, viewHolders ->
 *   viewHolders.firstOrNull { it.{some identifier} == expandingItem }
 * }
 * ```
 */
abstract class InboxItemExpander<T : Parcelable> : ExpandingViewIdentifier<T> {
  lateinit var recyclerView: InboxRecyclerView
  private var expandedItem: T? = null

  /**
   * Expand a list item. The item's View will be captured using [identifyExpandingView].
   */
  fun expandItem(item: T?, immediate: Boolean = false) {
    setItem(item)
    recyclerView.expandOnceLaidOut(immediate = immediate)
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

  internal fun captureExpandedItemInfo(): ExpandedItemLocation {
    val itemView = expandedItem?.let { expandedItem ->
      identifyExpandingView(
          expandingItem = expandedItem,
          childViewHolders = recyclerView.children.map(recyclerView::getChildViewHolder)
      )?.itemView
    }

    return if (itemView != null) {
      ExpandedItemLocation(
          viewIndex = recyclerView.indexOfChild(itemView),
          // Ignore translations done by the item expand animator.
          locationOnScreen = itemView.locationOnScreen(ignoreTranslations = true)
      )

    } else {
      val locationOnScreen = recyclerView.locationOnScreen()
      val paddedY = locationOnScreen.top + recyclerView.paddingTop // This is where list items will be laid out from.
      ExpandedItemLocation(
          viewIndex = -1,
          locationOnScreen = Rect(locationOnScreen.left, paddedY, locationOnScreen.right, paddedY)
      )
    }
  }
}

fun interface ExpandingViewIdentifier<T> {
  /**
   * Called when [InboxItemExpander.expandItem] is called and [InboxRecyclerView] needs to find
   * the item's corresponding View. The View is only used for capturing its location on screen.
   * This may be called multiple times while the page is visible if [InboxRecyclerView] detects
   * that the list item may have moved.
   *
   * @param expandingItem Item passed to [InboxItemExpander.expandItem].
   * @param childViewHolders ViewHolders for [InboxRecyclerView] visible list items.
   *
   * @return When null, the [ExpandablePageLayout] will be expanded from the top of the list.
   */
  fun identifyExpandingView(expandingItem: T, childViewHolders: Sequence<ViewHolder>): ViewHolder?
}

@Parcelize
internal data class ExpandedItemSavedState<T : Parcelable>(
  val superState: Parcelable?,
  val expandedItem: T?
) : Parcelable
