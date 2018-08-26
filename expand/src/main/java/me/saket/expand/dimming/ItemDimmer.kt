package me.saket.expand.dimming

import android.graphics.Canvas
import me.saket.expand.InboxRecyclerView
import me.saket.expand.page.ExpandablePageLayout

/**
 * Draws dimming on [InboxRecyclerView] rows while the page is expanded or expanding.
 */
abstract class ItemDimmer {

  lateinit var page: ExpandablePageLayout
  lateinit var recyclerView: InboxRecyclerView

  abstract fun onPageDetached(page: ExpandablePageLayout)

  abstract fun onPageAttached()

  abstract fun drawDimming(canvas: Canvas)

  companion object {
    fun uncoveredItems(): ItemDimmer {
      return UncoveredItemsDimmer()
    }

    fun allItems(): ItemDimmer {
      return AllItemsDimmer()
    }
  }
}
