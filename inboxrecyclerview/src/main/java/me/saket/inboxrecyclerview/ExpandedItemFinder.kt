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
data class AdapterIdBasedItem(val adapterId: Long) : Parcelable
