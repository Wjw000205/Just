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
          <template v-if="sidebarTree.length">
            <DatabaseSidebarTreeRow
              v-for="node in sidebarTree"
              :key="node.id"
              :node="node"
              :active-id="activeNodeId"
              :expanded-ids="sidebarExpanded"
              @select="selectNode"
              @toggle-expand="onSidebarToggleExpand"
            />
          </template>
          <li v-else class="sidebar-empty">暂无目录数据</li>
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
                <th class="col-tpl">模板名称</th>
                <th class="col-cat">数据类别</th>
                <th class="col-dept">所属部门</th>
                <th class="col-creator">创建人</th>
                <th class="col-time">创建时间</th>
                <th class="col-time">最近更新时间</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>

            <tbody v-if="loading">
              <tr>
                <td colspan="14" class="table-hint">加载中…</td>
              </tr>
            </tbody>
            <tbody v-else-if="rows.length === 0">
              <tr>
                <td colspan="14" class="table-hint muted">暂无数据</td>
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
                <td class="col-tpl">
                  <span class="ellipsis" :title="r.templateName">{{ r.templateName }}</span>
                </td>
                <td class="col-cat">
                  <span class="ellipsis" :title="r.dataCategory">{{ r.dataCategory }}</span>
                </td>
                <td class="col-dept">
                  <span class="ellipsis" :title="r.department">{{ r.department }}</span>
                </td>
                <td class="col-creator">
                  <span class="ellipsis" :title="r.creator">{{ r.creator }}</span>
                </td>
                <td class="col-time">
                  <span class="ellipsis" :title="r.createTime">{{ r.createTime }}</span>
                </td>
                <td class="col-time">
                  <span class="ellipsis" :title="r.updateTime">{{ r.updateTime }}</span>
                </td>
                <td class="col-action">
                  <div class="action-btns">
                    <button type="button" class="action-btn" title="查看" data-tip="查看" @click="handleView(r)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    </button>
                    <button type="button" class="action-btn" title="下载" data-tip="下载" @click="handleDownload(r)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                        <polyline points="7 10 12 15 17 10" />
                        <line x1="12" y1="15" x2="12" y2="3" />
                      </svg>
                    </button>
                    <button
                      type="button"
                      class="action-btn favorite"
                      :class="{ active: r.favorited }"
                      :title="r.favorited ? '取消收藏' : '收藏'"
                      :data-tip="r.favorited ? '取消收藏' : '收藏'"
                      @click="toggleFavorite(r)"
                    >
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

    <div v-if="downloadDialogVisible" class="download-mask" @click.self="closeDownloadDialog">
      <div class="download-dialog" role="dialog" aria-modal="true" aria-label="下载选项">
        <div class="download-header">
          <div class="download-title">下载选项</div>
          <button type="button" class="download-close" @click="closeDownloadDialog">×</button>
        </div>

        <div class="download-info">
          <div class="info-line"><span class="k">数据集名称：</span><span class="v">{{ downloadTarget?.datasetName || '-' }}</span></div>
          <div class="info-line"><span class="k">数据资源目录：</span><span class="v">-</span></div>
          <div class="info-line"><span class="k">数据量：</span><span class="v">{{ downloadTarget?.dataCount ?? '-' }}</span></div>
          <div class="info-line"><span class="k">模板名称：</span><span class="v">{{ downloadTarget?.templateName || '-' }}</span></div>
        </div>

        <div class="download-options">
          <div class="option-row">
            <div class="option-label">下载内容</div>
            <div class="option-btns">
              <button
                type="button"
                class="opt-btn"
                :class="{ active: downloadFieldMode === 'all' }"
                @click="downloadFieldMode = 'all'"
              >全部字段下载</button>
              <button
                type="button"
                class="opt-btn"
                :class="{ active: downloadFieldMode === 'partial' }"
                @click="selectPartialMode"
              >部分字段下载</button>
            </div>
          </div>

          <div v-if="downloadFieldMode === 'partial'" class="partial-fields-box">
            <div v-if="partialFieldsLoading" class="partial-fields-hint">加载字段中…</div>
            <div v-else-if="partialFieldOptions.length === 0" class="partial-fields-hint muted">暂无可选字段</div>
            <label v-for="field in partialFieldOptions" :key="field.value" class="partial-field-item">
              <input
                type="checkbox"
                :value="field.value"
                v-model="selectedPartialFields"
              />
              <span>{{ field.label }}</span>
            </label>
          </div>

          <div class="option-row">
            <div class="option-label">文件格式</div>
            <div class="option-btns">
              <button type="button" class="opt-btn" :class="{ active: downloadFileType === 'json' }" @click="downloadFileType = 'json'">JSON</button>
              <button type="button" class="opt-btn" :class="{ active: downloadFileType === 'xlsx' }" @click="downloadFileType = 'xlsx'">XLSX</button>
            </div>
          </div>

          <div class="option-row">
            <div class="option-label">数据形式</div>
            <div class="option-btns">
              <button type="button" class="opt-btn" :class="{ active: downloadDataShape === 'nested' }" @click="downloadDataShape = 'nested'">默认二维表</button>
              <button type="button" class="opt-btn" :class="{ active: downloadDataShape === 'flat' }" @click="downloadDataShape = 'flat'">平铺二维表</button>
            </div>
          </div>
        </div>

        <div v-if="downloadTaskHint" class="download-task-hint">{{ downloadTaskHint }}</div>
        <button type="button" class="download-confirm" :disabled="downloadSubmitting" @click="confirmDownload">
          {{ downloadSubmitting ? '处理中…' : '确定下载' }}
        </button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import {
  fetchDatabasePageInit,
  queryDatabaseDatasets,
  fetchDatabaseDownloadFields,
  downloadDatabaseDatasetFile,
} from '../api/database.js'
import DatabaseSidebarTreeRow from './DatabaseSidebarTreeRow.vue'

const emit = defineEmits(['go-home', 'view-detail'])

const categoryKeyword = ref('')
const sidebarTree = ref([])
/** @type {import('vue').Ref<Record<string, boolean>>} */
const sidebarExpanded = ref({})
const activeNodeId = ref('all')

function collectExpandedDefaults(nodes) {
  const o = {}
  function walk(arr) {
    for (const n of arr) {
      if (n.children?.length) {
        o[String(n.id)] = true
        walk(n.children)
      }
    }
  }
  walk(nodes)
  return o
}

function findDefaultActiveId(tree) {
  const q = [...tree]
  while (q.length) {
    const n = q.shift()
    if (String(n.id) === 'all') return 'all'
    if (n.children?.length) q.push(...n.children)
  }
  return tree[0] ? String(tree[0].id) : 'all'
}

function selectNode(id) {
  activeNodeId.value = id
  page.value = 1
  loadRows()
}

function onSidebarToggleExpand(id) {
  const k = String(id)
  sidebarExpanded.value = {
    ...sidebarExpanded.value,
    [k]: sidebarExpanded.value[k] !== true,
  }
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
const downloadDialogVisible = ref(false)
const downloadTarget = ref(null)
const downloadFieldMode = ref('all')
const downloadFileType = ref('json')
const downloadDataShape = ref('nested')
const selectedPartialFields = ref([])
const partialFieldOptions = ref([])
const partialFieldsLoading = ref(false)
const downloadSubmitting = ref(false)
const downloadTaskHint = ref('')

async function loadPartialFieldOptions() {
  const id = downloadTarget.value?.id
  if (id == null || id === '') {
    partialFieldOptions.value = []
    return
  }
  partialFieldsLoading.value = true
  try {
    partialFieldOptions.value = await fetchDatabaseDownloadFields(id)
  } catch (e) {
    console.error('loadPartialFieldOptions', e)
    const msg = e instanceof Error ? e.message : String(e)
    partialFieldOptions.value = []
    alert(msg || '加载下载字段失败')
  } finally {
    partialFieldsLoading.value = false
  }
}

function selectPartialMode() {
  downloadFieldMode.value = 'partial'
  if (partialFieldOptions.value.length === 0) {
    loadPartialFieldOptions()
  }
}

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
  emit('view-detail', row)
}
function handleDownload(row) {
  downloadTarget.value = row
  downloadFieldMode.value = 'all'
  downloadFileType.value = 'json'
  downloadDataShape.value = 'nested'
  selectedPartialFields.value = []
  partialFieldOptions.value = []
  downloadTaskHint.value = ''
  downloadDialogVisible.value = true
}
function toggleFavorite(row) {
  row.favorited = !row.favorited
}

function closeDownloadDialog() {
  downloadDialogVisible.value = false
}

async function confirmDownload() {
  if (downloadFieldMode.value === 'partial' && selectedPartialFields.value.length === 0) {
    alert('请选择至少一个下载字段')
    return
  }
  if (downloadSubmitting.value) return
  const id = downloadTarget.value?.id
  if (id == null || id === '') {
    alert('缺少数据集 id')
    return
  }

  downloadSubmitting.value = true
  downloadTaskHint.value = '正在生成并下载文件…'
  try {
    const body = {
      fieldMode: downloadFieldMode.value,
      fields: downloadFieldMode.value === 'partial' ? selectedPartialFields.value : [],
      fileType: downloadFileType.value,
      dataShape: downloadDataShape.value,
    }
    const { blob, filename } = await downloadDatabaseDatasetFile(id, body)
    const ext = downloadFileType.value === 'xlsx' ? 'xlsx' : 'json'
    const safeName =
      filename ||
      `${String(downloadTarget.value?.datasetName || 'dataset')}-${downloadFieldMode.value === 'partial' ? '部分字段' : '全部字段'}.${ext}`

    const url = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = url
    a.download = safeName
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(url)

    closeDownloadDialog()
  } catch (e) {
    console.error('confirmDownload', e)
    const msg = e instanceof Error ? e.message : String(e)
    alert(msg || '下载失败')
  } finally {
    downloadSubmitting.value = false
  }
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
    const { total: t, list } = await queryDatabaseDatasets({
      datasetName: searchForm.value.datasetName,
      industryCategory: searchForm.value.industryCategory,
      dataCategory: searchForm.value.dataCategory,
      templateName: searchForm.value.templateName,
      creator: searchForm.value.creator,
      department: searchForm.value.department,
      sidebarNodeId: activeNodeId.value,
      sidebarKeyword: categoryKeyword.value,
      page: page.value,
      pageSize: pageSize.value,
    })
    total.value = t
    rows.value = list
  } catch (e) {
    console.error('loadRows', e)
    const msg = e instanceof Error ? e.message : String(e)
    total.value = 0
    rows.value = []
    if (msg.includes('未登录') || msg.includes('无权限')) alert(msg)
  } finally {
    loading.value = false
  }
}

async function loadPageInit() {
  try {
    const res = await fetchDatabasePageInit({
      page: page.value,
      pageSize: pageSize.value,
    })
    sidebarTree.value = Array.isArray(res.tree) ? res.tree : []
    sidebarExpanded.value = collectExpandedDefaults(sidebarTree.value)
    activeNodeId.value = findDefaultActiveId(sidebarTree.value)

    industryOptions.value = res.industryCategories
    dataCategoryOptions.value = res.dataCategories
    deptOptions.value = res.departments

    total.value = res.total
    rows.value = res.list
  } catch (e) {
    console.error('loadPageInit', e)
    const msg = e instanceof Error ? e.message : String(e)
    if (msg.includes('未登录') || msg.includes('无权限')) alert(msg)
  }
}

onMounted(async () => {
  await loadPageInit()
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

.sidebar-empty {
  list-style: none;
  font-size: 13px;
  color: #999;
  padding: 12px 4px;
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

.col-tpl {
  min-width: 140px;
  max-width: 200px;
}

.col-cat {
  min-width: 110px;
  max-width: 160px;
}

.col-dept {
  min-width: 110px;
  max-width: 160px;
}

.col-creator {
  min-width: 90px;
  max-width: 140px;
}

.col-time {
  min-width: 150px;
  max-width: 190px;
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
  position: relative;
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

.action-btn[data-tip]:hover::after {
  content: attr(data-tip);
  position: absolute;
  left: 50%;
  top: -8px;
  transform: translate(-50%, -100%);
  padding: 4px 8px;
  border-radius: 4px;
  background: rgba(33, 33, 33, 0.92);
  color: #fff;
  font-size: 12px;
  line-height: 1;
  white-space: nowrap;
  pointer-events: none;
  z-index: 20;
}

.action-btn[data-tip]:hover::before {
  content: '';
  position: absolute;
  left: 50%;
  top: -8px;
  transform: translateX(-50%);
  border: 6px solid transparent;
  border-top-color: rgba(33, 33, 33, 0.92);
  pointer-events: none;
  z-index: 20;
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

.download-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.35);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 300;
}

.download-dialog {
  width: 680px;
  max-width: calc(100vw - 24px);
  border-radius: 8px;
  background: #fff;
  box-shadow: 0 12px 36px rgba(0, 0, 0, 0.24);
  padding: 14px 14px 16px;
}

.download-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  border-bottom: 1px solid #eceff4;
  padding-bottom: 10px;
  margin-bottom: 12px;
}

.download-title {
  font-size: 22px;
  font-weight: 600;
  color: #222;
}

.download-close {
  width: 28px;
  height: 28px;
  border: none;
  border-radius: 4px;
  background: transparent;
  color: #999;
  font-size: 20px;
  cursor: pointer;
}

.download-close:hover {
  background: #f5f7fb;
  color: #666;
}

.download-info {
  display: flex;
  flex-direction: column;
  gap: 8px;
  font-size: 13px;
  color: #333;
  margin-bottom: 12px;
}

.info-line .k {
  color: #666;
}

.download-options {
  border: 1px dashed #9ec0ef;
  border-radius: 6px;
  padding: 12px 14px;
  margin-bottom: 12px;
}

.option-row {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.option-row:last-child {
  margin-bottom: 0;
}

.option-label {
  width: 64px;
  color: #444;
  font-size: 13px;
  flex-shrink: 0;
}

.option-btns {
  display: flex;
  gap: 10px;
  flex-wrap: wrap;
}

.partial-fields-box {
  margin: -2px 0 12px 74px;
  max-height: 360px;
  overflow-y: auto;
  border: 1px solid #d7deea;
  border-radius: 6px;
  background: #fff;
  padding: 8px 10px;
}

.partial-fields-hint {
  padding: 10px 8px;
  font-size: 13px;
  color: #666;
}

.partial-fields-hint.muted {
  color: #999;
}

.partial-field-item {
  display: flex;
  align-items: center;
  gap: 8px;
  min-height: 30px;
  font-size: 14px;
  color: #333;
  padding: 0 8px;
  border-radius: 4px;
}

.partial-field-item:hover {
  background: #eef4ff;
}

.partial-field-item input {
  width: 14px;
  height: 14px;
}

.opt-btn {
  min-width: 90px;
  height: 32px;
  padding: 0 12px;
  border: none;
  border-radius: 4px;
  background: #e9eef6;
  color: #4a5f7a;
  font-size: 13px;
  cursor: pointer;
}

.opt-btn.active {
  background: #1a73e8;
  color: #fff;
}

.download-confirm {
  width: 100%;
  height: 36px;
  border: none;
  border-radius: 4px;
  background: #1a73e8;
  color: #fff;
  font-size: 14px;
  font-weight: 500;
  cursor: pointer;
}

.download-confirm:hover {
  background: #155fc0;
}

.download-confirm:disabled {
  opacity: 0.7;
  cursor: not-allowed;
}

.download-task-hint {
  margin: 0 0 10px;
  font-size: 13px;
  color: #666;
}

</style>

