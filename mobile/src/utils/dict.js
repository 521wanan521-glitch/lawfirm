// 枚举字典与展示标签（与 web 端一致）

export const ROLE_MAP = {
  ADMIN: '系统管理员',
  PARTNER: '合伙人',
  LAWYER: '执业律师',
  PARALEGAL: '律师助理',
  STAFF: '行政人员'
}

export const CASE_STATUS_MAP = {
  NEW: '待立案',
  ACTIVE: '办理中',
  PAUSED: '已暂停',
  CLOSED: '已结案',
  ARCHIVED: '已归档'
}

export const CASE_TYPE_MAP = {
  CIVIL: '民事',
  CRIMINAL: '刑事',
  ADMIN: '行政',
  COMMERCIAL: '商事',
  LABOR: '劳动争议',
  IP: '知识产权',
  FAMILY: '婚姻家事',
  OTHER: '其他'
}

export const PRIORITY_MAP = {
  LOW: '低',
  MEDIUM: '中',
  HIGH: '高',
  URGENT: '紧急'
}

export const CLIENT_TYPE_MAP = {
  PERSONAL: '个人',
  COMPANY: '企业'
}

export const CLIENT_LEVEL_MAP = {
  A: 'A 重要',
  B: 'B 普通',
  C: 'C 潜在'
}

export const INTERACTION_TYPE_MAP = {
  PHONE: '电话',
  VISIT: '拜访',
  EMAIL: '邮件',
  WECHAT: '微信',
  MEETING: '会议',
  OTHER: '其他'
}

export const EVENT_TYPE_MAP = {
  COURT: '开庭',
  MEETING: '会议',
  TASK: '任务',
  REMINDER: '提醒'
}

export const APPROVAL_TYPE_MAP = {
  SEAL: '用章申请',
  LEAVE: '请假申请',
  EXPENSE: '报销申请',
  CASE_FILING: '立案审批',
  OTHER: '其他'
}

export const APPROVAL_STATUS_MAP = {
  PENDING: '待审批',
  APPROVED: '已通过',
  REJECTED: '已驳回',
  CANCELLED: '已撤销'
}

export const roleLabel = (role) => ROLE_MAP[role] || role || '-'
export const caseStatusLabel = (s) => CASE_STATUS_MAP[s] || s || '-'
export const caseTypeLabel = (t) => CASE_TYPE_MAP[t] || t || '-'
export const priorityLabel = (p) => PRIORITY_MAP[p] || p || '-'
export const clientTypeLabel = (t) => CLIENT_TYPE_MAP[t] || t || '-'
export const clientLevelLabel = (l) => CLIENT_LEVEL_MAP[l] || l || '-'
export const eventTypeLabel = (t) => EVENT_TYPE_MAP[t] || t || '-'
export const approvalTypeLabel = (t) => APPROVAL_TYPE_MAP[t] || t || '-'
export const approvalStatusLabel = (s) => APPROVAL_STATUS_MAP[s] || s || '-'
