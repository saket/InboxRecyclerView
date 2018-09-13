package me.saket.inboxrecyclerview.sample

import androidx.annotation.DrawableRes

data class Person(
    val name: String,
    @DrawableRes val profileImageRes: Int? = null
)
