package com.devsgroup.foodmanager.core.data.repository

import com.devsgroup.foodmanager.core.domain.repository.GroupRepository
import com.devsgroup.foodmanager.core.model.GroupMember
import com.devsgroup.foodmanager.core.model.UserGroup
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

/**
 * In-memory implementation: holds groups for the process lifetime. There are none by
 * default — back this with Preferences DataStore (:core:datastore) to persist them
 * across sessions.
 */
@Singleton
class DefaultGroupRepository @Inject constructor() : GroupRepository {

    private val groups = MutableStateFlow<List<UserGroup>>(emptyList())

    override fun groups(): Flow<List<UserGroup>> = groups.asStateFlow()

    override fun group(groupId: String): Flow<UserGroup?> =
        groups.map { list -> list.find { it.id == groupId } }

    override suspend fun createGroup(name: String): UserGroup {
        val group = UserGroup(
            id = UUID.randomUUID().toString(),
            emoji = DEFAULT_GROUP_EMOJI,
            name = name,
            members = listOf(
                GroupMember(id = UUID.randomUUID().toString(), name = "Вы", role = "Организатор"),
            ),
        )
        groups.update { it + group }
        return group
    }

    override suspend fun deleteGroup(groupId: String) {
        groups.update { list -> list.filterNot { it.id == groupId } }
    }

    override suspend fun inviteMember(groupId: String, name: String) {
        groups.update { list ->
            list.map { group ->
                if (group.id == groupId) {
                    val member = GroupMember(
                        id = UUID.randomUUID().toString(),
                        name = name,
                        role = "Приглашён(а)",
                        isPending = true,
                    )
                    group.copy(members = group.members + member)
                } else {
                    group
                }
            }
        }
    }

    private companion object {
        const val DEFAULT_GROUP_EMOJI = "👥"
    }
}
