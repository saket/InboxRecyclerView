# Page callbacks

`ExpandablePageLayout` has four different states.

- `PageState.COLLAPSING`
- `PageState.COLLAPSED`
- `PageState.EXPANDING`
- `PageState.EXPANDED`

These can be accessed using `ExpandablePage#currentState` or by registering callbacks,

```kotlin
expandablePage.addStateChangeCallbacks(object: SimplePageStateChangeCallbacks() {
  override fun onPageAboutToExpand(expandAnimDuration: Long) {
  override fun onPageExpanded() {}
  override fun onPageAboutToCollapse(collapseAnimDuration: Long) {}
  override fun onPageCollapsed() {}
})
```

#### Overridable functions
`ExpandablePageLayout` offers the same set of callbacks as open functions that can be overridden when subclassed. This can also be useful for apps that use a View driven navigation stack instead of multiple Activities or Fragments.

```kotlin
class Screen(context: Context) : ExpandablePageLayout(context) {
  override fun onPageAboutToExpand(expandAnimDuration: Long) {}
  override fun onPageExpanded() {}
  override fun onPageAboutToCollapse(collapseAnimDuration: Long) {}
  override fun onPageCollapsed() {}
}
```

#### Pull-to-collapse gesture

```kotlin
expandablePage.addOnPullListener(object: SimpleOnPullListener() {
  override fun onPull(...) {}
  override fun onRelease(collapseEligible: Boolean) {}
})
```

