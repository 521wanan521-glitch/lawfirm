<template>
  <view class="chat-page">
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
  </view>
</template>

<script>
import { confirmAction, cancelAction } from '@/api/index'

const BASE_URL = 'http://47.107.62.86/api'

export default {
  data() {
    return {
      messages: [],
      draft: '',
      streaming: false,
      scrollTo: '',
      controller: null,
      sessionId: null
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
}
</style>
