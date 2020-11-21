package me.saket.inboxrecyclerview

import android.os.Parcelable
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.InboxRecyclerView.ExpandedItem

internal class StateRestorer(private val recyclerView: InboxRecyclerView) {
  private var itemToRestore = ExpandedItem.EMPTY

  internal fun save(outState: Parcelable?): Parcelable {
    return SavedState(outState, recyclerView.expandedItem)
  }

  internal fun restore(inState: Parcelable): Parcelable? {
    val savedState = inState as SavedState
    itemToRestore = savedState.expandedItem
    restoreIfPossible()
    return savedState.superState
  }

  internal fun restoreIfPossible() {
    val adapter = recyclerView.adapter
    val page = recyclerView.expandablePage

    if (itemToRestore.isEmpty().not() && page != null && adapter != null) {
      val canAutoRestore = itemToRestore.id is DefaultExpandedItemId || recyclerView.expandedItemFinder !is DefaultExpandedItemFinder
      check(canAutoRestore) {
        "Failed to auto restore InboxRecyclerView's state without a custom expandedItemFinder that can identify " +
            "expanded item of type ${itemToRestore.id!!::class.java}. Make sure your expandedItemFinder is set " +
            "before state restoration. Otherwise consider disabling state restoration for your InboxRecyclerView " +
            "(isSaveEnabled=false)."
      }

      recyclerView.expandItem(itemToRestore.id!!, immediate = true)
      itemToRestore = ExpandedItem.EMPTY
    }
  }
}

@Parcelize
private data class SavedState(
  val superState: Parcelable?,
  val expandedItem: ExpandedItem
) : Parcelable
