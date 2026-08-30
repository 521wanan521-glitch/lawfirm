<template>
  <view class="page">
    <view class="list">
      <view v-for="t in list" :key="t.id" class="time-card" @longpress="submitEntry(t)">
        <view class="head flex-between">
          <text class="case text-ellipsis">{{ t.caseTitle || t.caseNo }}</text>
          <text class="status" :class="'ts-' + t.status">{{ timeStatusLabel(t.status) }}</text>
        </view>
        <view class="info">
          <text class="i">{{ t.workDate }}</text>
          <text class="i">{{ t.hours }} 小时</text>
          <text class="i amount">¥{{ t.amount || 0 }}</text>
        </view>
        <text v-if="t.description" class="desc">{{ t.description }}</text>
      </view>

      <view v-if="!list.length && !loading" class="empty">暂无工时记录</view>
    </view>

    <view class="fab" @click="openCreate">＋</view>

    <!-- 记工时弹窗 -->
    <view v-if="showDialog" class="mask" @click="showDialog = false">
      <view class="dialog" @click.stop>
        <view class="d-title">记工时</view>
        <picker :range="cases" range-key="title" @change="onCaseChange">
          <view class="d-picker">{{ caseTitle || '选择案件' }}</view>
        </picker>
        <picker mode="date" :value="form.workDate" @change="onDateChange">
          <view class="d-picker">📅 {{ form.workDate }}</view>
        </picker>
        <input class="d-input" v-model="form.hours" type="digit" placeholder="工时（小时）" placeholder-class="ph" />
        <input class="d-input" v-model="form.rate" type="digit" placeholder="费率（元/小时，选填）" placeholder-class="ph" />
        <textarea class="d-textarea" v-model="form.description" placeholder="工作内容（必填）" placeholder-class="ph" />
        <view class="d-btn" @click="save">保存</view>
      </view>
    </view>
  </view>
</template>

<script>
import { pageTimeEntries, createTimeEntry, submitTimeEntry, caseOptions } from '@/api/index'
import { timeStatusLabel } from '@/utils/dict'

export default {
  data() {
    const d = new Date()
    const m = String(d.getMonth() + 1).padStart(2, '0')
    const day = String(d.getDate()).padStart(2, '0')
    return {
      list: [],
      cases: [],
      page: 1,
      size: 20,
      total: 0,
      loading: false,
      showDialog: false,
      caseTitle: '',
      form: {
        caseId: null,
        workDate: `${d.getFullYear()}-${m}-${day}`,
        hours: '',
        rate: '',
        description: ''
      }
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
    timeStatusLabel,
    async load(cb) {
      this.loading = true
      try {
        const data = await pageTimeEntries({ page: this.page, size: this.size })
        this.list = data.items
        this.total = data.total
      } finally {
        this.loading = false
        if (cb) cb()
      }
    },
    async openCreate() {
      const c = await caseOptions()
      this.cases = c.items
      this.showDialog = true
    },
    onCaseChange(e) {
      const c = this.cases[Number(e.detail.value)]
      this.form.caseId = c.id
      this.caseTitle = c.title
    },
    onDateChange(e) {
      this.form.workDate = e.detail.value
    },
    async save() {
      if (!this.form.caseId) return uni.showToast({ title: '请选择案件', icon: 'none' })
      if (!this.form.hours || parseFloat(this.form.hours) <= 0) return uni.showToast({ title: '请填写工时', icon: 'none' })
      if (!this.form.description.trim()) return uni.showToast({ title: '请填写工作内容', icon: 'none' })

      const data = {
        caseId: this.form.caseId,
        workDate: this.form.workDate,
        hours: parseFloat(this.form.hours),
        description: this.form.description.trim()
      }
      if (this.form.rate) data.rate = parseFloat(this.form.rate)
      try {
        await createTimeEntry(data)
        uni.showToast({ title: '已保存', icon: 'success' })
        this.showDialog = false
        this.page = 1
        this.load()
      } catch (e) {}
    },
    submitEntry(t) {
      if (t.status !== 'SUBMITTED' && t.status !== 'APPROVED') return
      uni.showModal({
        title: '提交工时',
        content: '确定提交该工时记录审核吗？',
        success: async (res) => {
          if (!res.confirm) return
          try {
            await submitTimeEntry(t.id)
            uni.showToast({ title: '已提交', icon: 'success' })
            this.load()
          } catch (e) {}
        }
      })
    }
  }
}
</script>

<style lang="scss" scoped>
.page { padding-bottom: 200rpx; }
.list { padding: 20rpx 24rpx; }
.time-card {
  background: #fff;
  border-radius: 16rpx;
  padding: 24rpx;
  margin-bottom: 20rpx;
  box-shadow: 0 2rpx 10rpx rgba(31, 35, 41, 0.04);

  .head {
    .case {
      flex: 1;
      min-width: 0;
      font-size: 28rpx;
      font-weight: 600;
      color: #1f2329;
      margin-right: 12rpx;
    }
    .status { font-size: 22rpx; color: #2f6fed; flex-shrink: 0; }
    .ts-APPROVED { color: #67c23a; }
    .ts-BILLED { color: #909399; }
  }
  .info {
    display: flex;
    gap: 20rpx;
    margin-top: 12rpx;
    .i { font-size: 24rpx; color: #606266; }
    .amount { color: #2f6fed; font-weight: 600; }
  }
  .desc {
    display: block;
    font-size: 24rpx;
    color: #909399;
    margin-top: 10rpx;
  }
}
.empty { text-align: center; color: #a8abb2; padding: 120rpx 0; font-size: 26rpx; }
.fab {
  position: fixed;
  right: 40rpx;
  bottom: 180rpx;
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
.mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 200;
  display: flex;
  align-items: flex-end;

  .dialog {
    width: 100%;
    background: #fff;
    border-radius: 24rpx 24rpx 0 0;
    padding: 40rpx 32rpx;
    padding-bottom: calc(40rpx + env(safe-area-inset-bottom));

    .d-title {
      font-size: 32rpx;
      font-weight: 700;
      text-align: center;
      margin-bottom: 30rpx;
    }
    .d-input, .d-picker {
      background: #f5f6f8;
      border-radius: 12rpx;
      padding: 22rpx;
      font-size: 28rpx;
      margin-bottom: 20rpx;
      color: #303133;
    }
    .ph { color: #c0c4cc; }
    .d-textarea {
      background: #f5f6f8;
      border-radius: 12rpx;
      padding: 22rpx;
      font-size: 28rpx;
      min-height: 120rpx;
      margin-bottom: 20rpx;
      width: 100%;
      box-sizing: border-box;
    }
    .d-btn {
      height: 88rpx;
      line-height: 88rpx;
      text-align: center;
      color: #fff;
      font-size: 30rpx;
      font-weight: 600;
      border-radius: 44rpx;
      background: linear-gradient(90deg, #2f6fed, #4a8bf5);
    }
  }
}
</style>
