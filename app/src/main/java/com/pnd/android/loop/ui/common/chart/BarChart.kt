package com.pnd.android.loop.ui.common.chart

import android.graphics.Typeface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.rememberColumnCartesianLayer
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.legend.legendItem
import com.patrykandpatrick.vico.compose.legend.verticalLegend
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.compose.style.currentChartStyle
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.formatter.PercentageFormatAxisValueFormatter
import com.patrykandpatrick.vico.core.chart.layer.ColumnCartesianLayer
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.chart.values.AxisValueOverrider
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurface

private const val NUMBER_OF_START_AXIS_INDICATORS = 5

@Composable
fun BarChart(
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer,
    barColors: List<Color>,
    maxY: Float = 100f,
    minY: Float = 0f,
    titleStartAxis: String = "",
    titleBottomAxis: String = "",
    legends: List<String> = emptyList(),
    bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
) {
    ProvideChartStyle(rememberChartStyle(barColors)) {
        val defaultColumns = currentChartStyle.columnLayer.columns
        CartesianChartHost(
            modifier = modifier,
            modelProducer = modelProducer,
            chart = rememberCartesianChart(
                rememberColumnCartesianLayer(
                    remember(defaultColumns) {
                        defaultColumns.map {
                            LineComponent(
                                color = it.color,
                                thicknessDp = 16f,
                                shape = it.shape
                            )
                        }
                    },
                    axisValueOverrider = AxisValueOverrider.fixed(
                        maxY = maxY,
                        minY = minY,
                    ),
                    mergeMode = { ColumnCartesianLayer.MergeMode.Grouped },
                ),
                startAxis = rememberChartStartAxis(title = titleStartAxis),
                bottomAxis = rememberBottomAxis(
                    valueFormatter = bottomAxisValueFormatter,
                    itemPlacer = AxisItemPlacer.Horizontal.default(spacing = 1)
                ),
                legend = rememberLegend(
                    barColors = barColors,
                    texts = legends
                )
            ),

            marker = rememberMarker(),
            runInitialAnimation = false,
            horizontalLayout = HorizontalLayout.FullWidth(
                scalableStartPaddingDp = 16f,
                scalableEndPaddingDp = 16f,
            ),
        )
    }
}

@Composable
private fun rememberChartStartAxis(title: String) = rememberStartAxis(
    title = title,
    titleComponent = rememberTextComponent(
        color = AppColor.onSurface,
        background = rememberShapeComponent(
            Shapes.pillShape,
            AppColor.onSurface.copy(alpha = 0.1f)
        ),
        padding = dimensionsOf(horizontal = 8.dp, vertical = 2.dp),
        margins = dimensionsOf(end = 4.dp),
        typeface = Typeface.MONOSPACE,
    ),
    valueFormatter = PercentageFormatAxisValueFormatter(),
    itemPlacer = AxisItemPlacer.Vertical.default({ NUMBER_OF_START_AXIS_INDICATORS })
)


@Composable
private fun rememberLegend(
    barColors: List<Color>,
    texts: List<String>
) =
    verticalLegend(
        items = texts.mapIndexed { index, text ->
            legendItem(
                icon = rememberShapeComponent(Shapes.pillShape, barColors[index]),
                label =
                rememberTextComponent(
                    color = currentChartStyle.axis.axisLabelColor,
                    textSize = 14.sp,
                    typeface = Typeface.MONOSPACE,
                ),
                labelText = text,
            )
        },
        iconSize = 12.dp,
        iconPadding = 10.dp,
        spacing = 4.dp,
        padding = dimensionsOf(top = 8.dp),
    )