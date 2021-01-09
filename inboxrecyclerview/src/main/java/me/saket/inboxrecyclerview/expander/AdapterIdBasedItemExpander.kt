package me.saket.inboxrecyclerview.expander

import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import me.saket.inboxrecyclerview.AdapterIdBasedItem
import me.saket.inboxrecyclerview.ExpandedItemFinder.FindResult
import me.saket.inboxrecyclerview.ItemExpander

class AdapterIdBasedItemExpander(private val requireStableIds: Boolean) : ItemExpander<AdapterIdBasedItem>() {
  override fun findExpandedItem(parent: RecyclerView, item: AdapterIdBasedItem): FindResult? {
    val adapter = parent.adapter!!
    check(requireStableIds && adapter.hasStableIds()) {
      "$adapter needs to have stable IDs so that the expanded item can be restored across " +
          "orientation changes. If using adapter IDs is not an option, consider setting a " +
          "custom InboxRecyclerView#itemExpandFinder."
    }

    return parent.children.map(parent::getChildViewHolder)
        .filter { it.itemId == item.adapterId }
        .firstOrNull()
        ?.let {
          FindResult(
              itemAdapterPosition = it.adapterPosition,
              itemView = it.itemView
          )
        }
  }
}
