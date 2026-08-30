<template>
  <view class="page">
    <!-- 案件类型分布 -->
    <view class="card">
      <view class="card-title">案件类型分布</view>
      <view v-for="c in stats.casesByType" :key="c.name" class="bar-row">
        <text class="bar-label">{{ c.name }}</text>
        <view class="bar-track">
          <view class="bar-fill" :style="{ width: barWidth(c.count) }"></view>
        </view>
        <text class="bar-num">{{ c.count }}</text>
      </view>
    </view>

    <!-- 月度趋势 -->
    <view class="card">
      <view class="card-title">近 6 个月新增案件与营收</view>
      <view v-for="m in stats.monthlyTrend" :key="m.month" class="trend-row">
        <text class="month">{{ m.month }}</text>
        <text class="cases">{{ m.newCases }} 件</text>
        <text class="revenue">¥{{ m.revenue || 0 }}</text>
      </view>
    </view>

    <!-- 律师工时排行 -->
    <view class="card">
      <view class="card-title">近 30 天律师工时排行</view>
      <view v-for="(l, i) in stats.lawyerHoursTop" :key="l.userId" class="rank-row">
        <text class="rank" :class="{ top: i < 3 }">{{ i + 1 }}</text>
        <text class="name">{{ l.userName }}</text>
        <text class="hours">{{ l.hours }} 小时</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getStats } from '@/api/index'

export default {
  data() {
    return {
      stats: { casesByType: [], monthlyTrend: [], lawyerHoursTop: [] }
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    barWidth(count) {
      const max = Math.max(...this.stats.casesByType.map((c) => c.count), 1)
      return (count / max) * 100 + '%'
    },
    async load() {
      try {
        this.stats = await getStats()
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding: 20rpx 0 60rpx; }
.card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin: 0 24rpx 20rpx;

  .card-title {
    font-size: 28rpx;
    font-weight: 600;
    color: #1f2329;
    margin-bottom: 20rpx;
  }

  .bar-row {
    display: flex;
    align-items: center;
    margin-bottom: 16rpx;

    .bar-label {
      width: 140rpx;
      font-size: 24rpx;
      color: #606266;
      flex-shrink: 0;
    }
    .bar-track {
      flex: 1;
      height: 20rpx;
      background: #f0f2f5;
      border-radius: 10rpx;
      overflow: hidden;

      .bar-fill {
        height: 100%;
        background: linear-gradient(90deg, #2f6fed, #4a8bf5);
        border-radius: 10rpx;
      }
    }
    .bar-num {
      width: 60rpx;
      text-align: right;
      font-size: 24rpx;
      color: #303133;
      margin-left: 12rpx;
      flex-shrink: 0;
    }
  }

  .trend-row {
    display: flex;
    align-items: center;
    justify-content: space-between;
    padding: 14rpx 0;
    border-bottom: 1px solid #f5f6f8;

    &:last-child { border-bottom: none; }
    .month { font-size: 24rpx; color: #606266; }
    .cases { font-size: 24rpx; color: #303133; }
    .revenue { font-size: 24rpx; color: #2f6fed; font-weight: 600; }
  }

  .rank-row {
    display: flex;
    align-items: center;
    padding: 16rpx 0;
    border-bottom: 1px solid #f5f6f8;

    &:last-child { border-bottom: none; }
    .rank {
      width: 50rpx;
      height: 50rpx;
      border-radius: 50%;
      background: #f0f2f5;
      color: #909399;
      font-size: 24rpx;
      display: flex;
      align-items: center;
      justify-content: center;
      margin-right: 16rpx;
      flex-shrink: 0;
    }
    .rank.top {
      background: #fef0e6;
      color: #e6a23c;
      font-weight: 700;
    }
    .name {
      flex: 1;
      font-size: 28rpx;
      color: #303133;
    }
    .hours {
      font-size: 26rpx;
      color: #2f6fed;
      font-weight: 600;
    }
  }
}
</style>
