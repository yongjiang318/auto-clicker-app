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
            )
            val packageName = packageName
            val isEnabled = enabledServices?.contains(packageName) ?: false

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
            service?.setClickActions(actions)
            service?.startAutoClick()
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
}
