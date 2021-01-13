package me.saket.inboxrecyclerview.page

import android.view.View
import androidx.core.view.NestedScrollingParent3

/**
 * Provides default implementations of [NestedScrollingParent3]'s APIs that we don't care about.
 */
interface SimpleNestedScrollingParent3 : NestedScrollingParent3 {
  override fun onNestedScrollAccepted(child: View, target: View, axes: Int, type: Int) = Unit

  override fun onNestedScroll(
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int,
    consumed: IntArray
  ) = Unit

  override fun onNestedScroll(
    target: View,
    dxConsumed: Int,
    dyConsumed: Int,
    dxUnconsumed: Int,
    dyUnconsumed: Int,
    type: Int
  ) = Unit
}
