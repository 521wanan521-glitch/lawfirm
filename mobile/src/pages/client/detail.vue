<template>
  <view class="detail" v-if="detail">
    <!-- 头部 -->
    <view class="head">
      <view class="name-row flex">
        <text class="name">{{ detail.name }}</text>
        <text v-if="detail.consultant" class="vip">法律顾问单位</text>
      </view>
      <text class="sub">{{ clientTypeLabel(detail.type) }} · {{ clientLevelLabel(detail.level) }}</text>
    </view>

    <!-- 基本信息 -->
    <view class="card">
      <view class="card-title">基本信息</view>
      <view class="row"><text class="k">证件号</text><text class="v">{{ detail.idNumber || '-' }}</text></view>
      <view class="row"><text class="k">所属行业</text><text class="v">{{ detail.industry || '-' }}</text></view>
      <view class="row"><text class="k">联系电话</text><text class="v">{{ detail.phone || '-' }}</text></view>
      <view class="row"><text class="k">电子邮箱</text><text class="v">{{ detail.email || '-' }}</text></view>
      <view class="row"><text class="k">联系地址</text><text class="v">{{ detail.address || '-' }}</text></view>
      <view class="row"><text class="k">客户来源</text><text class="v">{{ detail.source || '-' }}</text></view>
      <view class="row"><text class="k">负责人</text><text class="v">{{ detail.ownerName || '-' }}</text></view>
      <view v-if="detail.remark" class="row"><text class="k">备注</text><text class="v">{{ detail.remark }}</text></view>
    </view>

    <!-- 联系人 -->
    <view class="card">
      <view class="card-title">联系人（{{ contacts.length }}）</view>
      <view v-if="!contacts.length" class="empty">暂无联系人</view>
      <view v-for="c in contacts" :key="c.id" class="contact-item">
        <view class="c-name">{{ c.name }}<text v-if="c.primaryContact" class="primary">主要</text></view>
        <text class="c-info">{{ c.phone || '-' }} · {{ c.position || '-' }}</text>
      </view>
    </view>

    <!-- 跟进记录 -->
    <view class="card">
      <view class="card-title">跟进记录</view>
      <view v-if="!interactions.length" class="empty">暂无跟进记录</view>
      <view v-for="i in interactions" :key="i.id" class="interaction-item">
        <view class="i-head flex-between">
          <text class="i-user">{{ i.userName }}</text>
          <text class="i-date">{{ formatDate(i.createdAt) }}</text>
        </view>
        <text class="i-content">{{ i.content }}</text>
      </view>
    </view>
  </view>
</template>

<script>
import { getClient, getContacts, pageInteractions } from '@/api/index'
import { clientTypeLabel, clientLevelLabel } from '@/utils/dict'

export default {
  data() {
    return {
      id: null,
      detail: null,
      contacts: [],
      interactions: []
    }
  },
  onLoad(options) {
    this.id = options.id
    this.load()
  },
  methods: {
    clientTypeLabel,
    clientLevelLabel,
    formatDate(d) {
      return d ? d.substring(0, 10) : '-'
    },
    async load() {
      this.detail = await getClient(this.id)
      this.contacts = await getContacts(this.id)
      const p = await pageInteractions(this.id, { page: 1, size: 50 })
      this.interactions = p.items
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

    .name-row {
      .name {
        font-size: 34rpx;
        font-weight: 700;
        color: #1f2329;
      }
      .vip {
        font-size: 22rpx;
        color: #b88230;
        background: #fdf3e3;
        border-radius: 6rpx;
        padding: 4rpx 12rpx;
        margin-left: 16rpx;
      }
    }
    .sub {
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

    .empty {
      color: #a8abb2;
      font-size: 24rpx;
      padding: 20rpx 0;
      text-align: center;
    }

    .contact-item {
      padding: 16rpx 0;
      border-bottom: 1px solid #f5f6f8;

      &:last-child {
        border-bottom: none;
      }

      .c-name {
        font-size: 28rpx;
        color: #303133;
        font-weight: 600;

        .primary {
          font-size: 20rpx;
          color: #2f6fed;
          margin-left: 10rpx;
        }
      }
      .c-info {
        font-size: 24rpx;
        color: #909399;
        margin-top: 6rpx;
        display: block;
      }
    }

    .interaction-item {
      padding: 16rpx 0;
      border-bottom: 1px solid #f5f6f8;

      &:last-child {
        border-bottom: none;
      }

      .i-head {
        .i-user {
          font-size: 24rpx;
          color: #2f6fed;
          font-weight: 600;
        }
        .i-date {
          font-size: 22rpx;
          color: #a8abb2;
        }
      }
      .i-content {
        font-size: 26rpx;
        color: #303133;
        margin-top: 8rpx;
        display: block;
      }
    }
  }
}
</style>
