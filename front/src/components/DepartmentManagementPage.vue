<template>
  <section class="dept-page">
    <div class="dept-breadcrumb">
      当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
      <span class="crumb-parent">系统管理</span> &gt;
      <span class="crumb-now">部门管理</span>
    </div>

    <div class="dept-main">
      <div class="main-title-row">
        <span class="title-accent" aria-hidden="true" />
        <h1 class="main-title-text">部门管理</h1>
      </div>

      <div class="tabs">
        <button
          type="button"
          class="tab-btn tab-btn--left"
          :class="{ active: activeTab === 'config' }"
          @click="activeTab = 'config'"
        >
          部门配置
        </button>
        <button
          type="button"
          class="tab-btn tab-btn--right"
          :class="{ active: activeTab === 'audit' }"
          @click="activeTab = 'audit'"
        >
          部门审核
        </button>
      </div>

      <template v-if="activeTab === 'config'">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">部门名称</label>
            <input v-model="searchForm.name" class="filter-input" placeholder="请输入部门名称" />
          </div>
          <div class="filter-item">
            <label class="filter-label">部门等级</label>
            <div class="filter-select" @click.stop="levelOpen = !levelOpen">
              <span :class="['filter-select-text', { placeholder: !searchForm.level }]">
                {{ levelLabel }}
              </span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
              </svg>
              <ul v-if="levelOpen" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="pickLevel('')">请选择部门等级</li>
                <li class="filter-select-item" @click.stop="pickLevel('一级部门')">一级部门</li>
                <li class="filter-select-item" @click.stop="pickLevel('二级部门')">二级部门</li>
              </ul>
            </div>
          </div>
          <div class="filter-item">
            <label class="filter-label">所属部门</label>
            <div class="filter-select" @click.stop="parentOpen = !parentOpen">
              <span :class="['filter-select-text', { placeholder: !searchForm.parentId }]">
                {{ parentLabel }}
              </span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
              </svg>
              <ul v-if="parentOpen" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="pickParent('')">请选择</li>
                <li
                  v-for="opt in parentOptions"
                  :key="opt.value"
                  class="filter-select-item"
                  @click.stop="pickParent(opt.value)"
                >
                  {{ opt.label }}
                </li>
              </ul>
            </div>
          </div>
          <div class="filter-actions">
            <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
            <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
          </div>
        </div>

        <div class="toolbar-row">
          <button type="button" class="tool-btn primary" @click="onAdd">新增</button>
        </div>

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-name">部门名称</th>
                <th class="col-center">部门等级</th>
                <th class="col-center">所属部门</th>
                <th class="col-center">子部门数</th>
                <th class="col-center">部门用户数</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr
                v-for="row in visibleRows"
                :key="row.node.id"
                :class="row.depth === 0 ? 'row-root' : 'row-child'"
              >
                <td class="col-name cell-name">
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
                    <span>{{ row.node.name }}</span>
                  </div>
                </td>
                <td class="col-center">{{ row.node.levelLabel }}</td>
                <td class="col-center">{{ row.node.parentName }}</td>
                <td class="col-center">{{ row.childCount }}</td>
                <td class="col-center">{{ row.node.userCount }}</td>
                <td class="col-action">
                  <div class="action-icons">
                    <button
                      v-if="row.node.actions.view"
                      type="button"
                      class="icon-action-btn"
                      title="查看"
                      @click="onView(row.node)"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
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
                      v-if="row.node.actions.addSub"
                      type="button"
                      class="icon-action-btn"
                      title="新增子部门"
                      @click="onAddSub(row.node)"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                        <path d="M5 4v12M5 13h9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" />
                        <rect x="14" y="9" width="8" height="8" rx="1.2" stroke="currentColor" stroke-width="1.8" />
                        <path d="M11.5 5.5h3M13 4v3" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                      </svg>
                    </button>
                    <button
                      v-if="row.node.actions.edit"
                      type="button"
                      class="icon-action-btn"
                      title="编辑"
                      @click="onEdit(row.node)"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
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
                    <button
                      v-if="row.node.actions.delete"
                      type="button"
                      class="icon-action-btn icon-action-btn--danger"
                      title="删除"
                      @click="onDelete(row.node)"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
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

          <div v-if="visibleRows.length" class="pagination">
            <span class="pagination-total">共 {{ totalRootCount }} 条</span>
            <div class="pagination-nav">
              <button type="button" class="pagination-btn" :disabled="page <= 1" @click="goPage(page - 1)">上一页</button>
              <button
                v-for="p in pageNumbers"
                :key="p"
                type="button"
                class="page-num"
                :class="{ active: p === page }"
                @click="goPage(p)"
              >
                {{ p }}
              </button>
              <button type="button" class="pagination-btn" :disabled="page >= totalPages" @click="goPage(page + 1)">
                下一页
              </button>
            </div>
          </div>
        </div>
      </template>

      <template v-else>
        <div class="audit-filter-wrap">
          <div class="filter-row audit-filter-row">
            <div class="filter-item">
              <label class="filter-label">用户名</label>
              <input v-model="auditSearchForm.userName" class="filter-input" placeholder="请输入用户名" />
            </div>
            <div class="filter-item">
              <label class="filter-label">真实姓名</label>
              <input v-model="auditSearchForm.realName" class="filter-input" placeholder="请输入真实姓名" />
            </div>
            <div class="filter-item">
              <label class="filter-label">审核人</label>
              <input v-model="auditSearchForm.reviewer" class="filter-input" placeholder="请输入审核人" />
            </div>
            <div class="filter-item">
              <label class="filter-label">申请部门</label>
              <div class="filter-select" @click.stop="auditApplyDeptOpen = !auditApplyDeptOpen">
                <span :class="['filter-select-text', { placeholder: !auditSearchForm.applyDeptId }]">
                  {{ auditApplyDeptLabel }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="auditApplyDeptOpen" class="filter-select-dropdown">
                  <li
                    v-for="opt in auditApplyDeptOptions"
                    :key="opt.value || '_empty'"
                    class="filter-select-item"
                    @click.stop="pickAuditApplyDept(opt.value)"
                  >
                    {{ opt.label }}
                  </li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-row audit-filter-row">
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <div class="filter-select" @click.stop="auditStatusOpen = !auditStatusOpen">
                <span :class="['filter-select-text', { placeholder: !auditSearchForm.status }]">
                  {{ auditStatusLabel }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
                </svg>
                <ul v-if="auditStatusOpen" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="pickAuditStatus('')">请选择状态</li>
                  <li class="filter-select-item" @click.stop="pickAuditStatus('待审核')">待审核</li>
                  <li class="filter-select-item" @click.stop="pickAuditStatus('已通过')">已通过</li>
                  <li class="filter-select-item" @click.stop="pickAuditStatus('已驳回')">已驳回</li>
                </ul>
              </div>
            </div>
            <div class="filter-actions">
              <button type="button" class="search-btn primary" @click="handleAuditSearch">查询</button>
              <button type="button" class="search-btn ghost" @click="handleAuditReset">重置</button>
            </div>
          </div>
        </div>

        <div class="audit-batch-bar">
          <button type="button" class="btn-batch btn-batch--pass" @click="onBatchApprove">
            <span class="btn-batch-icon" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" fill="currentColor" opacity="0.95" />
                <path d="M7.5 12l3 3 5.5-6" stroke="#fff" stroke-width="2" stroke-linecap="round" stroke-linejoin="round" />
              </svg>
            </span>
            批量通过
          </button>
          <button type="button" class="btn-batch btn-batch--reject" @click="onBatchReject">
            <span class="btn-batch-icon" aria-hidden="true">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none">
                <circle cx="12" cy="12" r="10" fill="currentColor" opacity="0.95" />
                <path
                  d="M15 14.2C12.8 16.8 9 16.5 7 14a4 4 0 0 1 .2-5.2"
                  stroke="#fff"
                  stroke-width="1.7"
                  stroke-linecap="round"
                  fill="none"
                />
                <path
                  d="M7.5 10.5l-1 3.2 3.3-.2"
                  stroke="#fff"
                  stroke-width="1.7"
                  stroke-linecap="round"
                  stroke-linejoin="round"
                  fill="none"
                />
              </svg>
            </span>
            批量驳回
          </button>
        </div>

        <div class="table-wrap">
          <table class="data-table audit-table">
            <thead>
              <tr>
                <th class="col-audit-check">
                  <input type="checkbox" :checked="auditSelectAll" @change="onAuditSelectAll" />
                </th>
                <th class="col-audit-idx">序号</th>
                <th>用户名</th>
                <th>真实姓名</th>
                <th>申请部门</th>
                <th>申请理由</th>
                <th class="col-transfer">
                  <span class="th-with-hint">
                    是否转移数据
                    <abbr class="help-dot" title="是否将用户相关数据一并转移至目标部门">?</abbr>
                  </span>
                </th>
                <th>申请时间</th>
                <th class="col-audit-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in paginatedAuditRows" :key="row.id">
                <td class="col-audit-check">
                  <input type="checkbox" v-model="row.selected" />
                </td>
                <td class="col-audit-idx">{{ (auditPage - 1) * auditPageSize + index + 1 }}</td>
                <td>{{ row.userName }}</td>
                <td>{{ row.realName }}</td>
                <td>{{ row.applyDept }}</td>
                <td class="cell-reason">{{ row.reason }}</td>
                <td class="col-transfer">{{ row.transferData ? '是' : '否' }}</td>
                <td class="cell-time">{{ row.applyTime }}</td>
                <td class="col-audit-action">
                  <div class="action-icons audit-row-actions">
                    <button type="button" class="icon-action-btn" title="查看" @click="onAuditView(row)">
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
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
                      v-if="row.status === '待审核'"
                      type="button"
                      class="icon-action-btn icon-action-btn--pass"
                      title="通过"
                      @click="onAuditApproveRow(row)"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                        <path d="M8 12l2.5 2.5L16 9" stroke="currentColor" stroke-width="1.8" stroke-linecap="round" stroke-linejoin="round" />
                      </svg>
                    </button>
                    <button
                      v-if="row.status === '待审核'"
                      type="button"
                      class="icon-action-btn icon-action-btn--reject"
                      title="驳回"
                      @click="onAuditRejectRow(row)"
                    >
                      <svg width="18" height="18" viewBox="0 0 24 24" fill="none">
                        <circle cx="12" cy="12" r="9" stroke="currentColor" stroke-width="1.6" />
                        <path
                          d="M15 9.5c-1.8-1.2-4.3-.9-5.8.7a4.2 4.2 0 0 0 0 5.8c1.5 1.6 4 1.9 5.8.7"
                          stroke="currentColor"
                          stroke-width="1.6"
                          stroke-linecap="round"
                          fill="none"
                        />
                        <path d="M9.5 14.5L15 9" stroke="currentColor" stroke-width="1.5" stroke-linecap="round" />
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>
          <div v-if="!paginatedAuditRows.length" class="table-empty">暂无数据</div>

          <div v-if="filteredAuditRows.length" class="pagination">
            <span class="pagination-total">共 {{ filteredAuditRows.length }} 条</span>
            <div class="pagination-nav">
              <button type="button" class="pagination-btn" :disabled="auditPage <= 1" @click="goAuditPage(auditPage - 1)">
                上一页
              </button>
              <button
                v-for="p in auditPageNumbers"
                :key="p"
                type="button"
                class="page-num"
                :class="{ active: p === auditPage }"
                @click="goAuditPage(p)"
              >
                {{ p }}
              </button>
              <button
                type="button"
                class="pagination-btn"
                :disabled="auditPage >= auditTotalPages"
                @click="goAuditPage(auditPage + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </template>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home'])

const DEFAULT_ACTIONS_FULL = { view: true, addSub: true, edit: true, delete: true }

function cloneTree(nodes) {
  return JSON.parse(JSON.stringify(nodes))
}

const INITIAL_TREE = [
  {
    id: 'pub',
    name: '社会大众',
    levelLabel: '一级部门',
    parentName: '—',
    userCount: 128,
    actions: { view: true, addSub: false, edit: false, delete: false },
    children: [],
  },
  {
    id: 'node-mgmt',
    name: '节点管理部',
    levelLabel: '一级部门',
    parentName: '—',
    userCount: 56,
    actions: { view: true, addSub: true, edit: true, delete: false },
    children: [
      {
        id: 'zw',
        name: '知网',
        levelLabel: '二级部门',
        parentName: '节点管理部',
        userCount: 22,
        actions: { ...DEFAULT_ACTIONS_FULL },
        children: [],
      },
      {
        id: 'csu',
        name: '中南大学',
        levelLabel: '二级部门',
        parentName: '节点管理部',
        userCount: 18,
        actions: { ...DEFAULT_ACTIONS_FULL },
        children: [],
      },
    ],
  },
]

function branchExpandedDefaults(nodes, acc = {}) {
  for (const n of nodes) {
    if (n.children?.length) {
      acc[n.id] = true
      branchExpandedDefaults(n.children, acc)
    }
  }
  return acc
}

function flattenDeptOptions(nodes, prefix = '') {
  const out = []
  for (const n of nodes) {
    out.push({ value: n.id, label: prefix + n.name })
    if (n.children?.length) out.push(...flattenDeptOptions(n.children, prefix + n.name + ' / '))
  }
  return out
}

function findSubtreeAsRoots(nodes, targetId) {
  for (const n of nodes) {
    if (n.id === targetId) return [cloneTree([n])[0]]
    if (n.children?.length) {
      const found = findSubtreeAsRoots(n.children, targetId)
      if (found.length) return found
    }
  }
  return []
}

function pruneBySearch(nodes, form) {
  const nameKw = form.name.trim().toLowerCase()
  const level = form.level

  function nodeMatches(n) {
    if (nameKw && !String(n.name).toLowerCase().includes(nameKw)) return false
    if (level && n.levelLabel !== level) return false
    return true
  }

  function walk(list) {
    const out = []
    for (const n of list) {
      const children = walk(n.children || [])
      if (nodeMatches(n) || children.length) {
        out.push({ ...n, children })
      }
    }
    return out
  }

  return walk(nodes)
}

const activeTab = ref('config')
const searchForm = ref({ name: '', level: '', parentId: '' })
const levelOpen = ref(false)
const parentOpen = ref(false)

const page = ref(1)
const pageSize = ref(10)

const sourceTree = ref(cloneTree(INITIAL_TREE))
const expanded = ref(branchExpandedDefaults(INITIAL_TREE))

const parentOptions = computed(() => flattenDeptOptions(INITIAL_TREE))

const levelLabel = computed(() => {
  if (!searchForm.value.level) return '请选择部门等级'
  return searchForm.value.level
})

const parentLabel = computed(() => {
  if (!searchForm.value.parentId) return '请选择'
  const o = parentOptions.value.find((x) => x.value === searchForm.value.parentId)
  return o ? o.label : '请选择'
})

const filteredRoots = computed(() => {
  let roots = pruneBySearch(sourceTree.value, searchForm.value)
  if (searchForm.value.parentId) {
    const sub = findSubtreeAsRoots(roots, searchForm.value.parentId)
    roots = sub.length ? sub : []
  }
  return roots
})

const totalRootCount = computed(() => filteredRoots.value.length)

const totalPages = computed(() => Math.max(1, Math.ceil(totalRootCount.value / pageSize.value)))

const pageNumbers = computed(() => {
  const total = totalPages.value
  const cur = page.value
  const list = []
  const maxShow = 5
  let start = Math.max(1, cur - 2)
  let end = Math.min(total, start + maxShow - 1)
  if (end - start < maxShow - 1) start = Math.max(1, end - maxShow + 1)
  for (let i = start; i <= end; i++) list.push(i)
  return list
})

const pagedRoots = computed(() => {
  const start = (page.value - 1) * pageSize.value
  return filteredRoots.value.slice(start, start + pageSize.value)
})

function isExpanded(id) {
  return expanded.value[id] !== false
}

function toggleExpand(id) {
  expanded.value = {
    ...expanded.value,
    [id]: expanded.value[id] === true ? false : true,
  }
}

const visibleRows = computed(() => {
  const rows = []
  function walk(list, depth) {
    for (const n of list) {
      const kids = n.children || []
      const hasChildren = kids.length > 0
      const exp = !hasChildren || isExpanded(n.id)
      rows.push({
        node: n,
        depth,
        hasChildren,
        expanded: exp,
        childCount: kids.length,
      })
      if (hasChildren && exp) walk(kids, depth + 1)
    }
  }
  walk(pagedRoots.value, 0)
  return rows
})

watch(
  () => [searchForm.value.name, searchForm.value.level, searchForm.value.parentId],
  () => {
    page.value = 1
  },
)

watch(totalRootCount, () => {
  if (page.value > totalPages.value) page.value = totalPages.value
})

function pickLevel(v) {
  searchForm.value.level = v
  levelOpen.value = false
}

function pickParent(v) {
  searchForm.value.parentId = v
  parentOpen.value = false
}

function handleSearch() {
  page.value = 1
}

function handleReset() {
  searchForm.value = { name: '', level: '', parentId: '' }
  page.value = 1
}

function goPage(p) {
  if (p < 1 || p > totalPages.value) return
  page.value = p
}

function goHome() {
  emit('go-home')
}

function onDocClick(e) {
  if (!e.target.closest?.('.filter-select')) {
    levelOpen.value = false
    parentOpen.value = false
    auditStatusOpen.value = false
    auditApplyDeptOpen.value = false
  }
}

/** —— 部门审核（与用户截图一致） —— */
const auditApplyDeptOptions = [
  { value: '', label: '请选择' },
  { value: 'pub', label: '社会大众' },
  { value: 'node-mgmt', label: '节点管理部' },
  { value: 'zw', label: '知网' },
  { value: 'csu', label: '中南大学' },
]

const AUDIT_SEED = [
  {
    id: 'a1',
    userName: 'haha111',
    realName: '谢飞',
    applyDept: '节点管理部',
    applyDeptId: 'node-mgmt',
    reason: 'test',
    transferData: false,
    applyTime: '2026-03-25 23:25:34',
    status: '待审核',
    reviewer: '王审核',
    selected: false,
  },
  {
    id: 'a2',
    userName: 'zhuxiangdong',
    realName: '朱向东',
    applyDept: '社会大众',
    applyDeptId: 'pub',
    reason: '11',
    transferData: false,
    applyTime: '2026-03-26 14:18:22',
    status: '待审核',
    reviewer: '李审核',
    selected: false,
  },
  {
    id: 'a3',
    userName: 'zhoumin',
    realName: '周敏',
    applyDept: '节点管理部',
    applyDeptId: 'node-mgmt',
    reason: '组织调整',
    transferData: false,
    applyTime: '2026-03-27 09:05:11',
    status: '待审核',
    reviewer: '王审核',
    selected: false,
  },
]

const auditRecords = ref(AUDIT_SEED.map((r) => ({ ...r })))
const auditSearchForm = ref({
  userName: '',
  realName: '',
  reviewer: '',
  applyDeptId: '',
  status: '',
})
const auditStatusOpen = ref(false)
const auditApplyDeptOpen = ref(false)
const auditPage = ref(1)
const auditPageSize = ref(10)

const auditApplyDeptLabel = computed(() => {
  const opt = auditApplyDeptOptions.find((o) => o.value === auditSearchForm.value.applyDeptId)
  return opt ? opt.label : '请选择'
})

const auditStatusLabel = computed(() => {
  if (!auditSearchForm.value.status) return '请选择状态'
  return auditSearchForm.value.status
})

const filteredAuditRows = computed(() => {
  let list = auditRecords.value
  const u = auditSearchForm.value.userName.trim().toLowerCase()
  const rn = auditSearchForm.value.realName.trim().toLowerCase()
  const rv = auditSearchForm.value.reviewer.trim().toLowerCase()
  const deptId = auditSearchForm.value.applyDeptId
  const st = auditSearchForm.value.status
  if (u) list = list.filter((r) => String(r.userName).toLowerCase().includes(u))
  if (rn) list = list.filter((r) => String(r.realName).toLowerCase().includes(rn))
  if (rv) list = list.filter((r) => String(r.reviewer || '').toLowerCase().includes(rv))
  if (deptId) list = list.filter((r) => r.applyDeptId === deptId)
  if (st) list = list.filter((r) => r.status === st)
  return list
})

const auditTotalPages = computed(() =>
  Math.max(1, Math.ceil(filteredAuditRows.value.length / auditPageSize.value)),
)

const auditPageNumbers = computed(() => {
  const total = auditTotalPages.value
  const cur = auditPage.value
  const list = []
  const maxShow = 5
  let start = Math.max(1, cur - 2)
  let end = Math.min(total, start + maxShow - 1)
  if (end - start < maxShow - 1) start = Math.max(1, end - maxShow + 1)
  for (let i = start; i <= end; i++) list.push(i)
  return list
})

const paginatedAuditRows = computed(() => {
  const start = (auditPage.value - 1) * auditPageSize.value
  return filteredAuditRows.value.slice(start, start + auditPageSize.value)
})

watch(
  () => [
    auditSearchForm.value.userName,
    auditSearchForm.value.realName,
    auditSearchForm.value.reviewer,
    auditSearchForm.value.applyDeptId,
    auditSearchForm.value.status,
  ],
  () => {
    auditPage.value = 1
  },
)

const auditSelectAll = computed({
  get() {
    const rows = paginatedAuditRows.value
    return rows.length > 0 && rows.every((r) => r.selected)
  },
  set() {},
})

function onAuditSelectAll(e) {
  const checked = e.target.checked
  paginatedAuditRows.value.forEach((r) => {
    r.selected = checked
  })
}

watch(filteredAuditRows, () => {
  if (auditPage.value > auditTotalPages.value) auditPage.value = auditTotalPages.value
})

function pickAuditStatus(v) {
  auditSearchForm.value.status = v
  auditStatusOpen.value = false
}

function pickAuditApplyDept(v) {
  auditSearchForm.value.applyDeptId = v
  auditApplyDeptOpen.value = false
}

function handleAuditSearch() {
  auditPage.value = 1
}

function handleAuditReset() {
  auditSearchForm.value = {
    userName: '',
    realName: '',
    reviewer: '',
    applyDeptId: '',
    status: '',
  }
  auditPage.value = 1
}

function goAuditPage(p) {
  if (p < 1 || p > auditTotalPages.value) return
  auditPage.value = p
}

function onAuditView(row) {
  alert(
    `申请用户：${row.userName}（${row.realName}）\n申请部门：${row.applyDept}\n理由：${row.reason}\n时间：${row.applyTime}\n状态：${row.status}`,
  )
}

function onAuditApproveRow(row) {
  const rec = auditRecords.value.find((r) => r.id === row.id)
  if (!rec || rec.status !== '待审核') return
  if (!confirm(`确定通过「${row.userName}」的加入「${row.applyDept}」申请？`)) return
  rec.status = '已通过'
  rec.selected = false
}

function onAuditRejectRow(row) {
  const rec = auditRecords.value.find((r) => r.id === row.id)
  if (!rec || rec.status !== '待审核') return
  const reason = prompt('驳回原因（可留空）', '')
  if (reason === null) return
  rec.status = '已驳回'
  rec.selected = false
}

function onBatchApprove() {
  const sel = auditRecords.value.filter((r) => r.selected && r.status === '待审核')
  if (!sel.length) {
    alert('请先勾选需要批量通过的待审核记录')
    return
  }
  if (!confirm(`确定批量通过 ${sel.length} 条申请？`)) return
  sel.forEach((r) => {
    r.status = '已通过'
    r.selected = false
  })
}

function onBatchReject() {
  const sel = auditRecords.value.filter((r) => r.selected && r.status === '待审核')
  if (!sel.length) {
    alert('请先勾选需要批量驳回的待审核记录')
    return
  }
  const reason = prompt(`将批量驳回 ${sel.length} 条申请，驳回原因（可留空）`, '')
  if (reason === null) return
  sel.forEach((r) => {
    r.status = '已驳回'
    r.selected = false
  })
}

function onAdd() {
  alert('新增部门')
}

function onAddSub(node) {
  alert(`在「${node.name}」下新增子部门`)
}

function onView(node) {
  alert(`查看部门：${node.name}`)
}

function onEdit(node) {
  alert(`编辑部门：${node.name}`)
}

function onDelete(node) {
  if (!confirm(`确定删除「${node.name}」？`)) return
  alert('已提交删除（示例）')
}

onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<style scoped>
.dept-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.dept-breadcrumb {
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

.dept-main {
  background: #fff;
  border-radius: 4px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.main-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 16px;
}

.title-accent {
  width: 4px;
  height: 18px;
  background: #1a5ce6;
  border-radius: 2px;
}

.main-title-text {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.tabs {
  display: inline-flex;
  margin-bottom: 18px;
}

.tab-btn {
  padding: 8px 22px;
  font-size: 14px;
  cursor: pointer;
  border: 1px solid #1a5ce6;
  background: #fff;
  color: #1a5ce6;
  margin-left: -1px;
}

.tab-btn--left {
  border-radius: 4px 0 0 4px;
  margin-left: 0;
}

.tab-btn--right {
  border-radius: 0 4px 4px 0;
}

.tab-btn.active {
  background: #1a5ce6;
  color: #fff;
  z-index: 1;
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
  padding: 6px 10px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 13px;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: 140px;
  padding: 6px 10px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
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
  right: 0;
  margin: 0;
  padding: 4px 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 30;
  max-height: 240px;
  overflow-y: auto;
}

.filter-select-item {
  padding: 8px 12px;
  font-size: 13px;
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

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 920px;
}

.data-table thead th {
  padding: 10px 8px;
  text-align: center;
  font-weight: 500;
  background: #fafafa;
  border: 1px solid #e8e8e8;
  color: #333;
}

.data-table tbody td {
  padding: 10px 8px;
  border: 1px solid #f0f0f0;
  vertical-align: middle;
}

.row-root {
  background: #f5f5f5;
}

.row-child {
  background: #fff;
}

.cell-name {
  text-align: left;
}

.col-center {
  text-align: center;
  color: #333;
}

.col-name {
  min-width: 200px;
}

.col-action {
  width: 140px;
  text-align: center;
}

.tree-cell {
  display: flex;
  align-items: center;
  gap: 6px;
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

.action-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
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

.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-top: 16px;
  font-size: 13px;
  color: #666;
}

.pagination-nav {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pagination-btn {
  padding: 4px 12px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}

.pagination-btn:disabled {
  opacity: 0.5;
  cursor: not-allowed;
}

.page-num {
  min-width: 32px;
  height: 28px;
  padding: 0 8px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  background: #fff;
  cursor: pointer;
  font-size: 13px;
}

.page-num.active {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.audit-filter-wrap {
  margin-bottom: 8px;
}

.audit-filter-row {
  margin-bottom: 12px;
}

.audit-batch-bar {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 12px;
}

.btn-batch {
  display: inline-flex;
  align-items: center;
  gap: 8px;
  padding: 6px 16px;
  border: none;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 500;
  cursor: pointer;
  color: #fff;
}

.btn-batch--pass {
  background: #52c41a;
}

.btn-batch--pass:hover {
  background: #389e0d;
}

.btn-batch--reject {
  background: #ff7875;
}

.btn-batch--reject:hover {
  background: #ff4d4f;
}

.btn-batch-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  color: inherit;
}

.audit-table td,
.audit-table th {
  text-align: center;
}

.audit-table .cell-reason {
  max-width: 160px;
  word-break: break-all;
}

.col-audit-check {
  width: 44px;
}

.col-audit-idx {
  width: 56px;
}

.col-transfer {
  min-width: 120px;
}

.th-with-hint {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  gap: 2px;
}

.help-dot {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 14px;
  height: 14px;
  margin-left: 2px;
  border-radius: 50%;
  background: #faad14;
  color: #fff;
  font-size: 11px;
  font-weight: 700;
  line-height: 1;
  cursor: help;
  text-decoration: none;
}

.col-audit-action {
  width: 120px;
  white-space: nowrap;
}

.cell-time {
  white-space: nowrap;
  font-variant-numeric: tabular-nums;
}

.audit-row-actions {
  gap: 6px;
}

.icon-action-btn--pass {
  color: #52c41a;
}

.icon-action-btn--pass:hover {
  background: #f6ffed;
}

.icon-action-btn--reject {
  color: #ff4d4f;
}

.icon-action-btn--reject:hover {
  background: #fff2f0;
}
</style>
