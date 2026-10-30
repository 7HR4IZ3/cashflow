package app.cashflow.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

/**
 * Empty and blocked states: one centered voice headline, one sentence, and
 * actions below. No all-clear variant exists anywhere — when there is
 * nothing to say, this composable is simply not composed.
 */
@Composable
fun StateBlock(
    headline: String,
    sentence: String,
    modifier: Modifier = Modifier,
    glyph: ImageVector? = null,
    actions: @Composable ColumnScope.() -> Unit = {},
) {
    val c = LocalCashflowColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 36.dp, horizontal = 24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (glyph != null) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = c.faint,
                modifier = Modifier.size(34.dp),
            )
            Spacer(Modifier.height(14.dp))
        }
        Text(text = headline, style = CamelType.VoiceTitle, color = c.ink)
        Spacer(Modifier.height(8.dp))
        Text(text = sentence, style = CamelType.VoiceBody, color = c.muted, textAlign = androidx.compose.ui.text.style.TextAlign.Center)
        actions()
    }
}

/**
 * Quiet guide card: purpose headline plus one honest sentence plus an
 * optional action slot. Used by the principles and setup cards.
 */
@Composable
fun GuideCard(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    extra: (@Composable ColumnScope.() -> Unit)? = null,
) {
    val c = LocalCashflowColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(c.surface)
            .padding(horizontal = 18.dp, vertical = 16.dp),
    ) {
        Text(text = headline, style = CamelType.VoiceTitle, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(text = body, style = CamelType.VoiceBody, color = c.muted)
        if (extra != null) {
            Spacer(Modifier.height(12.dp))
            extra()
        }
    }
}

/** The citron word button used across full screens, always word-labelled. */
@Composable
fun GreenButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    Box(modifier = modifier) {
        ActionButton(label = label, onClick = onClick, enabled = enabled)
    }
}
