# 大肥鱼King KSU 管理器 · 源码（GPL-3.0）

本仓库是 **大肥鱼King** 管理器的完整源码，基于 **ReSukiSU 4.1.0（SukiSU Ultra）** 修改而来，
按 **GPL-3.0** 发布。分发 APK 或 `.ko` 时请一并提供本源码。

- 管理器 APK：`DaFeiYu-KSU-4.1.0.apk`（包名 `com.dafeiyu.king`）
- 内核模块：`kernelsu-android15-6.6-dafeiyu.ko` 等（见 Release）
- 源码包：`BakaSU-4.1.0-dafeiyu-src.zip`（本仓库 Release 里下载，解压即完整工程）

## 这一分支改了什么

详见源码包里的 `KSU-分支-交接.md`，摘要：

1. **改名/换皮**：`app_name` → 大肥鱼King，图标换成大肥鱼，去掉所有 SukiSU 字样
2. **UI**：主色 `#4D6BFE`、卡片 28dp 圆角、半透明卡片（「卡片不透明度」可调 0–100%）、
   内置壁纸（Fit 完整显示 + 同图模糊铺底）、「工作中」状态卡用大肥鱼头图
3. **ROOT 终端**（新增页）：常驻 root shell、脚本导入运行、ANSI 颜色
4. **DeepSeek 对话**（新增页）：自己的 API Key、余额查询、`deepseek-chat` 多轮对话
5. **内置 ksud**：`libksud.so` 打进 APK，模块页/feature 可用；
   `ksud boot-patch --magiskboot` 等旧参数已按新 ksud CLI 修正
6. **内置 `.ko`**：`assets/lkm/` 放 3 个 KMI 的模块，安装页按内核自动选择，
   `ksud boot-patch` 直接刷入，不再需要手动跑 `dynamic-manager set`

## 构建（Windows 实测）

环境：

| 组件 | 版本 |
|---|---|
| JDK | 17（AGP 8.13.1 可用；JDK 21 也行） |
| Gradle | 9.2.0 |
| Android SDK | compileSdk 36、build-tools 36.1.0 |
| NDK | 29.0.14206865 |

```powershell
$env:JAVA_HOME="<jdk-17>"
$env:ANDROID_HOME="<android-sdk>"
cd manager
gradle assembleRelease -Pcommit=2c68261c `
  "-PKEYSTORE_FILE=<你的.jks>" "-PKEYSTORE_PASSWORD=<密码>" `
  "-PKEY_ALIAS=<别名>" "-PKEY_PASSWORD=<密码>"
```

产物：`manager/app/build/outputs/apk/release/SukiSU_4.1.0_72164-release.apk`

> 签名说明：内核只信任编译时写死的证书哈希（`EXPECTED_HASH`/`EXPECTED_SIZE`）。
> 你要自己发版，需要改 `kernel/Kbuild` 里的 `KSU_EXPECTED_HASH` / `KSU_EXPECTED_SIZE`
> 并用**你自己的**证书重签 APK，然后重编 `.ko`。

## `.ko` 怎么编

见源码包里的 `LKM-怎么编.md`：GitHub Actions + `build-lkm.yml`，容器 `ghcr.io/ylarod/ddk-min:<kmi>-20260828`。
本分支已用同样流程编过 6.6 / 6.12 / 6.18 三个 KMI。

## 许可证

- 上游：ReSukiSU / SukiSU Ultra，GPL-3.0
- 本分支修改部分同样以 GPL-3.0 发布，源码见本仓库
- 动漫形象素材版权归原作者（怡子曰曰 / 明风 OuO），CC BY-NC-SA 4.0，不可商用
