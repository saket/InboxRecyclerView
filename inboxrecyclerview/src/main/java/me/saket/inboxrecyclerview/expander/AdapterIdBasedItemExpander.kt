package me.saket.inboxrecyclerview.expander

import android.os.Parcelable
import android.view.View
import androidx.recyclerview.widget.RecyclerView.ViewHolder
import kotlinx.android.parcel.Parcelize

class AdapterIdBasedItemExpander(private val requireStableIds: Boolean) : InboxItemExpander<AdapterIdBasedItem>() {
  override fun identifyExpandingView(
    expandingItem: AdapterIdBasedItem,
    childViewHolders: Sequence<ViewHolder>
  ): ViewHolder? {
    val adapter = recyclerView.adapter!!
    check(requireStableIds && adapter.hasStableIds()) {
      "$adapter needs to have stable IDs so that the expanded item can be restored across " +
          "state restorations. If auto state restoration isn't needed, consider setting " +
          "InboxRecyclerView#itemExpander = AdapterIdBasedItemExpander(requireStableIds = false). " +
          "A custom InboxItemExpander can also be used for expanding items using custom " +
          "Parcelable types instead of adapter IDs."
    }
    return childViewHolders.firstOrNull { it.itemId == expandingItem.adapterId }
  }
}

@Parcelize
data class AdapterIdBasedItem(val adapterId: Long) : Parcelable
