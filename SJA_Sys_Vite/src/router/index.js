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
    path: '/recruit',
    name: 'RecruitmentRegister',
    component: () => import('@/views/RecruitmentRegister.vue'),
    meta: { requiresAuth: false, title: '招新注册' }
  },
  {
    path: '/',
    redirect: '/login',
    meta: { requiresAuth: false }
  },
  {
    path: '/change-password',
    name: 'ForcePasswordChange',
    component: () => import('@/views/ForcePasswordChange.vue'),
    meta: { requiresAuth: true }
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
        path: 'fee-standards',
        name: 'FeeStandards',
        component: () => import('@/views/FeeStandards.vue'),
        meta: { title: '稿费标准' }
      },
      {
        path: 'monthly-fee-export',
        name: 'MonthlyFeeExport',
        component: () => import('@/views/MonthlyFeeExport.vue'),
        meta: { title: '月份稿费导出' }
      },
      {
        path: 'users',
        name: 'UserManagement',
        component: () => import('@/views/UserManagement.vue'),
        meta: { title: '人员信息' }
      },
      // 202609181028 - 完成了人员批量导入页面的独立权限路由。
      {
        path: 'user-batch-import',
        name: 'UserBatchImport',
        component: () => import('@/views/UserBatchImport.vue'),
        meta: { title: '人员批量导入', allowedRoleCodes: [0, 1, 3] }
      },
      // 202609301034 - 完成了管理员和秘书长文件保存路径页面的访问控制。
      {
        path: 'storage-settings',
        name: 'StorageSettings',
        component: () => import('@/views/StorageSettings.vue'),
        meta: { title: '文件保存路径', allowedHighestRoleCodes: [0, 3] }
      },
      {
        path: 'logs',
        name: 'OperationLogs',
        component: () => import('@/views/OperationLogs.vue'),
        meta: { title: '操作日志', allowedHighestRoleCodes: [0, 3] }
      },
      {
        path: 'recruitment',
        name: 'RecruitmentManagement',
        component: () => import('@/views/RecruitmentManagement.vue'),
        meta: { title: '招新管理', allowedRoleCodes: [0, 1, 3] }
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

router.beforeEach(async (to, from, next) => {
  const userStore = useUserStore()
  const allowedRoleCodes = to.meta.allowedRoleCodes
  // 202609291000 - 阻止非管理员、非秘书长通过直接输入地址进入操作日志页。
  const allowedHighestRoleCodes = to.meta.allowedHighestRoleCodes
  if (to.meta.requiresAuth && !userStore.sessionInitialized) {
    await userStore.initializeSession()
  }
  // 202609301034 - 修复了刷新页面时在会话恢复前读取角色，导致管理员无法进入文件保存路径页的问题。
  const highestRoleCode = Math.min(...(userStore.userInfo?.roleCodes || []))
  if (to.meta.requiresAuth && !userStore.authenticated) {
    next('/login')
  } else if (userStore.authenticated && userStore.mustChangePassword && to.path !== '/change-password') {
    next('/change-password')
  } else if (userStore.authenticated && !userStore.mustChangePassword && to.path === '/change-password') {
    next('/app')
  } else if (Array.isArray(allowedRoleCodes)
    && !allowedRoleCodes.some(code => userStore.userInfo?.roleCodes?.includes(code))) {
    // 202608312329 - 防止无招新管理权限的已登录用户直接输入管理页地址。
    next('/app/profile')
  } else if (Array.isArray(allowedHighestRoleCodes)
    && !allowedHighestRoleCodes.includes(highestRoleCode)) {
    next('/app/profile')
  } else {
    next()
  }
})

export default router
