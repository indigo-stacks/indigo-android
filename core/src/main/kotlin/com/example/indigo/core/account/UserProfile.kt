package com.example.indigo.core.account

import kotlinx.serialization.Serializable

/** Profile fields used by mobile; API timestamps and future fields are ignored by the API Json. */
@Serializable
data class UserProfile(
    val id: String,
    val name: String,
    val email: String,
    val emailVerified: Boolean,
    val image: String?,
)
