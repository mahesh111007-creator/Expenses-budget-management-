package com.example.ui.components

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
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
import androidx.compose.material.icons.filled.PieChart
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.data.model.ExpenseCategory
import com.example.ui.model.CategorySpending
import kotlin.math.atan2
import kotlin.math.min
import kotlin.math.sqrt

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun CategoryDonutChart(
    categorySpendings: List<CategorySpending>,
    totalSpent: Double,
    selectedCategory: ExpenseCategory?,
    onCategorySelected: (ExpenseCategory?) -> Unit,
    modifier: Modifier = Modifier
) {
    val animationProgress = remember { Animatable(0f) }
    LaunchedEffect(categorySpendings) {
        animationProgress.snapTo(0f)
        animationProgress.animateTo(
            targetValue = 1f,
            animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
        )
    }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .testTag("category_donut_card"),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Spending by Category",
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        text = "Tap any segment or tag to inspect",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }

                if (selectedCategory != null) {
                    Surface(
                        shape = RoundedCornerShape(8.dp),
                        color = MaterialTheme.colorScheme.primaryContainer,
                        modifier = Modifier
                            .clickable { onCategorySelected(null) }
                            .testTag("clear_category_selection")
                    ) {
                        Text(
                            text = "Reset Filter",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.SemiBold,
                            color = MaterialTheme.colorScheme.onPrimaryContainer,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(16.dp))

            if (categorySpendings.isEmpty() || totalSpent <= 0) {
                // Empty state
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            imageVector = Icons.Default.PieChart,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.outline,
                            modifier = Modifier.size(48.dp)
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "No expenses recorded this month",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            } else {
                // Donut with Center Content
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .testTag("donut_chart_canvas_box"),
                    contentAlignment = Alignment.Center
                ) {
                    Canvas(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(220.dp)
                            .pointerInput(categorySpendings, totalSpent) {
                                detectTapGestures { offset ->
                                    val centerX = size.width / 2f
                                    val centerY = size.height / 2f
                                    val dx = offset.x - centerX
                                    val dy = offset.y - centerY
                                    val distance = sqrt(dx * dx + dy * dy)
                                    val radius = min(centerX, centerY)
                                    val innerRadius = radius * 0.58f

                                    // If tapped within donut ring
                                    if (distance in innerRadius..radius) {
                                        var angle = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
                                        // Standardize to 0..360 starting from top (-90 degrees)
                                        angle = (angle + 90f + 360f) % 360f

                                        var cumulativeAngle = 0f
                                        for (item in categorySpendings) {
                                            val sweep = (item.amount / totalSpent).toFloat() * 360f
                                            if (angle in cumulativeAngle..(cumulativeAngle + sweep)) {
                                                if (selectedCategory == item.category) {
                                                    onCategorySelected(null)
                                                } else {
                                                    onCategorySelected(item.category)
                                                }
                                                break
                                            }
                                            cumulativeAngle += sweep
                                        }
                                    } else if (distance < innerRadius) {
                                        // Tapped center circle
                                        onCategorySelected(null)
                                    }
                                }
                            }
                    ) {
                        val strokeWidth = 26.dp.toPx()
                        val diameter = min(size.width, size.height) - strokeWidth
                        val topLeft = Offset((size.width - diameter) / 2f, (size.height - diameter) / 2f)
                        val arcSize = Size(diameter, diameter)

                        var startAngle = -90f
                        val gapAngle = if (categorySpendings.size > 1) 2.5f else 0f

                        categorySpendings.forEach { item ->
                            val fullSweep = (item.amount / totalSpent).toFloat() * 360f
                            val sweep = (fullSweep - gapAngle).coerceAtLeast(1f) * animationProgress.value
                            val isSelected = selectedCategory == null || selectedCategory == item.category
                            val segmentColor = if (isSelected) {
                                item.category.composeColor
                            } else {
                                item.category.composeColor.copy(alpha = 0.25f)
                            }
                            val currentStroke = if (selectedCategory == item.category) {
                                strokeWidth + 6.dp.toPx()
                            } else {
                                strokeWidth
                            }

                            drawArc(
                                color = segmentColor,
                                startAngle = startAngle + (gapAngle / 2f),
                                sweepAngle = sweep,
                                useCenter = false,
                                topLeft = topLeft,
                                size = arcSize,
                                style = Stroke(width = currentStroke, cap = StrokeCap.Round)
                            )
                            startAngle += fullSweep
                        }
                    }

                    // Center Content Hole
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(16.dp)
                    ) {
                        val activeItem = selectedCategory?.let { sel -> categorySpendings.find { it.category == sel } }
                        if (activeItem != null) {
                            Icon(
                                imageVector = activeItem.category.icon,
                                contentDescription = null,
                                tint = activeItem.category.composeColor,
                                modifier = Modifier.size(24.dp)
                            )
                            Spacer(modifier = Modifier.height(2.dp))
                            Text(
                                text = "$%.0f".format(activeItem.amount),
                                style = MaterialTheme.typography.titleLarge,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "%.1f%%".format(activeItem.percentage),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.SemiBold,
                                color = activeItem.category.composeColor
                            )
                            Text(
                                text = activeItem.category.title,
                                style = MaterialTheme.typography.bodySmall,
                                maxLines = 1,
                                textAlign = TextAlign.Center,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        } else {
                            Text(
                                text = "Total Spent",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                            Text(
                                text = "$%.0f".format(totalSpent),
                                style = MaterialTheme.typography.headlineSmall,
                                fontWeight = FontWeight.ExtraBold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = "${categorySpendings.size} Categories",
                                style = MaterialTheme.typography.labelSmall,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(18.dp))

                // Interactive Category Chips with percentages
                FlowRow(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    categorySpendings.take(8).forEach { item ->
                        val isSelected = selectedCategory == item.category
                        Surface(
                            shape = RoundedCornerShape(12.dp),
                            color = if (isSelected) {
                                item.category.composeColor.copy(alpha = 0.18f)
                            } else {
                                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f)
                            },
                            border = if (isSelected) {
                                androidx.compose.foundation.BorderStroke(1.5.dp, item.category.composeColor)
                            } else null,
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .clickable {
                                    if (isSelected) onCategorySelected(null)
                                    else onCategorySelected(item.category)
                                }
                                .testTag("category_chip_${item.category.name}")
                        ) {
                            Row(
                                verticalAlignment = Alignment.CenterVertically,
                                modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(10.dp)
                                        .background(item.category.composeColor, CircleShape)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = item.category.title,
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = MaterialTheme.colorScheme.onSurface
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = "%.0f%%".format(item.percentage),
                                    style = MaterialTheme.typography.labelSmall,
                                    fontWeight = FontWeight.Bold,
                                    color = if (isSelected) item.category.composeColor else MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}
