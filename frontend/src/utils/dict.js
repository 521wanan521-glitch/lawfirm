// 枚举字典与展示标签

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

export const CASE_STATUS_TYPE = {
  NEW: 'info',
  ACTIVE: 'success',
  PAUSED: 'warning',
  CLOSED: 'primary',
  ARCHIVED: 'info'
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

export const PRIORITY_TYPE = {
  LOW: 'info',
  MEDIUM: 'primary',
  HIGH: 'warning',
  URGENT: 'danger'
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

export const TIME_STATUS_MAP = {
  SUBMITTED: '待审核',
  APPROVED: '已审核',
  BILLED: '已开票'
}

export const TIME_STATUS_TYPE = {
  SUBMITTED: 'warning',
  APPROVED: 'success',
  BILLED: 'primary'
}

export const INVOICE_STATUS_MAP = {
  DRAFT: '草稿',
  ISSUED: '已开票',
  PAID: '已收款',
  VOID: '已作废'
}

export const INVOICE_STATUS_TYPE = {
  DRAFT: 'info',
  ISSUED: 'primary',
  PAID: 'success',
  VOID: 'danger'
}

export const EVENT_TYPE_MAP = {
  COURT: '开庭',
  MEETING: '会议',
  TASK: '任务',
  REMINDER: '提醒'
}

export const EVENT_TYPE_TYPE = {
  COURT: 'danger',
  MEETING: 'primary',
  TASK: 'success',
  REMINDER: 'warning'
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

export const APPROVAL_STATUS_TYPE = {
  PENDING: 'warning',
  APPROVED: 'success',
  REJECTED: 'danger',
  CANCELLED: 'info'
}

export const DOC_CATEGORY_MAP = {
  CONTRACT: '合同',
  EVIDENCE: '证据材料',
  JUDGMENT: '裁判文书',
  LEGAL_DOC: '法律文书',
  OTHER: '其他'
}

export const KNOWLEDGE_CATEGORY_MAP = {
  EXPERIENCE: '办案经验',
  LAW: '法律法规',
  TEMPLATE: '文书模板',
  TRAINING: '培训资料',
  OTHER: '其他'
}

export const roleLabel = (role) => ROLE_MAP[role] || role || '-'
export const caseStatusLabel = (s) => CASE_STATUS_MAP[s] || s || '-'
export const caseTypeLabel = (t) => CASE_TYPE_MAP[t] || t || '-'
export const priorityLabel = (p) => PRIORITY_MAP[p] || p || '-'
export const timeStatusLabel = (s) => TIME_STATUS_MAP[s] || s || '-'
export const invoiceStatusLabel = (s) => INVOICE_STATUS_MAP[s] || s || '-'
export const eventTypeLabel = (t) => EVENT_TYPE_MAP[t] || t || '-'
export const approvalTypeLabel = (t) => APPROVAL_TYPE_MAP[t] || t || '-'
export const approvalStatusLabel = (s) => APPROVAL_STATUS_MAP[s] || s || '-'
export const docCategoryLabel = (c) => DOC_CATEGORY_MAP[c] || c || '-'
export const knowledgeCategoryLabel = (c) => KNOWLEDGE_CATEGORY_MAP[c] || c || '-'
