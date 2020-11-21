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
 * from and collapse to. The default implementation uses adapter IDs but apps can implement
 * their own finder if using adapter IDs isn't desired because it's not 20th century anymore.
 *
 * The finder is unfortunately not type safe because of limitations of View's lifecycle.
 */
fun interface ExpandedItemFinder {
  /**
   * @param id ID of the expanded item passed through [InboxRecyclerView.expandItem].
   *
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

/**
 * @param requireStableIds Stable IDs are recommended for correctly collapsing an [ExpandablePageLayout]
 * back to its original list item after a state restoration (like an orientation change). Feel free to disable
 * this if your adapter overrides [RecyclerView.Adapter.getItemId] without setting
 * [RecyclerView.Adapter.setHasStableIds]=true.
 */
class DefaultExpandedItemFinder(
  private val requireStableIds: Boolean
) : ExpandedItemFinder {
  override fun findExpandedItem(parent: RecyclerView, id: Parcelable): FindResult? {
    val adapter = parent.adapter!!
    check(id is DefaultExpandedItemId) { "Expected the expanded item ID to be of type Long." }
    check(requireStableIds && adapter.hasStableIds()) {
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
