package app.cashflow.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cashflow.core.Money
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors
import app.cashflow.ui.theme.LocalMotion
import app.cashflow.ui.util.MoneyWords

/*
 * Shared night-current control language. Every interactive element rests on
 * the same three shapes — 12dp controls, 14dp fields/chips, 16dp action rows —
 * and every icon-only control carries both a spoken message and a 44dp+ touch
 * target. Citron is the only accent; lavender marks uncertainty; red marks
 * failure and the destructive moment. Press states breathe 0.96 at 160ms and
 * respect the reduce-motion setting.
 */

/** Tiny uppercase kicker: labels over fields, tiles and the summary. */
@Composable
fun Kicker(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text,
        style = CamelType.Kicker,
        color = color ?: LocalCashflowColors.current.muted,
        modifier = modifier,
    )
}

/** The primary action shape: citron filled, restrained press. */
@Composable
fun ActionButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    icon: ImageVector? = null,
) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val scale by pressScale(interaction, motion)
    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .background(if (enabled) c.citron else c.surface)
            .clickable(interactionSource = interaction, indication = null, enabled = enabled, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 14.dp)
            .semantics { contentDescription = label },
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = if (enabled) c.onCitron else c.muted,
                modifier = Modifier.size(19.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        Text(
            text = label,
            style = CamelType.VoiceBody,
            color = if (enabled) c.onCitron else c.muted,
        )
    }
}

/** Quiet outlined action on charcoal — secondary choices, never competition. */
@Composable
fun GhostButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tint: Color? = null,
) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction, motion)
    Row(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(16.dp))
            .border(1.dp, c.line, RoundedCornerShape(16.dp))
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 18.dp, vertical = 13.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.Center,
    ) {
        Text(text = label, style = CamelType.VoiceBody, color = tint ?: c.muted)
    }
}

/** The destructive moment's button: red words on a red hairline, never filled. */
@Composable
fun DangerGhostButton(label: String, onClick: () -> Unit, modifier: Modifier = Modifier) {
    GhostButton(label = label, onClick = onClick, modifier = modifier, tint = LocalCashflowColors.current.red)
}

private const val BADGE_TONE = 0xFF191327.toInt()

private fun badgeTone(): Color = Color(BADGE_TONE)

/** The lavender numeric badge: waits/uncertainty, never success. */
@Composable
fun CountBadge(count: Int, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    Box(
        modifier = modifier
            .padding(top = 2.dp, end = 2.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(c.lavender)
            .padding(horizontal = 4.dp, vertical = 1.dp),
    ) {
        Text(
            text = count.toString(),
            style = CamelType.Kicker,
            fontSize = 10.sp,
            color = badgeTone(),
        )
    }
}

/**
 * The compact icon control used across Home's header: 44dp, rounded 14, surface
 * ground when resting is colorless — meaning rides on the glyph, not a plate.
 * Selected ("on") state = citron glyph on citron wash; a lavender count badge
 * can ride the top end.
 */
@Composable
fun Ictl(
    icon: ImageVector,
    description: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    on: Boolean = false,
    count: Int = 0,
    tint: Color? = null,
) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction, motion)
    Box(
        modifier = modifier
            .scale(scale)
            .size(44.dp)
            .clip(RoundedCornerShape(14.dp))
            .then(if (on) Modifier.background(c.citronWash) else Modifier)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .semantics { contentDescription = description },
        contentAlignment = Alignment.Center,
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = when {
                on -> c.citron
                tint != null -> tint
                else -> c.muted
            },
            modifier = Modifier.size(20.dp),
        )
        if (count > 0) CountBadge(count = count, modifier = Modifier.align(Alignment.TopEnd))
    }
}

/** A filter chip pill: surface when resting, citron from selected. */
@Composable
fun OptionChip(label: String, selected: Boolean, onClick: () -> Unit, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    val motion = LocalMotion.current
    val interaction = remember { MutableInteractionSource() }
    val scale by pressScale(interaction, motion)
    Box(
        modifier = modifier
            .scale(scale)
            .clip(RoundedCornerShape(999.dp))
            .background(if (selected) c.citron else c.surface)
            .clickable(interactionSource = interaction, indication = null, onClick = onClick)
            .padding(horizontal = 14.dp)
            .heightIn(min = 44.dp)
            .semantics { contentDescription = label },
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = label,
            style = CamelType.LedgerLabel,
            color = if (selected) c.onCitron else c.muted,
        )
    }
}

/**
 * Closed glyph currency picker: folded, it shows only the currency symbol
 * (reading ~19sp so a symbol such as ₦ stays legible); opening it lists every
 * supported member — including JPY and KWD — as "full name — ISO — symbol",
 * plus an explicit Unknown option when asked. The underlying value stays the
 * ISO code; screen readers announce the selection in full.
 */
@Composable
fun CurrencyGlyphPicker(
    selected: String,
    onSelect: (String?) -> Unit,
    modifier: Modifier = Modifier,
    includeUnknown: Boolean = false,
    descriptionBase: String = "Currency",
) {
    val c = LocalCashflowColors.current
    var open by remember { mutableStateOf(false) }
    val unknown = !MoneyWords.isSupported(selected)
    val glyph = if (unknown) "?" else MoneyWords.symbol(selected)
    val spoken = if (unknown) {
        "$descriptionBase: unknown — it stays unknown until you pick one"
    } else {
        "$descriptionBase: ${MoneyWords.currencyLong(selected)}"
    }
    Box(modifier = modifier) {
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(12.dp))
                .background(c.surface)
                .clickable { open = true }
                .padding(horizontal = 12.dp, vertical = 9.dp)
                .semantics { contentDescription = spoken },
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(6.dp),
        ) {
            Text(
                text = glyph,
                style = CamelType.VoiceTitle,
                color = if (unknown) c.faint else c.ink,
                fontSize = 19.sp,
            )
            Icon(
                imageVector = CFIcons.Caret,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(12.dp),
            )
        }
        DropdownMenu(
            expanded = open,
            onDismissRequest = { open = false },
            modifier = Modifier.clip(RoundedCornerShape(16.dp)),
            containerColor = c.sheet,
        ) {
            Column(Modifier.heightIn(max = 420.dp).verticalScroll(rememberScrollState())) {
                if (includeUnknown) {
                    CurrencyOptionRow(
                        title = "Unknown \u2014 currency stays unknown until picked",
                        selected = unknown,
                        symbolWord = null,
                        onSelect = { onSelect(null); open = false },
                    )
                }
                Money.currencies.forEach { code ->
                    CurrencyOptionRow(
                        title = MoneyWords.currencyLong(code) + " \u2014 " + code,
                        selected = code == selected,
                        symbolWord = MoneyWords.symbol(code),
                        onSelect = { onSelect(code); open = false },
                    )
                }
            }
        }
    }
}

/**
 * One open-chooser row: full currency name, the symbol at the end; the ISO
 * is spoken as part of the merged description and shown beside the symbol.
 */
@Composable
private fun CurrencyOptionRow(
    title: String,
    symbolWord: String?,
    selected: Boolean,
    onSelect: () -> Unit,
) {
    val c = LocalCashflowColors.current
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(if (selected) c.citronWash else Color.Transparent)
            .clickable(onClick = onSelect)
            .padding(horizontal = 14.dp, vertical = 12.dp)
            .semantics(mergeDescendants = true) {},
    ) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                text = title,
                style = CamelType.VoiceBody,
                color = if (selected) c.citron else c.ink,
                modifier = Modifier.weight(1f),
            )
            Row(horizontalArrangement = Arrangement.spacedBy(6.dp), verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = symbolWord ?: "",
                    style = CamelType.VoiceBody,
                    color = if (selected) c.onCitron else c.muted,
                )
            }
        }
    }
}

/**
 * The live grey of a pressed control: one shared press-scale for the
 * night-current press voice, gated by reduce motion.
 */
@Composable
private fun pressScale(
    interaction: MutableInteractionSource,
    reduced: app.cashflow.ui.theme.Motion,
): androidx.compose.runtime.State<Float> {
    val pressed = interaction.collectIsPressedAsState()
    return animateFloatAsState(
        targetValue = if (pressed.value && !reduced.reduced) 0.96f else 1f,
        animationSpec = tween(if (reduced.reduced) 0 else 160),
        label = "press",
    )
}
