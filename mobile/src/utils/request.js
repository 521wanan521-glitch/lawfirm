// 统一请求封装（uni.request）
// 后端接口与 web 端一致，统一返回 { code, data, message }

// 后端地址：默认线上服务器，可在此修改
const BASE_URL = 'http://47.107.62.86/api'

export function request(options) {
  const { url, method = 'GET', data = {}, showLoading = false } = options

  if (showLoading) {
    uni.showLoading({ title: '加载中...', mask: true })
  }

  const token = uni.getStorageSync('token') || ''

  return new Promise((resolve, reject) => {
    uni.request({
      url: BASE_URL + url,
      method,
      data,
      timeout: 60000,
      header: {
        'Content-Type': 'application/json',
        Authorization: token ? `Bearer ${token}` : ''
      },
      success: (res) => {
        if (res.statusCode === 200) {
          const body = res.data
          if (body && body.code === 0) {
            resolve(body.data)
          } else {
            const msg = (body && body.message) || '请求失败'
            uni.showToast({ title: msg, icon: 'none' })
            reject(new Error(msg))
          }
        } else if (res.statusCode === 401) {
          uni.removeStorageSync('token')
          uni.removeStorageSync('user')
          uni.showToast({ title: '登录已过期，请重新登录', icon: 'none' })
          setTimeout(() => {
            uni.reLaunch({ url: '/pages/login/login' })
          }, 600)
          reject(new Error('未登录'))
        } else {
          const msg = (res.data && res.data.message) || `网络异常(${res.statusCode})`
          uni.showToast({ title: msg, icon: 'none' })
          reject(new Error(msg))
        }
      },
      fail: (err) => {
        uni.showToast({ title: '网络连接失败', icon: 'none' })
        reject(err)
      },
      complete: () => {
        if (showLoading) uni.hideLoading()
      }
    })
  })
}

export const get = (url, params) => request({ url, method: 'GET', data: params })
export const post = (url, data) => request({ url, method: 'POST', data })
export const put = (url, data) => request({ url, method: 'PUT', data })
export const del = (url) => request({ url, method: 'DELETE' })

export default request
