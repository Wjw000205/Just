<template>
  <section class="sci-page">
    <div class="sci-header">
      <div class="sci-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-parent">系统管理</span> &gt;
        <span class="crumb-now">产业分类管理</span>
      </div>
    </div>

    <section class="sci-main">
      <div class="main-title">产业分类管理</div>

      <div class="toolbar">
        <button type="button" class="tool-btn primary" @click="onAdd">新增目录</button>
        <button type="button" class="tool-btn ghost" @click="onRefresh">刷新</button>
      </div>

      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">目录名称</label>
          <input v-model="searchForm.name" class="filter-input" placeholder="请输入目录名称" />
        </div>
        <div class="filter-item">
          <label class="filter-label">目录编码</label>
          <input v-model="searchForm.code" class="filter-input" placeholder="请输入目录编码" />
        </div>
        <div class="filter-item">
          <label class="filter-label">目录来源</label>
          <div class="filter-select" @click.stop="sourceOpen = !sourceOpen">
            <span :class="['filter-select-text', { placeholder: !searchForm.source }]">
              {{ sourceLabel }}
            </span>
            <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
              <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
            </svg>
            <ul v-if="sourceOpen" class="filter-select-dropdown">
              <li class="filter-select-item" @click.stop="pickSource('')">全部</li>
              <li class="filter-select-item" @click.stop="pickSource('系统内置')">系统内置</li>
              <li class="filter-select-item" @click.stop="pickSource('同步')">同步</li>
              <li class="filter-select-item" @click.stop="pickSource('自建')">自建</li>
            </ul>
          </div>
        </div>
        <div class="filter-actions">
          <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
          <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
        </div>
      </div>

      <div class="table-wrap">
        <table class="data-table tree-table">
          <thead>
            <tr>
              <th class="col-idx">序号</th>
              <th class="col-name">目录名称</th>
              <th class="col-lv">目录等级</th>
              <th class="col-parent">所属目录</th>
              <th class="col-num">子目录数</th>
              <th class="col-num">目录数据集数</th>
              <th class="col-num">目录模板数</th>
              <th class="col-src">目录来源</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="row in visibleRows" :key="row.node.id">
              <td class="col-idx">{{ row.displayIndex != null ? row.displayIndex : '' }}</td>
              <td class="col-name">
                <div class="tree-name-cell" :style="{ paddingLeft: row.depth * 18 + 'px' }">
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
                  <span class="tree-name-text">{{ row.node.name }}</span>
                </div>
              </td>
              <td class="col-lv">{{ row.depth + 1 }}</td>
              <td class="col-parent">{{ row.parentName }}</td>
              <td class="col-num">{{ row.childCount }}</td>
              <td class="col-num">{{ row.node.datasetCount }}</td>
              <td class="col-num">{{ row.node.templateCount }}</td>
              <td class="col-src">{{ row.node.source }}</td>
              <td class="col-action">
                <div class="action-icons">
                  <button type="button" class="icon-action-btn" title="查看" @click="onView(row.node)">
                    <svg class="icon-svg icon-svg--primary" width="18" height="18" viewBox="0 0 24 24" fill="none">
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
                  <button type="button" class="icon-action-btn" title="新增子目录" @click="onAddChild(row.node)">
                    <svg class="icon-svg icon-svg--primary" width="18" height="18" viewBox="0 0 24 24" fill="none">
                      <path d="M5 4v12M5 13h9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                      <rect x="14" y="9" width="8" height="8" rx="1.2" stroke="currentColor" stroke-width="1.8" />
                      <path d="M11.5 5.5h3M13 4v3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                    </svg>
                  </button>
                  <button type="button" class="icon-action-btn" title="编辑" @click="onEdit(row.node)">
                    <svg class="icon-svg icon-svg--primary" width="18" height="18" viewBox="0 0 24 24" fill="none">
                      <path
                        d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                      <path
                        d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"
                        stroke="currentColor"
                        stroke-width="1.8"
                        stroke-linecap="round"
                        stroke-linejoin="round"
                      />
                    </svg>
                  </button>
                  <button type="button" class="icon-action-btn icon-action-btn--danger" title="删除" @click="onDelete(row.node)">
                    <svg class="icon-svg icon-svg--danger" width="18" height="18" viewBox="0 0 24 24" fill="none">
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home'])

/** 产业分类：嵌套 children 的树形列表（示例数据，后续可对接接口） */
const INITIAL_TREE = [
  {
    id: 'ind-bio',
    code: 'IND-BIO',
    name: '生物医用材料（产业）',
    sort: 1,
    datasetCount: 38,
    templateCount: 7,
    source: '系统内置',
    children: [
      {
        id: 'ind-bio-1',
        code: 'IND-BIO-COMMON',
        name: '通用',
        sort: 10,
        datasetCount: 11,
        templateCount: 3,
        source: '系统内置',
        children: [],
      },
      {
        id: 'ind-bio-2',
        code: 'IND-BIO-CHAIN',
        name: '产业链环节',
        sort: 20,
        datasetCount: 16,
        templateCount: 3,
        source: '同步',
        children: [
          {
            id: 'ind-bio-2-1',
            code: 'IND-BIO-UP',
            name: '上游原料与装备',
            sort: 21,
            datasetCount: 6,
            templateCount: 1,
            source: '同步',
            children: [],
          },
          {
            id: 'ind-bio-2-2',
            code: 'IND-BIO-MID',
            name: '中游制造',
            sort: 22,
            datasetCount: 5,
            templateCount: 1,
            source: '自建',
            children: [],
          },
        ],
      },
      {
        id: 'ind-bio-3',
        code: 'IND-BIO-SVC',
        name: '应用与服务',
        sort: 40,
        datasetCount: 4,
        templateCount: 1,
        source: '自建',
        children: [],
      },
    ],
  },
  {
    id: 'ind-it',
    code: 'IND-IT',
    name: '新一代信息技术产业',
    sort: 2,
    datasetCount: 22,
    templateCount: 4,
    source: '系统内置',
    children: [
      {
        id: 'ind-it-1',
        code: 'IND-IT-SW',
        name: '软件与信息服务',
        sort: 5,
        datasetCount: 10,
        templateCount: 2,
        source: '同步',
        children: [],
      },
      {
        id: 'ind-it-2',
        code: 'IND-IT-HW',
        name: '电子核心产业',
        sort: 6,
        datasetCount: 8,
        templateCount: 1,
        source: '自建',
        children: [],
      },
    ],
  },
]

function deepTree(src) {
  return JSON.parse(JSON.stringify(src))
}

function branchExpandedDefaults(nodes, acc = {}) {
  for (const n of nodes) {
    const kids = n.children
    if (Array.isArray(kids) && kids.length) {
      acc[n.id] = true
      branchExpandedDefaults(kids, acc)
    }
  }
  return acc
}

const categoryTree = ref(deepTree(INITIAL_TREE))
const tableExpanded = ref(branchExpandedDefaults(INITIAL_TREE))
const searchForm = ref({ name: '', code: '', source: '' })
const sourceOpen = ref(false)

function nodeMatchesFilter(n) {
  const { name, code, source } = searchForm.value
  const nameKw = name.trim().toLowerCase()
  const codeKw = code.trim().toLowerCase()
  if (nameKw && !String(n.name).toLowerCase().includes(nameKw)) return false
  if (codeKw && !String(n.code).toLowerCase().includes(codeKw)) return false
  if (source && String(n.source) !== source) return false
  return true
}

function branchVisibleInFilter(n) {
  if (nodeMatchesFilter(n)) return true
  const kids = n.children
  if (!Array.isArray(kids) || !kids.length) return false
  return kids.some(branchVisibleInFilter)
}

function isRowExpanded(id) {
  return tableExpanded.value[id] !== false
}

function toggleExpand(id) {
  tableExpanded.value = {
    ...tableExpanded.value,
    [id]: tableExpanded.value[id] === true ? false : true,
  }
}

function sortNodes(list) {
  return list.slice().sort((a, b) => a.sort - b.sort)
}

const visibleRows = computed(() => {
  const rows = []
  let rootOrdinal = 0

  function walk(list, depth, parentName) {
    for (const n of sortNodes(list)) {
      if (!branchVisibleInFilter(n)) continue

      const kids = Array.isArray(n.children) ? n.children : []
      const hasChildren = kids.length > 0
      const expanded = !hasChildren || isRowExpanded(n.id)

      rows.push({
        node: n,
        depth,
        parentName: parentName || '—',
        hasChildren,
        expanded,
        childCount: kids.length,
        /** 仅一级目录显示序号（1、2、3…），子级为空 */
        displayIndex: depth === 0 ? ++rootOrdinal : null,
      })

      if (hasChildren && expanded) walk(kids, depth + 1, n.name)
    }
  }

  walk(categoryTree.value, 0, '')
  return rows
})

const sourceLabel = computed(() => {
  if (!searchForm.value.source) return '全部'
  return searchForm.value.source
})

function pickSource(v) {
  searchForm.value.source = v
  sourceOpen.value = false
}

function handleSearch() {}

function handleReset() {
  searchForm.value = { name: '', code: '', source: '' }
}

function goHome() {
  emit('go-home')
}

function removeNodeById(list, id) {
  const idx = list.findIndex((n) => n.id === id)
  if (idx !== -1) {
    list.splice(idx, 1)
    return true
  }
  for (const n of list) {
    const kids = n.children
    if (Array.isArray(kids) && kids.length && removeNodeById(kids, id)) {
      if (kids.length === 0) n.children = []
      return true
    }
  }
  return false
}

function onAdd() {
  alert('新增目录：可对接表单或弹窗')
}

function onView(node) {
  alert(`查看目录：${node.name}（${node.code}）`)
}

function onAddChild(node) {
  alert(`在「${node.name}」下新增子目录：可对接表单`)
}

function onEdit(node) {
  alert(`编辑：${node.name}（${node.code}）`)
}

function onDelete(node) {
  if (!confirm(`确定删除「${node.name}」？`)) return
  removeNodeById(categoryTree.value, node.id)
}

function onRefresh() {
  categoryTree.value = deepTree(INITIAL_TREE)
  tableExpanded.value = branchExpandedDefaults(INITIAL_TREE)
  searchForm.value = { name: '', code: '', source: '' }
}

function onDocClick(e) {
  if (!e.target.closest?.('.filter-select')) {
    sourceOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<style scoped>
.sci-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.sci-header {
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

.sci-main {
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.tool-btn {
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #333;
}

.tool-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.tool-btn.primary:hover {
  background: #1246bb;
}

.tool-btn.ghost:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
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
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: 120px;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 13px;
  cursor: pointer;
}

.filter-select-text.placeholder {
  color: #999;
}

.filter-select-caret {
  margin-left: 6px;
  color: #999;
}

.filter-select-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  min-width: 100%;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 20;
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

.table-wrap {
  position: relative;
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 1020px;
}

.data-table thead th {
  padding: 10px 8px;
  text-align: center;
  font-weight: 500;
  color: #333;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  white-space: nowrap;
}

.data-table tbody td {
  padding: 8px;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
  vertical-align: middle;
  text-align: center;
}

.data-table tbody td.col-name {
  text-align: left;
}

.col-idx {
  width: 56px;
}

.col-lv {
  width: 72px;
}

.col-num {
  width: 96px;
}

.col-src {
  width: 88px;
}

.col-action {
  width: 140px;
}

.action-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
}

.icon-action-btn {
  display: inline-flex;
  align-items: center;
  justify-content: center;
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

.icon-svg--primary {
  color: #1a5ce6;
}

.icon-svg--danger {
  color: #cf1322;
}

.tree-name-cell {
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

.tree-name-text {
  line-height: 1.4;
}

.table-empty {
  padding: 40px;
  text-align: center;
  font-size: 13px;
  color: #999;
}
</style>
