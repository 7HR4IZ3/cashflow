package app.cashflow.ui.components

import androidx.compose.foundation.background
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import app.cashflow.ui.theme.CamelType
import app.cashflow.ui.theme.LocalCashflowColors

/**
 * The app's one input. It sits on the recessed [CashflowColors.field] ground —
 * always one tone past the surrounding stage — with no border to fight, so the
 * "write here" signal comes from the surface itself. Text never truncates:
 * single-line fields scroll, multi-line fields grow.
 */
@Composable
fun PaperField(
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    leading: ImageVector? = null,
    singleLine: Boolean = true,
    minLines: Int = 1,
    keyboardOptions: KeyboardOptions? = null,
    textStyle: TextStyle? = null,
) {
    val c = LocalCashflowColors.current
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.field)
            .padding(horizontal = 12.dp, vertical = 11.dp),
        verticalAlignment = if (singleLine) Alignment.CenterVertically else Alignment.Top,
    ) {
        if (leading != null) {
            Icon(
                imageVector = leading,
                contentDescription = null,
                tint = c.muted,
                modifier = Modifier.size(18.dp),
            )
            Spacer(Modifier.width(8.dp))
        }
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.weight(1f),
            textStyle = (textStyle ?: CamelType.VoiceBody).copy(color = c.ink),
            cursorBrush = SolidColor(c.citron),
            singleLine = singleLine,
            minLines = minLines,
            keyboardOptions = keyboardOptions ?: KeyboardOptions.Default,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = CamelType.VoiceBody, color = c.faint)
                    }
                    inner()
                }
            },
        )
    }
}

/** An inline field with an uppercase kicker label above the input line. */
@Composable
fun LabelledField(
    label: String,
    value: String,
    onValueChange: (String) -> Unit,
    placeholder: String,
    modifier: Modifier = Modifier,
    keyboardOptions: KeyboardOptions? = null,
    textStyle: TextStyle? = null,
    minLines: Int = 1,
) {
    val c = LocalCashflowColors.current
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .background(c.field)
            .padding(horizontal = 12.dp, vertical = 8.dp),
    ) {
        Text(text = label, style = CamelType.Kicker, color = c.muted)
        Spacer(Modifier.height(2.dp))
        BasicTextField(
            value = value,
            onValueChange = onValueChange,
            modifier = Modifier.fillMaxWidth(),
            textStyle = (textStyle ?: CamelType.VoiceBody).copy(color = c.ink),
            cursorBrush = SolidColor(c.citron),
            singleLine = minLines == 1,
            minLines = minLines,
            keyboardOptions = keyboardOptions ?: KeyboardOptions.Default,
            decorationBox = { inner ->
                Box {
                    if (value.isEmpty()) {
                        Text(text = placeholder, style = (textStyle ?: CamelType.VoiceBody), color = c.faint)
                    }
                    inner()
                }
            },
        )
    }
}

/** A one-line helper caption under rows and fields. */
@Composable
fun SideNote(text: String, modifier: Modifier = Modifier, color: Color? = null) {
    Text(
        text = text,
        modifier = modifier,
        style = CamelType.LedgerBody,
        color = color ?: LocalCashflowColors.current.muted,
    )
}

/** A label + value receipt row; hairline rule beneath, tabular value. */
@Composable
fun KvRow(label: String, value: String, modifier: Modifier = Modifier) {
    val c = LocalCashflowColors.current
    Column(modifier = modifier.fillMaxWidth()) {
        Row(Modifier.fillMaxWidth().padding(vertical = 9.dp)) {
            Text(text = label, style = CamelType.VoiceBody, color = c.muted)
            Spacer(Modifier.weight(1f))
            Text(
                text = value,
                style = CamelType.LedgerAmount,
                color = c.ink,
                modifier = Modifier.padding(start = 12.dp),
            )
        }
        Box(
            Modifier
                .fillMaxWidth()
                .height(1.dp)
                .background(c.hairline),
        )
    }
}
