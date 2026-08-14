<template>
  <!-- 202608140858 - 头像改为通过带访问令牌的请求加载，不再依赖匿名图片地址。 -->
  <el-avatar :src="imageUrl" :size="size" v-bind="$attrs">
    <slot />
  </el-avatar>
</template>

<script setup>
import { onBeforeUnmount, ref, watch } from 'vue'
import request from '@/utils/request'

defineOptions({ inheritAttrs: false })

const props = defineProps({
  src: { type: String, default: '' },
  size: { type: [Number, String], default: 40 }
})

const imageUrl = ref('')
let ownedObjectUrl = ''
let loadSequence = 0

const revokeOwnedUrl = () => {
  if (ownedObjectUrl) URL.revokeObjectURL(ownedObjectUrl)
  ownedObjectUrl = ''
}

const load = async value => {
  const currentSequence = ++loadSequence
  revokeOwnedUrl()
  imageUrl.value = ''
  if (!value) return

  if (value.startsWith('blob:') || value.startsWith('data:')) {
    imageUrl.value = value
    return
  }

  const match = value.match(/^\/api(\/v1\/upload\/avatar\/[0-9a-fA-F-]{36}\.(?:jpg|png))$/)
  if (!match) return

  try {
    const response = await request({
      url: match[1],
      method: 'get',
      responseType: 'blob',
      skipGlobalError: true
    })
    if (currentSequence !== loadSequence) return
    ownedObjectUrl = URL.createObjectURL(response.data)
    imageUrl.value = ownedObjectUrl
  } catch {
    // Keep the avatar fallback when the protected image cannot be loaded.
  }
}

watch(() => props.src, load, { immediate: true })

onBeforeUnmount(() => {
  loadSequence += 1
  revokeOwnedUrl()
})
</script>
