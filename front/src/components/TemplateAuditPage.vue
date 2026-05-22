<template>
  <section class="template-audit-page">
    <!-- 面包屑导航 -->
    <div class="audit-header">
      <div class="audit-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-parent">审核管理</span> &gt;
        <span class="crumb-now">模板审核</span>
      </div>
    </div>

    <div class="audit-layout">
      <!-- 左侧分类树 -->
      <aside class="audit-sidebar">
        <div class="sidebar-search">
          <input class="sidebar-input" placeholder="请输入数据资源目录名" />
          <button class="sidebar-search-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
          </button>
        </div>
        <div class="sidebar-menu-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="3" y1="12" x2="21" y2="12"/>
            <line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </div>
        <ul class="sidebar-list">
          <li class="sidebar-item active">
            <span class="item-text">全部</span>
          </li>
          <li class="sidebar-item">
            <span class="item-text">生物医用材料（科学）-通用</span>
          </li>
          <li class="sidebar-item">
            <span class="item-text">生物医用材料（产业）-通用</span>
          </li>
        </ul>
      </aside>

      <!-- 右侧主内容区 -->
      <section class="audit-main">
        <div class="main-title">模板审核</div>

        <!-- 搜索区域 -->
        <div class="search-area">
          <div class="search-item">
            <label class="search-label">模板名称</label>
            <input
              v-model="searchForm.name"
              class="search-input"
              placeholder="请输入模版名称"
            />
          </div>
          <div class="search-item">
            <label class="search-label">审核状态</label>
            <div class="search-select" @click.stop="toggleStatusDropdown">
              <span class="search-select-text">
                {{ searchForm.status || '请选择状态' }}
              </span>
              <svg class="search-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="statusDropdownVisible" class="search-select-dropdown">
                <li class="search-select-item" @click.stop="selectStatus('')">请选择状态</li>
                <li class="search-select-item" @click.stop="selectStatus('待审核')">待审核</li>
                <li class="search-select-item" @click.stop="selectStatus('已通过')">已通过</li>
              </ul>
            </div>
          </div>
          <button class="search-btn primary" @click="handleSearch">查询</button>
          <button class="search-btn ghost" @click="handleReset">重置</button>
        </div>

        <!-- 批量操作区域 -->
        <div class="batch-area">
          <label class="batch-checkbox">
            <input type="checkbox" v-model="selectAll" @change="handleSelectAll" />
            <span>全选</span>
          </label>
          <button class="batch-btn" @click="cancelSelectAll">取消全选</button>
          <span class="batch-info">已选：<span class="batch-count">{{ selectedCount }}</span></span>
          <button class="batch-btn primary" :disabled="batchAuditing" @click="handleBatchAudit">批量审核</button>
        </div>

        <!-- 表格区域 -->
        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-checkbox"></th>
                <th class="col-index">序号</th>
                <th class="col-name">模板名称</th>
                <th class="col-tag">模板标签</th>
                <th class="col-status">审核状态</th>
                <th class="col-time">最近审核时间</th>
                <th class="col-creator">创建人</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in tableData" :key="item.id">
                <td class="col-checkbox">
                  <input type="checkbox" v-model="item.selected" />
                </td>
                <td class="col-index">{{ (page - 1) * pageSize + index + 1 }}</td>
                <td class="col-name">{{ item.name }}</td>
                <td class="col-tag">
                  <span class="tag-badge">{{ item.tag }}</span>
                </td>
                <td class="col-status">
                  <span class="status-badge" :class="item.statusKind || 'pending'">
                    {{ item.status }}
                  </span>
                </td>
                <td class="col-time">{{ item.auditTime || '-' }}</td>
                <td class="col-creator">{{ item.creator }}</td>
                <td class="col-action">
                  <div class="action-btns">
                    <button class="action-btn" title="查看" @click="handleView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>
                    <button class="action-btn" title="编辑" @click="handleEdit(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </button>
                    <button class="action-btn" title="删除" @click="handleDelete(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- 分页 -->
          <div class="pagination">
            <span class="pagination-total">共 {{ total }} 条</span>
            <div class="pagination-nav">
              <button
                class="pagination-btn"
                :disabled="page <= 1"
                @click="goToPage(page - 1)"
              >
                上一页
              </button>
              <div class="pagination-pages">
                <button
                  v-for="p in displayedPages"
                  :key="p"
                  class="page-number"
                  :class="{ active: p === page }"
                  @click="goToPage(p)"
                >
                  {{ p }}
                </button>
              </div>
              <button
                class="pagination-btn"
                :disabled="page >= totalPages"
                @click="goToPage(page + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>

    <div v-if="detailDialogVisible" class="detail-mask" @click.self="closeDetailDialog">
      <div class="detail-dialog">
        <div class="detail-dialog-header">
          <h2 class="detail-dialog-title">查看模板</h2>
          <button type="button" class="detail-close-btn" @click="closeDetailDialog">×</button>
        </div>

        <div class="detail-switch">
          <button
            type="button"
            class="detail-switch-btn"
            :class="{ active: detailActiveTab === 'base' }"
            @click="detailActiveTab = 'base'"
          >
            基本信息
          </button>
          <button
            type="button"
            class="detail-switch-btn"
            :class="{ active: detailActiveTab === 'fields' }"
            @click="detailActiveTab = 'fields'"
          >
            字段信息
          </button>
        </div>

        <div v-if="detailLoading" class="detail-loading">加载中...</div>
        <template v-else>
          <section v-if="detailActiveTab === 'base'" class="detail-section">
            <h3 class="detail-section-title">模板基本信息</h3>
            <div class="base-info-grid">
              <div class="base-info-item">
                <span class="base-info-label">模板名称</span>
                <span class="base-info-value">{{ moduleBaseInfo.moduleName || '-' }}</span>
              </div>
              <div class="base-info-item">
                <span class="base-info-label">模板标签</span>
                <span class="base-info-value">{{ moduleBaseInfo.tag || '-' }}</span>
              </div>
              <div class="base-info-item">
                <span class="base-info-label">创建人</span>
                <span class="base-info-value">{{ moduleBaseInfo.creator ?? '-' }}</span>
              </div>
              <div class="base-info-item">
                <span class="base-info-label">可见范围</span>
                <span class="base-info-value">{{ formatVisibleArea(moduleBaseInfo.visibleArea) }}</span>
              </div>
              <div class="base-info-item">
                <span class="base-info-label">是否发布</span>
                <span class="base-info-value">{{ formatAgree(moduleBaseInfo.agree) }}</span>
              </div>
              <div class="base-info-item">
                <span class="base-info-label">审核状态</span>
                <span class="base-info-value">{{ formatAuditState(moduleBaseInfo.auditState) }}</span>
              </div>
              <div class="base-info-item">
                <span class="base-info-label">创建时间</span>
                <span class="base-info-value">{{ formatDateTime(moduleBaseInfo.createTime) || '-' }}</span>
              </div>
              <div class="base-info-item base-info-item-wide">
                <span class="base-info-label">模板说明</span>
                <span class="base-info-value">{{ moduleBaseInfo.description || '-' }}</span>
              </div>
            </div>
          </section>

          <section v-else class="detail-section">
            <h3 class="detail-section-title">模板字段信息</h3>
            <div class="field-groups">
              <div v-for="group in fieldGroups" :key="group.key" class="field-group">
                <div class="field-group-title">{{ group.title }}</div>
                <table class="field-table">
                  <thead>
                    <tr>
                      <th>序号</th>
                      <th>字段名称</th>
                      <th>字段类型</th>
                    </tr>
                  </thead>
                  <tbody>
                    <tr v-if="group.items.length === 0">
                      <td colspan="3" class="field-empty">暂无字段</td>
                    </tr>
                    <tr v-for="(field, index) in group.items" :key="`${group.key}-${index}`">
                      <td>{{ index + 1 }}</td>
                      <td>{{ field.columnName || '-' }}</td>
                      <td>{{ field.type || '-' }}</td>
                    </tr>
                  </tbody>
                </table>
              </div>
            </div>
          </section>
        </template>
      </div>
    </div>

    <div v-if="batchAuditDialogVisible" class="audit-confirm-mask" @click.self="closeBatchAuditDialog">
      <div class="audit-confirm-dialog">
        <div class="audit-confirm-title">批量审核</div>
        <div class="audit-confirm-message">
          确定审核通过已选择的 {{ batchAuditItems.length }} 个模板吗？
        </div>
        <div class="audit-confirm-actions">
          <button type="button" class="audit-confirm-btn ghost" :disabled="batchAuditing" @click="closeBatchAuditDialog">
            取消
          </button>
          <button type="button" class="audit-confirm-btn primary" :disabled="batchAuditing" @click="confirmBatchAudit">
            {{ batchAuditing ? '审核中...' : '确定' }}
          </button>
        </div>
      </div>
    </div>

    <div v-if="noticeDialogVisible" class="audit-confirm-mask" @click.self="closeNoticeDialog">
      <div class="audit-confirm-dialog audit-notice-dialog">
        <div class="audit-confirm-title">{{ noticeTitle }}</div>
        <div class="audit-confirm-message">{{ noticeMessage }}</div>
        <div class="audit-confirm-actions">
          <button type="button" class="audit-confirm-btn primary" @click="closeNoticeDialog">
            确定
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { auditModule, getModuleBaseInfo, getModuleDetailInfo, getPendingAuditModules } from '../api/module.js'

const emit = defineEmits(['go-home'])

// 搜索表单
const searchForm = ref({
  name: '',
  status: ''
})

// 下拉框状态
const statusDropdownVisible = ref(false)

// 分页相关
const page = ref(1)
const pageSize = ref(10)
const total = ref(0)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 显示的页码
const displayedPages = computed(() => {
  const pages = []
  const maxDisplay = 5
  let start = Math.max(1, page.value - 2)
  let end = Math.min(totalPages.value, start + maxDisplay - 1)

  if (end - start < maxDisplay - 1) {
    start = Math.max(1, end - maxDisplay + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

const tableData = ref([])
const loading = ref(false)
const batchAuditing = ref(false)
const batchAuditDialogVisible = ref(false)
const batchAuditItems = ref([])
const noticeDialogVisible = ref(false)
const noticeTitle = ref('提示')
const noticeMessage = ref('')
const detailDialogVisible = ref(false)
const detailLoading = ref(false)
const detailActiveTab = ref('base')
const moduleBaseInfo = ref({})
const moduleDetailInfo = ref({
  object: [],
  operation: [],
  result: [],
})

const fieldGroups = computed(() => [
  {
    key: 'object',
    title: 'Object 字段',
    items: Array.isArray(moduleDetailInfo.value.object) ? moduleDetailInfo.value.object : [],
  },
  {
    key: 'operation',
    title: 'Operation 字段',
    items: Array.isArray(moduleDetailInfo.value.operation) ? moduleDetailInfo.value.operation : [],
  },
  {
    key: 'result',
    title: 'Result 字段',
    items: Array.isArray(moduleDetailInfo.value.result) ? moduleDetailInfo.value.result : [],
  },
])

function formatDateTime(value) {
  if (!value) return ''
  return String(value).replace('T', ' ').slice(0, 19)
}

function formatVisibleArea(value) {
  if (Number(value) === 1) return 'public'
  if (Number(value) === 0) return 'private'
  return '-'
}

function formatAgree(value) {
  if (Number(value) === 1) return '是'
  if (Number(value) === 0) return '否'
  return '-'
}

function formatAuditState(value) {
  if (Number(value) === 0) return '待审核'
  if (Number(value) === 1) return '驳回'
  if (Number(value) === 2) return '通过'
  return '-'
}

function normalizePendingAuditModule(raw) {
  return {
    id: raw?.id,
    name: raw?.moduleName != null ? String(raw.moduleName) : '',
    tag: raw?.tag != null ? String(raw.tag) : '',
    status: '待审核',
    statusKind: 'pending',
    auditTime: '',
    creator: raw?.creator != null ? String(raw.creator) : '',
    description: raw?.description != null ? String(raw.description) : '',
    createTime: raw?.createTime != null ? String(raw.createTime) : '',
    selected: false,
  }
}

async function loadPendingAuditModules() {
  loading.value = true
  try {
    const list = await getPendingAuditModules()
    tableData.value = list.map(normalizePendingAuditModule)
    total.value = tableData.value.length
    selectAll.value = false
  } catch (e) {
    tableData.value = []
    total.value = 0
    alert(e?.message || '获取待审核模板列表失败')
  } finally {
    loading.value = false
  }
}

// 全选相关
const selectAll = ref(false)
const selectedCount = computed(() => tableData.value.filter(item => item.selected).length)

const handleSelectAll = () => {
  tableData.value.forEach(item => item.selected = selectAll.value)
}

const cancelSelectAll = () => {
  selectAll.value = false
  tableData.value.forEach(item => item.selected = false)
}

// 下拉框控制
const toggleStatusDropdown = () => {
  statusDropdownVisible.value = !statusDropdownVisible.value
}

const selectStatus = (status) => {
  searchForm.value.status = status
  statusDropdownVisible.value = false
}

// 搜索和重置
const handleSearch = () => {
  page.value = 1
}

const handleReset = () => {
  searchForm.value = { name: '', status: '' }
  page.value = 1
}

// 分页控制
const goToPage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
}

// 批量操作
function showNotice(message, title = '提示') {
  noticeTitle.value = title
  noticeMessage.value = message
  noticeDialogVisible.value = true
}

function closeNoticeDialog() {
  noticeDialogVisible.value = false
}

const handleBatchAudit = () => {
  const selected = tableData.value.filter(item => item.selected)
  if (selected.length === 0) {
    showNotice('请先选择要审核的模板')
    return
  }

  batchAuditItems.value = selected
  batchAuditDialogVisible.value = true
}

function closeBatchAuditDialog() {
  if (batchAuditing.value) return
  batchAuditDialogVisible.value = false
}

const confirmBatchAudit = async () => {
  batchAuditing.value = true
  try {
    await Promise.all(batchAuditItems.value.map(item => auditModule(item.id, 2)))
    batchAuditDialogVisible.value = false
    showNotice('批量审核通过成功')
    await loadPendingAuditModules()
  } catch (e) {
    showNotice(e?.message || '批量审核失败')
  } finally {
    batchAuditing.value = false
    batchAuditItems.value = []
  }
}

// 操作按钮
const handleView = async (item) => {
  if (!item?.id) {
    alert('模板ID缺失，无法查看')
    return
  }

  detailDialogVisible.value = true
  detailLoading.value = true
  detailActiveTab.value = 'base'
  moduleBaseInfo.value = {}
  moduleDetailInfo.value = { object: [], operation: [], result: [] }

  try {
    const [baseInfo, detailInfo] = await Promise.all([
      getModuleBaseInfo(item.id),
      getModuleDetailInfo(item.id),
    ])
    moduleBaseInfo.value = baseInfo || {}
    moduleDetailInfo.value = {
      object: Array.isArray(detailInfo?.object) ? detailInfo.object : [],
      operation: Array.isArray(detailInfo?.operation) ? detailInfo.operation : [],
      result: Array.isArray(detailInfo?.result) ? detailInfo.result : [],
    }
  } catch (e) {
    detailDialogVisible.value = false
    alert(e?.message || '获取模板详情失败')
  } finally {
    detailLoading.value = false
  }
}

const closeDetailDialog = () => {
  detailDialogVisible.value = false
}

const handleEdit = (item) => {
  console.log('编辑：', item)
}

const handleDelete = (item) => {
  console.log('删除：', item)
}

const goHome = () => {
  emit('go-home')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.search-select')) {
    statusDropdownVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadPendingAuditModules()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.template-audit-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.audit-header {
  font-size: 13px;
  color: #666;
}

.crumb-main {
  color: #1a5ce6;
  cursor: pointer;
}

.crumb-main:hover {
  text-decoration: underline;
}

.crumb-parent {
  color: #333;
}

.crumb-now {
  color: #333;
  font-weight: 500;
}

.audit-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: stretch;
}

/* 左侧边栏 */
.audit-sidebar {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.sidebar-input {
  flex: 1;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.sidebar-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
}

.sidebar-menu-btn {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 8px;
  color: #666;
  cursor: pointer;
}

.sidebar-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar-item {
  padding: 8px 0;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-item.active {
  color: #1a5ce6;
  font-weight: 500;
}

.sidebar-item:last-child {
  border-bottom: none;
}

.item-text {
  line-height: 1.4;
}

/* 右侧主内容区 */
.audit-main {
  background: #fff;
  border-radius: 4px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.main-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

/* 搜索区域 */
.search-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.search-input {
  width: 140px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.search-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  min-width: 120px;
}

.search-select-text {
  flex: 1;
}

.search-select-caret {
  margin-left: 4px;
  color: #999;
}

.search-select-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  min-width: 120px;
  padding: 4px 0;
  margin: 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 10;
}

.search-select-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.search-select-item:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.search-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
}

.search-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.search-btn.primary:hover {
  background: #1246bb;
}

.search-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

.search-btn.ghost:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

/* 批量操作区域 */
.batch-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.batch-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.batch-btn {
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
}

.batch-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.batch-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.batch-btn.primary:hover {
  background: #1246bb;
}

.batch-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.audit-confirm-mask {
  position: fixed;
  inset: 0;
  z-index: 1200;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
  background: rgba(15, 23, 42, 0.32);
}

.audit-confirm-dialog {
  width: min(420px, calc(100vw - 48px));
  padding: 22px 24px 20px;
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 18px 50px rgba(15, 23, 42, 0.22);
}

.audit-notice-dialog {
  width: min(360px, calc(100vw - 48px));
}

.audit-confirm-title {
  margin-bottom: 14px;
  color: #1f2937;
  font-size: 17px;
  font-weight: 600;
}

.audit-confirm-message {
  min-height: 48px;
  color: #333;
  font-size: 14px;
  line-height: 1.7;
}

.audit-confirm-actions {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  margin-top: 18px;
}

.audit-confirm-btn {
  min-width: 72px;
  height: 34px;
  padding: 0 16px;
  border-radius: 4px;
  font-size: 14px;
  cursor: pointer;
}

.audit-confirm-btn.ghost {
  border: 1px solid #d4dae6;
  background: #fff;
  color: #555;
}

.audit-confirm-btn.primary {
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
}

.audit-confirm-btn:disabled {
  cursor: not-allowed;
  opacity: 0.65;
}

.batch-info {
  font-size: 13px;
  color: #666;
}

.batch-count {
  color: #1a5ce6;
  font-weight: 500;
}

/* 表格区域 */
.table-area {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.data-table th,
.data-table td {
  padding: 12px 8px;
  text-align: left;
  border-bottom: 1px solid #f0f0f0;
}

.data-table th {
  background: #fafafa;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
}

.data-table tbody tr:hover {
  background: #fafafa;
}

.col-checkbox {
  width: 40px;
  text-align: center;
}

.col-index {
  width: 50px;
  text-align: center;
}

.col-name {
  min-width: 180px;
  max-width: 220px;
}

.col-tag {
  min-width: 140px;
}

.col-status {
  min-width: 80px;
}

.col-time {
  min-width: 140px;
}

.col-creator {
  min-width: 80px;
}

.col-action {
  min-width: 100px;
  text-align: center;
}

.tag-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #e8f0fe;
  color: #1a5ce6;
  border-radius: 3px;
  font-size: 12px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.status-badge.pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-badge.approved {
  background: #f6ffed;
  color: #52c41a;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s;
}

.action-btn:hover {
  background: #f0f5ff;
}

.action-btn svg {
  display: block;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 0;
  font-size: 13px;
  color: #666;
}

.pagination-total {
  color: #666;
}

.pagination-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  padding: 5px 12px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.pagination-btn:hover:not(:disabled) {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.pagination-pages {
  display: flex;
  align-items: center;
  gap: 4px;
}

.page-number {
  min-width: 28px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  background: #fff;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
}

.page-number:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.page-number.active {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.detail-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}

.detail-dialog {
  width: min(980px, calc(100vw - 48px));
  max-height: calc(100vh - 64px);
  overflow: auto;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.22);
}

.detail-dialog-header {
  position: sticky;
  top: 0;
  z-index: 1;
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf4;
  background: #fff;
}

.detail-dialog-title {
  margin: 0;
  font-size: 18px;
  font-weight: 600;
  color: #222;
}

.detail-close-btn {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #666;
  font-size: 24px;
  line-height: 1;
  cursor: pointer;
}

.detail-close-btn:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.detail-loading {
  padding: 40px 20px;
  text-align: center;
  color: #666;
}

.detail-switch {
  display: inline-flex;
  gap: 0;
  margin: 16px 20px 0;
  padding: 3px;
  border: 1px solid #d4dae6;
  border-radius: 6px;
  background: #f8f9fc;
}

.detail-switch-btn {
  min-width: 92px;
  height: 32px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #555;
  font-size: 13px;
  cursor: pointer;
}

.detail-switch-btn.active {
  background: #1a5ce6;
  color: #fff;
}

.detail-switch-btn:not(.active):hover {
  color: #1a5ce6;
  background: #eef4ff;
}

.detail-section {
  padding: 18px 20px 20px;
  border-bottom: 1px solid #eef1f6;
}

.detail-section:last-child {
  border-bottom: none;
}

.detail-section-title {
  margin: 0 0 14px;
  font-size: 15px;
  font-weight: 600;
  color: #333;
}

.base-info-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 12px 18px;
}

.base-info-item {
  display: grid;
  grid-template-columns: 88px minmax(0, 1fr);
  gap: 10px;
  min-height: 34px;
  align-items: center;
  padding: 8px 10px;
  background: #f8f9fc;
  border-radius: 4px;
}

.base-info-item-wide {
  grid-column: 1 / -1;
}

.base-info-label {
  color: #666;
  font-size: 13px;
}

.base-info-value {
  min-width: 0;
  color: #222;
  font-size: 13px;
  overflow-wrap: anywhere;
}

.field-groups {
  display: grid;
  gap: 16px;
}

.field-group-title {
  margin-bottom: 8px;
  color: #1a5ce6;
  font-size: 14px;
  font-weight: 600;
}

.field-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.field-table th,
.field-table td {
  padding: 10px 12px;
  border: 1px solid #e8ecf4;
  text-align: left;
}

.field-table th {
  background: #f5f7fb;
  color: #333;
  font-weight: 500;
}

.field-empty {
  text-align: center;
  color: #999;
}
</style>
