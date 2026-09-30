<template>
  <div class="storage-settings">
    <header class="page-title">
      <h1>文件保存路径</h1>
      <p>管理照片和人员导入 XLSX 原件在服务器上的保存位置。</p>
    </header>

    <el-card class="settings-card" v-loading="loading">
      <template #header>
        <div class="card-header">
          <span>本地文件配置</span>
          <el-button @click="load">刷新</el-button>
        </div>
      </template>

      <el-table :data="settings" stripe empty-text="暂无文件保存配置">
        <el-table-column label="配置名" min-width="150">
          <template #default="{ row }">{{ categoryName(row.category) }}</template>
        </el-table-column>
        <el-table-column label="存储器" width="110">
          <template #default><el-tag type="info">本地磁盘</el-tag></template>
        </el-table-column>
        <el-table-column label="基础路径" min-width="260">
          <template #default="{ row }"><span class="path-text">{{ row.resolvedPath }}</span></template>
        </el-table-column>
        <el-table-column label="子目录" min-width="140" prop="relativeDir" />
        <el-table-column label="操作" width="150" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openEdit(row)">编辑</el-button>
            <el-button link type="primary" :loading="checking === row.category" @click="testCurrent(row)">测试</el-button>
          </template>
        </el-table-column>
      </el-table>

      <!-- <el-alert class="storage-note" type="info" :closable="false" show-icon>
        <template #title>路径说明</template>
        页面显示的是后端进程看到的路径。Docker 部署时，基础路径必须位于已挂载的存储根目录内；
        如需换到另一块服务器磁盘，请先在服务器调整挂载。修改子目录只影响新上传文件，不迁移或删除旧文件。
      </el-alert> -->
    </el-card>

    <el-dialog v-model="dialogVisible" :title="`编辑${categoryName(draft.category)}保存路径`" width="560px" @closed="resetDialog">
      <el-form label-width="100px" label-position="left" @submit.prevent="save">
        <el-form-item label="存储器">本地磁盘</el-form-item>
        <el-form-item label="当前路径">
          <span class="path-text">{{ selected?.resolvedPath }}</span>
        </el-form-item>
        <el-form-item label="基础路径" required>
          <el-input v-model.trim="draft.fullPath" placeholder="请输入服务器上的完整保存路径" />
          <div class="field-tip">允许范围：{{ selected?.rootPath }}。Docker 中这里填写容器内路径，服务器磁盘须预先挂载。</div>
        </el-form-item>
        <el-form-item label="目录检测">
          <el-button :loading="checking === draft.category" :disabled="!draft.fullPath" @click="testDraft">
            测试并创建目录
          </el-button>
          <div v-if="testedPath" class="tested-path">可写：{{ testedPath }}</div>
        </el-form-item>
        <el-form-item label="当前密码" required>
          <el-input v-model="draft.password" type="password" show-password autocomplete="current-password"
            placeholder="保存前请输入当前账号密码" @keyup.enter="save" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" :disabled="!draft.fullPath || !draft.password" @click="save">
          保存配置
        </el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref, watch } from 'vue'
import { ElMessage } from 'element-plus'
import { getStorageSettings, checkStorageSetting, updateStorageSetting } from '@/api/system'

// 202609301200 - 修复了将服务器端原件留存误做为前台归档页的问题，改为参照文件配置列表管理保存路径。
const settings = ref([])
const loading = ref(false)
const checking = ref('')
const saving = ref(false)
const dialogVisible = ref(false)
const selected = ref(null)
const testedPath = ref('')
const draft = reactive({ category: '', fullPath: '', password: '' })

/** 将后端类别映射为管理员可理解的文件用途。 */
const categoryName = category => category === 'AVATAR' ? '照片' : '人员导入 XLSX 原件'

/** 重新读取当前生效的路径配置。 */
const load = async () => {
  loading.value = true
  try {
    const response = await getStorageSettings()
    settings.value = response.data || []
  } finally {
    loading.value = false
  }
}

/** 编辑在副本上进行；取消弹窗时不改动列表中的已生效路径。 */
const openEdit = row => {
  selected.value = row
  draft.category = row.category
  draft.fullPath = row.resolvedPath
  draft.password = ''
  testedPath.value = ''
  dialogVisible.value = true
}

const resetDialog = () => {
  selected.value = null
  draft.category = ''
  draft.fullPath = ''
  draft.password = ''
  testedPath.value = ''
}

/** 检测目录是否可写；服务端负责路径范围校验和创建目录。 */
const checkPath = async (category, relativeDir) => {
  checking.value = category
  try {
    const response = await checkStorageSetting(category, relativeDir)
    ElMessage.success('目录可写')
    return response.data.resolvedPath
  } finally {
    checking.value = ''
  }
}

/** 完整路径只可指向后端允许的根目录下；提交时仍使用服务端校验的相对目录协议。 */
const relativeFromFullPath = () => {
  const root = selected.value?.rootPath?.replace(/[\\/]+$/, '')
  const full = draft.fullPath.trim()
  if (!root || !full) return ''
  const rootParts = root.replace(/\\/g, '/').split('/')
  const fullParts = full.replace(/\\/g, '/').split('/')
  const windowsPath = /^[a-zA-Z]:/.test(root)
  const matches = rootParts.every((part, index) =>
    (windowsPath ? part.toLowerCase() : part) === (windowsPath ? fullParts[index]?.toLowerCase() : fullParts[index]))
  if (!matches || fullParts.length <= rootParts.length) return ''
  return fullParts.slice(rootParts.length).join('/')
}

const editedRelativeDir = () => {
  const relativeDir = relativeFromFullPath()
  if (!relativeDir) ElMessage.warning('保存路径必须位于允许的存储根目录下')
  return relativeDir
}

const testCurrent = async row => { await checkPath(row.category, row.relativeDir) }
const testDraft = async () => {
  const relativeDir = editedRelativeDir()
  if (relativeDir) testedPath.value = await checkPath(draft.category, relativeDir)
}

/** 修改保存路径需要管理员或秘书长的当前密码；历史文件位置由后端保留。 */
const save = async () => {
  if (!draft.category || !draft.fullPath || !draft.password || saving.value) return
  const relativeDir = editedRelativeDir()
  if (!relativeDir) return
  saving.value = true
  try {
    await updateStorageSetting(draft.category, relativeDir, draft.password)
    dialogVisible.value = false
    ElMessage.success('保存成功，新文件将使用新路径')
    await load()
  } finally {
    draft.password = ''
    saving.value = false
  }
}

watch(() => draft.fullPath, () => { testedPath.value = '' })
onMounted(load)
</script>

<style scoped>
.page-title p { color: #667085; }
.settings-card { margin-top: 18px; }
.card-header { display: flex; align-items: center; justify-content: space-between; font-weight: 600; }
.path-text { overflow-wrap: anywhere; font-family: Consolas, 'Courier New', monospace; }
.storage-note { margin-top: 20px; line-height: 1.6; }
.field-tip { color: #667085; font-size: 12px; line-height: 1.5; margin-top: 5px; }
.tested-path { width: 100%; margin-top: 8px; color: #15803d; overflow-wrap: anywhere; }
</style>
