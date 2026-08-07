package com.devsgroup.foodmanager.core.data.repository

import app.cash.turbine.test
import com.devsgroup.foodmanager.core.data.worker.ProfileSyncScheduler
import com.devsgroup.foodmanager.core.datastore.AuthPreferencesDataSource
import com.devsgroup.foodmanager.core.datastore.ProfilePreferenceList
import com.devsgroup.foodmanager.core.datastore.ProfilePreferencesDataSource
import com.devsgroup.foodmanager.core.model.UserProfile
import com.devsgroup.foodmanager.core.network.profile.ProfileApi
import com.devsgroup.foodmanager.core.network.profile.model.ProfileResponse
import com.devsgroup.foodmanager.core.network.profile.model.ProfileUserDto
import com.devsgroup.foodmanager.core.network.upload.UploadApi
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.IOException

/**
 * Covers the onboarding gate's decision rules, which are the part of this repository most
 * able to regress silently: getting them wrong means either re-interrogating users who
 * already answered (the bug this replaced) or never asking anyone.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultProfileRepositoryOnboardingTest {

    private val profileApi = mockk<ProfileApi>(relaxed = true)
    private val uploadApi = mockk<UploadApi>(relaxed = true)
    private val profilePrefs = mockk<ProfilePreferencesDataSource>(relaxed = true)
    private val authPrefs = mockk<AuthPreferencesDataSource>(relaxed = true)
    private val syncScheduler = mockk<ProfileSyncScheduler>(relaxed = true)

    private val storedProfile = MutableStateFlow<UserProfile?>(null)

    private fun repository(): DefaultProfileRepository {
        coEvery { profilePrefs.profileFlow } returns storedProfile
        coEvery { profilePrefs.currentProfile() } answers { storedProfile.value }
        coEvery { profilePrefs.pendingSyncLists() } returns emptySet<ProfilePreferenceList>()
        coEvery { authPrefs.accountBannedFlow } returns flowOf(false)
        return DefaultProfileRepository(
            profileApi = profileApi,
            uploadApi = uploadApi,
            profilePrefs = profilePrefs,
            authPrefs = authPrefs,
            profileSyncScheduler = syncScheduler,
            json = Json,
            ioDispatcher = UnconfinedTestDispatcher(),
        )
    }

    private fun dto(onboardingCompleted: Boolean) = ProfileResponse(
        ProfileUserDto(
            id = "u1",
            username = "sasha",
            name = "Саша",
            onboardingCompleted = onboardingCompleted,
        ),
    )

    private fun localProfile(onboardingCompleted: Boolean) = UserProfile(
        name = "Саша",
        username = "@sasha",
        onboardingCompleted = onboardingCompleted,
    )

    // --- The gate -----------------------------------------------------------

    @Test
    fun `reports complete when nothing is known yet, so a login never re-asks`() = runTest {
        // Logout wipes the cached profile; straight after a login this is the state until
        // the fetch lands. Defaulting to "ask" here is exactly the bug being avoided.
        storedProfile.value = null
        repository().onboardingComplete.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reports incomplete for an account the server says has never been asked`() = runTest {
        // Registration seeds a baseline profile with the flag false, so a genuinely new
        // user still gets the questions.
        storedProfile.value = localProfile(onboardingCompleted = false)
        repository().onboardingComplete.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reports complete for an account that has already answered`() = runTest {
        storedProfile.value = localProfile(onboardingCompleted = true)
        repository().onboardingComplete.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Latching -----------------------------------------------------------

    @Test
    fun `completing onboarding records it locally and on the account`() = runTest {
        storedProfile.value = localProfile(onboardingCompleted = false)
        coEvery { profileApi.completeOnboarding() } returns dto(onboardingCompleted = true)
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        repository().completeOnboarding()

        coVerify(exactly = 1) { profileApi.completeOnboarding() }
        assertTrue("the persisted profile must carry the latch", saved.captured.onboardingCompleted)
    }

    @Test
    fun `completing onboarding offline still records it locally and does not throw`() = runTest {
        storedProfile.value = localProfile(onboardingCompleted = false)
        coEvery { profileApi.completeOnboarding() } throws IOException("offline")
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        // Must not propagate: the user finished the questions and should leave the flow
        // regardless of whether the latch reached the server.
        repository().completeOnboarding()

        assertTrue(saved.captured.onboardingCompleted)
    }

    // --- Reconciliation on refresh -----------------------------------------

    @Test
    fun `refresh re-pushes a latch the server never received`() = runTest {
        // The completion happened while offline: local says done, server still says never.
        storedProfile.value = localProfile(onboardingCompleted = true)
        coEvery { profileApi.myProfile() } returns dto(onboardingCompleted = false)
        coEvery { profileApi.completeOnboarding() } returns dto(onboardingCompleted = true)
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        repository().refresh()

        coVerify(exactly = 1) { profileApi.completeOnboarding() }
        assertTrue("must not adopt the server's older 'never asked'", saved.captured.onboardingCompleted)
    }

    @Test
    fun `refresh keeps the local answer when the re-push also fails`() = runTest {
        storedProfile.value = localProfile(onboardingCompleted = true)
        coEvery { profileApi.myProfile() } returns dto(onboardingCompleted = false)
        coEvery { profileApi.completeOnboarding() } throws IOException("still offline")
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        repository().refresh()

        assertTrue("a failed retry must not put the user back through onboarding", saved.captured.onboardingCompleted)
    }

    @Test
    fun `refresh does not push when the server already agrees`() = runTest {
        storedProfile.value = localProfile(onboardingCompleted = true)
        coEvery { profileApi.myProfile() } returns dto(onboardingCompleted = true)

        repository().refresh()

        coVerify(exactly = 0) { profileApi.completeOnboarding() }
    }

    @Test
    fun `refresh adopts the server saying not-yet when this device never claimed otherwise`() = runTest {
        // A fresh install of an account that genuinely never onboarded: the questions
        // should still be asked, so refresh must not invent a completion.
        storedProfile.value = localProfile(onboardingCompleted = false)
        coEvery { profileApi.myProfile() } returns dto(onboardingCompleted = false)
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        repository().refresh()

        coVerify(exactly = 0) { profileApi.completeOnboarding() }
        assertEquals(false, saved.captured.onboardingCompleted)
    }
}
