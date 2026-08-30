<template>
  <view class="page">
    <view class="search-bar">
      <input class="search-input" v-model="keyword" confirm-type="search" placeholder="搜索知识文章" placeholder-class="ph" @confirm="onSearch" />
    </view>

    <view class="list">
      <view v-for="a in list" :key="a.id" class="article-card" @click="goDetail(a.id)">
        <view class="title text-ellipsis">{{ a.title }}</view>
        <view class="meta">
          <text class="tag">{{ categoryLabel(a.category) }}</text>
          <text v-if="a.tags" class="tag">{{ a.tags }}</text>
        </view>
        <view class="foot">
          <text class="author">{{ a.authorName || '-' }}</text>
          <text class="views">{{ a.viewCount }} 浏览</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">暂无知识文章</view>
    </view>
  </view>
</template>

<script>
import { pageArticles } from '@/api/index'
import { KNOWLEDGE_CATEGORY_MAP } from '@/utils/dict'

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
    categoryLabel(c) {
      return KNOWLEDGE_CATEGORY_MAP[c] || c || '-'
    },
    async load(cb) {
      this.loading = true
      try {
        const params = { page: this.page, size: this.size }
        if (this.keyword) params.keyword = this.keyword
        const data = await pageArticles(params)
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
      uni.navigateTo({ url: '/pages/knowledge/detail?id=' + id })
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
.article-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, 0.04);

  .title {
    font-size: 30rpx;
    font-weight: 600;
    color: #1f2329;
  }
  .meta {
    display: flex;
    gap: 10rpx;
    margin-top: 12rpx;
    .tag {
      font-size: 20rpx;
      color: #2f6fed;
      background: #eef3fe;
      border-radius: 6rpx;
      padding: 4rpx 12rpx;
    }
  }
  .foot {
    display: flex;
    justify-content: space-between;
    margin-top: 14rpx;
    padding-top: 14rpx;
    border-top: 1px solid #f5f6f8;
    .author { font-size: 22rpx; color: #909399; }
    .views { font-size: 22rpx; color: #a8abb2; }
  }
}
.empty { text-align: center; color: #a8abb2; padding: 120rpx 0; font-size: 26rpx; }
</style>
