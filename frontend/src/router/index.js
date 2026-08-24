import { createRouter, createWebHistory } from 'vue-router'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/',
    component: () => import('@/layout/Layout.vue'),
    redirect: '/dashboard',
    children: [
      {
        path: 'dashboard',
        name: 'Dashboard',
        component: () => import('@/views/Dashboard.vue'),
        meta: { title: '工作台', icon: 'Odometer' }
      },
      {
        path: 'assistant',
        name: 'Assistant',
        component: () => import('@/views/assistant/Assistant.vue'),
        meta: { title: 'AI 助手', icon: 'ChatDotRound' }
      },
      {
        path: 'cases',
        name: 'CaseList',
        component: () => import('@/views/case/CaseList.vue'),
        meta: { title: '案件管理', icon: 'Files' }
      },
      {
        path: 'cases/:id',
        name: 'CaseDetail',
        component: () => import('@/views/case/CaseDetail.vue'),
        meta: { title: '案件详情', hidden: true }
      },
      {
        path: 'clients',
        name: 'ClientList',
        component: () => import('@/views/client/ClientList.vue'),
        meta: { title: '客户管理', icon: 'User' }
      },
      {
        path: 'consultants',
        name: 'ConsultantList',
        component: () => import('@/views/client/ConsultantList.vue'),
        meta: { title: '法律顾问单位', icon: 'OfficeBuilding' }
      },
      {
        path: 'clients/:id',
        name: 'ClientDetail',
        component: () => import('@/views/client/ClientDetail.vue'),
        meta: { title: '客户详情', hidden: true }
      },
      {
        path: 'billing/time',
        name: 'TimeEntryList',
        component: () => import('@/views/billing/TimeEntryList.vue'),
        meta: { title: '工时记录', icon: 'Clock' }
      },
      {
        path: 'billing/invoices',
        name: 'InvoiceList',
        component: () => import('@/views/billing/InvoiceList.vue'),
        meta: { title: '账单管理', icon: 'Money' }
      },
      {
        path: 'documents',
        name: 'DocumentList',
        component: () => import('@/views/document/DocumentList.vue'),
        meta: { title: '文档中心', icon: 'FolderOpened' }
      },
      {
        path: 'calendar',
        name: 'CalendarView',
        component: () => import('@/views/calendar/CalendarView.vue'),
        meta: { title: '日程安排', icon: 'Calendar' }
      },
      {
        path: 'approvals',
        name: 'ApprovalList',
        component: () => import('@/views/approval/ApprovalList.vue'),
        meta: { title: '审批流程', icon: 'Stamp' }
      },
      {
        path: 'knowledge',
        name: 'KnowledgeList',
        component: () => import('@/views/knowledge/KnowledgeList.vue'),
        meta: { title: '知识库', icon: 'Reading' }
      },
      {
        path: 'stats',
        name: 'StatsView',
        component: () => import('@/views/stats/StatsView.vue'),
        meta: { title: '统计报表', icon: 'DataAnalysis', managerOnly: true }
      },
      {
        path: 'admin/users',
        name: 'UserList',
        component: () => import('@/views/admin/UserList.vue'),
        meta: { title: '成员管理', icon: 'Setting', adminOnly: true }
      },
      {
        path: 'help',
        name: 'Help',
        component: () => import('@/views/Help.vue'),
        meta: { title: '帮助中心', icon: 'QuestionFilled' }
      }
    ]
  },
  { path: '/:pathMatch(.*)*', redirect: '/dashboard' }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const token = localStorage.getItem('token')
  if (to.path !== '/login' && !token) {
    next('/login')
    return
  }
  if (to.path === '/login' && token) {
    next('/dashboard')
    return
  }
  document.title = (to.meta.title ? to.meta.title + ' - ' : '') + '律所数字化办公系统'
  next()
})

export default router
