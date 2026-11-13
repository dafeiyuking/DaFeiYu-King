# 大肥鱼King · KernelSU 管理器

基于 **ReSukiSU 4.1.0（SukiSU Ultra）** 修改的 KernelSU 管理器分支，GPL-3.0。

- 管理器 APK：`com.dafeiyu.king`，应用名 **大肥鱼King**
- 配套内核模块：覆盖 **Android 12 ~ 17**（8 个 KMI）
- 下载：见本仓库 [Releases](https://github.com/dafeiyuking/DaFeiYu-King/releases/latest)

## 这一分支改了什么

### 界面
- 改名、换图标，去掉所有上游 SukiSU 字样
- 主色 `#4D6BFE`、28dp 圆角；卡片半透明，不透明度 0–100% 可调
- 内置壁纸：Fit 完整显示 + 同图模糊铺底；首页状态卡用大肥鱼头图
- 深色模式逐页适配（不是简单反色）

### 新功能
- **ROOT 终端**：常驻 root shell、脚本导入运行、ANSI 颜色、IME 不遮挡输入
- **DeepSeek 对话**：自己的 API Key、余额查询、多轮对话；Key 只存本机

### 内核侧
- 内置 `libksud.so`，模块页 / feature 不再空
- **内置 8 个 `.ko`**，安装页按当前 KMI 自动选择，`直接安装` 一步刷入
- 刷完内核原生信任本 App，**不需要再跑 `dynamic-manager set`**

## 支持的 KMI

| KMI | 对应系统 |
|---|---|
| `android12-5.10` | Android 12 |
| `android13-5.10` / `android13-5.15` | Android 13 |
| `android14-5.15` / `android14-6.1` | Android 14 |
| `android15-6.6` | Android 15 / 16 |
| `android16-6.12` | Android 16 |
| `android17-6.18` | Android 17 |

非 GKI 老内核（4.14 / 4.19 / 5.4）用不了预编 `.ko`，只能用管理器 + `dynamic-manager`。

## 安装

1. 装 APK（手机需已有 KernelSU root）
2. 管理器 → 安装 → LKM 修补/安装 → 直接安装（推荐）
3. 重启，首页显示「工作中」即完成

⚠️ 刷写会改 `init_boot`，请先备份；刷完「超级用户」授权列表可能被清空，刷前记一下。

## 自行构建

| 组件 | 版本 |
|---|---|
| JDK | 17 |
| Gradle | 9.2.0 |
| Android SDK | compileSdk 36 / build-tools 36.1.0 |
| NDK | 29.0.14206865 |

```powershell
$env:JAVA_HOME="<jdk-17>"
$env:ANDROID_HOME="<android-sdk>"
cd manager
gradle assembleRelease -Pcommit=2c68261c `
  "-PKEYSTORE_FILE=<你的.jks>" "-PKEYSTORE_PASSWORD=<密码>" `
  "-PKEY_ALIAS=<别名>" "-PKEY_PASSWORD=<密码>"
```

`.ko` 用 GitHub Actions 编：见 `dist` 里的 `build-lkm.yml` / `LKM-怎么编.md`，容器 `ghcr.io/ylarod/ddk-min:<kmi>-20260828`。

> 自己发版必须换证书：改 `kernel/Kbuild` 的 `KSU_EXPECTED_HASH` / `KSU_EXPECTED_SIZE`，
> 并用同一证书重签 APK，否则内核不认。

## 许可证

- 上游：ReSukiSU / SukiSU Ultra，GPL-3.0；本分支修改部分同样 GPL-3.0
- 动漫形象素材版权归原作者（怡子曰曰 / 明风 OuO），CC BY-NC-SA 4.0，不可商用

## 相关

- 反馈 / 更新频道：<https://t.me/dafeiyuking>
- 内核模块编译仓库：<https://github.com/dafeiyuking/dafeiyu-lkm>
