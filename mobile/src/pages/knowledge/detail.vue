<template>
  <view class="detail" v-if="article">
    <view class="head">
      <text class="title">{{ article.title }}</text>
      <view class="meta">
        <text class="tag">{{ knowledgeCategoryLabel(article.category) }}</text>
        <text class="info">{{ article.authorName || '-' }} · {{ article.viewCount }} 浏览</text>
      </view>
    </view>
    <view class="card">
      <text class="content">{{ article.content }}</text>
    </view>
  </view>
</template>

<script>
import { getArticle } from '@/api/index'
import { knowledgeCategoryLabel } from '@/utils/dict'

export default {
  data() {
    return {
      id: null,
      article: null
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    knowledgeCategoryLabel,
    async load() {
      this.article = await getArticle(this.id)
    }
  }
}
</script>

<style lang="scss" scoped>
.detail {
  padding: 20rpx 0 60rpx;

  .head {
    background: #fff;
    padding: 28rpx 24rpx;
    margin-bottom: 20rpx;

    .title {
      font-size: 34rpx;
      font-weight: 700;
      color: #1f2329;
      display: block;
      line-height: 1.5;
    }
    .meta {
      display: flex;
      align-items: center;
      gap: 16rpx;
      margin-top: 16rpx;

      .tag {
        font-size: 22rpx;
        color: #2f6fed;
        background: #eef3fe;
        border-radius: 6rpx;
        padding: 4rpx 14rpx;
      }
      .info {
        font-size: 24rpx;
        color: #a8abb2;
      }
    }
  }

  .card {
    background: #fff;
    border-radius: 16rpx;
    padding: 28rpx 24rpx;
    margin: 0 24rpx;

    .content {
      font-size: 28rpx;
      color: #303133;
      line-height: 1.8;
      white-space: pre-wrap;
    }
  }
}
</style>
