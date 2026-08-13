<template>
  <div class="operation-logs">
    <div class="page-title">
      <h1>操作日志</h1>
      <p>查看资料修改、稿费提交和审核等关键操作记录。</p>
    </div>

    <el-card>
      <template #header>
        <div class="filters">
          <el-input v-model="query.realName" placeholder="操作人" clearable />
          <el-select v-model="query.operationType" placeholder="操作类型" clearable>
            <el-option v-for="item in operationTypes" :key="item" :label="item" :value="item" />
          </el-select>
          <el-input v-model="query.targetModule" placeholder="模块" clearable />
          <el-select v-model="query.resultStatus" placeholder="结果" clearable>
            <el-option label="成功" value="SUCCESS" />
            <el-option label="失败" value="FAILURE" />
          </el-select>
          <el-input v-model="query.requestId" placeholder="请求 ID" clearable />
          <el-button type="primary" @click="search">查询</el-button>
          <el-button :icon="Refresh" @click="reset">重置</el-button>
        </div>
      </template>
      <el-table :data="logs" v-loading="loading" border>
        <el-table-column prop="realName" label="操作人" width="120" />
        <el-table-column prop="operationType" label="类型" width="120" />
        <el-table-column prop="targetModule" label="模块" width="120" />
        <el-table-column label="结果" width="90">
          <template #default="{ row }">
            <el-tag :type="row.resultStatus === 'FAILURE' ? 'danger' : 'success'">
              {{ row.resultStatus === 'FAILURE' ? '失败' : '成功' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column prop="requestMethod" label="方法" width="80" />
        <el-table-column prop="requestUri" label="请求路径" min-width="180" show-overflow-tooltip />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column prop="durationMs" label="耗时(ms)" width="100" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ time(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="90" fixed="right">
          <template #default="{ row }">
            <el-button link type="primary" @click="openDetail(row)">详情</el-button>
          </template>
        </el-table-column>
      </el-table>
      <div class="pagination">
        <el-pagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50]"
          layout="total, sizes, prev, pager, next"
          :total="total"
          @size-change="loadLogs"
          @current-change="loadLogs"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="日志详情" size="520px">
      <el-descriptions v-if="selectedLog" :column="1" border>
        <el-descriptions-item label="请求 ID">{{ selectedLog.requestId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="请求">{{ selectedLog.requestMethod || '-' }} {{ selectedLog.requestUri || '-' }}</el-descriptions-item>
        <el-descriptions-item label="操作人">{{ selectedLog.realName || '-' }}</el-descriptions-item>
        <el-descriptions-item label="对象">{{ selectedLog.targetModule || '-' }} / {{ selectedLog.targetId || '-' }}</el-descriptions-item>
        <el-descriptions-item label="详情">{{ selectedLog.detail || '-' }}</el-descriptions-item>
        <el-descriptions-item label="客户端">{{ selectedLog.userAgent || '-' }}</el-descriptions-item>
      </el-descriptions>

      <template v-if="selectedChangeRows.length">
        <h3 class="change-title">字段变更</h3>
        <el-table :data="selectedChangeRows" border size="small" class="change-table">
          <el-table-column prop="label" label="字段" min-width="130" />
          <el-table-column label="变更前" min-width="180">
            <template #default="{ row }"><span class="snapshot-value removed-value">{{ row.before }}</span></template>
          </el-table-column>
          <el-table-column label="变更后" min-width="180">
            <template #default="{ row }"><span class="snapshot-value added-value">{{ row.after }}</span></template>
          </el-table-column>
        </el-table>
      </template>

      <el-empty v-else description="该日志没有记录字段级变更" :image-size="72" />

      <el-collapse v-if="selectedLog?.beforeData || selectedLog?.afterData" class="raw-snapshot">
        <el-collapse-item title="查看原始变更快照" name="raw">
          <div class="raw-block"><strong>变更前</strong><pre>{{ prettySnapshot(selectedLog.beforeData) }}</pre></div>
          <div class="raw-block"><strong>变更后</strong><pre>{{ prettySnapshot(selectedLog.afterData) }}</pre></div>
        </el-collapse-item>
      </el-collapse>
    </el-drawer>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getOperationLogs } from '@/api/system'

const query = reactive({
  realName: '', operationType: '', targetModule: '', resultStatus: '', requestId: '',
  pageNum: 1, pageSize: 10
})
const operationTypes = ['CREATE', 'UPDATE', 'DELETE', 'AUDIT', 'RESET_AUDIT', 'EXPORT', 'LOGIN', 'LOGOUT']
const logs = ref([])
const total = ref(0)
const loading = ref(false)
const detailVisible = ref(false)
const selectedLog = ref(null)
const time = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
const snapshotLabels = {
  workType: '作品类型', status: '审核状态', totalAmount: '总金额', remark: '备注', contributors: '参与人员',
  title: '标题', videoTitle: '视频标题', platform: '发布平台', category: '类别', readCount: '阅读量',
  wordCount: '字数', photoCount: '图片数量', videoDurationSec: '视频时长（秒）', likeCount: '点赞数',
  reprintStatus: '转载情况', isReposted: '是否官方转载', isComplexEdit: '是否复杂剪辑',
  authorCount: '参与人数', authorAvgFee: '人均金额', publishDate: '发布时间', articleLink: '文章链接',
  videoLink: '视频链接', isPublished: '是否已发布', isSpecialNode: '特殊节点',
  specialMultiplier: '特殊节点倍数', subsidyMonth: '补贴月份', realName: '姓名', studentNo: '学号',
  userId: '用户ID', roleInWork: '参与角色', feeAmount: '个人金额', workCount: '作品数量'
}

const parseSnapshot = value => {
  if (!value) return null
  try { return JSON.parse(value) } catch { return null }
}

const formatSnapshotValue = value => {
  if (value == null || value === '') return '-'
  if (typeof value === 'boolean') return value ? '是' : '否'
  if (Array.isArray(value)) {
    return value.map(item => {
      if (!item || typeof item !== 'object') return String(item)
      const name = item.realName || `用户 ${item.userId ?? '-'}`
      const details = [
        item.studentNo ? `学号 ${item.studentNo}` : '',
        item.roleInWork ? `角色 ${item.roleInWork}` : '',
        item.feeAmount != null ? `金额 ¥${Number(item.feeAmount).toFixed(1)}` : '',
        item.workCount != null ? `作品 ${item.workCount}` : ''
      ].filter(Boolean)
      return `${name}${details.length ? `（${details.join('，')}）` : ''}`
    }).join('；') || '-'
  }
  if (typeof value === 'object') return JSON.stringify(value, null, 2)
  return String(value)
}

const flattenSnapshot = (value, prefix = '', target = {}) => {
  if (Array.isArray(value) || value == null || typeof value !== 'object') {
    target[prefix] = value
    return target
  }
  Object.entries(value).forEach(([key, child]) => {
    const path = prefix ? `${prefix}.${key}` : key
    if (key === 'contributors' || Array.isArray(child)) target[path] = child
    else flattenSnapshot(child, path, target)
  })
  return target
}

const snapshotPathLabel = path => {
  const parts = path.split('.')
  const leaf = snapshotLabels[parts.at(-1)] || parts.at(-1)
  return parts[0] === 'workInfo' ? `作品 / ${leaf}` : leaf
}

const selectedChangeRows = computed(() => {
  const before = parseSnapshot(selectedLog.value?.beforeData)
  const after = parseSnapshot(selectedLog.value?.afterData)
  if (!before || !after) return []
  const beforeFlat = flattenSnapshot(before)
  const afterFlat = flattenSnapshot(after)
  const paths = [...new Set([...Object.keys(beforeFlat), ...Object.keys(afterFlat)])]
  return paths
    .filter(path => JSON.stringify(beforeFlat[path] ?? null) !== JSON.stringify(afterFlat[path] ?? null))
    .map(path => ({
      path,
      label: snapshotPathLabel(path),
      before: formatSnapshotValue(beforeFlat[path]),
      after: formatSnapshotValue(afterFlat[path])
    }))
})

const prettySnapshot = value => {
  if (!value) return '-'
  const parsed = parseSnapshot(value)
  return parsed ? JSON.stringify(parsed, null, 2) : value
}

const loadLogs = async () => {
  loading.value = true
  try {
    const res = await getOperationLogs({ ...query })
    logs.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const search = () => {
  query.pageNum = 1
  loadLogs()
}

const reset = () => {
  Object.assign(query, {
    realName: '', operationType: '', targetModule: '', resultStatus: '', requestId: '',
    pageNum: 1, pageSize: 10
  })
  loadLogs()
}

const openDetail = row => {
  selectedLog.value = row
  detailVisible.value = true
}

onMounted(loadLogs)
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.filters {
  display: grid;
  grid-template-columns: repeat(5, minmax(120px, 1fr)) auto auto;
  gap: 10px;
  align-items: center;
}

pre {
  margin: 0;
  white-space: pre-wrap;
  word-break: break-all;
}

.change-title {
  margin: 22px 0 10px;
  font-size: 16px;
  color: #1f2937;
}

.change-table {
  margin-bottom: 18px;
}

.snapshot-value {
  display: block;
  white-space: pre-wrap;
  word-break: break-word;
}

.removed-value {
  color: #991b1b;
  background: #fef2f2;
}

.added-value {
  color: #166534;
  background: #f0fdf4;
}

.raw-snapshot {
  margin-top: 12px;
}

.raw-block + .raw-block {
  margin-top: 14px;
}

@media (max-width: 1100px) {
  .filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
