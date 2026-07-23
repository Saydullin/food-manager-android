package com.devsgroup.foodmanager.core.domain.repository

import com.devsgroup.foodmanager.core.model.UserGroup
import kotlinx.coroutines.flow.Flow

/** Contract for the household/friend groups the user shares dish decisions with. */
interface GroupRepository {

    fun groups(): Flow<List<UserGroup>>

    fun group(groupId: String): Flow<UserGroup?>

    /** Creates a new group (with the current user as its first member) and returns it. */
    suspend fun createGroup(name: String): UserGroup

    suspend fun deleteGroup(groupId: String)

    /** Adds a pending member to the group by name (a real invite send is future work). */
    suspend fun inviteMember(groupId: String, name: String)
}
