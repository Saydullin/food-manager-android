package com.devsgroup.foodmanager.ui

import app.cash.turbine.test
import com.devsgroup.foodmanager.core.domain.repository.AuthRepository
import com.devsgroup.foodmanager.core.domain.repository.ProfileRepository
import com.devsgroup.foodmanager.core.domain.repository.SettingsRepository
import com.devsgroup.foodmanager.core.testing.MainDispatcherRule
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.every
import io.mockk.mockk
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test

/**
 * Covers the launch-time resolution of the email-confirmation gate.
 *
 * The stored `emailVerified` flag is only trustworthy on the device that did the
 * verifying, and the gate it feeds is a dead end (the confirmation screen can only ask
 * for a code nobody sent). So a stored "no" has to be checked against the server before
 * the start destination is committed to — but never at the cost of a launch that hangs
 * when the network is gone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class AppViewModelEmailGateTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private val authRepository = mockk<AuthRepository>(relaxed = true)
    private val profileRepository = mockk<ProfileRepository>(relaxed = true)
    private val settingsRepository = mockk<SettingsRepository>(relaxed = true)

    private val loggedIn = MutableStateFlow(true)
    private val emailVerified = MutableStateFlow(false)

    private fun viewModel(): AppViewModel {
        every { authRepository.isLoggedIn } returns loggedIn
        every { authRepository.sessionExpired } returns emptyFlow()
        every { profileRepository.emailVerified } returns emailVerified
        every { profileRepository.onboardingComplete } returns flowOf(true)
        every { settingsRepository.darkThemeEnabled() } returns flowOf(true)
        return AppViewModel(authRepository, profileRepository, settingsRepository)
    }

    @Test
    fun `a stored no is checked against the server before the gate is committed to`() = runTest {
        // The account was verified elsewhere; this install has only ever seen "no".
        coEvery { profileRepository.refresh() } answers { emailVerified.value = true }

        val state = viewModel().uiState
        state.test {
            assertEquals(AppUiState.Loading, awaitItem())
            advanceUntilIdle()
            val ready = expectMostRecentItem() as AppUiState.Ready
            assertEquals("the fetched answer must decide the start destination", true, ready.emailVerified)
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 1) { profileRepository.refresh() }
    }

    @Test
    fun `an already-verified account is not made to wait on a fetch it does not need`() = runTest {
        emailVerified.value = true

        viewModel().uiState.test {
            assertEquals(AppUiState.Loading, awaitItem())
            advanceUntilIdle()
            expectMostRecentItem() as AppUiState.Ready
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { profileRepository.refresh() }
    }

    @Test
    fun `a signed-out launch never fetches — there is no account to ask about`() = runTest {
        loggedIn.value = false

        viewModel().uiState.test {
            assertEquals(AppUiState.Loading, awaitItem())
            advanceUntilIdle()
            expectMostRecentItem() as AppUiState.Ready
            cancelAndIgnoreRemainingEvents()
        }
        coVerify(exactly = 0) { profileRepository.refresh() }
    }

    @Test
    fun `a failed fetch still resolves, keeping the stored answer`() = runTest {
        // Offline launch: the gate falls back to what's stored rather than holding the
        // splash on a dead network, and must not crash on the way through.
        coEvery { profileRepository.refresh() } throws java.io.IOException("offline")

        viewModel().uiState.test {
            assertEquals(AppUiState.Loading, awaitItem())
            advanceUntilIdle()
            val ready = expectMostRecentItem() as AppUiState.Ready
            assertEquals(false, ready.emailVerified)
            cancelAndIgnoreRemainingEvents()
        }
    }
}
