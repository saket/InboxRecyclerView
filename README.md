[![](https://github.com/saket/InboxRecyclerView/blob/master/images/static_thumbnail.jpg)](docs/images/animators)

InboxRecyclerView is a library for building expandable descendant navigation, inspired by [Google Inbox](http://androidniceties.tumblr.com/post/100872004063/inbox-by-gmail-google-play-link) and [Reply](https://material.io/design/material-studies/reply.html). If you're interested in learning how it was created, [here's a detailed blog post](https://saket.me/inbox-recyclerview).

```groovy
implementation 'me.saket:inboxrecyclerview:2.1.0'
```

FYI, `InboxRecyclerView` has a dependency on `androidx`. If you haven't [migrated](https://android-developers.googleblog.com/2018/05/hello-world-androidx.html) from the support library already, this would be a good opportunity.

### Usage

`InboxRecyclerView` can be dropped in existing projects without requiring any effort. You can take a look at the [sample app](https://github.com/saket/InboxRecyclerView/tree/master/sample) for best practices or [download its APK](https://github.com/saket/InboxRecyclerView/releases) for trying it out on your phone.

**Layout**

```xml
<me.saket.inboxrecyclerview.InboxRecyclerView
  android:layout_width="match_parent"
  android:layout_height="match_parent" />

<!--
  This is where your expandable content will be present. One
  way of using it would be to add a Fragment inside the layout
  and update it when any list item is clicked.

  It's recommended that the content page has a higher z-index
  than the list. This can be achieved by either giving it a
  higher view position or a higher elevation.
-->
<me.saket.inboxrecyclerview.page.ExpandablePageLayout
  android:layout_width="match_parent"
  android:layout_height="match_parent"
  android:background="@color/window_background" />
```

**Expanding content**

```kotlin
val page: ExpandablePageLayout = findViewById(...)
page.pushParentToolbarOnExpand(toolbar)

recyclerView.expandablePage = page
recyclerView.tintPainter = TintPainter.listAndPage(Color.WHITE, alpha = 0.65f)
recyclerView.itemExpandAnimator = ItemExpandAnimator.scale() // or split() / none()

recyclerViewAdapter.itemClickListener = { clickedItem ->
  // Load content inside expandablePage here.
  recyclerView.expandItem(clickedItem.adapterId)
}
```

### How do I…

- [customize item expand animations?](docs/item_animators.md)
- [control the pull-to-collapse gesture?](docs/pull_to_collapse.md)
- [change the background dim?](docs/background_dim.md)
- [listen to state changes?](docs/page_callbacks.md)

### Pull collapsible activities

To maintain consistency across the whole app, this library also includes a `PullCollapsibleActivity` that brings the same animations and gesture to activities with little effort.

Step 1. Extend `PullCollapsibleActivity`.

Step 2. Add these attributes to the activity’s theme:

```xml
<item name=“android:windowIsTranslucent”>true</item>
<item name=“android:colorBackgroundCacheHint”>@null</item>
```

### License
```
Copyright 2018 Saket Narayan.

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

   http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```
