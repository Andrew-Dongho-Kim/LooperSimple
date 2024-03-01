package com.pnd.android.loop.ui.common.chart

import android.graphics.PorterDuff
import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.axis.axisLabelComponent
import com.patrykandpatrick.vico.compose.axis.horizontal.rememberBottomAxis
import com.patrykandpatrick.vico.compose.axis.vertical.rememberStartAxis
import com.patrykandpatrick.vico.compose.chart.CartesianChartHost
import com.patrykandpatrick.vico.compose.chart.layer.lineSpec
import com.patrykandpatrick.vico.compose.chart.layer.rememberLineCartesianLayer
import com.patrykandpatrick.vico.compose.chart.layout.fullWidth
import com.patrykandpatrick.vico.compose.chart.rememberCartesianChart
import com.patrykandpatrick.vico.compose.component.overlayingComponent
import com.patrykandpatrick.vico.compose.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.component.shape.dashedShape
import com.patrykandpatrick.vico.compose.component.shape.shader.color
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.component.shape.shader.fromComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.verticalGradient
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.compose.style.ProvideChartStyle
import com.patrykandpatrick.vico.core.DefaultAlpha
import com.patrykandpatrick.vico.core.DefaultColors
import com.patrykandpatrick.vico.core.axis.AxisItemPlacer
import com.patrykandpatrick.vico.core.axis.AxisPosition
import com.patrykandpatrick.vico.core.axis.formatter.AxisValueFormatter
import com.patrykandpatrick.vico.core.axis.formatter.PercentageFormatAxisValueFormatter
import com.patrykandpatrick.vico.core.chart.dimensions.HorizontalDimensions
import com.patrykandpatrick.vico.core.chart.insets.Insets
import com.patrykandpatrick.vico.core.chart.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.chart.layout.HorizontalLayout
import com.patrykandpatrick.vico.core.chart.values.AxisValueOverrider
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.DashedShape
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.cornered.Corner
import com.patrykandpatrick.vico.core.component.shape.cornered.MarkerCorneredShape
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.component.shape.shader.TopBottomShader
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.extension.copyColor
import com.patrykandpatrick.vico.core.marker.Marker
import com.patrykandpatrick.vico.core.model.CartesianChartModelProducer
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onBackground
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface

@Composable
fun AdvancedLineChart(
    modifier: Modifier = Modifier,
    modelProducer: CartesianChartModelProducer,
    chartColors: List<Color>,
    maxY: Float = 100f,
    minY: Float = 0f,
    titleStartAxis: String = "",
    titleBottomAxis: String = "",
    bottomAxisValueFormatter: AxisValueFormatter<AxisPosition.Horizontal.Bottom>,
) {
    ProvideChartStyle(
        chartStyle = rememberChartStyle(chartColors)
    ) {
        CartesianChartHost(
            modifier = modifier,
            modelProducer = modelProducer,
            chart = rememberCartesianChart(
                rememberChartLineLayer(
                    chartColors = chartColors,
                    maxY = maxY,
                    minY = minY,
                ),
                startAxis = rememberChartStartAxis(titleStartAxis),
                bottomAxis = rememberBottomAxis(
                    title = titleBottomAxis,
                    guideline = null,
                    itemPlacer = remember {
                        AxisItemPlacer.Horizontal.default(
                            spacing = 1,
                            addExtremeLabelPadding = true
                        )
                    },
                    valueFormatter = bottomAxisValueFormatter
                ),
            ),
            marker = rememberMarker(),
            runInitialAnimation = false,
            horizontalLayout = HorizontalLayout.fullWidth(),
        )
    }
}

@Composable
private fun rememberChartStartAxis(axisTitle: String) = rememberStartAxis(
    title = axisTitle,
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
    label = axisLabelComponent(
        color = AppColor.onBackground,
        background = rememberShapeComponent(
            shape = Shapes.pillShape,
            color = Color.Transparent,
            strokeColor = MaterialTheme.colorScheme.outlineVariant,
            strokeWidth = 1.dp,
        ),
        padding = remember { dimensionsOf(horizontal = 6.dp, vertical = 2.dp) },
        margins = remember { dimensionsOf(end = 8.dp) },
    ),
    axis = null,
    tick = null,
    guideline = rememberLineComponent(
        thickness = 1.dp,
        color = MaterialTheme.colorScheme.outlineVariant,
        shape = remember {
            Shapes.dashedShape(
                shape = Shapes.pillShape,
                dashLength = 4.dp,
                gapLength = 8.dp,
            )
        },
    ),
    valueFormatter = PercentageFormatAxisValueFormatter(),
    itemPlacer = remember { AxisItemPlacer.Vertical.default(maxItemCount = { 5 }) },
)

@Composable
private fun rememberChartLineLayer(
    chartColors: List<Color>,
    maxY: Float,
    minY: Float,
) = rememberLineCartesianLayer(
    lines = listOf(
        lineSpec(
            shader = chartLineColorShader(chartColors),
            backgroundShader = chartLineBackgroundShader(chartColors),
        ),
    ),
    axisValueOverrider = AxisValueOverrider.fixed(
        maxY = maxY,
        minY = minY,
    )
)

@Composable
private fun chartLineColorShader(
    chartColors: List<Color>
) = TopBottomShader(
    DynamicShaders.color(chartColors[0]),
    DynamicShaders.color(chartColors[1]),
)

@Composable
private fun chartLineBackgroundShader(
    chartColors: List<Color>
) = TopBottomShader(
    topShader = DynamicShaders.composeShader(
        DynamicShaders.fromComponent(
            componentSize = 6.dp,
            component = rememberShapeComponent(
                shape = Shapes.pillShape,
                color = chartColors[0],
                margins = remember { dimensionsOf(1.dp) },
            ),
        ),
        DynamicShaders.verticalGradient(
            arrayOf(Color.Black, Color.Transparent),
        ),
        PorterDuff.Mode.DST_IN,
    ),
    bottomShader = DynamicShaders.composeShader(
        DynamicShaders.fromComponent(
            componentSize = 5.dp,
            component = rememberShapeComponent(
                shape = Shapes.rectShape,
                color = chartColors[1],
                margins = remember { dimensionsOf(horizontal = 2.dp) },
            ),
            checkeredArrangement = false,
        ),
        DynamicShaders.verticalGradient(
            arrayOf(Color.Transparent, Color.Black),
        ),
        PorterDuff.Mode.DST_IN,
    ),
)



const val COLUMN_WIDTH: Float = 8f
const val COLUMN_ROUNDNESS_PERCENT: Int = 12







