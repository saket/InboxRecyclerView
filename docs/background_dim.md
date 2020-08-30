# Background dim

![dim](images/background_dim.gif)

`InboxRecyclerView` applies soft dimming on the list when it's covered by `ExpandablePageLayout`. The dimming is shifted to the page when it's pulled past the collapse threshold, as a visual indication that the page can now be released.

By default, a black dim with an alpha of 15% is applied on the list. This can be customized by using,

```kotlin
recyclerView.dimPainter = DimPainter.listAndPage(color = Color.WHITE, alpha = 0.65F)
```

If you wish to only apply dimming on the list, `DimPainter.listOnly(...)` can be used.

It’s also encouraged that apps find other creative ways of communicating this to the user. [Dank](https://saket.me/dank), for example, uses the status bar color to indicate when the content is eligible for collapse.

![](images/status_bar_tint.gif)
