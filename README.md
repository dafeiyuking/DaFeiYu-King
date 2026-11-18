# 大肥鱼King · 两个版本说明

本仓库同时保留了 **4.1.0** 和 **3.3.1** 两条分支。两者**包名、驱动版本、配套 `.ko`、签名都不同，不能混装**。

| | **3.3.1 分支（新）** | **4.1.0 分支（旧）** |
|---|---|---|
| 上游 | 官方 **KernelSU v3.3.1** | **ReSukiSU 4.1.0（SukiSU Ultra）** |
| 管理器包名 | `com.dfyking` | `com.dafeiyu.king` |
| 管理器版本 | `3.3.1 (32680)` | `4.1.0 (72164)` |
| 内核驱动版本 | `32680` | `72164` |
| 配套 LKM | 8 个 KMI，`KSU_PACKAGE_NAME=com.dfyking`、fish887 证书 | 8 个 KMI，4.1.0 配套包名/证书 |
| 卡片透明度 | 40% / 60% / 80% / 100% 四档 | 0–100% 连续可调 |
| Release | [v3.3.1-dfyking](https://github.com/dafeiyuking/DaFeiYu-King/releases/tag/v3.3.1-dfyking) | [大肥鱼King 4.1.0](https://github.com/dafeiyuking/DaFeiYu-King/releases/tag/v4.1.0-dafeiyu) |
| 源码 | Release 里的 `DaFeiYu-King-3.3.1-source.tar.gz` | 本仓库 `main` |

## 怎么选

- 想用**官方 KernelSU v3.3.1** 底座、包名 `com.dfyking`、更新检查走本仓库：选 **3.3.1**。
- 已经在用 **4.1.0 / ReSukiSU**，或需要 0–100% 连续透明度：继续用 **4.1.0**。
- 不要拿 3.3.1 的 APK 配 4.1.0 的 `.ko`（反之亦然），内核会不认管理器。

## 3.3.1 分支

- 管理器：`3.3.1 (32680)`，包名 `com.dfyking`，签名 fish887（887 字节）
- 驱动：`32680`
- 8 个 KMI：`android12-5.10` / `android13-5.10` / `android13-5.15` / `android14-5.15` / `android14-6.1` / `android15-6.6` / `android16-6.12` / `android17-6.18`
- 特性：半透明淡蓝 UI、默认壁纸、卡片透明度四档、ROOT 终端、DeepSeek、8 个 `.ko` 内置在 `libksud.so`，直接安装按 KMI 自动选择
- 安装：装 APK → 安装 → 直接安装（推荐）/ 使用本地 LKM → 重启
- 一加 Ace 6 当前 ROM 可直接刷 Release 里的 `init_boot/init_boot_dfyking_32680.img` 到 `init_boot_b`
- 只有 `android15-6.6` 实机验证；其余 7 个只做了编译 + 版本/包名/证书/vermagic 校验
- **2026-11-18 修复**：重编的 `libksud.so` 缺少 `ksuinit`，刷写会报 `Failed to load ksuinit`；当前 APK / zip 已修复，请重新下载。

## 4.1.0 分支

- 管理器：`4.1.0 (72164)`，包名 `com.dafeiyu.king`
- 基于 **ReSukiSU 4.1.0（SukiSU Ultra）**，源码在本仓库 `main`
- 8 个 KMI，配套 4.1.0 的 `.ko`
- 特性：主色 `#4D6BFE`、28dp 圆角、卡片 0–100% 连续透明、Fit 壁纸 + 模糊铺底、ROOT 终端、DeepSeek
- 安装：装 APK → 安装 → 直接安装（推荐）/ 使用本地 LKM → 重启

## 支持的 KMI

| KMI | 对应系统 |
|---|---|
| `android12-5.10` | Android 12 |
| `android13-5.10` / `android13-5.15` | Android 13 |
| `android14-5.15` / `android14-6.1` | Android 14 |
| `android15-6.6` | Android 15 / 16 |
| `android16-6.12` | Android 16 |
| `android17-6.18` | Android 17 |

非 GKI 老内核（4.14 / 4.19 / 5.4）用不了预编 `.ko`。

## 构建

- **3.3.1**：源码在 Release 的 `DaFeiYu-King-3.3.1-source.tar.gz`；管理器用 JDK 21 + Gradle 9.7.1，`manager/` 下 `assembleRelease`。ksud 用 `tiann/KernelSU` v3.3.1 + 8 个 `.ko`、`KSU_PACKAGE_NAME=com.dfyking` 构建。
- **4.1.0**：源码在本仓库 `main`；管理器用 JDK 17 + Gradle 9.2.0。

发版必须保证「APK 签名/包名 = 内核 `.ko` 的 `KSU_EXPECTED_*` / `KSU_PACKAGE_NAME`」，否则内核不认管理器。

## 许可证 / 素材

- 上游 KernelSU / ReSukiSU 均为 GPL-3.0；本分支修改部分同样 GPL-3.0
- 动漫形象素材版权归原作者（怡子曰曰 / 明风 OuO），CC BY-NC-SA 4.0，不可商用

## 相关

- 反馈 / 更新频道：<https://t.me/dafeiyuking>
- 内核模块编译仓库：<https://github.com/dafeiyuking/dafeiyu-lkm>
