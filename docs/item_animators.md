# Item Animators

```kotlin
recyclerView.itemExpandAnimator = ItemExpandAnimator.split() / scale() / none()
```

`InboxRecyclerView` offers three kinds of animators for animating list items while the content is expanding, collapsing or being pulled.

1. Split [(video)](https://www.youtube.com/watch?v=WQGtweo-2dc)
2. Scale [(video)](https://www.youtube.com/watch?v=a0U8HcvT4G4)
3. None [(video)](https://www.youtube.com/watch?v=87ZbSFgwn8s)

### Customize
By default, a duration of `350ms` and the `FastOutSlowInInterpolator()` interpolator are used for item animations. This can be changed using,

```kotlin
pageLayout.animationDurationMillis = CUSTOM_DURATION
pageLayout.animationInterpolator = CUSTOM_INTERPOLATOR
```

### Make your own

Custom animations can be written by extending [ItemExpandAnimator](https://github.com/saket/InboxRecyclerView/blob/master/expand/src/main/java/me/saket/expand/animation/ItemExpandAnimator.kt):

```kotlin
recyclerView.itemExpandAnimator = object : ItemExpandAnimator() {
  override fun onPageMove(
    recyclerView: InboxRecyclerView,
    page: ExpandablePageLayout,
    anchorViewOverlay: View?
  ) {
    // This function gets called every time the page changes its position or size.
    // You'll want to describe frames of your animation here by syncing the position
    // of list items with the page. Avoid doing anything expensive, just like how
    // you'd treat onDraw().
  }
}
```
