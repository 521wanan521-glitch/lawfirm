# 律所系统移动端 App 打包指南

本移动端基于 **uni-app（Vue3 + Vite）**，一套代码可打包为 **安卓 App、iOS App、原生鸿蒙 App**，以及 H5、微信小程序。

> 打包需要 **HBuilderX**（DCloud 官方图形化工具）和对应的开发者账号。代码已就绪，照着下面步骤操作即可。

---

## 一、前置准备（一次性）

1. **下载 HBuilderX**
   - 官网：https://www.dcloud.io/hbuilderx.html
   - 选「HBuilderX 正式版」（Windows / Mac）

2. **注册 DCloud 账号**（免费）
   - https://dev.dcloud.net.cn
   - 云打包需要登录此账号

3. **获取 AppID**
   - 在 HBuilderX 里打开项目后，`manifest.json` → 基础配置 → 点击「重新获取」AppID（用 DCloud 账号登录后自动获取）

4. **准备图标**
   - `manifest.json` → App 图标配置，上传一张 1024×1024 的 Logo 图（可让我生成或自己设计）

---

## 二、用 HBuilderX 打开项目

1. 打开 HBuilderX
2. 菜单「文件」→「打开目录」→ 选择本项目的 `mobile` 目录
   （路径：`lawfirm/mobile`，即包含 `src/`、`package.json` 的那一层）
3. 项目加载完成后，左侧能看到 `pages`、`pages.json`、`manifest.json` 等

---

## 三、打包安卓 App（apk）

**方式 A：云打包（推荐，无需本地环境）**

1. 菜单「发行」→「原生App-云打包」
2. 弹窗里选「Android」，勾选「使用公共测试证书」或上传自己的证书
3. 点击「打包」，等待云端完成（几分钟到几十分钟）
4. 完成后下载 apk，发给用户安装即可

**方式 B：本地打包（需要 Android Studio + JDK）**

- 参考 DCloud 离线打包文档（较复杂，不推荐新手）

> 云打包需要「打包服务费」：个人开发者每月有免费额度，超出需充值（价格不高）。

---

## 四、打包 iOS App（ipa）

> ⚠️ iOS 打包门槛最高：**必须 Apple 开发者账号（个人 $99/年）**，否则只能出测试包、无法上架 App Store。

1. 准备 Apple 开发者账号 + 证书（在 Apple 开发者后台创建）
2. HBuilderX「发行」→「原生App-云打包」→ 选「iOS」
3. 上传/配置签名证书（p12 + 描述文件）
4. 打包完成后得到 ipa
5. 上架：用 Transporter 或 Xcode 上传到 App Store Connect

> 没有 Apple 账号时，可以先只做安卓 + 鸿蒙 + H5（手机浏览器）版本。

---

## 五、打包原生鸿蒙 App（HarmonyOS）

1. 用 **HBuilderX 4.0+**（新版已支持鸿蒙）
2. 菜单「发行」→「鸿蒙」相关选项
3. 需要 **华为开发者账号**（免费注册，在华为开发者联盟）
4. 按提示配置签名证书
5. 打包得到 `.app`/`.hap` 安装包

> 鸿蒙打包在 HBuilderX 里跟随官方更新，具体菜单名以当前版本为准（「发行 → 原生App-云打包」或「发行 → 鸿蒙App云打包」）。

---

## 六、H5（手机浏览器，已经部署好）

- 线上地址：`http://47.107.62.86:8089/`
- 手机浏览器直接打开即用，无需安装

---

## 七、常见问题

1. **打包前先本地验证**：`npm run dev:h5` 浏览器预览没问题再打包。
2. **接口地址**：移动端后端地址已配置为 `http://47.107.62.86/api`（在 `src/utils/request.js` 的 `BASE_URL`）。若换服务器，改这里即可。
3. **AppID 为空**：首次打包必须先在 `manifest.json` 里获取 AppID。
4. **图标缺失**：`manifest.json` → App 图标配置里上传图标，否则用默认图标。

---

## 八、需要我做的

如果你把 **DCloud 账号** 和 **HBuilderX** 准备好，但仍卡在某一步（比如证书、图标、manifest 配置），把报错发我，我帮你排查。图标我可以帮你生成一张。
