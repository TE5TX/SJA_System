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
        <el-descriptions-item label="变更前"><pre>{{ selectedLog.beforeData || '-' }}</pre></el-descriptions-item>
        <el-descriptions-item label="变更后"><pre>{{ selectedLog.afterData || '-' }}</pre></el-descriptions-item>
        <el-descriptions-item label="客户端">{{ selectedLog.userAgent || '-' }}</el-descriptions-item>
      </el-descriptions>
    </el-drawer>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
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

@media (max-width: 1100px) {
  .filters {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
