package me.saket.expand.sample.inbox

import me.saket.expand.sample.EmailThread

data class EmailThreadClicked(
    val thread: EmailThread,
    val itemPosition: Int,
    val itemId: Long
)
