// Converts lawfirm docs/*.md into:
//   - exports/html/*.html  (styled, for PDF via headless Edge)
//   - exports/docx-build/<name>/... (OOXML tree, zipped to .docx by a follow-up step)
// No external deps beyond markdown-it already present in the frontend.
const fs = require('fs')
const path = require('path')
const MarkdownIt = require('../frontend/node_modules/markdown-it')

const DOCS_DIR = __dirname
const EXPORTS = path.join(DOCS_DIR, 'exports')
const HTML_DIR = path.join(EXPORTS, 'html')
const DOCX_DIR = path.join(EXPORTS, 'docx-build')

const FILES = [
  'FEATURE_GUIDE.md',
  'USER_GUIDE.md',
  'AI_ASSISTANT_GUIDE.md',
  'ARCHITECTURE.md',
  'DEPLOY_ALIYUN.md',
  'DEMO_SCRIPT.md'
]

const md = new MarkdownIt({ html: false, linkify: true, breaks: false })

// ---------- XML helpers ----------
function esc(s) {
  return String(s)
    .replace(/&/g, '&amp;')
    .replace(/</g, '&lt;')
    .replace(/>/g, '&gt;')
    .replace(/"/g, '&quot;')
}

function rPrXml(f) {
  const parts = []
  if (f.b) parts.push('<w:b/>')
  if (f.i) parts.push('<w:i/>')
  if (f.code) parts.push('<w:rFonts w:ascii="Consolas" w:hAnsi="Consolas" w:eastAsia="Consolas"/>')
  if (f.color) parts.push('<w:color w:val="' + f.color + '"/>')
  if (f.link) { parts.push('<w:color w:val="0563C1"/>'); parts.push('<w:u w:val="single"/>') }
  parts.push('<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="微软雅黑" w:cs="Times New Roman"/>')
  return '<w:rPr>' + parts.join('') + '</w:rPr>'
}

function run(text, fmt) {
  return '<w:r>' + rPrXml(fmt || {}) + '<w:t xml:space="preserve">' + esc(text) + '</w:t></w:r>'
}

function renderInline(children) {
  if (!children || !children.length) return ''
  let out = ''
  const stack = []
  let runOpen = false
  let openSig = null

  function currentFmt() {
    const f = { b: false, i: false, code: false, link: false, color: null }
    for (const x of stack) {
      if (x.b) f.b = true
      if (x.i) f.i = true
      if (x.code) f.code = true
      if (x.link) f.link = true
      if (x.color) f.color = x.color
    }
    return f
  }
  function sig(f) { return [f.b, f.i, f.code, f.link, f.color].join('|') }
  function closeRun() { if (runOpen) { out += '</w:r>'; runOpen = false; openSig = null } }
  function ensureRun() {
    const f = currentFmt()
    const s = sig(f)
    if (runOpen && openSig !== s) closeRun()
    if (!runOpen) { out += '<w:r>' + rPrXml(f); runOpen = true; openSig = s }
  }

  for (const t of children) {
    switch (t.type) {
      case 'text':
        ensureRun()
        out += '<w:t xml:space="preserve">' + esc(t.content) + '</w:t>'
        break
      case 'softbreak':
        ensureRun()
        out += '<w:t xml:space="preserve"> </w:t>'
        break
      case 'hardbreak':
        closeRun()
        out += '<w:br/>'
        break
      case 'code_inline':
        closeRun()
        out += '<w:r>' + rPrXml({ code: true, color: 'C7254E' }) +
          '<w:t xml:space="preserve">' + esc(t.content) + '</w:t></w:r>'
        break
      case 'strong_open': stack.push({ b: true }); break
      case 'strong_close': stack.pop(); break
      case 'em_open': stack.push({ i: true }); break
      case 'em_close': stack.pop(); break
      case 's_open': stack.push({ s: true }); break
      case 's_close': stack.pop(); break
      case 'link_open': stack.push({ link: true }); break
      case 'link_close': stack.pop(); break
      default: break
    }
  }
  closeRun()
  return out
}

// ---------- paragraph builders ----------
function para(runsXml, opts) {
  opts = opts || {}
  const pPr = []
  if (opts.heading) {
    const h = opts.heading
    const sizes = { 1: 44, 2: 32, 3: 28, 4: 24, 5: 22, 6: 20 }
    const colors = { 1: '1F3864', 2: '1F3864', 3: '2E5395', 4: '404040', 5: '404040', 6: '595959' }
    pPr.push('<w:spacing w:before="' + (h === 1 ? 240 : 200) + '" w:after="120"/>')
    pPr.push('<w:jc w:val="left"/>')
    pPr.push('<w:rPr><w:b/><w:color w:val="' + colors[h] + '"/><w:sz w:val="' + sizes[h] + '"/><w:szCs w:val="' + sizes[h] + '"/></w:rPr>')
  } else {
    pPr.push('<w:spacing w:after="100" w:line="300" w:lineRule="auto"/>')
  }
  if (opts.indent) pPr.push('<w:ind w:left="' + opts.indent + '" w:hanging="240"/>')
  if (opts.bq) {
    pPr.push('<w:ind w:left="360" w:right="120"/>')
    pPr.push('<w:pBdr><w:left w:val="single" w:sz="12" w:space="4" w:color="9CC3E5"/></w:pBdr>')
    pPr.push('<w:rPr><w:color w:val="595959"/></w:rPr>')
  }
  if (opts.shd) pPr.push('<w:shd w:val="clear" w:color="auto" w:fill="' + opts.shd + '"/>')
  if (opts.hr) pPr.push('<w:pBdr><w:bottom w:val="single" w:sz="6" w:space="1" w:color="auto"/></w:pBdr>')
  return '<w:p><w:pPr>' + pPr.join('') + '</w:pPr>' + runsXml + '</w:p>'
}

function renderTable(rows) {
  const colCount = rows.reduce((m, r) => Math.max(m, r.length), 0)
  const colW = Math.floor(9360 / colCount)
  let xml = '<w:tbl><w:tblPr><w:tblW w:w="9360" w:type="dxa"/>' +
    '<w:tblBorders>' +
    '<w:top w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>' +
    '<w:left w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>' +
    '<w:bottom w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>' +
    '<w:right w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>' +
    '<w:insideH w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>' +
    '<w:insideV w:val="single" w:sz="4" w:space="0" w:color="BFBFBF"/>' +
    '</w:tblBorders></w:tblPr>'
  xml += '<w:tblGrid>' + Array(colCount).fill('<w:gridCol w:w="' + colW + '"/>').join('') + '</w:tblGrid>'
  rows.forEach((row, ri) => {
    xml += '<w:tr>'
    for (let c = 0; c < colCount; c++) {
      const cell = row[c]
      const isHeader = ri === 0 || (cell && cell.header)
      const content = cell ? renderInline(cell.children) : ''
      let tcPr = '<w:tcPr><w:tcW w:w="' + colW + '" w:type="dxa"/>'
      if (isHeader) tcPr += '<w:shd w:val="clear" w:color="auto" w:fill="DEEAF6"/>'
      tcPr += '</w:tcPr>'
      const rPr = isHeader ? '<w:rPr><w:b/></w:rPr>' : ''
      xml += '<w:tc>' + tcPr + '<w:p><w:pPr><w:spacing w:after="0" w:line="240" w:lineRule="auto"/></w:pPr>' + rPr + content + '</w:p></w:tc>'
    }
    xml += '</w:tr>'
  })
  xml += '</w:tbl>'
  return xml
}

// ---------- token parser (block level) ----------
function parseBlocks(tokens, i) {
  const nodes = []
  while (i < tokens.length) {
    const t = tokens[i]
    if (t.nesting === -1) return { nodes, end: i }
    switch (t.type) {
      case 'heading_open': {
        const level = Number(t.tag[1])
        nodes.push({ type: 'heading', level, children: tokens[i + 1].children })
        i += 3
        break
      }
      case 'paragraph_open': {
        nodes.push({ type: 'paragraph', children: tokens[i + 1].children })
        i += 3
        break
      }
      case 'bullet_list_open':
      case 'ordered_list_open': {
        const ordered = t.type === 'ordered_list_open'
        const closeType = t.type.replace('_open', '_close')
        const items = []
        let j = i + 1
        while (j < tokens.length && tokens[j].type !== closeType) {
          if (tokens[j].type === 'list_item_open') {
            const item = { blocks: [] }
            let k = j + 1
            while (k < tokens.length && tokens[k].type !== 'list_item_close') {
              if (tokens[k].type === 'paragraph_open') {
                item.blocks.push({ type: 'paragraph', children: tokens[k + 1].children })
                k += 3
              } else if (tokens[k].type.endsWith('_open') && tokens[k].nesting === 1) {
                const sub = parseBlocks(tokens, k)
                item.blocks.push(...sub.nodes)
                k = sub.end + 1
              } else {
                k++
              }
            }
            items.push(item)
            j = k + 1
          } else {
            j++
          }
        }
        nodes.push({ type: 'list', ordered, items })
        i = j + 1
        break
      }
      case 'table_open': {
        const rows = []
        let j = i + 1
        while (j < tokens.length && tokens[j].type !== 'table_close') {
          if (tokens[j].type === 'tr_open') {
            const cells = []
            let k = j + 1
            while (k < tokens.length && tokens[k].type !== 'tr_close') {
              if (tokens[k].type === 'th_open' || tokens[k].type === 'td_open') {
                cells.push({ header: tokens[k].type === 'th_open', children: tokens[k + 1].children })
                k += 3
              } else {
                k++
              }
            }
            rows.push(cells)
            j = k + 1
          } else {
            j++
          }
        }
        nodes.push({ type: 'table', rows })
        i = j + 1
        break
      }
      case 'blockquote_open': {
        const sub = parseBlocks(tokens, i + 1)
        nodes.push({ type: 'blockquote', nodes: sub.nodes })
        i = sub.end + 1
        break
      }
      case 'hr': nodes.push({ type: 'hr' }); i += 1; break
      case 'fence':
      case 'code_block': nodes.push({ type: 'code', content: t.content, info: t.info || '' }); i += 1; break
      default: i += 1; break
    }
  }
  return { nodes, end: i }
}

// ---------- node renderer ----------
function renderNodes(nodes, ctx) {
  ctx = ctx || {}
  let xml = ''
  for (const node of nodes) {
    switch (node.type) {
      case 'heading':
        xml += para(renderInline(node.children), { heading: node.level, bq: ctx.bq })
        break
      case 'paragraph':
        xml += para(renderInline(node.children), { bq: ctx.bq })
        break
      case 'list': {
        node.items.forEach((item, idx) => {
          const bullet = node.ordered ? String(idx + 1) + '. ' : '\u2022 '
          const first = item.blocks[0] || { type: 'paragraph', children: [] }
          const text = renderInline(first.children)
          xml += para(run(bullet) + text, { indent: 360, bq: ctx.bq })
          for (let b = 1; b < item.blocks.length; b++) {
            xml += renderNodes([item.blocks[b]], { indent: 720, bq: ctx.bq })
          }
        })
        break
      }
      case 'table':
        xml += renderTable(node.rows)
        xml += para('', {})
        break
      case 'blockquote':
        xml += renderNodes(node.nodes, { bq: true })
        break
      case 'hr':
        xml += para('', { hr: true })
        break
      case 'code': {
        const lines = node.content.replace(/\n$/, '').split('\n')
        for (const line of lines) {
          xml += '<w:p><w:pPr><w:shd w:val="clear" w:color="auto" w:fill="F5F5F5"/><w:spacing w:after="0"/></w:pPr>' +
            run(line === '' ? ' ' : line, { code: true }) + '</w:p>'
        }
        xml += para('', {})
        break
      }
      default: break
    }
  }
  return xml
}

function buildDocx(src) {
  const tokens = md.parse(src, {})
  const { nodes } = parseBlocks(tokens, 0)
  const body = renderNodes(nodes, {})
  return '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
    '<w:document xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">' +
    '<w:body>' + body +
    '<w:sectPr><w:pgSz w:w="11906" w:h="16838"/>' +
    '<w:pgMar w:top="1440" w:right="1440" w:bottom="1440" w:left="1440" w:header="720" w:footer="720" w:gutter="0"/>' +
    '</w:sectPr></w:body></w:document>'
}

// ---------- static OOXML parts ----------
const CONTENT_TYPES = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
  '<Types xmlns="http://schemas.openxmlformats.org/package/2006/content-types">' +
  '<Default Extension="rels" ContentType="application/vnd.openxmlformats-package.relationships+xml"/>' +
  '<Default Extension="xml" ContentType="application/xml"/>' +
  '<Override PartName="/word/document.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.document.main+xml"/>' +
  '<Override PartName="/word/styles.xml" ContentType="application/vnd.openxmlformats-officedocument.wordprocessingml.styles+xml"/>' +
  '</Types>'

const RELS = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
  '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
  '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument" Target="word/document.xml"/>' +
  '</Relationships>'

const DOC_RELS = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
  '<Relationships xmlns="http://schemas.openxmlformats.org/package/2006/relationships">' +
  '<Relationship Id="rId1" Type="http://schemas.openxmlformats.org/officeDocument/2006/relationships/styles" Target="styles.xml"/>' +
  '</Relationships>'

const STYLES = '<?xml version="1.0" encoding="UTF-8" standalone="yes"?>' +
  '<w:styles xmlns:w="http://schemas.openxmlformats.org/wordprocessingml/2006/main">' +
  '<w:docDefaults><w:rPrDefault><w:rPr>' +
  '<w:rFonts w:ascii="Calibri" w:hAnsi="Calibri" w:eastAsia="微软雅黑" w:cs="Times New Roman"/>' +
  '<w:sz w:val="21"/><w:szCs w:val="21"/></w:rPr></w:rPrDefault>' +
  '<w:pPrDefault><w:pPr><w:spacing w:after="120" w:line="300" w:lineRule="auto"/></w:pPr></w:pPrDefault></w:docDefaults>' +
  '</w:styles>'

// ---------- HTML (for PDF) ----------
const HTML_CSS = `
@page { size: A4; margin: 16mm 15mm; }
* { box-sizing: border-box; }
body { font-family: "Microsoft YaHei", "微软雅黑", "PingFang SC", sans-serif; color: #1f2329; font-size: 10.5pt; line-height: 1.75; margin: 0; }
h1 { font-size: 20pt; color: #1f3864; border-bottom: 2px solid #1f3864; padding-bottom: 6px; margin: 8px 0 14px; }
h2 { font-size: 15pt; color: #1f3864; margin: 18px 0 8px; border-bottom: 1px solid #d0d7e2; padding-bottom: 4px; }
h3 { font-size: 12.5pt; color: #2e5395; margin: 14px 0 6px; }
h4, h5, h6 { font-size: 11pt; color: #404040; margin: 12px 0 4px; }
p { margin: 6px 0; }
ul, ol { margin: 6px 0 6px 0; padding-left: 22px; }
li { margin: 2px 0; }
table { border-collapse: collapse; width: 100%; margin: 10px 0; font-size: 9.5pt; page-break-inside: auto; }
th, td { border: 1px solid #c5ccd6; padding: 5px 8px; text-align: left; vertical-align: top; }
th { background: #eef3fa; font-weight: 600; color: #1f3864; }
tr { page-break-inside: avoid; }
blockquote { margin: 10px 0; padding: 6px 12px; border-left: 4px solid #9cc3e5; background: #f4f8fc; color: #595959; }
blockquote p { margin: 3px 0; }
code { font-family: Consolas, "Courier New", monospace; background: #f4f4f4; padding: 1px 4px; border-radius: 3px; font-size: 9pt; color: #c7254e; }
pre { background: #f5f5f5; border: 1px solid #e0e0e0; padding: 10px; border-radius: 4px; overflow-x: hidden; }
pre code { background: none; color: #1f2329; padding: 0; }
hr { border: none; border-top: 1px solid #d0d7e2; margin: 14px 0; }
a { color: #0563c1; text-decoration: none; }
em { font-style: italic; }
strong { font-weight: 700; }
`

function buildHtml(src, title) {
  const body = md.render(src)
  return '<!DOCTYPE html><html lang="zh-CN"><head><meta charset="UTF-8">' +
    '<title>' + esc(title) + '</title><style>' + HTML_CSS + '</style></head>' +
    '<body>' + body + '</body></html>'
}

// ---------- main ----------
function main() {
  fs.mkdirSync(HTML_DIR, { recursive: true })
  fs.mkdirSync(DOCX_DIR, { recursive: true })

  const manifest = []
  for (const file of FILES) {
    const src = fs.readFileSync(path.join(DOCS_DIR, file), 'utf8')
    const base = file.replace(/\.md$/, '')
    const title = (src.match(/^#\s+(.+)$/m) || [])[1] || base

    // HTML
    const htmlPath = path.join(HTML_DIR, base + '.html')
    fs.writeFileSync(htmlPath, buildHtml(src, title), 'utf8')

    // DOCX build tree
    const docDir = path.join(DOCX_DIR, base)
    fs.mkdirSync(path.join(docDir, '_rels'), { recursive: true })
    fs.mkdirSync(path.join(docDir, 'word', '_rels'), { recursive: true })
    fs.writeFileSync(path.join(docDir, '[Content_Types].xml'), CONTENT_TYPES, 'utf8')
    fs.writeFileSync(path.join(docDir, '_rels', '.rels'), RELS, 'utf8')
    fs.writeFileSync(path.join(docDir, 'word', 'document.xml'), buildDocx(src), 'utf8')
    fs.writeFileSync(path.join(docDir, 'word', '_rels', 'document.xml.rels'), DOC_RELS, 'utf8')
    fs.writeFileSync(path.join(docDir, 'word', 'styles.xml'), STYLES, 'utf8')

    manifest.push({ base, title, html: htmlPath, docxDir: docDir })
    console.log('OK ' + base)
  }

  fs.writeFileSync(path.join(EXPORTS, 'manifest.json'), JSON.stringify(manifest, null, 2), 'utf8')
  console.log('DONE ' + FILES.length + ' docs')
}

main()
