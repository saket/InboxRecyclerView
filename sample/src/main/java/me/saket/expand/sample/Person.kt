package me.saket.expand.sample

import androidx.annotation.DrawableRes

data class Person(
    val name: String,
    @DrawableRes val profileImageRes: Int? = null
)
