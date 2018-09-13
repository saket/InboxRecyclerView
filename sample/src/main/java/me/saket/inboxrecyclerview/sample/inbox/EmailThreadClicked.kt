package me.saket.inboxrecyclerview.sample.inbox

import me.saket.inboxrecyclerview.sample.EmailThread

data class EmailThreadClicked(
    val thread: EmailThread,
    val itemId: Long
)
