<template>
  <section class="data-audit-page">
    <div class="audit-layout">
      <!-- 左侧边栏 -->
      <aside class="audit-sidebar">
        <div class="sidebar-search">
          <input
            v-model="categorySearch"
            class="sidebar-search-input"
            placeholder="请输入科学分类名称"
            @keyup.enter="handleCategorySearch"
          />
          <button class="sidebar-search-btn" @click="handleCategorySearch">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </button>
        </div>

        <ul class="category-list">
          <template v-if="categoryList.length">
            <DatabaseSidebarTreeRow
              v-for="node in categoryList"
              :key="node.id"
              :node="node"
              :active-id="selectedCategory"
              :expanded-ids="categoryExpanded"
              @select="selectCategory"
              @toggle-expand="toggleCategory"
            />
          </template>
          <li v-else class="category-empty">暂无目录数据</li>
        </ul>
      </aside>

      <!-- 主内容区 -->
      <main class="audit-main">
        <!-- 面包屑 -->
        <div class="audit-breadcrumb">
          当前位置：<span class="crumb-link" @click="goHome">首页</span> &gt;
          <span class="crumb-link" @click="goAuditManage">审核管理</span> &gt;
          <span class="crumb-current">数据审核</span>
        </div>

        <!-- 页面标题 -->
        <div class="audit-title">数据审核</div>

        <!-- 搜索筛选区 -->
        <div class="search-filter-area">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">数据集名称</label>
              <input v-model="searchForm.datasetName" class="filter-input" placeholder="请输入数据集名称" />
            </div>
            <div class="filter-item">
              <label class="filter-label">审核状态</label>
              <div class="filter-select" @click.stop="toggleDropdown('auditStatus')">
                <span class="filter-select-text">{{ searchForm.auditStatus || '请选择审核状态' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.auditStatus" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '')">请选择审核状态</li>
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '待审核')">待审核</li>
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '已通过')">已通过</li>
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '已驳回')">已驳回</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input v-model="searchForm.templateName" class="filter-input" placeholder="请输入模板名称" />
            </div>
            <div class="filter-item">
              <label class="filter-label">所属部门</label>
              <div class="filter-select" @click.stop="toggleDropdown('department')">
                <span class="filter-select-text">{{ searchForm.department || '请选择部门' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.department" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('department', '')">请选择部门</li>
                  <li class="filter-select-item" @click.stop="selectOption('department', '节点管理部')">节点管理部</li>
                  <li class="filter-select-item" @click.stop="selectOption('department', '社会大众')">社会大众</li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-row second-row">
            <div class="filter-item">
              <label class="filter-label">数据级别</label>
              <div class="filter-select" @click.stop="toggleDropdown('dataLevel')">
                <span class="filter-select-text">{{ searchForm.dataLevel || '请选择数据级别' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.dataLevel" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '')">请选择数据级别</li>
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '公开')">公开</li>
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '高值')">高值</li>
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '内部')">内部</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">产业分类</label>
              <div class="filter-select" @click.stop="toggleDropdown('industry')">
                <span class="filter-select-text">{{ searchForm.industry || '请选择产业分类' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.industry" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('industry', '')">请选择产业分类</li>
                  <li class="filter-select-item" @click.stop="selectOption('industry', '生物医用材料制造')">生物医用材料制造</li>
                  <li class="filter-select-item" @click.stop="selectOption('industry', '先进制造基础零部件')">先进制造基础零部件</li>
                </ul>
              </div>
            </div>
            <div class="filter-buttons">
              <button class="filter-btn primary" @click="handleSearch">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <circle cx="11" cy="11" r="8"></circle>
                  <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
                </svg>
                查询
              </button>
              <button class="filter-btn ghost" @click="handleReset">重置</button>
            </div>
          </div>
        </div>

        <!-- 批量操作区 -->
        <div class="batch-action-area">
          <div class="batch-left">
            <label class="checkbox-label">
              <input type="checkbox" v-model="selectAll" />
              <span>全选</span>
            </label>
            <button type="button" class="batch-btn" @click="handleCancelSelect">取消全选</button>
            <span class="selected-count">已选：{{ selectedCount }}</span>
            <button type="button" class="batch-audit-btn" @click="handleBatchAudit">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              批量审核
            </button>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-checkbox"></th>
                <th class="col-index">序号</th>
                <th class="col-dataset">数据集名称</th>
                <th class="col-level">数据级别</th>
                <th class="col-product">已上传记录数</th>
                <th class="col-scientific">审核状态</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody v-if="loading">
              <tr>
                <td colspan="7" class="table-empty">加载中...</td>
              </tr>
            </tbody>
            <tbody v-else-if="tableData.length === 0">
              <tr>
                <td colspan="7" class="table-empty">暂无数据</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr v-for="(item, index) in tableData" :key="item.id">
                <td class="col-checkbox">
                  <input type="checkbox" v-model="item.selected" />
                </td>
                <td class="col-index">{{ (currentPage - 1) * pageSize + index + 1 }}</td>
                <td class="col-dataset" :title="item.datasetName">{{ item.datasetName }}</td>
                <td class="col-level">
                  <span :class="['level-badge', getLevelClass(item.dataLevel)]">
                    {{ dataLevelText(item.dataLevel) }}
                  </span>
                </td>
                <td class="col-product">{{ item.recordCount ?? 0 }}</td>
                <td class="col-scientific">
                  <span :class="['tag-badge', auditStatusClass(item.auditStatus)]">{{ auditStatusText(item.auditStatus) }}</span>
                </td>
                <td class="col-action">
                  <div class="action-icons">
                    <button class="icon-btn view" title="查看" @click="handleView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>
                    <button class="icon-btn audit" title="通过" @click="handleAudit(item, 1)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="20 6 9 17 4 12"></polyline>
                      </svg>
                    </button>
                    <button class="icon-btn delete" title="驳回" @click="handleAudit(item, 2)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <line x1="18" y1="6" x2="6" y2="18"></line>
                        <line x1="6" y1="6" x2="18" y2="18"></line>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>

        <!-- 分页 -->
        <div class="pagination">
          <span class="total">共 {{ total }} 条</span>
          <button class="page-btn" :disabled="currentPage === 1" @click="goToPage(currentPage - 1)">上一页</button>
          <span class="page-current">{{ currentPage }}</span>
          <button class="page-btn" :disabled="currentPage >= totalPages" @click="goToPage(currentPage + 1)">下一页</button>
        </div>
      </main>
    </div>

    <div v-if="detailVisible" class="detail-modal-mask" @click.self="closeDetailModal">
      <div class="detail-modal">
        <div class="detail-modal-header">
          <div class="detail-modal-title">数据集详情</div>
          <button type="button" class="detail-modal-close" @click="closeDetailModal">×</button>
        </div>
        <div class="detail-modal-body">
          <div class="detail-tabs">
            <button
              type="button"
              :class="['detail-tab', { active: detailTab === 'info' }]"
              @click="detailTab = 'info'"
            >
              详细信息
            </button>
            <button
              type="button"
              :class="['detail-tab', { active: detailTab === 'fields' }]"
              @click="detailTab = 'fields'"
            >
              字段信息
            </button>
          </div>
          <div v-if="detailTab === 'info'" class="detail-grid">
            <div class="detail-item">
              <span class="detail-label">数据集名称</span>
              <span class="detail-value">{{ detailValue('datasetName') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">数据级别</span>
              <span class="detail-value">{{ dataLevelText(detailValue('dataLevel')) }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">数据分类</span>
              <span class="detail-value">{{ detailValue('dataCategory') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">数据条数</span>
              <span class="detail-value">{{ detailValue('dataCount') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">模板名称</span>
              <span class="detail-value">{{ detailValue('templateName') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">所属部门</span>
              <span class="detail-value">{{ detailValue('department') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">创建人</span>
              <span class="detail-value">{{ detailValue('creator') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">创建时间</span>
              <span class="detail-value">{{ detailValue('createTime') }}</span>
            </div>
            <div class="detail-item">
              <span class="detail-label">更新时间</span>
              <span class="detail-value">{{ detailValue('updateTime') }}</span>
            </div>
            <div class="detail-item detail-item-wide">
              <span class="detail-label">科学分类</span>
              <span class="detail-value">{{ formatList(detailData.scienceCategories) }}</span>
            </div>
            <div class="detail-item detail-item-wide">
              <span class="detail-label">产业分类</span>
              <span class="detail-value">{{ formatList(detailData.industryCategories) }}</span>
            </div>
            <div class="detail-item detail-item-wide">
              <span class="detail-label">产品代码</span>
              <span class="detail-value">{{ formatList(detailData.productCodes) }}</span>
            </div>
          </div>
          <div v-else class="field-sections">
            <div v-if="!schemaSections.length" class="field-empty">暂无字段信息</div>
            <div v-for="section in schemaSections" :key="section.id || section.title" class="field-section">
              <div class="field-section-header">
                <div class="field-section-title">{{ section.title || section.id || '未命名分区' }}</div>
                <div v-if="section.subtitle" class="field-section-subtitle">{{ section.subtitle }}</div>
              </div>
              <table class="field-table">
                <thead>
                  <tr>
                    <th>字段名称</th>
                    <th>类型</th>
                    <th>必填</th>
                    <th>提示</th>
                    <th>说明</th>
                    <th>选项</th>
                  </tr>
                </thead>
                <tbody>
                  <tr v-for="field in normalizeFields(section.fields)" :key="field.id || field.label">
                    <td>{{ field.label || field.id || '-' }}</td>
                    <td>{{ field.type || '-' }}</td>
                    <td>{{ field.required ? '是' : '否' }}</td>
                    <td>{{ field.placeholder || '-' }}</td>
                    <td>{{ field.description || '-' }}</td>
                    <td>{{ formatOptions(field.options) }}</td>
                  </tr>
                  <tr v-if="!normalizeFields(section.fields).length">
                    <td colspan="6" class="field-empty-cell">暂无字段</td>
                  </tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <div class="detail-modal-footer">
          <button type="button" class="detail-modal-btn" @click="closeDetailModal">关闭</button>
        </div>
      </div>
    </div>

    <div v-if="messageVisible" class="audit-dialog-mask" @click.self="closeMessage">
      <div class="audit-dialog audit-message-dialog">
        <div class="audit-dialog-header">
          <div class="audit-dialog-title">{{ messageTitle }}</div>
          <button type="button" class="audit-dialog-close" @click="closeMessage">×</button>
        </div>
        <div class="audit-dialog-body">
          <div :class="['audit-dialog-icon', messageType]">
            <svg v-if="messageType === 'success'" width="18" height="18" viewBox="0 0 24 24" fill="none">
              <polyline points="20 6 9 17 4 12" stroke="currentColor" stroke-width="2.2" stroke-linecap="round" stroke-linejoin="round"/>
            </svg>
            <svg v-else width="18" height="18" viewBox="0 0 24 24" fill="none">
              <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="2"/>
              <line x1="12" y1="8" x2="12" y2="13" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <circle cx="12" cy="17" r="1" fill="currentColor"/>
            </svg>
          </div>
          <div class="audit-dialog-text">{{ messageText }}</div>
        </div>
        <div class="audit-dialog-footer">
          <button type="button" class="audit-dialog-btn primary" @click="closeMessage">确定</button>
        </div>
      </div>
    </div>

    <div v-if="confirmVisible" class="audit-dialog-mask">
      <div class="audit-dialog">
        <div class="audit-dialog-header">
          <div class="audit-dialog-title">{{ confirmTitle }}</div>
          <button type="button" class="audit-dialog-close" @click="resolveConfirm(false)">×</button>
        </div>
        <div class="audit-dialog-body">
          <div class="audit-dialog-icon warning">
            <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
              <path d="M12 3L22 20H2L12 3Z" stroke="currentColor" stroke-width="2" stroke-linejoin="round"/>
              <line x1="12" y1="9" x2="12" y2="14" stroke="currentColor" stroke-width="2" stroke-linecap="round"/>
              <circle cx="12" cy="17" r="1" fill="currentColor"/>
            </svg>
          </div>
          <div class="audit-dialog-text">{{ confirmText }}</div>
        </div>
        <div class="audit-dialog-footer">
          <button type="button" class="audit-dialog-btn ghost" @click="resolveConfirm(false)">取消</button>
          <button type="button" class="audit-dialog-btn primary" @click="resolveConfirm(true)">确定</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { fetchDatabaseScienceTree, fetchDatasetOptions } from '../api/database.js'
import DatabaseSidebarTreeRow from './DatabaseSidebarTreeRow.vue'

const emit = defineEmits(['go-home', 'go-audit-manage'])

// 分类搜索
const categorySearch = ref('')
const selectedCategory = ref('all')
const categoryList = ref([])
const categoryExpanded = ref({})
const loading = ref(false)

// 搜索表单
const searchForm = ref({
  datasetName: '',
  auditStatus: '',
  templateName: '',
  department: '',
  dataLevel: '',
  industry: ''
})

// 下拉框状态
const dropdowns = ref({
  auditStatus: false,
  department: false,
  dataLevel: false,
  industry: false
})

const tableData = ref([])
const detailVisible = ref(false)
const detailData = ref({})
const schemaData = ref({})
const detailTab = ref('info')
const messageVisible = ref(false)
const messageTitle = ref('提示')
const messageText = ref('')
const messageType = ref('success')
const confirmVisible = ref(false)
const confirmTitle = ref('确认操作')
const confirmText = ref('')
let confirmResolver = null

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(0)
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

// 全选
const selectAll = computed({
  get: () => tableData.value.length > 0 && tableData.value.every(item => item.selected),
  set: (value) => tableData.value.forEach(item => item.selected = value)
})

// 已选数量
const selectedCount = computed(() => tableData.value.filter(item => item.selected).length)

// 获取数据级别样式
const getLevelClass = (level) => {
  const map = {
    public: 'public',
    highvalue: 'high',
    private: 'internal'
  }
  return map[level] || 'public'
}

function dataLevelText(level) {
  return {
    public: '公开',
    highvalue: '高值',
    private: '内部',
  }[level] || level || '-'
}

function auditStatusText(status) {
  return {
    0: '待审核',
    1: '已通过',
    2: '已驳回',
  }[Number(status)] || '待审核'
}

function auditStatusClass(status) {
  return Number(status) === 1 ? 'blue' : Number(status) === 2 ? 'light-blue' : 'pending'
}

function auditStatusValue(label) {
  return {
    待审核: 0,
    已通过: 1,
    已驳回: 2,
  }[label]
}

function collectExpandedDefaults(nodes) {
  const result = {}
  const walk = (items) => {
    for (const item of items || []) {
      if (item.children?.length) {
        result[String(item.id)] = true
        walk(item.children)
      }
    }
  }
  walk(nodes)
  return result
}

async function loadCategories() {
  categoryList.value = await fetchDatabaseScienceTree({
    keyword: categorySearch.value,
    page: 1,
    pageSize: 500,
  })
  categoryExpanded.value = collectExpandedDefaults(categoryList.value)
}

function selectedScienceCategoryIds() {
  const id = Number(selectedCategory.value)
  return selectedCategory.value !== 'all' && Number.isFinite(id) ? [id] : []
}

async function loadTableData() {
  loading.value = true
  try {
    const { total: t, list } = await fetchDatasetOptions({
      scienceCategoryIds: selectedScienceCategoryIds(),
      keyword: searchForm.value.datasetName,
      auditStatus: auditStatusValue(searchForm.value.auditStatus) ?? -1,
      page: currentPage.value,
      pageSize: pageSize.value,
    })
    const auditFilter = auditStatusValue(searchForm.value.auditStatus)
    const filteredList = list
      .filter((item) => auditFilter == null || Number(item.auditStatus ?? 0) === auditFilter)
      .filter((item) => {
        if (searchForm.value.dataLevel) {
          return dataLevelText(item.dataLevel) === searchForm.value.dataLevel
        }
        return true
      })
    total.value = Math.min(t, filteredList.length)
    tableData.value = filteredList
      .map((item) => ({
        id: item.id,
        datasetName: item.name,
        dataLevel: item.dataLevel,
        auditStatus: item.auditStatus ?? 0,
        recordCount: item.recordCount ?? 0,
        selected: false,
      }))
  } catch (e) {
    console.error('loadTableData', e)
    total.value = 0
    tableData.value = []
    const msg = e instanceof Error ? e.message : String(e)
    if (msg.includes('未登录') || msg.includes('无权限')) showMessage(msg, 'error')
  } finally {
    loading.value = false
  }
}

async function selectCategory(id) {
  selectedCategory.value = id
  currentPage.value = 1
  await loadTableData()
}

async function handleCategorySearch() {
  currentPage.value = 1
  await loadCategories()
  selectedCategory.value = 'all'
  await loadTableData()
}

function goToPage(page) {
  if (page < 1 || page > totalPages.value) return
  currentPage.value = page
  loadTableData()
}

function toggleCategory(id) {
  const key = String(id)
  categoryExpanded.value = {
    ...categoryExpanded.value,
    [key]: categoryExpanded.value[key] !== true,
  }
}

// 下拉框控制
const toggleDropdown = (name) => {
  Object.keys(dropdowns.value).forEach(key => {
    if (key !== name) dropdowns.value[key] = false
  })
  dropdowns.value[name] = !dropdowns.value[name]
}

const selectOption = (name, value) => {
  searchForm.value[name] = value
  dropdowns.value[name] = false
}

// 操作函数
const handleSearch = () => {
  currentPage.value = 1
  loadTableData()
}

const handleReset = () => {
  searchForm.value = {
    datasetName: '',
    auditStatus: '',
    templateName: '',
    department: '',
    dataLevel: '',
    industry: ''
  }
  currentPage.value = 1
  loadTableData()
}

const handleCancelSelect = () => {
  tableData.value.forEach(item => item.selected = false)
}

const handleBatchAudit = async () => {
  const selected = tableData.value.filter(item => item.selected)
  if (selected.length === 0) {
    showMessage('请先选择要审核的数据', 'error')
    return
  }
  const confirmed = await askConfirm(`确定通过选中的 ${selected.length} 个数据集吗？`, '批量审核')
  if (!confirmed) return
  try {
    await Promise.all(selected.map((item) => auditDataset(item, 1)))
    showMessage(`已通过 ${selected.length} 个数据集`, 'success')
    await loadTableData()
  } catch (e) {
    console.error('handleBatchAudit', e)
    showMessage(e?.message || '批量审核失败', 'error')
  }
}

const handleView = async (item) => {
  try {
    const [detailJson, schemaJson] = await Promise.all([
      fetchDatasetDetail(item.id),
      fetchDatasetSchema(item.id),
    ])
    console.log('数据集详情:', detailJson.data)
    console.log('字段信息:', schemaJson.data)
    detailData.value = detailJson.data || {}
    schemaData.value = schemaJson.data || {}
    detailTab.value = 'info'
    detailVisible.value = true
  } catch (e) {
    console.error('handleView', e)
    showMessage(e?.message || '获取数据集详情失败', 'error')
  }
}

async function fetchDatasetDetail(id) {
  const resp = await fetch(`/database/datasets/${encodeURIComponent(String(id))}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || (json.code !== 0 && json.code !== 200)) {
    throw new Error(json?.message || '获取数据集详情失败')
  }
  return json
}

async function fetchDatasetSchema(id) {
  const resp = await fetch('/api/data/online/schema', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({ datasetId: Number(id) }),
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || (json.code !== 0 && json.code !== 200)) {
    throw new Error(json?.message || '获取字段信息失败')
  }
  return json
}

function closeDetailModal() {
  detailVisible.value = false
}

const schemaSections = computed(() => {
  const sections = schemaData.value?.sections
  return Array.isArray(sections) ? sections : []
})

function detailValue(key) {
  const value = detailData.value?.[key]
  return value == null || value === '' ? '-' : value
}

function formatList(value) {
  if (Array.isArray(value)) return value.length ? value.join(' / ') : '-'
  return value == null || value === '' ? '-' : String(value)
}

function normalizeFields(fields) {
  return Array.isArray(fields) ? fields : []
}

function formatOptions(options) {
  if (!Array.isArray(options) || options.length === 0) return '-'
  return options
    .map((opt) => opt?.label || opt?.value || '')
    .filter(Boolean)
    .join(' / ') || '-'
}

async function auditDataset(item, status) {
  const resp = await fetch('/Dataset/audit-template', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({
      datasetName: item.datasetName,
      status: status === 2 ? 0 : status,
      remark: status === 1 ? '审核通过' : '审核驳回',
      auditor: getCurrentAuditor(),
    }),
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || (json.code !== 0 && json.code !== 200)) {
    throw new Error(json?.message || '审核失败')
  }
  return json
}

const handleAudit = async (item, status) => {
  const action = status === 1 ? '通过' : '驳回'
  const confirmed = await askConfirm(`确定${action}「${item.datasetName}」吗？`, `${action}数据集`)
  if (!confirmed) return
  try {
    await auditDataset(item, status)
    showMessage(`已${action}「${item.datasetName}」`, 'success')
    await loadTableData()
  } catch (e) {
    console.error('handleAudit', e)
    showMessage(e?.message || '审核失败', 'error')
  }
}

function showMessage(text, type = 'success', title = '提示') {
  messageText.value = text
  messageType.value = type
  messageTitle.value = title
  messageVisible.value = true
}

function closeMessage() {
  messageVisible.value = false
}

function askConfirm(text, title = '确认操作') {
  confirmText.value = text
  confirmTitle.value = title
  confirmVisible.value = true
  return new Promise((resolve) => {
    confirmResolver = resolve
  })
}

function resolveConfirm(value) {
  confirmVisible.value = false
  if (confirmResolver) {
    confirmResolver(value)
    confirmResolver = null
  }
}

function getAuthHeader() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function getCurrentAuditor() {
  return localStorage.getItem('username') || sessionStorage.getItem('username') || 'admin'
}

const goHome = () => {
  emit('go-home')
}

const goAuditManage = () => {
  emit('go-audit-manage')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
    Object.keys(dropdowns.value).forEach(key => {
      dropdowns.value[key] = false
    })
  }
}

onMounted(() => {
  loadCategories().then(loadTableData)
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.data-audit-page {
  background: #f0f2f5;
  min-height: calc(100vh - 110px);
  padding: 16px 20px 20px;
}

.audit-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: start;
}

/* 左侧边栏 */
.audit-sidebar {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.sidebar-search {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.sidebar-search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 13px;
}

.sidebar-search-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.sidebar-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
}

.sidebar-search-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-item {
  padding: 10px 12px;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-item:hover {
  background: #f5f7fb;
  color: #1a5ce6;
}

.category-item.active {
  background: #e6f7ff;
  color: #1a5ce6;
  font-weight: 500;
}

.category-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  transition: transform 0.2s;
}

.category-item.expanded .category-arrow {
  transform: rotate(90deg);
}

.category-name {
  flex: 1;
}

.subcategory-list {
  list-style: none;
  padding: 0;
  margin: 4px 0 0 22px;
}

.subcategory-item {
  padding: 8px 12px;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.subcategory-item:hover {
  color: #1a5ce6;
}

.subcategory-item.active {
  color: #1a5ce6;
  font-weight: 500;
}

/* 主内容区 */
.audit-main {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.audit-breadcrumb {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
}

.crumb-link {
  color: #1a5ce6;
  cursor: pointer;
}

.crumb-link:hover {
  text-decoration: underline;
}

.crumb-current {
  color: #333;
}

.audit-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

/* 搜索筛选区 */
.search-filter-area {
  background: #f8f9fc;
  border-radius: 6px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-row.second-row {
  margin-top: 12px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.filter-input {
  width: 160px;
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 13px;
  background: #fff;
}

.filter-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  background: #fff;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  min-width: 140px;
}

.filter-select-text {
  flex: 1;
  color: #666;
}

.filter-select-caret {
  margin-left: 4px;
  color: #999;
}

.filter-select-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  min-width: 160px;
  padding: 4px 0;
  margin: 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 10;
}

.filter-select-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.filter-select-item:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.filter-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.filter-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
}

.filter-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.filter-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

/* 批量操作区 */
.batch-action-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #e8ecf4;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}

.checkbox-label input {
  width: 14px;
  height: 14px;
  accent-color: #1a5ce6;
}

.batch-btn {
  padding: 4px 12px;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
}

.batch-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.selected-count {
  font-size: 13px;
  color: #666;
}

.batch-audit-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  background: #1a5ce6;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.batch-audit-btn:hover {
  background: #1246bb;
}

/* 表格 */
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
  padding: 12px 10px;
  text-align: left;
  border-bottom: 1px solid #e8ecf4;
}

.data-table th {
  background: #f5f7fb;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
}

/* 列宽 */
.col-checkbox {
  width: 40px;
  text-align: center;
}

.col-index {
  width: 50px;
  text-align: center;
}

.col-dataset {
  min-width: 200px;
  max-width: 300px;
}

.col-scientific,
.col-industry {
  min-width: 120px;
}

.col-product {
  width: 100px;
}

.col-level {
  width: 80px;
  text-align: center;
}

.col-action {
  width: 100px;
  text-align: center;
}

/* 标签样式 */
.tag-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.tag-badge.blue {
  color: #1a5ce6;
  background: #e8f0fe;
}

.tag-badge.light-blue {
  color: #1a5ce6;
  background: #e6f7ff;
}

.level-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.level-badge.public {
  color: #67c23a;
  background: #f0f9eb;
}

.level-badge.high {
  color: #f56c6c;
  background: #fef0f0;
}

.level-badge.internal {
  color: #909399;
  background: #f4f4f5;
}

/* 操作图标 */
.action-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.icon-btn.audit:hover {
  background: #f0f9eb;
  color: #67c23a;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8ecf4;
}

.total {
  font-size: 13px;
  color: #666;
}

.page-btn {
  padding: 6px 12px;
  font-size: 13px;
  color: #666;
  background: #fff;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  cursor: pointer;
  transition: all 0.2s;
}

.page-btn:hover:not(:disabled) {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.page-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-current {
  padding: 6px 12px;
  font-size: 13px;
  color: #fff;
  background: #1a5ce6;
  border-radius: 4px;
  min-width: 32px;
  text-align: center;
}

.detail-modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.42);
}

.detail-modal {
  width: 720px;
  max-width: calc(100vw - 32px);
  max-height: calc(100vh - 80px);
  display: flex;
  flex-direction: column;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 16px 48px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.detail-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf4;
}

.detail-modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #1f2d3d;
}

.detail-modal-close {
  border: none;
  background: transparent;
  font-size: 24px;
  line-height: 1;
  color: #666;
  cursor: pointer;
}

.detail-modal-body {
  padding: 20px;
  overflow-y: auto;
}

.detail-tabs {
  display: flex;
  gap: 8px;
  margin-bottom: 18px;
  border-bottom: 1px solid #e8ecf4;
}

.detail-tab {
  padding: 8px 14px;
  border: none;
  border-bottom: 2px solid transparent;
  background: transparent;
  color: #666;
  font-size: 13px;
  cursor: pointer;
}

.detail-tab.active {
  color: #1a5ce6;
  border-bottom-color: #1a5ce6;
  font-weight: 600;
}

.detail-grid {
  display: grid;
  grid-template-columns: repeat(2, minmax(0, 1fr));
  gap: 14px 18px;
}

.detail-item {
  display: flex;
  min-width: 0;
  gap: 10px;
}

.detail-item-wide {
  grid-column: 1 / -1;
}

.detail-label {
  flex: 0 0 86px;
  color: #666;
  font-size: 13px;
}

.detail-value {
  min-width: 0;
  color: #1f2d3d;
  font-size: 13px;
  word-break: break-word;
}

.detail-modal-footer {
  display: flex;
  justify-content: flex-end;
  padding: 14px 20px;
  border-top: 1px solid #e8ecf4;
}

.detail-modal-btn {
  padding: 8px 20px;
  border: 1px solid #1a5ce6;
  border-radius: 4px;
  background: #1a5ce6;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
}

.field-sections {
  display: flex;
  flex-direction: column;
  gap: 18px;
}

.field-empty {
  padding: 36px 0;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.field-section {
  border: 1px solid #e8ecf4;
  border-radius: 6px;
  overflow: hidden;
}

.field-section-header {
  padding: 12px 14px;
  background: #f8f9fc;
  border-bottom: 1px solid #e8ecf4;
}

.field-section-title {
  font-size: 14px;
  font-weight: 600;
  color: #1f2d3d;
}

.field-section-subtitle {
  margin-top: 4px;
  font-size: 12px;
  color: #666;
}

.field-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 12px;
}

.field-table th,
.field-table td {
  padding: 10px 8px;
  border-bottom: 1px solid #eef1f6;
  text-align: left;
  vertical-align: top;
}

.field-table th {
  background: #fbfcff;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
}

.field-table tr:last-child td {
  border-bottom: none;
}

.field-empty-cell {
  text-align: center;
  color: #999;
}

.audit-dialog-mask {
  position: fixed;
  inset: 0;
  z-index: 1100;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.36);
}

.audit-dialog {
  width: 420px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 18px 48px rgba(15, 23, 42, 0.18);
  overflow: hidden;
}

.audit-message-dialog {
  width: 390px;
}

.audit-dialog-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 15px 18px;
  border-bottom: 1px solid #e8ecf4;
}

.audit-dialog-title {
  font-size: 15px;
  font-weight: 600;
  color: #1f2d3d;
}

.audit-dialog-close {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  color: #8c95a6;
  cursor: pointer;
}

.audit-dialog-close:hover {
  color: #1f2d3d;
}

.audit-dialog-body {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  padding: 22px 20px;
}

.audit-dialog-icon {
  flex: 0 0 32px;
  width: 32px;
  height: 32px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  border-radius: 50%;
}

.audit-dialog-icon.success {
  color: #2f9e44;
  background: #edf9f0;
}

.audit-dialog-icon.error {
  color: #d93025;
  background: #fff1f0;
}

.audit-dialog-icon.warning {
  color: #d48806;
  background: #fff7e6;
}

.audit-dialog-text {
  flex: 1;
  min-width: 0;
  padding-top: 5px;
  color: #1f2d3d;
  font-size: 14px;
  line-height: 1.6;
  word-break: break-word;
}

.audit-dialog-footer {
  display: flex;
  justify-content: flex-end;
  gap: 10px;
  padding: 14px 18px 18px;
}

.audit-dialog-btn {
  min-width: 74px;
  padding: 8px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #d4dae6;
}

.audit-dialog-btn.ghost {
  background: #fff;
  color: #333;
}

.audit-dialog-btn.ghost:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.audit-dialog-btn.primary {
  border-color: #1a5ce6;
  background: #1a5ce6;
  color: #fff;
}

.audit-dialog-btn.primary:hover {
  background: #1246bb;
  border-color: #1246bb;
}
</style>
