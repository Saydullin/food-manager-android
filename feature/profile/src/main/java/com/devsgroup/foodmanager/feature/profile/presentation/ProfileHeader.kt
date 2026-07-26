package com.devsgroup.foodmanager.feature.profile.presentation

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.devsgroup.foodmanager.core.designsystem.R
import com.devsgroup.foodmanager.core.designsystem.component.FoodIconButton
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme
import com.devsgroup.foodmanager.core.model.CookingAttitude
import com.devsgroup.foodmanager.core.model.UserProfile

/**
 * The profile screen's header row: avatar, name, username, an optional age/cooking-
 * attitude meta line, and the edit-profile entry point.
 */
@Composable
internal fun ProfileHeader(
    profile: UserProfile,
    onEditClick: () -> Unit,
    modifier: Modifier = Modifier,
    isBanned: Boolean = false,
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(15.dp),
    ) {
        ProfileAvatar(profile = profile, modifier = Modifier.size(74.dp))

        Column(modifier = Modifier.weight(1f)) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                Text(
                    text = profile.name,
                    style = MaterialTheme.typography.headlineSmall,
                    color = MaterialTheme.colorScheme.onBackground,
                )
                if (isBanned) {
                    SuspendedTag()
                }
            }
            Text(
                text = profile.username,
                style = MaterialTheme.typography.labelLarge,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(top = 6.dp),
            )
            profile.metaLine()?.let { meta ->
                Text(
                    text = meta,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.padding(top = 4.dp),
                )
            }
        }

        FoodIconButton(
            icon = painterResource(R.drawable.ic_edit),
            contentDescription = "Изменить профиль",
            onClick = onEditClick,
        )
    }
}

/** Small red pill shown next to the name when the account has been suspended. */
@Composable
private fun SuspendedTag(modifier: Modifier = Modifier) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(6.dp),
        color = MaterialTheme.colorScheme.error,
    ) {
        Text(
            text = "заблокирован",
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onError,
            modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp),
        )
    }
}

@Composable
private fun ProfileAvatar(profile: UserProfile, modifier: Modifier = Modifier) {
    val avatarUrl = profile.avatarUrl
    if (avatarUrl != null) {
        var isLoaded by remember(avatarUrl) { mutableStateOf(false) }
        val alpha by animateFloatAsState(
            targetValue = if (isLoaded) 1f else 0f,
            animationSpec = tween(durationMillis = 300),
            label = "avatarAlpha",
        )
        val imageRequest = ImageRequest.Builder(LocalContext.current)
            .data(avatarUrl)
            .crossfade(true)
            .build()

        Box(modifier = modifier.clip(CircleShape)) {
            // Neutral placeholder shown until the photo finishes loading, so the
            // avatar fades in instead of popping in over a blank circle.
            Box(
                modifier = Modifier
                    .matchParentSize()
                    .background(MaterialTheme.colorScheme.surfaceVariant),
            )
            AsyncImage(
                model = imageRequest,
                contentDescription = profile.name,
                contentScale = ContentScale.Crop,
                onSuccess = { isLoaded = true },
                modifier = Modifier
                    .matchParentSize()
                    .alpha(alpha),
            )
        }
    } else {
        Surface(
            modifier = modifier,
            shape = CircleShape,
            color = MaterialTheme.colorScheme.primaryContainer,
        ) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Text(
                    text = profile.name.firstOrNull()?.uppercase() ?: "?",
                    style = MaterialTheme.typography.headlineMedium,
                    color = MaterialTheme.colorScheme.onPrimaryContainer,
                )
            }
        }
    }
}

private fun UserProfile.metaLine(): String? {
    val parts = listOfNotNull(
        age?.let { pluralize(it, one = "год", few = "года", many = "лет") },
        cookingAttitude?.label,
    )
    return parts.takeIf { it.isNotEmpty() }?.joinToString(" · ")
}

@Preview
@Composable
private fun ProfileHeaderPreview() {
    FoodManagerTheme {
        Surface {
            ProfileHeader(
                profile = UserProfile(
                    name = "Мария",
                    username = "@chef_maria",
                    age = 28,
                    cookingAttitude = CookingAttitude.LOVE_COOKING,
                ),
                onEditClick = {},
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
