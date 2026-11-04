# 智能指南针 CompassApp · 项目说明文档

> **项目名称**：智能指南针（CompassApp）
> **项目类型**：Android 原生应用
> **技术栈**：Kotlin + Jetpack Compose + Material 3
> **版本**：1.0.0（versionCode 1）
> **文档版本**：v1.0
> **文档日期**：2026-11-04

---

## 1. 文档说明

### 1.1 文档目的

本文档是智能指南针 CompassApp 的完整说明文档，供以下人群使用：

- **开发者**：了解项目结构、依赖与编译过程，以便二次开发或参与贡献；
- **测试 / 体验用户**：了解如何从源码构建 APK，以及如何将应用安装到 Android 设备；
- **GitHub 访客**：快速理解本项目能做什么、如何跑起来。

本文档与项目一同上传至 GitHub，作为仓库的权威说明资料。

### 1.2 文档内容一览

| 章节 | 内容 |
|---|---|
| 第 1 章 文档说明 | 本文档的定位、适用范围与内容结构 |
| 第 2 章 项目简介 | 项目背景、功能清单、技术选型 |
| 第 3 章 环境要求 | 构建所需的硬件 / 软件 / 网络前提 |
| 第 4 章 编译过程 | 从源码到 APK 的完整编译流程（含命令行与 IDE 两种方式） |
| 第 5 章 安装过程 | APK 安装、权限授予与启动步骤 |
| 第 6 章 运行与使用 | 各功能页面的操作说明 |
| 第 7 章 项目结构 | 目录与关键文件说明 |
| 第 8 章 GitHub 上传 | 将本项目发布到 GitHub 的操作指引 |
| 第 9 章 常见问题 | 构建 / 安装 / 使用中的典型问题与解决办法 |

### 1.3 版本约定

- 版本号采用 `主版本.次版本.修订号` 三段式；
- `versionCode` 用于 Android 系统内部版本比较，每次发布递增；
- 本文档随代码同步维护，重大变更需更新「文档版本」。

---

## 2. 项目简介

### 2.1 项目背景

一款功能强大的 Android 指南针应用，基于 **Kotlin + Jetpack Compose + Material 3** 开发，参考市面主流指南针 App 设计，融合了方向、定位、气压、姿态等多种传感器数据，实现高精度的实时指南针与水平仪体验。

### 2.2 功能清单

| 功能 | 说明 |
|---|---|
| 🧭 实时指南针 | 旋转矢量传感器融合定位，精确到 1°，表盘平滑动画（低通滤波 + Compose 动画插值） |
| 🌐 真北/磁北切换 | 获取定位后自动启用真北；通过 GNSS 位置 + 地磁模型（GeomagneticField）计算磁偏角修正 |
| 📍 经纬度显示 | 实时 GNSS 定位，度分秒格式（如 北纬 31°48′11″ / 东经 117°17′14″） |
| 🌡 气压与海拔 | 气压计实时气压（百帕）；海拔多级兜底（见 2.4 海拔算法） |
| 💧 水平仪 | 气泡式水平仪，倾斜 <1° 变绿提示"完全水平" |
| 📐 姿态显示 | 实时俯仰 / 横滚角度 |
| 🧲 磁场监测 | 实时磁场强度（μT），超出 20~70μT 自动告警 |
| 🎯 校准向导 | 画"8"字动画引导 + 磁场强度仪表 + 传感器状态提示 |
| 🌐 联网校准海拔 | 通过 Open-Meteo 接口获取当地海平面气压（QNH），提升气压海拔精度 |
| ✍️ 手动海拔校准 | 点击海拔数字输入已知真实海拔，锁存修正量 |
| 🎨 深色主题 | 全局深色 UI，夜间友好，高对比度表盘 |

### 2.3 技术选型

| 项目 | 选型 |
|---|---|
| 语言 | Kotlin 2.0.20 |
| UI 框架 | Jetpack Compose（Compose BOM 2024.08.00） |
| 设计语言 | Material 3 |
| 构建工具 | Gradle 8.7（AGP 8.5.2） |
| 最小系统 | Android 8.0（minSdk 26） |
| 目标系统 | Android 14（targetSdk 34） |

### 2.4 海拔算法（多级兜底）

优先级从高到低，确保任何场景都有合理输出：

```
1. GPS 海拔          —— 需通过有效性验证（|海拔|>1m 且 垂直精度≤30m）
2. 联网校准气压海拔   —— 用 Open-Meteo 获取的当地海平面气压 (QNH) 反演
3. GPS 自动校准气压   —— 用有效 GPS 海拔反算气压偏差，平滑锁存
4. 手动校准           —— 用户输入已知海拔，锁存修正量
5. 标准大气           —— 1013.25 hPa 基准（最后兜底）
```

> 为什么需要这些兜底：气压反演海拔的基准"海平面气压"随季节/天气在 980~1040 hPa 波动；
> 而部分手机 GPS 垂直定位未收敛时会返回 0（`hasAltitude()` 仍为 true），必须做有效性验证。

---

## 3. 环境要求

### 3.1 硬件与系统要求

| 项目 | 要求 |
|---|---|
| 操作系统 | Windows 10/11、macOS、Linux |
| 内存 | 建议 ≥ 8GB（Gradle + 编译器占用较高） |
| 磁盘空间 | 预留 ≥ 10GB（含 SDK 与依赖缓存） |
| 目标设备 | 具备**磁力计**的 Android 8.0+ 真机（模拟器指南针读数不准） |

### 3.2 软件要求

| 软件 | 版本要求 | 说明 |
|---|---|---|
| JDK | 17 及以上 | 项目编译目标为 Java 17 |
| Android SDK | Platform 34 | 需安装 SDK Platform 34 |
| Android Studio | Hedgehog (2023.1.1) 或更新 | 推荐 IDE，自带 SDK 管理 |
| Git | 任意近期版本 | 用于版本控制 / GitHub 上传 |

### 3.3 网络要求

- 首次构建需联网下载 Gradle 发行版与 Maven 依赖；
- 本项目已配置 **腾讯云 + 阿里云镜像**，国内网络可直接构建（见 4.2 镜像说明）。

---

## 4. 编译过程

### 4.1 构建方式总览

项目提供两种编译方式，任选其一：

| 方式 | 适用场景 | 说明 |
|---|---|---|
| **方式一：Android Studio** | 推荐，日常开发 | 图形界面，自动管理 SDK 与依赖 |
| **方式二：命令行 Gradle** | CI / 无 IDE 环境 | 使用 `gradlew` 脚本，跨平台 |

两种方式最终产物一致：`app/build/outputs/apk/debug/app-debug.apk`。

### 4.2 镜像说明（重要）

为保证国内网络下构建顺畅，项目已在 `settings.gradle.kts` 与 `gradle/wrapper/gradle-wrapper.properties` 中配置镜像：

- **Gradle 发行版**：腾讯镜像（`mirrors.cloud.tencent.com/gradle/`）
- **Maven 仓库**：腾讯云 + 阿里云 + Google 官方 + Maven Central 四路并存

> 无需任何手动修改即可在国内环境直接构建。

### 4.3 方式一：Android Studio 编译

1. 打开 Android Studio，选择 **Open**，选中 `CompassApp` 目录；
2. 等待 **Gradle Sync** 完成（首次会联网下载依赖，耗时较长）；
3. 确认 SDK 版本：`File → Project Structure → SDK Location` 或直接让 IDE 提示安装；
4. 如需生成调试 APK：菜单 **Build → Build Bundle(s) / APK(s) → Build APK(s)**；
5. 构建完成后点击通知栏 **locate** 即可找到 APK 文件。

### 4.4 方式二：命令行编译

#### 4.4.1 调试包（Debug）

```bash
# Windows（Git Bash / CMD）
gradlew.bat assembleDebug

# macOS / Linux
./gradlew assembleDebug
```

#### 4.4.2 发布包（Release，开启混淆与压缩）

```bash
# Windows
gradlew.bat assembleRelease

# macOS / Linux
./gradlew assembleRelease
```

> Release 包未配置签名，产物默认为**未签名 APK**；如需发布到应用商店，请先在
> `app/build.gradle.kts` 的 `buildTypes.release` 中配置 `signingConfig`。

#### 4.4.3 产物路径

```
app/build/outputs/apk/debug/app-debug.apk     # 调试包
app/build/outputs/apk/release/app-release-unsigned.apk   # 未签名发布包
```

#### 4.4.4 其他常用命令

```bash
./gradlew clean          # 清理构建产物
./gradlew build          # 完整构建（含 lint / 测试）
./gradlew lint           # 静态代码检查
./gradlew tasks          # 查看所有可用任务
```

### 4.5 编译常见报错速查

| 报错现象 | 可能原因 | 解决办法 |
|---|---|---|
| `SDK location not found` | 未设置 SDK 路径 | 项目根目录新建 `local.properties`，写入 `sdk.dir=/你的SDK路径` |
| `Unsupported class file major version` | JDK 版本过低 | 升级到 JDK 17+ |
| 依赖下载失败 | 网络受限 | 确认 4.2 节镜像配置生效，或更换网络 |
| `Could not find com.android.application` | AGP 版本冲突 | 检查 `build.gradle.kts` 插件版本 8.5.2 |

---

## 5. 安装过程

### 5.1 安装前准备

- 目标设备为 **Android 8.0（API 26）及以上**；
- 确认设备具备**磁力计**（传感器），否则指南针无法正常工作；
- 首次安装需在设备上允许"安装未知来源应用"。

### 5.2 方式一：真机直接安装（推荐）

1. 手机开启 **开发者选项 → USB 调试**；
2. 连接电脑，手机弹窗选择 **传输文件（MTP）**；
3. 将 `app-debug.apk` 拷贝到手机存储（或直接使用下面的 Gradle 命令）：

```bash
# 连接设备后一键安装到手机
./gradlew installDebug
```

4. 在手机上打开安装包，按提示完成安装。

### 5.3 方式二：使用 Gradle 命令安装

```bash
# 确保设备已连接且开启 USB 调试
./gradlew installDebug        # 安装调试包
./gradlew installRelease      # 安装发布包（需已配置签名）
```

### 5.4 首次启动与权限授予

安装后首次启动，按界面提示依次授予权限：

| 权限 | 用途 | 建议 |
|---|---|---|
| 定位（精确定位） | 经纬度、真北磁偏角修正、联网校准 | **允许**（始终） |
| 相机 | 预留 AR 实景罗盘（当前未使用） | 可拒绝 |
| 通知 / 其他 | 预留 | 按需 |

> 指南针功能本身不依赖定位权限；定位权限仅用于真北切换与海拔联网校准。若拒绝定位，
> 应用仍可显示磁北方向与气压海拔。

---

## 6. 运行与使用

### 6.1 页面结构

应用采用底部导航，共三个主页面：

| 页面 | 功能 |
|---|---|
| 🧭 指南针 | 表盘方位 + 读数 + 经纬度 / 气压 / 海拔 / 磁场信息栏 |
| 💧 水平仪 | 气泡式水平仪 + 俯仰 / 横滚角度 |
| 🎯 校准 | "8"字校准向导 + 磁场仪表 + 联网校准开关 |

### 6.2 指南针页操作

- **方位读取**：表盘上方显示当前方向（如 北 / 东北 / 东…）与精确角度；
- **真北 / 磁北切换**：获取定位后自动启用真北，可在界面手动切换；
- **海拔校准**：点击海拔数字，输入已知真实海拔进行手动校准；
- **告警**：磁场强度超出 20~70μT 正常范围时自动提示，可能受磁体干扰。

### 6.3 水平仪页操作

- 将手机平放，观察气泡移动；
- 当倾斜角 < 1° 时，气泡变绿，提示"完全水平"。

### 6.4 校准页操作

- 按屏幕动画引导，手持手机画"8"字，完成磁力计校准；
- 开启「联网校准」开关后，应用会联网获取当地海平面气压，提升海拔精度。

---

## 7. 项目结构

```
CompassApp/
├── app/
│   ├── build.gradle.kts                 # 应用模块构建配置（compileSdk 34 / minSdk 26）
│   ├── proguard-rules.pro               # 混淆规则
│   └── src/main/
│       ├── AndroidManifest.xml          # 权限与组件声明
│       ├── java/com/example/compass/
│       │   ├── MainActivity.kt          # 入口 + 底部导航 + 版权页脚
│       │   ├── sensor/
│       │   │   └── CompassViewModel.kt  # 传感器融合、方位角、定位、海拔校准、联网 QNH
│       │   └── ui/
│       │       ├── CompassScreen.kt     # 表盘 Canvas 绘制 + 读数 + 信息栏 + 海拔校准弹窗
│       │       ├── LevelScreen.kt       # 气泡水平仪
│       │       └── CalibrateScreen.kt   # 8 字校准向导 + 联网校准开关
│       └── res/
│           ├── drawable/                # 应用图标矢量资源
│           ├── mipmap-anydpi-v26/       # 自适应图标
│           └── values/                  # 字符串 / 主题资源
├── gradle/
│   └── wrapper/gradle-wrapper.properties  # Gradle 8.7 + 腾讯镜像
├── build.gradle.kts                     # 根构建脚本（插件版本声明）
├── settings.gradle.kts                  # 仓库镜像 + 模块声明
├── gradle.properties                    # JVM 参数与 AndroidX 配置
├── gradlew / gradlew.bat                # Gradle Wrapper 脚本
├── .gitignore                           # Git 忽略规则
├── README.md                            # 项目 README（GitHub 首页展示）
├── DOCUMENTATION.md                     # 本文档
├── preview.html                         # 界面效果预览（浏览器打开）
└── screenshots/                         # 运行截图（compass / level / calibrate）
```

---

## 8. GitHub 上传

> 本节为将当前 `CompassApp` 文件夹发布到 GitHub 的操作指引。

### 8.1 上传前检查（重要）

1. **确认 `.gitignore` 已生效**：项目根目录已包含 `.gitignore`，会忽略 `build/`、`.gradle/`、
   `local.properties`、`.idea/`、`*.apk` 等不应入库的文件；
2. **确认 `local.properties` 不入库**：该文件含本机 SDK 路径，属于个人环境，不要提交；
3. **确认没有敏感信息**：项目内无密钥 / 令牌，若后续添加请注意规避；
4. **可选：先本地构建验证**（第 4 章），确保仓库克隆后能正常编译。

### 8.2 方式一：命令行上传

```bash
# 1. 进入项目目录
cd CompassApp

# 2. 初始化仓库（若尚未 init）
git init

# 3. 查看待提交文件，确认没有 build/、local.properties 等
git status

# 4. 添加全部文件
git add .

# 5. 提交
git commit -m "feat: 智能指南针 CompassApp v1.0.0"

# 6. 关联远程仓库（替换为你的仓库地址）
git remote add origin https://github.com/<你的用户名>/CompassApp.git

# 7. 推送
git branch -M main
git push -u origin main
```

### 8.3 方式二：GitHub Desktop / Web 上传

- **GitHub Desktop**：`File → Add Local Repository` 选择项目，按提示提交并 `Publish repository`；
- **网页上传**：在 GitHub 新建仓库后，将整个文件夹拖入上传区即可（会跳过 `.gitignore` 规则，
  请务必预先删除 `build/` 等目录，避免上传体积过大）。

### 8.4 上传后完善

上传成功后，在仓库设置中可补充：

- **Repository description**：`功能强大的 Android 智能指南针 · Kotlin + Jetpack Compose`
- **Topics**：`android` `kotlin` `jetpack-compose` `compass` `material3`
- **Release**：可基于 `versionName 1.0.0` 打 tag 并附加说明

---

## 9. 常见问题

### 9.1 构建类

**Q：`SDK location not found` 如何解决？**
A：在项目根目录创建 `local.properties`，写入 `sdk.dir=/你的Android/Sdk路径`（该文件已被
`.gitignore` 忽略，不会入库）。

**Q：Gradle 下载依赖缓慢 / 失败？**
A：确认 `settings.gradle.kts` 与 `gradle-wrapper.properties` 中的腾讯 / 阿里镜像未被人为删除；
若公司网络仍受限，可在 `~/.gradle/init.gradle` 中追加镜像。

**Q：`Unsupported class file major version 61`？**
A：JDK 需为 17 及以上。`java -version` 检查后，在 `gradle.properties` 或 IDE 的
`Gradle JVM` 设置中指定 JDK 17。

### 9.2 安装 / 运行类

**Q：安装提示"应用未安装"？**
A：可能是签名冲突或系统版本过低。确认设备 ≥ Android 8.0，且卸载旧版本后重装；
调试包与发布包签名不同，不能覆盖安装。

**Q：指南针不动或读数不准？**
A：确认设备具备磁力计（模拟器通常没有）；避开磁体 / 金属；到「校准」页画"8"字重新校准。

**Q：海拔显示为 0 或明显异常？**
A：GPS 垂直定位未收敛或信号弱。请到空旷处等待定位收敛，或使用「手动海拔校准」输入已知海拔。

### 9.3 传感器类

**Q：哪些设备能用？**
A：所有带磁力计、气压计、GPS 的 Android 8.0+ 真机。无气压计时，应用自动走 GPS 海拔兜底；
无 GPS 时走标准大气兜底。

---

## 附录 A：版本记录

| 版本 | 日期 | 说明 |
|---|---|---|
| 1.0.0 | 2026-11-04 | 首个发布版本：指南针 / 水平仪 / 校准 / 海拔多级兜底 |

---

*智能指南针 CompassApp · 小虫工作室 @2026*
