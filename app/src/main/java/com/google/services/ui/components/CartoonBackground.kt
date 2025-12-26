/**
 * ============================================================================
 * CartoonBackground.kt - 卡通动画背景组件（增强版）
 * ============================================================================
 *
 * 功能简介：
 *   为应用提供丰富的卡通动画背景效果，包括：
 *   - 闪烁的星星（更大更亮）
 *   - 飘动的云朵（更明显）
 *   - 漂浮的气泡（更大更清晰）
 *   - 飞舞的精灵（8字形轨迹，翅膀扇动，明显可见）
 *   - 闪光特效（更强烈）
 *
 * 优化说明：
 *   - 增大所有元素的尺寸
 *   - 提高颜色对比度和透明度
 *   - 增加精灵数量和飞行范围
 *   - 加快部分动画速度
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.*
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.drawscope.scale
import com.google.services.ui.theme.*
import kotlin.math.*
import kotlin.random.Random

// ==================== 数据类定义 ====================

/** 星星数据 */
data class Star(
    val x: Float,           // X 坐标比例 (0-1)
    val y: Float,           // Y 坐标比例 (0-1)
    val size: Float,        // 大小 (dp)
    val initialPhase: Float // 初始相位 (0-2π)
)

/** 云朵数据 */
data class Cloud(
    val y: Float,           // Y 坐标比例 (0-1)
    val size: Float,        // 大小缩放
    val speed: Float,       // 移动速度
    val initialX: Float     // 初始 X 坐标比例
)

/** 气泡数据 */
data class Bubble(
    val x: Float,           // X 坐标比例 (0-1)
    val size: Float,        // 大小 (dp)
    val speed: Float,       // 上升速度
    val initialY: Float,    // 初始 Y 坐标比例
    val wobblePhase: Float  // 左右摇摆相位
)

/** 精灵数据 */
data class Fairy(
    val centerX: Float,     // 中心 X 坐标比例
    val centerY: Float,     // 中心 Y 坐标比例
    val radiusX: Float,     // X 方向飞行半径
    val radiusY: Float,     // Y 方向飞行半径
    val speed: Float,       // 飞行速度
    val size: Float         // 精灵大小
)

/** 闪光数据 */
data class Sparkle(
    val x: Float,           // X 坐标比例
    val y: Float,           // Y 坐标比例
    val delay: Int,         // 延迟出现时间 (ms)
    val duration: Int       // 闪光持续时间 (ms)
)

// ==================== 主组件 ====================

/**
 * 卡通动画背景
 *
 * @param modifier 修饰符
 * @param showStars 是否显示星星
 * @param showClouds 是否显示云朵
 * @param showBubbles 是否显示气泡
 * @param showFairies 是否显示精灵
 * @param showSparkles 是否显示闪光
 * @param content 子内容
 */
@Composable
fun CartoonBackground(
    modifier: Modifier = Modifier,
    showStars: Boolean = true,
    showClouds: Boolean = true,
    showBubbles: Boolean = true,
    showFairies: Boolean = true,
    showSparkles: Boolean = true,
    content: @Composable () -> Unit
) {
    Box(modifier = modifier) {
        // 粉紫渐变背景
        GradientBackground()

        // 动画层
        if (showClouds) CloudLayer()       // 云朵在最底层
        if (showBubbles) BubbleLayer()     // 气泡
        if (showStars) StarLayer()         // 星星
        if (showSparkles) SparkleLayer()   // 闪光
        if (showFairies) FairyLayer()      // 精灵在最顶层

        // 子内容
        content()
    }
}

// ==================== 背景渐变 ====================

/**
 * 粉紫渐变背景
 */
@Composable
private fun GradientBackground() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Pink40.copy(alpha = 0.3f),       // 顶部：草莓粉
                        Lavender80,                      // 中上：薰衣草
                        SoftPurple.copy(alpha = 0.5f),  // 中下：芋泥紫
                        SkyBlue.copy(alpha = 0.4f),     // 底部：苏打蓝
                        Mint80.copy(alpha = 0.3f)       // 最底：薄荷绿
                    )
                )
            )
    )
}

// ==================== 星星动画层（增强版） ====================

/**
 * 星星闪烁层 - 增大尺寸和亮度
 */
@Composable
private fun StarLayer() {
    // 生成更多更大的星星
    val stars = remember {
        List(20) {                                       // 增加到 20 颗
            Star(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.8f,          // 分布更广
                size = Random.nextFloat() * 20f + 15f,  // 15-35dp（更大）
                initialPhase = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    // 无限循环动画 - 加快速度
    val infiniteTransition = rememberInfiniteTransition(label = "star")
    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),  // 加快到 2 秒
            repeatMode = RepeatMode.Restart
        ),
        label = "starTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        stars.forEach { star ->
            val phase = animatedTime + star.initialPhase
            val alpha = (sin(phase) + 1f) / 2f * 0.8f + 0.2f  // 0.2-1.0（更亮）
            val scale = (sin(phase * 1.5f) + 1f) / 2f * 0.4f + 0.6f

            val x = star.x * size.width
            val y = star.y * size.height

            // 绘制更亮的星星
            drawStar(
                center = Offset(x, y),
                size = star.size * scale,
                color = Color(0xFFFFD700).copy(alpha = alpha),  // 金色
                points = 5
            )

            // 星星中心白点 - 更大
            drawCircle(
                color = Color.White.copy(alpha = alpha),
                radius = star.size * scale * 0.2f,
                center = Offset(x, y)
            )
        }
    }
}

/**
 * 绘制五角星
 */
private fun DrawScope.drawStar(
    center: Offset,
    size: Float,
    color: Color,
    points: Int = 5
) {
    val path = Path()
    val outerRadius = size
    val innerRadius = size * 0.4f
    val angleStep = PI.toFloat() / points

    for (i in 0 until points * 2) {
        val radius = if (i % 2 == 0) outerRadius else innerRadius
        val angle = i * angleStep - PI.toFloat() / 2
        val x = center.x + cos(angle) * radius
        val y = center.y + sin(angle) * radius

        if (i == 0) path.moveTo(x, y)
        else path.lineTo(x, y)
    }
    path.close()

    drawPath(path, color)
}

// ==================== 云朵动画层（增强版） ====================

/**
 * 云朵飘动层 - 更大更白
 */
@Composable
private fun CloudLayer() {
    val clouds = remember {
        List(5) {                                        // 增加到 5 朵
            Cloud(
                y = Random.nextFloat() * 0.5f + 0.05f,  // 0.05-0.55
                size = Random.nextFloat() * 0.6f + 0.6f, // 0.6-1.2（更大）
                speed = Random.nextFloat() * 0.2f + 0.15f,
                initialX = Random.nextFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "cloud")
    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(25000, easing = LinearEasing),  // 稍快
            repeatMode = RepeatMode.Restart
        ),
        label = "cloudTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        clouds.forEach { cloud ->
            val progress = (animatedTime * cloud.speed + cloud.initialX) % 1.3f - 0.15f
            val x = progress * size.width
            val y = cloud.y * size.height

            // 绘制更白的云朵
            drawCloud(
                center = Offset(x, y),
                scale = cloud.size,
                alpha = 0.85f                           // 更不透明
            )
        }
    }
}

/**
 * 绘制云朵
 */
private fun DrawScope.drawCloud(
    center: Offset,
    scale: Float,
    alpha: Float
) {
    val cloudColor = Color.White.copy(alpha = alpha)
    val baseSize = 50f * scale                          // 基础更大

    drawCircle(cloudColor, baseSize * 0.8f, Offset(center.x - baseSize * 0.6f, center.y))
    drawCircle(cloudColor, baseSize, Offset(center.x, center.y - baseSize * 0.2f))
    drawCircle(cloudColor, baseSize * 0.9f, Offset(center.x + baseSize * 0.7f, center.y))
    drawCircle(cloudColor, baseSize * 0.7f, Offset(center.x + baseSize * 1.2f, center.y + baseSize * 0.1f))
    drawCircle(cloudColor, baseSize * 0.6f, Offset(center.x - baseSize * 1.0f, center.y + baseSize * 0.1f))

    drawOval(
        cloudColor,
        Offset(center.x - baseSize * 1.2f, center.y),
        androidx.compose.ui.geometry.Size(baseSize * 2.6f, baseSize * 0.8f)
    )
}

// ==================== 气泡动画层（增强版） ====================

/**
 * 气泡上升层 - 更大更清晰
 */
@Composable
private fun BubbleLayer() {
    val bubbles = remember {
        List(12) {                                       // 增加到 12 个
            Bubble(
                x = Random.nextFloat(),
                size = Random.nextFloat() * 25f + 15f,  // 15-40dp（更大）
                speed = Random.nextFloat() * 0.25f + 0.15f,
                initialY = Random.nextFloat(),
                wobblePhase = Random.nextFloat() * 2f * PI.toFloat()
            )
        }
    }

    val infiniteTransition = rememberInfiniteTransition(label = "bubble")
    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(12000, easing = LinearEasing),  // 稍快
            repeatMode = RepeatMode.Restart
        ),
        label = "bubbleTime"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        bubbles.forEach { bubble ->
            val yProgress = (animatedTime * bubble.speed + bubble.initialY) % 1.2f
            val y = size.height * (1f - yProgress + 0.1f)

            val wobble = sin(animatedTime * 2f * PI.toFloat() * 3f + bubble.wobblePhase) * 25f
            val x = bubble.x * size.width + wobble

            if (y > -50f && y < size.height + 50f) {
                // 气泡主体 - 更不透明
                drawCircle(
                    color = Color.White.copy(alpha = 0.6f),
                    radius = bubble.size,
                    center = Offset(x, y)
                )

                // 气泡边框
                drawCircle(
                    color = Color.White.copy(alpha = 0.3f),
                    radius = bubble.size,
                    center = Offset(x, y),
                    style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2f)
                )

                // 气泡高光 - 更大
                drawCircle(
                    color = Color.White.copy(alpha = 0.9f),
                    radius = bubble.size * 0.3f,
                    center = Offset(x - bubble.size * 0.3f, y - bubble.size * 0.3f)
                )

                // 小高光
                drawCircle(
                    color = Color.White.copy(alpha = 0.7f),
                    radius = bubble.size * 0.15f,
                    center = Offset(x - bubble.size * 0.45f, y)
                )
            }
        }
    }
}

// ==================== 精灵动画层（增强版 - 更明显） ====================

/**
 * 精灵飞舞层 - 更大、更明显的 8 字形轨迹
 */
@Composable
private fun FairyLayer() {
    // 创建更多、更大的精灵
    val fairies = remember {
        listOf(
            // 主精灵 - 大号，中央飞舞
            Fairy(
                centerX = 0.5f,
                centerY = 0.35f,
                radiusX = 0.25f,                        // 更大的飞行范围
                radiusY = 0.15f,
                speed = 0.6f,
                size = 45f                              // 非常大
            ),
            // 副精灵 1 - 左上
            Fairy(
                centerX = 0.3f,
                centerY = 0.25f,
                radiusX = 0.15f,
                radiusY = 0.1f,
                speed = 0.8f,
                size = 35f
            ),
            // 副精灵 2 - 右侧
            Fairy(
                centerX = 0.7f,
                centerY = 0.45f,
                radiusX = 0.18f,
                radiusY = 0.12f,
                speed = 0.5f,
                size = 30f
            ),
            // 小精灵 - 底部
            Fairy(
                centerX = 0.4f,
                centerY = 0.6f,
                radiusX = 0.12f,
                radiusY = 0.08f,
                speed = 1.0f,
                size = 25f
            )
        )
    }

    val infiniteTransition = rememberInfiniteTransition(label = "fairy")

    // 8 字形飞行路径动画
    val animatedTime by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(5000, easing = LinearEasing),   // 5 秒一个完整 8 字
            repeatMode = RepeatMode.Restart
        ),
        label = "fairyTime"
    )

    // 翅膀快速扇动动画
    val wingFlap by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(150, easing = LinearEasing),    // 非常快的扇动
            repeatMode = RepeatMode.Reverse
        ),
        label = "wingFlap"
    )

    // 精灵光晕脉冲
    val glowPulse by infiniteTransition.animateFloat(
        initialValue = 0.5f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glowPulse"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        fairies.forEach { fairy ->
            val t = animatedTime * fairy.speed

            // 8 字形轨迹（利萨茹曲线：x = sin(t), y = sin(2t)）
            val x = fairy.centerX * size.width + sin(t) * fairy.radiusX * size.width
            val y = fairy.centerY * size.height + sin(t * 2) * fairy.radiusY * size.height

            // 飞行方向（用于尾迹）
            val direction = cos(t)

            // 绘制增强版精灵
            drawEnhancedFairy(
                center = Offset(x, y),
                size = fairy.size,
                wingPhase = wingFlap,
                glowPhase = glowPulse,
                direction = direction
            )
        }
    }
}

/**
 * 绘制增强版精灵 - 更大、更明显
 */
private fun DrawScope.drawEnhancedFairy(
    center: Offset,
    size: Float,
    wingPhase: Float,
    glowPhase: Float,
    direction: Float
) {
    // === 外层光晕（最外层，最大） ===
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFFB6C1).copy(alpha = 0.4f * glowPhase),
                Color(0xFFFFB6C1).copy(alpha = 0.0f)
            ),
            center = center,
            radius = size * 3f
        ),
        radius = size * 3f,
        center = center
    )

    // === 内层光晕 ===
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(
                Color(0xFFFF69B4).copy(alpha = 0.6f * glowPhase),
                Color(0xFFFF69B4).copy(alpha = 0.0f)
            ),
            center = center,
            radius = size * 1.8f
        ),
        radius = size * 1.8f,
        center = center
    )

    // === 翅膀（根据扇动相位调整） ===
    val wingScale = 0.5f + wingPhase * 0.5f             // 0.5-1.0 缩放
    val wingColor = Color(0xFFE6E6FA).copy(alpha = 0.9f)
    val wingHighlight = Color.White.copy(alpha = 0.7f)

    // 左翅膀 - 上
    drawOval(
        color = wingColor,
        topLeft = Offset(
            center.x - size * 1.2f * wingScale - size * 0.15f,
            center.y - size * 0.5f
        ),
        size = androidx.compose.ui.geometry.Size(
            size * 1.0f * wingScale,
            size * 0.6f
        )
    )
    // 左翅膀 - 下
    drawOval(
        color = wingColor.copy(alpha = 0.7f),
        topLeft = Offset(
            center.x - size * 0.9f * wingScale - size * 0.1f,
            center.y
        ),
        size = androidx.compose.ui.geometry.Size(
            size * 0.7f * wingScale,
            size * 0.45f
        )
    )

    // 右翅膀 - 上
    drawOval(
        color = wingColor,
        topLeft = Offset(
            center.x + size * 0.15f,
            center.y - size * 0.5f
        ),
        size = androidx.compose.ui.geometry.Size(
            size * 1.0f * wingScale,
            size * 0.6f
        )
    )
    // 右翅膀 - 下
    drawOval(
        color = wingColor.copy(alpha = 0.7f),
        topLeft = Offset(
            center.x + size * 0.1f,
            center.y
        ),
        size = androidx.compose.ui.geometry.Size(
            size * 0.7f * wingScale,
            size * 0.45f
        )
    )

    // === 精灵身体（粉色圆） ===
    drawCircle(
        color = Color(0xFFFF69B4),
        radius = size * 0.35f,
        center = center
    )

    // 身体高光
    drawCircle(
        color = Color.White.copy(alpha = 0.4f),
        radius = size * 0.15f,
        center = Offset(center.x - size * 0.1f, center.y - size * 0.1f)
    )

    // === 精灵头部（肤色） ===
    drawCircle(
        color = Color(0xFFFFE4E1),
        radius = size * 0.22f,
        center = Offset(center.x, center.y - size * 0.4f)
    )

    // 头部腮红
    drawCircle(
        color = Color(0xFFFFB6C1).copy(alpha = 0.6f),
        radius = size * 0.08f,
        center = Offset(center.x - size * 0.12f, center.y - size * 0.32f)
    )
    drawCircle(
        color = Color(0xFFFFB6C1).copy(alpha = 0.6f),
        radius = size * 0.08f,
        center = Offset(center.x + size * 0.12f, center.y - size * 0.32f)
    )

    // === 精灵尾迹（魔法星尘） ===
    val trailAlpha = 0.5f + wingPhase * 0.3f

    // 尾迹 1
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = trailAlpha),
        radius = size * 0.18f,
        center = Offset(center.x - direction * size * 1.0f, center.y + size * 0.3f)
    )
    // 尾迹 2
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = trailAlpha * 0.7f),
        radius = size * 0.12f,
        center = Offset(center.x - direction * size * 1.5f, center.y + size * 0.5f)
    )
    // 尾迹 3
    drawCircle(
        color = Color(0xFFFFD700).copy(alpha = trailAlpha * 0.4f),
        radius = size * 0.08f,
        center = Offset(center.x - direction * size * 1.9f, center.y + size * 0.7f)
    )
}

// ==================== 闪光动画层（增强版） ====================

/**
 * 闪光特效层 - 更大更亮
 */
@Composable
private fun SparkleLayer() {
    val sparkles = remember {
        List(10) {                                       // 增加到 10 个
            Sparkle(
                x = Random.nextFloat(),
                y = Random.nextFloat() * 0.85f,
                delay = Random.nextInt(2000),
                duration = Random.nextInt(800) + 400
            )
        }
    }

    sparkles.forEach { sparkle ->
        SparkleAnimation(sparkle)
    }
}

/**
 * 单个闪光动画
 */
@Composable
private fun SparkleAnimation(sparkle: Sparkle) {
    val infiniteTransition = rememberInfiniteTransition(label = "sparkle_${sparkle.x}")

    val alpha by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500 + sparkle.delay
                0f at 0 using LinearEasing
                0f at sparkle.delay using LinearEasing
                1f at sparkle.delay + sparkle.duration / 2 using FastOutSlowInEasing
                0f at sparkle.delay + sparkle.duration using LinearEasing
                0f at 2500 + sparkle.delay using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleAlpha"
    )

    val scale by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1.5f,                              // 更大的缩放
        animationSpec = infiniteRepeatable(
            animation = keyframes {
                durationMillis = 2500 + sparkle.delay
                0.3f at 0 using LinearEasing
                0.3f at sparkle.delay using LinearEasing
                1.5f at sparkle.delay + sparkle.duration / 2 using FastOutSlowInEasing
                0.3f at sparkle.delay + sparkle.duration using LinearEasing
                0.3f at 2500 + sparkle.delay using LinearEasing
            },
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleScale"
    )

    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 90f,                               // 更大的旋转
        animationSpec = infiniteRepeatable(
            animation = tween(2500 + sparkle.delay, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "sparkleRotation"
    )

    Canvas(modifier = Modifier.fillMaxSize()) {
        if (alpha > 0.01f) {
            val x = sparkle.x * size.width
            val y = sparkle.y * size.height

            rotate(rotation, Offset(x, y)) {
                scale(scale, Offset(x, y)) {
                    // 十字闪光 - 更大
                    val sparkleSize = 25f
                    val color = Color.White.copy(alpha = alpha)

                    // 水平线
                    drawLine(
                        color = color,
                        start = Offset(x - sparkleSize, y),
                        end = Offset(x + sparkleSize, y),
                        strokeWidth = 3f
                    )

                    // 垂直线
                    drawLine(
                        color = color,
                        start = Offset(x, y - sparkleSize),
                        end = Offset(x, y + sparkleSize),
                        strokeWidth = 3f
                    )

                    // 45 度线
                    drawLine(
                        color = color.copy(alpha = alpha * 0.6f),
                        start = Offset(x - sparkleSize * 0.7f, y - sparkleSize * 0.7f),
                        end = Offset(x + sparkleSize * 0.7f, y + sparkleSize * 0.7f),
                        strokeWidth = 2f
                    )
                    drawLine(
                        color = color.copy(alpha = alpha * 0.6f),
                        start = Offset(x + sparkleSize * 0.7f, y - sparkleSize * 0.7f),
                        end = Offset(x - sparkleSize * 0.7f, y + sparkleSize * 0.7f),
                        strokeWidth = 2f
                    )

                    // 中心亮点 - 更大
                    drawCircle(
                        color = Color(0xFFFFD700).copy(alpha = alpha),
                        radius = 5f,
                        center = Offset(x, y)
                    )
                }
            }
        }
    }
}
