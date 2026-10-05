package de.easynfp.app.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.snapping.rememberSnapFlingBehavior
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.derivedStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.runtime.snapshotFlow
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import de.easynfp.app.R
import de.easynfp.app.text.Formats
import de.easynfp.rules.model.Temperature
import kotlin.math.abs
import kotlinx.coroutines.flow.distinctUntilChanged

private const val VISIBLE_ITEMS = 5
private val MIN = Temperature.MIN.hundredths
private val MAX = Temperature.MAX.hundredths

/**
 * F-03: number wheel for temperatures in steps of 0.01 °C between 34.00 and 42.00 (F-01),
 * plus stepper buttons as an alternative. No keyboard needed.
 */
@Composable
fun TemperatureWheel(
    value: Temperature,
    onValueChange: (Temperature) -> Unit,
    modifier: Modifier = Modifier,
    itemHeight: Dp = 48.dp,
    large: Boolean = false,
) {
    val clamped = value.hundredths.coerceIn(MIN, MAX)
    val listState = rememberLazyListState(initialFirstVisibleItemIndex = clamped - MIN)
    val currentOnChange by rememberUpdatedState(onValueChange)

    val centeredIndex by remember {
        derivedStateOf {
            val info = listState.layoutInfo
            val center = (info.viewportStartOffset + info.viewportEndOffset) / 2
            info.visibleItemsInfo.minByOrNull { abs(it.offset + it.size / 2 - center) }?.index
                ?: listState.firstVisibleItemIndex
        }
    }

    // Wheel -> value, only for user gestures (not while the wheel follows the stepper buttons).
    var programmaticScroll by remember { mutableStateOf(false) }
    val currentValue by rememberUpdatedState(clamped)
    LaunchedEffect(listState) {
        snapshotFlow { centeredIndex }
            .distinctUntilChanged()
            .collect { index ->
                if (!programmaticScroll && index + MIN != currentValue) currentOnChange(Temperature(index + MIN))
            }
    }
    // Value -> wheel
    LaunchedEffect(clamped) {
        if (!listState.isScrollInProgress && centeredIndex != clamped - MIN) {
            programmaticScroll = true
            try {
                listState.animateScrollToItem(clamped - MIN)
            } finally {
                programmaticScroll = false
            }
        }
    }

    val description = stringResource(R.string.cd_temperature_wheel, Formats.temperature(Temperature(clamped)))
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(itemHeight * VISIBLE_ITEMS)
                .semantics { contentDescription = description },
            contentAlignment = Alignment.Center,
        ) {
            // Selection highlight behind the centered item.
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(itemHeight)
                    .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), RoundedCornerShape(12.dp)),
            )
            LazyColumn(
                state = listState,
                flingBehavior = rememberSnapFlingBehavior(listState),
                contentPadding = PaddingValues(vertical = itemHeight * (VISIBLE_ITEMS / 2)),
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth(),
            ) {
                items(count = MAX - MIN + 1, key = { it }) { index ->
                    val distance = abs(index - centeredIndex)
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(itemHeight)
                            .alpha(if (distance == 0) 1f else (1f - distance * 0.3f).coerceAtLeast(0.2f))
                            .clickable { onValueChange(Temperature(index + MIN)) },
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(
                            text = Formats.temperatureValue(Temperature(index + MIN)),
                            fontSize = when {
                                distance == 0 && large -> 44.sp
                                distance == 0 -> 30.sp
                                large -> 26.sp
                                else -> 20.sp
                            },
                            fontWeight = if (distance == 0) FontWeight.SemiBold else FontWeight.Normal,
                            color = MaterialTheme.colorScheme.onSurface,
                        )
                    }
                }
            }
        }
        StepperRow(clamped = clamped, onValueChange = onValueChange)
    }
}

@Composable
private fun StepperRow(clamped: Int, onValueChange: (Temperature) -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(top = 8.dp),
        horizontalArrangement = Arrangement.spacedBy(8.dp, Alignment.CenterHorizontally),
    ) {
        listOf(-10, -1, 1, 10).forEach { delta ->
            val label = (if (delta < 0) "−" else "+") + Formats.temperatureValue(Temperature(abs(delta)))
            OutlinedButton(
                onClick = { onValueChange(Temperature((clamped + delta).coerceIn(MIN, MAX))) },
                contentPadding = PaddingValues(horizontal = 10.dp),
            ) {
                Text(label)
            }
        }
    }
}
