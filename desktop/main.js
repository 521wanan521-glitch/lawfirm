// 律所办公系统 Windows 桌面客户端（Electron 外壳）
// 打开的网址优先级：环境变量 LAWAPP_URL > config.json > 默认 http://localhost:5173
const { app, BrowserWindow, Tray, Menu, shell, nativeImage } = require('electron')
const fs = require('fs')
const path = require('path')

const DEFAULT_URL = 'http://localhost:5173'

function resolveUrl() {
  if (process.env.LAWAPP_URL) {
    return process.env.LAWAPP_URL
  }
  const candidates = [
    path.join(__dirname, 'config.json'),                        // 开发目录
    path.join(process.resourcesPath || '', 'config.json'),      // 安装包 resources 目录
    path.join(path.dirname(process.execPath), 'config.json')    // 安装目录
  ]
  for (const p of candidates) {
    try {
      if (fs.existsSync(p)) {
        const cfg = JSON.parse(fs.readFileSync(p, 'utf-8'))
        if (cfg && cfg.url) {
          return cfg.url
        }
      }
    } catch (e) {
      /* ignore */
    }
  }
  return DEFAULT_URL
}

let mainWindow = null
let tray = null
let quitting = false

function createWindow() {
  mainWindow = new BrowserWindow({
    width: 1280,
    height: 860,
    minWidth: 1024,
    minHeight: 700,
    title: '律所办公系统',
    icon: path.join(__dirname, 'icon.png'),
    autoHideMenuBar: true,
    show: false,
    webPreferences: {
      contextIsolation: true,
      nodeIntegration: false,
      sandbox: true
    }
  })
  mainWindow.loadURL(resolveUrl())
  mainWindow.once('ready-to-show', () => mainWindow.show())

  // 新窗口/外部链接用系统浏览器打开
  mainWindow.webContents.setWindowOpenHandler(({ url }) => {
    if (/^https?:/.test(url)) {
      shell.openExternal(url)
    }
    return { action: 'deny' }
  })

  // 点关闭按钮时最小化到托盘，不退出
  mainWindow.on('close', (e) => {
    if (!quitting) {
      e.preventDefault()
      mainWindow.hide()
    }
  })
  mainWindow.on('closed', () => {
    mainWindow = null
  })
}

function showWindow() {
  if (!mainWindow) {
    createWindow()
    return
  }
  if (mainWindow.isMinimized()) {
    mainWindow.restore()
  }
  mainWindow.show()
  mainWindow.focus()
}

function createTray() {
  const img = nativeImage
    .createFromPath(path.join(__dirname, 'icon.png'))
    .resize({ width: 16, height: 16 })
  tray = new Tray(img)
  tray.setToolTip('律所办公系统')
  const menu = Menu.buildFromTemplate([
    { label: '打开律所办公系统', click: showWindow },
    {
      label: '开机自启',
      type: 'checkbox',
      checked: app.getLoginItemSettings().openAtLogin,
      click: (item) => app.setLoginItemSettings({ openAtLogin: item.checked })
    },
    { label: '刷新页面', click: () => mainWindow && mainWindow.webContents.reload() },
    { type: 'separator' },
    {
      label: '退出',
      click: () => {
        quitting = true
        app.quit()
      }
    }
  ])
  tray.setContextMenu(menu)
  tray.on('click', showWindow)
}

app.whenReady().then(() => {
  createWindow()
  createTray()
  app.on('activate', () => {
    if (BrowserWindow.getAllWindows().length === 0) {
      createWindow()
    }
  })
})

app.on('before-quit', () => {
  quitting = true
})

// 常驻托盘：窗口全部关闭时不退出，通过托盘菜单退出
app.on('window-all-closed', () => {
  /* keep running in tray */
})
