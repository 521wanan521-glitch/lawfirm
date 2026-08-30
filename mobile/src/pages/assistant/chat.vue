<template>
  <view class="chat-page">
    <view class="top-bar">
      <text class="title">AI 助手</text>
      <text class="setting" @click="openSetting">⚙️ 模型设置</text>
    </view>
    <scroll-view scroll-y class="msg-list" :scroll-into-view="scrollTo">
      <view v-if="!messages.length" class="welcome">
        <text class="w-icon">🤖</text>
        <text class="w-title">AI 助手</text>
        <text class="w-tip">可以查询案件、客户、日程，起草文书；写操作需人工确认后执行</text>
      </view>

      <view v-for="(m, i) in messages" :key="i" :id="'msg-' + i" class="msg" :class="m.role === 'user' ? 'mine' : 'ai'">
        <view class="bubble">
          <text class="content">{{ m.content }}</text>
          <view v-if="m.tools && m.tools.length" class="tools">
            <view v-for="t in m.tools" :key="t.id" class="tool-card">
              <text class="tool-name">🛠️ {{ t.summary || t.name }}</text>
              <view v-if="t.status === 'pending'" class="tool-actions">
                <view class="btn ok" @click="confirmTool(t)">确认执行</view>
                <view class="btn cancel" @click="cancelTool(t)">取消</view>
              </view>
              <text v-else-if="t.status === 'done'" class="tool-result" :class="{ fail: t.ok === false }">
                {{ t.ok === false ? '执行失败' : '已执行' }}
              </text>
            </view>
          </view>
        </view>
      </view>
    </scroll-view>

    <view class="input-bar">
      <input class="chat-input" v-model="draft" confirm-type="send" placeholder="输入你的问题..." placeholder-class="ph" @confirm="send" />
      <view class="send-btn" @click="send">{{ streaming ? '停止' : '发送' }}</view>
    </view>

    <!-- 模型设置弹窗 -->
    <view v-if="showSetting" class="mask" @click="showSetting = false">
      <view class="dialog" @click.stop>
        <view class="d-title">模型设置</view>
        <view class="d-tip">未配置时无法使用 AI 助手，Key 加密存储仅本人可见</view>
        <picker :range="providers" range-key="name" @change="onProviderChange">
          <view class="d-picker">{{ providerName || '选择厂商' }}</view>
        </picker>
        <input class="d-input" v-model="llmForm.apiKey" type="password" placeholder="API Key（必填）" placeholder-class="ph" />
        <input class="d-input" v-model="llmForm.baseUrl" placeholder="接口地址" placeholder-class="ph" />
        <input class="d-input" v-model="llmForm.model" placeholder="模型名称" placeholder-class="ph" />
        <view class="d-btn" @click="saveSetting">保存</view>
      </view>
    </view>
  </view>
</template>

<script>
import { confirmAction, cancelAction, getLlmConfig, saveLlmConfig } from '@/api/index'

const BASE_URL = 'http://47.107.62.86/api'

const LLM_PROVIDERS = [
  { key: 'deepseek', name: 'DeepSeek（推荐）', baseUrl: 'https://api.deepseek.com', model: 'deepseek-chat' },
  { key: 'qwen', name: '通义千问（阿里云）', baseUrl: 'https://dashscope.aliyuncs.com/compatible-mode/v1', model: 'qwen-plus' },
  { key: 'glm', name: '智谱 GLM', baseUrl: 'https://open.bigmodel.cn/api/paas/v4', model: 'glm-4-flash' },
  { key: 'kimi', name: 'Kimi 月之暗面', baseUrl: 'https://api.moonshot.cn/v1', model: 'moonshot-v1-8k' },
  { key: 'openai', name: 'OpenAI', baseUrl: 'https://api.openai.com/v1', model: 'gpt-4o-mini' },
  { key: 'custom', name: '自定义（OpenAI 兼容接口）', baseUrl: '', model: '' }
]

export default {
  data() {
    return {
      messages: [],
      draft: '',
      streaming: false,
      scrollTo: '',
      controller: null,
      sessionId: null,
      showSetting: false,
      providers: LLM_PROVIDERS,
      providerName: '',
      llmForm: { provider: 'deepseek', apiKey: '', baseUrl: 'https://api.deepseek.com', model: 'deepseek-chat' }
    }
  },
  methods: {
    scrollToBottom() {
      this.scrollTo = ''
      this.$nextTick(() => {
        this.scrollTo = 'msg-' + (this.messages.length - 1)
      })
    },
    async send() {
      if (this.streaming) {
        this.stop()
        return
      }
      const content = this.draft.trim()
      if (!content) return
      this.draft = ''
      this.messages.push({ role: 'user', content })
      const assistantItem = { role: 'assistant', content: '', tools: [] }
      this.messages.push(assistantItem)
      this.streaming = true
      this.scrollToBottom()

      try {
        const token = uni.getStorageSync('token')
        this.controller = new AbortController()
        const res = await fetch(BASE_URL + '/assistant/chat', {
          method: 'POST',
          headers: {
            'Content-Type': 'application/json',
            Authorization: `Bearer ${token}`
          },
          body: JSON.stringify({ sessionId: this.sessionId, message: content }),
          signal: this.controller.signal
        })
        if (!res.ok || !res.body) {
          throw new Error('请求失败')
        }
        const reader = res.body.getReader()
        const decoder = new TextDecoder('utf-8')
        let buffer = ''
        while (true) {
          const { done, value } = await reader.read()
          if (done) break
          buffer += decoder.decode(value, { stream: true }).replace(/\r\n/g, '\n')
          let idx
          while ((idx = buffer.indexOf('\n\n')) >= 0) {
            const raw = buffer.slice(0, idx)
            buffer = buffer.slice(idx + 2)
            this.parseEvent(raw, assistantItem)
          }
        }
        if (buffer.trim()) this.parseEvent(buffer, assistantItem)
      } catch (e) {
        if (e.name !== 'AbortError') {
          assistantItem.content += '\n\n⚠️ ' + (e.message || '请求失败')
        }
      } finally {
        this.streaming = false
        this.scrollToBottom()
      }
    },
    parseEvent(raw, item) {
      let event = 'message'
      const lines = []
      for (const line of raw.split('\n')) {
        if (line.startsWith('event:')) event = line.slice(6).trim()
        else if (line.startsWith('data:')) lines.push(line.slice(5).trim())
      }
      if (!lines.length) return
      let data = lines.join('\n')
      try {
        data = JSON.parse(data)
      } catch (e) {}

      switch (event) {
        case 'meta':
          this.sessionId = data.sessionId
          break
        case 'delta':
          item.content += data.content || ''
          this.scrollToBottom()
          break
        case 'tool':
          item.tools.push({
            id: data.id,
            name: data.name,
            summary: data.summary || '',
            status: data.pending ? 'pending' : 'running',
            actionId: data.actionId || null
          })
          this.scrollToBottom()
          break
        case 'tool_result': {
          const t = item.tools.find((x) => x.id === data.id)
          if (t && t.status !== 'pending') {
            t.status = 'done'
            t.ok = data.ok
          }
          break
        }
        case 'error':
          item.content += '\n\n⚠️ ' + (data.message || '发生错误')
          break
        default:
          break
      }
    },
    stop() {
      if (this.controller) {
        this.controller.abort()
        this.controller = null
      }
      this.streaming = false
    },
    async confirmTool(t) {
      if (!t.actionId) return
      try {
        const res = await confirmAction(t.actionId)
        t.status = 'done'
        t.ok = res.ok
        uni.showToast({ title: res.ok ? '操作已执行' : '执行失败', icon: res.ok ? 'success' : 'none' })
      } catch (e) {}
    },
    async cancelTool(t) {
      if (!t.actionId) return
      try {
        await cancelAction(t.actionId)
        t.status = 'done'
        t.ok = false
        uni.showToast({ title: '已取消', icon: 'none' })
      } catch (e) {}
    },
    async openSetting() {
      this.showSetting = true
      try {
        const cfg = await getLlmConfig()
        if (cfg && cfg.configured) {
          this.llmForm.provider = cfg.provider
          this.providerName = (LLM_PROVIDERS.find((p) => p.key === cfg.provider) || {}).name || cfg.provider
          this.llmForm.baseUrl = cfg.baseUrl
          this.llmForm.model = cfg.model
          this.llmForm.apiKey = ''
        }
      } catch (e) {}
    },
    onProviderChange(e) {
      const p = this.providers[Number(e.detail.value)]
      this.llmForm.provider = p.key
      this.providerName = p.name
      this.llmForm.baseUrl = p.baseUrl
      this.llmForm.model = p.model
    },
    async saveSetting() {
      if (!this.llmForm.apiKey.trim()) return uni.showToast({ title: '请填写 API Key', icon: 'none' })
      if (!this.llmForm.baseUrl.trim()) return uni.showToast({ title: '请填写接口地址', icon: 'none' })
      if (!this.llmForm.model.trim()) return uni.showToast({ title: '请填写模型名称', icon: 'none' })
      try {
        await saveLlmConfig({
          provider: this.llmForm.provider,
          apiKey: this.llmForm.apiKey.trim(),
          baseUrl: this.llmForm.baseUrl.trim(),
          model: this.llmForm.model.trim()
        })
        uni.showToast({ title: '已保存', icon: 'success' })
        this.showSetting = false
      } catch (e) {}
    }
  }
}
</script>

<style lang="scss" scoped>
.chat-page {
  display: flex;
  flex-direction: column;
  height: 100vh;
  background: #f5f6f8;

  .top-bar {
    display: flex;
    align-items: center;
    justify-content: space-between;
    background: #fff;
    padding: 16rpx 24rpx;
    border-bottom: 1px solid #f0f2f5;

    .title {
      font-size: 30rpx;
      font-weight: 600;
      color: #1f2329;
    }
    .setting {
      font-size: 24rpx;
      color: #2f6fed;
    }
  }

  .msg-list {
    flex: 1;
    padding: 24rpx;
    box-sizing: border-box;

    .welcome {
      display: flex;
      flex-direction: column;
      align-items: center;
      padding-top: 120rpx;

      .w-icon { font-size: 100rpx; }
      .w-title { font-size: 34rpx; font-weight: 700; margin-top: 20rpx; color: #1f2329; }
      .w-tip { font-size: 24rpx; color: #a8abb2; margin-top: 12rpx; text-align: center; padding: 0 40rpx; }
    }

    .msg {
      display: flex;
      margin-bottom: 24rpx;

      .bubble {
        max-width: 80%;
        padding: 20rpx 24rpx;
        border-radius: 16rpx;
        font-size: 28rpx;
        line-height: 1.6;

        .content {
          white-space: pre-wrap;
          word-break: break-word;
        }
      }

      &.mine {
        justify-content: flex-end;
        .bubble {
          background: #2f6fed;
          color: #fff;
          border-radius: 16rpx 4rpx 16rpx 16rpx;
        }
      }
      &.ai {
        justify-content: flex-start;
        .bubble {
          background: #fff;
          color: #303133;
          border-radius: 4rpx 16rpx 16rpx 16rpx;
          box-shadow: 0 2rpx 8rpx rgba(31, 35, 41, 0.04);
        }
      }
    }

    .tools {
      margin-top: 16rpx;

      .tool-card {
        background: #f8f9fb;
        border-radius: 12rpx;
        padding: 16rpx;
        margin-bottom: 12rpx;

        .tool-name {
          font-size: 24rpx;
          color: #606266;
          display: block;
        }
        .tool-actions {
          display: flex;
          gap: 16rpx;
          margin-top: 14rpx;

          .btn {
            flex: 1;
            text-align: center;
            padding: 12rpx 0;
            border-radius: 8rpx;
            font-size: 26rpx;
            font-weight: 600;
          }
          .ok { background: #2f6fed; color: #fff; }
          .cancel { background: #f0f2f5; color: #606266; }
        }
        .tool-result {
          font-size: 24rpx;
          color: #67c23a;
          display: block;
          margin-top: 8rpx;
        }
        .tool-result.fail { color: #f56c6c; }
      }
    }
  }

  .input-bar {
    display: flex;
    align-items: center;
    padding: 16rpx 24rpx;
    padding-bottom: calc(16rpx + env(safe-area-inset-bottom));
    background: #fff;
    border-top: 1px solid #f0f2f5;

    .chat-input {
      flex: 1;
      background: #f5f6f8;
      border-radius: 32rpx;
      padding: 16rpx 28rpx;
      font-size: 28rpx;
    }
    .ph { color: #a8abb2; }
    .send-btn {
      color: #fff;
      font-size: 28rpx;
      font-weight: 600;
      margin-left: 16rpx;
      background: #2f6fed;
      padding: 14rpx 32rpx;
      border-radius: 32rpx;
    }
  }

  .mask {
    position: fixed;
    inset: 0;
    background: rgba(0, 0, 0, 0.45);
    z-index: 1000;
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
        margin-bottom: 12rpx;
      }
      .d-tip {
        font-size: 22rpx;
        color: #a8abb2;
        text-align: center;
        margin-bottom: 24rpx;
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
}
</style>
