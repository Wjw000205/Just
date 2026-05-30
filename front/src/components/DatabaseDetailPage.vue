<template>
  <section class="db-detail-page">
    <div class="db-header">
      <div class="db-breadcrumb">
        当前位置：<span class="crumb-main" @click="emit('go-home')">首页</span> &gt;
        <span class="crumb-main" @click="emit('back')">数据库</span> &gt;
        <span class="crumb-now">查看</span>
      </div>
    </div>

    <div class="detail-card">
      <div class="cover" aria-hidden="true">
        <div class="cover-img"></div>
      </div>
      <div class="meta">
        <div class="meta-top">
          <div class="title-wrap">
            <div class="title">{{ headerTitle }}</div>
            <span class="level-pill">{{ headerLevel }}</span>
          </div>
          <div class="top-actions">
            <button type="button" class="btn ghost" @click="collapsed = !collapsed">
              {{ collapsed ? '展开' : '收起' }}
            </button>
            <button type="button" class="btn ghost" @click="emit('back')">返回</button>
          </div>
        </div>
        <div class="meta-grid">
          <div class="kv"><span class="k">模板：</span><span class="v">{{ headerTemplate }}</span></div>
          <div class="kv"><span class="k">所属部门：</span><span class="v">{{ headerDept }}</span></div>
          <div class="kv"><span class="k">创建人：</span><span class="v">{{ headerCreator }}</span></div>
          <div class="kv"><span class="k">创建时间：</span><span class="v">{{ headerCreateTime }}</span></div>
        </div>
      </div>
    </div>

    <div class="layout">
      <aside class="side">
        <div class="side-actions">
          <button type="button" class="btn primary" @click="handleSearch">查询</button>
          <button type="button" class="btn ghost" @click="handleReset">重置</button>
        </div>

        <div v-if="!collapsed" class="field-tree">
          <div
            v-for="{ node, depth } in visibleTreeNodes"
            :key="node.id"
            class="tree-row"
            :style="{ paddingLeft: `${8 + depth * 18}px` }"
          >
            <button
              v-if="node.children && node.children.length"
              type="button"
              class="tree-toggle"
              @click="toggleNodeExpand(node.id)"
            >
              <span :class="['caret', { open: node.expanded !== false }]">▶</span>
            </button>
            <span v-else class="tree-toggle placeholder"></span>

            <input
              type="checkbox"
              :checked="nodeChecked(node)"
              @change="setNodeChecked(node, $event.target.checked)"
            />
            <span class="field-text">{{ node.label }}</span>
          </div>
        </div>
      </aside>

      <section class="main">
        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th v-for="column in visibleDataColumns" :key="column.key" class="col-dynamic">
                  {{ column.label }}
                </th>
              </tr>
            </thead>

            <tbody v-if="loading">
              <tr><td :colspan="tableColspan" class="table-hint">加载中…</td></tr>
            </tbody>
            <tbody v-else-if="rows.length === 0">
              <tr><td :colspan="tableColspan" class="table-hint muted">暂无数据</td></tr>
            </tbody>
            <tbody v-else>
              <tr v-for="(r, idx) in rows" :key="r.id ?? idx">
                <td class="col-index">{{ (pageNum - 1) * pageSize + idx + 1 }}</td>
                <td v-for="column in visibleDataColumns" :key="column.key" class="col-dynamic">
                  {{ cellValue(r, column) }}
                </td>
              </tr>
            </tbody>
          </table>
        </div>
        <div class="pagination">
          <span class="pagination-total">共 {{ total }} 条</span>
          <div class="pagination-nav">
            <button type="button" class="pagination-btn" :disabled="pageNum <= 1" @click="goToPage(pageNum - 1)">上一页</button>
            <span class="page-number active">{{ pageNum }}</span>
            <button type="button" class="pagination-btn" :disabled="pageNum >= totalPages" @click="goToPage(pageNum + 1)">下一页</button>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref, watch } from 'vue'

const props = defineProps({
  dataset: { type: Object, default: null },
})
const emit = defineEmits(['go-home', 'back'])

const collapsed = ref(false)
const loading = ref(false)
const rows = ref([])
const columns = ref([])
const total = ref(0)
const pageNum = ref(1)
const pageSize = ref(10)

function makeFieldTree(nextColumns = []) {
  return nextColumns.map((column, index) => ({
    id: column.key || `column-${index}`,
    label: column.label || column.key || `字段${index + 1}`,
    key: column.key || column.label || `column-${index}`,
    visible: true,
  }))
}

const fieldTree = ref(makeFieldTree())

function walkNodes(nodes, fn) {
  nodes.forEach((n) => {
    fn(n)
    if (n.children?.length) walkNodes(n.children, fn)
  })
}

function findNodeById(nodes, id) {
  for (const n of nodes) {
    if (n.id === id) return n
    if (n.children?.length) {
      const found = findNodeById(n.children, id)
      if (found) return found
    }
  }
  return null
}

const leafColumns = computed(() => {
  const leaves = []
  walkNodes(fieldTree.value, (n) => {
    if (n.key) leaves.push(n)
  })
  return leaves
})

const visibleDataColumns = computed(() =>
  leafColumns.value.filter((column) => column.visible !== false),
)

const visibleTreeNodes = computed(() => {
  const result = []
  const dfs = (nodes, depth) => {
    nodes.forEach((n) => {
      result.push({ node: n, depth })
      if (n.children?.length && n.expanded !== false) dfs(n.children, depth + 1)
    })
  }
  dfs(fieldTree.value, 0)
  return result
})

function toggleNodeExpand(id) {
  const node = findNodeById(fieldTree.value, id)
  if (!node || !node.children?.length) return
  node.expanded = node.expanded === false
}

function nodeChecked(node) {
  if (!node.children?.length) return node.visible !== false
  const leaves = []
  walkNodes([node], (n) => {
    if (n.key) leaves.push(n)
  })
  return leaves.length > 0 && leaves.every((x) => x.visible !== false)
}

function setNodeChecked(node, checked) {
  if (!node.children?.length) {
    node.visible = checked
    return
  }
  walkNodes([node], (n) => {
    if (n.key) n.visible = checked
  })
}

function colVisible(key) {
  return leafColumns.value.find((c) => c.key === key)?.visible !== false
}

function groupSpan(group) {
  if (group === 'source') return groupSpan('paper') + groupSpan('experiment')
  return leafColumns.value.filter((c) => c.group === group && c.visible).length
}

const tableColspan = computed(() => {
  return visibleDataColumns.value.length + 1
})

const headerTitle = computed(() => props.dataset?.datasetName || props.dataset?.name || '数据集详情')
const headerTemplate = computed(() => props.dataset?.templateName || props.dataset?.template || '-')
const headerDept = computed(() => props.dataset?.department || props.dataset?.deptName || '-')
const headerCreator = computed(() => props.dataset?.creator || props.dataset?.createdBy || '-')
const headerCreateTime = computed(() => props.dataset?.createTime || props.dataset?.createdAt || '-')
const headerLevel = computed(() => {
  const level = props.dataset?.dataLevel
  const map = { public: '公益', highvalue: '高值', private: '私有' }
  if (level && typeof level === 'object') {
    return map[level.value] || map[level.text] || level.text || '公益'
  }
  return map[level] || level || '公益'
})
const totalPages = computed(() => Math.max(1, Math.ceil(total.value / pageSize.value)))

function handleSearch() {
  pageNum.value = 1
  loadRows()
}
function handleReset() {
  fieldTree.value = makeFieldTree(columns.value)
  pageNum.value = 1
  loadRows()
}

function cellValue(row, column) {
  const data = row?.data || {}
  const value = data[column.label] ?? data[column.key] ?? data[column.id]
  return value == null || value === '' ? '-' : value
}

function getDatasetName() {
  return props.dataset?.datasetName || props.dataset?.name || ''
}

function normalizeColumns(rawColumns) {
  return Array.isArray(rawColumns)
    ? rawColumns.map((column, index) => ({
        id: column.id ?? index,
        key: column.columnName || `column-${index + 1}`,
        label: column.columnName || `字段${index + 1}`,
        type: column.columnType || '',
      }))
    : []
}

function normalizeRows(records) {
  return Array.isArray(records)
    ? records.map((record, index) => ({
        id: record.rowId ?? index,
        rowId: record.rowId,
        data: record.data || {},
      }))
    : []
}

async function fetchDatasetDataPage() {
  const datasetName = getDatasetName()
  if (!datasetName) {
    throw new Error('缺少数据集名称')
  }

  const params = new URLSearchParams({
    DatasetName: datasetName,
    pageNum: String(pageNum.value),
    pageSize: String(pageSize.value),
  })
  const resp = await fetch(`/Dataset/data-page?${params.toString()}`, {
    method: 'GET',
    headers: getAuthHeader(),
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || (json.code !== 0 && json.code !== 200)) {
    throw new Error(json?.message || '查询数据失败')
  }
  return json.data || {}
}

function getAuthHeader() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  return token ? { Authorization: `Bearer ${token}` } : {}
}

function goToPage(nextPage) {
  if (nextPage < 1 || nextPage > totalPages.value) return
  pageNum.value = nextPage
  loadRows()
}

async function loadRows() {
  loading.value = true
  try {
    const data = await fetchDatasetDataPage()
    const nextColumns = normalizeColumns(data.columns)
    columns.value = nextColumns
    fieldTree.value = makeFieldTree(nextColumns)
    const pageData = data.pageData || {}
    const records = pageData.records || pageData.list || pageData.rows || data.records || []
    total.value = Number(pageData.total ?? data.total ?? records.length)
    rows.value = normalizeRows(records)
  } catch (e) {
    console.error('loadRows', e)
    columns.value = []
    fieldTree.value = []
    rows.value = []
    total.value = 0
    alert(e?.message || '查询数据失败')
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRows()
})

watch(
  () => getDatasetName(),
  () => {
    pageNum.value = 1
    loadRows()
  },
)
</script>

<style scoped>
.db-detail-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f3f5f8;
  padding: 12px 16px 18px;
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

.detail-card {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 14px;
  background: #fff;
  border-radius: 8px;
  padding: 10px 14px;
  border: 1px solid #e9edf3;
}

.cover {
  display: flex;
  align-items: center;
  justify-content: center;
}

.cover-img {
  width: 188px;
  height: 96px;
  border-radius: 8px;
  background:
    radial-gradient(circle at 20% 20%, rgba(26, 92, 230, 0.25), transparent 55%),
    radial-gradient(circle at 80% 80%, rgba(26, 92, 230, 0.18), transparent 55%),
    linear-gradient(135deg, #0c2f66, #1a5ce6);
}

.meta .title {
  font-size: 20px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.meta-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  margin: 2px 0 14px;
}

.title-wrap {
  display: flex;
  align-items: center;
  gap: 12px;
  min-width: 0;
}

.level-pill {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  min-width: 48px;
  height: 24px;
  padding: 0 10px;
  border-radius: 4px;
  background: #1a5ce6;
  color: #fff;
  font-size: 12px;
  font-weight: 500;
  flex-shrink: 0;
}

.top-actions {
  display: flex;
  align-items: center;
  gap: 8px;
}

.meta-grid {
  display: grid;
  grid-template-columns: repeat(4, minmax(0, 1fr));
  gap: 12px 22px;
  font-size: 13px;
}

.kv {
  min-width: 0;
  display: flex;
  gap: 6px;
  align-items: baseline;
}
.k {
  color: #3a3f4a;
  white-space: nowrap;
}
.v {
  color: #1f2329;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.layout {
  display: grid;
  grid-template-columns: 220px 1fr;
  gap: 10px;
  align-items: stretch;
}

.side {
  background: #f5f6f8;
  border-radius: 8px;
  padding: 12px 10px;
  border: 1px solid #e7ebf1;
}

.side-actions {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 8px;
}

.btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 4px;
  border: 1px solid;
  font-size: 13px;
  cursor: pointer;
}
.btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}
.btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}
.field-tree {
  display: flex;
  flex-direction: column;
  gap: 1px;
  padding: 2px 0 2px;
}
.tree-row {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #1f2329;
  min-height: 38px;
  border-radius: 4px;
}

.tree-row:hover {
  background: #edf1f7;
}
.tree-toggle {
  width: 14px;
  height: 14px;
  border: none;
  background: transparent;
  padding: 0;
  color: #a0a7b4;
  cursor: pointer;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}
.tree-toggle.placeholder {
  visibility: hidden;
}
.caret {
  font-size: 10px;
  line-height: 1;
  transition: transform 0.15s ease;
}
.caret.open {
  transform: rotate(90deg);
}
.field-text {
  user-select: none;
}

.main {
  background: #fff;
  border-radius: 8px;
  padding: 10px 12px 12px;
  border: 1px solid #e9edf3;
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
  padding: 10px 10px;
  text-align: left;
  border: 1px solid #edf0f5;
  vertical-align: top;
}
.data-table th {
  background: #f7f9fc;
  color: #333;
  font-weight: 500;
  white-space: nowrap;
}
.data-table tbody tr:hover {
  background: #fafafa;
}

.group {
  text-align: center;
  font-weight: 600;
}
.subgroup {
  text-align: center;
  font-weight: 600;
}

.col-index {
  width: 70px;
  text-align: center;
}
.col-dynamic {
  min-width: 140px;
  max-width: 260px;
  word-break: break-word;
}
.col-code {
  min-width: 180px;
}
.col-small {
  min-width: 150px;
}
.col-mid {
  min-width: 170px;
}
.col-action {
  width: 130px;
  text-align: center;
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
.sort-caret {
  color: #c0c4cc;
  font-size: 14px;
  margin-left: 4px;
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
  width: 30px;
  height: 30px;
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

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 16px;
  padding: 12px 2px 0;
  font-size: 13px;
  color: #666;
}

.pagination-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn,
.page-number {
  min-width: 34px;
  height: 32px;
  padding: 0 12px;
  border: 1px solid #d9e0ec;
  border-radius: 4px;
  background: #fff;
  color: #4a5568;
  font-size: 13px;
  cursor: pointer;
}

.pagination-btn:hover:not(:disabled),
.page-number:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.pagination-btn:disabled {
  color: #b8beca;
  background: #f7f8fb;
  cursor: not-allowed;
}

.page-number.active {
  border-color: #1a5ce6;
  background: #1a5ce6;
  color: #fff;
}

@media (max-width: 1100px) {
  .meta-grid {
    grid-template-columns: repeat(2, minmax(0, 1fr));
  }
}

@media (max-width: 860px) {
  .detail-card {
    grid-template-columns: 1fr;
  }
  .layout {
    grid-template-columns: 1fr;
  }
}
</style>

