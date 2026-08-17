import request from './request'

export const listSessions = () => request.get('/assistant/sessions')
export const listMessages = (id) => request.get(`/assistant/sessions/${id}/messages`)
export const deleteSession = (id) => request.delete(`/assistant/sessions/${id}`)
export const renameSession = (id, title) => request.put(`/assistant/sessions/${id}`, { title })

/**
 * 流式对话（SSE）。后端事件：meta / delta / tool / tool_result / done / error
 * @param {{sessionId:number|null, message:string, caseId?:number, clientId?:number, onEvent:function, signal?:AbortSignal}} params
 */
export function chatStream({ sessionId, message, caseId, clientId, onEvent, signal }) {
  const token = localStorage.getItem('token')
  return fetch('/api/assistant/chat', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      Authorization: `Bearer ${token}`
    },
    body: JSON.stringify({ sessionId, message, caseId, clientId }),
    signal
  }).then(async (res) => {
    if (!res.ok || !res.body) {
      let msg = '请求失败'
      try {
        const j = await res.json()
        msg = j.message || msg
      } catch (e) {
        /* ignore */
      }
      throw new Error(msg)
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
        parseSse(raw, onEvent)
      }
    }
    if (buffer.trim()) {
      parseSse(buffer, onEvent)
    }
  })
}

function parseSse(raw, onEvent) {
  let event = 'message'
  const dataLines = []
  for (const line of raw.split('\n')) {
    if (line.startsWith('event:')) {
      event = line.slice(6).trim()
    } else if (line.startsWith('data:')) {
      dataLines.push(line.slice(5).trim())
    }
  }
  if (!dataLines.length) return
  const dataStr = dataLines.join('\n')
  let data = dataStr
  try {
    data = JSON.parse(dataStr)
  } catch (e) {
    /* 非 JSON 数据原样返回 */
  }
  if (typeof onEvent === 'function') onEvent(event, data)
}
