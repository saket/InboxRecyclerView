package me.saket.inboxrecyclerview

import android.os.Parcelable
import android.view.View
import androidx.core.view.children
import androidx.recyclerview.widget.RecyclerView
import kotlinx.android.parcel.Parcelize
import me.saket.inboxrecyclerview.ExpandedItemFinder.FindResult
import me.saket.inboxrecyclerview.page.ExpandablePageLayout

/**
 * Identifies an expanding item's location on screen from where an [ExpandablePageLayout] can expand
 * from as well as collapse to. The default implementation uses adapter IDs but apps can implement
 * their own finder if using adapter IDs isn't desired.
 *
 * The finder is unfortunately not type safe because of limitations of a View's lifecycle.
 */
fun interface ExpandedItemFinder {
  /**
   * @return When null, the content will be expanded from the top of the list.
   */
  fun findExpandedItem(parent: RecyclerView, id: Parcelable): FindResult?

  data class FindResult(
    val itemAdapterPosition: Int,
    val itemView: View?
  )
}

@Parcelize
internal data class DefaultExpandedItemId(val adapterId: Long) : Parcelable

internal class DefaultExpandedItemFinder : ExpandedItemFinder {
  override fun findExpandedItem(parent: RecyclerView, id: Parcelable): FindResult? {
    val adapter = parent.adapter!!
    check(id is DefaultExpandedItemId) { "Expected the expanded item ID to be of type Long." }
    check(adapter.hasStableIds()) {
      "$adapter needs to have stable IDs so that the expanded item can be restored across " +
          "orientation changes. If using adapter IDs is not an option, consider setting a " +
          "custom InboxRecyclerView#itemExpandFinder."
    }

    return parent.children.map(parent::getChildViewHolder)
        .filter { it.itemId == id.adapterId }
        .firstOrNull()
        ?.let {
          FindResult(
              itemAdapterPosition = it.adapterPosition,
              itemView = it.itemView
          )
        }
  }
}
