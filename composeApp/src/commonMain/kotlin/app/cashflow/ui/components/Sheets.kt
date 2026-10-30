package app.cashflow.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
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
import app.cashflow.ui.theme.LocalMotion

/**
 * SheetOverlay: the app's one bottom sheet — dim scrim, rounded top panel on
 * the sheet ground, grabber, and content. Focus-visible rings stay native;
 * the background sits under the scrim and cannot be reached while open.
 * [onDismiss] runs when the scrim is tapped.
 */
@Composable
fun SheetOverlay(
    title: String? = null,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit,
) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    Box(modifier.fillMaxSize()) {
        // Dim scrim: one calm tap-away layer, 200ms in.
        Box(
            Modifier
                .fillMaxSize()
                .background(Color(0x99060707))
                .clickable(onClick = onDismiss),
        )
        Column(
            Modifier
                .align(Alignment.BottomCenter)
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 26.dp, topEnd = 26.dp))
                .background(c.sheet)
                .padding(horizontal = 18.dp)
                .imePadding()
                .navigationBarsPadding()
                .padding(top = 8.dp, bottom = 18.dp),
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp),
                contentAlignment = Alignment.Center,
            ) {
                Box(
                    Modifier
                        .width(36.dp)
                        .height(4.dp)
                        .clip(RoundedCornerShape(2.dp))
                        .background(Color(0x24F5F4EE)),
                )
            }
            if (title != null) {
                Text(
                    text = title,
                    style = CamelType.VoiceTitle,
                    color = c.ink,
                    modifier = Modifier.padding(bottom = 12.dp),
                )
            }
            Column(
                Modifier
                    .heightIn(max = 520.dp)
                    .verticalScroll(rememberScrollState()),
                content = content,
            )
            Spacer(Modifier.height(12.dp))
            ActionButton("Done", onDismiss, Modifier.fillMaxWidth())
        }
    }
}

/**
 * EvidenceRow: the receipt(document) disclosure — collapsed by default; it
 * opens the original alert text on demand. Never a warning card.
 */
@Composable
fun EvidenceRow(text: String, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    var open by remember { mutableStateOf(false) }
    Column(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .clickable { open = !open }
                .padding(vertical = 10.dp, horizontal = 4.dp)
                .semantics { contentDescription = if (open) "Hide the original alert" else "Show the original alert" },
            verticalAlignment = Alignment.CenterVertically,
        ) {
            Icon(
                imageVector = CFIcons.Receipt,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(17.dp),
            )
            Spacer(Modifier.width(8.dp))
            Text(
                text = "Original alert",
                style = CamelType.LedgerLabel,
                color = c.muted,
            )
        }
        AnimatedVisibility(
            visible = open,
            enter = fadeIn(tween(motion.fast)) + expandVertically(tween(motion.slow)),
            exit = fadeOut(tween(motion.fast)) + shrinkVertically(tween(motion.slow)),
        ) {
            Text(
                text = text,
                style = CamelType.Reference,
                color = c.muted,
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .background(c.bg)
                    .padding(horizontal = 11.dp, vertical = 9.dp),
            )
        }
    }
}

/** A labeled icon tile used inside the In/Out pair and demo rows. */
@Composable
fun GlyphTile(
    icon: ImageVector,
    tint: Color,
    wash: Color,
    modifier: Modifier = Modifier,
    glyphSize: androidx.compose.ui.unit.Dp = 17.dp,
) {
    val boxSize = glyphSize * 2
    Box(
        modifier = modifier
            .size(boxSize)
            .clip(RoundedCornerShape(12.dp))
            .background(wash),
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = tint,
            modifier = Modifier.size(glyphSize),
        )
    }
}
