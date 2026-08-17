# 律所办公系统 Windows 客户端（Electron 外壳）

把 Web 系统包成一个 Windows 桌面应用：双击打开、独立窗口、系统托盘、开机自启。

## 打开哪个网址（优先级从高到低）

1. 环境变量 `LAWAPP_URL`
2. `config.json` 的 `url` 字段
3. 默认 `http://localhost:5173`（本地开发）

**部署到服务器后**：修改 `config.json` 为正式地址，例如：

```json
{ "url": "https://lawfirm.example.com" }
```

安装包会把 `config.json` 放到安装目录的 `resources\config.json`，管理员直接改这个文件即可切换服务器地址。

## 本地开发

```bash
cd desktop
npm install
npm start
```

## 打包 Windows 安装程序（exe）

```bash
npm run dist
```

产物在 `desktop/dist/lawfirm-setup-1.0.0.exe`（NSIS 安装包，可选安装目录，自动建桌面/开始菜单快捷方式）。

> 国内网络构建慢时，可先设置镜像加速：
>
> ```bash
> set ELECTRON_MIRROR=https://npmmirror.com/mirrors/electron/
> set ELECTRON_BUILDER_BINARIES_MIRROR=https://npmmirror.com/mirrors/electron-builder-binaries/
> ```

## 功能

- 独立窗口（1280×860，可缩放）
- 点关闭按钮最小化到系统托盘，双击托盘图标恢复
- 托盘菜单：打开 / 开机自启 / 刷新页面 / 退出
- 页面中的外部链接用系统默认浏览器打开
- 登录数据与浏览器独立（各自登录）

## 说明

客户端只是一个"壳"，业务和更新都在服务器端，客户端无需随业务发版。
