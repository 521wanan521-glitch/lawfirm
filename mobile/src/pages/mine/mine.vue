<template>
  <view class="mine">
    <!-- 用户卡片 -->
    <view class="user-card">
      <view class="avatar">{{ avatarText }}</view>
      <view class="user-info">
        <text class="name">{{ store.displayName || '未登录' }}</text>
        <text class="role">{{ roleLabel(store.role) }}{{ store.user && store.user.department ? ' · ' + store.user.department : '' }}</text>
      </view>
    </view>

    <!-- 功能入口 -->
    <view class="menu-card">
      <view class="menu-item" @click="go('/pages/client/list')">
        <text class="m-icon">👥</text>
        <text class="m-name">客户管理</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/approval/list')">
        <text class="m-icon">✍️</text>
        <text class="m-name">审批流程</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="changePwd">
        <text class="m-icon">🔒</text>
        <text class="m-name">修改密码</text>
        <text class="m-arrow">›</text>
      </view>
    </view>

    <!-- 退出登录 -->
    <view class="logout" @click="doLogout">退出登录</view>

    <view class="version">律所办公系统 移动端 v1.0.0</view>
  </view>
</template>

<script>
import { changePassword } from '@/api/index'
import { useUserStore } from '@/store/index'
import { roleLabel } from '@/utils/dict'

export default {
  data() {
    return {
      showPwd: false,
      pwdForm: { oldPassword: '', newPassword: '', confirm: '' }
    }
  },
  computed: {
    store() {
      return useUserStore()
    },
    avatarText() {
      return (this.store.displayName || '律').charAt(0)
    }
  },
  methods: {
    roleLabel,
    go(url) {
      uni.navigateTo({ url })
    },
    changePwd() {
      uni.showModal({
        title: '修改密码',
        editable: true,
        placeholderText: '请输入原密码',
        success: (res1) => {
          if (!res1.confirm || !res1.content) return
          const oldPassword = res1.content
          uni.showModal({
            title: '修改密码',
            editable: true,
            placeholderText: '请输入新密码（至少6位）',
            success: async (res2) => {
              if (!res2.confirm || !res2.content) return
              const newPassword = res2.content
              if (newPassword.length < 6) return uni.showToast({ title: '密码至少6位', icon: 'none' })
              try {
                await changePassword({ oldPassword, newPassword })
                uni.showToast({ title: '修改成功，请重新登录', icon: 'success' })
                setTimeout(() => {
                  this.store.logout()
                  uni.reLaunch({ url: '/pages/login/login' })
                }, 800)
              } catch (e) {}
            }
          })
        }
      })
    },
    doLogout() {
      uni.showModal({
        title: '退出登录',
        content: '确定退出当前账号吗？',
        success: (res) => {
          if (res.confirm) {
            this.store.logout()
            uni.reLaunch({ url: '/pages/login/login' })
          }
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.mine {
  padding-bottom: 60rpx;

  .user-card {
    background: linear-gradient(135deg, #2f6fed, #4a8bf5);
    padding: 60rpx 32rpx;
    display: flex;
    align-items: center;
    color: #fff;

    .avatar {
      width: 120rpx;
      height: 120rpx;
      border-radius: 50%;
      background: rgba(255, 255, 255, 0.25);
      display: flex;
      align-items: center;
      justify-content: center;
      font-size: 48rpx;
      font-weight: 700;
      margin-right: 24rpx;
    }
    .user-info {
      display: flex;
      flex-direction: column;

      .name {
        font-size: 38rpx;
        font-weight: 700;
      }
      .role {
        font-size: 26rpx;
        opacity: 0.92;
        margin-top: 8rpx;
      }
    }
  }

  .menu-card {
    background: #fff;
    border-radius: 20rpx;
    margin: 20rpx 24rpx;
    padding: 0 28rpx;

    .menu-item {
      display: flex;
      align-items: center;
      padding: 30rpx 0;
      border-bottom: 1px solid #f5f6f8;

      &:last-child {
        border-bottom: none;
      }

      .m-icon {
        font-size: 36rpx;
        margin-right: 20rpx;
      }
      .m-name {
        flex: 1;
        font-size: 28rpx;
        color: #303133;
      }
      .m-arrow {
        font-size: 36rpx;
        color: #c0c4cc;
      }
    }
  }

  .logout {
    margin: 24rpx;
    background: #fff;
    border-radius: 20rpx;
    text-align: center;
    padding: 28rpx 0;
    color: #f56c6c;
    font-size: 30rpx;
    font-weight: 600;
  }

  .version {
    text-align: center;
    color: #c0c4cc;
    font-size: 24rpx;
    padding: 30rpx 0;
  }
}
</style>
