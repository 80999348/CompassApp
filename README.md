# 智能指南针 CompassApp（Android）

功能强大的 Android 指南针应用，基于 **Kotlin + Jetpack Compose + Material 3** 开发，参考市面主流指南针 App 设计。

## 功能清单

| 功能 | 说明 |
|---|---|
| 🧭 实时指南针 | 旋转矢量传感器融合定位，精确到 1°，表盘平滑动画（低通滤波 + Compose 动画插值） |
| 🌐 真北/磁北切换 | 获取定位后自动启用真北；通过 GNSS 位置 + 地磁模型（GeomagneticField）计算磁偏角修正 |
| 📍 经纬度显示 | 实时 GNSS 定位，度分秒格式（如 北纬 31°48′11″ / 东经 117°17′14″） |
| 🌡 气压与海拔 | 气压计实时气压（百帕）；海拔多级兜底（见下方"海拔算法"） |
| 💧 水平仪 | 气泡式水平仪，倾斜 <1° 变绿提示"完全水平" |
| 📐 姿态显示 | 实时俯仰 / 横滚角度 |
| 🧲 磁场监测 | 实时磁场强度（μT），超出 20~70μT 自动告警 |
| 🎯 校准向导 | 画"8"字动画引导 + 磁场强度仪表 + 传感器状态提示 |
| 🌐 联网校准海拔 | 通过 Open-Meteo 接口获取当地海平面气压（QNH），提升气压海拔精度 |
| ✍️ 手动海拔校准 | 点击海拔数字输入已知真实海拔，锁存修正量 |
| 🎨 深色主题 | 全局深色 UI，夜间友好，高对比度表盘 |

## 海拔算法（多级兜底）

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

## 项目结构

```
CompassApp/
├── app/src/main/java/com/example/compass/
│   ├── MainActivity.kt                 # 入口 + 底部导航（指南针/水平仪/校准）+ 版权页脚
│   ├── sensor/CompassViewModel.kt      # 传感器融合、方位角计算、定位、海拔多级校准、联网 QNH
│   └── ui/
│       ├── CompassScreen.kt            # 表盘 Canvas 绘制 + 读数 + 底部信息栏 + 海拔校准弹窗
│       ├── LevelScreen.kt              # 气泡水平仪
│       └── CalibrateScreen.kt          # 8 字校准向导 + 联网校准开关
├── app/src/main/AndroidManifest.xml    # 权限：定位、网络
├── app/build.gradle.kts                # minSdk 26 / targetSdk 34
├── screenshots/                        # 运行截图
└── preview.html                        # 界面效果预览（浏览器打开）
```

## 构建运行

### 方式一：Android Studio（推荐）

1. 用 Android Studio（Hedgehog 或更新）打开 `CompassApp` 目录
2. 等待 Gradle Sync 完成（需联网下载依赖）
3. 连接真机（**指南针必须有磁力计，模拟器读数不准**），点击 Run ▶

### 方式二：命令行

```bash
cd CompassApp
./gradlew assembleDebug        # 产物：app/build/outputs/apk/debug/app-debug.apk
./gradlew installDebug         # 直接安装到已连接设备
```

> 若网络受限，Gradle 发行版已配置为腾讯镜像；Maven 仓库已配置腾讯云 + 阿里云镜像。

## 权限说明

| 权限 | 用途 |
|---|---|
| ACCESS_FINE_LOCATION / COARSE | 经纬度显示、真北磁偏角修正、联网校准获取当地气压 |
| INTERNET | 联网校准（获取当地海平面气压 QNH） |
| CAMERA | 预留（AR 实景罗盘，当前未使用） |

## 可扩展方向

- 相机 AR 实景罗盘（已预留 CAMERA 权限，可引入 CameraX 叠加表盘）
- 角度拍照记录、方位角测量锁定
- 太阳/月亮方位指示、Qibla 朝向
- 地图导航模式（结合方位角与地图旋转）
- 气压趋势图 / 高度计记录

## 界面

见 `screenshots/` 目录，或在浏览器打开 `preview.html` 查看三页布局预览。

---

小虫工作室 @2026
