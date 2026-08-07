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
import io.mockk.mockk
import io.mockk.slot
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Covers the email-confirmation gate's decision rules. Getting these wrong is worse than
 * the onboarding equivalent: a wrongly gated account can't get *out* of the confirmation
 * screen, since it can only ask for a code the server was never told to send.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class DefaultProfileRepositoryEmailVerificationTest {

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

    private fun dto(email: String?, emailVerified: Boolean) = ProfileResponse(
        ProfileUserDto(
            id = "u1",
            username = "sasha",
            name = "Саша",
            email = email,
            emailVerified = emailVerified,
        ),
    )

    private fun localProfile(email: String?, emailVerified: Boolean) = UserProfile(
        name = "Саша",
        username = "@sasha",
        email = email,
        emailVerified = emailVerified,
    )

    // --- The gate -----------------------------------------------------------

    @Test
    fun `reports verified when nothing is known yet, so a login is never gated on a guess`() = runTest {
        // Logout wipes the cached profile, and a code login lands here until the fetch
        // completes. Gating on an unknown answer is the lockout this default prevents.
        storedProfile.value = null
        repository().emailVerified.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reports unverified for an account the server says has not confirmed`() = runTest {
        // Registration seeds a baseline profile carrying the server's answer, so a
        // genuinely unverified account still gets the gate.
        storedProfile.value = localProfile(email = "sasha@example.com", emailVerified = false)
        repository().emailVerified.test {
            assertEquals(false, awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `reports verified for a confirmed account`() = runTest {
        storedProfile.value = localProfile(email = "sasha@example.com", emailVerified = true)
        repository().emailVerified.test {
            assertTrue(awaitItem())
            cancelAndIgnoreRemainingEvents()
        }
    }

    // --- Who owns the answer ------------------------------------------------

    @Test
    fun `refresh adopts the server's verification state over a stale local no`() = runTest {
        // The account was verified elsewhere (another device, or before a reinstall);
        // this install has never seen it happen. Carrying the local answer forward here
        // is what used to strand such a user on the confirmation screen forever.
        storedProfile.value = localProfile(email = null, emailVerified = false)
        coEvery { profileApi.myProfile() } returns dto("sasha@example.com", emailVerified = true)
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        repository().refresh()

        assertTrue("the server's confirmation must win", saved.captured.emailVerified)
        assertEquals("sasha@example.com", saved.captured.email)
    }

    @Test
    fun `refresh adopts a server-side email change that cleared verification`() = runTest {
        // POST auth/email/add moves the address and resets emailVerified server-side —
        // possibly from another device — so a local "verified" must not survive it.
        storedProfile.value = localProfile(email = "old@example.com", emailVerified = true)
        coEvery { profileApi.myProfile() } returns dto("new@example.com", emailVerified = false)
        val saved = slot<UserProfile>()
        coEvery { profilePrefs.saveProfile(capture(saved)) } answers { storedProfile.value = saved.captured }

        repository().refresh()

        assertFalse("a pending address is not a confirmed one", saved.captured.emailVerified)
        assertEquals("new@example.com", saved.captured.email)
    }
}
