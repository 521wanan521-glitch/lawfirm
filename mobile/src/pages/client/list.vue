<template>
  <view class="page">
    <view class="search-bar">
      <input
        class="search-input"
        v-model="keyword"
        confirm-type="search"
        placeholder="搜索客户名称 / 电话"
        placeholder-class="ph"
        @confirm="onSearch"
      />
    </view>

    <view class="list">
      <view v-for="c in list" :key="c.id" class="client-card" @click="goDetail(c.id)">
        <view class="head flex-between">
          <view class="name-row flex">
            <text class="name text-ellipsis">{{ c.name }}</text>
            <text v-if="c.consultant" class="vip">VIP</text>
          </view>
          <text class="type">{{ clientTypeLabel(c.type) }}</text>
        </view>
        <view class="meta">
          <text class="tag">{{ clientLevelLabel(c.level) }}</text>
          <text v-if="c.industry" class="tag">{{ c.industry }}</text>
          <text class="tag">{{ c.ownerName || '未分配' }}</text>
        </view>
        <view class="foot flex-between">
          <text class="phone">{{ c.phone || '-' }}</text>
          <text class="count">案件 {{ c.caseCount }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">
        <text class="empty-icon">👥</text>
        <text class="empty-text">暂无客户</text>
      </view>
    </view>
  </view>
</template>

<script>
import { pageClients } from '@/api/index'
import { clientTypeLabel, clientLevelLabel } from '@/utils/dict'

export default {
  data() {
    return {
      keyword: '',
      list: [],
      page: 1,
      size: 20,
      total: 0,
      loading: false
    }
  },
  onShow() {
    this.load()
  },
  onReachBottom() {
    if (this.list.length < this.total) {
      this.page++
      this.load()
    }
  },
  onPullDownRefresh() {
    this.page = 1
    this.load(() => uni.stopPullDownRefresh())
  },
  methods: {
    clientTypeLabel,
    clientLevelLabel,
    async load(cb) {
      this.loading = true
      try {
        const params = { page: this.page, size: this.size }
        if (this.keyword) params.keyword = this.keyword
        const data = await pageClients(params)
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
    goDetail(id) {
      uni.navigateTo({ url: '/pages/client/detail?id=' + id })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 40rpx;
}

.search-bar {
  background: #fff;
  padding: 16rpx 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;

  .search-input {
    background: #f5f6f8;
    border-radius: 30rpx;
    padding: 14rpx 24rpx;
    font-size: 26rpx;
  }
  .ph {
    color: #a8abb2;
  }
}

.list {
  padding: 20rpx 24rpx;

  .client-card {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin-bottom: 20rpx;
    box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, 0.04);

    .head {
      .name-row {
        min-width: 0;
        .name {
          font-size: 30rpx;
          font-weight: 600;
          color: #1f2329;
          max-width: 400rpx;
        }
        .vip {
          font-size: 20rpx;
          color: #b88230;
          background: #fdf3e3;
          border-radius: 6rpx;
          padding: 2rpx 10rpx;
          margin-left: 12rpx;
          flex-shrink: 0;
        }
      }
      .type {
        font-size: 24rpx;
        color: #2f6fed;
        flex-shrink: 0;
        margin-left: 12rpx;
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

    .foot {
      margin-top: 14rpx;
      padding-top: 14rpx;
      border-top: 1px solid #f5f6f8;

      .phone {
        font-size: 22rpx;
        color: #909399;
      }
      .count {
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
}
</style>
