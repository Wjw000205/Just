<template>
  <section class="data-publish-page">
    <div class="publish-header">
      <div class="publish-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-now">数据发布</span>
      </div>
    </div>

    <div class="publish-layout">
      <aside class="publish-sidebar">
        <div class="sidebar-search-row">
          <div class="sidebar-search">
            <input
              v-model="categoryKeyword"
              class="sidebar-input"
              placeholder="请输入科学分类名称"
            />
            <button type="button" class="sidebar-search-btn" aria-label="搜索" @click="onCategorySearch">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
            </button>
          </div>
          <button type="button" class="sidebar-menu-btn" aria-label="列表">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
        </div>
        <ul class="sidebar-tree">
          <li
            class="tree-node"
            :class="{ active: activeCategory === 'all' }"
            @click="selectCategory('all')"
          >
            <span class="tree-chevron placeholder" aria-hidden="true" />
            <span class="tree-label">全部</span>
          </li>
          <li
            class="tree-node"
            :class="{ active: activeCategory === 'sci', expanded: treeExpanded.sci }"
            @click="toggleTree('sci')"
          >
            <span class="tree-chevron" :class="{ open: treeExpanded.sci }">
              <svg width="8" height="8" viewBox="0 0 10 10" fill="currentColor">
                <path d="M3 1L7 5L3 9V1Z" />
              </svg>
            </span>
            <span class="tree-label">生物医用材料（科学）</span>
          </li>
          <li
            class="tree-node"
            :class="{ active: activeCategory === 'industry', expanded: treeExpanded.industry }"
            @click="toggleTree('industry')"
          >
            <span class="tree-chevron" :class="{ open: treeExpanded.industry }">
              <svg width="8" height="8" viewBox="0 0 10 10" fill="currentColor">
                <path d="M3 1L7 5L3 9V1Z" />
              </svg>
            </span>
            <span class="tree-label">生物医用材料（产业）</span>
          </li>
        </ul>
      </aside>

      <section class="publish-main">
        <div class="main-title">数据发布</div>

        <div class="filter-grid">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">数据集名称</label>
              <input
                v-model="searchForm.datasetName"
                class="filter-input"
                placeholder="请输入数据集名称"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">产业分类</label>
              <div class="filter-select" @click.stop="toggleDropdown('industry')">
                <span :class="['filter-select-text', { placeholder: !searchForm.industry }]">
                  {{ industryLabel || '请选择产业分类' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="dropdownOpen === 'industry'" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickIndustry('')">请选择产业分类</li>
                  <li
                    v-for="opt in industryOptions"
                    :key="opt.value"
                    class="filter-select-item"
                    @click.stop="pickIndustry(opt.value)"
                  >
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <div class="filter-select" @click.stop="toggleDropdown('status')">
                <span :class="['filter-select-text', { placeholder: !searchForm.status }]">
                  {{ auditStatusLabel || '请选择审核状态' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="dropdownOpen === 'status'" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickStatus('')">请选择审核状态</li>
                  <li
                    v-for="opt in auditStatusOptions"
                    :key="opt.value"
                    class="filter-select-item"
                    @click.stop="pickStatus(opt.value)"
                  >
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input
                v-model="searchForm.templateName"
                class="filter-input"
                placeholder="请输入模板名称"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">所属部门</label>
              <div class="filter-select" @click.stop="toggleDropdown('dept')">
                <span :class="['filter-select-text', { placeholder: !searchForm.department }]">
                  {{ departmentLabel || '请选择' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="dropdownOpen === 'dept'" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickDept('')">请选择</li>
                  <li
                    v-for="opt in deptOptions"
                    :key="opt.value"
                    class="filter-select-item"
                    @click.stop="pickDept(opt.value)"
                  >
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">数据级别</label>
              <div class="filter-select" @click.stop="toggleDropdown('level')">
                <span :class="['filter-select-text', { placeholder: !searchForm.dataLevel }]">
                  {{ dataLevelLabel || '请选择数据级别' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="dropdownOpen === 'level'" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickLevel('')">请选择数据级别</li>
                  <li
                    v-for="opt in dataLevelOptions"
                    :key="opt.value"
                    class="filter-select-item"
                    @click.stop="pickLevel(opt.value)"
                  >
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-actions">
            <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
            <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
          </div>
        </div>

        <div class="batch-area">
          <label class="batch-checkbox">
            <input type="checkbox" v-model="selectAll" @change="handleSelectAll" />
            <span>全选</span>
          </label>
          <button type="button" class="batch-link" @click="cancelSelectAll">取消全选</button>
          <span class="batch-info">已选：<span class="batch-count">{{ selectedCount }}</span></span>
          <button type="button" class="batch-btn pre-publish" @click="handlePrePublish">
            <svg class="pre-publish-icon" width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="12" cy="12" r="9" />
              <path d="M12 7v5l3 2" stroke-linecap="round" />
              <path d="M18.36 18.36a9 9 0 0 0 1.5-3.5" stroke-linecap="round" />
            </svg>
            预发布
          </button>
        </div>

        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-checkbox" />
                <th class="col-index">序号</th>
                <th class="col-name">数据集名称</th>
                <th class="col-tags">科学分类</th>
                <th class="col-tags">产业分类</th>
                <th class="col-tags">产品代码</th>
                <th class="col-level">数据级别</th>
                <th class="col-status">状态</th>
                <th class="col-count">数据量</th>
                <th class="col-count">待审核</th>
                <th class="col-count">已通过</th>
                <th class="col-count">未通过</th>
                <th class="col-count">已通过比例</th>
                <th class="col-summary">摘要</th>
                <th class="col-name">模板</th>
                <th class="col-dept">所属部门</th>
                <th class="col-creator">创建人</th>
                <th class="col-time">创建时间</th>
                <th class="col-time">最近跟新时间</th>
                <th class="col-creator">审核人</th>
                <th class="col-time">最近审核时间</th>
                <th class="col-time">预发布时间</th>
                <th class="col-id">数据标识</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody v-if="listLoading">
              <tr>
                <td colspan="24" class="table-hint">加载中…</td>
              </tr>
            </tbody>
            <tbody v-else-if="tableData.length === 0">
              <tr>
                <td colspan="24" class="table-hint muted">暂无数据</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr v-for="(item, index) in tableData" :key="String(item.id ?? index)">
                <td class="col-checkbox">
                  <input v-model="item.selected" type="checkbox" />
                </td>
                <td class="col-index">{{ (page - 1) * pageSize + index + 1 }}</td>
                <td class="col-name">
                  <span class="ellipsis" :title="item.datasetName">{{ item.datasetName }}</span>
                </td>
                <td class="col-tags">
                  <div class="tag-stack">
                    <span v-for="(t, i) in item.sciCategories" :key="'s' + i" class="tag-badge blue">{{ t }}</span>
                  </div>
                </td>
                <td class="col-tags">
                  <div class="tag-stack">
                    <span v-for="(t, i) in item.industryTags" :key="'i' + i" class="tag-badge blue">{{ t }}</span>
                  </div>
                </td>
                <td class="col-tags">
                  <div class="tag-stack">
                    <span v-for="(t, i) in item.productCodes" :key="'p' + i" class="tag-badge blue">{{ t }}</span>
                  </div>
                </td>
                <td class="col-level">
                  <div class="tag-stack">
                    <span
                      v-for="(lv, i) in item.dataLevels"
                      :key="'l' + i"
                      class="level-badge"
                      :class="lv.kind === 'high' ? 'high' : 'public'"
                    >
                      {{ lv.text }}
                    </span>
                  </div>
                </td>
                <td class="col-status">{{ item.status }}</td>
                <td class="col-count">{{ item.dataCount }}</td>
                <td class="col-count">{{ item.pendingCount }}</td>
                <td class="col-count">{{ item.passedCount }}</td>
                <td class="col-count">{{ item.rejectedCount }}</td>
                <td class="col-count">{{ formatPassedRatio(item.passedRatio) }}</td>
                <td class="col-summary">
                  <span class="ellipsis" :title="item.summary">{{ item.summary }}</span>
                </td>
                <td class="col-name">
                  <span class="ellipsis" :title="item.templateName || ''">{{ item.templateName }}</span>
                </td>
                <td class="col-dept">{{ item.department }}</td>
                <td class="col-creator">{{ item.creator }}</td>
                <td class="col-time">{{ item.createTime }}</td>
                <td class="col-time">{{ item.updateTime }}</td>
                <td class="col-creator">{{ item.auditor }}</td>
                <td class="col-time">{{ item.auditTime }}</td>
                <td class="col-time">{{ item.prePublishTime }}</td>
                <td class="col-id">
                  <span class="ellipsis" :title="item.dataIdentifier">{{ item.dataIdentifier }}</span>
                </td>
                <td class="col-action">
                  <div class="action-btns">
                    <button type="button" class="action-btn" title="查看" @click="handleView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    </button>
                    <button type="button" class="action-btn" title="下载" @click="handleDownload(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                        <polyline points="7 10 12 15 17 10"/>
                        <line x1="12" y1="15" x2="12" y2="3"/>
                      </svg>
                    </button>
                    <button
                      type="button"
                      class="action-btn favorite"
                      :class="{ active: item.favorited }"
                      :title="item.favorited ? '取消收藏' : '收藏'"
                      @click="toggleFavorite(item)"
                    >
                      <svg
                        width="16"
                        height="16"
                        viewBox="0 0 24 24"
                        :fill="item.favorited ? 'currentColor' : 'none'"
                        stroke="#1a5ce6"
                        stroke-width="2"
                      >
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <div class="pagination">
            <span class="pagination-total">共 {{ total }} 条</span>
            <div class="pagination-nav">
              <button type="button" class="pagination-btn" :disabled="page <= 1" @click="goToPage(page - 1)">
                上一页
              </button>
              <div class="pagination-pages">
                <button
                  v-for="p in displayedPages"
                  :key="p"
                  type="button"
                  class="page-number"
                  :class="{ active: p === page }"
                  @click="goToPage(p)"
                >
                  {{ p }}
                </button>
              </div>
              <button
                type="button"
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
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'
import {
  fetchPublishFilterOptions,
  fetchPublishDatasetList,
  fetchPublishDatasetDetail,
  downloadPublishDataset,
  setPublishDatasetFavorite,
  batchPrePublishDatasets,
} from '../api/dataPublish.js'

const emit = defineEmits(['go-home'])

const categoryKeyword = ref('')
const activeCategory = ref('sci')
const treeExpanded = ref({ sci: true, industry: false })

function selectCategory(id) {
  activeCategory.value = id
}

function onCategorySearch() {
  page.value = 1
  loadTableData()
}

function toggleTree(key) {
  treeExpanded.value[key] = !treeExpanded.value[key]
  if (key === 'sci') activeCategory.value = 'sci'
  if (key === 'industry') activeCategory.value = 'industry'
}

const searchForm = ref({
  datasetName: '',
  industry: '',
  status: '',
  templateName: '',
  department: '',
  dataLevel: ''
})

/** @type {import('vue').Ref<{ label: string, value: string }[]>} */
const industryOptions = ref([])
/** @type {import('vue').Ref<{ label: string, value: string }[]>} */
const auditStatusOptions = ref([])
/** @type {import('vue').Ref<{ label: string, value: string }[]>} */
const deptOptions = ref([])
/** @type {import('vue').Ref<{ label: string, value: string, kind?: string }[]>} */
const dataLevelOptions = ref([])

const industryLabel = computed(() => {
  const v = searchForm.value.industry
  if (!v) return ''
  return industryOptions.value.find((o) => o.value === v)?.label ?? ''
})
const auditStatusLabel = computed(() => {
  const v = searchForm.value.status
  if (!v) return ''
  return auditStatusOptions.value.find((o) => o.value === v)?.label ?? ''
})
const departmentLabel = computed(() => {
  const v = searchForm.value.department
  if (!v) return ''
  return deptOptions.value.find((o) => o.value === v)?.label ?? ''
})
const dataLevelLabel = computed(() => {
  const v = searchForm.value.dataLevel
  if (!v) return ''
  return dataLevelOptions.value.find((o) => o.value === v)?.label ?? ''
})

const dropdownOpen = ref(null)

function toggleDropdown(key) {
  dropdownOpen.value = dropdownOpen.value === key ? null : key
}

function pickIndustry(v) {
  searchForm.value.industry = v
  dropdownOpen.value = null
}
function pickStatus(v) {
  searchForm.value.status = v
  dropdownOpen.value = null
}
function pickDept(v) {
  searchForm.value.department = v
  dropdownOpen.value = null
}
function pickLevel(v) {
  searchForm.value.dataLevel = v
  dropdownOpen.value = null
}

async function loadFilterOptions() {
  try {
    const data = await fetchPublishFilterOptions()
    industryOptions.value = data.industryCategories
    auditStatusOptions.value = data.auditStatuses
    deptOptions.value = data.departments
    dataLevelOptions.value = data.dataLevels
  } catch (e) {
    console.error('loadFilterOptions', e)
    const msg = e instanceof Error ? e.message : String(e)
    if (msg.includes('未登录') || msg.includes('无权限')) {
      alert(msg)
    }
  }
}

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const listLoading = ref(false)
const tableData = ref([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

const displayedPages = computed(() => {
  const pages = []
  const maxDisplay = 5
  let start = Math.max(1, page.value - 2)
  let end = Math.min(totalPages.value, start + maxDisplay - 1)
  if (end - start < maxDisplay - 1) {
    start = Math.max(1, end - maxDisplay + 1)
  }
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

async function loadTableData() {
  listLoading.value = true
  selectAll.value = false
  try {
    const { total: t, list } = await fetchPublishDatasetList({
      datasetName: searchForm.value.datasetName,
      industryCategory: searchForm.value.industry,
      auditStatus: searchForm.value.status,
      templateName: searchForm.value.templateName,
      department: searchForm.value.department,
      dataLevel: searchForm.value.dataLevel,
      scienceCategoryScope: activeCategory.value,
      categoryKeyword: categoryKeyword.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    total.value = t
    tableData.value = list
  } catch (e) {
    console.error('loadTableData', e)
    const msg = e instanceof Error ? e.message : String(e)
    total.value = 0
    tableData.value = []
    if (msg.includes('未登录') || msg.includes('无权限')) {
      alert(msg)
    }
  } finally {
    listLoading.value = false
  }
}

watch(activeCategory, () => {
  page.value = 1
  loadTableData()
})

const selectAll = ref(false)
const selectedCount = computed(() => tableData.value.filter((r) => r.selected).length)

function handleSelectAll() {
  tableData.value.forEach((r) => {
    r.selected = selectAll.value
  })
}

function cancelSelectAll() {
  selectAll.value = false
  tableData.value.forEach((r) => {
    r.selected = false
  })
}

function handleSearch() {
  page.value = 1
  loadTableData()
}

function handleReset() {
  searchForm.value = {
    datasetName: '',
    industry: '',
    status: '',
    templateName: '',
    department: '',
    dataLevel: ''
  }
  categoryKeyword.value = ''
  page.value = 1
  loadTableData()
}

function goToPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadTableData()
}

function handlePrePublish() {
  const selected = tableData.value.filter((r) => r.selected)
  if (selected.length === 0) {
    alert('请先选择要预发布的数据集')
    return
  }
  const ids = selected.map((r) => r.id)
  batchPrePublishDatasets(ids)
    .then((res) => {
      const ok = res?.successCount ?? 0
      const fail = res?.failCount ?? 0
      alert(`预发布完成：成功 ${ok}，失败 ${fail}`)
      loadTableData()
    })
    .catch((e) => {
      const msg = e instanceof Error ? e.message : String(e)
      alert(msg)
    })
}

function handleView(item) {
  fetchPublishDatasetDetail(item.id)
    .then((data) => {
      console.log('查看详情：', data)
      const name = data?.datasetName || data?.templateName || ''
      alert(name ? `查看：${name}` : '查看详情成功')
    })
    .catch((e) => {
      const msg = e instanceof Error ? e.message : String(e)
      alert(msg)
    })
}

function handlePublishAction(item) {
  console.log('预发布/记录：', item)
}

function handleDownload(item) {
  downloadPublishDataset(item.id)
    .then((data) => {
      if (data?.downloadUrl) {
        window.open(data.downloadUrl, '_blank')
        return
      }
      alert('未获取到下载链接')
    })
    .catch((e) => {
      const msg = e instanceof Error ? e.message : String(e)
      alert(msg)
    })
}

function toggleFavorite(item) {
  const next = !item.favorited
  setPublishDatasetFavorite(item.id, next)
    .then((res) => {
      item.favorited = Boolean(res?.favorited ?? next)
    })
    .catch((e) => {
      const msg = e instanceof Error ? e.message : String(e)
      alert(msg)
    })
}

function formatPassedRatio(ratio) {
  if (ratio == null || ratio === '') return ''
  const n = typeof ratio === 'number' ? ratio : Number(ratio)
  if (Number.isNaN(n)) return String(ratio)
  // 兼容后端返回 0~1 或 0~100
  const pct = n <= 1 ? n * 100 : n
  return `${pct.toFixed(2)}%`
}

function goHome() {
  emit('go-home')
}

function handleClickOutside(e) {
  if (!e.target.closest('.filter-select')) {
    dropdownOpen.value = null
  }
}

onMounted(() => {
  loadFilterOptions()
  loadTableData()
  document.addEventListener('click', handleClickOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.data-publish-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.publish-header {
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

.crumb-now {
  color: #333;
  font-weight: 500;
}

.publish-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: stretch;
}

.publish-sidebar {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.sidebar-search-row {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.sidebar-search {
  display: flex;
  align-items: center;
  flex: 1;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  overflow: hidden;
}

.sidebar-input {
  flex: 1;
  border: none;
  padding: 6px 10px;
  font-size: 13px;
  outline: none;
}

.sidebar-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 30px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
}

.sidebar-menu-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  background: #fff;
  color: #666;
  cursor: pointer;
}

.sidebar-tree {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tree-node {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 8px 10px 4px;
  margin: 0 -8px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  border-left: 3px solid transparent;
}

.tree-node:hover {
  background: #fafafa;
}

.tree-node.active {
  background: #e8f4ff;
  color: #1a5ce6;
  font-weight: 500;
  border-left-color: #1a5ce6;
}

.tree-chevron {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  color: #999;
  flex-shrink: 0;
  transition: transform 0.15s;
}

.tree-chevron.open {
  transform: rotate(90deg);
}

.tree-chevron.placeholder {
  visibility: hidden;
}

.tree-label {
  line-height: 1.4;
}

.publish-main {
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
  padding-left: 12px;
  border-left: 3px solid #1a5ce6;
}

.filter-grid {
  margin-bottom: 8px;
}

.filter-row {
  display: grid;
  grid-template-columns: repeat(3, 1fr);
  gap: 12px 20px;
  margin-bottom: 12px;
}

@media (max-width: 1100px) {
  .filter-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .publish-layout {
    grid-template-columns: 1fr;
  }
  .filter-row {
    grid-template-columns: 1fr;
  }
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-width: 0;
}

.filter-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
  flex-shrink: 0;
}

.filter-input {
  flex: 1;
  min-width: 0;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.filter-select {
  position: relative;
  flex: 1;
  min-width: 0;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}

.filter-select-text {
  flex: 1;
  color: #333;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.filter-select-text.placeholder {
  color: #999;
}

.filter-select-caret {
  margin-left: 4px;
  color: #999;
  flex-shrink: 0;
}

.filter-select-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  right: 0;
  padding: 4px 0;
  margin: 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 10;
  max-height: 220px;
  overflow-y: auto;
}

.filter-select-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.filter-select-item:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.filter-actions {
  display: flex;
  gap: 12px;
  margin-top: 4px;
  margin-bottom: 8px;
}

.search-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
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

.batch-area {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 12px;
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

.batch-link {
  padding: 0;
  border: none;
  background: none;
  font-size: 13px;
  color: #1a5ce6;
  cursor: pointer;
}

.batch-link:hover {
  text-decoration: underline;
}

.batch-info {
  font-size: 13px;
  color: #666;
}

.batch-count {
  color: #1a5ce6;
  font-weight: 500;
}

.batch-btn.pre-publish {
  display: inline-flex;
  align-items: center;
  gap: 4px;
  margin-left: auto;
  padding: 6px 14px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #1a5ce6;
  background: #fff;
  color: #1a5ce6;
}

.batch-btn.pre-publish:hover {
  background: #f0f5ff;
}

.pre-publish-icon {
  flex-shrink: 0;
}

.table-hint {
  text-align: center;
  padding: 28px 12px !important;
  color: #666;
  font-size: 13px;
}

.table-hint.muted {
  color: #999;
}

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
  border-bottom: 1px solid #f0f0f0;
  vertical-align: top;
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
  width: 52px;
  text-align: center;
}

.col-name {
  min-width: 160px;
  max-width: 220px;
}

.col-status {
  min-width: 90px;
}

.col-count {
  min-width: 80px;
}

.col-summary {
  min-width: 200px;
  max-width: 280px;
}

.col-id {
  min-width: 120px;
}

.col-time {
  min-width: 150px;
}

.col-type {
  min-width: 90px;
}

.col-dept {
  min-width: 120px;
}

.col-creator {
  min-width: 90px;
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-tags {
  min-width: 120px;
}

.col-level {
  min-width: 88px;
}

.col-action {
  width: 96px;
  text-align: center;
}

.tag-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.tag-badge.blue {
  display: inline-block;
  padding: 2px 8px;
  background: #e8f4ff;
  color: #1a5ce6;
  border-radius: 3px;
  font-size: 12px;
}

.level-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.level-badge.high {
  background: #fff1f0;
  color: #cf1322;
}

.level-badge.public {
  background: #f6ffed;
  color: #389e0d;
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
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  cursor: pointer;
}

.action-btn:hover {
  background: #f0f5ff;
}

.action-btn.favorite {
  color: #999;
}

.action-btn.favorite.active {
  color: #e6821a;
}

.action-btn.favorite.active:hover {
  background: #fff5e6;
}

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px 0 0;
  font-size: 13px;
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
</style>
