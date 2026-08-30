<template>
  <view class="mine">
    <!-- 用户卡片 -->
    <view class="user-card">
      <view class="avatar-wrap" @click="changeAvatar">
        <image v-if="avatarUrl" class="avatar-img" :src="avatarUrl" mode="aspectFill" />
        <view v-else class="avatar">{{ avatarText }}</view>
        <view class="camera">📷</view>
      </view>
      <view class="user-info">
        <text class="name">{{ store.displayName || '未登录' }}</text>
        <text class="role">{{ roleLabel(store.role) }}{{ store.user && store.user.department ? ' · ' + store.user.department : '' }}</text>
      </view>
      <text class="edit" @click="openEdit">编辑</text>
    </view>

    <!-- 功能入口 -->
    <view class="menu-card">
      <view class="menu-item" @click="go('/pages/client/list')">
        <text class="m-icon">👥</text>
        <text class="m-name">客户管理</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/time/list')">
        <text class="m-icon">⏱️</text>
        <text class="m-name">工时记录</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/invoice/list')">
        <text class="m-icon">💰</text>
        <text class="m-name">账单管理</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/document/list')">
        <text class="m-icon">📄</text>
        <text class="m-name">文档中心</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/approval/list')">
        <text class="m-icon">✍️</text>
        <text class="m-name">审批流程</text>
        <text class="m-arrow">›</text>
      </view>
      <view class="menu-item" @click="go('/pages/knowledge/list')">
        <text class="m-icon">📚</text>
        <text class="m-name">知识库</text>
        <text class="m-arrow">›</text>
      </view>
      <view v-if="store.isManager" class="menu-item" @click="go('/pages/stats/stats')">
        <text class="m-icon">📊</text>
        <text class="m-name">统计报表</text>
        <text class="m-arrow">›</text>
      </view>
      <view v-if="store.isAdmin" class="menu-item" @click="go('/pages/member/list')">
        <text class="m-icon">👤</text>
        <text class="m-name">成员管理</text>
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

    <!-- 编辑资料弹窗 -->
    <view v-if="showEdit" class="mask" @click="showEdit = false">
      <view class="dialog" @click.stop>
        <view class="d-title">编辑资料</view>
        <input class="d-input" v-model="editForm.realName" placeholder="姓名" placeholder-class="ph" />
        <input class="d-input" v-model="editForm.phone" placeholder="电话" placeholder-class="ph" />
        <input class="d-input" v-model="editForm.email" placeholder="邮箱" placeholder-class="ph" />
        <view class="d-btn" @click="saveProfile">保存</view>
      </view>
    </view>
  </view>
</template>

<script>
import { changePassword, updateProfile, uploadAvatar } from '@/api/index'
import { useUserStore } from '@/store/index'
import { roleLabel } from '@/utils/dict'

const BASE_URL = 'http://47.107.62.86/api'

export default {
  data() {
    return {
      showPwd: false,
      pwdForm: { oldPassword: '', newPassword: '', confirm: '' },
      showEdit: false,
      editForm: { realName: '', email: '', phone: '' }
    }
  },
  computed: {
    store() {
      return useUserStore()
    },
    avatarText() {
      return (this.store.displayName || '律').charAt(0)
    },
    avatarUrl() {
      const a = this.store.user && this.store.user.avatar
      if (!a) return ''
      return a.startsWith('http') ? a : BASE_URL + a
    }
  },
  methods: {
    roleLabel,
    go(url) {
      uni.navigateTo({ url })
    },
    changeAvatar() {
      uni.chooseImage({
        count: 1,
        sizeType: ['compressed'],
        sourceType: ['album', 'camera'],
        success: async (res) => {
          const path = res.tempFilePaths && res.tempFilePaths[0]
          if (!path) return
          uni.showLoading({ title: '上传中...' })
          try {
            const user = await uploadAvatar(path)
            this.store.user = { ...this.store.user, ...user }
            uni.setStorageSync('user', this.store.user)
            uni.hideLoading()
            uni.showToast({ title: '头像已更新', icon: 'success' })
          } catch (e) {
            uni.hideLoading()
          }
        }
      })
    },
    openEdit() {
      this.editForm = {
        realName: this.store.user ? this.store.user.realName : '',
        email: this.store.user ? this.store.user.email : '',
        phone: this.store.user ? this.store.user.phone : ''
      }
      this.showEdit = true
    },
    async saveProfile() {
      if (!this.editForm.realName.trim()) return uni.showToast({ title: '请输入姓名', icon: 'none' })
      try {
        const user = await updateProfile({
          realName: this.editForm.realName.trim(),
          email: this.editForm.email,
          phone: this.editForm.phone
        })
        this.store.user = { ...this.store.user, ...user }
        uni.setStorageSync('user', this.store.user)
        uni.showToast({ title: '已保存', icon: 'success' })
        this.showEdit = false
      } catch (e) {}
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
  padding-bottom: 200rpx;

  .user-card {
    background: linear-gradient(135deg, #2f6fed, #4a8bf5);
    padding: 60rpx 32rpx;
    display: flex;
    align-items: center;
    color: #fff;

    .avatar-wrap {
      position: relative;
      margin-right: 24rpx;

      .avatar, .avatar-img {
        width: 120rpx;
        height: 120rpx;
        border-radius: 50%;
        display: flex;
        align-items: center;
        justify-content: center;
      }
      .avatar {
        background: rgba(255, 255, 255, 0.25);
        font-size: 48rpx;
        font-weight: 700;
      }
      .avatar-img {
        border: 2rpx solid rgba(255, 255, 255, 0.5);
      }
      .camera {
        position: absolute;
        right: -4rpx;
        bottom: -4rpx;
        width: 44rpx;
        height: 44rpx;
        border-radius: 50%;
        background: #fff;
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 22rpx;
        box-shadow: 0 2rpx 6rpx rgba(0, 0, 0, 0.15);
      }
    }
    .user-info {
      flex: 1;
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
    .edit {
      font-size: 24rpx;
      color: #fff;
      border: 1px solid rgba(255, 255, 255, 0.6);
      border-radius: 24rpx;
      padding: 6rpx 20rpx;
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

  .mask {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    z-index: 300;
    display: flex;
    align-items: flex-end;

    .dialog {
      width: 100%;
      background: #fff;
      border-radius: 24rpx 24rpx 0 0;
      padding: 40rpx 32rpx;
      padding-bottom: calc(40rpx + env(safe-area-inset-bottom));

      .d-title {
        font-size: 32rpx;
        font-weight: 700;
        text-align: center;
        margin-bottom: 30rpx;
      }
      .d-input {
        background: #f5f6f8;
        border-radius: 12rpx;
        padding: 22rpx;
        font-size: 28rpx;
        margin-bottom: 20rpx;
      }
      .ph { color: #c0c4cc; }
      .d-btn {
        height: 88rpx;
        line-height: 88rpx;
        text-align: center;
        color: #fff;
        font-size: 30rpx;
        font-weight: 600;
        border-radius: 44rpx;
        background: linear-gradient(90deg, #2f6fed, #4a8bf5);
      }
    }
  }
}
</style>
