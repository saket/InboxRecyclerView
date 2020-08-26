# Pull to collapse

```kotlin
pageLayout.pullToCollapseListener.apply {
  // Threshold distance for collapsing the page when 
  // its pulled (upwards/downwards). Defaults to 56dp.
  collapseDistanceThreshold = THRESHOLD_IN_PIXELS
  
  // The page isn't moved with the same speed as the finger. Some friction 
  // is applied to make the gesture feel nice. This friction is increased 
  // further once the page is eligible for collapse as a visual indicator 
  // that the page should no longer be dragged. Setting this to 1f will 
  // remove the friction entirely. Defaults to 3.5f.
  pullFrictionFactor = 3.5f
}
```

### Intercepting pulls

`ExpandablePageLayout` currently does not understand nested scrolling, so if the content contains scrollable child Views, the pull-to-collapse gesture will have to be intercepted manually.

```kotlin
expandablePage.pullToCollapseInterceptor = { downX, downY, upwardPull ->
  val directionInt = if (upwardPull) +1 else -1
  val canScrollFurther = scrollableContainer.canScrollVertically(directionInt)
  if (canScrollFurther) InterceptResult.INTERCEPTED else InterceptResult.IGNORED
}
```

When the scrollable children do not consume the entire space, the `downX` and `downY` parameters can be used to check if the touch actually lies on them. See the [sample app](https://github.com/saket/InboxRecyclerView/blob/cebf081d9398059ecaa9f04909ff3e9c48afd9cf/sample/src/main/java/me/saket/inboxrecyclerview/sample/email/EmailThreadFragment.kt#L65) for an example.
