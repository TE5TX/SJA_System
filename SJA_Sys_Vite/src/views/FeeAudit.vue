<template>
  <div class="fee-audit">
    <div class="page-title">
      <h1>{{ canAuditFee ? '稿费审核与导出' : '稿费任务管理' }}</h1>
      <p>{{ canAuditFee ? '查看稿费明细、审核通过或驳回，并按筛选条件导出对账清单。' : '查看所有部门任务，并修改尚未审核的任务信息。' }}</p>
    </div>

    <el-card>
      <template #header>
        <div class="toolbar">
          <!-- 202608120849 - 修复了稿费审核筛选条件变化后必须点击查询按钮才会刷新数据的问题。 -->
          <el-select v-model="query.deptCode" placeholder="全部部门" clearable @change="searchFees">
            <el-option v-for="dept in deptOptions" :key="dept.value" :label="dept.label" :value="dept.value" />
          </el-select>
          <el-date-picker
            v-model="query.publishDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="发布开始日期"
            end-placeholder="发布结束日期"
            range-separator="至"
            aria-label="发布日期范围"
            class="date-range-filter"
            clearable
            @change="searchFees"
          />
          <!-- 202609091610 - 完成了发布日期和创建日期的起止范围筛选。 -->
          <el-date-picker
            v-model="query.createDateRange"
            type="daterange"
            value-format="YYYY-MM-DD"
            start-placeholder="创建开始日期"
            end-placeholder="创建结束日期"
            range-separator="至"
            aria-label="创建日期范围"
            class="date-range-filter"
            clearable
            @change="searchFees"
          />
          <el-select v-model="query.status" placeholder="全部状态" clearable @change="searchFees">
            <el-option label="已提交" value="SUBMITTED" />
            <el-option label="已通过" value="APPROVED" />
            <el-option label="已驳回" value="REJECTED" />
            <!-- 202609081000 - 完成了稿费审核与导出页未发布任务筛选的任务。 -->
            <el-option label="未发布" value="UNPUBLISHED" />
          </el-select>
          <!-- 202608121041 - 新增了按参与人员姓名实时查找稿费任务的功能。 -->
          <el-input
            v-model="query.contributorName"
            class="contributor-search"
            clearable
            :prefix-icon="Search"
            placeholder="查找参与人员"
            @input="scheduleContributorSearch"
            @clear="searchFees"
            @keyup.enter="searchFees"
          />
          <el-button type="primary" :icon="Search" @click="searchFees">查询</el-button>
          <el-button :icon="Refresh" @click="resetQuery">重置</el-button>
          <el-button v-if="canManageFeeAmount" type="success" :icon="Download" @click="openExport(false)">导出</el-button>
          <el-dropdown v-if="canManageFeeAmount" trigger="click" @command="handleBatchCommand">
            <el-button :disabled="selectedRecords.length === 0">
              批量操作{{ selectedRecords.length ? `（${selectedRecords.length}）` : '' }}
              <el-icon class="el-icon--right"><ArrowDown /></el-icon>
            </el-button>
            <template #dropdown>
              <el-dropdown-menu>
                <el-dropdown-item command="approve">批量通过</el-dropdown-item>
                <el-dropdown-item command="reject">批量驳回</el-dropdown-item>
                <el-dropdown-item command="export" divided>批量导出</el-dropdown-item>
              </el-dropdown-menu>
            </template>
          </el-dropdown>
        </div>
      </template>

      <el-table
        ref="feeTableRef"
        class="centered-data-table fee-audit-table desktop-only"
        :data="records"
        v-loading="loading"
        border
        :row-key="feeRowKey"
        :row-class-name="feeRowClassName"
        @row-click="handleFeeRowClick"
        @selection-change="handleSelectionChange"
      >
        <!-- 202608111533 - 修复了稿费审核与导出列表没有任务勾选框和批量操作入口的问题。 -->
        <!-- 202609290930 - 收敛列宽并移除固定列，让桌面端完整展示审核表格而不横向滚动。 -->
        <el-table-column type="selection" width="42" reserve-selection />
        <!-- 202608131617 - 为稿费审核与导出列表增加了跨分页连续序号。 -->
        <el-table-column label="序号" width="52" align="center">
          <template #default="{ $index }">{{ rowNumber($index) }}</template>
        </el-table-column>
        <!-- 202609291103 - 将稿费任务类型改为普通文字，移除蓝色标签边框。 -->
        <el-table-column prop="workType" label="类型" width="78">
          <template #default="{ row }">
            <span>{{ workTypeName(row.workType) }}</span>
          </template>
        </el-table-column>
        <el-table-column prop="title" label="作品标题" min-width="180">
          <template #default="{ row }"><span class="fee-audit-title">{{ row.title || '未命名作品' }}</span></template>
        </el-table-column>
        <el-table-column label="发布时间" width="105">
          <template #default="{ row }">{{ displayPublishDate(row.publishDate, row.isPublished) }}</template>
        </el-table-column>
        <el-table-column v-if="canManageFeeAmount" label="金额" width="95">
          <template #default="{ row }">¥{{ money(row.totalAmount) }}</template>
        </el-table-column>
        <el-table-column label="状态" width="86">
          <template #default="{ row }">
            <el-tag :type="statusType(row.status)">{{ statusName(row.status) }}</el-tag>
          </template>
        </el-table-column>
        <el-table-column label="创建时间" width="145">
          <template #default="{ row }">{{ time(row.createTime) }}</template>
        </el-table-column>
        <el-table-column label="操作" width="210">
          <template #default="{ row }">
            <div class="action-buttons">
              <el-button link type="primary" :icon="View" @click="openDetail(row)">详情</el-button>
              <el-button v-if="canEditRecord(row)" link type="primary" :icon="Edit" @click="openEdit(row)">
                {{ row.status === 'REJECTED' ? '修改重提' : '编辑' }}
              </el-button>
              <el-button v-if="canAuditFee && row.status === 'SUBMITTED'" link type="success" :icon="CircleCheck" @click="openAudit(row, true)">通过</el-button>
              <el-button v-if="canAuditFee && row.status === 'SUBMITTED'" link type="danger" :icon="CircleClose" @click="openAudit(row, false)">驳回</el-button>
              <el-button v-if="canAuditFee && row.status === 'APPROVED'" link type="warning" :icon="RefreshLeft" @click="handleResetAudit(row)">退回待审</el-button>
              <el-button v-if="canInvalidateRecord(row)" link type="danger" :icon="Delete" @click="handleInvalidate(row)">删除</el-button>
            </div>
          </template>
        </el-table-column>
      </el-table>

      <!-- 审核信息在手机端按任务分组，避免固定操作列挤占标题和状态。 -->
      <div v-loading="loading" class="mobile-only mobile-fee-list">
        <div v-if="!loading && records.length === 0" class="mobile-empty">暂无稿费任务</div>
        <article v-for="(record, index) in records" :key="feeRowKey(record)" class="mobile-record-card">
          <div class="mobile-record-card__header">
            <div class="mobile-fee-title">
              <el-checkbox
                v-if="canManageFeeAmount"
                :model-value="isRecordSelected(record)"
                :aria-label="`选择${record.title}`"
                @change="toggleMobileRecord(record)"
              />
              <strong>{{ rowNumber(index) }}. {{ record.title || '未命名作品' }}</strong>
            </div>
            <el-tag :type="statusType(record.status)">{{ statusName(record.status) }}</el-tag>
          </div>
          <dl class="mobile-record-card__grid">
            <div><dt>类型</dt><dd>{{ workTypeName(record.workType) }}</dd></div>
            <div><dt>发布时间</dt><dd>{{ displayPublishDate(record.publishDate, record.isPublished) }}</dd></div>
            <div v-if="canManageFeeAmount"><dt>金额</dt><dd>¥{{ money(record.totalAmount) }}</dd></div>
            <div><dt>创建时间</dt><dd>{{ time(record.createTime) }}</dd></div>
          </dl>
          <div class="mobile-record-card__actions fee-card-actions">
            <el-button type="primary" plain :icon="View" @click="openDetail(record)">详情</el-button>
            <el-button
              v-if="canEditRecord(record)"
              type="primary"
              plain
              :icon="Edit"
              @click="openEdit(record)"
            >{{ record.status === 'REJECTED' ? '修改重提' : '编辑' }}</el-button>
            <el-button v-if="canAuditFee && record.status === 'SUBMITTED'" type="success" plain :icon="CircleCheck" @click="openAudit(record, true)">通过</el-button>
            <el-button v-if="canAuditFee && record.status === 'SUBMITTED'" type="danger" plain :icon="CircleClose" @click="openAudit(record, false)">驳回</el-button>
            <el-button v-if="canAuditFee && record.status === 'APPROVED'" type="warning" plain :icon="RefreshLeft" @click="handleResetAudit(record)">退回待审</el-button>
            <el-button v-if="canInvalidateRecord(record)" type="danger" plain :icon="Delete" @click="handleInvalidate(record)">删除</el-button>
          </div>
        </article>
      </div>

      <div class="pagination">
        <ResponsivePagination
          v-model:current-page="query.pageNum"
          v-model:page-size="query.pageSize"
          :page-sizes="[10, 20, 50, 100, 200]"
          :total="total"
          @size-change="loadList"
          @current-change="loadList"
        />
      </div>
    </el-card>

    <el-drawer v-model="detailVisible" title="稿费详情" :size="isMobile ? '100%' : '620px'">
      <div v-loading="detailLoading">
        <el-descriptions v-if="detail" :column="1" border>
          <el-descriptions-item label="作品类型">{{ workTypeName(detail.workType) }}</el-descriptions-item>
          <el-descriptions-item label="状态">{{ statusName(detail.status) }}</el-descriptions-item>
          <el-descriptions-item v-if="canManageFeeAmount" label="总金额">¥{{ money(detail.totalAmount) }}</el-descriptions-item>
          <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
        </el-descriptions>

        <h3 class="detail-title">作品字段</h3>
        <el-table :data="workInfoRows" border size="small">
          <el-table-column prop="label" label="字段" width="160" />
          <el-table-column prop="value" label="内容" min-width="260" show-overflow-tooltip />
        </el-table>

        <h3 class="detail-title">参与人员</h3>
        <el-table :data="detail?.contributors || []" border size="small">
          <el-table-column prop="realName" label="姓名" min-width="120" />
          <el-table-column prop="roleInWork" label="角色" width="110">
            <template #default="{ row }">{{ roleName(row.roleInWork) }}</template>
          </el-table-column>
          <el-table-column v-if="detailShowsWorkCount" prop="workCount" label="作品数量" width="100" />
          <el-table-column v-if="canManageFeeAmount" label="金额" width="120">
            <template #default="{ row }">¥{{ money(row.feeAmount) }}</template>
          </el-table-column>
        </el-table>
      </div>
    </el-drawer>

    <ResponsiveDialog v-model="auditVisible" :title="auditDialogTitle" desktop-width="480px">
      <el-form class="mobile-form-top-label" label-width="86px">
        <el-form-item label="作品">
          <el-input :model-value="batchAuditMode ? `已选择 ${selectedRecords.length} 个任务` : currentRow?.title" disabled />
        </el-form-item>
        <el-form-item label="意见">
          <el-input v-model="auditForm.opinion" type="textarea" :rows="4" maxlength="500" show-word-limit placeholder="填写审核意见" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="auditVisible = false">取消</el-button>
        <el-button :type="auditForm.approved ? 'success' : 'danger'" :loading="auditing" @click="handleAudit">确认</el-button>
      </template>
    </ResponsiveDialog>

    <ResponsiveDialog v-model="editVisible" title="编辑稿费任务" desktop-width="760px">
      <el-alert
        v-if="hasAutomaticReadOnlyFields"
        class="automatic-field-notice"
        type="info"
        :closable="false"
        show-icon
        title="标记为“自动计算”的内容由稿费标准和参与人员生成，不能手工修改。"
      />
      <el-form label-width="110px" class="edit-work-form mobile-form-top-label">
        <el-form-item v-for="field in editWorkFields" :key="field.key" :label="field.label">
          <el-select
            v-if="field.key === 'platform'"
            v-model="editForm.workInfo[field.key]"
            @change="handleEditPlatformChange"
          >
            <el-option v-for="option in platformOptions" :key="option" :label="option" :value="option" />
          </el-select>
          <el-select
            v-else-if="field.key === 'category' && editCategoryOptions.length"
            v-model="editForm.workInfo[field.key]"
            placeholder="请选择类别"
            @change="handleEditCategoryChange"
          >
            <el-option v-for="option in editCategoryOptions" :key="option" :label="option" :value="option" />
          </el-select>
          <el-switch
            v-else-if="switchFieldKeys.includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            :active-value="1"
            :inactive-value="0"
            @change="value => handleEditSwitchChange(field.key, value)"
          />
          <el-input-number
            v-else-if="stepThousandNumberFields.includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            :min="0"
            :step="1000"
            controls-position="right"
            @focus="selectNumberInputText"
            @click="selectNumberInputText"
          />
          <el-input-number
            v-else-if="numberFieldKeys.includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            :min="0"
            controls-position="right"
            @focus="selectNumberInputText"
            @click="selectNumberInputText"
          />
          <el-segmented
            v-else-if="field.key === 'specialMultiplier'"
            v-model="editForm.workInfo[field.key]"
            :options="[2, 3]"
          />
          <!-- 202609281449 - 修复了编辑稿费任务时滚轮误改发布日期的问题，保留日期选择器与弹窗正常滚动。 -->
          <el-date-picker
            v-else-if="field.key === 'publishDate'"
            v-model="editForm.workInfo[field.key]"
            type="date"
            value-format="YYYY-MM-DD"
            :teleported="false"
            popper-class="single-year-date-picker"
          />
          <el-date-picker
            v-else-if="field.key === 'subsidyMonth'"
            v-model="editForm.workInfo[field.key]"
            type="month"
            value-format="YYYY-MM"
          />
          <el-input
            v-else-if="['remark', 'reprintStatus'].includes(field.key)"
            v-model="editForm.workInfo[field.key]"
            type="textarea"
            :rows="3"
            maxlength="500"
            show-word-limit
          />
          <el-input
            v-else
            v-model="editForm.workInfo[field.key]"
            :placeholder="['articleLink', 'videoLink'].includes(field.key) ? 'https://' : ''"
          />
        </el-form-item>
      </el-form>
      <!-- 202609291624 - 完成了编辑页人员选择与作品数量调整，并限制金额列可见角色的任务。 -->
      <div class="edit-contributor-heading">
        <h3 class="detail-title">{{ canManageFeeAmount ? '参与人员金额' : '参与人员' }}（{{ editForm.contributors.length }} 人）</h3>
        <el-button type="primary" plain @click="addEditContributor">添加人员</el-button>
      </div>
      <el-table :data="editForm.contributors" border size="small" v-loading="videoPreviewLoading">
        <el-table-column label="姓名" min-width="190">
          <template #default="{ row }">
            <el-select v-model="row.userId" filterable placeholder="请选择人员" aria-label="参与人员">
              <el-option
                v-for="person in editContributorOptions(row)"
                :key="person.id"
                :label="`${person.realName}（${person.studentNo || '-'}）`"
                :value="Number(person.id)"
              />
            </el-select>
          </template>
        </el-table-column>
        <el-table-column label="角色" width="150">
          <template #default="{ row }">
            <el-select v-if="editForm.workType === 'VIDEO'" v-model="row.roleInWork" aria-label="任务角色">
              <el-option label="拍摄" value="SHOOTER" />
              <el-option label="剪辑" value="EDITOR" />
            </el-select>
            <span v-else>{{ roleMap[row.roleInWork] || row.roleInWork || '-' }}</span>
          </template>
        </el-table-column>
        <el-table-column v-if="canManageFeeAmount" label="金额（自动计算）" width="170">
          <template #default="{ row }">
            <div class="readonly-money" aria-readonly="true">
              <span>¥{{ money(row.feeAmount) }}</span>
              <el-tag size="small" type="info" effect="plain">只读</el-tag>
            </div>
          </template>
        </el-table-column>
        <el-table-column v-if="editShowsWorkCount" label="作品数量" width="130">
          <template #default="{ row }">
            <el-input-number v-model="row.workCount" :min="1" controls-position="right" />
          </template>
        </el-table-column>
        <el-table-column label="操作" width="70">
          <template #default="{ $index }">
            <el-button link type="danger" @click="editForm.contributors.splice($index, 1)">删除</el-button>
          </template>
        </el-table-column>
      </el-table>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="handleSaveEdit">保存</el-button>
      </template>
    </ResponsiveDialog>

    <ResponsiveDialog v-model="exportVisible" :title="exportSelectedOnly ? '批量导出已选稿费' : '导出稿费明细'" desktop-width="760px">
      <!-- 202608111715 - 新增了采编各平台、摄影、视觉、视频、补贴和自定义八种导出字段模板。 -->
      <el-radio-group v-model="exportPreset" class="export-preset-list" @change="applyExportPreset">
        <el-radio-button v-for="preset in exportPresetOptions" :key="preset.value" :label="preset.value">
          {{ preset.label }}
        </el-radio-button>
      </el-radio-group>
      <div v-if="usesFixedWorkbookTemplate" class="export-template-tip">
        当前类型将按现有部门工作表版式导出，字段顺序与样表保持一致。
      </div>
      <div class="export-actions">
        <el-button text class="export-action-btn" @click="toggleAllExportFields">
          {{ allExportFieldsSelected ? '取消全选' : '全选' }}
        </el-button>
        <el-button text class="export-action-btn" @click="restoreDefaultExportFields">恢复默认</el-button>
      </div>
      <el-checkbox-group v-model="exportFields" class="export-field-list" @change="handleExportFieldsChange">
        <el-checkbox v-for="field in exportFieldOptions" :key="field.key" :label="field.key">
          {{ field.label }}
        </el-checkbox>
      </el-checkbox-group>
      <template #footer>
        <el-button @click="exportVisible = false">取消</el-button>
        <el-button type="primary" :loading="exporting" @click="handleExport">导出</el-button>
      </template>
    </ResponsiveDialog>
  </div>
</template>

<script setup>
import { computed, onMounted, reactive, ref, watch } from 'vue'
import { ArrowDown, CircleCheck, CircleClose, Delete, Download, Edit, Refresh, RefreshLeft, Search, View } from '@element-plus/icons-vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { auditFee, batchAuditFee, calculateFee, exportFee, getFeeContributors, getFeeDetail, getFeeList, invalidateRejectedFee, resetFeeAudit, updateFeeDetail } from '@/api/fee'
import { useUserStore } from '@/store/user'
import ResponsiveDialog from '@/components/responsive/ResponsiveDialog.vue'
import ResponsivePagination from '@/components/responsive/ResponsivePagination.vue'
import { useResponsive } from '@/composables/useResponsive'

const { isMobile } = useResponsive()

const fallbackDepts = [
  { label: '采编部', value: 'EDITORIAL' },
  { label: '采编部-微信', value: 'EDITORIAL:微信' },
  { label: '采编部-院网', value: 'EDITORIAL:院网' },
  { label: '采编部-校报', value: 'EDITORIAL:校报' },
  { label: '摄影部', value: 'PHOTOGRAPHY' },
  { label: '摄影部-微信', value: 'PHOTOGRAPHY:微信' },
  { label: '摄影部-院网', value: 'PHOTOGRAPHY:院网' },
  { label: '摄影部-校报', value: 'PHOTOGRAPHY:校报' },
  { label: '视觉部', value: 'VISUAL' },
  { label: '视频部', value: 'VIDEO' },
  { label: '月度补贴', value: 'MANAGEMENT' }
]
const platformOptions = ['微信', '院网', '校报']
const photoCategoryOptions = ['原创类', '半原创类']
const visualCategoryOptions = ['普通消息类', '原创美文（不含制图）', '原创美文（含部分制图）', '互动、长图、SVG动图、手绘', '校园网首页大图']
const subsidyCategoryOptions = ['微信统筹编辑', '排版编辑', '校对', '主席团成员', '校园巡查组长', '校园巡查组员']
const videoCategoryOptions = ['活动新闻类视频', '风景类系列短视频', '原创类短视频', '原创类长视频']
const switchFieldKeys = ['isPublished', 'isSpecialNode', 'isReposted', 'isComplexEdit']
const stepThousandNumberFields = ['readCount', 'wordCount']
const numberFieldKeys = ['likeCount']

// 202608111533 - 修复了稿费审核与导出列表单页不能选择100或200条的问题。
// 202609091610 - 完成了列表与导出共用日期范围参数的转换。
const dateQueryParams = () => ({
  deptCode: query.deptCode,
  status: query.status,
  contributorName: query.contributorName,
  publishStartDate: query.publishDateRange?.[0] || '',
  publishEndDate: query.publishDateRange?.[1] || '',
  createStartDate: query.createDateRange?.[0] || '',
  createEndDate: query.createDateRange?.[1] || ''
})
const query = reactive({ deptCode: '', publishDateRange: [], createDateRange: [], status: '', contributorName: '', pageNum: 1, pageSize: 10 })
const records = ref([])
const total = ref(0)
const loading = ref(false)
const exporting = ref(false)
const exportVisible = ref(false)
const exportSelectedOnly = ref(false)
const detailVisible = ref(false)
const detailLoading = ref(false)
const detail = ref(null)
const auditVisible = ref(false)
const auditing = ref(false)
const batchAuditMode = ref(false)
const editVisible = ref(false)
const saving = ref(false)
const videoPreviewLoading = ref(false)
const currentRow = ref(null)
const feeTableRef = ref(null)
const selectedRecords = ref([])
const highlightedFeeRowKey = ref('')
const deptOptions = ref(fallbackDepts)
const auditForm = reactive({ approved: true, opinion: '' })
const editForm = reactive({ workType: '', workInfo: {}, contributors: [] })
const editUsers = ref([])
const userStore = useUserStore()

const workTypeMap = { EDITORIAL: '采编', PHOTO: '摄影', VIDEO: '视频', VISUAL: '视觉', SUBSIDY: '月度补贴' }
const roleMap = { AUTHOR: '作者', SHOOTER: '拍摄', EDITOR: '编辑/剪辑', DESIGNER: '设计', MEMBER: '补贴人员' }
const exportFieldOptions = [
  { key: 'workType', label: '作品类型' },
  { key: 'deptName', label: '部门' },
  { key: 'title', label: '标题' },
  { key: 'platform', label: '发布平台' },
  { key: 'category', label: '类别' },
  { key: 'publishDate', label: '发布时间' },
  { key: 'articleLink', label: '作品链接' },
  { key: 'isPublished', label: '是否已发布' },
  { key: 'readCount', label: '阅读量' },
  { key: 'wordCount', label: '字数' },
  { key: 'videoDurationSec', label: '视频时长' },
  { key: 'likeCount', label: '点赞数' },
  { key: 'isReposted', label: '官方转载' },
  { key: 'isComplexEdit', label: '复杂剪辑' },
  { key: 'subsidyMonth', label: '补贴月份' },
  { key: 'contributorNames', label: '参与人员姓名' },
  { key: 'contributorStudentNos', label: '参与人员学号' },
  { key: 'contributorRoles', label: '参与角色' },
  { key: 'workCounts', label: '作品数量' },
  { key: 'contributorAmounts', label: '个人金额' },
  { key: 'totalAmount', label: '总金额' },
  { key: 'auditStatus', label: '审核状态' },
  { key: 'isSpecialNode', label: '特殊节点' },
  { key: 'specialMultiplier', label: '特殊节点倍数' },
  { key: 'remark', label: '备注' },
  { key: 'createTime', label: '创建时间' }
]
const defaultExportFields = [
  'workType', 'deptName', 'title', 'platform', 'category', 'publishDate',
  'contributorNames', 'contributorStudentNos', 'contributorRoles',
  'contributorAmounts', 'totalAmount', 'auditStatus', 'remark'
]
const exportPresetOptions = [
  { label: '采编-微信', value: 'EDITORIAL_WECHAT' },
  { label: '采编-院网', value: 'EDITORIAL_WEBSITE' },
  { label: '采编-校报', value: 'EDITORIAL_NEWSPAPER' },
  { label: '摄影', value: 'PHOTO' },
  { label: '视觉', value: 'VISUAL' },
  { label: '视频', value: 'VIDEO' },
  { label: '补贴', value: 'SUBSIDY' },
  { label: '自定义', value: 'CUSTOM' }
]
const commonWorkFields = [
  'title', 'category', 'publishDate', 'articleLink',
  'contributorNames', 'contributorAmounts', 'totalAmount',
  'isSpecialNode', 'specialMultiplier', 'remark'
]
const exportPresetFields = {
  EDITORIAL_WECHAT: [...commonWorkFields.slice(0, 5), 'readCount', ...commonWorkFields.slice(5)],
  EDITORIAL_WEBSITE: [...commonWorkFields],
  EDITORIAL_NEWSPAPER: [...commonWorkFields.slice(0, 5), 'wordCount', ...commonWorkFields.slice(5)],
  PHOTO: [...commonWorkFields.slice(0, 6), 'workCounts', ...commonWorkFields.slice(6)],
  VISUAL: [...commonWorkFields],
  VIDEO: [
    ...commonWorkFields.slice(0, 6), 'likeCount', 'isReposted', 'isComplexEdit',
    ...commonWorkFields.slice(6)
  ],
  SUBSIDY: [
    'title', 'category', 'subsidyMonth',
    'contributorNames', 'contributorAmounts', 'totalAmount', 'remark'
  ]
}
const exportPreset = ref('CUSTOM')
const exportFields = ref([...defaultExportFields])
const allExportFieldsSelected = computed(() => exportFields.value.length === exportFieldOptions.length)
// 202609081435 - 完成了采编校报使用与参考工作簿一致的固定导出模板的任务。
const fixedWorkbookTemplates = new Set(['EDITORIAL_WECHAT', 'EDITORIAL_WEBSITE', 'EDITORIAL_NEWSPAPER', 'PHOTO', 'VISUAL', 'VIDEO'])
const usesFixedWorkbookTemplate = computed(() => fixedWorkbookTemplates.has(exportPreset.value))

// 202608101716 - 将稿费金额统一显示为四舍五入后的一位小数。
const money = value => Number(value || 0).toFixed(1)
const time = value => value ? String(value).replace('T', ' ').slice(0, 16) : '-'
// 202609281627 - 未发布任务的隐藏日期只用于内部统计，页面统一以“-”表示尚无实际发布日期。
const displayPublishDate = (value, isPublished) => String(isPublished) === '0' ? '-' : (value || '-')
const workTypeName = type => workTypeMap[type] || type || '-'
const roleName = role => roleMap[role] || role || '-'
const statusName = status => ({ SUBMITTED: '已提交', APPROVED: '已通过', REJECTED: '已驳回' }[status] || status || '-')
const statusType = status => ({ APPROVED: 'success', REJECTED: 'danger', SUBMITTED: 'warning' }[status] || 'info')
const isEditableStatus = status => status === 'SUBMITTED' || status === 'REJECTED'
const roleCodes = computed(() => userStore.userInfo?.roleCodes || [])
const highestRoleCode = computed(() => roleCodes.value.length ? Math.min(...roleCodes.value) : 13)
const canManageFeeAmount = computed(() => [0, 3].includes(highestRoleCode.value))
const canAuditFee = computed(() => [0, 3].includes(highestRoleCode.value))
const canEditFeeTask = computed(() => [0, 3, 4].includes(highestRoleCode.value))
// 202609291058 - 部长可跨部门查看任务，但编辑和删除入口只针对其所属部门显示。
const workTypeDeptCodes = { EDITORIAL: 'EDITORIAL', PHOTO: 'PHOTOGRAPHY', VIDEO: 'VIDEO', VISUAL: 'VISUAL', SUBSIDY: 'MANAGEMENT' }
const canManageRecordDepartment = row => canManageFeeAmount.value
  || (userStore.userInfo?.deptCodes || []).includes(workTypeDeptCodes[row.workType])
const canEditRecord = row => canEditFeeTask.value && canManageRecordDepartment(row)
  && (canManageFeeAmount.value || isEditableStatus(row.status))
const canInvalidateRecord = row => canEditFeeTask.value && canManageRecordDepartment(row)
  && row.status === 'REJECTED'
const automaticEditTypes = new Set(['EDITORIAL', 'PHOTO', 'VISUAL', 'VIDEO', 'SUBSIDY'])
const isAutomaticEditType = computed(() => automaticEditTypes.has(editForm.workType))
const hasAutomaticReadOnlyFields = computed(() => isAutomaticEditType.value)
const auditDialogTitle = computed(() => {
  const action = auditForm.approved ? '通过' : '驳回'
  return batchAuditMode.value ? `批量${action}稿费任务` : `审核${action}`
})
const fieldLabelMap = {
  title: '标题',
  videoTitle: '视频标题',
  platform: '发布平台',
  category: '类别',
  readCount: '阅读量',
  wordCount: '字数',
  videoDurationSec: '时长（秒）',
  likeCount: '点赞数',
  reprintStatus: '转载情况',
  isReposted: '是否官方转载',
  isComplexEdit: '是否复杂剪辑',
  authorCount: '参与人数',
  authorAvgFee: '人均金额',
  publishDate: '发布时间',
  articleLink: '文章链接',
  videoLink: '视频链接',
  isPublished: '是否已发布',
  isSpecialNode: '特殊节点',
  specialMultiplier: '特殊节点倍数',
  subsidyMonth: '补贴月份',
  remark: '备注'
}

const booleanWorkInfoFields = new Set(['isPublished', 'isSpecialNode', 'isReposted', 'isComplexEdit'])
/** 将作品字段转换为详情面板中的展示值，不改变提交或查询使用的原始数据。 */
const formatWorkInfoValue = (key, value, info) => {
  if (key === 'publishDate') return displayPublishDate(value, info.isPublished)
  if (key === 'authorAvgFee') return money(value)
  if (booleanWorkInfoFields.has(key)) return Number(value) === 1 ? '是' : '否'
  return value ?? '-'
}

const workInfoRows = computed(() => {
  const info = detail.value?.workInfo || {}
  // 202609081000 - 修复了视频部详情仍显示无计费作用时长字段的问题。
  return Object.entries(info)
    .filter(([key]) => !(detail.value?.workType === 'VIDEO' && key === 'videoDurationSec'))
    .map(([key, value]) => ({
      label: fieldLabelMap[key] || key,
      value: formatWorkInfoValue(key, value, info)
    }))
})
// 202609291624 - 修复了编辑字段依赖详情返回键导致条件字段遗漏的问题。
const editWorkFields = computed(() => {
  const info = editForm.workInfo || {}
  const type = editForm.workType
  const fields = type === 'EDITORIAL'
    ? ['title', 'platform', 'category', ...(info.platform === '微信' && Number(info.isPublished) === 1 ? ['readCount'] : []), ...(info.platform === '校报' ? ['wordCount'] : []), 'articleLink']
    : type === 'PHOTO'
      ? ['title', 'platform', ...(info.platform === '微信' ? ['readCount'] : []), 'articleLink']
      : type === 'VIDEO'
        ? ['videoTitle', 'category', 'likeCount', 'isReposted', ...(info.category === '原创类短视频' ? ['isComplexEdit'] : []), 'videoLink']
        : type === 'VISUAL'
          ? ['title', 'category', 'articleLink']
          : ['title', 'category', 'subsidyMonth']
  if (type !== 'SUBSIDY') {
    fields.push('isPublished')
    if (Number(info.isPublished) === 1) fields.push('publishDate')
    if (canManageFeeAmount.value) {
      fields.push('isSpecialNode')
      if (Number(info.isSpecialNode) === 1) fields.push('specialMultiplier')
    }
  }
  fields.push('remark')
  return fields.map(key => ({ key, label: fieldLabelMap[key] || key }))
})
const detailShowsWorkCount = computed(() => (detail.value?.contributors || []).some(item => item.workCount != null))
const editShowsWorkCount = computed(() =>
  (editForm.workType === 'PHOTO' && ['院网', '校报'].includes(editForm.workInfo.platform))
  || (editForm.workType === 'VISUAL' && editForm.workInfo.category === '校园网首页大图'))
const editCategoryOptions = computed(() => {
  if (editForm.workType === 'VIDEO') return videoCategoryOptions
  if (editForm.workType === 'VISUAL') return visualCategoryOptions
  if (editForm.workType === 'SUBSIDY') return subsidyCategoryOptions
  if (editForm.workType === 'PHOTO') {
    return editForm.workInfo?.platform === '微信' ? photoCategoryOptions : []
  }
  if (editForm.workType === 'EDITORIAL') {
    return {
      微信: ['原创类', '半原创类'],
      院网: ['莲峰群英', '学校新闻（含学校要闻、校园时讯）'],
      校报: ['原创头版头条', '原创一般稿件', '重新编辑类稿件']
    }[editForm.workInfo?.platform] || []
  }
  return []
})

const selectNumberInputText = event => {
  const target = event?.target
  if (target?.closest?.('.el-input-number__decrease, .el-input-number__increase')) return
  const input = target?.tagName === 'INPUT'
    ? target
    : event?.currentTarget?.querySelector?.('input')
  if (input?.select) {
    requestAnimationFrame(() => input.select())
  }
}

const handleEditPlatformChange = () => {
  if (editForm.workType === 'EDITORIAL') {
    editForm.workInfo.category = editCategoryOptions.value[0] || ''
  } else if (editForm.workType === 'PHOTO') {
    editForm.workInfo.category = editForm.workInfo.platform === '微信' ? photoCategoryOptions[0] : ''
  }
  editForm.contributors.forEach(item => { item.workCount = 1 })
}

const handleEditCategoryChange = () => {
  // 202609291624 - 修复了切换视频类别后隐藏的复杂剪辑状态仍影响作品信息的问题。
  if (editForm.workType === 'VIDEO' && editForm.workInfo.category !== '原创类短视频') {
    editForm.workInfo.isComplexEdit = 0
  }
  if (editForm.workType === 'VISUAL' && editForm.workInfo.category === '校园网首页大图') {
    editForm.contributors.forEach(item => { item.workCount = Number(item.workCount) > 0 ? item.workCount : 1 })
  }
  if (editForm.workType === 'SUBSIDY') {
    ElMessage.info('补贴类型已变更，请核对参与人员名单')
  }
}

const handleEditSwitchChange = (key, value) => {
  // 202609291624 - 修复了未发布任务改为已发布时沿用隐藏创建日期作为实际发布日期的问题。
  if (key === 'isPublished' && Number(value) === 1 && Number(detail.value?.workInfo?.isPublished) === 0) {
    editForm.workInfo.publishDate = ''
  }
}

const defaultEditRole = () => ({ VIDEO: 'SHOOTER', VISUAL: 'DESIGNER', SUBSIDY: 'MEMBER' }[editForm.workType] || 'AUTHOR')
const addEditContributor = () => {
  editForm.contributors.push({ userId: null, roleInWork: defaultEditRole(), workCount: 1, feeAmount: null, remark: '' })
}
const editContributorOptions = row => {
  const options = editUsers.value.filter(person => person.isValid !== 0)
  if (row.userId == null || options.some(person => Number(person.id) === Number(row.userId))) return options
  return [{ id: row.userId, realName: row.realName || '原参与人员', studentNo: row.studentNo || '', isValid: 1 }, ...options]
}

/** 编辑入参只允许提交页同样的有效字段，防止切换平台后沿用不相关的旧数据。 */
const editWorkInfoForSave = () => {
  const info = { ...editForm.workInfo }
  if (editForm.workType !== 'SUBSIDY' && Number(info.isPublished) !== 1) delete info.publishDate
  if (editForm.workType === 'EDITORIAL' && info.platform === '微信' && Number(info.isPublished) !== 1) delete info.readCount
  return info
}

const validateEdit = () => {
  const info = editForm.workInfo
  if (!String((editForm.workType === 'VIDEO' ? info.videoTitle : info.title) || '').trim()) return '请填写任务标题'
  if (editForm.workType !== 'PHOTO' && !info.category) return '请选择类别'
  if (['EDITORIAL', 'PHOTO'].includes(editForm.workType) && !info.platform) return '请选择发布平台'
  if (editForm.workType === 'SUBSIDY' && !info.subsidyMonth) return '请选择补贴月份'
  if (editForm.workType !== 'SUBSIDY' && Number(info.isPublished) === 1 && !info.publishDate) return '请选择发布时间'
  if (editForm.workType === 'EDITORIAL' && info.platform === '校报' && Number(info.wordCount) <= 0) return '校报文章字数必须大于 0'
  if (canManageFeeAmount.value && Number(info.isSpecialNode) === 1 && ![2, 3].includes(Number(info.specialMultiplier))) return '请选择特殊节点发放倍数'
  if (!editForm.contributors.length) return '请添加至少一名参与人员'
  if (editForm.contributors.some(item => !item.userId || !item.roleInWork)) return '请补全参与人员和角色'
  if (editShowsWorkCount.value && editForm.contributors.some(item => Number(item.workCount) <= 0)) return '请填写每位人员对应的作品数量'
  const identities = editForm.contributors.map(item => `${item.userId}:${item.roleInWork}`)
  if (new Set(identities).size !== identities.length) return '同一人员不能以相同角色重复参与'
  if (editForm.workType === 'VIDEO' && info.category === '原创类短视频' && Number(info.isComplexEdit) === 1
    && !editForm.contributors.some(item => item.roleInWork === 'EDITOR')) return '复杂剪辑须有剪辑人员'
  return ''
}

const feeRowKey = row => `${row.workType}:${row.id}`

// 202609290937 - 点击任务行后保留悬停式高亮；再次点击同一行取消，且不干扰批量勾选。
const feeRowClassName = ({ row }) => feeRowKey(row) === highlightedFeeRowKey.value ? 'fee-row-pinned' : ''
const handleFeeRowClick = (row, column) => {
  if (column?.type === 'selection' || column?.label === '操作') return
  const key = feeRowKey(row)
  highlightedFeeRowKey.value = highlightedFeeRowKey.value === key ? '' : key
}

const handleSelectionChange = selection => {
  selectedRecords.value = selection
}

const isRecordSelected = record => selectedRecords.value.some(item => feeRowKey(item) === feeRowKey(record))

// 手机卡片与桌面表格共享批量选择数据，切换视口时操作结果保持一致。
const toggleMobileRecord = record => {
  selectedRecords.value = isRecordSelected(record)
    ? selectedRecords.value.filter(item => feeRowKey(item) !== feeRowKey(record))
    : [...selectedRecords.value, record]
}

const clearSelection = () => {
  feeTableRef.value?.clearSelection()
  selectedRecords.value = []
}

const selectedTaskRefs = () => selectedRecords.value.map(item => ({
  workType: item.workType,
  id: Number(item.id)
}))

const loadList = async () => {
  loading.value = true
  try {
    const res = await getFeeList({ ...dateQueryParams(), pageNum: query.pageNum, pageSize: query.pageSize })
    records.value = res.data?.list || []
    total.value = Number(res.data?.total || 0)
  } finally {
    loading.value = false
  }
}

const searchFees = () => {
  if (contributorSearchTimer) clearTimeout(contributorSearchTimer)
  clearSelection()
  highlightedFeeRowKey.value = ''
  query.pageNum = 1
  loadList()
}

let contributorSearchTimer
const scheduleContributorSearch = () => {
  if (contributorSearchTimer) clearTimeout(contributorSearchTimer)
  contributorSearchTimer = setTimeout(searchFees, 300)
}

const resetQuery = () => {
  clearSelection()
  highlightedFeeRowKey.value = ''
  Object.assign(query, { deptCode: '', publishDateRange: [], createDateRange: [], status: '', contributorName: '', pageNum: 1, pageSize: 10 })
  loadList()
}

const openDetail = async (row, showDrawer = true) => {
  currentRow.value = row
  detail.value = null
  if (showDrawer) detailVisible.value = true
  detailLoading.value = true
  try {
    const res = await getFeeDetail(row.workType, row.id)
    detail.value = res.data
    return res.data
  } finally {
    detailLoading.value = false
  }
}

const openAudit = (row, approved) => {
  batchAuditMode.value = false
  currentRow.value = row
  auditForm.approved = approved
  auditForm.opinion = approved ? '审核通过' : ''
  auditVisible.value = true
}

const openBatchAudit = approved => {
  if (!selectedRecords.value.length) return
  if (selectedRecords.value.some(item => item.status !== 'SUBMITTED')) {
    ElMessage.warning('批量审核只能选择状态为“已提交”的任务')
    return
  }
  batchAuditMode.value = true
  currentRow.value = null
  auditForm.approved = approved
  auditForm.opinion = approved ? '批量审核通过' : ''
  auditVisible.value = true
}

const handleBatchCommand = command => {
  if (command === 'approve') openBatchAudit(true)
  else if (command === 'reject') openBatchAudit(false)
  else if (command === 'export') openExport(true)
}

const rowNumber = index => (query.pageNum - 1) * query.pageSize + index + 1

const requestBatchPassword = async actionName => {
  try {
    const { value } = await ElMessageBox.prompt(
      `请输入当前用户密码以执行${actionName}`,
      '批量操作身份校验',
      {
        confirmButtonText: '确认执行',
        cancelButtonText: '取消',
        inputType: 'password',
        inputPlaceholder: '当前用户密码',
        inputValidator: value => String(value || '').trim() ? true : '请输入当前用户密码',
        closeOnClickModal: false,
        showClose: false
      }
    )
    return value
  } catch {
    return ''
  }
}

const openEdit = async row => {
  if (!canEditRecord(row)) return
  // 202609291624 - 修复了直接编辑任务时先打开详情抽屉且可能沿用上次详情的问题。
  const task = await openDetail(row, false)
  // 202609291624 - 完成了编辑页复用提交页参与人员接口的任务。
  const usersResponse = await getFeeContributors()
  editUsers.value = usersResponse.data || []
  editForm.workType = task.workType
  editForm.workInfo = { ...(task.workInfo || {}), remark: task.remark || '' }
  editForm.contributors = (task.contributors || []).map(item => ({ ...item }))
  editVisible.value = true
  if (isAutomaticEditType.value && canManageFeeAmount.value) {
    scheduleAutomaticFeePreview()
  }
}

let feePreviewSequence = 0
let feePreviewTimer
const refreshAutomaticFeePreview = async () => {
  if (!editVisible.value || !isAutomaticEditType.value || !canManageFeeAmount.value || !editForm.contributors.length
    || editForm.contributors.some(item => !item.userId || !item.roleInWork)
    || (editForm.workType !== 'PHOTO' && !editForm.workInfo.category)) return
  const sequence = ++feePreviewSequence
  videoPreviewLoading.value = true
  try {
    const res = await calculateFee({
      workType: editForm.workType,
      workInfo: { ...editForm.workInfo },
      contributors: editForm.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: item.roleInWork,
        workCount: item.workCount == null ? null : Number(item.workCount),
        feeAmount: null,
        remark: item.remark || ''
      }))
    })
    if (sequence !== feePreviewSequence) return
    ;(res.data || []).forEach((amount, index) => {
      if (editForm.contributors[index]) editForm.contributors[index].feeAmount = Number(amount)
    })
  } finally {
    if (sequence === feePreviewSequence) videoPreviewLoading.value = false
  }
}

const scheduleAutomaticFeePreview = () => {
  if (feePreviewTimer) clearTimeout(feePreviewTimer)
  feePreviewTimer = setTimeout(refreshAutomaticFeePreview, 200)
}

watch(
  () => isAutomaticEditType.value ? JSON.stringify({
    workType: editForm.workType,
    workInfo: editForm.workInfo,
    contributors: editForm.contributors.map(item => ({
      userId: item.userId,
      roleInWork: item.roleInWork,
      workCount: item.workCount
    }))
  }) : '',
  scheduleAutomaticFeePreview
)

const handleSaveEdit = async () => {
  if (!currentRow.value) return
  const validationMessage = validateEdit()
  if (validationMessage) {
    ElMessage.warning(validationMessage)
    return
  }
  saving.value = true
  try {
    await updateFeeDetail(currentRow.value.workType, currentRow.value.id, {
      workType: currentRow.value.workType,
      workInfo: editWorkInfoForSave(),
      contributors: editForm.contributors.map(item => ({
        userId: Number(item.userId),
        roleInWork: item.roleInWork,
        // 202609291624 - 修复了编辑请求可能将页面金额当作权威计价结果的问题。
        feeAmount: null,
        workCount: editShowsWorkCount.value ? Number(item.workCount || 1) : null,
        remark: item.remark || ''
      }))
    })
    ElMessage.success('稿费任务已保存')
    editVisible.value = false
    detailVisible.value = false
    await loadList()
  } finally {
    saving.value = false
  }
}

const handleAudit = async () => {
  if (!batchAuditMode.value && !currentRow.value) return
  auditing.value = true
  try {
    if (batchAuditMode.value) {
      const currentPassword = await requestBatchPassword(`批量${auditForm.approved ? '通过' : '驳回'}`)
      if (!currentPassword) return
      await batchAuditFee({
        tasks: selectedTaskRefs(),
        approved: auditForm.approved,
        opinion: auditForm.opinion,
        currentPassword
      })
      ElMessage.success(`已批量${auditForm.approved ? '通过' : '驳回'} ${selectedRecords.value.length} 个任务`)
      clearSelection()
    } else {
      await auditFee(currentRow.value.workType, currentRow.value.id, { ...auditForm })
      ElMessage.success('审核操作已提交')
    }
    auditVisible.value = false
    await loadList()
  } finally {
    auditing.value = false
  }
}

const handleResetAudit = async row => {
  await ElMessageBox.confirm(
    `确定将“${row.title}”退回待审核状态吗？`,
    '退回待审核',
    { confirmButtonText: '确定退回', cancelButtonText: '取消', type: 'warning' }
  )
  await resetFeeAudit(row.workType, row.id)
  ElMessage.success('已退回待审核状态')
  await loadList()
}

const handleInvalidate = async row => {
  if (!canInvalidateRecord(row)) return
  await ElMessageBox.confirm(
    `确定删除已驳回的任务“${row.title}”吗？删除后不会出现在任务列表和稿费导出中，数据库记录仍会保留。`,
    '删除稿费任务',
    { confirmButtonText: '确定删除', cancelButtonText: '取消', type: 'warning' }
  )
  await invalidateRejectedFee(row.workType, row.id)
  ElMessage.success('任务已删除')
  await loadList()
}

const openExport = (selectedOnly = false) => {
  if (selectedOnly && !selectedRecords.value.length) {
    ElMessage.warning('请至少勾选一个稿费任务')
    return
  }
  exportSelectedOnly.value = selectedOnly
  if (!selectedOnly) {
    const inferredPreset = {
      'EDITORIAL:微信': 'EDITORIAL_WECHAT',
      'EDITORIAL:院网': 'EDITORIAL_WEBSITE',
      'EDITORIAL:校报': 'EDITORIAL_NEWSPAPER',
      PHOTOGRAPHY: 'PHOTO',
      VISUAL: 'VISUAL',
      VIDEO: 'VIDEO',
      MANAGEMENT: 'SUBSIDY'
    }[query.deptCode]
    if (inferredPreset) {
      exportPreset.value = inferredPreset
      applyExportPreset(inferredPreset)
    }
  }
  exportVisible.value = true
}

const applyExportPreset = preset => {
  if (preset === 'CUSTOM') return
  exportFields.value = [...exportPresetFields[preset]]
}

const handleExportFieldsChange = () => {
  const expected = exportPresetFields[exportPreset.value]
  if (!expected || expected.length !== exportFields.value.length || expected.some(field => !exportFields.value.includes(field))) {
    exportPreset.value = 'CUSTOM'
  }
}

const toggleAllExportFields = () => {
  exportFields.value = allExportFieldsSelected.value ? [] : exportFieldOptions.map(field => field.key)
}

const restoreDefaultExportFields = () => {
  const presetFields = exportPresetFields[exportPreset.value]
  exportFields.value = [...(presetFields || defaultExportFields)]
}

const handleExport = async () => {
  if (!exportFields.value.length) {
    ElMessage.warning('请至少选择一个导出字段')
    return
  }
  exporting.value = true
  try {
    const response = await exportFee({
      ...dateQueryParams(),
      templateType: exportPreset.value,
      fields: exportFields.value,
      selectedTasks: exportSelectedOnly.value ? selectedTaskRefs() : null
    })
    const blob = new Blob([response.data], { type: response.headers['content-type'] || 'application/vnd.openxmlformats-officedocument.spreadsheetml.sheet' })
    const url = window.URL.createObjectURL(blob)
    const link = document.createElement('a')
    link.href = url
    const templateFileNames = {
      EDITORIAL_WECHAT: '校学生记者团稿费统计（微信）',
      EDITORIAL_WEBSITE: '校学生记者团稿费统计（院网）',
      EDITORIAL_NEWSPAPER: '校学生记者团稿费统计（校报）',
      PHOTO: '校学生记者团稿费统计（摄影）',
      VISUAL: '校学生记者团稿费统计（视觉）',
      VIDEO: '校学生记者团稿费统计（视频）'
    }
    // 202609081510 - 统一了所有稿费统计表在浏览器端的下载文件名。
    link.download = `${templateFileNames[exportPreset.value] || '校学生记者团稿费统计（自定义）'}_${query.publishDateRange?.length === 2 ? query.publishDateRange.join('至') : '全部'}.xlsx`
    link.click()
    window.URL.revokeObjectURL(url)
    exportVisible.value = false
    ElMessage.success(exportSelectedOnly.value ? `已导出 ${selectedRecords.value.length} 个选中任务` : '导出完成')
  } finally {
    exporting.value = false
  }
}

onMounted(() => {
  loadList()
})
</script>

<style scoped>
.page-title p {
  margin: 0;
  color: #6b7280;
}

.toolbar {
  display: flex;
  flex-wrap: wrap;
  align-items: center;
  gap: 10px;
}

.toolbar :deep(.el-select),
.toolbar :deep(.el-date-editor) {
  width: 160px;
}

.toolbar :deep(.date-range-filter) {
  width: 300px;
  max-width: 100%;
  flex: 0 1 300px;
}

.toolbar .contributor-search {
  width: 180px;
}

.pagination {
  display: flex;
  justify-content: flex-end;
  margin-top: 18px;
}

.detail-title {
  margin: 18px 0 10px;
  font-size: 16px;
  color: var(--secondary-color);
}

.edit-work-form :deep(.el-select),
.edit-work-form :deep(.el-date-editor),
.edit-work-form :deep(.el-input-number),
.edit-work-form :deep(.el-segmented) {
  width: 100%;
}

/* 202609291624 - 完成了编辑人员操作区域在窄弹窗中换行展示的任务。 */
.edit-contributor-heading {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 10px;
}

.edit-contributor-heading .detail-title {
  margin: 0;
}

/* 202608131646 - 统一了稿费审核与导出主表各列的居中对齐样式。 */
.centered-data-table :deep(.el-table__cell .cell) {
  text-align: center;
}

.fee-audit-table {
  width: 100%;
}

/* 202609290937 - 固定高亮采用轻蓝底和左侧标识，持续显示且与普通悬停区分。 */
.fee-audit-table :deep(.el-table__body tr.fee-row-pinned > td.el-table__cell) {
  background-color: #edf3ff !important;
}

/* 左侧蓝色选中标识 */
/* .fee-audit-table :deep(.el-table__body tr.fee-row-pinned > td.el-table__cell:first-child) {
  box-shadow: inset 3px 0 var(--primary-color);
} */

.fee-audit-table :deep(.el-table__body tr.fee-row-pinned:hover > td.el-table__cell) {
  background-color: #e7efff !important;
}

/* 202609290930 - 标题按行换行，避免为了显示完整内容撑宽整张审核表。 */
.fee-audit-title {
  display: block;
  line-height: 1.45;
  white-space: normal;
  overflow-wrap: anywhere;
}

/* 内容区不足以容纳全部列时沿用已有任务卡片，保证信息可见而非裁掉表格。 */
@media (max-width: 1365px) {
  .fee-audit :deep(.desktop-only) {
    display: none !important;
  }

  .fee-audit :deep(.mobile-only) {
    display: block !important;
  }

  .mobile-fee-title {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 10px;
  }

  .mobile-fee-title strong {
    overflow-wrap: anywhere;
  }
}

.automatic-field-notice {
  margin-bottom: 18px;
}

.readonly-money {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 10px;
  width: 100%;
  min-height: 32px;
  padding: 0 10px;
  color: #374151;
  background: #f8fafc;
  border: 1px solid #e5e7eb;
  border-radius: 4px;
}

.readonly-money {
  min-height: 28px;
  font-variant-numeric: tabular-nums;
}

/* 202608121123 - 移除了审核编辑日期选择器中容易误触的跨年双箭头按钮。 */
:global(.single-year-date-picker .d-arrow-left),
:global(.single-year-date-picker .d-arrow-right) {
  display: none;
}

@media (max-width: 767px) {
  .toolbar {
    display: grid;
    grid-template-columns: minmax(0, 1fr);
    align-items: stretch;
    gap: 12px;
  }

  .toolbar :deep(.el-select),
  .toolbar :deep(.el-date-editor),
  .toolbar .contributor-search,
  .toolbar .el-button,
  .toolbar :deep(.el-dropdown) {
    width: 100%;
  }

  /* DatePicker 外层默认仍按 32px 参与 Grid 排版，内部 44px 输入框会覆盖下一行间距。 */
  .toolbar :deep(.el-date-editor.el-input),
  .toolbar :deep(.el-date-editor.el-input__wrapper) {
    box-sizing: border-box;
    height: var(--mobile-touch-size);
    min-height: var(--mobile-touch-size);
  }

  /* Element Plus 会给相邻按钮增加左边距，纵向工具栏必须清除，否则按钮会逐个缩进。 */
  .toolbar > .el-button,
  .toolbar > .el-button + .el-button,
  .toolbar :deep(.el-dropdown .el-button) {
    width: 100%;
    margin: 0;
  }

  .pagination {
    justify-content: stretch;
  }

  .mobile-fee-title {
    display: flex;
    min-width: 0;
    align-items: center;
    gap: 10px;
  }

  .mobile-fee-title strong {
    overflow-wrap: anywhere;
  }

  .fee-card-actions {
    display: grid;
    grid-template-columns: repeat(2, minmax(0, 1fr));
    grid-auto-rows: minmax(44px, auto);
    align-items: stretch;
  }

  .fee-card-actions .el-button {
    width: 100%;
    margin: 0;
  }

  .fee-card-actions > :last-child:nth-child(odd) {
    grid-column: 1 / -1;
  }
}

.action-buttons {
  display: flex;
  align-items: center;
  justify-content: center;
  flex-wrap: wrap;
  gap: 4px 7px;
  white-space: normal;
}

.action-buttons :deep(.el-button + .el-button) {
  margin-left: 0;
}

.export-actions {
  display: flex;
  justify-content: flex-end;
  gap: 8px;
  margin-bottom: 14px;
}

.export-template-tip {
  margin: -6px 0 12px;
  color: #606266;
  font-size: 13px;
  line-height: 1.6;
}

.export-preset-list {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  width: 100%;
  margin-bottom: 18px;
}

.export-preset-list :deep(.el-radio-button) {
  width: 100%;
}

.export-preset-list :deep(.el-radio-button__inner) {
  width: 100%;
}

.export-field-list {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 14px 18px;
}

.export-field-list :deep(.el-checkbox) {
  margin-right: 0;
}

@media (max-width: 640px) {
  .export-field-list {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}
</style>
