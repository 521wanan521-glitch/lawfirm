<template>
  <view class="edit">
    <view class="card form">
      <view class="f-item">
        <text class="f-label">案件名称 <text class="req">*</text></text>
        <input class="f-input" v-model="form.title" placeholder="请输入案件名称" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">原告</text>
        <input class="f-input" v-model="form.plaintiff" placeholder="原告名称" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">被告</text>
        <input class="f-input" v-model="form.defendant" placeholder="被告名称" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">客户 <text class="req">*</text></text>
        <picker :range="clients" range-key="name" @change="onClientChange">
          <view class="f-picker">
            <text :class="{ ph: !clientName }">{{ clientName || '请选择客户' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="f-item">
        <text class="f-label">案件类型 <text class="req">*</text></text>
        <picker :range="typeOptions" range-key="label" @change="onTypeChange">
          <view class="f-picker">
            <text>{{ typeLabel || '请选择类型' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="f-item">
        <text class="f-label">优先级</text>
        <picker :range="priorityOptions" range-key="label" @change="onPriorityChange">
          <view class="f-picker">
            <text>{{ priorityLabel(form.priority) }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="f-item">
        <text class="f-label">主办律师 <text class="req">*</text></text>
        <picker :range="users" range-key="realName" @change="onLeadChange">
          <view class="f-picker">
            <text :class="{ ph: !leadName }">{{ leadName || '请选择主办律师' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="f-item">
        <text class="f-label">受理法院</text>
        <input class="f-input" v-model="form.court" placeholder="受理法院" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">标的额(元)</text>
        <input class="f-input" v-model="form.caseAmount" type="digit" placeholder="标的额" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">收费金额(元)</text>
        <input class="f-input" v-model="form.fee" type="digit" placeholder="收费金额" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">立案日期</text>
        <picker mode="date" :value="form.filingDate" @change="onFilingChange">
          <view class="f-picker">
            <text :class="{ ph: !form.filingDate }">{{ form.filingDate || '选择日期' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="f-item col">
        <text class="f-label">案情摘要</text>
        <textarea class="f-textarea" v-model="form.description" placeholder="案情摘要" placeholder-class="ph" />
      </view>
    </view>

    <view class="submit" @click="save">保 存</view>
  </view>
</template>

<script>
import { getCase, createCase, updateCase, pageClients, userOptions } from '@/api/index'
import { CASE_TYPE_MAP, PRIORITY_MAP, priorityLabel } from '@/utils/dict'

export default {
  data() {
    return {
      id: null,
      clients: [],
      users: [],
      clientName: '',
      typeLabel: '',
      leadName: '',
      typeOptions: Object.keys(CASE_TYPE_MAP).map((k) => ({ value: k, label: CASE_TYPE_MAP[k] })),
      priorityOptions: Object.keys(PRIORITY_MAP).map((k) => ({ value: k, label: PRIORITY_MAP[k] })),
      form: {
        title: '',
        plaintiff: '',
        defendant: '',
        clientId: null,
        type: 'CIVIL',
        priority: 'MEDIUM',
        leadLawyerId: null,
        court: '',
        caseAmount: '',
        filingDate: '',
        description: '',
        fee: ''
      }
    }
  },
  onLoad(options) {
    this.id = options.id
    this.init()
  },
  methods: {
    priorityLabel,
    async init() {
      const [c, u] = await Promise.all([
        pageClients({ page: 1, size: 200 }),
        userOptions()
      ])
      this.clients = c.items
      this.users = u
      if (this.id) {
        const d = await getCase(this.id)
        Object.assign(this.form, {
          title: d.title,
          plaintiff: d.plaintiff,
          defendant: d.defendant,
          clientId: d.clientId,
          type: d.type,
          priority: d.priority,
          leadLawyerId: d.leadLawyerId,
          court: d.court,
          caseAmount: d.caseAmount,
          filingDate: d.filingDate || '',
          description: d.description,
          fee: d.fee
        })
        this.clientName = d.clientName || ''
        this.typeLabel = CASE_TYPE_MAP[d.type]
        this.leadName = d.leadLawyerName || ''
      }
    },
    onClientChange(e) {
      const c = this.clients[Number(e.detail.value)]
      this.form.clientId = c.id
      this.clientName = c.name
    },
    onTypeChange(e) {
      const t = this.typeOptions[Number(e.detail.value)]
      this.form.type = t.value
      this.typeLabel = t.label
    },
    onPriorityChange(e) {
      this.form.priority = this.priorityOptions[Number(e.detail.value)].value
    },
    onLeadChange(e) {
      const u = this.users[Number(e.detail.value)]
      this.form.leadLawyerId = u.id
      this.leadName = u.realName
    },
    onFilingChange(e) {
      this.form.filingDate = e.detail.value
    },
    async save() {
      if (!this.form.title.trim()) return uni.showToast({ title: '请输入案件名称', icon: 'none' })
      if (!this.form.clientId) return uni.showToast({ title: '请选择客户', icon: 'none' })
      if (!this.form.leadLawyerId) return uni.showToast({ title: '请选择主办律师', icon: 'none' })

      const data = { ...this.form }
      if (!data.caseAmount) delete data.caseAmount
      if (!data.fee) delete data.fee

      uni.showLoading({ title: '保存中...' })
      try {
        if (this.id) {
          await updateCase(this.id, data)
        } else {
          await createCase(data)
        }
        uni.hideLoading()
        uni.showToast({ title: '保存成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (e) {
        uni.hideLoading()
      }
    }
  }
}
</script>

<style lang="scss" scoped>
.edit {
  padding: 20rpx 0 140rpx;

  .form {
    .f-item {
      padding: 20rpx 0;
      border-bottom: 1px solid #f5f6f8;

      &:last-child {
        border-bottom: none;
      }

      .f-label {
        display: block;
        font-size: 26rpx;
        color: #606266;
        margin-bottom: 14rpx;

        .req {
          color: #f56c6c;
        }
      }
      .f-input {
        font-size: 28rpx;
      }
      .ph {
        color: #c0c4cc;
      }
      .f-picker {
        display: flex;
        align-items: center;
        justify-content: space-between;
        font-size: 28rpx;
        color: #303133;

        .ph {
          color: #c0c4cc;
        }
        .arrow {
          color: #c0c4cc;
        }
      }
      .f-textarea {
        width: 100%;
        min-height: 120rpx;
        font-size: 28rpx;
      }
    }
  }

  .submit {
    margin: 30rpx 24rpx;
    height: 92rpx;
    line-height: 92rpx;
    text-align: center;
    color: #fff;
    font-size: 32rpx;
    font-weight: 600;
    border-radius: 46rpx;
    background: linear-gradient(90deg, #2f6fed, #4a8bf5);
    box-shadow: 0 8rpx 20rpx rgba(47, 111, 237, 0.3);
  }
}
</style>
