package me.saket.expand.sample.inbox

import android.os.Bundle
import androidx.appcompat.app.AppCompatActivity
import com.google.android.material.bottomappbar.BottomAppBar
import kotterknife.bindView
import me.saket.expand.ExpandablePageLayout
import me.saket.expand.InboxRecyclerView
import me.saket.expand.sample.EmailRepository
import me.saket.expand.sample.R

class InboxActivity : AppCompatActivity() {

  private val recyclerView by bindView<InboxRecyclerView>(R.id.inbox_recyclerview)
  private val emailPageLayout by bindView<ExpandablePageLayout>(R.id.inbox_email_thread_page)
  private val bottomAppBar by bindView<BottomAppBar>(R.id.inbox_bottomappbar)

  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    setContentView(R.layout.activity_inbox)

    setupThreadList()
  }

  private fun setupThreadList() {
    recyclerView.layoutManager = recyclerView.createLayoutManager()
    recyclerView.setExpandablePage(emailPageLayout)

    val adapter = ThreadsAdapter(clickListener = { emailThread -> })
    adapter.submitList(EmailRepository.threads())
    recyclerView.adapter = adapter
  }
}
