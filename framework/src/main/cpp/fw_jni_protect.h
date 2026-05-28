/**
 * ============================================================================
 * fw_jni_protect.h - Native JNI 字符串防护工具
 * ============================================================================
 *
 * 功能简介：
 *   提供轻量级编译期字符串异或封装，用于 JNI 动态注册时隐藏类名、方法名
 *   和签名字符串，降低 so 被静态分析时直接看到关键入口的概率。
 *
 * 主要函数：
 *   - FW_PROTECT_STR: 将字符串字面量编译为异或后的字节数组，运行时解码
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.0.1
 * ============================================================================
 */

#pragma once

#include <array>
#include <cstdint>
#include <string>

namespace fw {
namespace protect {

/**
 * 根据种子和索引生成单字节密钥。
 */
constexpr char keyAt(uint32_t seed, size_t index) {
    return static_cast<char>(((seed >> ((index % 4U) * 8U)) & 0xFFU) ^
                             static_cast<uint32_t>((index * 31U + 0x5AU) & 0xFFU));
}

/**
 * 编译期字符串异或容器。
 */
template <size_t Size, uint32_t Seed>
class ObfuscatedLiteral {
public:
    /**
     * 构造时把明文字面量转成异或后的字节数组。
     */
    constexpr explicit ObfuscatedLiteral(const char (&plain)[Size]) : data_{} {
        for (size_t index = 0; index < Size; ++index) {
            data_[index] = static_cast<char>(plain[index] ^ keyAt(Seed, index));
        }
    }

    /**
     * 运行时解码字符串。
     */
    std::string decode() const {
        std::string result;
        result.resize(Size > 0 ? Size - 1 : 0);
        for (size_t index = 0; index + 1 < Size; ++index) {
            result[index] = static_cast<char>(data_[index] ^ keyAt(Seed, index));
        }
        return result;
    }

private:
    std::array<char, Size> data_;
};

} // namespace protect
} // namespace fw

#define FW_PROTECT_STR(value) \
    ([]() -> std::string { \
        constexpr ::fw::protect::ObfuscatedLiteral<sizeof(value), \
            static_cast<uint32_t>((__LINE__ * 1103515245U) + 12345U)> obfuscated(value); \
        return obfuscated.decode(); \
    }())
