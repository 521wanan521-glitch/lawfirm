import MarkdownIt from 'markdown-it'
import hljs from 'highlight.js'
import DOMPurify from 'dompurify'
import 'highlight.js/styles/github.css'

const md = new MarkdownIt({
  html: false, // 禁用原始 HTML，防止 XSS
  linkify: true,
  breaks: true,
  highlight(str, lang) {
    if (lang && hljs.getLanguage(lang)) {
      try {
        return (
          '<pre class="hljs"><code>' +
          hljs.highlight(str, { language: lang, ignoreIllegals: true }).value +
          '</code></pre>'
        )
      } catch (e) {
        /* fallthrough */
      }
    }
    return '<pre class="hljs"><code>' + md.utils.escapeHtml(str) + '</code></pre>'
  }
})

/** 渲染 Markdown 为安全的 HTML */
export function renderMarkdown(text) {
  if (!text) return ''
  const html = md.render(text)
  return DOMPurify.sanitize(html)
}
