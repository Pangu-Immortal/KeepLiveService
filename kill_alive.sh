#!/bin/bash
# ============================================================================
# kill_alive.sh - 保活效果自动化测试脚本
# ============================================================================
#
# 功能简介：
#   循环使用 am force-stop 强制停止应用，验证保活策略的恢复能力。
#   每次停止后等待指定时间，然后检查应用是否自动恢复。
#
# 使用方式：
#   ./kill_alive.sh [次数] [间隔秒数] [包名]
#
# 示例：
#   ./kill_alive.sh              # 默认：100次，5秒间隔
#   ./kill_alive.sh 50 3         # 50次，3秒间隔
#   ./kill_alive.sh 200 10 com.example.app  # 指定包名
#
# @author Pangu-Immortal
# @github https://github.com/Pangu-Immortal/KeepLiveService
# @since 2.2.1
# ============================================================================

TIMES=${1:-100}                          # 测试次数，默认 100
INTERVAL=${2:-5}                         # 每次间隔秒数，默认 5
PACKAGE=${3:-"com.google.services"}      # 应用包名

SUCCESS=0                                # 恢复成功计数
FAIL=0                                   # 恢复失败计数

echo "========================================"
echo " 保活测试开始"
echo " 包名: $PACKAGE"
echo " 测试次数: $TIMES"
echo " 检查间隔: ${INTERVAL}s"
echo "========================================"

for i in $(seq 1 $TIMES); do
    echo ""
    echo "--- 第 $i/$TIMES 次测试 ---"

    # 强制停止应用
    adb shell am force-stop "$PACKAGE"
    echo "[$(date +%H:%M:%S)] 已执行 force-stop"

    # 等待恢复
    sleep "$INTERVAL"

    # 检查应用是否恢复运行
    RUNNING=$(adb shell ps -A 2>/dev/null | grep "$PACKAGE" | grep -v grep | wc -l)

    if [ "$RUNNING" -gt 0 ]; then
        SUCCESS=$((SUCCESS + 1))
        echo "[$(date +%H:%M:%S)] ✅ 恢复成功（进程数: $RUNNING）"
    else
        FAIL=$((FAIL + 1))
        echo "[$(date +%H:%M:%S)] ❌ 恢复失败"
    fi
done

echo ""
echo "========================================"
echo " 测试结果"
echo " 总次数: $TIMES"
echo " 恢复成功: $SUCCESS"
echo " 恢复失败: $FAIL"
echo " 成功率: $(echo "scale=1; $SUCCESS * 100 / $TIMES" | bc)%"
echo "========================================"
