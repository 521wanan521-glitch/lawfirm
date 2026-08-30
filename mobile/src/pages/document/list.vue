<template>
  <view class="page">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" confirm-type="search" placeholder="搜索文档" placeholder-class="ph" @confirm="onSearch" />
    </view>

    <view class="list">
      <view v-for="d in list" :key="d.id" class="doc-card">
        <view class="icon">📄</view>
        <view class="main">
          <text class="name text-ellipsis">{{ d.name }}</text>
          <text class="meta">{{ docCategoryLabel(d.category) }} · {{ formatSize(d.size) }} · v{{ d.version }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">暂无文档</view>
    </view>
  </view>
</template>

<script>
import { pageDocuments } from '@/api/index'
import { docCategoryLabel } from '@/utils/dict'

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
  methods: {
    docCategoryLabel,
    formatSize(s) {
      if (!s) return '0B'
      if (s < 1024) return s + 'B'
      if (s < 1024 * 1024) return (s / 1024).toFixed(1) + 'KB'
      return (s / 1024 / 1024).toFixed(1) + 'MB'
    },
    async load() {
      this.loading = true
      try {
        const params = { page: this.page, size: this.size }
        if (this.keyword) params.keyword = this.keyword
        const data = await pageDocuments(params)
        this.list = data.items
        this.total = data.total
      } finally {
        this.loading = false
      }
    },
    onSearch() {
      this.page = 1
      this.load()
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding-bottom: 40rpx; }
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
  .ph { color: #a8abb2; }
}
.list { padding: 20rpx 24rpx; }
.doc-card {
  display: flex;
  align-items: center;
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, 0.04);

  .icon {
    font-size: 44rpx;
    margin-right: 20rpx;
  }
  .main {
    flex: 1;
    min-width: 0;
    .name {
      font-size: 28rpx;
      color: #1f2329;
      font-weight: 600;
      display: block;
    }
    .meta {
      font-size: 22rpx;
      color: #909399;
      margin-top: 6rpx;
      display: block;
    }
  }
}
.empty { text-align: center; color: #a8abb2; padding: 120rpx 0; font-size: 26rpx; }
</style>
