package com.devsgroup.foodmanager.core.designsystem.component

import android.text.method.LinkMovementMethod
import android.widget.TextView
import androidx.compose.material3.LocalContentColor
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.text.HtmlCompat

/**
 * Renders a server-supplied HTML string (e.g. a recipe article's [content]) as native
 * text. Compose has no built-in HTML renderer, so this wraps a plain [TextView] fed
 * through [HtmlCompat.fromHtml] — the only HTML source in the app.
 */
@Composable
fun HtmlText(
    html: String,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
    color: Color = LocalContentColor.current,
) {
    val density = LocalDensity.current
    val textColorArgb = color.toArgb()
    val textSizeSp = style.fontSize.value
    val lineSpacingExtraPx = with(density) {
        (style.lineHeight.toPx() - style.fontSize.toPx()).coerceAtLeast(0f)
    }
    AndroidView(
        modifier = modifier,
        factory = { context ->
            TextView(context).apply {
                movementMethod = LinkMovementMethod.getInstance()
            }
        },
        update = { textView ->
            textView.setTextColor(textColorArgb)
            textView.textSize = textSizeSp
            textView.setLineSpacing(lineSpacingExtraPx, 1f)
            textView.text = HtmlCompat.fromHtml(html, HtmlCompat.FROM_HTML_MODE_COMPACT)
        },
    )
}
