package me.saket.inboxrecyclerview.dimming

import android.graphics.Color

object TintPainter {
  @JvmStatic
  @JvmOverloads
  @Deprecated(
      "Use listAndPage() instead",
      ReplaceWith("DimPainter.listAndPage(color, opacity)")
  )
  fun uncoveredArea(color: Int = Color.BLACK, opacity: Float = 0.15F) =
    DimPainter.listAndPage(color, opacity)

  @JvmStatic
  @JvmOverloads
  @Deprecated(
      "No longer supported. Use listAndPage() instead",
      ReplaceWith("DimPainter.listAndPage(color, opacity)")
  )
  fun completeList(color: Int = Color.BLACK, opacity: Float = 0.15F) =
    DimPainter.listAndPage(color, opacity)

  @JvmStatic
  @Deprecated("Use none() instead", ReplaceWith("DimPainter.none()"))
  fun noOp() = DimPainter.none()
}