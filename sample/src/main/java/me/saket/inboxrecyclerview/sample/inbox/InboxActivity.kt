package me.saket.inboxrecyclerview.sample.inbox

import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.PublishRelay
import kotterknife.bindView
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.dimming.TintPainter
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.SimplePageStateChangeCallbacks
import me.saket.inboxrecyclerview.sample.EmailRepository
import me.saket.inboxrecyclerview.sample.R
import me.saket.inboxrecyclerview.sample.about.AboutActivity
import me.saket.inboxrecyclerview.sample.email.EmailThreadFragment
import me.saket.inboxrecyclerview.sample.widgets.ReversibleAnimatedVectorDrawable

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

  override fun onBackPressed() {
    if (emailPageLayout.isExpandedOrExpanding) {
      recyclerView.collapse()
    } else {
      super.onBackPressed()
    }
  }

  private fun setupThreadList() {
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.expandablePage = emailPageLayout
    recyclerView.tintPainter = TintPainter.uncoveredArea(color = Color.WHITE, opacity = 0.65F)

    threadsAdapter.submitList(EmailRepository.threads())
    recyclerView.adapter = threadsAdapter

    threadsAdapter.itemClicks
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
        .subscribe {
          threadFragment.populate(it)
        }
  }

  private fun setupFab() {
    val editToReplyAllIcon = ReversibleAnimatedVectorDrawable(fab.drawable as AnimatedVectorDrawable)

    emailPageLayout.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageAboutToExpand(expandAnimDuration: Long) {
        editToReplyAllIcon.play()
      }

      override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
        editToReplyAllIcon.reverse()
      }
    })
  }
}
