# 🤖 自动点击器 APK

> 一个使用 Flutter 开发的独立 Android 应用，用于自动点击微信小程序中的按钮和选择框

## 📱 快速获取 APK

### 方式 1：从 GitHub Actions 下载（推荐）

1. 访问此仓库的 **Actions** 标签页
2. 点击最新的 **"Build Android APK"** 工作流
3. 滚动到底部的 **Artifacts** 部分
4. 下载 `auto-clicker-apk` 压缩包
5. 解压后得到 `app-release.apk`

### 方式 2：手动构建

如果你有 Flutter 和 Android SDK 环境：

```bash
cd auto_clicker_app
flutter build apk --release
# APK 输出位置：build/app/outputs/flutter-apk/app-release.apk
```

---

## 🚀 安装使用

### 1. 安装 APK

将 `app-release.apk` 文件传输到华为手机，直接安装即可。

### 2. 启用无障碍服务（必须）

```
设置 → 辅助功能 → 无障碍 → 自动点击器 → 开启
```

或在 App 内点击右上角设置图标。

### 3. 开始使用

1. 打开微信小程序到目标界面
2. 在 App 中添加点击动作
3. 点击"开始"按钮

详细使用方法请查看：[用户指南.md](auto_clicker_app/用户指南.md) 和 [README.md](auto_clicker_app/README.md)

---

## ✨ 功能特性

- ✅ **坐标点击**：精确点击屏幕指定位置
- ✅ **文本点击**：根据按钮文字自动查找并点击（推荐）
- ✅ **动作序列**：支持多个点击动作按顺序执行
- ✅ **延迟控制**：每个动作之间可设置延迟
- ✅ **独立运行**：无需电脑，手机上直接使用

---

## 📂 项目结构

```
.
├── auto_clicker_app/           # Flutter 项目主目录
│   ├── lib/main.dart           # Flutter 主界面
│   ├── android/                # Android 原生代码
│   │   └── AccessibilityService.kt  # 无障碍服务
│   ├── README.md               # 使用说明
│   ├── BUILD_GUIDE.md          # 构建指南
│   └── 用户指南.md              # 中文用户指南
└── .github/workflows/          # GitHub Actions 工作流
    └── build-apk.yml          # 自动构建 APK 配置
```

---

## 🔧 技术栈

- **前端框架**：Flutter (Dart)
- **原生代码**：Android (Kotlin)
- **目标平台**：Android 7.0+ (API 24+)
- **无障碍服务**：AccessibilityService API

---

## 📖 文档

- [使用说明](auto_clicker_app/README.md)
- [构建指南](auto_clicker_app/BUILD_GUIDE.md)
- [中文用户指南](auto_clicker_app/用户指南.md)

---

## ⚠️ 注意事项

### 安装前
- 确保 Android 版本 >= 7.0
- 允许安装来自未知来源的应用

### 使用时
- 必须启用无障碍服务
- 保持屏幕解锁
- 微信小程序必须在前台显示

### 常见问题

**问题：无法点击**
- 检查无障碍服务是否开启
- 确保微信在前台
- 尝试使用文本点击替代坐标点击

**问题：无障碍服务自动关闭**
```
设置 → 应用管理 → 自动点击器 → 电池优化 → 不优化
```

---

## 🔒 隐私与安全

- ✅ 本地执行，不上传数据
- ✅ 不需要网络权限
- ✅ 源代码完全开源
- ⚠️ 仅用于测试，请遵守相关法律法规

---

## 📞 技术支持

- 查看 [BUILD_GUIDE.md](auto_clicker_app/BUILD_GUIDE.md) 了解构建问题
- 查看 [README.md](auto_clicker_app/README.md) 了解使用问题

---

## 📄 许可证

本项目源代码开源，仅供学习和测试使用。

---

**开发者**：小龙虾 🦞
**版本**：1.0.0
**更新日期**：2026-03-05

---

*从 GitHub Actions 下载 APK，或使用 Flutter 自行构建*
