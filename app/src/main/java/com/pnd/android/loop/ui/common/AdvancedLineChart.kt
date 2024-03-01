package com.pnd.android.loop.ui.common

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
        color = MaterialTheme.colorScheme.onBackground,
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


@Composable
internal fun rememberMarker(
    labelPosition: MarkerComponent.LabelPosition = MarkerComponent.LabelPosition.Top
): Marker {
    val labelBackgroundColor = AppColor.surface
    val labelBackground = remember(labelBackgroundColor) {
        ShapeComponent(labelBackgroundShape, labelBackgroundColor.toArgb())
            .setShadow(
                radius = LABEL_BACKGROUND_SHADOW_RADIUS,
                dy = LABEL_BACKGROUND_SHADOW_DY,
                applyElevationOverlay = true,
            )
    }
    val label = rememberTextComponent(
        background = labelBackground,
        lineCount = LABEL_LINE_COUNT,
        padding = labelPadding,
        typeface = Typeface.MONOSPACE,
    )
    val indicatorInnerComponent = rememberShapeComponent(Shapes.pillShape, AppColor.surface)
    val indicatorCenterComponent = rememberShapeComponent(Shapes.pillShape, Color.White)
    val indicatorOuterComponent = rememberShapeComponent(Shapes.pillShape, Color.White)
    val indicator = overlayingComponent(
        outer = indicatorOuterComponent,
        inner = overlayingComponent(
            outer = indicatorCenterComponent,
            inner = indicatorInnerComponent,
            innerPaddingAll = indicatorInnerAndCenterComponentPaddingValue,
        ),
        innerPaddingAll = indicatorCenterAndOuterComponentPaddingValue,
    )
    val guideline = rememberLineComponent(
        MaterialTheme.colorScheme.onSurface.copy(GUIDELINE_ALPHA),
        guidelineThickness,
        guidelineShape,
    )
    return remember(label, labelPosition, indicator, guideline) {
        object : MarkerComponent(label, labelPosition, indicator, guideline) {
            init {
                indicatorSizeDp = INDICATOR_SIZE_DP
                onApplyEntryColor = { entryColor ->
                    indicatorOuterComponent.color =
                        entryColor.copyColor(INDICATOR_OUTER_COMPONENT_ALPHA)
                    with(indicatorCenterComponent) {
                        color = entryColor
                        setShadow(
                            radius = INDICATOR_CENTER_COMPONENT_SHADOW_RADIUS,
                            color = entryColor
                        )
                    }
                }
            }

            override fun getInsets(
                context: MeasureContext,
                outInsets: Insets,
                horizontalDimensions: HorizontalDimensions,
            ) {
                with(context) {
                    outInsets.top =
                        (SHADOW_RADIUS_MULTIPLIER * LABEL_BACKGROUND_SHADOW_RADIUS - LABEL_BACKGROUND_SHADOW_DY).pixels
                    if (labelPosition == LabelPosition.AroundPoint) return
                    outInsets.top += label.getHeight(context) + labelBackgroundShape.tickSizeDp.pixels
                }
            }
        }
    }
}


@Composable
internal fun rememberChartStyle(chartColors: List<Color>) = rememberChartStyle(
    columnLayerColors = chartColors,
    lineLayerColors = chartColors
)

@Composable
internal fun rememberChartStyle(
    columnLayerColors: List<Color>,
    lineLayerColors: List<Color>,
): ChartStyle {
    val isSystemInDarkTheme = isSystemInDarkTheme()
    return remember(columnLayerColors, lineLayerColors, isSystemInDarkTheme) {
        val defaultColors = if (isSystemInDarkTheme) {
            DefaultColors.Dark
        } else {
            DefaultColors.Light
        }

        ChartStyle(
            axis = ChartStyle.Axis(
                axisLabelColor = Color(defaultColors.axisLabelColor),
                axisGuidelineColor = Color(defaultColors.axisGuidelineColor),
                axisLineColor = Color(defaultColors.axisLineColor),
            ),
            columnLayer = ChartStyle.ColumnLayer(
                columnLayerColors.map { columnChartColor ->
                    LineComponent(
                        columnChartColor.toArgb(),
                        COLUMN_WIDTH,
                        Shapes.roundedCornerShape(COLUMN_ROUNDNESS_PERCENT),
                    )
                },
            ),
            lineLayer = ChartStyle.LineLayer(
                lineLayerColors.map { color ->
                    LineCartesianLayer.LineSpec(
                        shader = DynamicShaders.color(color),
                        backgroundShader = DynamicShaders.fromBrush(
                            Brush.verticalGradient(
                                listOf(
                                    color.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_START),
                                    color.copy(DefaultAlpha.LINE_BACKGROUND_SHADER_END),
                                ),
                            ),
                        ),
                    )
                },
            ),
            marker = ChartStyle.Marker(),
            elevationOverlayColor = Color(defaultColors.elevationOverlayColor),
        )
    }
}

const val COLUMN_WIDTH: Float = 8f
const val COLUMN_ROUNDNESS_PERCENT: Int = 40

private const val LABEL_BACKGROUND_SHADOW_RADIUS = 4f
private const val LABEL_BACKGROUND_SHADOW_DY = 2f
private const val LABEL_LINE_COUNT = 1
private const val GUIDELINE_ALPHA = .2f
private const val INDICATOR_SIZE_DP = 36f
private const val INDICATOR_OUTER_COMPONENT_ALPHA = 32
private const val INDICATOR_CENTER_COMPONENT_SHADOW_RADIUS = 12f
private const val GUIDELINE_DASH_LENGTH_DP = 8f
private const val GUIDELINE_GAP_LENGTH_DP = 4f
private const val SHADOW_RADIUS_MULTIPLIER = 1.3f

private val labelBackgroundShape = MarkerCorneredShape(Corner.FullyRounded)
private val labelHorizontalPaddingValue = 8.dp
private val labelVerticalPaddingValue = 4.dp
private val labelPadding = dimensionsOf(labelHorizontalPaddingValue, labelVerticalPaddingValue)
private val indicatorInnerAndCenterComponentPaddingValue = 5.dp
private val indicatorCenterAndOuterComponentPaddingValue = 10.dp
private val guidelineThickness = 2.dp
private val guidelineShape =
    DashedShape(Shapes.pillShape, GUIDELINE_DASH_LENGTH_DP, GUIDELINE_GAP_LENGTH_DP)



