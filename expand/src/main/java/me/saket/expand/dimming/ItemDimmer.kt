package me.saket.expand.dimming

import android.graphics.Canvas
import me.saket.expand.InboxRecyclerView

/**
 * Draws dimming on [InboxRecyclerView] rows while the page is expanded or expanding.
 */
abstract class ItemDimmer {

  abstract fun onAttachRecyclerView(recyclerView: InboxRecyclerView)

  abstract fun drawDimming(canvas: Canvas)

  companion object {
    fun uncoveredItems(): ItemDimmer {
      return UncoveredItemsDimmer()
    }

    fun allItems(): ItemDimmer {
      return AllItemsDimmer()
    }

    fun noOp(): ItemDimmer {
      return object : ItemDimmer() {
        override fun onAttachRecyclerView(recyclerView: InboxRecyclerView) {}
        override fun drawDimming(canvas: Canvas) {}
      }
    }
  }
}
