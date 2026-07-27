package com.devsgroup.foodmanager.feature.recommendations.presentation

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import coil3.compose.AsyncImage
import coil3.request.ImageRequest
import coil3.request.crossfade
import com.devsgroup.foodmanager.core.designsystem.theme.FoodManagerTheme

/**
 * Blur strength for the frosted panel behind the text. ~24dp reads as a soft
 * "frosted glass" — enough to melt the photo detail behind the title without
 * turning it into a flat block. (Real blur needs API 31+; see [RecommendationCard].)
 */
private val TextBackdropBlur = 24.dp

/**
 * The recommendation *hero card* from the "Для вас" mock — a full-bleed dish photo
 * with everything else floating on top of it:
 *  - a dark gradient **scrim** so text stays legible over any photo,
 *  - the nutrition / diet / allergen **badges** in the top-left corner,
 *  - a tracked-out **categories** eyebrow and the big dish **title** at the bottom.
 *
 * It lives in the feature (like [RecommendationActionsBar] and [DislikeReasonSheet])
 * because the layered "photo + overlay" treatment is specific to this discovery
 * flow. It stays UI-only: it renders a single [RecommendationUiModel] and holds no
 * state — the deck and which card is on top live in [RecommendationsViewModel].
 *
 * The over-image text is deliberately light in *both* themes: it always sits on the
 * dark scrim, so it does not follow `colorScheme.onSurface`. Only the accent eyebrow
 * borrows `colorScheme.primary` to keep the brand orange in play.
 *
 * Legibility over the photo is built in three cheap-to-expensive layers, bottom-up:
 *  1. a **frosted panel** — a second, blurred copy of the same photo, alpha-masked so
 *     the blur only ramps up over the bottom (behind the text). This melts the busy
 *     detail behind the title while *preserving* the photo, unlike a flat scrim.
 *  2. a **gradient scrim** that dims the top (badges) and darkens the bottom (title).
 *  3. the text's own weight/color.
 *
 * The frosted panel uses [Modifier.blur], which renders only on **API 31+**; on
 * 29–30 it is a no-op, and layer 2 alone still keeps the text readable, so the effect
 * degrades gracefully rather than breaking.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
internal fun RecommendationCard(
    card: RecommendationUiModel,
    modifier: Modifier = Modifier,
) {
    val imageRequest = ImageRequest.Builder(LocalContext.current)
        .data(card.imageUrl)
        .crossfade(300)
        .build()

    Box(
        modifier = modifier
            .fillMaxSize()
            .clip(MaterialTheme.shapes.extraLarge)
            // Neutral placeholder instead of a hard black flash while the photo loads;
            // the crossfade above fades the image in over it.
            .background(MaterialTheme.colorScheme.surfaceVariant),
    ) {
        // The dish photo, cropped to fill the card.
        AsyncImage(
            model = imageRequest,
            contentDescription = card.title,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize(),
        )

        // Frosted panel: a blurred copy of the same photo, drawn full-card so it lines
        // up pixel-for-pixel with the sharp layer, then alpha-masked (BlendMode.DstIn)
        // so the blur is invisible up top and ramps to full over the bottom ~45% —
        // a soft, natural falloff instead of a hard-edged panel. The Offscreen layer
        // is what lets DstIn mask *this* copy only, not the sharp photo beneath it.
        AsyncImage(
            model = imageRequest,
            contentDescription = null,
            contentScale = ContentScale.Crop,
            modifier = Modifier
                .matchParentSize()
                .graphicsLayer { compositingStrategy = CompositingStrategy.Offscreen }
                .drawWithContent {
                    drawContent()
                    drawRect(
                        brush = Brush.verticalGradient(
                            0.0f to Color.Transparent,
                            0.45f to Color.Transparent,
                            1.0f to Color.Black,
                        ),
                        blendMode = BlendMode.DstIn,
                    )
                }
                .blur(TextBackdropBlur),
        )

        // Dark scrim: dim the top (behind the badges) and darken the bottom (behind
        // the title) while leaving the middle of the photo clear. Kept moderate at the
        // bottom so the frosted photo still shows through rather than going flat black.
        Box(
            modifier = Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        0.0f to Color.Black.copy(alpha = 0.45f),
                        0.30f to Color.Transparent,
                        0.55f to Color.Transparent,
                        1.0f to Color.Black.copy(alpha = 0.70f),
                    ),
                ),
        )

        // Top-left badges: nutrition (servings/calories/protein/fat/carbs) on top, then
        // diets and allergens each on their own wrapping row below. FlowRow (rather than
        // Row) lets a 4th/5th badge wrap onto a new line instead of squeezing every
        // badge in the row to fit, which was shrinking the last one unreadably.
        Column(
            modifier = Modifier
                .align(Alignment.TopStart)
                .fillMaxWidth(0.85f)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            if (card.nutritionBadges.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.nutritionBadges.forEach { badge -> OverlayBadge(text = badge) }
                }
            }
            if (card.dietBadges.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.dietBadges.forEach { badge -> OverlayBadge(text = badge) }
                }
            }
            if (card.allergyBadges.isNotEmpty()) {
                FlowRow(horizontalArrangement = Arrangement.spacedBy(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    card.allergyBadges.forEach { badge -> OverlayBadge(text = badge) }
                }
            }
        }

        // Bottom block: the categories eyebrow, the title, and a short description.
        Column(
            modifier = Modifier
                .align(Alignment.BottomStart)
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 24.dp),
            verticalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            if (card.categories.isNotEmpty()) {
                Text(
                    text = card.categories.joinToString("  •  ") { it.uppercase() },
                    style = MaterialTheme.typography.labelLarge,
                    color = MaterialTheme.colorScheme.primary,
                )
            }
            Text(
                text = card.title,
                style = MaterialTheme.typography.displaySmall,
                color = Color.White,
            )
            if (card.description.isNotBlank()) {
                Text(
                    text = card.description,
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.82f),
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis,
                )
            }
        }
    }
}

/**
 * A single overlay pill: light text on a translucent dark chip so it reads over any
 * part of the photo. Used for the corner badges (calories, a diet tag, etc.).
 */
@Composable
private fun OverlayBadge(
    text: String,
    modifier: Modifier = Modifier,
) {
    Text(
        text = text,
        style = MaterialTheme.typography.labelMedium,
        color = Color.White,
        modifier = modifier
            .clip(MaterialTheme.shapes.extraSmall)
            .background(Color.Black.copy(alpha = 0.55f))
            .padding(horizontal = 12.dp, vertical = 6.dp),
    )
}

@Preview
@Composable
private fun RecommendationCardPreview() {
    FoodManagerTheme {
        // A dark backdrop stands in for the photo, which Coil can't load in a preview.
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(560.dp)
                .background(Color(0xFF2A2018)),
        ) {
            RecommendationCard(
                card = RecommendationUiModel(
                    id = "preview",
                    imageUrl = "",
                    title = "Том Ям с креветками",
                    description = "Острый тайский суп на кокосовом молоке с креветками и грибами.",
                    nutritionBadges = listOf("2 порц.", "480 ккал", "18 г белк.", "22 г жир.", "40 г угл."),
                    dietBadges = listOf("Остро"),
                    categories = listOf("Острое", "Тайская"),
                ),
                modifier = Modifier.padding(16.dp),
            )
        }
    }
}
