package me.saket.inboxrecyclerview.sample.inbox

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.AnimatedVectorDrawable
import android.os.Bundle
import android.util.TypedValue
import android.util.TypedValue.COMPLEX_UNIT_DIP
import android.view.View
import androidx.appcompat.app.AppCompatActivity
import androidx.appcompat.content.res.AppCompatResources.getDrawable
import androidx.recyclerview.widget.LinearLayoutManager
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.jakewharton.rxrelay2.PublishRelay
import kotterknife.bindView
import me.saket.inboxrecyclerview.InboxRecyclerView
import me.saket.inboxrecyclerview.animation.ItemExpandAnimator
import me.saket.inboxrecyclerview.dimming.DimPainter
import me.saket.inboxrecyclerview.dimming.TintPainter
import me.saket.inboxrecyclerview.page.ExpandablePageLayout
import me.saket.inboxrecyclerview.page.SimplePageStateChangeCallbacks
import me.saket.inboxrecyclerview.sample.EmailRepository
import me.saket.inboxrecyclerview.sample.R
import me.saket.inboxrecyclerview.sample.about.AboutActivity
import me.saket.inboxrecyclerview.sample.email.EmailThreadFragment

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

  @SuppressLint("CheckResult")
  private fun setupThreadList() {
    recyclerView.layoutManager = LinearLayoutManager(this)
    recyclerView.expandablePage = emailPageLayout
    recyclerView.dimPainter = DimPainter.listAndPage(
        listColor = Color.WHITE,
        listAlpha = 0.75F,
        pageColor = Color.WHITE,
        pageAlpha = 0.65f
    )
    recyclerView.itemExpandAnimator = ItemExpandAnimator.scale()
    emailPageLayout.pullToCollapseThresholdDistance = dp(72)

    threadsAdapter.submitList(EmailRepository.threads())
    recyclerView.adapter = threadsAdapter

    threadsAdapter.itemClicks
        .takeUntil(onDestroy)
        .subscribe {
          recyclerView.expandItem(it.itemId)
        }
  }

  @SuppressLint("CheckResult")
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
    val avd = { iconRes: Int -> getDrawable(this, iconRes) as AnimatedVectorDrawable }
    fab.setImageDrawable(avd(R.drawable.avd_edit_to_reply_all))

    emailPageLayout.addStateChangeCallbacks(object : SimplePageStateChangeCallbacks() {
      override fun onPageAboutToExpand(expandAnimDuration: Long) {
        val icon = avd(R.drawable.avd_edit_to_reply_all)
        fab.setImageDrawable(icon)
        icon.start()
      }

      override fun onPageAboutToCollapse(collapseAnimDuration: Long) {
        val icon = avd(R.drawable.avd_reply_all_to_edit)
        fab.setImageDrawable(icon)
        icon.start()
      }
    })
  }
}

private fun Context.dp(value: Int): Int {
  val metrics = resources.displayMetrics
  return TypedValue.applyDimension(COMPLEX_UNIT_DIP, value.toFloat(), metrics).toInt()
}