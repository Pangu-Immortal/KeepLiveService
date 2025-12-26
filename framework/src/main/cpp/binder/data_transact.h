/**
 * ============================================================================
 * data_transact.h - Binder 数据事务处理头文件
 * ============================================================================
 *
 * 功能简介：
 *   声明与 Binder 驱动通信的函数接口，包括驱动初始化、数据事务处理
 *   和响应处理等功能的函数原型。
 *
 * 主要函数声明：
 *   - open_driver：打开 Binder 驱动
 *   - initProcessState/unInitProcessState：进程状态管理
 *   - write_transact：发起 Binder 事务
 *   - writeTransactionData：写入事务数据
 *   - waitForResponse：等待响应
 *   - talkWithDriver：与驱动通信
 *   - executeCommand：执行命令
 *   - freeBuffer：释放缓冲区
 *
 * @author Pangu-Immortal
 * @github https://github.com/Pangu-Immortal/KeepLiveService
 * @since 2.1.0
 */

#ifndef FW_DATA_TRANSACT_H
#define FW_DATA_TRANSACT_H

#include <jni.h>
#include <sys/wait.h>
#include <stdio.h>
#include <stdlib.h>
#include <unistd.h>
#include <sys/file.h>
#include <linux/android/binder.h>
#include "common.h"
#include "cParcel.h"

#define BINDER_VM_SIZE ((1 * 1024 * 1024) - sysconf(_SC_PAGE_SIZE) * 2)
#define DEFAULT_MAX_BINDER_THREADS 15

using namespace android;
extern "C" {
int open_driver();

void initProcessState(int mDriverFD, void *mVMStart);
void unInitProcessState(int mDriverFD, void *mVMStart);

status_t
write_transact(int32_t handle, uint32_t code, const Parcel &data, Parcel *reply, uint32_t flags,
               int driverFD);

status_t writeTransactionData(int32_t cmd, uint32_t binderFlags, int32_t handle, uint32_t code,
                              const Parcel &data, Parcel &mOut, status_t *statusBuffer);

status_t
waitForResponse(Parcel *reply, status_t *acquireResult, int mDriverFD, Parcel &mOut, Parcel &mIn);

status_t talkWithDriver(bool doReceive, int mDriverFD, Parcel &mOut, Parcel &mIn);

status_t executeCommand(uint32_t cmd, Parcel &mIn, Parcel &mOut);

//void freeBuffer(Parcel* parcel, const uint8_t* data,
//                size_t /*dataSize*/,
//                const binder_size_t* /*objects*/,
//                size_t /*objectsSize*/, void* /*cookie*/);
void freeBuffer(Parcel* parcel,
                const uint8_t* data, size_t dataSize,
                const binder_size_t* objects, size_t objectsSize,
                void* cookie);
}
#endif //FW_DATA_TRANSACT_H
