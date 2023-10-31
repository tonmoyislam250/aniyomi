package tachiyomi.presentation.widget.util

import androidx.annotation.StringRes
import androidx.compose.runtime.Composable
import androidx.compose.ui.unit.DpSize
import androidx.glance.GlanceModifier
import androidx.glance.LocalContext
import androidx.glance.appwidget.cornerRadius
import tachiyomi.presentation.widget.R

fun GlanceModifier.appWidgetBackgroundRadius(): GlanceModifier {
    return this.cornerRadius(R.dimen.appwidget_background_radius)
}
fun GlanceModifier.appWidgetInnerRadius(): GlanceModifier {
    return this.cornerRadius(R.dimen.appwidget_inner_radius)
}

@Composable
fun stringResource(@StringRes id: Int): String {
    return LocalContext.current.getString(id)
}

/**
 * Calculates row-column count.
 *
 * Row
 * Numerator: Container height - container vertical padding
 * Denominator: Cover height + cover vertical padding
 *
 * Column
 * Numerator: Container width - container horizontal padding
 * Denominator: Cover width + cover horizontal padding
 *
 * @return pair of row and column count
 */
internal fun DpSize.calculateRowAndColumnCount(): Pair<Int, Int> {
    // Hack: Size provided by Glance manager is not reliable so take at least 1 row and 1 column
    // Set max to 10 children each direction because of Glance limitation
    val rowCount = (height.value / 95).toInt().coerceIn(1, 10)
    val columnCount = (width.value / 64).toInt().coerceIn(1, 10)
    return Pair(rowCount, columnCount)
}
