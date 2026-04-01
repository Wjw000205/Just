<template>
  <section class="industry-page">
    <div class="industry-header">
      <div class="industry-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-parent">系统管理</span> &gt;
        <span class="crumb-now">产业分类管理</span>
      </div>
    </div>

    <section class="industry-main">
      <h1 class="page-title">产业分类管理</h1>

      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">产业分类</label>
          <input v-model="searchForm.industryClass" class="filter-input wide" placeholder="请输入产业分类" />
        </div>
        <div class="filter-item">
          <label class="filter-label">行业分类</label>
          <input v-model="searchForm.sectorClass" class="filter-input wide" placeholder="请输入行业分类" />
        </div>
        <div class="filter-item">
          <label class="filter-label">产品分类</label>
          <input v-model="searchForm.productClass" class="filter-input wide" placeholder="请输入产品分类" />
        </div>
        <div class="filter-actions">
          <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
          <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
        </div>
      </div>

      <div class="toolbar-row">
        <button type="button" class="tool-btn primary" @click="onFetch">获取</button>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-code-tree">产业代码</th>
              <th class="col-text">产业分类</th>
              <th class="col-code">行业代码</th>
              <th class="col-text">行业分类</th>
              <th class="col-code">产品代码</th>
              <th class="col-text col-product">产品分类</th>
              <th class="col-num">数据集数</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in visibleRows" :key="row.node.id">
              <td class="col-code-tree cell-tree">
                <div class="tree-cell" :style="{ paddingLeft: row.depth * 18 + 'px' }">
                  <button
                    v-if="row.hasChildren"
                    type="button"
                    class="tree-toggle"
                    :aria-expanded="row.expanded"
                    @click="toggleExpand(row.node.id)"
                  >
                    <svg class="tree-chevron" :class="{ open: row.expanded }" width="10" height="10" viewBox="0 0 10 10">
                      <path d="M3 1L7 5L3 9V1Z" fill="currentColor" />
                    </svg>
                  </button>
                  <span v-else class="tree-toggle-ph" />
                  <span class="mono">{{ row.node.industryCode }}</span>
                </div>
              </td>
              <td class="col-text cell-left">{{ row.node.industryName }}</td>
              <td class="col-code">{{ row.node.sectorCode || '—' }}</td>
              <td class="col-text">{{ row.node.sectorName || '—' }}</td>
              <td class="col-code">{{ row.node.productCode || '—' }}</td>
              <td class="col-text col-product">
                <span class="ellipsis" :title="row.node.productName || ''">{{ row.node.productName || '—' }}</span>
              </td>
              <td class="col-num">{{ row.node.datasetCount }}</td>
              <td class="col-action">
                <div class="action-icons">
                  <button type="button" class="icon-action-btn" title="查看" @click="onView(row.node)">
                            <svg class="icon-svg" width="18" height="18" viewBox="0 0 24 24" fill="none">
                              <path
                                d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                              />
                              <circle cx="12" cy="12" r="3" stroke="currentColor" stroke-width="1.8" />
                            </svg>
                  </button>
                  <button
                    type="button"
                    class="icon-action-btn icon-action-btn--danger"
                    title="删除"
                    @click="onDelete(row.node)"
                  >
                            <svg class="icon-svg" width="18" height="18" viewBox="0 0 24 24" fill="none">
                              <path d="M3 6h18" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                              <path
                                d="M8 6V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                              />
                              <path
                                d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6"
                                stroke="currentColor"
                                stroke-width="1.8"
                                stroke-linecap="round"
                                stroke-linejoin="round"
                              />
                              <path d="M10 11v6M14 11v6" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                            </svg>
                  </button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>
        <div v-if="!visibleRows.length" class="table-empty">暂无数据</div>
      </div>
    </section>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['go-home'])

/**
 * 产业分类树（示例，结构与接口返回类似）
 * industryCode：层级编号；叶子行可填行业/产品字段
 */
const INITIAL_TREE = [
  {
    id: 'i-3',
    industryCode: '3',
    industryName: '新材料产业',
    sectorCode: '',
    sectorName: '',
    productCode: '',
    productName: '',
    datasetCount: 0,
    children: [
      {
        id: 'i-3-1',
        industryCode: '3.1',
        industryName: '先进钢铁材料',
        sectorCode: '',
        sectorName: '',
        productCode: '',
        productName: '',
        datasetCount: 0,
        children: [
          {
            id: 'i-3-1-1',
            industryCode: '3.1.1',
            industryName: '先进制造基础零部件用钢制造',
            sectorCode: '',
            sectorName: '',
            productCode: '',
            productName: '',
            datasetCount: 0,
            children: [
              {
                id: 'i-3-1-1-1',
                industryCode: '3.1.1.1',
                industryName: '高品质特种钢材',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130003',
                productName:
                  '用于高端装备制造的合金结构钢、轴承钢、弹簧钢等高品质特种钢材材…（示例长文案省略号展示用）',
                datasetCount: 7,
                children: [],
              },
              {
                id: 'i-3-1-1-2',
                industryCode: '3.1.1.2',
                industryName: '特种薄板带钢',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130004',
                productName: '汽车用超高强度冷轧钢板',
                datasetCount: 0,
                children: [],
              },
            ],
          },
        ],
      },
    ],
  },
]

function deepTree(t) {
  return JSON.parse(JSON.stringify(t))
}

function branchExpandedDefaults(nodes, acc = {}) {
  for (const n of nodes) {
    if (n.children?.length) {
      acc[n.id] = true
      branchExpandedDefaults(n.children, acc)
    }
  }
  return acc
}

const treeData = ref(deepTree(INITIAL_TREE))
const tableExpanded = ref(branchExpandedDefaults(INITIAL_TREE))

const searchForm = ref({
  industryClass: '',
  sectorClass: '',
  productClass: '',
})

function nodeMatchesSearch(n) {
  const ic = searchForm.value.industryClass.trim().toLowerCase()
  const sc = searchForm.value.sectorClass.trim().toLowerCase()
  const pc = searchForm.value.productClass.trim().toLowerCase()
  if (ic && !String(n.industryName).toLowerCase().includes(ic)) return false
  if (sc && !String(n.sectorName || '').toLowerCase().includes(sc)) return false
  if (pc && !String(n.productName || '').toLowerCase().includes(pc)) return false
  return true
}

function branchMatches(n) {
  if (nodeMatchesSearch(n)) return true
  return (n.children || []).some(branchMatches)
}

function isExpanded(id) {
  return tableExpanded.value[id] !== false
}

function toggleExpand(id) {
  tableExpanded.value = {
    ...tableExpanded.value,
    [id]: tableExpanded.value[id] === true ? false : true,
  }
}

const visibleRows = computed(() => {
  const rows = []

  function walk(list, depth) {
    for (const n of list) {
      if (!branchMatches(n)) continue
      const kids = n.children || []
      const hasChildren = kids.length > 0
      const expanded = !hasChildren || isExpanded(n.id)
      rows.push({
        node: n,
        depth,
        hasChildren,
        expanded,
      })
      if (hasChildren && expanded) walk(kids, depth + 1)
    }
  }

  walk(treeData.value, 0)
  return rows
})

function handleSearch() {}

function handleReset() {
  searchForm.value = { industryClass: '', sectorClass: '', productClass: '' }
}

function goHome() {
  emit('go-home')
}

function onFetch() {
  treeData.value = deepTree(INITIAL_TREE)
  tableExpanded.value = branchExpandedDefaults(INITIAL_TREE)
  alert('已重新获取产业分类数据（示例）')
}

function onView(node) {
  alert(`查看：${node.industryCode} ${node.industryName}`)
}

function removeById(list, id) {
  const i = list.findIndex((n) => n.id === id)
  if (i !== -1) {
    list.splice(i, 1)
    return true
  }
  for (const n of list) {
    if (n.children?.length && removeById(n.children, id)) {
      if (!n.children.length) n.children = []
      return true
    }
  }
  return false
}

function onDelete(node) {
  if (!confirm(`确定删除「${node.industryCode} ${node.industryName}」及其子节点？`)) return
  removeById(treeData.value, node.id)
}
</script>

<style scoped>
.industry-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.industry-breadcrumb {
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

.crumb-parent,
.crumb-now {
  color: #333;
}

.crumb-now {
  font-weight: 500;
}

.industry-main {
  background: #fff;
  border-radius: 4px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.page-title {
  margin: 0 0 18px;
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 16px;
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
  border: 1px solid #d4dae6;
  border-radius: 4px;
  padding: 6px 10px;
  font-size: 13px;
}

.filter-input.wide {
  width: 180px;
}

.filter-actions {
  display: flex;
  gap: 8px;
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

.toolbar-row {
  margin-bottom: 12px;
}

.tool-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
}

.tool-btn:hover {
  background: #1246bb;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 1100px;
}

.data-table thead th {
  padding: 10px 8px;
  text-align: center;
  font-weight: 500;
  color: #333;
  background: #f5f7fa;
  border: 1px solid #e8e8e8;
  white-space: nowrap;
}

.data-table tbody td {
  padding: 8px;
  border: 1px solid #f0f0f0;
  color: #333;
  vertical-align: middle;
  text-align: center;
}

.cell-tree,
.cell-left {
  text-align: left;
}

.col-code-tree {
  min-width: 120px;
}

.col-text {
  max-width: 200px;
}

.col-product {
  max-width: 280px;
}

.col-code {
  min-width: 88px;
}

.col-num {
  width: 88px;
}

.col-action {
  width: 100px;
}

.tree-cell {
  display: flex;
  align-items: center;
  gap: 6px;
  min-height: 28px;
}

.tree-toggle {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
  flex-shrink: 0;
}

.tree-chevron {
  display: block;
  transition: transform 0.15s;
}

.tree-chevron.open {
  transform: rotate(90deg);
}

.tree-toggle-ph {
  width: 22px;
  flex-shrink: 0;
}

.mono {
  font-variant-numeric: tabular-nums;
}

.ellipsis {
  display: -webkit-box;
  -webkit-line-clamp: 2;
  -webkit-box-orient: vertical;
  overflow: hidden;
  text-align: left;
  line-height: 1.4;
}

.action-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.icon-action-btn {
  display: inline-flex;
  padding: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  border-radius: 4px;
  color: #1a5ce6;
}

.icon-action-btn:hover {
  background: #f0f5ff;
}

.icon-action-btn--danger {
  color: #cf1322;
}

.icon-action-btn--danger:hover {
  background: #fff2f0;
}

.table-empty {
  padding: 40px;
  text-align: center;
  color: #999;
  font-size: 13px;
}
</style>
