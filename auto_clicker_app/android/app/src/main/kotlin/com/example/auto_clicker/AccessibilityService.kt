package com.example.auto_clicker

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.GestureDescription
import android.graphics.Path
import android.graphics.Rect
import android.os.Bundle
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import android.util.Log
import kotlin.math.abs

class AutoClickerAccessibilityService : AccessibilityService() {
    companion object {
        private const val TAG = "AutoClickerService"
        private var instance: AutoClickerAccessibilityService? = null

        fun getInstance(): AutoClickerAccessibilityService? = instance

        fun isRunning(): Boolean = instance != null
    }

    private var clickActions: List<Map<String, Any>> = emptyList()
    private var isAutoClicking = false
    private var currentActionIndex = 0

    override fun onServiceConnected() {
        super.onServiceConnected()
        instance = this
        Log.d(TAG, "无障碍服务已连接")
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // 可以在这里监听界面变化
    }

    override fun onInterrupt() {
        Log.d(TAG, "无障碍服务被中断")
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        isAutoClicking = false
        Log.d(TAG, "无障碍服务已销毁")
    }

    /**
     * 执行坐标点击
     */
    fun performCoordinateClick(x: Float, y: Float): Boolean {
        Log.d(TAG, "执行坐标点击: ($x, $y)")

        val path = Path()
        path.moveTo(x, y)

        val gestureBuilder = GestureDescription.Builder()
        gestureBuilder.addStroke(GestureDescription.StrokeDescription(path, 0, 100))

        val gesture = gestureBuilder.build()

        return dispatchGesture(gesture, null, null)
    }

    /**
     * 查找并点击包含指定文本的节点
     */
    fun performTextClick(text: String): Boolean {
        if (text.isNullOrEmpty()) {
            Log.e(TAG, "文本为空")
            return false
        }

        Log.d(TAG, "查找文本: $text")

        val rootNode = rootInActiveWindow
        if (rootNode == null) {
            Log.e(TAG, "无法获取根节点，无障碍服务可能未正确连接")
            return false
        }

        val node = findNodeByText(rootNode, text)
        return if (node != null) {
            val bounds = Rect()
            node.getBoundsInScreen(bounds)
            val centerX = (bounds.left + bounds.right) / 2f
            val centerY = (bounds.top + bounds.bottom) / 2f

            Log.d(TAG, "找到节点，坐标: ($centerX, $centerY)")
            performCoordinateClick(centerX, centerY)
        } else {
            Log.e(TAG, "未找到文本节点: $text")
            false
        }
    }

    /**
     * 递归查找包含指定文本的节点
     */
    private fun findNodeByText(node: AccessibilityNodeInfo?, text: String): AccessibilityNodeInfo? {
        if (node == null) return null

        // 检查当前节点
        val nodeText = node.text?.toString() ?: ""
        val contentDesc = node.contentDescription?.toString() ?: ""

        if (nodeText.contains(text, ignoreCase = true) ||
            contentDesc.contains(text, ignoreCase = true)) {
            Log.d(TAG, "找到匹配节点: $nodeText")
            return node
        }

        // 检查子节点
        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                val result = findNodeByText(child, text)
                if (result != null) {
                    return result
                }
            }
        }

        return null
    }

    /**
     * 执行单个点击动作
     */
    fun performClickAction(action: Map<String, Any>): Boolean {
        try {
            val type = action["type"] as? String ?: run {
                Log.e(TAG, "缺少点击类型")
                return false
            }

            return when (type) {
                "coordinate" -> {
                    val x = (action["x"] as? Number)?.toFloat() ?: run {
                        Log.e(TAG, "坐标 X 无效")
                        return false
                    }
                    val y = (action["y"] as? Number)?.toFloat() ?: run {
                        Log.e(TAG, "坐标 Y 无效")
                        return false
                    }
                    Log.d(TAG, "执行坐标点击: ($x, $y)")
                    performCoordinateClick(x, y)
                }
                "text" -> {
                    val text = action["text"] as? String ?: run {
                        Log.e(TAG, "点击文本为空")
                        return false
                    }
                    performTextClick(text)
                }
                else -> {
                    Log.e(TAG, "未知的点击类型: $type")
                    false
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "执行点击动作时发生异常: ${e.message}", e)
            false
        }
    }

    /**
     * 设置点击动作列表
     */
    fun setClickActions(actions: List<Map<String, Any>>) {
        clickActions = actions
        Log.d(TAG, "设置了 ${actions.size} 个点击动作")
    }

    /**
     * 开始自动点击
     */
    fun startAutoClick() {
        if (isAutoClicking) {
            Log.w(TAG, "自动点击已在运行中")
            return
        }

        if (clickActions.isEmpty()) {
            Log.w(TAG, "没有可执行的点击动作")
            return
        }

        isAutoClicking = true
        currentActionIndex = 0
        Log.d(TAG, "开始自动点击，共 ${clickActions.size} 个动作")

        executeNextAction()
    }

    /**
     * 停止自动点击
     */
    fun stopAutoClick() {
        isAutoClicking = false
        currentActionIndex = 0
        Log.d(TAG, "停止自动点击")
    }

    /**
     * 执行下一个动作
     */
    private fun executeNextAction() {
        if (!isAutoClicking || currentActionIndex >= clickActions.size) {
            stopAutoClick()
            return
        }

        val action = clickActions[currentActionIndex]
        val delay = (action["delay"] as? Double)?.toLong() ?: 500

        android.os.Handler(android.os.Looper.getMainLooper()).postDelayed({
            if (!isAutoClicking) return@postDelayed

            val success = performClickAction(action)
            Log.d(TAG, "动作 ${currentActionIndex + 1} 执行${if (success) "成功" else "失败"}")

            currentActionIndex++

            if (currentActionIndex < clickActions.size) {
                executeNextAction()
            } else {
                stopAutoClick()
            }
        }, delay)
    }

    /**
     * 获取屏幕尺寸
     */
    fun getScreenSize(): Pair<Int, Int> {
        try {
            val rootNode = rootInActiveWindow ?: run {
                Log.w(TAG, "无法获取根节点，返回默认屏幕尺寸")
                return Pair(1080, 1920)
            }
            val bounds = Rect()
            rootNode.getBoundsInScreen(bounds)
            val width = bounds.width()
            val height = bounds.height()

            if (width > 0 && height > 0) {
                Log.d(TAG, "屏幕尺寸: ${width}x$height")
                return Pair(width, height)
            } else {
                Log.w(TAG, "屏幕尺寸无效，使用默认值")
                return Pair(1080, 1920)
            }
        } catch (e: Exception) {
            Log.e(TAG, "获取屏幕尺寸失败: ${e.message}", e)
            return Pair(1080, 1920)
        }
    }
}
