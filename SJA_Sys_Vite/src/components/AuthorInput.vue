<template>
  <div class="author-input">
    <el-input
      v-model="inputText"
      type="textarea"
      :rows="2"
      :placeholder="`请输入${personLabel}姓名，用空格、逗号或换行分隔\n例如：张三 李四 王五`"
      clearable
      @input="parseNames"
    />

    <div v-if="parsedPeople.length" class="people-tags">
      <span class="tag-label">已识别 {{ parsedPeople.length }} 人</span>
      <el-tag
        v-for="(person, index) in parsedPeople"
        :key="`${person.name}-${index}`"
        :type="person.valid ? 'success' : 'danger'"
        closable
        size="small"
        @close="removePerson(index)"
      >
        {{ person.name }}
        <el-icon v-if="!person.valid"><Warning /></el-icon>
      </el-tag>
    </div>

    <div v-if="invalidPeople.length" class="error-tip">
      <el-icon><InfoFilled /></el-icon>
      <span>{{ invalidMessage }}</span>
    </div>
  </div>
</template>

<script setup>
import { computed, ref, watch } from 'vue'
import { InfoFilled, Warning } from '@element-plus/icons-vue'

const props = defineProps({
  modelValue: { type: String, default: '' },
  users: { type: Array, default: () => [] },
  personLabel: { type: String, default: '作者' }
})
const emit = defineEmits(['update:modelValue', 'validation-change'])

const inputText = ref(props.modelValue)
const parsedPeople = ref([])
const normalizeName = value => String(value || '').replace(/\u3000/g, ' ').trim()

const activeUsers = computed(() => props.users.filter(user => user.isValid !== 0))
const invalidPeople = computed(() => parsedPeople.value.filter(person => !person.valid))
const invalidMessage = computed(() => invalidPeople.value
  .map(person => person.ambiguous ? `${person.name}（存在重名）` : `${person.name}（系统中不存在）`)
  .join('、'))

const parseNames = () => {
  const names = [...new Set(String(inputText.value || '')
    .replace(/\u3000/g, ' ')
    .split(/[\s,，、;；]+/)
    .map(normalizeName)
    .filter(Boolean))]

  parsedPeople.value = names.map(name => {
    const matches = activeUsers.value.filter(user => normalizeName(user.realName) === name)
    return {
      name,
      valid: matches.length === 1,
      ambiguous: matches.length > 1,
      user: matches.length === 1 ? matches[0] : null
    }
  })

  emit('update:modelValue', inputText.value)
  emit('validation-change', {
    valid: invalidPeople.value.length === 0,
    matchedUsers: parsedPeople.value.filter(person => person.valid).map(person => person.user),
    invalidNames: invalidPeople.value.map(person => person.name)
  })
}

const removePerson = index => {
  parsedPeople.value.splice(index, 1)
  inputText.value = parsedPeople.value.map(person => person.name).join(' ')
  parseNames()
}

watch(() => props.modelValue, value => {
  if (value !== inputText.value) {
    inputText.value = value || ''
    parseNames()
  }
})
watch(() => props.users, parseNames, { deep: true })
</script>

<style scoped>
.author-input {
  width: 100%;
}

.people-tags {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 6px;
  margin-top: 8px;
  padding: 8px;
  border-radius: 4px;
  background: #f5f7fa;
}

.tag-label {
  margin-right: 2px;
  color: #909399;
  font-size: 12px;
}

.error-tip {
  display: flex;
  align-items: center;
  gap: 6px;
  margin-top: 8px;
  padding: 8px 10px;
  border: 1px solid #fbc4c4;
  border-radius: 4px;
  background: #fef0f0;
  color: #f56c6c;
  font-size: 13px;
}
</style>
