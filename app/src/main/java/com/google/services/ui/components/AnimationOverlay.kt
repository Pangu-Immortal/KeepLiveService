/**
 * ============================================================================
 * AnimationOverlay.kt - Lottie 动画叠加层组件
 * ============================================================================
 *
 * 功能简介：
 *   提供不影响触摸手势的 Lottie 动画叠加层
 *   在屏幕边缘和顶部显示跑来跑去的卡通动画
 *
 * 动画效果：
 *   - 顶部横向奔跑的星星（带尾迹）
 *   - 左右边缘纵向奔跑的卡通元素
 *   - 顶部跳跃的吉祥物（兔子、猫咪、小鸡）
 *
 * 特点：
 *   - 不拦截触摸事件（pointerInput 穿透）
 *   - 半透明显示，不遮挡内容
 *   - 无限循环播放
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.airbnb.lottie.compose.*
import com.google.services.R

/**
 * 全屏动画叠加层
 *
 * 在屏幕边缘显示各种卡通动画，不影响用户操作
 *
 * @param modifier 修饰符
 * @param showTopRunner 是否显示顶部奔跑动画
 * @param showEdgeRunners 是否显示边缘奔跑动画
 * @param showBouncingMascots 是否显示跳跃吉祥物
 */
@Composable
fun AnimationOverlay(
    modifier: Modifier = Modifier,
    showTopRunner: Boolean = true,
    showEdgeRunners: Boolean = true,
    showBouncingMascots: Boolean = true
) {
    Box(modifier = modifier.fillMaxSize()) {
        // 顶部奔跑的星星（在 TopBar 下方）
        if (showTopRunner) {
            TopRunnerAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(60.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 120.dp)  // TopBar 下方
            )
        }

        // 左边缘下落动画
        if (showEdgeRunners) {
            EdgeRunnerAnimation(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterStart),
                isLeft = true
            )
        }

        // 右边缘下落动画
        if (showEdgeRunners) {
            EdgeRunnerAnimation(
                modifier = Modifier
                    .width(50.dp)
                    .fillMaxHeight()
                    .align(Alignment.CenterEnd),
                isLeft = false
            )
        }

        // 跳跃的吉祥物（顶部空白区域）
        if (showBouncingMascots) {
            BouncingMascotsAnimation(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(100.dp)
                    .align(Alignment.TopCenter)
                    .offset(y = 180.dp)
            )
        }
    }
}

/**
 * 顶部奔跑星星动画
 *
 * 星星从左到右快速奔跑，带有粉紫色尾迹
 */
@Composable
fun TopRunnerAnimation(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.running_star)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.8f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

/**
 * 边缘下落动画
 *
 * 卡通元素沿屏幕边缘从上往下滑动
 *
 * @param isLeft 是否在左边缘（决定动画方向）
 */
@Composable
fun EdgeRunnerAnimation(
    modifier: Modifier = Modifier,
    isLeft: Boolean
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.edge_runner)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = if (isLeft) 0.6f else 0.8f  // 左右速度略有不同
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

/**
 * 跳跃吉祥物动画
 *
 * 可爱的小兔子、猫咪、小鸡在空白区域跳跃
 */
@Composable
fun BouncingMascotsAnimation(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.bouncing_mascots)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.7f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

/**
 * 简化版顶部动画装饰
 *
 * 仅在顶部 Title 区域添加跳跃吉祥物
 * 适用于不需要全屏动画的场景
 *
 * @param modifier 修饰符
 */
@Composable
fun TopBarDecoration(
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceEvenly
    ) {
        // 单独的弹跳吉祥物
        BouncingMascotsAnimation(
            modifier = Modifier
                .width(150.dp)
                .height(80.dp)
        )
    }
}

/**
 * 浮动精灵动画
 *
 * 在指定位置显示精灵飞舞效果
 * 可用于卡片装饰
 *
 * @param modifier 修饰符
 */
@Composable
fun FloatingFairyDecoration(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.splash_fairy)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 1.0f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}

/**
 * 闪烁星星装饰
 *
 * 在指定位置显示闪烁星星效果
 * 可用于标题或按钮装饰
 *
 * @param modifier 修饰符
 */
@Composable
fun SparklingStarsDecoration(
    modifier: Modifier = Modifier
) {
    val composition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.sparkle_stars)
    )
    val progress by animateLottieCompositionAsState(
        composition = composition,
        iterations = LottieConstants.IterateForever,
        speed = 0.6f
    )

    LottieAnimation(
        composition = composition,
        progress = { progress },
        modifier = modifier
    )
}
