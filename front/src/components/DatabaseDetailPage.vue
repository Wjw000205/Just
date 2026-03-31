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
          <div class="kv"><span class="k">科学分类：</span><span class="v">{{ headerSci }}</span></div>
          <div class="kv"><span class="k">产业分类：</span><span class="v">{{ headerInd }}</span></div>
          <div class="kv"><span class="k">产品代码：</span><span class="v">{{ headerCodes }}</span></div>
          <div class="kv"><span class="k">数据类别：</span><span class="v">{{ headerDataCategory }}</span></div>

          <div class="kv"><span class="k">数据量：</span><span class="v">{{ headerCount }}</span></div>
          <div class="kv"><span class="k">模板：</span><span class="v">{{ headerTemplate }}</span></div>
          <div class="kv"><span class="k">数据来源：</span><span class="v">{{ headerDept }}</span></div>
          <div class="kv"><span class="k">数据标识：</span><span class="v">-</span></div>

          <div class="kv"><span class="k">创建人：</span><span class="v">{{ headerCreator }}</span></div>
          <div class="kv"><span class="k">创建时间：</span><span class="v">{{ headerCreateTime }}</span></div>
          <div class="kv"><span class="k">审核时间：</span><span class="v">{{ headerAuditTime }}</span></div>
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
                <th v-if="colVisible('index')" class="col-index" rowspan="3">序号</th>
                <th v-if="colVisible('materialCode')" class="col-code" rowspan="3">
                  材料编号 <span class="sort-caret" aria-hidden="true">⇅</span>
                </th>
                <th class="group" :colspan="groupSpan('source')">数据来源</th>
                <th class="col-action" rowspan="3">操作</th>
              </tr>
              <tr>
                <th class="subgroup" :colspan="groupSpan('paper')">文献</th>
                <th class="subgroup" :colspan="groupSpan('experiment')">实验</th>
              </tr>
              <tr>
                <th v-if="colVisible('collector')" class="col-small">采集人员 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th v-if="colVisible('collectorOrg')" class="col-mid">采集单位 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th v-if="colVisible('collectDate')" class="col-mid">采集日期 <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th v-if="colVisible('doi')" class="col-mid">文献DOI <span class="sort-caret" aria-hidden="true">⇅</span></th>
                <th v-if="colVisible('experimenter')" class="col-small">实验人员 <span class="sort-caret" aria-hidden="true">⇅</span></th>
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
                <td v-if="colVisible('index')" class="col-index">{{ idx + 1 }}</td>
                <td v-if="colVisible('materialCode')" class="col-code">{{ r.materialCode }}</td>
                <td v-if="colVisible('collector')" class="col-small">{{ r.collector || '-' }}</td>
                <td v-if="colVisible('collectorOrg')" class="col-mid">{{ r.collectorOrg || '-' }}</td>
                <td v-if="colVisible('collectDate')" class="col-mid">{{ r.collectDate || '-' }}</td>
                <td v-if="colVisible('doi')" class="col-mid">{{ r.doi || '-' }}</td>
                <td v-if="colVisible('experimenter')" class="col-small">{{ r.experimenter || '-' }}</td>
                <td class="col-action">
                  <div class="action-btns">
                    <button type="button" class="action-btn" data-tip="查看" @click="onRowView(r)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z" />
                        <circle cx="12" cy="12" r="3" />
                      </svg>
                    </button>
                    <button type="button" class="action-btn favorite" :class="{ active: r.favorited }" :data-tip="r.favorited ? '取消收藏' : '收藏'" @click="r.favorited = !r.favorited">
                      <svg width="16" height="16" viewBox="0 0 24 24" :fill="r.favorited ? 'currentColor' : 'none'" stroke="#1a5ce6" stroke-width="2">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, onMounted, ref } from 'vue'

const props = defineProps({
  dataset: { type: Object, default: null },
})
const emit = defineEmits(['go-home', 'back'])

const collapsed = ref(false)
const loading = ref(false)
const rows = ref([])

function makeFieldTree() {
  // 左侧树与表格列共用同一份 key（叶子节点）
  return [
    { id: 'materialCode', label: '材料编号', key: 'materialCode', group: 'base', visible: true },
    { id: 'materialCnName', label: '材料中文名称', visible: true },
    { id: 'materialCnAlias', label: '材料中文名称简称', visible: true },
    { id: 'materialEnName', label: '材料英文名称', visible: true },
    { id: 'materialEnAlias', label: '材料英文名称简称', visible: true },
    { id: 'materialCategory', label: '材料类别', visible: true },
    { id: 'materialUsage', label: '材料用途', visible: true },
    {
      id: 'source',
      label: '数据来源',
      expanded: true,
      children: [
        {
          id: 'paper',
          label: '文献',
          expanded: true,
          children: [
            { id: 'collector', label: '采集人员', key: 'collector', group: 'paper', visible: true },
            { id: 'collectorOrg', label: '采集单位', key: 'collectorOrg', group: 'paper', visible: true },
            { id: 'collectDate', label: '采集日期', key: 'collectDate', group: 'paper', visible: true },
            { id: 'doi', label: '文献DOI', key: 'doi', group: 'paper', visible: true },
          ],
        },
        {
          id: 'experiment',
          label: '实验',
          expanded: true,
          children: [
            { id: 'experimenter', label: '实验人员', key: 'experimenter', group: 'experiment', visible: true },
            { id: 'calc', label: '计算', visible: true },
            { id: 'rd', label: '研发', visible: true },
            { id: 'manufacturing', label: '生产', visible: true },
            { id: 'clinical', label: '临床', visible: true },
          ],
        },
      ],
    },
    { id: 'sampleInfo', label: '样品信息', visible: true },
  ]
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
  // base 两列（index/materialCode）按可见计算 + paper/experiment 可见列 + 操作列
  const base = ['index', 'materialCode'].filter((k) => colVisible(k)).length
  const rest = leafColumns.value.filter((c) => c.group !== 'base' && c.visible).length
  return base + rest + 1
})

const headerTitle = computed(() => props.dataset?.datasetName || '数据集详情')
const headerSci = computed(() => (props.dataset?.scienceCategories || []).join('、') || '-')
const headerInd = computed(() => (props.dataset?.industryCategories || []).join('、') || '-')
const headerCodes = computed(() => (props.dataset?.productCodes || []).join('、') || '-')
const headerDataCategory = computed(() => props.dataset?.dataCategory || '-')
const headerCount = computed(() => (props.dataset?.dataCount != null ? `${props.dataset.dataCount}条` : '-'))
const headerTemplate = computed(() => props.dataset?.templateName || '-')
const headerDept = computed(() => props.dataset?.department || '-')
const headerCreator = computed(() => props.dataset?.creator || '-')
const headerCreateTime = computed(() => props.dataset?.createTime || '-')
const headerAuditTime = computed(() => props.dataset?.updateTime || '-')
const headerLevel = computed(() => props.dataset?.dataLevel?.text || '公益')

function handleSearch() {
  loadRows()
}
function handleReset() {
  fieldTree.value = makeFieldTree()
  loadRows()
}

function onRowView(r) {
  console.log('查看行：', r)
}

async function loadRows() {
  loading.value = true
  try {
    // 先用 mock，后续接后端详情列表接口时替换
    const base = String(props.dataset?.id ?? '1')
    rows.value = Array.from({ length: 9 }).map((_, i) => ({
      id: `${base}-${i + 1}`,
      materialCode: `nHAp - ${String(i + 1).padStart(3, '0')}`,
      collector: '-',
      collectorOrg: '-',
      collectDate: '-',
      doi: '-',
      experimenter: i % 2 === 0 ? '史浩' : '史浩',
      favorited: false,
    }))
  } finally {
    loading.value = false
  }
}

onMounted(() => {
  loadRows()
})
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

