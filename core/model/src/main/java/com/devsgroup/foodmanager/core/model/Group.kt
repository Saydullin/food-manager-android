package com.devsgroup.foodmanager.core.model

/** A household/friend group the user shares dish decisions with. */
data class UserGroup(
    val id: String,
    val emoji: String,
    val name: String,
    val members: List<GroupMember> = emptyList(),
)

/** A member of a [UserGroup]. Pending members haven't accepted their invite yet. */
data class GroupMember(
    val id: String,
    val name: String,
    val role: String,
    val isPending: Boolean = false,
)
