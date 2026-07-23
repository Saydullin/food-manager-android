package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import androidx.navigation.toRoute
import com.devsgroup.foodmanager.core.domain.repository.GroupRepository
import com.devsgroup.foodmanager.core.model.GroupMember
import com.devsgroup.foodmanager.feature.profile.navigation.GroupDetailRoute
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import javax.inject.Inject
import kotlin.time.Duration.Companion.seconds

data class GroupDetailUiState(
    val groupName: String = "",
    val members: List<GroupMember> = emptyList(),
    val inviteSheetOpen: Boolean = false,
    val inviteInput: String = "",
    val toast: String? = null,
)

private data class LocalState(
    val inviteSheetOpen: Boolean = false,
    val inviteInput: String = "",
    val toast: String? = null,
)

@HiltViewModel
class GroupDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val groupRepository: GroupRepository,
) : ViewModel() {

    private val groupId = savedStateHandle.toRoute<GroupDetailRoute>().groupId
    private val localState = MutableStateFlow(LocalState())

    val uiState: StateFlow<GroupDetailUiState> = combine(
        groupRepository.group(groupId),
        localState,
    ) { group, local ->
        GroupDetailUiState(
            groupName = group?.name.orEmpty(),
            members = group?.members.orEmpty(),
            inviteSheetOpen = local.inviteSheetOpen,
            inviteInput = local.inviteInput,
            toast = local.toast,
        )
    }.stateIn(
        scope = viewModelScope,
        started = SharingStarted.WhileSubscribed(5_000),
        initialValue = GroupDetailUiState(),
    )

    fun onOpenInvite() {
        localState.update { it.copy(inviteSheetOpen = true) }
    }

    fun onDismissInvite() {
        localState.update { it.copy(inviteSheetOpen = false, inviteInput = "") }
    }

    fun onInviteInputChange(value: String) {
        localState.update { it.copy(inviteInput = value) }
    }

    fun onSendInvite() {
        val name = localState.value.inviteInput.trim()
        if (name.isBlank()) return
        viewModelScope.launch { groupRepository.inviteMember(groupId, name) }
        localState.update { it.copy(inviteSheetOpen = false, inviteInput = "") }
    }

    /** "Предложить" isn't built yet — surfaces a "coming soon" toast like other stub actions. */
    fun onOfferClick() {
        showToast("Скоро будет доступно")
    }

    private fun showToast(message: String) {
        localState.update { it.copy(toast = message) }
        viewModelScope.launch {
            delay(TOAST_DURATION.inWholeMilliseconds)
            localState.update { if (it.toast == message) it.copy(toast = null) else it }
        }
    }

    private companion object {
        val TOAST_DURATION = 2.seconds
    }
}
