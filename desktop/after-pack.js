// 构建钩子：打包完成后给主程序 exe 写入图标与版本信息。
// 由于本机构建环境无法创建符号链接（winCodeSign 解压失败），
// 已通过 signAndEditExecutable:false 跳过 electron-builder 自带的 rcedit/sign 流程，
// 这里改用本地 rcedit 单独处理，效果等同。
const { execFileSync } = require('child_process')
const path = require('path')

exports.default = async function (context) {
  if (context.electronPlatformName !== 'win32') {
    return
  }
  const rcedit = path.join(__dirname, 'tools', 'rcedit-x64.exe')
  const exe = path.join(context.appOutDir, context.packager.appInfo.productFilename + '.exe')
  const ico = path.join(__dirname, 'icon.ico')
  try {
    execFileSync(rcedit, [
      exe,
      '--set-icon', ico,
      '--set-version-string', 'ProductName', '律所办公系统',
      '--set-version-string', 'FileDescription', '律所数字化办公系统',
      '--set-version-string', 'CompanyName', 'LawFirm',
      '--set-version-string', 'LegalCopyright', 'LawFirm',
      '--set-file-version', '1.0.0.0',
      '--set-product-version', '1.0.0.0'
    ])
    console.log('[after-pack] 已写入图标与版本信息:', exe)
  } catch (e) {
    console.warn('[after-pack] rcedit 执行失败（不影响打包继续）:', e.message)
  }
}
