package app.cashflow.ui.components

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

/**
 * CaptureBanner: the one-sentence attention row. When nothing needs attention
 * the banner is absent entirely; there is no all-clear variant to keep empty.
 * Words name the observed fact and a fix action; never "healthy" or "listening".
 */
@Composable
fun CaptureBanner(
    tint: Color,
    wash: Color,
    glyph: ImageVector,
    message: String,
    actionLabel: String,
    onAction: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val c = LocalCashflowColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(wash)
            .border(0.5.dp, tint.copy(alpha = 0.35f), RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Icon(imageVector = glyph, contentDescription = null, tint = tint, modifier = Modifier.size(20.dp))
        Spacer(Modifier.width(10.dp))
        Text(
            text = message,
            modifier = Modifier.weight(1f),
            style = CamelType.LedgerBody,
            color = c.ink,
        )
        TextButton(onClick = onAction) {
            Text(text = actionLabel, style = CamelType.VoiceBody, color = tint)
        }
    }
}

/**
 * StateBlock: the paper's state page (empty, permission blocked, storage
 * failure). Voice headline, one sentence, action buttons in a flow.
 */
@Composable
fun StateBlock(
    headline: String,
    sentence: String,
    modifier: Modifier = Modifier,
    glyph: ImageVector? = null,
    actions: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit = {},
) {
    val c = LocalCashflowColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 32.dp, horizontal = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (glyph != null) {
            Icon(
                imageVector = glyph,
                contentDescription = null,
                tint = c.ink3,
                modifier = Modifier.size(36.dp),
            )
            Spacer(Modifier.height(14.dp))
        }
        Text(
            text = headline,
            style = CamelType.VoiceHeadline,
            color = c.ink,
        )
        Spacer(Modifier.height(8.dp))
        Text(
            text = sentence,
            style = CamelType.VoiceBody,
            color = c.ink2,
        )
        ConnectorUnderline(modifier = Modifier.padding(vertical = 16.dp))
    }
    Column(horizontalAlignment = Alignment.CenterHorizontally, content = actions)
}

/**
 * ChecklistTile: setup guide step state. Dashed (not started), half (in
 * progress), done, failed with a fix link. Words come with every state.
 */
@Composable
fun ChecklistTile(
    label: String,
    state: ChecklistState,
    modifier: Modifier = Modifier,
    detail: String? = null,
    fixLabel: String? = null,
    onFix: (() -> Unit)? = null,
) {
    val c = LocalCashflowColors.current
    val (tint, glyph, spoken) = when (state) {
        ChecklistState.DONE -> Triple(c.green, Icons.Outlined.CheckCircle as ImageVector?, "Done")
        ChecklistState.IN_PROGRESS -> Triple(c.amber, Icons.Outlined.Refresh as ImageVector?, "In progress")
        ChecklistState.NOT_STARTED -> Triple(c.ink3, null, "Not started")
        ChecklistState.MISSING -> Triple(c.fail, Icons.Outlined.Warning as ImageVector?, "Missing, needs fixing")
    }
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.raise)
            .border(0.5.dp, c.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 12.dp, vertical = 10.dp)
            .semantics(mergeDescendants = true) {},
        verticalAlignment = Alignment.CenterVertically,
    ) {
        if (glyph != null) {
            Icon(
                imageVector = glyph,
                contentDescription = spoken,
                tint = tint,
                modifier = Modifier.size(20.dp),
            )
        } else {
            Canvas(modifier = Modifier.size(20.dp).semantics { contentDescription = spoken }) {
                drawCircle(
                    color = tint,
                    radius = size.minDimension / 2f - 1.5f,
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 1.5.dp.toPx()),
                )
            }
        }
        Spacer(Modifier.width(10.dp))
        Column(Modifier.weight(1f)) {
            Text(text = label, style = CamelType.VoiceBody, color = c.ink)
            if (detail != null) {
                Text(text = detail, style = CamelType.LedgerBody, color = c.ink2)
            }
        }
        if (fixLabel != null && onFix != null) {
            TextButton(onClick = onFix) {
                Text(text = fixLabel, style = CamelType.VoiceBody, color = tint)
            }
        }
    }
}

enum class ChecklistState { DONE, IN_PROGRESS, NOT_STARTED, MISSING }

/**
 * Quiet card used for guide explanations: purpose sentence plus optional slot.
 */
@Composable
fun GuideCard(
    headline: String,
    body: String,
    modifier: Modifier = Modifier,
    extra: (@Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit)? = null,
) {
    val c = LocalCashflowColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .background(c.raise)
            .border(0.5.dp, c.line, RoundedCornerShape(16.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
    ) {
        Text(text = headline, style = CamelType.VoiceTitle, color = c.ink)
        Spacer(Modifier.height(6.dp))
        Text(text = body, style = CamelType.VoiceBody, color = c.ink2)
        if (extra != null) {
            Spacer(Modifier.height(12.dp))
            extra()
        }
    }
}

/** The plain green word-button used across screens, always word-labelled. */
@Composable
fun GreenButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
) {
    val c = LocalCashflowColors.current
    Button(
        onClick = onClick,
        modifier = modifier,
        enabled = enabled,
        colors = ButtonDefaults.buttonColors(
            containerColor = c.green,
            contentColor = c.onGreen,
            disabledContainerColor = c.lineSoft,
            disabledContentColor = c.ink4,
        ),
        shape = RoundedCornerShape(12.dp),
    ) {
        Text(text = label, style = CamelType.VoiceBody, modifier = Modifier.semantics { contentDescription = label })
    }
}

/** Quiet text link direction (buttons like "Set up later" stay calm). */
@Composable
fun QuietLink(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    color: Color? = null,
) {
    val c = LocalCashflowColors.current
    TextButton(onClick = onClick, modifier = modifier) {
        Text(
            text = label,
            style = CamelType.VoiceBody,
            color = color ?: c.ink2,
            modifier = Modifier.semantics { contentDescription = label },
        )
    }
}
