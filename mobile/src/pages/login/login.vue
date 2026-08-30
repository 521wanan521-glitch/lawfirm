<template>
  <view class="login-page">
    <view class="brand">
      <text class="logo">律所办公系统</text>
      <text class="slogan">案件 · 客户 · 日程 · 审批，移动办公随身行</text>
    </view>

    <view class="form-card">
      <view class="field">
        <text class="label">账号</text>
        <input class="input" v-model="username" placeholder="请输入用户名" placeholder-class="ph" />
      </view>
      <view class="field">
        <text class="label">密码</text>
        <input class="input" v-model="password" type="password" placeholder="请输入密码" placeholder-class="ph" />
      </view>

      <view class="login-btn" @click="doLogin">登 录</view>
    </view>

    <view class="tips">首次使用请联系管理员开通账号</view>
  </view>
</template>

<script>
import { login } from '@/api/index'
import { useUserStore } from '@/store/index'

export default {
  data() {
    return {
      username: '',
      password: '',
      loading: false
    }
  },
  methods: {
    async doLogin() {
      if (!this.username.trim() || !this.password) {
        uni.showToast({ title: '请输入账号和密码', icon: 'none' })
        return
      }
      if (this.loading) return
      this.loading = true
      try {
        const data = await login({ username: this.username.trim(), password: this.password })
        const store = useUserStore()
        store.setLogin(data.token, data.user)
        uni.showToast({ title: '登录成功', icon: 'success' })
        setTimeout(() => uni.switchTab({ url: '/pages/index/index' }), 400)
      } catch (e) {
        // 错误已由 request 层提示
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.login-page {
  min-height: 100vh;
  background: linear-gradient(160deg, #2f6fed 0%, #4a8bf5 52%, #f5f6f8 52.1%);
  padding: 0 48rpx;

  .brand {
    padding: 160rpx 0 70rpx;
    color: #fff;

    .logo {
      font-size: 56rpx;
      font-weight: 800;
      display: block;
      letter-spacing: 2rpx;
    }
    .slogan {
      font-size: 26rpx;
      opacity: 0.92;
      margin-top: 16rpx;
      display: block;
    }
  }

  .form-card {
    background: #fff;
    border-radius: 24rpx;
    padding: 44rpx 36rpx;
    box-shadow: 0 12rpx 40rpx rgba(47, 111, 237, 0.16);

    .field {
      margin-bottom: 32rpx;

      .label {
        font-size: 26rpx;
        color: #606266;
        display: block;
        margin-bottom: 12rpx;
      }
      .input {
        background: #f5f6f8;
        border-radius: 14rpx;
        padding: 24rpx;
        font-size: 30rpx;
      }
      .ph {
        color: #c0c4cc;
      }
    }

    .login-btn {
      height: 92rpx;
      line-height: 92rpx;
      text-align: center;
      color: #fff;
      font-size: 32rpx;
      font-weight: 600;
      border-radius: 46rpx;
      background: linear-gradient(90deg, #2f6fed, #4a8bf5);
      box-shadow: 0 8rpx 20rpx rgba(47, 111, 237, 0.3);
      margin-top: 12rpx;
    }
  }

  .tips {
    text-align: center;
    color: #a8abb2;
    font-size: 24rpx;
    margin-top: 40rpx;
  }
}
</style>
