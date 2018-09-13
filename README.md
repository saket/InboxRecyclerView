InboxRecyclerView is a library for building expandable descendant navigation, inspired by [Google Inbox](http://androidniceties.tumblr.com/post/100872004063/inbox-by-gmail-google-play-link) and [Reply](https://material.io/design/material-studies/reply.html). You can read [my blog post](https://saket.me/inbox-recyclerview) to learn how it was created.

```
implementation 'me.saket.inboxrecyclerview:1.0.0-beta1'
```

### Usage

`InboxRecyclerView` can be dropped in existing projects without requiring any effort. You can take a look at the sample app for best practices or download its APK for trying it out on your phone.

**Layout**

```xml
<me.saket.expand.InboxRecyclerView
  android:layout_width="match_parent"
  android:layout_height="match_parent" />

<me.saket.expand.page.ExpandablePageLayout
  android:layout_width="match_parent"
  android:layout_height="match_parent" />
```

**Expanding content**

```java
recyclerView.setExpandablePage(contentPage)

recyclerViewAdapter.setOnItemClickListener { clickedItem ->
  expandablePageFragment.loadContent(clickedItem)
  recyclerView.expandItem(clickedItem.adapterId)`
}
```


### How do I…

- [customize item expand animations?](https://github.com/saket/InboxRecyclerView/wiki/Item-animations)
- [control the pull-to-collapse gesture?](https://github.com/saket/InboxRecyclerView/wiki/Pull-to-collapse)
- [change the background tint?](https://github.com/saket/InboxRecyclerView/wiki/Background-tint)
- [listen to state changes?](https://github.com/saket/InboxRecyclerView/wiki/Page-callbacks)

### Pull collapsible activities

To maintain consistency across the whole app, the library also includes a `PullCollapsibleActivity` that brings the same animations and gesture to activities with little effort.

**Usage**

Step 1. Extend `PullCollapsibleActivity`.

Step 2. Add these attributes to the activity’s theme:

```xml
<item name=“android:windowIsTranslucent”>true</item>
<item name=“android:colorBackgroundCacheHint”>@null</item>
```

### License
```
Copyright 2019 Saket Narayan.

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
