<template>
  <view class="page">
    <!-- 月份导航 -->
    <view class="month-bar">
      <text class="nav" @click="prevMonth">‹</text>
      <text class="month">{{ year }}年{{ month }}月</text>
      <text class="nav" @click="nextMonth">›</text>
    </view>

    <!-- 日程列表 -->
    <view class="list">
      <view v-for="(group, i) in grouped" :key="i">
        <view class="date-header">{{ group.date }}（{{ group.weekday }}）</view>
        <view v-for="e in group.events" :key="e.id" class="event-card" @longpress="removeEvent(e)">
          <view class="e-left" :class="'et-' + e.type"></view>
          <view class="e-main">
            <text class="e-title">{{ e.title }}</text>
            <text class="e-meta">
              {{ eventTypeLabel(e.type) }} · {{ timeOf(e.startTime) }}{{ e.endTime ? ' - ' + timeOf(e.endTime) : '' }}
            </text>
            <text v-if="e.location" class="e-loc">📍 {{ e.location }}</text>
          </view>
        </view>
      </view>

      <view v-if="!grouped.length" class="empty">
        <text class="empty-icon">📅</text>
        <text class="empty-text">本月暂无日程</text>
      </view>
    </view>

    <!-- 新建按钮 -->
    <view class="fab" @click="openCreate">＋</view>

    <!-- 新建弹窗 -->
    <view v-if="showDialog" class="mask" @click="showDialog = false">
      <view class="dialog" @click.stop>
        <view class="d-title">新建日程</view>
        <input class="d-input" v-model="form.title" placeholder="日程标题（必填）" placeholder-class="ph" />
        <picker :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="d-picker">{{ typeLabel || '选择类型' }}</view>
        </picker>
        <picker mode="date" :value="form.date" @change="onDateChange">
          <view class="d-picker">📅 {{ form.date }}</view>
        </picker>
        <view class="d-time-row">
          <picker mode="time" :value="form.startTime" @change="onStartChange">
            <view class="d-picker">开始 {{ form.startTime }}</view>
          </picker>
          <picker mode="time" :value="form.endTime" @change="onEndChange">
            <view class="d-picker">结束 {{ form.endTime }}</view>
          </picker>
        </view>
        <input class="d-input" v-model="form.location" placeholder="地点（选填）" placeholder-class="ph" />
        <textarea class="d-textarea" v-model="form.description" placeholder="说明（选填）" placeholder-class="ph" />
        <view class="d-btn" @click="save">保存</view>
      </view>
    </view>
  </view>
</template>

<script>
import { listEvents, createEvent, deleteEvent } from '@/api/index'
import { EVENT_TYPE_MAP, eventTypeLabel } from '@/utils/dict'

export default {
  data() {
    const now = new Date()
    return {
      year: now.getFullYear(),
      month: now.getMonth() + 1,
      events: [],
      showDialog: false,
      typeOptions: Object.keys(EVENT_TYPE_MAP).map((k) => ({ value: k, label: EVENT_TYPE_MAP[k] })),
      typeLabel: '',
      form: {
        title: '',
        type: 'TASK',
        date: this.todayStr(),
        startTime: '09:00',
        endTime: '10:00',
        location: '',
        description: ''
      }
    }
  },
  computed: {
    grouped() {
      const map = {}
      for (const e of this.events) {
        const d = (e.startTime || '').substring(0, 10)
        if (!map[d]) map[d] = []
        map[d].push(e)
      }
      const weekdays = ['周日', '周一', '周二', '周三', '周四', '周五', '周六']
      return Object.keys(map)
        .sort()
        .map((date) => {
          const wd = weekdays[new Date(date).getDay()]
          return { date, weekday: wd, events: map[date] }
        })
    }
  },
  onShow() {
    this.load()
  },
  methods: {
    eventTypeLabel,
    todayStr() {
      const d = new Date()
      const m = String(d.getMonth() + 1).padStart(2, '0')
      const day = String(d.getDate()).padStart(2, '0')
      return `${d.getFullYear()}-${m}-${day}`
    },
    timeOf(t) {
      return t ? t.substring(11, 16) : ''
    },
    async load() {
      const start = `${this.year}-${String(this.month).padStart(2, '0')}-01T00:00:00`
      const lastDay = new Date(this.year, this.month, 0).getDate()
      const end = `${this.year}-${String(this.month).padStart(2, '0')}-${String(lastDay).padStart(2, '0')}T23:59:59`
      this.events = await listEvents({ start, end })
    },
    prevMonth() {
      if (this.month === 1) {
        this.month = 12
        this.year--
      } else {
        this.month--
      }
      this.load()
    },
    nextMonth() {
      if (this.month === 12) {
        this.month = 1
        this.year++
      } else {
        this.month++
      }
      this.load()
    },
    openCreate() {
      this.form = { ...this.form, title: '', date: this.todayStr(), startTime: '09:00', endTime: '10:00', location: '', description: '' }
      this.typeLabel = ''
      this.showDialog = true
    },
    onTypeChange(e) {
      const t = this.typeOptions[Number(e.detail.value)]
      this.form.type = t.value
      this.typeLabel = t.label
    },
    onDateChange(e) {
      this.form.date = e.detail.value
    },
    onStartChange(e) {
      this.form.startTime = e.detail.value
    },
    onEndChange(e) {
      this.form.endTime = e.detail.value
    },
    async save() {
      if (!this.form.title.trim()) return uni.showToast({ title: '请输入标题', icon: 'none' })
      const data = {
        title: this.form.title.trim(),
        type: this.form.type,
        startTime: `${this.form.date}T${this.form.startTime}:00`,
        endTime: `${this.form.date}T${this.form.endTime}:00`,
        location: this.form.location,
        description: this.form.description
      }
      try {
        await createEvent(data)
        uni.showToast({ title: '已保存', icon: 'success' })
        this.showDialog = false
        this.load()
      } catch (e) {}
    },
    removeEvent(e) {
      uni.showModal({
        title: '删除日程',
        content: '确定删除「' + e.title + '」吗？',
        success: async (res) => {
          if (res.confirm) {
            try {
              await deleteEvent(e.id)
              uni.showToast({ title: '已删除', icon: 'success' })
              this.load()
            } catch (err) {}
          }
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

.month-bar {
  display: flex;
  align-items: center;
  justify-content: space-between;
  background: #fff;
  padding: 20rpx 40rpx;
  position: sticky;
  top: 0;
  z-index: 10;

  .nav {
    font-size: 40rpx;
    color: #2f6fed;
    padding: 0 20rpx;
  }
  .month {
    font-size: 30rpx;
    font-weight: 600;
  }
}

.list {
  padding: 20rpx 24rpx;

  .date-header {
    font-size: 24rpx;
    color: #909399;
    padding: 16rpx 4rpx 8rpx;
  }

  .event-card {
    display: flex;
    background: #fff;
    border-radius: 12rpx;
    padding: 20rpx;
    margin-bottom: 14rpx;
    box-shadow: 0 2rpx 8rpx rgba(31, 35, 41, 0.04);

    .e-left {
      width: 8rpx;
      border-radius: 4rpx;
      margin-right: 16rpx;
      background: #2f6fed;
      flex-shrink: 0;
    }
    .et-COURT {
      background: #f56c6c;
    }
    .et-MEETING {
      background: #2f6fed;
    }
    .et-TASK {
      background: #67c23a;
    }
    .et-REMINDER {
      background: #e6a23c;
    }

    .e-main {
      flex: 1;
      min-width: 0;

      .e-title {
        font-size: 28rpx;
        color: #1f2329;
        font-weight: 600;
        display: block;
      }
      .e-meta {
        font-size: 22rpx;
        color: #909399;
        margin-top: 6rpx;
        display: block;
      }
      .e-loc {
        font-size: 22rpx;
        color: #a8abb2;
        margin-top: 4rpx;
        display: block;
      }
    }
  }

  .empty {
    display: flex;
    flex-direction: column;
    align-items: center;
    padding-top: 120rpx;

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
    .d-input {
      background: #f5f6f8;
      border-radius: 12rpx;
      padding: 22rpx;
      font-size: 28rpx;
      margin-bottom: 20rpx;
    }
    .ph {
      color: #c0c4cc;
    }
    .d-picker {
      background: #f5f6f8;
      border-radius: 12rpx;
      padding: 22rpx;
      font-size: 28rpx;
      margin-bottom: 20rpx;
      color: #303133;
    }
    .d-time-row {
      display: flex;
      gap: 20rpx;

      .d-picker {
        flex: 1;
      }
    }
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
