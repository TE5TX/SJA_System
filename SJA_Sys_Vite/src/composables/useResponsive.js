import { computed, onBeforeUnmount, onMounted, ref } from 'vue'

const MOBILE_BREAKPOINT = 768
const DESKTOP_BREAKPOINT = 1200

/**
 * 统一管理需要在 JavaScript 中切换结构的响应式状态。
 * 普通尺寸变化仍交给 CSS；只有表格/卡片、弹窗/全屏等结构变化使用这里的状态。
 */
export function useResponsive() {
  const width = ref(typeof window === 'undefined' ? DESKTOP_BREAKPOINT : window.innerWidth)

  const updateWidth = () => {
    width.value = window.innerWidth
  }

  onMounted(() => {
    updateWidth()
    window.addEventListener('resize', updateWidth, { passive: true })
  })

  onBeforeUnmount(() => window.removeEventListener('resize', updateWidth))

  return {
    width,
    isMobile: computed(() => width.value < MOBILE_BREAKPOINT),
    isTablet: computed(() => width.value >= MOBILE_BREAKPOINT && width.value < DESKTOP_BREAKPOINT),
    isCompact: computed(() => width.value < DESKTOP_BREAKPOINT)
  }
}

