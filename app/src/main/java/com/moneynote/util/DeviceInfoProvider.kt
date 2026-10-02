package com.moneynote.util

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.pm.PackageInfoCompat

/**
 * 采集运行环境信息，用于反馈邮件和日志导出。
 *
 * 反馈页和日志页都需要，所以抽到这里共用，避免两处各写一份。
 */
fun collectDeviceInfo(context: Context): DeviceInfo {
    val packageManager = context.packageManager
    val packageInfo = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        packageManager.getPackageInfo(
            context.packageName,
            PackageManager.PackageInfoFlags.of(0L),
        )
    } else {
        @Suppress("DEPRECATION")
        packageManager.getPackageInfo(context.packageName, 0)
    }

    return DeviceInfo(
        appVersion = (packageInfo.versionName ?: "未知") +
            " (" + PackageInfoCompat.getLongVersionCode(packageInfo) + ")",
        androidVersion = "Android " + Build.VERSION.RELEASE +
            " (API " + Build.VERSION.SDK_INT + ")",
        deviceModel = (Build.MANUFACTURER + " " + Build.MODEL).trim(),
    )
}
