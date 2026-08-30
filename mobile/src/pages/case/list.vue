<template>
  <view class="page">
    <!-- 搜索 + 筛选 -->
    <view class="search-bar">
      <input
        class="search-input"
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索案号 / 案件名称"
        placeholder-class="ph"
        @confirm="onSearch"
      />
      <picker :range="statusLabels" range-key="label" @change="onStatusChange">
        <view class="status-picker">
          <text>{{ statusLabel || '状态' }}</text>
          <text class="arrow">▾</text>
        </view>
      </picker>
    </view>

    <!-- 案件列表 -->
    <view class="list">
      <view v-for="c in list" :key="c.id" class="case-card" @click="goDetail(c.id)">
        <view class="head flex-between">
          <text class="title text-ellipsis">{{ c.title }}</text>
          <text class="status" :class="'st-' + c.status">{{ caseStatusLabel(c.status) }}</text>
        </view>
        <view class="meta">
          <text class="tag">{{ c.caseNo }}</text>
          <text class="tag">{{ caseTypeLabel(c.type) }}</text>
          <text class="tag">{{ priorityLabel(c.priority) }}</text>
        </view>
        <view v-if="c.plaintiff || c.defendant" class="parties">
          <text class="p">原告：{{ c.plaintiff || '-' }}</text>
          <text class="p">被告：{{ c.defendant || '-' }}</text>
        </view>
        <view class="foot flex-between">
          <text class="lawyer">主办：{{ c.leadLawyerName || '-' }}</text>
          <text class="date">{{ formatDate(c.filingDate) }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">
        <text class="empty-icon">📁</text>
        <text class="empty-text">暂无案件</text>
      </view>
      <view v-if="loading" class="loading">加载中...</view>
    </view>

    <!-- 悬浮新建按钮 -->
    <view class="fab" @click="goEdit()">＋</view>
  </view>
</template>

<script>
import { pageCases } from '@/api/index'
import { CASE_STATUS_MAP, caseStatusLabel, caseTypeLabel, priorityLabel } from '@/utils/dict'

export default {
  data() {
    return {
      keyword: '',
      status: '',
      statusLabel: '',
      list: [],
      page: 1,
      size: 20,
      total: 0,
      loading: false,
      statusLabels: Object.keys(CASE_STATUS_MAP).map((k) => ({ value: k, label: CASE_STATUS_MAP[k] }))
    }
  },
  onShow() {
    this.load()
  },
  onPullDownRefresh() {
    this.load(() => uni.stopPullDownRefresh())
  },
  onReachBottom() {
    if (this.list.length < this.total) {
      this.page++
      this.load()
    }
  },
  methods: {
    caseStatusLabel,
    caseTypeLabel,
    priorityLabel,
    formatDate(d) {
      return d ? d.substring(0, 10) : '-'
    },
    async load(cb) {
      this.loading = true
      try {
        const params = { page: this.page, size: this.size }
        if (this.keyword) params.keyword = this.keyword
        if (this.status) params.status = this.status
        const data = await pageCases(params)
        this.list = data.items
        this.total = data.total
      } finally {
        this.loading = false
        if (cb) cb()
      }
    },
    onSearch() {
      this.page = 1
      this.load()
    },
    onStatusChange(e) {
      const idx = Number(e.detail.value)
      this.status = this.statusLabels[idx].value
      this.statusLabel = this.statusLabels[idx].label
      this.page = 1
      this.load()
    },
    goDetail(id) {
      uni.navigateTo({ url: '/pages/case/detail?id=' + id })
    },
    goEdit() {
      uni.navigateTo({ url: '/pages/case/edit' })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 140rpx;
}

.search-bar {
  display: flex;
  align-items: center;
  background: #fff;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;

  .search-input {
    flex: 1;
    background: #f5f6f8;
    border-radius: 30rpx;
    padding: 14rpx 24rpx;
    font-size: 26rpx;
  }
  .ph {
    color: #a8abb2;
  }
  .status-picker {
    display: flex;
    align-items: center;
    margin-left: 16rpx;
    font-size: 26rpx;
    color: #606266;

    .arrow {
      margin-left: 6rpx;
      color: #a8abb2;
    }
  }
}

.list {
  padding: 20rpx 24rpx;

  .case-card {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, 0.04);

    .head {
      .title {
        flex: 1;
        min-width: 0;
        font-size: 30rpx;
        font-weight: 600;
        color: #1f2329;
        margin-right: 12rpx;
      }
      .status {
        font-size: 22rpx;
        flex-shrink: 0;
        color: #2f6fed;
      }
    }

    .meta {
      display: flex;
      flex-wrap: wrap;
      gap: 10rpx;
      margin-top: 12rpx;

      .tag {
        font-size: 20rpx;
        color: #606266;
        background: #f5f6f8;
        border-radius: 6rpx;
        padding: 4rpx 12rpx;
      }
    }

    .parties {
      margin-top: 12rpx;
      display: flex;
      flex-direction: column;
      gap: 4rpx;

      .p {
        font-size: 24rpx;
        color: #606266;
      }
    }

    .foot {
      margin-top: 14rpx;
      padding-top: 14rpx;
      border-top: 1px solid #f5f6f8;

      .lawyer {
        font-size: 22rpx;
        color: #909399;
      }
      .date {
        font-size: 22rpx;
        color: #a8abb2;
      }
    }
  }

  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 140rpx;

    .empty-icon {
      font-size: 120rpx;
    }
    .empty-text {
      color: #a8abb2;
      font-size: 28rpx;
      margin-top: 20rpx;
    }
  }
  .loading {
    text-align: center;
    color: #a8abb2;
    font-size: 24rpx;
    padding: 20rpx 0;
  }
}

.fab {
  position: fixed;
  right: 40rpx;
  bottom: 120rpx;
  width: 100rpx;
  height: 100rpx;
  border-radius: 50%;
  background: linear-gradient(135deg, #2f6fed, #4a8bf5);
  color: #fff;
  font-size: 60rpx;
  display: flex;
  align-items: center;
  justify-content: center;
  box-shadow: 0 8rpx 24rpx rgba(47, 111, 237, 0.4);
  z-index: 99;
}
</style>
