<template>
  <view class="detail" v-if="detail">
    <!-- 头部 -->
    <view class="head">
      <view class="title-row">
        <text class="title">{{ detail.title }}</text>
        <text class="status" :class="'st-' + detail.status">{{ caseStatusLabel(detail.status) }}</text>
      </view>
      <text class="case-no">{{ detail.caseNo }}</text>
    </view>

    <!-- 当事人 -->
    <view v-if="detail.plaintiff || detail.defendant" class="card">
      <view class="row"><text class="k">原告</text><text class="v">{{ detail.plaintiff || '-' }}</text></view>
      <view class="row"><text class="k">被告</text><text class="v">{{ detail.defendant || '-' }}</text></view>
    </view>

    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title">基本信息</view>
      <view class="row"><text class="k">案件类型</text><text class="v">{{ caseTypeLabel(detail.type) }}</text></view>
      <view class="row"><text class="k">优先级</text><text class="v">{{ priorityLabel(detail.priority) }}</text></view>
      <view class="row"><text class="k">客户</text><text class="v">{{ detail.clientName || '-' }}</text></view>
      <view class="row"><text class="k">主办律师</text><text class="v">{{ detail.leadLawyerName || '-' }}</text></view>
      <view v-if="detail.coLawyerNames && detail.coLawyerNames.length" class="row">
        <text class="k">协办律师</text><text class="v">{{ detail.coLawyerNames.join('、') }}</text>
      </view>
      <view class="row"><text class="k">受理法院</text><text class="v">{{ detail.court || '-' }}</text></view>
      <view class="row"><text class="k">标的额</text><text class="v">{{ detail.caseAmount ? '¥' + detail.caseAmount : '-' }}</text></view>
      <view class="row"><text class="k">立案日期</text><text class="v">{{ detail.filingDate || '-' }}</text></view>
      <view v-if="detail.closeDate" class="row"><text class="k">结案日期</text><text class="v">{{ detail.closeDate }}</text></view>
      <view class="row"><text class="k">收费金额</text><text class="v">{{ detail.fee ? '¥' + detail.fee : '-' }}</text></view>
    </view>

    <!-- 案情摘要 -->
    <view v-if="detail.description" class="card">
      <view class="card-title">案情摘要</view>
      <text class="desc">{{ detail.description }}</text>
    </view>

    <!-- 办理结果 -->
    <view v-if="detail.result" class="card">
      <view class="card-title">办理结果</view>
      <text class="desc">{{ detail.result }}</text>
    </view>

    <!-- 进程记录 -->
    <view class="card">
      <view class="card-title">进程记录</view>
      <view v-if="!progress.length" class="empty">暂无进程记录</view>
      <view v-for="p in progress" :key="p.id" class="progress-item">
        <view class="p-head flex-between">
          <text class="p-user">{{ p.userName }}</text>
          <text class="p-date">{{ p.progressDate }}</text>
        </view>
        <text class="p-content">{{ p.content }}</text>
      </view>
    </view>

    <!-- 底部操作 -->
    <view class="bottom-bar">
      <view class="btn ghost" @click="goEdit">编辑</view>
      <view class="btn primary" @click="changeStatus">更新状态</view>
    </view>
  </view>
</template>

<script>
import { getCase, pageProgress, updateCaseStatus } from '@/api/index'
import { caseStatusLabel, caseTypeLabel, priorityLabel, CASE_STATUS_MAP } from '@/utils/dict'

export default {
  data() {
    return {
      id: null,
      detail: null,
      progress: []
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    caseStatusLabel,
    caseTypeLabel,
    priorityLabel,
    async load() {
      this.detail = await getCase(this.id)
      const p = await pageProgress(this.id, { page: 1, size: 100 })
      this.progress = p.items
    },
    goEdit() {
      uni.navigateTo({ url: '/pages/case/edit?id=' + this.id })
    },
    changeStatus() {
      const items = Object.keys(CASE_STATUS_MAP).filter((k) => k !== this.detail.status)
      uni.showActionSheet({
        itemList: items.map((k) => CASE_STATUS_MAP[k]),
        success: async (res) => {
          const status = items[res.tapIndex]
          let result = ''
          let closeDate = ''
          if (status === 'CLOSED') {
            result = '已结案'
          }
          try {
            await updateCaseStatus(this.id, { status, result, closeDate: closeDate || undefined })
            uni.showToast({ title: '状态已更新', icon: 'success' })
            this.load()
          } catch (e) {}
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.detail {
  padding: 20rpx 0 160rpx;

  .head {
    background: #fff;
    padding: 28rpx 24rpx;
    margin-bottom: 20rpx;

    .title-row {
      display: flex;
      align-items: flex-start;

      .title {
        flex: 1;
        font-size: 32rpx;
        font-weight: 600;
        color: #1f2329;
        margin-right: 12rpx;
        line-height: 1.5;
      }
      .status {
        font-size: 22rpx;
        color: #2f6fed;
        flex-shrink: 0;
        padding-top: 6rpx;
      }
    }
    .case-no {
      font-size: 24rpx;
      color: #909399;
      margin-top: 10rpx;
      display: block;
    }
  }

  .card {
    background: #fff;
    border-radius: 16rpx;
    padding: 24rpx;
    margin: 0 24rpx 20rpx;

    .card-title {
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2329;
      margin-bottom: 16rpx;
    }

    .row {
      display: flex;
      padding: 10rpx 0;

      .k {
        width: 140rpx;
        color: #909399;
        font-size: 26rpx;
        flex-shrink: 0;
      }
      .v {
        flex: 1;
        color: #303133;
        font-size: 26rpx;
      }
    }

    .desc {
      font-size: 26rpx;
      color: #606266;
      line-height: 1.7;
    }

    .empty {
      color: #a8abb2;
      font-size: 24rpx;
      padding: 20rpx 0;
      text-align: center;
    }

    .progress-item {
      padding: 16rpx 0;
      border-bottom: 1px solid #f5f6f8;

      &:last-child {
        border-bottom: none;
      }

      .p-head {
        .p-user {
          font-size: 24rpx;
          color: #2f6fed;
          font-weight: 600;
        }
        .p-date {
          font-size: 22rpx;
          color: #a8abb2;
        }
      }
      .p-content {
        font-size: 26rpx;
        color: #303133;
        margin-top: 8rpx;
        display: block;
      }
    }
  }
}

.bottom-bar {
  position: fixed;
  left: 0;
  right: 0;
  bottom: 0;
  background: #fff;
  padding: 16rpx 24rpx;
  padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
  display: flex;
  gap: 20rpx;
  box-shadow: 0 -2rpx 10rpx rgba(31, 35, 41, 0.06);

  .btn {
    flex: 1;
    height: 84rpx;
    line-height: 84rpx;
    text-align: center;
    border-radius: 42rpx;
    font-size: 30rpx;
    font-weight: 600;
  }
  .ghost {
    color: #2f6fed;
    border: 1px solid #2f6fed;
  }
  .primary {
    color: #fff;
    background: linear-gradient(90deg, #2f6fed, #4a8bf5);
  }
}
</style>
