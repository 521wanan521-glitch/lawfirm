<template>
  <view class="page">
    <view class="list">
      <view v-for="u in list" :key="u.id" class="member-card">
        <view class="head flex-between">
          <view class="name-row flex">
            <text class="name">{{ u.realName }}</text>
            <text class="role" :class="'role-' + u.role">{{ roleLabel(u.role) }}</text>
          </view>
          <text class="enabled" :class="{ off: !u.enabled }">{{ u.enabled ? '启用' : '停用' }}</text>
        </view>
        <view class="meta">
          <text class="m">{{ u.username }}</text>
          <text v-if="u.department" class="m">{{ u.department }}</text>
          <text v-if="u.phone" class="m">{{ u.phone }}</text>
        </view>
      </view>

      <view v-if="!list.length && !loading" class="empty">暂无成员</view>
    </view>
  </view>
</template>

<script>
import { pageUsers } from '@/api/index'
import { roleLabel } from '@/utils/dict'

export default {
  data() {
    return {
      list: [],
      page: 1,
      size: 50,
      total: 0,
      loading: false
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    roleLabel,
    async load() {
      this.loading = true
      try {
        const data = await pageUsers({ page: this.page, size: this.size })
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
.member-card {
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
      }
      .role {
        font-size: 20rpx;
        color: #2f6fed;
        background: #eef3fe;
        border-radius: 6rpx;
        padding: 2rpx 12rpx;
        margin-left: 12rpx;
      }
      .role-ADMIN { color: #f56c6c; background: #fef0f0; }
      .role-PARTNER { color: #e6a23c; background: #fdf6ec; }
    }
    .enabled {
      font-size: 24rpx;
      color: #67c23a;
      flex-shrink: 0;
    }
    .enabled.off { color: #a8abb2; }
  }
  .meta {
    display: flex;
    flex-wrap: wrap;
    gap: 16rpx;
    margin-top: 12rpx;
    .m { font-size: 24rpx; color: #909399; }
  }
}
.empty { text-align: center; color: #a8abb2; padding: 120rpx 0; font-size: 26rpx; }
</style>
