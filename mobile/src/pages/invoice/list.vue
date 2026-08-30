<template>
  <view class="page">
    <view class="list">
      <view v-for="i in list" :key="i.id" class="invoice-card">
        <view class="head flex-between">
          <text class="no">{{ i.invoiceNo }}</text>
          <text class="status" :class="'is-' + i.status">{{ invoiceStatusLabel(i.status) }}</text>
        </view>
        <view class="client">{{ i.clientName || '-' }}</view>
        <view class="foot flex-between">
          <text class="count">{{ i.timeEntryCount }} 条工时</text>
          <text class="amount">¥{{ i.totalAmount || 0 }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">暂无账单</view>
    </view>
  </view>
</template>

<script>
import { pageInvoices } from '@/api/index'
import { invoiceStatusLabel } from '@/utils/dict'

export default {
  data() {
    return {
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
    invoiceStatusLabel,
    async load() {
      this.loading = true
      try {
        const data = await pageInvoices({ page: this.page, size: this.size })
        this.list = data.items
        this.total = data.total
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding-bottom: 40rpx; }
.list { padding: 20rpx 24rpx; }
.invoice-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, 0.04);

  .head {
    .no {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2329;
    }
    .status { font-size: 22rpx; color: #2f6fed; }
    .is-PAID { color: #67c23a; }
    .is-VOID { color: #f56c6c; }
    .is-DRAFT { color: #a8abb2; }
  }
  .client {
    font-size: 26rpx;
    color: #606266;
    margin-top: 12rpx;
  }
  .foot {
    margin-top: 16rpx;
    padding-top: 16rpx;
    border-top: 1px solid #f5f6f8;

    .count { font-size: 22rpx; color: #909399; }
    .amount { font-size: 32rpx; font-weight: 700; color: #2f6fed; }
  }
}
.empty { text-align: center; color: #a8abb2; padding: 120rpx 0; font-size: 26rpx; }
</style>
