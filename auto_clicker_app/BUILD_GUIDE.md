# 📦 APK 构建指南

由于系统限制，无法在当前环境直接构建 APK。以下是完整的构建方案。

---

## 方案一：在有 Android SDK 的机器上构建

### 前置要求

已安装以下工具：
- ✅ Flutter SDK（已有）
- ❌ Android SDK（需要安装）
- ❌ Java JDK（需要安装）

### 安装步骤

#### 1. 安装 Java JDK

```bash
# Ubuntu/Debian
sudo apt update
sudo apt install openjdk-17-jdk

# 验证安装
java -version
```

#### 2. 安装 Android SDK

**方法 A：使用 Android Studio（推荐）**

1. 下载 Android Studio
   - 访问：https://developer.android.com/studio
   - 下载 Linux 版本

2. 安装
   ```bash
   tar -xvzf android-studio-*.tar.gz
   cd android-studio/bin
   ./studio.sh
   ```

3. 通过 Android Studio 安装 SDK
   - 启动 Android Studio
   - 配置向导中选择 "Standard"
   - 等待 SDK 下载完成

**方法 B：使用命令行安装 SDK**

```bash
# 下载 commandlinetools
mkdir -p ~/Android/sdk/cmdline-tools
cd ~/Android/sdk/cmdline-tools
wget https://dl.google.com/android/repository/commandlinetools-linux-11076708_latest.zip
unzip commandlinetools-linux-11076708_latest.zip
mv cmdline-tools latest

# 设置环境变量
export ANDROID_HOME=~/Android/sdk
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools

# 安装必要组件
sdkmanager "platform-tools" "platforms;android-34" "build-tools;34.0.0"

# 接受许可
yes | sdkmanager --licenses
```

#### 3. 配置环境变量

编辑 `~/.bashrc` 或 `~/.zshrc`，添加：

```bash
export ANDROID_HOME=~/Android/sdk
export ANDROID_SDK_ROOT=$ANDROID_HOME
export PATH=$PATH:$ANDROID_HOME/cmdline-tools/latest/bin
export PATH=$PATH:$ANDROID_HOME/platform-tools
export PATH=$PATH:$ANDROID_HOME/emulator
export PATH=$PATH:$ANDROID_HOME/tools/bin
```

重新加载配置：
```bash
source ~/.bashrc
```

#### 4. 验证安装

```bash
# 检查 Android SDK
adb version

# 检查 Flutter 环境
flutter doctor -v
```

应该显示所有项目都有绿色✅标记。

#### 5. 构建 APK

```bash
cd ~/.openclaw/workspace/auto_clicker_app
flutter build apk --release
```

构建成功后，APK 文件位置：
```
build/app/outputs/flutter-apk/app-release.apk
```

---

## 方案二：使用在线构建服务

### 使用 GitHub Actions

1. 将代码推送到 GitHub 仓库
2. 创建 `.github/workflows/build.yml`：
   ```yaml
   name: Build APK

   on: [push]

   jobs:
     build:
       runs-on: ubuntu-latest
       steps:
         - uses: actions/checkout@v3
         - uses: subosito/flutter-action@v2
           with:
             channel: 'stable'
         - run: flutter pub get
         - run: flutter build apk --release
         - uses: actions/upload-artifact@v3
           with:
             name: release-apk
             path: build/app/outputs/flutter-apk/app-release.apk
   ```
3. 推送代码后，GitHub 会自动构建并生成 APK

### 使用 Codemagic

1. 访问：https://codemagic.io
2. 连接 GitHub 仓库
3. 配置构建流程
4. 自动生成 APK

---

## 方案三：使用 Android Studio 构建

### 步骤

1. **打开 Android Studio**
   ```bash
   ~/android-studio/bin/studio.sh
   ```

2. **打开项目**
   - 选择 "Open an Existing Project"
   - 选择 `auto_clicker_app` 文件夹

3. **等待 Gradle 同步**
   - Android Studio 会自动下载依赖
   - 等待底部状态栏显示 "Gradle sync finished"

4. **构建 APK**
   - 菜单：Build → Build Bundle(s) / APK(s) → Build APK(s)
   - 等待构建完成

5. **定位 APK**
   - 点击弹窗中的 "locate"
   - 或手动查找：`build/app/outputs/flutter-apk/app-release.apk`

---

## 📱 安装 APK 到手机

### 方法 1：通过 USB 安装

```bash
# 1. 连接手机
adb devices

# 2. 安装 APK
adb install -r build/app/outputs/flutter-apk/app-release.apk

# 3. 启动应用
adb shell am start -n com.example.auto_clicker_app/com.example.auto_clicker.MainActivity
```

### 方法 2：直接传输安装

1. 将 APK 文件传输到手机（USB、微信、蓝牙等）
2. 在手机文件管理器中找到 APK 文件
3. 点击安装
4. 如果提示"允许未知来源"，选择"允许"

---

## 🔧 调试技巧

### 使用 ADB 查看日志

```bash
# 实时查看日志
adb logcat | grep AutoClicker

# 查看无障碍服务日志
adb logcat | grep AccessibilityService

# 保存日志到文件
adb logcat -d > logcat.txt
```

### 安装并运行 Debug 版本

```bash
# 构建 debug 版本
flutter build apk --debug

# 安装
adb install -r build/app/outputs/flutter-apk/app-debug.apk

# 查看日志
flutter logs
```

---

## ⚠️ 常见问题

### 问题 1：flutter doctor 报错

**错误：** "Android licenses not accepted"

**解决：**
```bash
flutter doctor --android-licenses
# 按提示输入 y 接受许可
```

### 问题 2：Gradle 同步失败

**解决：**
```bash
# 清理构建缓存
cd android
./gradlew clean
cd ..

# 重新构建
flutter clean
flutter pub get
flutter build apk --release
```

### 问题 3：构建超时

**解决：**
```bash
# 增加内存
export GRADLE_OPTS="-Xmx4g -XX:MaxPermSize=512m"

# 或使用 gradle daemon
cd android
./gradlew assembleRelease
```

### 问题 4：签名问题

**发布版需要签名，debug 版本使用默认签名**

**生成发布版签名：**

```bash
# 生成 keystore
keytool -genkey -v -keystore ~/upload-keystore.jks -keyalg RSA -keysize 2048 -validity 10000 -alias upload

# 配置签名（编辑 android/key.properties）
echo "storePassword=<your-password>" > android/key.properties
echo "keyPassword=<your-password>" >> android/key.properties
echo "keyAlias=upload" >> android/key.properties
echo "storeFile=/home/<user>/upload-keystore.jks" >> android/key.properties

# 构建签名 APK
flutter build apk --release
```

---

## 📊 构建后验证

### 验证 APK 信息

```bash
# 查看 APK 信息
aapt dump badging build/app/outputs/flutter-apk/app-release.apk

# 查看 APK 内容
unzip -l build/app/outputs/flutter-apk/app-release.apk
```

### 测试安装

```bash
# 安装到设备
adb install -r build/app/outputs/flutter-apk/app-release.apk

# 验证是否成功
adb shell pm list packages | grep auto_clicker
```

---

## 🚀 快速构建脚本

创建 `build_apk.sh`：

```bash
#!/bin/bash

echo "🔨 开始构建 APK..."

# 清理旧文件
flutter clean
flutter pub get

# 构建
flutter build apk --release

# 检查是否成功
if [ -f "build/app/outputs/flutter-apk/app-release.apk" ]; then
    echo "✅ 构建成功！"
    echo "📱 APK 位置: build/app/outputs/flutter-apk/app-release.apk"
    ls -lh build/app/outputs/flutter-apk/app-release.apk
else
    echo "❌ 构建失败"
    exit 1
fi
```

使用：
```bash
chmod +x build_apk.sh
./build_apk.sh
```

---

**下一步：** 按照以上方案安装 Android SDK 后，运行 `flutter build apk --release` 即可生成 APK。

🦞 小龙虾
