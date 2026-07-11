import { createRouter, createWebHistory } from 'vue-router'
import { useUserStore } from '@/store/user'

const routes = [
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/Login.vue'),
    meta: { requiresAuth: false }
  },
  {
    path: '/',
    redirect: '/login',
    meta: { requiresAuth: false }
  },
  {
    path: '/app',
    component: () => import('@/views/Layout.vue'),
    redirect: '/app/profile',
    meta: { requiresAuth: true },
    children: [
      {
        path: 'profile',
        name: 'Profile',
        component: () => import('@/views/Profile.vue'),
        meta: { title: '个人管理' }
      },
      {
        path: 'fee-submit',
        name: 'FeeSubmit',
        component: () => import('@/views/FeeSubmit.vue'),
        meta: { title: '稿费提交' }
      },
      {
        path: 'fee-audit',
        name: 'FeeAudit',
        component: () => import('@/views/FeeAudit.vue'),
        meta: { title: '稿费审核与导出' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '人员信息' }
      },
      {
        path: 'logs',
        name: 'OperationLogs',
        component: () => import('@/views/OperationLogs.vue'),
        meta: { title: '操作日志' }
      },
      {
        path: 'sujia',
        name: 'SujiaTool',
        component: () => import('@/views/SujiaTool.vue'),
        meta: { title: '素拓名单' }
      }
    ]
  }
]

const router = createRouter({
  history: createWebHistory(),
  routes
})

router.beforeEach((to, from, next) => {
  const userStore = useUserStore()
  if (to.meta.requiresAuth && !userStore.token) {
    next('/login')
  } else {
    next()
  }
})

export default router
