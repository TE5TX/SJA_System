<template>
  <el-container class="layout-container">
    <!-- 头部 -->
    <el-header class="header" height="60px">
      <div class="header-left">
        <button
          class="toggle-sidebar-btn"
          :class="{ 'is-open': isCompact && isMobileMenuOpen }"
          type="button"
          :aria-label="isCompact
            ? (isMobileMenuOpen ? '关闭导航菜单' : '打开导航菜单')
            : (isDesktopCollapsed ? '展开导航菜单' : '折叠导航菜单')"
          :aria-expanded="isCompact ? isMobileMenuOpen : !isDesktopCollapsed"
          @click="toggleSidebar"
        >
          <!-- 移动端用图标切换明确表达菜单当前的开关状态。 -->
          <el-icon :size="24">
            <Close v-if="isCompact && isMobileMenuOpen" />
            <Menu v-else />
          </el-icon>
        </button>
        <div class="logo">
          <span class="logo-text">校记者团管理系统</span>
        </div>
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
      <!-- 小屏侧栏作为抽屉覆盖内容，桌面端仍保留原来的折叠行为。 -->
      <el-aside
        :width="sidebarWidth"
        class="sidebar"
        :class="{
          'sidebar-collapsed': !isCompact && isDesktopCollapsed,
          'sidebar-mobile-open': isCompact && isMobileMenuOpen
        }"
      >
        <el-menu
          :default-active="activeMenu"
          router
          :collapse="!isCompact && isDesktopCollapsed"
          @select="handleMenuSelect"
          background-color="#fff"
          text-color="#012970"
          active-text-color="#4154f1"
        >
          <el-menu-item index="/app/profile">
            <el-icon><User /></el-icon>
            <template #title>个人管理</template>
          </el-menu-item>

          <el-sub-menu v-if="canUseFee" index="fee">
            <template #title>
              <el-icon><Document /></el-icon>
              <span>稿费管理</span>
            </template>
            <el-menu-item v-if="canSubmitFee" index="/app/fee-submit">
              <el-icon><EditPen /></el-icon>
              <template #title>稿费提交</template>
            </el-menu-item>
            <el-menu-item v-if="canViewFeeTasks" index="/app/fee-audit">
              <el-icon><CircleCheck /></el-icon>
              <template #title>{{ canManageFeeAmount ? '审核与导出' : '稿费任务' }}</template>
            </el-menu-item>
            <el-menu-item v-if="canManageFeeAmount" index="/app/fee-standards">
              <el-icon><Setting /></el-icon>
              <template #title>稿费标准</template>
            </el-menu-item>
            <el-menu-item v-if="canManageFeeAmount" index="/app/monthly-fee-export">
              <el-icon><Download /></el-icon>
              <template #title>月份稿费导出</template>
            </el-menu-item>
          </el-sub-menu>

          <el-sub-menu v-if="canManageUsers" index="system">
            <template #title>
              <el-icon><Setting /></el-icon>
              <span>系统管理</span>
            </template>
            <el-menu-item index="/app/users">
              <el-icon><UserFilled /></el-icon>
              <template #title>人员信息</template>
            </el-menu-item>
            <!-- 202609181028 - 在系统管理中增加受权限限制的人员批量导入入口。 -->
            <el-menu-item v-if="canManageRecruitment" index="/app/user-batch-import">
              <el-icon><UploadFilled /></el-icon>
              <template #title>人员批量导入</template>
            </el-menu-item>
            <el-menu-item index="/app/logs">
              <el-icon><Tickets /></el-icon>
              <template #title>操作日志</template>
            </el-menu-item>
            <el-menu-item v-if="canManageRecruitment" index="/app/recruitment">
              <el-icon><Promotion /></el-icon>
              <template #title>注册管理</template>
            </el-menu-item>
          </el-sub-menu>

          <el-menu-item v-if="canUseSujia" index="/app/sujia">
            <el-icon><Files /></el-icon>
            <template #title>素拓名单</template>
          </el-menu-item>
        </el-menu>
      </el-aside>

      <Transition name="sidebar-mask">
        <button
          v-if="isCompact && isMobileMenuOpen"
          class="sidebar-overlay"
          type="button"
          aria-label="关闭导航菜单"
          @click="closeMobileMenu"
        />
      </Transition>

      <!-- 主内容区 -->
      <el-main class="main-content" :class="{ 'content-expanded': !isCompact && isDesktopCollapsed }">
        <router-view />
      </el-main>
    </el-container>
  </el-container>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount, watch } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { ElMessageBox } from 'element-plus'
import { useUserStore } from '@/store/user'
import { logout } from '@/api/auth'
import { useResponsive } from '@/composables/useResponsive'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const { isCompact } = useResponsive()
const isDesktopCollapsed = ref(false)
const isMobileMenuOpen = ref(false)
const dropdownVisible = ref(false)
const sidebarWidth = computed(() => (!isCompact.value && isDesktopCollapsed.value) ? '64px' : '260px')
const activeMenu = computed(() => route.path)
const roleCodes = computed(() => userStore.userInfo?.roleCodes || [])
const highestRoleCode = computed(() => roleCodes.value.length ? Math.min(...roleCodes.value) : 13)
const isInfoManager = computed(() => [0, 1, 2, 3].includes(highestRoleCode.value))
const isMinister = computed(() => roleCodes.value.includes(4))
const canManageUsers = computed(() => isInfoManager.value)
const canManageRecruitment = computed(() => [0, 1, 3].includes(highestRoleCode.value))
const canManageFeeAmount = computed(() => [0, 3].includes(highestRoleCode.value))
const canSubmitFee = computed(() => canManageFeeAmount.value || isMinister.value)
const canViewFeeTasks = computed(() => canManageFeeAmount.value || isMinister.value)
const canUseFee = computed(() => canSubmitFee.value || canViewFeeTasks.value)
const canUseSujia = computed(() => isInfoManager.value || isMinister.value)

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
  if (isCompact.value) {
    isMobileMenuOpen.value = !isMobileMenuOpen.value
    return
  }
  isDesktopCollapsed.value = !isDesktopCollapsed.value
}

const closeMobileMenu = () => {
  isMobileMenuOpen.value = false
}

const handleMenuSelect = () => {
  if (isCompact.value) closeMobileMenu()
}

const handleEscape = (event) => {
  if (event.key === 'Escape') closeMobileMenu()
}

// 抽屉展开时锁定页面滚动，避免用户滑动到遮罩后的业务内容。
watch(isMobileMenuOpen, (open) => {
  document.body.style.overflow = open && isCompact.value ? 'hidden' : ''
})

watch(() => route.fullPath, closeMobileMenu)
watch(isCompact, (compact) => {
  if (!compact) closeMobileMenu()
})

onMounted(() => window.addEventListener('keydown', handleEscape))
onBeforeUnmount(() => {
  window.removeEventListener('keydown', handleEscape)
  document.body.style.overflow = ''
})

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
  min-height: 100vh;
  min-height: 100dvh;
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
  border: 1px solid transparent;
  border-radius: 6px;
  background: transparent;
  cursor: pointer;
  color: var(--secondary-color);
  width: 44px;
  height: 44px;
  flex: 0 0 44px;
  padding: 0;
  justify-content: center;
  transition:
    color 0.2s,
    background-color 0.2s,
    border-color 0.2s,
    transform 0.1s;
  display: flex;
  align-items: center;
}

.toggle-sidebar-btn:hover {
  color: var(--primary-color);
  background: rgba(65, 84, 241, 0.08);
}

.toggle-sidebar-btn:focus-visible {
  outline: 3px solid rgba(64, 158, 255, 0.35);
  outline-offset: 2px;
}

.toggle-sidebar-btn:active {
  transform: scale(0.96);
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

/* 202608111533 - 修复了侧边栏普通菜单与折叠菜单鼠标悬停样式不一致的问题。 */
.sidebar :deep(.el-sub-menu__title) {
  margin-bottom: 5px;
  border-radius: 4px;
  height: 45px;
  line-height: 45px;
}

.sidebar :deep(.el-menu-item.is-active) {
  background-color: var(--background-color);
  color: var(--primary-color);
}

.sidebar :deep(.el-menu-item:hover),
.sidebar :deep(.el-sub-menu__title:hover) {
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
  min-width: 0;
  overflow-x: hidden;
  transition: all 0.3s;
}

.main-content.content-expanded {
  margin-left: 64px !important;
}

/* 响应式 */
@media (max-width: 1199px) {
  .sidebar {
    left: 0;
    transform: translateX(-105%);
    width: min(82vw, 300px) !important;
    z-index: 1001;
    box-shadow: 8px 0 28px rgba(1, 41, 112, 0.18);
  }

  .sidebar.sidebar-mobile-open {
    transform: translateX(0);
  }

  .sidebar :deep(.el-menu) {
    padding: 15px;
  }

  .main-content {
    margin-left: 0;
    padding: 20px;
  }

  .logo-text {
    font-size: 18px;
  }

  .sidebar-overlay {
    position: fixed;
    inset: 60px 0 0;
    z-index: 1000;
    border: 0;
    background: rgba(16, 24, 40, 0.42);
  }

  .sidebar-mask-enter-active,
  .sidebar-mask-leave-active {
    transition: opacity 0.2s ease;
  }

  .sidebar-mask-enter-from,
  .sidebar-mask-leave-to {
    opacity: 0;
  }
}

@media (max-width: 767px) {
  .header {
    padding: 0 10px;
  }

  .header-left {
    min-width: 0;
    gap: 8px;
  }

  /* 202608312256 - 强化移动端导航入口，保证触控尺寸和视觉对比度。 */
  .toggle-sidebar-btn {
    color: #fff;
    background: var(--primary-color);
    border-color: var(--primary-color);
    box-shadow: 0 2px 6px rgba(0, 41, 112, 0.2);
  }

  .toggle-sidebar-btn:hover {
    color: #fff;
    background: var(--primary-color);
  }

  .toggle-sidebar-btn.is-open {
    color: var(--primary-color);
    background: #eef4ff;
    border-color: #b8d3ff;
    box-shadow: none;
  }

  .logo-text {
    display: block;
    max-width: 55vw;
    overflow: hidden;
    text-overflow: ellipsis;
    white-space: nowrap;
    font-size: 17px;
  }

  .nav-profile {
    gap: 4px;
    padding: 4px;
  }

  .nav-profile .username {
    display: none;
  }

  .main-content {
    padding: 14px 12px calc(18px + env(safe-area-inset-bottom));
    min-height: calc(100dvh - 60px);
  }
}
</style>
