<template>
  <view class="edit">
    <view class="card form">
      <view class="f-item">
        <text class="f-label">审批类型 <text class="req">*</text></text>
        <picker :range="templates" range-key="name" @change="onTemplateChange">
          <view class="f-picker">
            <text :class="{ ph: !templateName }">{{ templateName || '请选择类型' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
      <view class="f-item">
        <text class="f-label">标题 <text class="req">*</text></text>
        <input class="f-input" v-model="form.title" placeholder="请输入标题" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">申请内容 <text class="req">*</text></text>
        <textarea class="f-textarea" v-model="form.content" placeholder="请填写申请内容" placeholder-class="ph" />
      </view>
      <view class="f-item">
        <text class="f-label">审批人 <text class="req">*</text></text>
        <picker :range="approvers" range-key="realName" @change="onApproverChange">
          <view class="f-picker">
            <text :class="{ ph: !approverName }">{{ approverName || '请选择审批人' }}</text>
            <text class="arrow">›</text>
          </view>
        </picker>
      </view>
    </view>

    <view class="submit" @click="save">提 交</view>
  </view>
</template>

<script>
import { listTemplates, listApprovers, createInstance } from '@/api/index'

export default {
  data() {
    return {
      templates: [],
      approvers: [],
      templateName: '',
      approverName: '',
      form: {
        templateId: null,
        title: '',
        content: '',
        approverId: null
      }
    }
  },
  onLoad() {
    this.init()
  },
  methods: {
    async init() {
      this.templates = await listTemplates()
      this.approvers = await listApprovers()
    },
    onTemplateChange(e) {
      const t = this.templates[Number(e.detail.value)]
      this.form.templateId = t.id
      this.templateName = t.name
    },
    onApproverChange(e) {
      const u = this.approvers[Number(e.detail.value)]
      this.form.approverId = u.id
      this.approverName = u.realName
    },
    async save() {
      if (!this.form.templateId) return uni.showToast({ title: '请选择审批类型', icon: 'none' })
      if (!this.form.title.trim()) return uni.showToast({ title: '请输入标题', icon: 'none' })
      if (!this.form.content.trim()) return uni.showToast({ title: '请填写申请内容', icon: 'none' })
      if (!this.form.approverId) return uni.showToast({ title: '请选择审批人', icon: 'none' })

      try {
        await createInstance({
          templateId: this.form.templateId,
          title: this.form.title.trim(),
          content: this.form.content.trim(),
          approverId: this.form.approverId
        })
        uni.showToast({ title: '提交成功', icon: 'success' })
        setTimeout(() => uni.navigateBack(), 500)
      } catch (e) {}
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
        min-height: 160rpx;
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
