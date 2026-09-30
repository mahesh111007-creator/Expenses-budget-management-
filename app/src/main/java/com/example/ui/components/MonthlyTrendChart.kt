package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.TrendingDown
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilterChip
import androidx.compose.material3.FilterChipDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.model.DailySpending
import com.example.ui.model.MonthlyTrendPoint
import kotlin.math.max

enum class TrendViewMode {
    MONTHLY_6M,
    DAILY_PACE
}

@Composable
fun MonthlyTrendChart(
    trends: List<MonthlyTrendPoint>,
    dailySpendings: List<DailySpending>,
    overallBudget: Double,
    modifier: Modifier = Modifier
) {
    var viewMode by remember { mutableStateOf(TrendViewMode.MONTHLY_6M) }
    var selectedIndex by remember { mutableStateOf<Int?>(null) }

    // Reset selection when changing mode
    LaunchedEffect(viewMode) {
        selectedIndex = null
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("trend_chart_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header with toggle
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending Trends",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = if (viewMode == TrendViewMode.MONTHLY_6M) "6-Month historical comparison" else "Month daily pacing vs budget",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(6.dp)) {
                    FilterChip(
                        selected = viewMode == TrendViewMode.MONTHLY_6M,
                        onClick = { viewMode = TrendViewMode.MONTHLY_6M },
                        label = { Text("6-Month", fontSize = 12.sp) },
                        modifier = Modifier.testTag("toggle_6m_trend"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                    FilterChip(
                        selected = viewMode == TrendViewMode.DAILY_PACE,
                        onClick = { viewMode = TrendViewMode.DAILY_PACE },
                        label = { Text("Daily Pace", fontSize = 12.sp) },
                        modifier = Modifier.testTag("toggle_daily_pace"),
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = MaterialTheme.colorScheme.primaryContainer,
                            selectedLabelColor = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                    )
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            // Interactive Tooltip / Detail Banner when user scrubs or taps
            if (viewMode == TrendViewMode.MONTHLY_6M) {
                val activePoint = selectedIndex?.let { trends.getOrNull(it) } ?: trends.lastOrNull()
                if (activePoint != null) {
                    TrendDetailBanner(point = activePoint)
                }
            } else {
                val activeDay = selectedIndex?.let { dailySpendings.getOrNull(it) } ?: dailySpendings.lastOrNull { it.amount > 0 }
                if (activeDay != null) {
                    DailyPaceDetailBanner(day = activeDay, totalBudget = overallBudget)
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            // Chart Canvas
            if (viewMode == TrendViewMode.MONTHLY_6M) {
                SixMonthTrendCanvas(
                    trends = trends,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it }
                )
            } else {
                DailyPaceTrendCanvas(
                    dailySpendings = dailySpendings,
                    totalBudget = overallBudget,
                    selectedIndex = selectedIndex,
                    onSelect = { selectedIndex = it }
                )
            }

            Spacer(modifier = Modifier.height(10.dp))

            // Legend
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (viewMode == TrendViewMode.MONTHLY_6M) {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "Spending Trend")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = MaterialTheme.colorScheme.primary.copy(alpha = 0.35f), label = "Monthly Total")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = MaterialTheme.colorScheme.outline, label = "Average", isDashed = true)
                } else {
                    LegendItem(color = MaterialTheme.colorScheme.primary, label = "Actual Cumulative")
                    Spacer(modifier = Modifier.width(16.dp))
                    LegendItem(color = MaterialTheme.colorScheme.error.copy(alpha = 0.7f), label = "Ideal Budget Pace", isDashed = true)
                }
            }
        }
    }
}

@Composable
private fun SixMonthTrendCanvas(
    trends: List<MonthlyTrendPoint>,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val primaryContainer = MaterialTheme.colorScheme.primaryContainer
    val onSurfaceVariant = MaterialTheme.colorScheme.onSurfaceVariant
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.4f)
    val highlightColor = MaterialTheme.colorScheme.tertiary

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(trends) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    val maxVal = remember(trends) {
        val maxExpense = trends.maxOfOrNull { it.totalExpense } ?: 3000.0
        val maxBudget = trends.maxOfOrNull { it.budgetLimit } ?: 3500.0
        max(maxExpense, maxBudget) * 1.15
    }

    val avgExpense = remember(trends) {
        if (trends.isNotEmpty()) trends.map { it.totalExpense }.average() else 0.0
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("trend_canvas_box")
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(trends) {
                    detectTapGestures { offset ->
                        val count = trends.size
                        if (count > 0) {
                            val step = size.width / count
                            val index = (offset.x / step).toInt().coerceIn(0, count - 1)
                            onSelect(index)
                        }
                    }
                }
                .pointerInput(trends) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val count = trends.size
                        if (count > 0) {
                            val step = size.width / count
                            val index = (change.position.x / step).toInt().coerceIn(0, count - 1)
                            onSelect(index)
                        }
                    }
                }
        ) {
            val count = trends.size
            if (count == 0) return@Canvas

            val chartWidth = size.width
            val chartHeight = size.height - 30.dp.toPx() // Reserve bottom for month labels
            val bottomY = chartHeight
            val stepX = chartWidth / count
            val barWidth = stepX * 0.42f

            // Draw horizontal grid lines
            val gridLines = 4
            for (i in 0..gridLines) {
                val y = (chartHeight / gridLines) * i
                drawLine(
                    color = outlineColor.copy(alpha = 0.2f),
                    start = Offset(0f, y),
                    end = Offset(chartWidth, y),
                    strokeWidth = 1.dp.toPx()
                )
            }

            // Draw average dashed line
            if (avgExpense > 0) {
                val avgY = bottomY - ((avgExpense / maxVal) * bottomY).toFloat() * animationProgress.value
                drawLine(
                    color = outlineColor,
                    start = Offset(0f, avgY),
                    end = Offset(chartWidth, avgY),
                    strokeWidth = 1.5.dp.toPx(),
                    pathEffect = PathEffect.dashPathEffect(floatArrayOf(10f, 10f), 0f)
                )
            }

            // Compute coordinates for bars and trend line
            val points = mutableListOf<Offset>()
            for (i in 0 until count) {
                val point = trends[i]
                val centerX = (i * stepX) + (stepX / 2f)
                val expenseHeight = ((point.totalExpense / maxVal) * bottomY).toFloat() * animationProgress.value
                val topY = bottomY - expenseHeight
                points.add(Offset(centerX, topY))

                // Draw bar with subtle gradient
                val isSelected = selectedIndex == i
                val barBrush = Brush.verticalGradient(
                    colors = if (isSelected) {
                        listOf(highlightColor, highlightColor.copy(alpha = 0.5f))
                    } else {
                        listOf(primaryColor.copy(alpha = 0.55f), primaryColor.copy(alpha = 0.15f))
                    },
                    startY = topY,
                    endY = bottomY
                )

                drawRoundRect(
                    brush = barBrush,
                    topLeft = Offset(centerX - barWidth / 2f, topY),
                    size = Size(barWidth, expenseHeight),
                    cornerRadius = CornerRadius(6.dp.toPx(), 6.dp.toPx())
                )
            }

            // Draw smooth trend curve
            if (points.size > 1) {
                val linePath = Path()
                val areaPath = Path()

                linePath.moveTo(points[0].x, points[0].y)
                areaPath.moveTo(points[0].x, bottomY)
                areaPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    areaPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }

                areaPath.lineTo(points.last().x, bottomY)
                areaPath.close()

                // Area under curve gradient
                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.20f), Color.Transparent),
                        startY = 0f,
                        endY = bottomY
                    )
                )

                // Main trend line
                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(
                        width = 3.dp.toPx(),
                        cap = StrokeCap.Round
                    )
                )

                // Draw points and highlight rings
                for (i in 0 until points.size) {
                    val p = points[i]
                    val isSelected = selectedIndex == i

                    if (isSelected) {
                        drawCircle(
                            color = highlightColor.copy(alpha = 0.35f),
                            radius = 9.dp.toPx(),
                            center = p
                        )
                        drawCircle(
                            color = highlightColor,
                            radius = 5.dp.toPx(),
                            center = p
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = p
                        )
                    } else {
                        drawCircle(
                            color = primaryColor,
                            radius = 4.dp.toPx(),
                            center = p
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 2.dp.toPx(),
                            center = p
                        )
                    }
                }
            }
        }

        // Bottom Month Labels
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceAround
        ) {
            trends.forEachIndexed { index, point ->
                val isSelected = selectedIndex == index
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelSmall,
                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                    color = if (isSelected) MaterialTheme.colorScheme.primary else onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun DailyPaceTrendCanvas(
    dailySpendings: List<DailySpending>,
    totalBudget: Double,
    selectedIndex: Int?,
    onSelect: (Int) -> Unit
) {
    val primaryColor = MaterialTheme.colorScheme.primary
    val errorColor = MaterialTheme.colorScheme.error
    val outlineColor = MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
    val highlightColor = MaterialTheme.colorScheme.tertiary

    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(dailySpendings) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(1f, animationSpec = tween(durationMillis = 700))
    }

    val daysCount = dailySpendings.size.coerceAtLeast(28)
    val maxVal = remember(dailySpendings, totalBudget) {
        val maxSpend = dailySpendings.maxOfOrNull { it.cumulativeAmount } ?: totalBudget
        max(maxSpend, totalBudget) * 1.15
    }

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .testTag("daily_pace_canvas_box")
    ) {
        Canvas(
            modifier = Modifier
                .matchParentSize()
                .pointerInput(dailySpendings) {
                    detectTapGestures { offset ->
                        val count = dailySpendings.size
                        if (count > 0) {
                            val step = size.width / count
                            val index = (offset.x / step).toInt().coerceIn(0, count - 1)
                            onSelect(index)
                        }
                    }
                }
                .pointerInput(dailySpendings) {
                    detectDragGestures { change, _ ->
                        change.consume()
                        val count = dailySpendings.size
                        if (count > 0) {
                            val step = size.width / count
                            val index = (change.position.x / step).toInt().coerceIn(0, count - 1)
                            onSelect(index)
                        }
                    }
                }
        ) {
            val count = dailySpendings.size
            if (count == 0) return@Canvas

            val chartWidth = size.width
            val chartHeight = size.height - 30.dp.toPx()
            val bottomY = chartHeight
            val stepX = chartWidth / (count - 1).coerceAtLeast(1)

            // Draw Ideal Budget Pace Line (Linear from (0,0) to (end, totalBudget))
            val idealEndY = bottomY - ((totalBudget / maxVal) * bottomY).toFloat()
            drawLine(
                color = errorColor.copy(alpha = 0.6f),
                start = Offset(0f, bottomY),
                end = Offset(chartWidth, idealEndY),
                strokeWidth = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(8f, 8f), 0f)
            )

            // Draw Actual Cumulative Spending Path
            val points = mutableListOf<Offset>()
            for (i in 0 until count) {
                val day = dailySpendings[i]
                val x = i * stepX
                val y = bottomY - ((day.cumulativeAmount / maxVal) * bottomY).toFloat() * animationProgress.value
                points.add(Offset(x, y))
            }

            if (points.size > 1) {
                val linePath = Path()
                val areaPath = Path()

                linePath.moveTo(points[0].x, points[0].y)
                areaPath.moveTo(points[0].x, bottomY)
                areaPath.lineTo(points[0].x, points[0].y)

                for (i in 0 until points.size - 1) {
                    val p0 = points[i]
                    val p1 = points[i + 1]
                    val cx = (p0.x + p1.x) / 2f
                    linePath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                    areaPath.cubicTo(cx, p0.y, cx, p1.y, p1.x, p1.y)
                }

                areaPath.lineTo(points.last().x, bottomY)
                areaPath.close()

                drawPath(
                    path = areaPath,
                    brush = Brush.verticalGradient(
                        colors = listOf(primaryColor.copy(alpha = 0.25f), Color.Transparent),
                        startY = 0f,
                        endY = bottomY
                    )
                )

                drawPath(
                    path = linePath,
                    color = primaryColor,
                    style = Stroke(width = 3.dp.toPx(), cap = StrokeCap.Round)
                )

                // Highlight selected day
                selectedIndex?.let { idx ->
                    if (idx in points.indices) {
                        val p = points[idx]
                        // Vertical guideline
                        drawLine(
                            color = highlightColor.copy(alpha = 0.5f),
                            start = Offset(p.x, 0f),
                            end = Offset(p.x, bottomY),
                            strokeWidth = 1.dp.toPx(),
                            pathEffect = PathEffect.dashPathEffect(floatArrayOf(4f, 4f), 0f)
                        )
                        drawCircle(
                            color = highlightColor,
                            radius = 6.dp.toPx(),
                            center = p
                        )
                        drawCircle(
                            color = Color.White,
                            radius = 3.dp.toPx(),
                            center = p
                        )
                    }
                }
            }
        }

        // Day numbers along bottom
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .align(Alignment.BottomCenter)
                .padding(top = 4.dp),
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val checkpoints = listOf(1, 7, 14, 21, 28, daysCount)
            checkpoints.forEach { dayNum ->
                Text(
                    text = "Day $dayNum",
                    style = MaterialTheme.typography.labelSmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun TrendDetailBanner(point: MonthlyTrendPoint) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = point.label,
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Budget: $%.0f".format(point.budgetLimit),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "$%.2f".format(point.totalExpense),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                Row(verticalAlignment = Alignment.CenterVertically) {
                    val isSavingsPositive = point.netSavings >= 0
                    Icon(
                        imageVector = if (isSavingsPositive) Icons.Default.TrendingUp else Icons.Default.TrendingDown,
                        contentDescription = null,
                        tint = if (isSavingsPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(modifier = Modifier.width(3.dp))
                    Text(
                        text = if (isSavingsPositive) "Saved $%.0f".format(point.netSavings) else "Deficit $%.0f".format(-point.netSavings),
                        style = MaterialTheme.typography.bodySmall,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isSavingsPositive) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error
                    )
                }
            }
        }
    }
}

@Composable
private fun DailyPaceDetailBanner(day: DailySpending, totalBudget: Double) {
    Surface(
        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
        shape = RoundedCornerShape(12.dp),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 14.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Column {
                Text(
                    text = "Day ${day.day}",
                    style = MaterialTheme.typography.labelLarge,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    text = "Spent that day: $%.2f".format(day.amount),
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "Cumulative: $%.2f".format(day.cumulativeAmount),
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
                val percentOfBudget = if (totalBudget > 0) (day.cumulativeAmount / totalBudget) * 100 else 0.0
                Text(
                    text = "%.1f%% of budget".format(percentOfBudget),
                    style = MaterialTheme.typography.bodySmall,
                    color = if (percentOfBudget > 100) MaterialTheme.colorScheme.error else MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

@Composable
private fun LegendItem(color: Color, label: String, isDashed: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        if (isDashed) {
            Box(
                modifier = Modifier
                    .width(14.dp)
                    .height(3.dp)
                    .background(color, RoundedCornerShape(1.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(10.dp)
                    .background(color, CircleShape)
            )
        }
        Spacer(modifier = Modifier.width(6.dp))
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
