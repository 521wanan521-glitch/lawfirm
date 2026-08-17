import dayjs from 'dayjs'

export function formatDate(date) {
  return date ? dayjs(date).format('YYYY-MM-DD') : '-'
}

export function formatDateTime(date) {
  return date ? dayjs(date).format('YYYY-MM-DD HH:mm') : '-'
}

export function formatMoney(value) {
  if (value === null || value === undefined) return '-'
  const num = Number(value)
  return '¥' + num.toLocaleString('zh-CN', { minimumFractionDigits: 2, maximumFractionDigits: 2 })
}

export function formatNumber(value) {
  return value === null || value === undefined ? '-' : Number(value).toLocaleString('zh-CN')
}

export function formatSize(bytes) {
  if (!bytes) return '0 B'
  const units = ['B', 'KB', 'MB', 'GB']
  let i = 0
  let v = bytes
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return v.toFixed(1) + ' ' + units[i]
}

export function formatHours(value) {
  if (value === null || value === undefined) return '-'
  return Number(value).toFixed(2) + ' h'
}

export function today() {
  return dayjs().format('YYYY-MM-DD')
}

export function monthRange() {
  return {
    start: dayjs().startOf('month').format('YYYY-MM-DD'),
    end: dayjs().endOf('month').format('YYYY-MM-DD')
  }
}
