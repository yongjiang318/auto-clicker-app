package com.example.auto_clicker

import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.text.TextUtils
import android.accessibilityservice.AccessibilityServiceInfo
import android.util.Log
import io.flutter.embedding.android.FlutterActivity
import io.flutter.embedding.engine.FlutterEngine
import io.flutter.plugin.common.MethodChannel

class MainActivity : FlutterActivity() {
    companion object {
        private const val TAG = "AutoClickerMain"
    }

    private val CHANNEL = "com.example.auto_clicker/accessibility"

    override fun configureFlutterEngine(flutterEngine: FlutterEngine) {
        super.configureFlutterEngine(flutterEngine)

        MethodChannel(flutterEngine.dartExecutor.binaryMessenger, CHANNEL).setMethodCallHandler { call, result ->
            when (call.method) {
                "isAccessibilityEnabled" -> {
                    result.success(isAccessibilityServiceEnabled())
                }
                "openAccessibilitySettings" -> {
                    openAccessibilitySettings()
                    result.success(null)
                }
                "startAutoClick" -> {
                    val actions = call.argument<List<Map<String, Any>>>("actions")
                    if (actions != null) {
                        startAutoClick(actions)
                        result.success(true)
                    } else {
                        result.success(false)
                    }
                }
                "stopAutoClick" -> {
                    stopAutoClick()
                    result.success(null)
                }
                "performClick" -> {
                    val action = call.argument<Map<String, Any>>("action")
                    if (action != null) {
                        val success = performSingleClick(action)
                        result.success(success)
                    } else {
                        result.success(false)
                    }
                }
                "getScreenSize" -> {
                    val (width, height) = getScreenSize()
                    result.success(mapOf("width" to width, "height" to height))
                }
                "requestBatteryOptimizationExemption" -> {
                    requestBatteryOptimizationExemption()
                    result.success(null)
                }
                "openHuaWeiBackgroundSettings" -> {
                    openHuaWeiBackgroundSettings()
                    result.success(null)
                }
                else -> {
                    result.notImplemented()
                }
            }
        }
    }

    /**
     * 检查无障碍服务是否启用
     */
    private fun isAccessibilityServiceEnabled(): Boolean {
        try {
            val enabledServices = Settings.Secure.getString(
                contentResolver,
                Settings.Secure.ENABLED_ACCESSIBILITY_SERVICES
            ) ?: return false
            
            // 使用 ComponentName 进行更准确的检查
            val componentName = ComponentName(this, AutoClickerAccessibilityService::class.java)
            val flattenToString = componentName.flattenToString()
            val packageName = packageName
            
            Log.d(TAG, "检查无障碍服务：")
            Log.d(TAG, "  包名: $packageName")
            Log.d(TAG, "  组件名: $flattenToString")
            Log.d(TAG, "  已启用服务: $enabledServices")
            
            val isEnabled = enabledServices.contains(flattenToString) || 
                           enabledServices.contains(packageName)
            
            Log.d(TAG, "无障碍服务状态: $isEnabled")
            return isEnabled
        } catch (e: Exception) {
            Log.e(TAG, "检查无障碍服务失败", e)
            return false
        }
    }

    /**
     * 打开无障碍设置
     */
    private fun openAccessibilitySettings() {
        val intent = Intent(Settings.ACTION_ACCESSIBILITY_SETTINGS)
        startActivity(intent)
    }

    /**
     * 开始自动点击
     */
    private fun startAutoClick(actions: List<Map<String, Any>>) {
        try {
            // 启动前台服务
            startForegroundService()

            val service = AutoClickerAccessibilityService.getInstance()
            if (service == null) {
                Log.e(TAG, "无障碍服务未启动，请先在设置中启用")
                return
            }
            
            service.setClickActions(actions)
            service.startAutoClick()
            Log.d(TAG, "自动点击已启动，共 ${actions.size} 个动作")
        } catch (e: Exception) {
            Log.e(TAG, "启动自动点击失败", e)
        }
    }

    /**
     * 停止自动点击
     */
    private fun stopAutoClick() {
        try {
            val service = AutoClickerAccessibilityService.getInstance()
            service?.stopAutoClick()

            // 停止前台服务
            stopForegroundService()
        } catch (e: Exception) {
            Log.e(TAG, "停止自动点击失败", e)
        }
    }

    /**
     * 执行单个点击
     */
    private fun performSingleClick(action: Map<String, Any>): Boolean {
        val service = AutoClickerAccessibilityService.getInstance()
        return service?.performClickAction(action) ?: false
    }

    /**
     * 获取屏幕尺寸
     */
    private fun getScreenSize(): Pair<Int, Int> {
        val service = AutoClickerAccessibilityService.getInstance()
        return service?.getScreenSize() ?: Pair(0, 0)
    }

    /**
     * 启动前台服务
     */
    private fun startForegroundService() {
        try {
            val intent = Intent(this, AutoClickerForegroundService::class.java)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                startForegroundService(intent)
            } else {
                startService(intent)
            }
        } catch (e: Exception) {
            Log.e(TAG, "启动前台服务失败", e)
        }
    }

    /**
     * 停止前台服务
     */
    private fun stopForegroundService() {
        try {
            val intent = Intent(this, AutoClickerForegroundService::class.java)
            stopService(intent)
        } catch (e: Exception) {
            Log.e(TAG, "停止前台服务失败", e)
        }
    }

    /**
     * 请求电池优化豁免（华为等国产手机必须）
     */
    private fun requestBatteryOptimizationExemption() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M) {
            try {
                val intent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS)
                intent.data = Uri.parse("package:$packageName")
                Log.d(TAG, "请求电池优化豁免")
                startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "通用电池优化设置失败，尝试华为专用路径", e)
                // 华为 EMUI 专用路径
                try {
                    val intent = Intent()
                    intent.setClassName("com.huawei.systemmanager", 
                        "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
                    Log.d(TAG, "使用华为专用路径")
                    startActivity(intent)
                } catch (e2: Exception) {
                    Log.e(TAG, "华为专用路径也失败", e2)
                    // 降级到应用详情页面
                    val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                    intent.data = Uri.parse("package:$packageName")
                    startActivity(intent)
                }
            }
        }
    }

    /**
     * 打开华为后台保护设置
     */
    private fun openHuaWeiBackgroundSettings() {
        try {
            // 华为 EMUI 10+ 特殊跳转
            val intent = Intent()
            intent.setClassName("com.huawei.systemmanager", 
                "com.huawei.systemmanager.startupmgr.ui.StartupNormalAppListActivity")
            Log.d(TAG, "打开华为后台保护设置")
            startActivity(intent)
        } catch (e: Exception) {
            Log.e(TAG, "华为后台设置打开失败", e)
            // 降级到通用设置
            try {
                val intent = Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
                intent.data = Uri.parse("package:$packageName")
                startActivity(intent)
            } catch (e2: Exception) {
                Log.e(TAG, "通用设置也失败", e2)
            }
        }
    }
}
