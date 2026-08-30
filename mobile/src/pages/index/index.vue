<template>
  <view class="home">
    <!-- 顶部用户信息 -->
    <view class="header">
      <view class="user-row">
        <view class="avatar">{{ avatarText }}</view>
        <view class="user-info">
          <text class="name">{{ store.displayName || '未登录' }}</text>
          <text class="role">{{ roleLabel(store.role) }}</text>
        </view>
      </view>
    </view>

    <!-- 统计卡片 -->
    <view class="stats-grid">
      <view class="stat-item" @click="go('/pages/case/list')">
        <text class="num">{{ summary.totalCases }}</text>
        <text class="label">案件总数</text>
      </view>
      <view class="stat-item" @click="go('/pages/case/list')">
        <text class="num">{{ summary.activeCases }}</text>
        <text class="label">办理中</text>
      </view>
      <view class="stat-item" @click="go('/pages/client/list')">
        <text class="num">{{ summary.totalClients }}</text>
        <text class="label">客户</text>
      </view>
      <view class="stat-item" @click="go('/pages/approval/list')">
        <text class="num">{{ summary.pendingApprovals }}</text>
        <text class="label">待审批</text>
      </view>
    </view>

    <!-- 快捷入口 -->
    <view class="quick">
      <view class="quick-item" @click="go('/pages/case/list')">
        <text class="q-icon">📁</text>
        <text class="q-text">案件管理</text>
      </view>
      <view class="quick-item" @click="go('/pages/client/list')">
        <text class="q-icon">👥</text>
        <text class="q-text">客户管理</text>
      </view>
      <view class="quick-item" @click="go('/pages/approval/list')">
        <text class="q-icon">✍️</text>
        <text class="q-text">审批流程</text>
      </view>
      <view class="quick-item" @click="go('/pages/calendar/calendar')">
        <text class="q-icon">📅</text>
        <text class="q-text">日程安排</text>
      </view>
    </view>

    <!-- 最近案件 -->
    <view class="card recent">
      <view class="section-title flex-between">
        <text class="t">最近案件</text>
        <text class="more" @click="go('/pages/case/list')">全部 ›</text>
      </view>
      <view v-if="!summary.recentCases || !summary.recentCases.length" class="empty">暂无案件</view>
      <view
        v-for="c in summary.recentCases"
        :key="c.id"
        class="recent-item"
        @click="go('/pages/case/detail?id=' + c.id)"
      >
        <view class="r-main">
          <text class="r-title text-ellipsis">{{ c.title }}</text>
          <text class="r-no">{{ c.caseNo }} · {{ c.leadLawyerName || '-' }}</text>
        </view>
        <text class="r-status">{{ caseStatusLabel(c.status) }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getSummary } from '@/api/index'
import { useUserStore } from '@/store/index'
import { roleLabel, caseStatusLabel } from '@/utils/dict'

export default {
  data() {
    return {
      summary: {
        totalCases: 0,
        activeCases: 0,
        totalClients: 0,
        pendingApprovals: 0,
        recentCases: []
      }
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
  onShow() {
    this.load()
  },
  methods: {
    roleLabel,
    caseStatusLabel,
    async load() {
      try {
        this.summary = await getSummary()
      } catch (e) {}
    },
    go(url) {
      uni.navigateTo({ url })
    }
  }
}
</script>

<style lang="scss" scoped>
.home {
  padding-bottom: 40rpx;

  .header {
    background: linear-gradient(135deg, #2f6fed, #4a8bf5);
    padding: 30rpx 32rpx 60rpx;
    color: #fff;

    .user-row {
      display: flex;
      align-items: center;

      .avatar {
        width: 96rpx;
        height: 96rpx;
        border-radius: 50%;
        background: rgba(255, 255, 255, 0.25);
        display: flex;
        align-items: center;
        justify-content: center;
        font-size: 40rpx;
        font-weight: 700;
        margin-right: 20rpx;
      }
      .user-info {
        display: flex;
        flex-direction: column;

        .name {
          font-size: 34rpx;
          font-weight: 700;
        }
        .role {
          font-size: 24rpx;
          opacity: 0.9;
          margin-top: 4rpx;
        }
      }
    }
  }

  .stats-grid {
    display: grid;
    grid-template-columns: repeat(4, 1fr);
    background: #fff;
    border-radius: 20rpx;
    margin: -36rpx 24rpx 0;
    padding: 28rpx 0;
    box-shadow: 0 6rpx 20rpx rgba(31, 35, 41, 0.06);

    .stat-item {
      display: flex;
      flex-direction: column;
      align-items: center;
      border-right: 1px solid #f0f2f5;

      &:last-child {
        border-right: none;
      }

      .num {
        font-size: 40rpx;
        font-weight: 700;
        color: #2f6fed;
      }
      .label {
        font-size: 22rpx;
        color: #909399;
        margin-top: 6rpx;
      }
    }
  }

  .quick {
    display: flex;
    background: #fff;
    border-radius: 20rpx;
    margin: 20rpx 24rpx;
    padding: 30rpx 0;

    .quick-item {
      flex: 1;
      display: flex;
      flex-direction: column;
      align-items: center;

      .q-icon {
        font-size: 48rpx;
        margin-bottom: 10rpx;
      }
      .q-text {
        font-size: 24rpx;
        color: #606266;
      }
    }
  }

  .recent {
    .section-title {
      margin-bottom: 8rpx;

      .t {
        font-size: 30rpx;
        font-weight: 600;
      }
      .more {
        font-size: 24rpx;
        color: #909399;
      }
    }

    .empty {
      text-align: center;
      color: #a8abb2;
      padding: 40rpx 0;
      font-size: 26rpx;
    }

    .recent-item {
      display: flex;
      align-items: center;
      justify-content: space-between;
      padding: 20rpx 0;
      border-bottom: 1px solid #f5f6f8;

      &:last-child {
        border-bottom: none;
      }

      .r-main {
        flex: 1;
        min-width: 0;
        display: flex;
        flex-direction: column;

        .r-title {
          font-size: 28rpx;
          color: #1f2329;
        }
        .r-no {
          font-size: 22rpx;
          color: #a8abb2;
          margin-top: 4rpx;
        }
      }
      .r-status {
        font-size: 24rpx;
        color: #2f6fed;
        margin-left: 16rpx;
        flex-shrink: 0;
      }
    }
  }
}
</style>
