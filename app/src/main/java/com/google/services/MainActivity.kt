/**
 * ============================================================================
 * MainActivity.kt - 主界面 Activity
 * ============================================================================
 *
 * 功能简介：
 *   应用主界面，展示守护精灵控制面板
 *   包含权限管理、服务控制、策略展示等功能
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */
package com.google.services

import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.*
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.outlined.*
import androidx.compose.material.icons.rounded.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.google.services.ui.components.AnimationOverlay
import com.google.services.ui.components.BouncingMascotsAnimation
import com.google.services.ui.components.CartoonBackground
import com.google.services.ui.components.SparklingStarsDecoration
import com.google.services.ui.theme.*
import com.google.services.util.PermissionHelper
import com.service.framework.Fw
import com.service.framework.strategy.AutoStartPermissionManager
import com.service.framework.strategy.BatteryOptimizationManager
import com.service.framework.util.DeviceUtils

class MainActivity : ComponentActivity() {

    private val permissionHelper by lazy { PermissionHelper(this) }

    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        handlePermissionResult(permissions)
    }

    private var refreshTrigger = mutableStateOf(0)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        setContent {
            KeepLiveServiceTheme {
                MainScreen(
                    refreshTrigger = refreshTrigger.value,
                    onStartService = ::startService,
                    onStopService = ::stopService,
                    onRequestPermissions = ::requestNecessaryPermissions,
                    onCheckService = ::checkService,
                    onRequestBatteryOptimization = ::requestBatteryOptimization,
                    onOpenAutoStartSettings = ::openAutoStartSettings,
                    onOpenOverlaySettings = ::openOverlaySettings,
                    onOpenAppSettings = ::openAppSettings
                )
            }
        }

        requestNecessaryPermissions()
    }

    override fun onResume() {
        super.onResume()
        refreshTrigger.value++
    }

    private fun requestNecessaryPermissions() {
        val permissionsToRequest = permissionHelper.getRequiredPermissions()
            .filter { !permissionHelper.hasPermission(it) }

        if (permissionsToRequest.isNotEmpty()) {
            permissionLauncher.launch(permissionsToRequest.toTypedArray())
        }
    }

    private fun handlePermissionResult(permissions: Map<String, Boolean>) {
        val allGranted = permissions.values.all { it }
        refreshTrigger.value++

        if (allGranted) {
            Toast.makeText(this, "技能get~ 你太棒啦 ✨", Toast.LENGTH_SHORT).show()
        } else {
            Toast.makeText(this, "呜呜有技能没学会 ┭┮﹏┭┮", Toast.LENGTH_LONG).show()
        }
    }

    private fun requestBatteryOptimization() {
        if (BatteryOptimizationManager.isIgnoringBatteryOptimizations(this)) {
            Toast.makeText(this, "电池buff已激活啦~ 🔋✨", Toast.LENGTH_SHORT).show()
        } else {
            BatteryOptimizationManager.requestIgnoreBatteryOptimizations(this)
        }
    }

    private fun openAutoStartSettings() {
        val success = AutoStartPermissionManager.openAutoStartSettings(this)
        if (success) {
            Toast.makeText(this, "找到啦~ 快去打开开关叭 🎯", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(this, "呜呜找不到入口 (´;ω;`)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun openOverlaySettings() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            if (Settings.canDrawOverlays(this)) {
                Toast.makeText(this, "悬浮窗技能已解锁~ 🎈✨", Toast.LENGTH_SHORT).show()
            } else {
                try {
                    startActivity(Intent(
                        Settings.ACTION_MANAGE_OVERLAY_PERMISSION,
                        Uri.parse("package:$packageName")
                    ))
                } catch (e: Exception) {
                    Toast.makeText(this, "呜呜打不开 (´;ω;`)", Toast.LENGTH_SHORT).show()
                }
            }
        }
    }

    private fun openAppSettings() {
        try {
            startActivity(Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS).apply {
                data = Uri.parse("package:$packageName")
            })
        } catch (e: Exception) {
            Toast.makeText(this, "呜呜打不开 (´;ω;`)", Toast.LENGTH_SHORT).show()
        }
    }

    private fun startService() {
        try {
            Fw.check()
            Toast.makeText(this, "小守护出动啦~ ٩(๑❛ᴗ❛๑)۶", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "呜呜启动失败了 ┭┮﹏┭┮", Toast.LENGTH_LONG).show()
        }
    }

    private fun stopService() {
        try {
            Fw.stop()
            Toast.makeText(this, "小守护去睡觉觉啦~ 💤", Toast.LENGTH_SHORT).show()
        } catch (e: Exception) {
            Toast.makeText(this, "停不下来啦 (´;ω;`)", Toast.LENGTH_LONG).show()
        }
    }

    private fun checkService() {
        val isInitialized = Fw.isInitialized()
        val message = if (isInitialized) "小守护正在努力工作ing~ 💪✨" else "小守护还在睡觉觉 💤"
        Toast.makeText(this, message, Toast.LENGTH_SHORT).show()
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    refreshTrigger: Int,
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onRequestPermissions: () -> Unit,
    onCheckService: () -> Unit,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    val context = LocalContext.current
    val scrollState = rememberScrollState()

    val isBatteryOptimized = remember(refreshTrigger) {
        BatteryOptimizationManager.isIgnoringBatteryOptimizations(context)
    }
    val hasOverlayPermission = remember(refreshTrigger) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            Settings.canDrawOverlays(context)
        } else true
    }
    val isFrameworkInitialized = remember(refreshTrigger) {
        Fw.isInitialized()
    }
    val manufacturer = remember { DeviceUtils.getManufacturer() }

    // 使用 DeviceUtils 工具类判断是否为需要额外权限的特殊机型
    val isSpecialVendor = remember { DeviceUtils.isSpecialVendor() }

    Scaffold(
        topBar = {
            // 可爱的渐变色顶部 - 带动画效果
            AnimatedTopBar()
        },
        containerColor = Color.Transparent
    ) { innerPadding ->
        // 最外层 Box - 用于叠加动画层
        Box(modifier = Modifier.fillMaxSize()) {
            // 使用卡通动画背景替代静态渐变
            CartoonBackground(
                modifier = Modifier.fillMaxSize(),
                showStars = true,
                showClouds = true,
                showBubbles = true,
                showFairies = true,
                showSparkles = true
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(scrollState)
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    // 带动画的状态卡片 - 传入机型信息用于动态判断
                    AnimatedStatusCard(
                        isBatteryOptimized = isBatteryOptimized,
                        hasOverlayPermission = hasOverlayPermission,
                        isFrameworkInitialized = isFrameworkInitialized,
                        isSpecialVendor = isSpecialVendor
                    )

                    PermissionSection(
                        isBatteryOptimized = isBatteryOptimized,
                        hasOverlayPermission = hasOverlayPermission,
                        manufacturer = manufacturer,
                        onRequestBatteryOptimization = onRequestBatteryOptimization,
                        onOpenAutoStartSettings = onOpenAutoStartSettings,
                        onOpenOverlaySettings = onOpenOverlaySettings,
                        onRequestPermissions = onRequestPermissions
                    )

                    // 带动画的操控台
                    AnimatedServiceControlSection(
                        onStartService = onStartService,
                        onStopService = onStopService,
                        onCheckService = onCheckService,
                        onOpenAppSettings = onOpenAppSettings
                    )

                    StrategyInfoSection()

                    Spacer(modifier = Modifier.height(8.dp))
                }
            }

            // 最上层动画叠加 - 不影响触摸
            AnimationOverlay(
                modifier = Modifier.fillMaxSize(),
                showTopRunner = true,
                showEdgeRunners = true,
                showBouncingMascots = true
            )
        }
    }
}

@Composable
fun PermissionSection(
    isBatteryOptimized: Boolean,
    hasOverlayPermission: Boolean,
    manufacturer: String,
    onRequestBatteryOptimization: () -> Unit,
    onOpenAutoStartSettings: () -> Unit,
    onOpenOverlaySettings: () -> Unit,
    onRequestPermissions: () -> Unit
) {
    // 使用 DeviceUtils 工具类判断机型
    val isSpecialVendor = DeviceUtils.isSpecialVendor()
    val isHuaweiVendor = DeviceUtils.isHuaweiVendor()

    // 简洁卡片
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Security,
                    contentDescription = null,
                    tint = Pink40
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "技能树 🌳",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PinkDeep
                )
            }

            // 电池护盾 - 仅小米/vivo/OPPO 显示
            if (isSpecialVendor) {
                PermissionItem(
                    icon = Icons.Outlined.BatteryChargingFull,
                    title = "电池护盾 🔋",
                    subtitle = if (isBatteryOptimized) "已点亮~" else "戳我点亮~",
                    isGranted = isBatteryOptimized,
                    onClick = onRequestBatteryOptimization
                )
            }

            // 自启动魔法 - 仅小米/vivo/OPPO 显示
            if (isSpecialVendor) {
                PermissionItem(
                    icon = Icons.Outlined.RocketLaunch,
                    title = "自启动魔法 🚀",
                    subtitle = "需要手动点亮哦~",
                    isGranted = null,
                    onClick = onOpenAutoStartSettings
                )
            }

            // 悬浮窗特权 - 仅小米/vivo/OPPO 显示
            if (isSpecialVendor) {
                PermissionItem(
                    icon = Icons.Outlined.Layers,
                    title = "悬浮窗特权 🎈",
                    subtitle = if (hasOverlayPermission) "已点亮~" else "戳我点亮~",
                    isGranted = hasOverlayPermission,
                    onClick = onOpenOverlaySettings
                )
            }

            // 消息铃铛 - 所有机型都显示
            PermissionItem(
                icon = Icons.Outlined.Notifications,
                title = "消息铃铛 🔔",
                subtitle = "蓝牙、通知啥的~",
                isGranted = null,
                onClick = onRequestPermissions
            )

            // 厂商提示 - 使用 DeviceUtils 工具类判断
            if (DeviceUtils.isXiaomiVendor()) {
                VendorTipCard(
                    title = "小米/红米攻略 📱",
                    tips = listOf(
                        "安全中心 → 自启动 → 打开开关叭",
                        "设置 → 电池 → 选择无限制呀",
                        "最近任务往下滑 → 锁住小精灵"
                    )
                )
            }

            if (DeviceUtils.isHuaweiVendor()) {
                VendorTipCard(
                    title = "华为/荣耀攻略 📱",
                    tips = listOf(
                        "手机管家 → 启动管理 → 允许叭",
                        "电池 → 保持网络连接哦",
                        "最近任务往下滑 → 锁住小精灵"
                    )
                )
            }

            if (DeviceUtils.isOppoVendor() || DeviceUtils.isVivoVendor()) {
                VendorTipCard(
                    title = "OPPO/vivo/一加攻略 📱",
                    tips = listOf(
                        "电池 → 允许后台运行叭",
                        "手机管家 → 自启动 → 打开呀",
                        "最近任务往下滑 → 锁住小精灵"
                    )
                )
            }
        }
    }
}

@Composable
fun PermissionItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    isGranted: Boolean?,
    onClick: () -> Unit
) {
    // 状态颜色：统一使用粉绿配色，null 状态使用蓝色表示"待设置"
    val statusColor = when (isGranted) {
        true -> Mint40                    // 已授权 - 薄荷绿
        false -> RosePink                 // 未授权 - 玫瑰粉
        null -> Color(0xFF64B5F6)         // 待设置 - 天蓝色（友好提示）
    }

    // 简洁按钮样式
    Surface(
        onClick = onClick,
        shape = RoundedCornerShape(16.dp),
        color = PinkLight80.copy(alpha = 0.6f),
        tonalElevation = 2.dp
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Pink40.copy(alpha = 0.15f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Pink40,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium,
                    color = Color(0xFF333333)
                )
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF888888)
                )
            }

            // 右侧状态图标 - 统一圆形风格
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .clip(CircleShape)
                    .background(statusColor.copy(alpha = 0.2f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = when (isGranted) {
                        true -> Icons.Default.Check           // ✓ 已授权
                        false -> Icons.Default.Close          // ✕ 未授权
                        null -> Icons.AutoMirrored.Filled.ArrowForward  // → 去设置（友好提示）
                    },
                    contentDescription = null,
                    tint = statusColor,
                    modifier = Modifier.size(16.dp)
                )
            }
        }
    }
}

@Composable
fun VendorTipCard(title: String, tips: List<String>) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = SoftPurple.copy(alpha = 0.5f)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Lightbulb,
                    contentDescription = null,
                    tint = Lavender40,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B5B95)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            tips.forEachIndexed { index, tip ->
                Text(
                    text = "${index + 1}. $tip",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color(0xFF6B5B95).copy(alpha = 0.9f)
                )
            }
        }
    }
}

@Composable
fun StrategyInfoSection() {
    // 简洁卡片 - 无阴影避免边框感
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Lavender80.copy(alpha = 0.9f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(20.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Outlined.Checklist,
                    contentDescription = null,
                    tint = Lavender40
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "技能图鉴 📖",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = Color(0xFF6B5B95)
                )
            }

            Spacer(modifier = Modifier.height(12.dp))

            // 核心保活策略列表
            val strategies = listOf(
                // 核心服务类
                "前台服务 + MediaSession" to "核心魔法 🎵",
                "双进程守护" to "影分身术 👯",
                "Native 守护进程" to "底层结界 🔮",
                // 系统服务类
                "无障碍服务保活" to "无敌护盾 ♿",
                "通知监听服务" to "消息雷达 📡",
                "账户同步机制" to "系统通行证 🎫",
                // 唤醒策略类
                "定时任务调度" to "闹钟精灵 ⏰",
                "蓝牙广播监听" to "唤醒咒语 📶",
                "系统广播监听" to "开机守卫 🌅",
                // 特殊策略类
                "无法强制停止" to "金钟罩 🛡️",
                "1像素 Activity" to "隐身斗篷 👻",
                "悬浮窗保活" to "小窗精灵 🎈",
                "进程优先级提升" to "VIP通道 ⚡"
            )

            strategies.forEach { (name, desc) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(6.dp)
                                .clip(CircleShape)
                                .background(SakuraPink)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = name,
                            style = MaterialTheme.typography.bodyMedium,
                            color = Color(0xFF555555)
                        )
                    }
                    Text(
                        text = desc,
                        style = MaterialTheme.typography.bodySmall,
                        color = Lavender40
                    )
                }
            }
        }
    }
}

// ==================== 动画组件 ====================

/**
 * 动画顶部栏
 * Logo 有轻微的漂浮动画，标题有呼吸效果
 */
@Composable
fun AnimatedTopBar() {
    // Logo 漂浮动画
    val infiniteTransition = rememberInfiniteTransition(label = "topBar")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float"
    )

    // 光晕脉冲
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.3f,
        targetValue = 0.6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "glow"
    )

    Box(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                Brush.verticalGradient(
                    colors = listOf(
                        Pink40,
                        SakuraPink
                    )
                )
            )
            .statusBarsPadding()
            .padding(horizontal = 20.dp, vertical = 24.dp)
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            // Logo图标 - 带漂浮动画和光晕
            Box(contentAlignment = Alignment.Center) {
                // 光晕效果
                Box(
                    modifier = Modifier
                        .size(64.dp)
                        .offset(y = (-floatOffset).dp)
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    Color.White.copy(alpha = glowAlpha),
                                    Color.Transparent
                                )
                            ),
                            CircleShape
                        )
                )
                // Logo
                Image(
                    painter = painterResource(id = com.service.framework.R.drawable.ic_account),
                    contentDescription = "Logo",
                    modifier = Modifier
                        .size(56.dp)
                        .offset(y = (-floatOffset).dp)
                )
            }

            Spacer(modifier = Modifier.width(8.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = "守护精灵 🧚",
                    style = MaterialTheme.typography.headlineSmall,
                    fontWeight = FontWeight.Bold,
                    color = Color.White
                )
                Spacer(modifier = Modifier.height(2.dp))
                Text(
                    text = "${Build.MANUFACTURER} ${Build.MODEL}",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White.copy(alpha = 0.85f)
                )
            }

            // 右侧闪烁星星装饰
            SparklingStarsDecoration(
                modifier = Modifier.size(60.dp)
            )
        }
    }
}

/**
 * 带动画的状态卡片
 * 图标有脉冲效果，进入时有弹性动画
 *
 * @param isBatteryOptimized 电池优化是否已忽略
 * @param hasOverlayPermission 悬浮窗权限是否已授权
 * @param isFrameworkInitialized 框架是否已初始化
 * @param isSpecialVendor 是否为特殊机型（小米/vivo/OPPO 等需要额外权限的机型）
 */
@Composable
fun AnimatedStatusCard(
    isBatteryOptimized: Boolean,
    hasOverlayPermission: Boolean,
    isFrameworkInitialized: Boolean,
    isSpecialVendor: Boolean = false
) {
    // 根据机型动态判断"满血状态"条件
    // 特殊机型：需要电池优化 + 悬浮窗权限 + 框架初始化
    // 普通机型：只需要框架初始化即可
    val allGood = if (isSpecialVendor) {
        isBatteryOptimized && hasOverlayPermission && isFrameworkInitialized
    } else {
        isFrameworkInitialized
    }

    // 图标脉冲动画
    val infiniteTransition = rememberInfiniteTransition(label = "status")
    val iconScale by infiniteTransition.animateFloat(
        initialValue = 1f,
        targetValue = 1.15f,
        animationSpec = infiniteRepeatable(
            animation = tween(1000, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "iconPulse"
    )

    // 光晕旋转
    val rotation by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 360f,
        animationSpec = infiniteRepeatable(
            animation = tween(8000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "rotation"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (allGood)
                Color(0xFFE8F5E9).copy(alpha = 0.95f)
            else
                Color(0xFFFCE4EC).copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(56.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(
                        if (allGood) Mint40.copy(alpha = 0.2f)
                        else RosePink.copy(alpha = 0.2f)
                    ),
                contentAlignment = Alignment.Center
            ) {
                // 旋转光晕（成功时显示）
                if (allGood) {
                    Box(
                        modifier = Modifier
                            .size(48.dp)
                            .rotate(rotation)
                            .background(
                                Brush.sweepGradient(
                                    colors = listOf(
                                        Mint40.copy(alpha = 0.3f),
                                        Color.Transparent,
                                        Mint40.copy(alpha = 0.3f),
                                        Color.Transparent
                                    )
                                ),
                                CircleShape
                            )
                    )
                }
                Icon(
                    imageVector = if (allGood) Icons.Rounded.Verified else Icons.Rounded.ErrorOutline,
                    contentDescription = null,
                    tint = if (allGood) Mint40 else RosePink,
                    modifier = Modifier
                        .size(32.dp)
                        .scale(iconScale)
                )
            }

            Spacer(modifier = Modifier.width(16.dp))

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = if (allGood) "满血状态~ ✨" else "还差一点点~",
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold,
                    color = if (allGood) Color(0xFF2E7D5A) else PinkDeep
                )
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = if (allGood) "所有buff已就位，冲鸭~"
                    else "完成下面的任务，解锁全技能~",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color(0xFF666666)
                )
            }
        }
    }
}

/**
 * 带动画的操控台
 * 按钮有悬浮和发光效果
 */
@Composable
fun AnimatedServiceControlSection(
    onStartService: () -> Unit,
    onStopService: () -> Unit,
    onCheckService: () -> Unit,
    onOpenAppSettings: () -> Unit
) {
    // 主按钮发光动画
    val infiniteTransition = rememberInfiniteTransition(label = "control")
    val glowAlpha by infiniteTransition.animateFloat(
        initialValue = 0.4f,
        targetValue = 0.8f,
        animationSpec = infiniteRepeatable(
            animation = tween(1200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "buttonGlow"
    )

    // 火箭摇摆动画
    val rocketRotation by infiniteTransition.animateFloat(
        initialValue = -5f,
        targetValue = 5f,
        animationSpec = infiniteRepeatable(
            animation = tween(500, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "rocketWobble"
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color.White.copy(alpha = 0.95f)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                // 游戏手柄图标 - 带轻微旋转
                Icon(
                    imageVector = Icons.Outlined.SportsEsports,
                    contentDescription = null,
                    tint = Pink40,
                    modifier = Modifier.rotate(rocketRotation / 2)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = "操控台 🎮",
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.Bold,
                    color = PinkDeep
                )
            }

            // 按钮行
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 出发按钮 - 带发光效果
                Box(
                    modifier = Modifier.weight(1f)
                ) {
                    // 发光背景
                    Box(
                        modifier = Modifier
                            .matchParentSize()
                            .offset(y = 2.dp)
                            .background(
                                Pink40.copy(alpha = glowAlpha * 0.3f),
                                RoundedCornerShape(14.dp)
                            )
                    )
                    Button(
                        onClick = onStartService,
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(14.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = Pink40),
                        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Rounded.RocketLaunch,
                            contentDescription = null,
                            modifier = Modifier
                                .size(20.dp)
                                .rotate(rocketRotation)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text("冲鸭!", fontWeight = FontWeight.Bold)
                    }
                }

                // 休息按钮
                OutlinedButton(
                    onClick = onStopService,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Pink40)
                ) {
                    Icon(
                        imageVector = Icons.Rounded.Bedtime,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("休息啦", fontWeight = FontWeight.Bold)
                }
            }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // 看状态按钮
                OutlinedButton(
                    onClick = onCheckService,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Lavender40)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Visibility,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("瞅一眼", fontWeight = FontWeight.Medium)
                }

                // 更多按钮
                OutlinedButton(
                    onClick = onOpenAppSettings,
                    modifier = Modifier.weight(1f),
                    shape = RoundedCornerShape(14.dp),
                    colors = ButtonDefaults.outlinedButtonColors(contentColor = Mint40)
                ) {
                    Icon(
                        imageVector = Icons.Outlined.Settings,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("更多~", fontWeight = FontWeight.Medium)
                }
            }
        }
    }
}
