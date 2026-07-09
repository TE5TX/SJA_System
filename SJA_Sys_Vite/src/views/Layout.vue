<template>
  <el-container class="layout-container">
    <!-- 头部 -->
    <el-header class="header" height="60px">
      <div class="header-left">
        <div class="logo">
          <span class="logo-text">校记者团管理系统</span>
        </div>
        <i class="toggle-sidebar-btn" @click="toggleSidebar">
          <el-icon :size="28"><Menu /></el-icon>
        </i>
      </div>

      <div class="header-right">
        <el-dropdown @command="handleCommand" @visible-change="handleDropdownVisible">
          <span class="nav-profile">
            <el-avatar :size="36">
              {{ userStore.userInfo?.realName?.charAt(0) || 'U' }}
            </el-avatar>
            <span class="username">{{ userStore.userInfo?.realName || '用户' }}</span>
            <el-icon :class="{ 'rotate-icon': dropdownVisible }">
              <ArrowDown />
            </el-icon>
          </span>
          <template #dropdown>
            <el-dropdown-menu class="profile-dropdown">
              <li class="dropdown-header">
                <h6>{{ deptDisplay }}</h6>
                <span>{{ roleDisplay }}</span>
              </li>
              <li><hr class="dropdown-divider"></li>
              <el-dropdown-item command="profile">
                <el-icon><Setting /></el-icon>
                <span>个人管理</span>
              </el-dropdown-item>
              <li><hr class="dropdown-divider"></li>
              <el-dropdown-item command="logout">
                <el-icon><SwitchButton /></el-icon>
                <span>退出登录</span>
              </el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </div>
    </el-header>

    <el-container>
      <!-- 侧边栏 -->
      <el-aside :width="sidebarWidth" class="sidebar" :class="{ 'sidebar-collapsed': isCollapse }">
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="isCollapse"
          background-color="#fff"
          text-color="#012970"
          active-text-color="#4154f1"
        >
          <el-menu-item index="/app/profile">
            <el-icon><User /></el-icon>
            <template #title>个人管理</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <!-- 主内容区 -->
      <el-main class="main-content" :class="{ 'content-expanded': isCollapse }">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { logout } from '@/api/auth'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const isCollapse = ref(false)
const dropdownVisible = ref(false)
const sidebarWidth = computed(() => isCollapse.value ? '64px' : '260px')
const activeMenu = computed(() => route.path)

// 部门显示（后端返回 deptNames 数组）
const deptDisplay = computed(() => {
  const names = userStore.userInfo?.deptNames
  return (names && names.length > 0) ? names.join('、') : '未分配部门'
})

// 角色显示（后端返回 roleNames 数组）
const roleDisplay = computed(() => {
  const names = userStore.userInfo?.roleNames
  return (names && names.length > 0) ? names.join('、') : '-'
})

const toggleSidebar = () => {
  isCollapse.value = !isCollapse.value
}

const handleDropdownVisible = (visible) => {
  dropdownVisible.value = visible
}

const handleCommand = (command) => {
  if (command === 'logout') {
    ElMessageBox.confirm('确定要退出登录吗？', '提示', {
      confirmButtonText: '确定',
      cancelButtonText: '取消',
      type: 'warning'
    }).then(async () => {
      try {
        await logout()
      } catch (e) {
        // 即使后端调用失败也清除本地状态
      }
      userStore.logout()
      router.push('/login')
    }).catch(() => {})
  } else if (command === 'profile') {
    router.push('/app/profile')
  }
}
</script>

<style scoped>
.layout-container {
  height: 100vh;
}

/* 头部 */
.header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  background-color: #fff;
  box-shadow: var(--header-shadow);
  padding: 0 20px;
  z-index: 997;
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  height: 60px;
}

.header-left {
  display: flex;
  align-items: center;
  gap: 15px;
}

.logo {
  display: flex;
  align-items: center;
}

.logo-text {
  font-size: 22px;
  font-weight: 700;
  color: var(--secondary-color);
  font-family: 'Nunito', sans-serif;
}

.toggle-sidebar-btn {
  cursor: pointer;
  color: var(--secondary-color);
  font-size: 32px;
  padding: 5px;
  transition: 0.3s;
  display: flex;
  align-items: center;
}

.toggle-sidebar-btn:hover {
  color: var(--primary-color);
}

.header-right {
  display: flex;
  align-items: center;
}

.header-right :deep(.el-dropdown) {
  outline: none !important;
}

.nav-profile {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  color: var(--secondary-color);
  padding: 5px 10px;
  border-radius: 4px;
  transition: 0.3s;
  outline: none !important;
}

.nav-profile:focus,
.nav-profile:focus-visible {
  outline: none !important;
  box-shadow: none !important;
}

.nav-profile .username {
  font-size: 14px;
  font-weight: 600;
}

.nav-profile :deep(.el-icon) {
  transition: transform 0.4s cubic-bezier(0.4, 0, 0.2, 1);
}

.nav-profile .rotate-icon {
  transform: rotate(180deg);
}

/* 下拉菜单 */
.profile-dropdown {
  min-width: 150px;
  padding: 10px 0;
  border: 0;
  box-shadow: 0 5px 30px 0 rgba(82, 63, 105, 0.2);
  border-radius: 4px;
  list-style: none;
}

.profile-dropdown :deep(.el-dropdown-menu__item) {
  justify-content: center;
  text-align: center;
  outline: none !important;
  padding: 12px 20px;
  margin: 0 8px;
  border-radius: 4px;
}

.profile-dropdown :deep(.el-dropdown-menu__item:hover) {
  outline: none !important;
  box-shadow: none !important;
  background-color: var(--background-color) !important;
  color: var(--primary-color) !important;
}

.profile-dropdown :deep(.el-dropdown-menu__item) > span {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.dropdown-header {
  padding: 10px 20px;
  text-align: center;
  list-style: none;
}

.dropdown-header h6 {
  font-size: 16px;
  font-weight: 600;
  color: #444444;
  margin: 0 0 5px 0;
}

.dropdown-header span {
  font-size: 14px;
  color: #899bbd;
}

.dropdown-divider {
  margin: 10px 0;
  border: 0;
  border-top: 1px solid #ebeef4;
}

/* 侧边栏 */
.sidebar {
  position: fixed;
  top: 60px;
  left: 0;
  bottom: 0;
  width: 260px;
  z-index: 996;
  transition: all 0.3s;
  overflow-y: auto;
  background-color: #fff;
  box-shadow: 0px 0px 20px rgba(1, 41, 112, 0.1);
}

.sidebar.sidebar-collapsed {
  width: 64px !important;
}

.sidebar :deep(.el-menu) {
  border-right: none;
  padding: 15px;
}

.sidebar.sidebar-collapsed :deep(.el-menu) {
  padding: 15px 0;
}

.sidebar :deep(.el-menu-item) {
  margin-bottom: 5px;
  border-radius: 4px;
  height: 45px;
  line-height: 45px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background-color: var(--background-color);
  color: var(--primary-color);
}

.sidebar :deep(.el-menu-item:hover) {
  background-color: var(--background-color);
  color: var(--primary-color);
}

/* 主内容区 */
.main-content {
  margin-top: 60px;
  margin-left: 260px;
  padding: 20px 30px;
  background-color: var(--background-color);
  min-height: calc(100vh - 60px);
  transition: all 0.3s;
}

.main-content.content-expanded {
  margin-left: 64px !important;
}

/* 响应式 */
@media (max-width: 1199px) {
  .sidebar {
    left: -260px;
  }

  .main-content {
    margin-left: 0;
    padding: 20px;
  }

  .logo-text {
    display: none;
  }
}
</style>
