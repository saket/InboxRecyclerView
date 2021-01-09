package me.saket.inboxrecyclerview.expander

import android.os.Parcelable
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize

class AdapterIdBasedItemExpander(private val requireStableIds: Boolean) : ItemExpander<AdapterIdBasedItem>() {
  override fun identifyExpandingItem(parent: RecyclerView, item: AdapterIdBasedItem): IdentifiedExpandingItem? {
    val adapter = parent.adapter!!
    check(requireStableIds && adapter.hasStableIds()) {
      "$adapter needs to have stable IDs so that the expanded item can be restored across " +
          "orientation changes. If using adapter IDs is not an option, consider setting a " +
          "custom InboxRecyclerView#itemExpander = AdapterIdBasedItemExpander(requireStableIds = false)."
    }

    return parent.children.map(parent::getChildViewHolder)
        .filter { it.itemId == item.adapterId }
        .firstOrNull()
        ?.let {
          IdentifiedExpandingItem(
              itemAdapterPosition = it.adapterPosition,
              itemView = it.itemView
          )
        }
  }
}

@Parcelize
data class AdapterIdBasedItem(val adapterId: Long) : Parcelable
