# Pull to collapse

### Intercept gesture

`ExpandablePageLayout` currently does not understand nested scrolling, so if the content contains scrollable child Views, the pull-to-collapse gesture will have to be intercepted manually. 

```kotlin
expandablePage.pullToCollapseInterceptor = { downX, downY, upwardPull ->
  val directionInt = if (upwardPull) +1 else -1
  val canScrollFurther = scrollableContainer.canScrollVertically(directionInt)
  if (canScrollFurther) InterceptResult.INTERCEPTED else InterceptResult.IGNORED
}
```

When the scrollable children do not consume the entire space, the `downX` and `downY` parameters can be used to check if the touch actually lies on them. See the [sample app](https://github.com/saket/InboxRecyclerView/blob/cebf081d9398059ecaa9f04909ff3e9c48afd9cf/sample/src/main/java/me/saket/inboxrecyclerview/sample/email/EmailThreadFragment.kt#L65) for an example.

### Collapse threshold

When the content is pulled, `ExpandablePageLayout` uses the default toolbar height (~56dp) as the threshold to decide if the content can be collapsed. This can be customised using,

```kotlin
expandablePage.pullToCollapseThresholdDistance = THRESHOLD_IN_PIXELS
```