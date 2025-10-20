package com.example.questflow.presentation.components

import android.util.Log
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType

/**
 * Number input field that clears on focus for easy editing.
 *
 * UX Fix (P2-001): Auto-clears on focus so user can immediately type new value
 * without manually deleting the old one.
 *
 * @param value Current integer value
 * @param onValueChange Callback when value changes (validated)
 * @param modifier Modifier for the TextField
 * @param label Optional label text
 * @param placeholder Optional placeholder text
 * @param minValue Minimum allowed value (default 1)
 * @param maxValue Maximum allowed value (default Int.MAX_VALUE)
 * @param suffix Optional suffix composable (e.g., unit display)
 */
@Composable
fun NumberInputField(
    value: Int,
    onValueChange: (Int) -> Unit,
    modifier: Modifier = Modifier,
    label: String? = null,
    placeholder: String? = null,
    minValue: Int = 1,
    maxValue: Int = Int.MAX_VALUE,
    suffix: @Composable (() -> Unit)? = null
) {
    var textValue by remember(value) { mutableStateOf(value.toString()) }
    var isFirstFocus by remember { mutableStateOf(true) }
    val focusManager = LocalFocusManager.current

    OutlinedTextField(
        value = textValue,
        onValueChange = { text ->
            if (text.all { it.isDigit() } || text.isEmpty()) {
                textValue = text
                text.toIntOrNull()?.let { newValue ->
                    if (newValue in minValue..maxValue) {
                        onValueChange(newValue)
                        Log.d("QuestFlow_NumberInput", "Value changed: $value → $newValue (label: '$label')")
                    }
                }
            }
        },
        modifier = modifier.onFocusChanged { focusState ->
            if (focusState.isFocused && isFirstFocus) {
                // First time getting focus - clear the field
                isFirstFocus = false
                textValue = ""
                Log.d("QuestFlow_NumberInput", "Field focused & cleared (label: '$label', old value: $value)")
            } else if (!focusState.isFocused) {
                // Lost focus - restore value if empty
                if (textValue.isEmpty()) {
                    textValue = value.toString()
                    Log.d("QuestFlow_NumberInput", "Focus lost - restored value: $value (label: '$label')")
                } else {
                    val finalValue = textValue.toIntOrNull() ?: value
                    Log.d("QuestFlow_NumberInput", "Focus lost - final value: $finalValue (label: '$label')")
                }
                // Reset for next time
                isFirstFocus = true
            }
        },
        label = label?.let { { Text(it) } },
        placeholder = placeholder?.let { { Text(it) } },
        suffix = suffix,
        singleLine = true,
        keyboardOptions = KeyboardOptions(
            keyboardType = KeyboardType.Number,
            imeAction = ImeAction.Done
        ),
        keyboardActions = KeyboardActions(
            onDone = {
                // Clear focus when Done is pressed
                focusManager.clearFocus()
                Log.d("QuestFlow_NumberInput", "Done pressed - cleared focus (label: '$label')")
            }
        )
    )
}
