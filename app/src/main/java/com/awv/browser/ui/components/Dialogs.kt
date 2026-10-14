package com.awv.browser.ui.components

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.awv.browser.ui.theme.Awv
import com.awv.browser.ui.theme.awvText

/**
 * Centered dialog on a dimmed background. Tapping outside or pressing Back dismisses it.
 * Its BackHandler is registered after the screen's own, so it gets Back first.
 */
@Composable
fun AwvDialog(onDismiss: () -> Unit, content: @Composable ColumnScope.() -> Unit) {
    BackHandler(onBack = onDismiss)
    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black.copy(alpha = .5f))
            .pointerInput(Unit) { detectTapGestures { onDismiss() } }
            .imePadding()
            .padding(horizontal = 28.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .background(Awv.Card, RoundedCornerShape(28.dp))
                .pointerInput(Unit) { detectTapGestures { } }
                .padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp),
            content = content
        )
    }
}

/** Title + message + Cancel / [confirmLabel]. */
@Composable
fun ConfirmDialog(
    title: String,
    message: String,
    confirmLabel: String,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AwvDialog(onDismiss) {
        BasicText(title, style = awvText(20, FontWeight.ExtraBold, Awv.Ink, ls = -.3f), maxLines = 2, overflow = TextOverflow.Ellipsis)
        BasicText(
            message,
            style = awvText(14, FontWeight.Normal, Awv.Muted, lh = 21),
            maxLines = 6, overflow = TextOverflow.Ellipsis
        )
        DialogButtons(confirmLabel, onDismiss, onConfirm)
    }
}

/**
 * "Add shortcut" form. [onSave] gets the typed name and address and returns an error message
 * to show under the fields, or null when the shortcut was saved (the caller then closes the dialog).
 */
@Composable
fun AddShortcutDialog(onSave: (name: String, address: String) -> String?, onDismiss: () -> Unit) {
    var name by remember { mutableStateOf("") }
    var address by remember { mutableStateOf("") }
    var error by remember { mutableStateOf<String?>(null) }
    val focus = remember { FocusRequester() }
    val save = { error = onSave(name, address) }

    LaunchedEffect(Unit) { runCatching { focus.requestFocus() } }

    AwvDialog(onDismiss) {
        BasicText("Add shortcut", style = awvText(20, FontWeight.ExtraBold, Awv.Ink, ls = -.3f))
        DialogField(
            value = name,
            onValueChange = { name = it.take(24); error = null },
            hint = "Name (optional)",
            keyboardType = KeyboardType.Text,
            imeAction = ImeAction.Next,
            modifier = Modifier.focusRequester(focus)
        )
        DialogField(
            value = address,
            onValueChange = { address = it; error = null },
            hint = "Website address",
            keyboardType = KeyboardType.Uri,
            imeAction = ImeAction.Done,
            onAction = save
        )
        error?.let { BasicText(it, style = awvText(13, FontWeight.Medium, Awv.Brand)) }
        DialogButtons("Add", onDismiss, save)
    }
}

@Composable
private fun DialogField(
    value: String,
    onValueChange: (String) -> Unit,
    hint: String,
    keyboardType: KeyboardType,
    imeAction: ImeAction,
    modifier: Modifier = Modifier,
    onAction: () -> Unit = {}
) {
    BasicTextField(
        value = value,
        onValueChange = onValueChange,
        modifier = modifier.fillMaxWidth(),
        singleLine = true,
        textStyle = awvText(15, FontWeight.Medium, Awv.Ink),
        cursorBrush = SolidColor(Awv.Coral),
        keyboardOptions = KeyboardOptions(keyboardType = keyboardType, imeAction = imeAction),
        keyboardActions = KeyboardActions(onDone = { onAction() }),
        decorationBox = { inner ->
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(50.dp)
                    .background(Awv.Surface, RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp),
                contentAlignment = Alignment.CenterStart
            ) {
                if (value.isEmpty()) {
                    BasicText(
                        hint,
                        style = awvText(15, FontWeight.Medium, Awv.Muted),
                        maxLines = 1, overflow = TextOverflow.Ellipsis
                    )
                }
                inner()
            }
        }
    )
}

@Composable
private fun DialogButtons(confirmLabel: String, onCancel: () -> Unit, onConfirm: () -> Unit) {
    Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        DialogButton("Cancel", filled = false, modifier = Modifier.weight(1f), onClick = onCancel)
        DialogButton(confirmLabel, filled = true, modifier = Modifier.weight(1f), onClick = onConfirm)
    }
}

@Composable
private fun DialogButton(label: String, filled: Boolean, modifier: Modifier, onClick: () -> Unit) {
    Box(
        modifier
            .height(48.dp)
            .pressable(label = label, onClick = onClick)
            .background(if (filled) Awv.Brand else Awv.Surface, RoundedCornerShape(24.dp)),
        contentAlignment = Alignment.Center
    ) {
        BasicText(label, style = awvText(15, FontWeight.ExtraBold, if (filled) Color.White else Awv.Ink), maxLines = 1)
    }
}
