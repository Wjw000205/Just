<template>
  <section class="database-page">
    <div class="db-header">
      <div class="db-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-now">数据库</span>
      </div>
    </div>

    <div class="db-layout">
      <aside class="db-sidebar">
        <div class="sidebar-search-row">
          <div class="sidebar-search">
            <input v-model="categoryKeyword" class="sidebar-input" placeholder="请输入科学分类名称" />
            <button type="button" class="sidebar-search-btn" aria-label="搜索" @click="handleCategorySearch">
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
          <li class="tree-node root" :class="{ active: activeNodeId === 'all' }" @click="selectNode('all')">
            <span class="tree-chevron placeholder" aria-hidden="true" />
            <span class="tree-label">全部</span>
          </li>

          <li class="tree-node group" :class="{ expanded: treeExpanded.sci }" @click="toggleGroup('sci')">
            <span class="tree-chevron" :class="{ open: treeExpanded.sci }" aria-hidden="true">
              <svg width="8" height="8" viewBox="0 0 10 10" fill="currentColor">
                <path d="M3 1L7 5L3 9V1Z" />
              </svg>
            </span>
            <span class="tree-label">生物医用材料（科学）</span>
          </li>
          <li v-if="treeExpanded.sci" class="tree-child-wrap">
            <ul class="tree-children">
              <li class="tree-node child" :class="{ active: activeNodeId === 'sci-common' }" @click.stop="selectNode('sci-common')">
                <span class="tree-chevron placeholder" aria-hidden="true" />
                <span class="tree-label">通用</span>
              </li>
            </ul>
          </li>

          <li class="tree-node group" :class="{ expanded: treeExpanded.industry }" @click="toggleGroup('industry')">
            <span class="tree-chevron" :class="{ open: treeExpanded.industry }" aria-hidden="true">
              <svg width="8" height="8" viewBox="0 0 10 10" fill="currentColor">
                <path d="M3 1L7 5L3 9V1Z" />
              </svg>
            </span>
            <span class="tree-label">生物医用材料（产业）</span>
          </li>
          <li v-if="treeExpanded.industry" class="tree-child-wrap">
            <ul class="tree-children">
              <li class="tree-node child" :class="{ active: activeNodeId === 'ind-common' }" @click.stop="selectNode('ind-common')">
                <span class="tree-chevron placeholder" aria-hidden="true" />
                <span class="tree-label">通用</span>
              </li>
            </ul>
          </li>
        </ul>
      </aside>

      <section class="db-main">
        <div class="main-title">数据库</div>

        <div class="filter-grid">
          <div class="filter-row cols-4">
            <div class="filter-item">
              <label class="filter-label">数据集名称</label>
              <input v-model="searchForm.datasetName" class="filter-input" placeholder="请输入数据集名称" />
            </div>
            <div class="filter-item">
              <label class="filter-label">产业分类</label>
              <div class="filter-select" @click.stop="toggleDropdown('industry')">
                <span :class="['filter-select-text', { placeholder: !searchForm.industryCategory }]">
                  {{ industryLabel || '请选择产业分类' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="dropdownOpen === 'industry'" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickIndustry('')">请选择产业分类</li>
                  <li v-for="opt in industryOptions" :key="opt.value" class="filter-select-item" @click.stop="pickIndustry(opt.value)">
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">数据类别</label>
              <div class="filter-select" @click.stop="toggleDropdown('category')">
                <span :class="['filter-select-text', { placeholder: !searchForm.dataCategory }]">
                  {{ dataCategoryLabel || '请选择数据类别' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="dropdownOpen === 'category'" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickDataCategory('')">请选择数据类别</li>
                  <li v-for="opt in dataCategoryOptions" :key="opt.value" class="filter-select-item" @click.stop="pickDataCategory(opt.value)">
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input v-model="searchForm.templateName" class="filter-input" placeholder="请输入模板名称" />
            </div>
          </div>

          <div class="filter-row second-row">
            <div class="filter-item">
              <label class="filter-label">创建人</label>
              <input v-model="searchForm.creator" class="filter-input" placeholder="请输入创建人" />
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
                  <li class="filter-select-item" @click.stop="pickDepartment('')">请选择</li>
                  <li v-for="opt in deptOptions" :key="opt.value" class="filter-select-item" @click.stop="pickDepartment(opt.value)">
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-actions inline-actions">
              <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
              <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
            </div>
          </div>
        </div>

        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th class="col-name">数据集名称 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th class="col-tags">科学分类 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th class="col-tags">产业分类 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th class="col-tags">产品代码 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th class="col-level">数据级别</th>
                <th class="col-count">数据量</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>

            <tbody v-if="loading">
              <tr>
                <td colspan="8" class="table-hint">加载中…</td>
              </tr>
            </tbody>
            <tbody v-else-if="rows.length === 0">
              <tr>
                <td colspan="8" class="table-hint muted">暂无数据</td>
              </tr>
            </tbody>
            <tbody v-else>
              <tr v-for="(r, idx) in rows" :key="String(r.id ?? idx)">
                <td class="col-index">{{ (page - 1) * pageSize + idx + 1 }}</td>
                <td class="col-name">
                  <span class="ellipsis" :title="r.datasetName">{{ r.datasetName }}</span>
                </td>
                <td class="col-tags">
                  <div class="tag-stack">
                    <span v-for="(t, i) in r.scienceCategories" :key="'s' + i" class="tag-badge blue">{{ t }}</span>
                  </div>
                </td>
                <td class="col-tags">
                  <div class="tag-stack">
                    <span v-for="(t, i) in r.industryCategories" :key="'i' + i" class="tag-badge blue">{{ t }}</span>
                  </div>
                </td>
                <td class="col-tags">
                  <div class="tag-stack">
                    <span v-for="(t, i) in r.productCodes" :key="'p' + i" class="tag-badge blue">{{ t }}</span>
                  </div>
                </td>
                <td class="col-level">
                  <span class="level-badge" :class="r.dataLevel.kind === 'high' ? 'high' : 'public'">{{ r.dataLevel.text }}</span>
                </td>
                <td class="col-count">{{ r.dataCount }}</td>
                <td class="col-action">
                  <div class="action-btns">
                    <button type="button" class="action-btn" title="查看" @click="handleView(r)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    </button>
                    <button type="button" class="action-btn" title="下载" @click="handleDownload(r)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                        <polyline points="7 10 12 15 17 10" />
                        <line x1="12" y1="15" x2="12" y2="3" />
                      </svg>
                    </button>
                    <button type="button" class="action-btn favorite" :class="{ active: r.favorited }" :title="r.favorited ? '取消收藏' : '收藏'" @click="toggleFavorite(r)">
                      <svg width="16" height="16" viewBox="0 0 24 24" :fill="r.favorited ? 'currentColor' : 'none'" stroke="#1a5ce6" stroke-width="2">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
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
              <button type="button" class="pagination-btn" :disabled="page <= 1" @click="goToPage(page - 1)">上一页</button>
              <div class="pagination-pages">
                <button v-for="p in displayedPages" :key="p" type="button" class="page-number" :class="{ active: p === page }" @click="goToPage(p)">
                  {{ p }}
                </button>
              </div>
              <button type="button" class="pagination-btn" :disabled="page >= totalPages" @click="goToPage(page + 1)">下一页</button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { fetchDatabaseFilterOptions } from '../api/database.js'

const emit = defineEmits(['go-home'])

const categoryKeyword = ref('')
const treeExpanded = ref({ sci: true, industry: true })
const activeNodeId = ref('all')

function selectNode(id) {
  activeNodeId.value = id
  page.value = 1
  loadRows()
}

function toggleGroup(key) {
  treeExpanded.value[key] = !treeExpanded.value[key]
}

function handleCategorySearch() {
  page.value = 1
  loadRows()
}

const searchForm = ref({
  datasetName: '',
  industryCategory: '',
  dataCategory: '',
  templateName: '',
  creator: '',
  department: '',
})

const dropdownOpen = ref(null)
function toggleDropdown(key) {
  dropdownOpen.value = dropdownOpen.value === key ? null : key
}

const industryOptions = ref([])
const dataCategoryOptions = ref([])
const deptOptions = ref([])

const industryLabel = computed(() => {
  const v = searchForm.value.industryCategory
  if (!v) return ''
  return industryOptions.value.find((o) => o.value === v)?.label ?? ''
})
const dataCategoryLabel = computed(() => {
  const v = searchForm.value.dataCategory
  if (!v) return ''
  return dataCategoryOptions.value.find((o) => o.value === v)?.label ?? ''
})
const departmentLabel = computed(() => {
  const v = searchForm.value.department
  if (!v) return ''
  return deptOptions.value.find((o) => o.value === v)?.label ?? ''
})

function pickIndustry(v) {
  searchForm.value.industryCategory = v
  dropdownOpen.value = null
}
function pickDataCategory(v) {
  searchForm.value.dataCategory = v
  dropdownOpen.value = null
}
function pickDepartment(v) {
  searchForm.value.department = v
  dropdownOpen.value = null
}

const page = ref(1)
const pageSize = ref(10)
const total = ref(0)
const loading = ref(false)
const rows = ref([])

const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))
const displayedPages = computed(() => {
  const pages = []
  const maxDisplay = 5
  let start = Math.max(1, page.value - 2)
  let end = Math.min(totalPages.value, start + maxDisplay - 1)
  if (end - start < maxDisplay - 1) start = Math.max(1, end - maxDisplay + 1)
  for (let i = start; i <= end; i++) pages.push(i)
  return pages
})

function handleSearch() {
  page.value = 1
  loadRows()
}
function handleReset() {
  searchForm.value = { datasetName: '', industryCategory: '', dataCategory: '', templateName: '', creator: '', department: '' }
  categoryKeyword.value = ''
  activeNodeId.value = 'all'
  page.value = 1
  loadRows()
}
function goToPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
  loadRows()
}

function handleView(row) {
  console.log('查看：', row)
}
function handleDownload(row) {
  console.log('下载：', row)
}
function toggleFavorite(row) {
  row.favorited = !row.favorited
}

function goHome() {
  emit('go-home')
}

function handleClickOutside(e) {
  if (!e.target.closest('.filter-select')) dropdownOpen.value = null
}

async function loadRows() {
  loading.value = true
  try {
    const all = [
      {
        id: 1,
        datasetName: 'DLP3D磷酸钙瓷打印高值数据',
        scienceCategories: ['双相磷酸钙'],
        industryCategories: ['生物医用材料制造'],
        productCodes: [],
        dataLevel: { text: '高值', kind: 'high' },
        dataCount: 746,
        favorited: false,
      },
      {
        id: 2,
        datasetName: '0318生物医用对接数据集',
        scienceCategories: ['α-磷酸三钙'],
        industryCategories: ['铝及铝合金制造'],
        productCodes: [],
        dataLevel: { text: '高值', kind: 'high' },
        dataCount: 144,
        favorited: false,
      },
      {
        id: 3,
        datasetName: '22',
        scienceCategories: ['羟基磷灰石'],
        industryCategories: ['先进制造基础零部件…'],
        productCodes: [],
        dataLevel: { text: '高值', kind: 'high' },
        dataCount: 38,
        favorited: false,
      },
      {
        id: 4,
        datasetName: '表格1',
        scienceCategories: ['羟基磷灰石'],
        industryCategories: ['先进制造基础零部件…'],
        productCodes: [],
        dataLevel: { text: '高值', kind: 'high' },
        dataCount: 18,
        favorited: false,
      },
      {
        id: 5,
        datasetName: '公益数据照演示测试0316',
        scienceCategories: ['钛及钛合金', '钛及钛合金', '钛及钛合金'],
        industryCategories: ['生物医用材料制造', '生物医用材料制造', '生物医用材料制造'],
        productCodes: ['2770035', '2770036', '2770037', '3073052'],
        dataLevel: { text: '公益', kind: 'public' },
        dataCount: 72,
        favorited: false,
      },
    ]

    const catKw = categoryKeyword.value.trim()
    const dsName = searchForm.value.datasetName.trim()
    const industry = searchForm.value.industryCategory

    let filtered = all
    if (activeNodeId.value === 'sci-common') filtered = filtered.filter((r) => (r.scienceCategories || []).length > 0)
    if (activeNodeId.value === 'ind-common') filtered = filtered.filter((r) => (r.industryCategories || []).length > 0)
    if (catKw) {
      filtered = filtered.filter((r) => (r.scienceCategories || []).join(',').includes(catKw) || String(r.datasetName || '').includes(catKw))
    }
    if (dsName) filtered = filtered.filter((r) => String(r.datasetName || '').includes(dsName))
    if (industry) {
      const label = industryOptions.value.find((o) => o.value === industry)?.label || ''
      if (label) filtered = filtered.filter((r) => (r.industryCategories || []).some((x) => String(x).includes(label)))
    }

    total.value = filtered.length
    const start = (page.value - 1) * pageSize.value
    rows.value = filtered.slice(start, start + pageSize.value)
  } finally {
    loading.value = false
  }
}

async function loadDropdownOptions() {
  try {
    const res = await fetchDatabaseFilterOptions()
    industryOptions.value = res.industryCategories
    dataCategoryOptions.value = res.dataCategories
    deptOptions.value = res.departments
  } catch (e) {
    console.error('loadDropdownOptions', e)
    const msg = e instanceof Error ? e.message : String(e)
    if (msg.includes('未登录') || msg.includes('无权限')) alert(msg)
  }
}

onMounted(async () => {
  await loadDropdownOptions()
  loadRows()
  document.addEventListener('click', handleClickOutside)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.database-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.db-header {
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

.db-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: stretch;
}

.db-sidebar {
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
  flex-shrink: 0;
  color: #999;
  transition: transform 0.15s;
}

.tree-chevron.open {
  transform: rotate(90deg);
}

.tree-chevron.placeholder {
  visibility: hidden;
}

.tree-children {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tree-child-wrap {
  margin-left: 18px;
}

.tree-node.child {
  padding-left: 18px;
}

.tree-node.group {
  color: #333;
}

.db-main {
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

.filter-row.cols-4 {
  grid-template-columns: repeat(4, 1fr);
}

.filter-row.second-row {
  grid-template-columns: 1fr 1fr auto;
  align-items: center;
}

@media (max-width: 1100px) {
  .filter-row {
    grid-template-columns: 1fr 1fr;
  }
  .filter-row.cols-4 {
    grid-template-columns: 1fr 1fr;
  }
  .filter-row.second-row {
    grid-template-columns: 1fr 1fr;
  }
}

@media (max-width: 720px) {
  .db-layout {
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

.filter-actions.inline-actions {
  margin: 0;
  justify-content: flex-start;
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

.col-index {
  width: 52px;
  text-align: center;
}

.col-name {
  min-width: 200px;
  max-width: 260px;
}

.col-tags {
  min-width: 120px;
}

.col-level {
  min-width: 88px;
}

.col-count {
  min-width: 80px;
}

.col-action {
  width: 96px;
  text-align: center;
}

.tag-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #e8f4ff;
  color: #1a5ce6;
  border-radius: 3px;
  font-size: 12px;
}

.tag-badge.blue {
  background: #e8f4ff;
  color: #1a5ce6;
}

.tag-stack {
  display: flex;
  flex-direction: column;
  align-items: flex-start;
  gap: 6px;
}

.ellipsis {
  display: inline-block;
  max-width: 100%;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.sort-caret {
  color: #c0c4cc;
  font-size: 11px;
  margin-left: 4px;
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

