package com.pnd.android.loop.ui.common.chart

import android.graphics.Typeface
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.unit.dp
import com.patrykandpatrick.vico.compose.component.overlayingComponent
import com.patrykandpatrick.vico.compose.component.rememberLineComponent
import com.patrykandpatrick.vico.compose.component.rememberShapeComponent
import com.patrykandpatrick.vico.compose.component.rememberTextComponent
import com.patrykandpatrick.vico.compose.component.shape.shader.color
import com.patrykandpatrick.vico.compose.component.shape.shader.fromBrush
import com.patrykandpatrick.vico.compose.dimensions.dimensionsOf
import com.patrykandpatrick.vico.compose.style.ChartStyle
import com.patrykandpatrick.vico.core.DefaultAlpha
import com.patrykandpatrick.vico.core.DefaultColors
import com.patrykandpatrick.vico.core.chart.dimensions.HorizontalDimensions
import com.patrykandpatrick.vico.core.chart.insets.Insets
import com.patrykandpatrick.vico.core.chart.layer.LineCartesianLayer
import com.patrykandpatrick.vico.core.component.marker.MarkerComponent
import com.patrykandpatrick.vico.core.component.shape.DashedShape
import com.patrykandpatrick.vico.core.component.shape.LineComponent
import com.patrykandpatrick.vico.core.component.shape.ShapeComponent
import com.patrykandpatrick.vico.core.component.shape.Shapes
import com.patrykandpatrick.vico.core.component.shape.cornered.Corner
import com.patrykandpatrick.vico.core.component.shape.cornered.MarkerCorneredShape
import com.patrykandpatrick.vico.core.component.shape.shader.DynamicShaders
import com.patrykandpatrick.vico.core.context.MeasureContext
import com.patrykandpatrick.vico.core.extension.copyColor
import com.patrykandpatrick.vico.core.marker.Marker
import com.pnd.android.loop.ui.theme.AppColor
import com.pnd.android.loop.ui.theme.onSurface
import com.pnd.android.loop.ui.theme.surface


@Composable
internal fun rememberMarker(
    labelPosition: MarkerComponent.LabelPosition = MarkerComponent.LabelPosition.Top
): Marker {
    val labelBackgroundColor = AppColor.surface
    val labelBackground = remember(labelBackgroundColor) {
        ShapeComponent(
            shape = labelBackgroundShape,
            color = labelBackgroundColor.toArgb()
        ).setShadow(
            radius = 4f,
            dy = 2f,
            applyElevationOverlay = true,
        )
    }
    val label = rememberTextComponent(
        background = labelBackground,
        lineCount = 1,
        padding = dimensionsOf(8.dp, 4.dp),
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
            innerPaddingAll = 5.dp,
        ),
        innerPaddingAll = 10.dp,
    )

    val guideline = rememberLineComponent(
        color = AppColor.onSurface.copy(0.2f),
        thickness = 2.dp,
        shape = DashedShape(
            shape = Shapes.pillShape,
            dashLengthDp = 8f,
            gapLengthDp = 4f
        ),
    )
    return remember(label, labelPosition, indicator, guideline) {
        object : MarkerComponent(label, labelPosition, indicator, guideline) {
            init {
                indicatorSizeDp = 36f
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

private const val LABEL_BACKGROUND_SHADOW_RADIUS = 8f
private const val LABEL_BACKGROUND_SHADOW_DY = 4f

private const val INDICATOR_OUTER_COMPONENT_ALPHA = 32
private const val INDICATOR_CENTER_COMPONENT_SHADOW_RADIUS = 12f
private const val SHADOW_RADIUS_MULTIPLIER = 1.3f
private val labelBackgroundShape = MarkerCorneredShape(Corner.FullyRounded)