package com.lovelyy.app.ui.components

import androidx.compose.foundation.layout.padding
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDefaults
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.SelectableDates
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.lovelyy.app.R
import com.lovelyy.app.ui.theme.Brown
import com.lovelyy.app.ui.theme.BrownSoft
import com.lovelyy.app.ui.theme.Cream
import com.lovelyy.app.ui.theme.Peach
import com.lovelyy.app.ui.theme.Quicksand
import java.time.Instant
import java.time.LocalDate
import java.time.ZoneOffset

/** Material date picker in Lovelyy colors. Works in local dates (the picker itself thinks in UTC days). */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun LovelyyDatePicker(
    initial: LocalDate?,
    title: String,
    allowFuture: Boolean,
    onDismiss: () -> Unit,
    onPick: (LocalDate) -> Unit,
    /** Only days after today (time-capsule letters). */
    onlyFuture: Boolean = false,
) {
    val todayUtc = LocalDate.now().atStartOfDay(ZoneOffset.UTC).toInstant().toEpochMilli()
    val state = rememberDatePickerState(
        initialSelectedDateMillis = initial?.atStartOfDay(ZoneOffset.UTC)?.toInstant()?.toEpochMilli(),
        selectableDates = object : SelectableDates {
            override fun isSelectableDate(utcTimeMillis: Long) =
                if (onlyFuture) utcTimeMillis > todayUtc else allowFuture || utcTimeMillis <= todayUtc
            override fun isSelectableYear(year: Int) =
                if (onlyFuture) year >= LocalDate.now().year else allowFuture || year <= LocalDate.now().year
        },
    )
    val colors = DatePickerDefaults.colors(
        containerColor = Cream,
        selectedDayContainerColor = Peach,
        selectedDayContentColor = Brown,
        todayDateBorderColor = Peach,
        todayContentColor = Brown,
        selectedYearContainerColor = Peach,
        selectedYearContentColor = Brown,
    )
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                enabled = state.selectedDateMillis != null,
                onClick = { state.selectedDateMillis?.let { onPick(Instant.ofEpochMilli(it).atZone(ZoneOffset.UTC).toLocalDate()) } },
            ) { Text(stringResource(R.string.action_save), fontFamily = Quicksand, fontWeight = FontWeight.Bold, color = Brown) }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) { Text(stringResource(R.string.action_cancel), fontFamily = Quicksand, color = BrownSoft) }
        },
        colors = colors,
    ) {
        DatePicker(
            state = state,
            colors = colors,
            title = {
                Text(
                    title, fontFamily = Quicksand, fontWeight = FontWeight.Bold, fontSize = 16.sp, color = Brown,
                    modifier = Modifier.padding(start = 24.dp, top = 20.dp, end = 12.dp),
                )
            },
        )
    }
}
