package me.saket.inboxrecyclerview

import android.graphics.Rect
import android.os.Parcelable
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.ExpandedItemFinder.FindResult
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Identifies an expanding item's location on screen from where an [ExpandablePageLayout] can expand
 * from and collapse to. The default implementation uses adapter IDs but apps can implement
 * their own finder if using adapter IDs isn't desired because it's not 20th century anymore.
 */
abstract class ItemExpander<T: Parcelable> {
  private var expandedItem: T? = null
  internal lateinit var recyclerView: InboxRecyclerView

  /**
   * @param item List item to expand, passed through [InboxRecyclerView.expandItem].
   *
   * @return When null, the content will be expanded from the top of the list.
   */
  abstract fun findExpandedItem(parent: RecyclerView, item: T): FindResult?

  fun expandItem(item: T?, immediate: Boolean = false) {
    setItem(item)
    recyclerView.expandInternal(immediate = immediate)
  }

  fun expandFromTop(immediate: Boolean = false) {
    expandItem(item = null, immediate = immediate)
  }

  fun collapse() {
    recyclerView.collapse()
  }

  open fun saveState(outState: Parcelable?): Parcelable {
    return ExpandedItemSavedState(outState, expandedItem)
  }

  @Suppress("UNCHECKED_CAST")
  open fun restoreState(inState: Parcelable): Parcelable? {
    val savedState = inState as ExpandedItemSavedState<T>
    expandedItem = savedState.expandedItem
    return savedState.superState
  }

  internal fun captureExpandedItemInfo(recyclerView: InboxRecyclerView): ExpandedItem {
    val findResult = expandedItem?.let { findExpandedItem(recyclerView, it) }
    val itemView = findResult?.itemView

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

  /**
   * InboxRecyclerView rejects duplicate calls to [expandItem] if the page is already expanded.
   * If the expanded item still needs to be updated for some reason (for eg., if the page was
   * immediately expanded from an arbitrary location earlier but can now collapse to an actual
   * list item), this can be used.
   */
  fun forceUpdateExpandedItem(item: T?) {
    setItem(item)
  }

  fun setItem(item: T?) {
    expandedItem = item
  }
}

@Parcelize
internal data class ExpandedItemSavedState<T: Parcelable>(
  val superState: Parcelable?,
  val expandedItem: T?
) : Parcelable

