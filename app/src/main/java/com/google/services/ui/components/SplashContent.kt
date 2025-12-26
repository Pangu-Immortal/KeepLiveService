/**
 * ============================================================================
 * SplashContent.kt - Lottie 动画启动页组件
 * ============================================================================
 *
 * 功能简介：
 *   提供带 Lottie 动画效果的启动页内容
 *   包含精灵飞舞、星星闪烁、游戏风格进度条
 *
 * 动画效果：
 *   - 精灵 8 字形飞舞 + 翅膀扇动
 *   - 星星闪烁旋转动画
 *   - 粉紫渐变背景
 *   - 游戏风格加载进度条
 *
 * 配置说明：
 *   - SPLASH_DURATION_MS: 启动页显示时长（毫秒），统一配置点
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services.ui.components

import androidx.compose.animation.core.*
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.airbnb.lottie.compose.*
import com.google.services.R
import com.google.services.ui.theme.*

/** Splash 页面显示时长（毫秒）- 全局统一配置点 */
const val SPLASH_DURATION_MS = 15000

/**
 * Splash 页面内容组件
 *
 * @param modifier 修饰符
 * @param durationMs 显示时长（毫秒），默认使用 SPLASH_DURATION_MS
 * @param onAnimationEnd 动画结束回调（可选）
 */
@Composable
fun SplashContent(
    modifier: Modifier = Modifier,
    durationMs: Int = SPLASH_DURATION_MS,
    onAnimationEnd: (() -> Unit)? = null
) {
    // 精灵动画
    val fairyComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_fairy)
    )
    val fairyProgress by animateLottieCompositionAsState(
        composition = fairyComposition,
        iterations = LottieConstants.IterateForever,
        speed = 1.2f
    )

    // 星星动画
    val starsComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.sparkle_stars)
    )
    val starsProgress by animateLottieCompositionAsState(
        composition = starsComposition,
        iterations = LottieConstants.IterateForever,
        speed = 0.8f
    )

    // 进度条动画（0 -> 1，持续 durationMs 毫秒）
    var progress by remember { mutableFloatStateOf(0f) }
    val animatedProgress by animateFloatAsState(
        targetValue = progress,
        animationSpec = tween(durationMs, easing = LinearEasing),
        label = "progress"
    )

    // 启动进度动画
    LaunchedEffect(Unit) {
        progress = 1f
    }

    // ========== 绚丽动画效果 ==========
    val infiniteTransition = rememberInfiniteTransition(label = "splash")

    // 标题缩放动画 - 更大幅度
    val titleScale by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleScale"
    )

    // 标题发光透明度
    val titleGlow by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(800, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "titleGlow"
    )

    // 精灵浮动动画
    val fairyFloat by infiniteTransition.animateFloat(
        initialValue = -15f,
        targetValue = 15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fairyFloat"
    )

    // 精灵旋转摆动
    val fairyRotate by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fairyRotate"
    )

    // 精灵缩放脉冲
    val fairyPulse by infiniteTransition.animateFloat(
        initialValue = 0.95f,
        targetValue = 1.1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "fairyPulse"
    )

    // 副标题闪烁
    val subtitleAlpha by infiniteTransition.animateFloat(
        initialValue = 0.7f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "subtitleAlpha"
    )

    // 背景色彩渐变动画
    val bgColorShift by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(4000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bgColor"
    )

    // 进度条闪光动画
    val shimmerOffset by infiniteTransition.animateFloat(
        initialValue = -0.3f,
        targetValue = 1.3f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "shimmer"
    )

    // 彩虹光环旋转
    val rainbowRotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rainbow"
    )

    // 加载文字变化
    val loadingTexts = listOf(
        "正在施展魔法...",
        "召唤守护精灵...",
        "注入魔力能量...",
        "编织保护结界...",
        "激活守护技能...",
        "同步精灵状态...",
        "准备就绪..."
    )
    val textIndex = (animatedProgress * (loadingTexts.size - 1)).toInt()
        .coerceIn(0, loadingTexts.size - 1)

    // 动态背景颜色
    val bgPink = Color(0xFFFF6B9D).copy(red = 1f - bgColorShift * 0.1f)
    val bgPurple = Color(0xFFB39DDB).copy(blue = 0.86f + bgColorShift * 0.1f)
    val bgDeep = Color(0xFF9575CD).copy(red = 0.58f + bgColorShift * 0.1f)

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(bgPink, bgPurple, bgDeep)
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 旋转彩虹光环背景
        Box(
            modifier = Modifier
                .size(350.dp)
                .rotate(rainbowRotation)
                .scale(1f + bgColorShift * 0.1f)
                .background(
                    Brush.sweepGradient(
                        colors = listOf(
                            Color(0x40FF6B9D),
                            Color(0x40FFB6C1),
                            Color(0x40DDA0DD),
                            Color(0x40B39DDB),
                            Color(0x4087CEEB),
                            Color(0x4098FB98),
                            Color(0x40FFFACD),
                            Color(0x40FFB6C1),
                            Color(0x40FF6B9D)
                        )
                    ),
                    shape = CircleShape
                )
        )

        // 星星动画层 - 全屏
        LottieAnimation(
            composition = starsComposition,
            progress = { starsProgress },
            modifier = Modifier
                .fillMaxSize()
                .scale(1.1f + bgColorShift * 0.1f)  // 星星也有缩放动画
        )

        // 中心内容
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 32.dp)
        ) {
            Spacer(modifier = Modifier.weight(1f))

            // 精灵动画 - 增强浮动、旋转、脉冲效果
            Box(contentAlignment = Alignment.Center) {
                // 精灵光晕
                Box(
                    modifier = Modifier
                        .size(220.dp)
                        .scale(fairyPulse)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = titleGlow * 0.4f),
                                    Color(0xFFFFB6C1).copy(alpha = titleGlow * 0.2f),
                                    Color.Transparent
                                )
                            ),
                            shape = CircleShape
                        )
                )
                // 精灵本体
                LottieAnimation(
                    composition = fairyComposition,
                    progress = { fairyProgress },
                    modifier = Modifier
                        .size(200.dp)
                        .offset(y = fairyFloat.dp)
                        .rotate(fairyRotate)
                        .scale(fairyPulse)
                )
            }

            Spacer(modifier = Modifier.height(16.dp))

            // 标题 - 打字机效果
            TypewriterTitle(
                text = "守护精灵 🧚",
                titleScale = titleScale,
                titleGlow = titleGlow
            )

            Spacer(modifier = Modifier.height(8.dp))

            // 副标题 - 打字机效果（延迟显示）
            TypewriterSubtitle(
                text = "守护你的每一刻 ✨",
                subtitleAlpha = subtitleAlpha
            )

            Spacer(modifier = Modifier.weight(1f))

            // ========== 游戏风格进度条区域 ==========
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                modifier = Modifier.fillMaxWidth()
            ) {
                // 加载提示文字 - 带脉冲效果
                Text(
                    text = loadingTexts[textIndex],
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Medium,
                    color = Color.White,
                    modifier = Modifier.scale(0.95f + titleGlow * 0.08f)
                )

                Spacer(modifier = Modifier.height(12.dp))

                // 游戏风格进度条
                GameStyleProgressBar(
                    progress = animatedProgress,
                    shimmerOffset = shimmerOffset,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(24.dp)
                )

                Spacer(modifier = Modifier.height(8.dp))

                // 百分比显示
                Text(
                    text = "${(animatedProgress * 100).toInt()}%",
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
            }

            Spacer(modifier = Modifier.height(48.dp))
        }
    }
}

/**
 * 游戏风格进度条
 *
 * 参考经典游戏加载条设计：
 * - 外层金色/橙色边框
 * - 内层深色背景
 * - 渐变填充 + 分段效果
 * - 流动光效
 * - 精灵装饰
 */
@Composable
fun GameStyleProgressBar(
    progress: Float,
    shimmerOffset: Float,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(14.dp))
            .border(
                width = 3.dp,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFFE082),  // 浅金
                        Color(0xFFFFB300),  // 金色
                        Color(0xFFFF8F00),  // 深橙
                        Color(0xFFFFB300)   // 金色
                    )
                ),
                shape = RoundedCornerShape(14.dp)
            )
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFF2D1B4E),  // 深紫
                        Color(0xFF1A0F2E)   // 更深紫
                    )
                ),
                RoundedCornerShape(14.dp)
            )
            .padding(4.dp),
        contentAlignment = Alignment.CenterStart
    ) {
        // 内层进度条容器
        Box(
            modifier = Modifier
                .fillMaxSize()
                .clip(RoundedCornerShape(10.dp))
                .background(Color(0xFF0D0620))  // 深色背景
        ) {
            Canvas(modifier = Modifier.fillMaxSize()) {
                val width = size.width
                val height = size.height
                val progressWidth = (width * progress).coerceAtLeast(0f)
                val cornerRadius = 8.dp.toPx()

                // 进度条渐变填充
                if (progress > 0.01f && progressWidth > 1f) {
                    // 主填充 - 粉紫渐变
                    drawRoundRect(
                        brush = Brush.horizontalGradient(
                            colors = listOf(
                                Color(0xFFE040FB),  // 亮紫
                                Color(0xFFFF4081),  // 粉红
                                Color(0xFFFF6EC7),  // 亮粉
                                Color(0xFFFFAB40)   // 橙黄
                            )
                        ),
                        size = Size(progressWidth, height),
                        cornerRadius = CornerRadius(cornerRadius)
                    )

                    // 顶部高光条
                    drawRoundRect(
                        brush = Brush.verticalGradient(
                            colors = listOf(
                                Color.White.copy(alpha = 0.5f),
                                Color.Transparent
                            )
                        ),
                        topLeft = Offset(0f, 0f),
                        size = Size(progressWidth, height * 0.4f),
                        cornerRadius = CornerRadius(cornerRadius)
                    )

                    // 流动光效 - 安全计算
                    val shimmerWidth = 80f
                    val maxStart = (progressWidth - shimmerWidth).coerceAtLeast(0f)
                    if (maxStart > 0f) {
                        val shimmerStart = (shimmerOffset * (progressWidth + shimmerWidth) - shimmerWidth)
                            .coerceIn(0f, maxStart)
                        val shimmerEnd = (shimmerStart + shimmerWidth).coerceAtMost(progressWidth)

                        if (shimmerEnd > shimmerStart) {
                            drawRect(
                                brush = Brush.horizontalGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.White.copy(alpha = 0.7f),
                                        Color.Transparent
                                    ),
                                    startX = shimmerStart,
                                    endX = shimmerEnd
                                ),
                                topLeft = Offset(shimmerStart, 0f),
                                size = Size(shimmerEnd - shimmerStart, height)
                            )
                        }
                    }
                }

                // 分段标记线（每 20%）- 像游戏格子
                for (i in 1..4) {
                    val x = width * i / 5f
                    drawLine(
                        color = Color.White.copy(alpha = 0.15f),
                        start = Offset(x, 2f),
                        end = Offset(x, height - 2f),
                        strokeWidth = 2f
                    )
                }
            }
        }

        // 进度条头部精灵图标
        if (progress > 0.05f) {
            Box(
                modifier = Modifier
                    .fillMaxWidth(progress.coerceIn(0.08f, 1f))
                    .fillMaxHeight(),
                contentAlignment = Alignment.CenterEnd
            ) {
                Text(
                    text = "🧚",
                    fontSize = 18.sp
                )
            }
        }
    }
}

/**
 * 简化版 Splash（仅用于 Activity 启动过渡）
 */
@Composable
fun SimpleSplash(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Color(0xFFFF6B9D),
                        Color(0xFFB39DDB),
                        Color(0xFF9575CD)
                    )
                )
            ),
        contentAlignment = Alignment.Center
    ) {
        // 只显示星星动画
        val composition by rememberLottieComposition(
            LottieCompositionSpec.RawRes(R.raw.sparkle_stars)
        )
        val progress by animateLottieCompositionAsState(
            composition = composition,
            iterations = LottieConstants.IterateForever
        )

        LottieAnimation(
            composition = composition,
            progress = { progress },
            modifier = Modifier.fillMaxSize()
        )
    }
}

// ==================== 打字机效果组件 ====================

/**
 * 将字符串拆分为图形单元列表（正确处理 emoji）
 *
 * 使用 Java 的 BreakIterator 来正确识别 grapheme clusters，
 * 确保多码点 emoji（如 🧚）作为一个整体处理
 */
private fun String.toGraphemes(): List<String> {
    val result = mutableListOf<String>()
    val iterator = java.text.BreakIterator.getCharacterInstance()
    iterator.setText(this)
    var start = 0
    var end = iterator.next()
    while (end != java.text.BreakIterator.DONE) {
        result.add(this.substring(start, end))
        start = end
        end = iterator.next()
    }
    return result
}

/**
 * 打字机效果标题
 *
 * 逐字显示文本，带光标闪烁效果
 * 使用 grapheme clusters 正确处理 emoji
 */
@Composable
fun TypewriterTitle(
    text: String,
    titleScale: Float,
    titleGlow: Float,
    modifier: Modifier = Modifier
) {
    // 将文本拆分为图形单元（正确处理 emoji）
    val graphemes = remember(text) { text.toGraphemes() }
    val totalGraphemes = graphemes.size

    // 打字机动画状态
    var displayedCount by remember { mutableIntStateOf(0) }

    // 打字机效果：每100ms显示一个图形单元
    LaunchedEffect(text) {
        displayedCount = 0
        for (i in 1..totalGraphemes) {
            kotlinx.coroutines.delay(100)
            displayedCount = i
        }
    }

    // 光标闪烁动画
    val infiniteTransition = rememberInfiniteTransition(label = "cursor")
    val cursorAlpha by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 0f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "cursorAlpha"
    )

    // 每个字符的跳动动画
    val charBounce by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(600, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "charBounce"
    )

    val displayedGraphemes = graphemes.take(displayedCount)
    val showCursor = displayedCount < totalGraphemes

    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier
    ) {
        // 发光阴影层
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .scale(titleScale * 1.05f)
                .offset(y = 2.dp)
        ) {
            displayedGraphemes.forEachIndexed { index, grapheme ->
                val bounce = if (index == displayedCount - 1 && displayedCount > 0) {
                    charBounce * 4f  // 最后一个字符跳动更明显
                } else {
                    0f
                }
                Text(
                    text = grapheme,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color.White.copy(alpha = titleGlow * 0.4f),
                    modifier = Modifier.offset(y = (-bounce).dp)
                )
            }
        }

        // 主标题层
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.scale(titleScale)
        ) {
            displayedGraphemes.forEachIndexed { index, grapheme ->
                val bounce = if (index == displayedCount - 1 && displayedCount > 0) {
                    charBounce * 4f
                } else {
                    0f
                }
                // 每个图形单元带渐变色（emoji 显示为白色）
                val isEmoji = grapheme.length > 1 || grapheme.firstOrNull()?.code?.let { it > 0x1000 } == true
                val charColor = when {
                    isEmoji -> Color.White
                    index % 3 == 0 -> Color(0xFFFFB6C1)  // 浅粉
                    index % 3 == 1 -> Color.White
                    else -> Color(0xFFE6E6FA)           // 薰衣草
                }
                Text(
                    text = grapheme,
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = charColor,
                    modifier = Modifier.offset(y = (-bounce).dp)
                )
            }
            // 光标
            if (showCursor) {
                Text(
                    text = "│",
                    fontSize = 32.sp,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFFFFD700).copy(alpha = cursorAlpha)
                )
            }
        }
    }
}

/**
 * 打字机效果副标题
 *
 * 延迟后逐字显示，带跳动动画效果
 * 使用 grapheme clusters 正确处理 emoji
 * 紫色系配色，更清晰醒目
 */
@Composable
fun TypewriterSubtitle(
    text: String,
    subtitleAlpha: Float,
    modifier: Modifier = Modifier
) {
    // 将文本拆分为图形单元（正确处理 emoji）
    val graphemes = remember(text) { text.toGraphemes() }
    val totalGraphemes = graphemes.size

    // 延迟开始打字
    var startTyping by remember { mutableStateOf(false) }
    var displayedCount by remember { mutableIntStateOf(0) }

    // 延迟 1.5 秒后开始副标题打字
    LaunchedEffect(Unit) {
        kotlinx.coroutines.delay(1500)
        startTyping = true
    }

    // 打字机效果
    LaunchedEffect(startTyping) {
        if (startTyping) {
            for (i in 1..totalGraphemes) {
                kotlinx.coroutines.delay(80)
                displayedCount = i
            }
        }
    }

    val displayedGraphemes = graphemes.take(displayedCount)

    // 跳动动画 - 波浪式，每个字符有不同相位
    val infiniteTransition = rememberInfiniteTransition(label = "subtitleBounce")

    // 基础跳动动画周期
    val bouncePhase by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 2f * Math.PI.toFloat(),
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "bouncePhase"
    )

    // 紫色系配色
    val purpleColors = listOf(
        Color(0xFFE1BEE7),  // 浅紫
        Color(0xFFCE93D8),  // 淡紫
        Color(0xFFBA68C8),  // 紫色
        Color(0xFFAB47BC),  // 深紫
        Color(0xFFFFD54F),  // 星星用金色
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = modifier
    ) {
        displayedGraphemes.forEachIndexed { index, grapheme ->
            // 每个字符有不同的跳动相位，形成波浪效果
            val charPhase = bouncePhase + (index * 0.5f)
            val bounce = kotlin.math.sin(charPhase) * 4f  // 跳动幅度 4dp

            // 判断是否为 emoji
            val isEmoji = grapheme.length > 1 || grapheme.firstOrNull()?.code?.let { it > 0x1000 } == true

            // 紫色系渐变配色（emoji 用金色）
            val charColor = when {
                isEmoji -> purpleColors[4]  // 金色闪亮
                else -> purpleColors[index % 4]  // 紫色系循环
            }

            Text(
                text = grapheme,
                fontSize = 18.sp,
                fontWeight = FontWeight.SemiBold,
                color = charColor,
                modifier = Modifier.offset(y = (-bounce).dp)
            )
        }
    }
}
