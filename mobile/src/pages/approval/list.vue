<template>
  <view class="page">
    <!-- 状态筛选 -->
    <view class="tabs">
      <view
        v-for="t in tabs"
        :key="t.value"
        class="tab"
        :class="{ active: status === t.value }"
        @click="switchStatus(t.value)"
      >
        {{ t.label }}
      </view>
    </view>

    <!-- 审批列表 -->
    <view class="list">
      <view v-for="a in list" :key="a.id" class="approval-card" @click="operate(a)">
        <view class="head flex-between">
          <text class="title text-ellipsis">{{ a.title }}</text>
          <text class="status" :class="'as-' + a.status">{{ approvalStatusLabel(a.status) }}</text>
        </view>
        <view class="meta">
          <text class="tag">{{ a.templateName || approvalTypeLabel(a.type) }}</text>
          <text class="tag">{{ a.applicantName }}</text>
        </view>
        <view class="foot flex-between">
          <text class="approver">审批人：{{ a.approverName || '-' }}</text>
          <text class="date">{{ formatDate(a.createdAt) }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">
        <text class="empty-icon">✍️</text>
        <text class="empty-text">暂无审批</text>
      </view>
    </view>

    <view class="fab" @click="goCreate">＋</view>
  </view>
</template>

<script>
import { pageInstances, approveInstance, rejectInstance, cancelInstance } from '@/api/index'
import { APPROVAL_STATUS_MAP, approvalStatusLabel, approvalTypeLabel } from '@/utils/dict'

export default {
  data() {
    return {
      status: '',
      list: [],
      page: 1,
      size: 20,
      total: 0,
      loading: false,
      tabs: [
        { value: '', label: '全部' },
        { value: 'PENDING', label: '待审批' },
        { value: 'APPROVED', label: '已通过' },
        { value: 'REJECTED', label: '已驳回' }
      ]
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    approvalStatusLabel,
    approvalTypeLabel,
    formatDate(d) {
      return d ? d.substring(0, 10) : '-'
    },
    async load() {
      this.loading = true
      try {
        const params = { page: this.page, size: this.size }
        if (this.status) params.status = this.status
        const data = await pageInstances(params)
        this.list = data.items
        this.total = data.total
      } finally {
        this.loading = false
      }
    },
    switchStatus(s) {
      this.status = s
      this.page = 1
      this.load()
    },
    goCreate() {
      uni.navigateTo({ url: '/pages/approval/edit' })
    },
    operate(a) {
      const items = []
      if (a.status === 'PENDING') {
        items.push('通过', '驳回', '撤销')
      }
      if (!items.length) {
        // 展示详情
        uni.showModal({
          title: a.title,
          content: (a.content || '') + (a.comment ? '\n\n审批意见：' + a.comment : ''),
          showCancel: false
        })
        return
      }
      uni.showActionSheet({
        itemList: items,
        success: (res) => {
          const action = items[res.tapIndex]
          if (action === '通过') this.doApprove(a, true)
          else if (action === '驳回') this.doApprove(a, false)
          else if (action === '撤销') this.doCancel(a)
        }
      })
    },
    doApprove(a, pass) {
      uni.showModal({
        title: pass ? '通过审批' : '驳回审批',
        editable: true,
        placeholderText: '审批意见（选填）',
        success: async (res) => {
          if (!res.confirm) return
          try {
            const opinion = res.content || ''
            if (pass) await approveInstance(a.id, { opinion })
            else await rejectInstance(a.id, { opinion })
            uni.showToast({ title: '已处理', icon: 'success' })
            this.load()
          } catch (e) {}
        }
      })
    },
    doCancel(a) {
      uni.showModal({
        title: '撤销申请',
        content: '确定撤销该审批申请吗？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await cancelInstance(a.id)
            uni.showToast({ title: '已撤销', icon: 'success' })
            this.load()
          } catch (e) {}
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page {
  padding-bottom: 140rpx;
}

.tabs {
  display: flex;
  background: #fff;
  padding: 0 24rpx;
  position: sticky;
  top: 0;
  z-index: 10;

  .tab {
    flex: 1;
    text-align: center;
    padding: 24rpx 0;
    font-size: 28rpx;
    color: #606266;
    position: relative;

    &.active {
      color: #2f6fed;
      font-weight: 600;

      &::after {
        content: '';
        position: absolute;
        bottom: 0;
        left: 50%;
        transform: translateX(-50%);
        width: 48rpx;
        height: 6rpx;
        border-radius: 3rpx;
        background: #2f6fed;
      }
    }
  }
}

.list {
  padding: 20rpx 24rpx;

  .approval-card {
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
      .as-APPROVED {
        color: #67c23a;
      }
      .as-REJECTED {
        color: #f56c6c;
      }
      .as-CANCELLED {
        color: #a8abb2;
      }
    }

    .meta {
      display: flex;
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

      .approver {
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
