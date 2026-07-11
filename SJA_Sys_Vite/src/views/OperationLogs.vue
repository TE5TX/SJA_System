<template>
  <div class="operation-logs">
    <div class="page-title">
      <h1>操作日志</h1>
      <p>查看资料修改、稿费提交和审核等关键操作记录。</p>
    </div>

    <el-card>
      <template #header>
        <el-button type="primary" :icon="Refresh" @click="loadLogs">刷新</el-button>
      </template>
      <el-table :data="logs" v-loading="loading" border>
        <el-table-column prop="realName" label="操作人" width="120" />
        <el-table-column prop="operationType" label="类型" width="120" />
        <el-table-column prop="targetModule" label="模块" width="120" />
        <el-table-column prop="detail" label="详情" min-width="260" show-overflow-tooltip />
        <el-table-column prop="ipAddress" label="IP" width="140" />
        <el-table-column label="时间" width="170">
          <template #default="{ row }">{{ time(row.createTime) }}</template>
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
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { Refresh } from '@element-plus/icons-vue'
import { getOperationLogs } from '@/api/system'

const query = reactive({ pageNum: 1, pageSize: 10 })
const logs = ref([])
const total = ref(0)
const loading = ref(false)
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
</style>
