package com.example.questflow.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.animateScrollBy
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.input.pointer.PointerEventType
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.launch
import kotlin.math.abs
import kotlin.math.absoluteValue

/**
 * Horizontal Wheel Time Picker with Vertical Scroll
 * - HHMM field at top for quick full time entry
 * - Swipe up/down to rotate the horizontal wheel
 * - Center value is highlighted and clickable for direct input
 * - Side values fade out
 * - Infinite circular scroll
 */
@Composable
fun HorizontalWheelTimePicker(
    initialHour: Int,
    initialMinute: Int,
    onTimeChange: (hour: Int, minute: Int) -> Unit,
    modifier: Modifier = Modifier
) {
    var currentHour by remember { mutableStateOf(initialHour) }
    var currentMinute by remember { mutableStateOf(initialMinute) }
    var showHHMMInput by remember { mutableStateOf(false) }
    var showHourInput by remember { mutableStateOf(false) }
    var showMinuteInput by remember { mutableStateOf(false) }

    // Key to force wheel recomposition when direct input is confirmed
    var wheelKey by remember { mutableStateOf(0) }

    Column(
        modifier = modifier
            .fillMaxWidth()
            .padding(vertical = 16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(24.dp)
    ) {
        // HHMM Display (clickable for full time input)
        Surface(
            modifier = Modifier
                .clickable { showHHMMInput = true }
                .padding(8.dp),
            color = MaterialTheme.colorScheme.primaryContainer,
            shape = MaterialTheme.shapes.medium
        ) {
            Text(
                text = String.format("%02d:%02d", currentHour, currentMinute),
                style = MaterialTheme.typography.headlineLarge,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.padding(horizontal = 24.dp, vertical = 12.dp),
                maxLines = 1,
                softWrap = false
            )
        }

        // Hour Wheel
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Stunde",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalScrollWheel(
                maxValue = 23,
                initialValue = currentHour,
                onValueChange = { hour ->
                    android.util.Log.d("TimePicker", "Wheel hour changed to: $hour")
                    currentHour = hour
                    onTimeChange(currentHour, currentMinute)
                },
                onCenterClick = { showHourInput = true },
                forceUpdateKey = wheelKey  // Pass wheelKey as forceUpdateKey
            )
        }

        // Minute Wheel
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = "Minute",
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            HorizontalScrollWheel(
                maxValue = 59,
                initialValue = currentMinute,
                onValueChange = { minute ->
                    android.util.Log.d("TimePicker", "Wheel minute changed to: $minute")
                    currentMinute = minute
                    onTimeChange(currentHour, currentMinute)
                },
                onCenterClick = { showMinuteInput = true },
                forceUpdateKey = wheelKey  // Pass wheelKey as forceUpdateKey
            )
        }
    }

    // HHMM Direct Input Dialog
    if (showHHMMInput) {
        HHMMInputDialog(
            currentHour = currentHour,
            currentMinute = currentMinute,
            onDismiss = { showHHMMInput = false },
            onConfirm = { newHour, newMinute ->
                android.util.Log.d("TimePicker-Dialog", "╔═══════════════════════════════════════════════════════════")
                android.util.Log.d("TimePicker-Dialog", "║ HHMM INPUT CONFIRMED")
                android.util.Log.d("TimePicker-Dialog", "╠═══════════════════════════════════════════════════════════")
                android.util.Log.d("TimePicker-Dialog", "║ Input: ${String.format("%02d:%02d", newHour, newMinute)}")
                android.util.Log.d("TimePicker-Dialog", "║ Before: currentHour=$currentHour, currentMinute=$currentMinute")

                currentHour = newHour
                currentMinute = newMinute

                android.util.Log.d("TimePicker-Dialog", "║ After: currentHour=$currentHour, currentMinute=$currentMinute")
                android.util.Log.d("TimePicker-Dialog", "║ → Calling onTimeChange($currentHour, $currentMinute)")

                onTimeChange(currentHour, currentMinute)

                android.util.Log.d("TimePicker-Dialog", "║ → Incrementing wheelKey to trigger recomposition")
                android.util.Log.d("TimePicker-Dialog", "║   wheelKey before: $wheelKey")
                wheelKey++ // Trigger wheel recomposition to sync to new time
                android.util.Log.d("TimePicker-Dialog", "║   wheelKey after: $wheelKey")
                android.util.Log.d("TimePicker-Dialog", "║ → Closing dialog")
                android.util.Log.d("TimePicker-Dialog", "╚═══════════════════════════════════════════════════════════")

                showHHMMInput = false
            }
        )
    }

    // Hour Direct Input Dialog
    if (showHourInput) {
        SingleValueInputDialog(
            title = "Stunde eingeben",
            label = "Stunde (00-23)",
            maxValue = 23,
            currentValue = currentHour,
            onDismiss = { showHourInput = false },
            onConfirm = { newHour ->
                android.util.Log.d("TimePicker-Dialog", "╔═══════════════════════════════════════════════════════════")
                android.util.Log.d("TimePicker-Dialog", "║ HOUR INPUT CONFIRMED")
                android.util.Log.d("TimePicker-Dialog", "╠═══════════════════════════════════════════════════════════")
                android.util.Log.d("TimePicker-Dialog", "║ Input: $newHour")
                android.util.Log.d("TimePicker-Dialog", "║ Before: currentHour=$currentHour")

                currentHour = newHour

                android.util.Log.d("TimePicker-Dialog", "║ After: currentHour=$currentHour")
                android.util.Log.d("TimePicker-Dialog", "║ → Calling onTimeChange($currentHour, $currentMinute)")

                onTimeChange(currentHour, currentMinute)

                android.util.Log.d("TimePicker-Dialog", "║ → Incrementing wheelKey to trigger recomposition")
                wheelKey++
                android.util.Log.d("TimePicker-Dialog", "║ → Closing dialog")
                android.util.Log.d("TimePicker-Dialog", "╚═══════════════════════════════════════════════════════════")

                showHourInput = false
            }
        )
    }

    // Minute Direct Input Dialog
    if (showMinuteInput) {
        SingleValueInputDialog(
            title = "Minute eingeben",
            label = "Minute (00-59)",
            maxValue = 59,
            currentValue = currentMinute,
            onDismiss = { showMinuteInput = false },
            onConfirm = { newMinute ->
                android.util.Log.d("TimePicker-Dialog", "╔═══════════════════════════════════════════════════════════")
                android.util.Log.d("TimePicker-Dialog", "║ MINUTE INPUT CONFIRMED")
                android.util.Log.d("TimePicker-Dialog", "╠═══════════════════════════════════════════════════════════")
                android.util.Log.d("TimePicker-Dialog", "║ Input: $newMinute")
                android.util.Log.d("TimePicker-Dialog", "║ Before: currentMinute=$currentMinute")

                currentMinute = newMinute

                android.util.Log.d("TimePicker-Dialog", "║ After: currentMinute=$currentMinute")
                android.util.Log.d("TimePicker-Dialog", "║ → Calling onTimeChange($currentHour, $currentMinute)")

                onTimeChange(currentHour, currentMinute)

                android.util.Log.d("TimePicker-Dialog", "║ → Incrementing wheelKey to trigger recomposition")
                wheelKey++
                android.util.Log.d("TimePicker-Dialog", "║ → Closing dialog")
                android.util.Log.d("TimePicker-Dialog", "╚═══════════════════════════════════════════════════════════")

                showMinuteInput = false
            }
        )
    }
}

/**
 * HHMM Input Dialog
 * Enter full time as 3-4 digits:
 * - 3 digits: HMM (e.g., 612 for 06:12)
 * - 4 digits: HHMM (e.g., 1430 for 14:30)
 */
@Composable
fun HHMMInputDialog(
    currentHour: Int,
    currentMinute: Int,
    onDismiss: () -> Unit,
    onConfirm: (hour: Int, minute: Int) -> Unit
) {
    var timeInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var lastInvalidInput by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    // Helper function to parse and validate time input
    fun parseTimeInput(input: String): Pair<Int, Int>? {
        return when (input.length) {
            3 -> {
                // HMM format: 612 -> 06:12
                val hour = input.substring(0, 1).toIntOrNull()
                val minute = input.substring(1, 3).toIntOrNull()
                if (hour != null && minute != null && hour in 0..9 && minute in 0..59) {
                    Pair(hour, minute)
                } else null
            }
            4 -> {
                // HHMM format: 0612 -> 06:12
                val hour = input.substring(0, 2).toIntOrNull()
                val minute = input.substring(2, 4).toIntOrNull()
                if (hour != null && minute != null && hour in 0..23 && minute in 0..59) {
                    Pair(hour, minute)
                } else null
            }
            else -> null
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Zeit eingeben") },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aktuell: ${String.format("%02d:%02d", currentHour, currentMinute)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                Text(
                    text = "Format: 3-4 Ziffern (z.B. 612 oder 0612 für 06:12)",
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )

                OutlinedTextField(
                    value = timeInput,
                    onValueChange = { input ->
                        if (input.length <= 4 && input.all { it.isDigit() }) {
                            timeInput = input
                            errorMessage = null
                            lastInvalidInput = null
                        }
                    },
                    label = { Text("Zeit") },
                    placeholder = { Text("612") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val parsed = parseTimeInput(timeInput)
                            if (parsed != null) {
                                onConfirm(parsed.first, parsed.second)
                            } else if (timeInput.isNotEmpty()) {
                                // Invalid input - save it, clear field, show error
                                lastInvalidInput = timeInput
                                timeInput = ""
                                errorMessage = "Ungültige Zeit: $lastInvalidInput"
                            }
                        }
                    ),
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    when {
                        // Empty input: Keep old time and close dialog
                        timeInput.isEmpty() -> {
                            onConfirm(currentHour, currentMinute)
                        }
                        // Try to parse 3 or 4 digit input
                        else -> {
                            val parsed = parseTimeInput(timeInput)
                            if (parsed != null) {
                                onConfirm(parsed.first, parsed.second)
                            } else {
                                // Invalid input - save it, clear field, show error
                                lastInvalidInput = timeInput
                                timeInput = ""
                                errorMessage = "Ungültige Zeit: $lastInvalidInput"
                            }
                        }
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Single Value Input Dialog
 * Enter a single value (hour or minute) as 2 digits
 * Auto-focuses on input field with keyboard
 */
@Composable
fun SingleValueInputDialog(
    title: String,
    label: String,
    maxValue: Int,
    currentValue: Int,
    onDismiss: () -> Unit,
    onConfirm: (Int) -> Unit
) {
    var valueInput by remember { mutableStateOf("") }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val focusRequester = remember { FocusRequester() }

    // Auto-focus when dialog opens
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Text(
                    text = "Aktuell: ${String.format("%02d", currentValue)}",
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Bold
                )

                OutlinedTextField(
                    value = valueInput,
                    onValueChange = { input ->
                        if (input.length <= 2 && input.all { it.isDigit() }) {
                            valueInput = input
                            errorMessage = null
                        }
                    },
                    label = { Text(label) },
                    placeholder = { Text("00") },
                    keyboardOptions = KeyboardOptions(
                        keyboardType = KeyboardType.Number,
                        imeAction = ImeAction.Done
                    ),
                    keyboardActions = KeyboardActions(
                        onDone = {
                            val value = valueInput.toIntOrNull()
                            if (value != null && value in 0..maxValue) {
                                onConfirm(value)
                            } else {
                                errorMessage = "Ungültiger Wert (0-$maxValue)"
                            }
                        }
                    ),
                    isError = errorMessage != null,
                    singleLine = true,
                    modifier = Modifier
                        .fillMaxWidth()
                        .focusRequester(focusRequester)
                )

                if (errorMessage != null) {
                    Text(
                        text = errorMessage!!,
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.error
                    )
                }
            }
        },
        confirmButton = {
            TextButton(
                onClick = {
                    val value = valueInput.toIntOrNull()
                    if (value != null && value in 0..maxValue) {
                        onConfirm(value)
                    } else {
                        errorMessage = "Ungültiger Wert (0-$maxValue)"
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Abbrechen")
            }
        }
    )
}

/**
 * Horizontal Scroll Wheel Component with Infinite Circular Scroll
 * Displays values 0..maxValue horizontally, controlled by vertical drag gestures
 * Values wrap around: after maxValue comes 0 again
 * Center value is clickable for direct input
 */
@Composable
fun HorizontalScrollWheel(
    maxValue: Int,
    initialValue: Int,
    onValueChange: (Int) -> Unit,
    onCenterClick: () -> Unit = {},
    modifier: Modifier = Modifier,
    forceUpdateKey: Int = 0  // NEW: Key to force recomposition
) {
    val coroutineScope = rememberCoroutineScope()

    // Create a very long list by repeating the values many times for circular effect
    val repeatCount = 1000
    val totalItems = (maxValue + 1) * repeatCount
    val middleStart = totalItems / 2 - (totalItems / 2) % (maxValue + 1)

    val listState = rememberLazyListState(
        initialFirstVisibleItemIndex = middleStart + initialValue
    )

    // Track current selected value
    var currentValue by remember { mutableStateOf(initialValue) }

    // Track if user actually dragged (to distinguish from recomposition)
    var userDidDrag by remember { mutableStateOf(false) }

    // Skip snap after direct input to prevent +1 bug
    var skipNextSnap by remember { mutableStateOf(true) }  // CRITICAL: Start with TRUE to skip initial snap!

    // Track last forceUpdateKey to detect external changes
    var lastForceUpdateKey by remember { mutableStateOf(forceUpdateKey) }

    // Track if this is first composition
    var isInitialComposition by remember { mutableStateOf(true) }

    // Sync to initialValue changes from parent (direct input)
    LaunchedEffect(initialValue, forceUpdateKey) {
        android.util.Log.d("TimePicker-Wheel", "╔═══════════════════════════════════════════════════════════")
        android.util.Log.d("TimePicker-Wheel", "║ DIRECT INPUT SYNC START (maxValue=$maxValue)")
        android.util.Log.d("TimePicker-Wheel", "╠═══════════════════════════════════════════════════════════")
        android.util.Log.d("TimePicker-Wheel", "║ Current state: currentValue=$currentValue")
        android.util.Log.d("TimePicker-Wheel", "║ New state: initialValue=$initialValue")
        android.util.Log.d("TimePicker-Wheel", "║ forceUpdateKey: $forceUpdateKey (last: $lastForceUpdateKey)")
        android.util.Log.d("TimePicker-Wheel", "║ isInitialComposition: $isInitialComposition")

        val forceUpdateKeyChanged = forceUpdateKey != lastForceUpdateKey

        // Handle initial composition - center the wheel without triggering snap
        if (isInitialComposition) {
            android.util.Log.d("TimePicker-Wheel", "║ → INITIAL COMPOSITION - Centering wheel")
            isInitialComposition = false

            // Wait for layout to settle, then manually center
            coroutineScope.launch {
                kotlinx.coroutines.delay(100)

                val layoutInfo = listState.layoutInfo
                val centerX = layoutInfo.viewportEndOffset / 2f

                // Find the target item
                val targetItem = layoutInfo.visibleItemsInfo.find {
                    it.index % (maxValue + 1) == initialValue
                }

                targetItem?.let { item ->
                    val itemCenter = item.offset + item.size / 2f
                    val offsetFromCenter = itemCenter - centerX

                    android.util.Log.d("TimePicker-Wheel", "║ → Initial centering: offsetFromCenter=$offsetFromCenter px")

                    if (offsetFromCenter.absoluteValue > 2f) {
                        listState.animateScrollToItem(
                            index = item.index,
                            scrollOffset = -layoutInfo.viewportEndOffset / 2 + item.size / 2
                        )
                    }

                    // After centering, allow future snaps
                    skipNextSnap = false
                    android.util.Log.d("TimePicker-Wheel", "║ → Initial centering complete, skipNextSnap=false")
                }
            }

            android.util.Log.d("TimePicker-Wheel", "╚═══════════════════════════════════════════════════════════")
            return@LaunchedEffect
        }

        if (initialValue != currentValue || forceUpdateKeyChanged) {
            android.util.Log.d("TimePicker-Wheel", "║ → SYNC NEEDED (${if (initialValue != currentValue) "value changed" else "forceUpdate"})")

            currentValue = initialValue
            skipNextSnap = true // CRITICAL: Skip snap to prevent +1 bug
            userDidDrag = false
            lastForceUpdateKey = forceUpdateKey

            val targetIndex = middleStart + initialValue
            android.util.Log.d("TimePicker-Wheel", "║ → Scrolling to index: $targetIndex")
            android.util.Log.d("TimePicker-Wheel", "║   (middleStart=$middleStart + initialValue=$initialValue)")
            android.util.Log.d("TimePicker-Wheel", "║ → skipNextSnap set to TRUE")

            listState.scrollToItem(targetIndex)

            android.util.Log.d("TimePicker-Wheel", "║ → After scrollToItem:")
            android.util.Log.d("TimePicker-Wheel", "║   firstVisibleItemIndex=${listState.firstVisibleItemIndex}")
            android.util.Log.d("TimePicker-Wheel", "║   layoutInfo.visibleItemsInfo.size=${listState.layoutInfo.visibleItemsInfo.size}")

            // CRITICAL FIX: Manually center the item since snap is skipped
            android.util.Log.d("TimePicker-Wheel", "║ → Manually centering item (snap will be skipped)")

            // Wait for layout to settle, then manually center
            coroutineScope.launch {
                // Small delay for layout to complete
                kotlinx.coroutines.delay(50)

                val layoutInfo = listState.layoutInfo
                val centerX = layoutInfo.viewportEndOffset / 2f

                android.util.Log.d("TimePicker-Wheel", "║ → Checking position after scrollToItem:")
                android.util.Log.d("TimePicker-Wheel", "║   centerX=$centerX")

                // Find the target item
                val targetItem = layoutInfo.visibleItemsInfo.find {
                    it.index % (maxValue + 1) == initialValue
                }

                targetItem?.let { item ->
                    val itemCenter = item.offset + item.size / 2f
                    val offsetFromCenter = itemCenter - centerX

                    android.util.Log.d("TimePicker-Wheel", "║   Target item [${item.index}] value=$initialValue")
                    android.util.Log.d("TimePicker-Wheel", "║   itemCenter=$itemCenter, offsetFromCenter=$offsetFromCenter")

                    if (offsetFromCenter.absoluteValue > 2f) {
                        android.util.Log.d("TimePicker-Wheel", "║   → Correcting offset by $offsetFromCenter px")
                        listState.animateScrollToItem(
                            index = item.index,
                            scrollOffset = -layoutInfo.viewportEndOffset / 2 + item.size / 2
                        )
                        android.util.Log.d("TimePicker-Wheel", "║   → Manual centering complete")
                    } else {
                        android.util.Log.d("TimePicker-Wheel", "║   → Already centered")
                    }
                }
            }
        } else {
            android.util.Log.d("TimePicker-Wheel", "║ → VALUES SAME - No sync needed")
        }

        android.util.Log.d("TimePicker-Wheel", "╚═══════════════════════════════════════════════════════════")
    }

    // ALWAYS snap after scroll stops (not just user drag)
    LaunchedEffect(listState.isScrollInProgress) {
        if (!listState.isScrollInProgress) {
            android.util.Log.d("TimePicker-Snap", "╔═══════════════════════════════════════════════════════════")
            android.util.Log.d("TimePicker-Snap", "║ SNAP TRIGGERED (maxValue=$maxValue)")
            android.util.Log.d("TimePicker-Snap", "╠═══════════════════════════════════════════════════════════")
            android.util.Log.d("TimePicker-Snap", "║ State: currentValue=$currentValue, skipNextSnap=$skipNextSnap, userDidDrag=$userDidDrag")

            if (skipNextSnap) {
                android.util.Log.d("TimePicker-Snap", "║ → SKIPPING snap (after direct input)")
                android.util.Log.d("TimePicker-Snap", "║ → Setting skipNextSnap=false")
                skipNextSnap = false
                android.util.Log.d("TimePicker-Snap", "╚═══════════════════════════════════════════════════════════")
                return@LaunchedEffect
            }

            android.util.Log.d("TimePicker-Snap", "║ → EXECUTING snap logic...")

            // Find nearest item to center
            val layoutInfo = listState.layoutInfo
            val centerX = layoutInfo.viewportEndOffset / 2f

            android.util.Log.d("TimePicker-Snap", "║ Viewport info:")
            android.util.Log.d("TimePicker-Snap", "║   start=${layoutInfo.viewportStartOffset}")
            android.util.Log.d("TimePicker-Snap", "║   end=${layoutInfo.viewportEndOffset}")
            android.util.Log.d("TimePicker-Snap", "║   centerX=$centerX")
            android.util.Log.d("TimePicker-Snap", "║ Visible items: ${layoutInfo.visibleItemsInfo.size}")

            layoutInfo.visibleItemsInfo.forEach { item ->
                val itemCenter = item.offset + item.size / 2f
                val distance = (itemCenter - centerX).absoluteValue
                val value = item.index % (maxValue + 1)
                android.util.Log.d("TimePicker-Snap", "║   [${item.index}] value=$value, offset=${item.offset}, center=$itemCenter, dist=$distance")
            }

            val centerItem = layoutInfo.visibleItemsInfo.minByOrNull { item ->
                val itemCenter = item.offset + item.size / 2f
                (itemCenter - centerX).absoluteValue
            }

            centerItem?.let { item ->
                val actualValue = item.index % (maxValue + 1)
                val itemCenter = item.offset + item.size / 2f
                val distanceFromCenter = (itemCenter - centerX).absoluteValue

                android.util.Log.d("TimePicker-Snap", "║ ✓ Selected center item:")
                android.util.Log.d("TimePicker-Snap", "║   index=${item.index}")
                android.util.Log.d("TimePicker-Snap", "║   actualValue=$actualValue (index % ${maxValue + 1})")
                android.util.Log.d("TimePicker-Snap", "║   distance from center=$distanceFromCenter px")

                if (actualValue != currentValue) {
                    android.util.Log.d("TimePicker-Snap", "║ → VALUE CHANGE: $currentValue → $actualValue")
                    currentValue = actualValue
                    onValueChange(actualValue)
                } else {
                    android.util.Log.d("TimePicker-Snap", "║ → No value change (already $actualValue)")
                }

                // INSTANT snap to center (no animation)
                val viewportCenter = layoutInfo.viewportEndOffset / 2
                val scrollOffset = itemCenter - viewportCenter

                android.util.Log.d("TimePicker-Snap", "║ Centering:")
                android.util.Log.d("TimePicker-Snap", "║   scrollOffset=$scrollOffset px")

                if (scrollOffset.absoluteValue > 1f) {
                    android.util.Log.d("TimePicker-Snap", "║ → Correcting position with animateScrollToItem")
                    // Use animateScrollToItem for precise centering
                    coroutineScope.launch {
                        listState.animateScrollToItem(
                            index = item.index,
                            scrollOffset = -layoutInfo.viewportEndOffset / 2 + item.size / 2
                        )
                    }
                } else {
                    android.util.Log.d("TimePicker-Snap", "║ → Already centered (offset < 1px)")
                }
            }

            userDidDrag = false
            android.util.Log.d("TimePicker-Snap", "╚═══════════════════════════════════════════════════════════")
        }
    }

    Box(
        modifier = modifier
            .fillMaxWidth()
            .height(100.dp)
            .pointerInput(Unit) {
                awaitEachGesture {
                    // Wait for first pointer down
                    val down = awaitFirstDown(requireUnconsumed = false)
                    val downTime = System.currentTimeMillis()
                    val downPosition = down.position
                    var isDragging = false
                    var totalDrag = 0f

                    android.util.Log.d("TimePicker-Gesture", "╔═══════════════════════════════════════════════════════════")
                    android.util.Log.d("TimePicker-Gesture", "║ POINTER DOWN at y=${downPosition.y}")
                    android.util.Log.d("TimePicker-Gesture", "║ currentValue at down: $currentValue")

                    // Track drag or release
                    do {
                        val event = awaitPointerEvent()
                        val change = event.changes.firstOrNull() ?: break

                        if (event.type == PointerEventType.Move) {
                            val dragAmount = change.position.y - change.previousPosition.y
                            totalDrag += dragAmount

                            // Start dragging if movement exceeds threshold (lower threshold)
                            if (!isDragging && abs(totalDrag) > 5f) {
                                isDragging = true
                                userDidDrag = true
                                android.util.Log.d("TimePicker-Gesture", "║ → STARTED DRAGGING (totalDrag=$totalDrag)")
                            }

                            if (isDragging) {
                                change.consume()
                                // Invert drag: swipe up = scroll left, swipe down = scroll right
                                coroutineScope.launch {
                                    listState.animateScrollBy(-dragAmount * 1.5f)
                                }
                            }
                        } else if (event.type == PointerEventType.Release) {
                            val duration = System.currentTimeMillis() - downTime
                            val distance = (change.position - downPosition).getDistance()

                            android.util.Log.d("TimePicker-Gesture", "║ POINTER UP")
                            android.util.Log.d("TimePicker-Gesture", "║   duration=$duration ms")
                            android.util.Log.d("TimePicker-Gesture", "║   distance=$distance px")
                            android.util.Log.d("TimePicker-Gesture", "║   totalDrag=$totalDrag px")
                            android.util.Log.d("TimePicker-Gesture", "║   isDragging=$isDragging")
                            android.util.Log.d("TimePicker-Gesture", "║   currentValue at release: $currentValue")

                            // It's a tap if: short duration, small distance, and not marked as dragging
                            if (!isDragging && duration < 300 && distance < 20f) {
                                android.util.Log.d("TimePicker-Gesture", "║ ✓ DETECTED TAP")
                                android.util.Log.d("TimePicker-Gesture", "║   → Calling onCenterClick() with currentValue=$currentValue")
                                android.util.Log.d("TimePicker-Gesture", "╚═══════════════════════════════════════════════════════════")
                                // Use currentValue directly instead of calculating from layout
                                // This is the ALREADY SNAPPED value, so it's accurate
                                onCenterClick()
                            } else {
                                android.util.Log.d("TimePicker-Gesture", "║ ✓ DETECTED DRAG/SWIPE")
                                android.util.Log.d("TimePicker-Gesture", "║   → Will trigger snap when scroll stops")
                                android.util.Log.d("TimePicker-Gesture", "╚═══════════════════════════════════════════════════════════")
                            }
                            break
                        }
                    } while (event.changes.any { it.pressed })
                }
            },
        contentAlignment = Alignment.Center
    ) {
        // Center indicator box
        Box(
            modifier = Modifier
                .width(70.dp)
                .height(70.dp)
        ) {
            Surface(
                modifier = Modifier.fillMaxSize(),
                color = MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.2f),
                shape = MaterialTheme.shapes.medium
            ) {}
        }

        // Horizontal scrolling numbers
        LazyRow(
            state = listState,
            modifier = Modifier.fillMaxSize(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            items(totalItems) { index ->
                val value = index % (maxValue + 1)

                // Calculate distance from center for fade/scale effect
                val layoutInfo = listState.layoutInfo
                val centerX = layoutInfo.viewportEndOffset / 2f

                val itemInfo = layoutInfo.visibleItemsInfo.find { it.index == index }
                val distance = if (itemInfo != null) {
                    val itemCenter = itemInfo.offset + itemInfo.size / 2f
                    ((itemCenter - centerX).absoluteValue / (centerX * 0.5f)).coerceIn(0f, 1f)
                } else {
                    1f
                }

                val alpha = (1f - distance * 0.8f).coerceAtLeast(0.2f)
                val scale = (1f - distance * 0.4f).coerceAtLeast(0.6f)

                val isCenter = distance < 0.15f

                Box(
                    modifier = Modifier
                        .width(70.dp)
                        .height(70.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = String.format("%02d", value),
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = if (isCenter) FontWeight.Bold else FontWeight.Normal,
                        fontSize = (36.sp.value * scale).sp,
                        modifier = Modifier
                            .alpha(alpha),
                        color = if (isCenter)
                            MaterialTheme.colorScheme.primary
                        else
                            MaterialTheme.colorScheme.onSurface
                    )
                }
            }
        }
    }
}
