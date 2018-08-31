package me.saket.expand.sample.inbox

import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.PublishRelay
import io.reactivex.android.schedulers.AndroidSchedulers.mainThread
import kotterknife.bindView
import me.saket.expand.InboxRecyclerView
import me.saket.expand.page.ExpandablePageLayout
import me.saket.expand.page.SimpleExpandablePageStateChangeCallbacks
import me.saket.expand.sample.EmailRepository
import me.saket.expand.sample.R
import me.saket.expand.sample.about.AboutActivity
import me.saket.expand.sample.email.EmailThreadFragment
import me.saket.expand.sample.widgets.ReversibleAnimatedVectorDrawable
import java.util.concurrent.TimeUnit

class InboxActivity : AppCompatActivity() {

  private val recyclerView by bindView<InboxRecyclerView>(R.id.inbox_recyclerview)
  private val emailPageLayout by bindView<ExpandablePageLayout>(R.id.inbox_email_thread_page)
  private val fab by bindView<FloatingActionButton>(R.id.inbox_fab)
  private val settingsButton by bindView<View>(R.id.inbox_settings)

  private val onDestroy = PublishRelay.create<Any>()
  private val threadsAdapter = ThreadsAdapter()

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_inbox)

    setupThreadList()
    setupThreadPage()
    setupFab()

    settingsButton.setOnClickListener {
      startActivity(AboutActivity.intent(this))
    }
  }

  override fun onDestroy() {
    onDestroy.accept(Any())
    super.onDestroy()
  }

  override fun onSaveInstanceState(outState: Bundle) {
    recyclerView.saveExpandableState(outState)
    super.onSaveInstanceState(outState)
  }

  override fun onRestoreInstanceState(savedInstanceState: Bundle) {
    super.onRestoreInstanceState(savedInstanceState)
    recyclerView.restoreExpandableState(savedInstanceState)
  }

  override fun onBackPressed() {
    if (emailPageLayout.isExpandedOrExpanding) {
      recyclerView.collapse()
    } else {
      super.onBackPressed()
    }
  }

  private fun setupThreadList() {
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.setExpandablePage(emailPageLayout, resources.getDimensionPixelSize(R.dimen.inbox_email_page_pulltocollapse_threshold))

    threadsAdapter.submitList(EmailRepository.threads())
    recyclerView.adapter = threadsAdapter

    threadsAdapter.itemClicks
        // Adding a tiny, non-noticeable delay allows:
        // - the UI to prepare itself before the user can see anything.
        // - the ripple animation to complete
        .delay(100, TimeUnit.MILLISECONDS, mainThread())
        .takeUntil(onDestroy)
        .subscribe {
          recyclerView.expandItem(it.itemId)
        }
  }

  private fun setupThreadPage() {
    var threadFragment = supportFragmentManager.findFragmentById(emailPageLayout.id) as EmailThreadFragment?
    if (threadFragment == null) {
      threadFragment = EmailThreadFragment()
    }

    supportFragmentManager
        .beginTransaction()
        .replace(emailPageLayout.id, threadFragment)
        .commitNowAllowingStateLoss()

    threadsAdapter.itemClicks
        .map { it.thread.id }
        .takeUntil(onDestroy)
        .subscribe(threadFragment)
  }

  private fun setupFab() {
    val editToReplyAllIcon = ReversibleAnimatedVectorDrawable(fab.drawable as AnimatedVectorDrawable)

    emailPageLayout.addStateChangeCallbacks(object : SimpleExpandablePageStateChangeCallbacks() {
      override fun onPageAboutToExpand(expandAnimDuration: Long) {
        editToReplyAllIcon.play()
      }

      override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
        editToReplyAllIcon.reverse()
      }
    })
  }
}
