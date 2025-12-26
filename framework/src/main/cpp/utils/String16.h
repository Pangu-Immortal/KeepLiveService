/**
 * ============================================================================
 * String16.h - UTF-16字符串类头文件
 * ============================================================================
 *
 * 功能简介：
 *   定义UTF-16编码的字符串类，提供从UTF-8和UTF-16创建字符串的构造函数，
 *   支持获取字符串内容和长度，用于Android Binder通信中的字符串处理。
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#ifndef KEEPALIVE_STRING16_H
#define KEEPALIVE_STRING16_H

#include <stdint.h>
#include <string.h>
#include "Unicode.h"

namespace android {
    class String16 {
    public:
        String16();

        explicit String16(const char *o);

        explicit String16(const Char16 *o, size_t len);

        ~String16();

        inline const Char16 *string() const;

        size_t size() const;

    private:
        const Char16 *mString;
    };

    inline const Char16 *String16::string() const {
        return mString;
    }
}
#endif //KEEPALIVE_STRING16_H
