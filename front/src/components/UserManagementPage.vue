<template>
  <section class="user-page">
    <div class="user-header">
      <div class="user-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-parent">系统管理</span> &gt;
        <span class="crumb-now">用户管理</span>
      </div>
    </div>

    <div class="user-layout">
      <aside class="user-sidebar">
        <div class="sidebar-search-row">
          <div class="sidebar-search">
            <input v-model="deptKeyword" class="sidebar-input" placeholder="请输入部门名称" />
            <button type="button" class="sidebar-search-btn" aria-label="搜索">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8" />
                <path d="M21 21l-4.35-4.35" />
              </svg>
            </button>
          </div>
          <button type="button" class="sidebar-filter-btn" aria-label="筛选">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="3" y1="6" x2="21" y2="6" />
              <line x1="3" y1="12" x2="21" y2="12" />
              <line x1="3" y1="18" x2="21" y2="18" />
            </svg>
          </button>
        </div>

        <ul class="sidebar-tree">
          <li
            class="tree-all"
            :class="{ active: activeDeptId === 'all' }"
            @click="selectDept('all')"
          >
            全部
          </li>
          <template v-if="filteredDeptTree.length">
            <template v-for="node in filteredDeptTree" :key="node.id">
              <li
                v-if="!node.children?.length"
                class="dept-item dept-leaf"
                :class="{ active: activeDeptId === node.id }"
                @click="selectDept(node.id)"
              >
                {{ node.label }}
              </li>
              <template v-else>
                <li class="dept-item dept-parent-row">
                  <button
                    type="button"
                    class="dept-chevron"
                    :class="{ 'is-open': deptExpanded[node.id] }"
                    aria-label="展开或收起"
                    @click.stop="toggleDeptExpand(node.id)"
                  >
                    <svg width="8" height="8" viewBox="0 0 10 10" fill="currentColor" aria-hidden="true">
                      <path d="M3 1L7 5L3 9V1Z" />
                    </svg>
                  </button>
                  <span
                    class="dept-parent-label"
                    :class="{ active: activeDeptId === node.id }"
                    @click="selectDept(node.id)"
                  >
                    {{ node.label }}
                  </span>
                </li>
                <ul v-show="deptExpanded[node.id]" class="dept-children">
                  <li
                    v-for="c in node.children"
                    :key="c.id"
                    class="dept-item dept-leaf dept-leaf-sub"
                    :class="{ active: activeDeptId === c.id }"
                    @click="selectDept(c.id)"
                  >
                    {{ c.label }}
                  </li>
                </ul>
              </template>
            </template>
          </template>
          <li v-else class="sidebar-empty">无匹配部门</li>
        </ul>
      </aside>

      <section class="user-main">
        <div class="main-title-row">
          <span class="title-accent" aria-hidden="true" />
          <h2 class="main-title-text">用户管理</h2>
        </div>

        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">用户名称</label>
            <input v-model="searchForm.userName" class="filter-input" placeholder="请输入用户名称" />
          </div>
          <div class="filter-item">
            <label class="filter-label">真实姓名</label>
            <input v-model="searchForm.realName" class="filter-input" placeholder="请输入真实姓名" />
          </div>
          <div class="filter-item">
            <label class="filter-label">状态</label>
            <div class="filter-select" @click.stop="statusOpen = !statusOpen">
              <span :class="['filter-select-text', { placeholder: !searchForm.status }]">
                {{ statusLabel }}
              </span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
              </svg>
              <ul v-if="statusOpen" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="pickStatus('')">请选择状态</li>
                <li class="filter-select-item" @click.stop="pickStatus('启用')">启用</li>
                <li class="filter-select-item" @click.stop="pickStatus('停用')">停用</li>
              </ul>
            </div>
          </div>
          <div class="filter-actions">
            <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
            <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
          </div>
        </div>

        <div class="toolbar">
          <button type="button" class="tool-btn primary" @click="onAdd">新增</button>
          <button type="button" class="tool-btn outline" @click="onBatchAdd">批量新增</button>
        </div>

        <div class="table-wrap">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-idx">序号</th>
                <th class="col-user">用户名</th>
                <th class="col-name">真实姓名</th>
                <th class="col-phone">手机号</th>
                <th class="col-email">邮箱</th>
                <th class="col-role">权限角色</th>
                <th class="col-dept">所属部门</th>
                <th class="col-status">状态</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(row, index) in filteredUsers" :key="row.id">
                <td>{{ index + 1 }}</td>
                <td>{{ row.userName }}</td>
                <td>{{ row.realName }}</td>
                <td>{{ row.phone }}</td>
                <td class="cell-email">{{ row.email }}</td>
                <td>
                  <span v-for="r in row.roles" :key="r" class="role-tag">{{ r }}</span>
                </td>
                <td>{{ row.department }}</td>
                <td>
                  <span :class="row.status === '启用' ? 'status-on' : 'status-off'">{{ row.status }}</span>
                </td>
                <td>
                  <div class="action-icons">
                    <button type="button" class="icon-action-btn" title="查看" @click="onView(row)">
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
                    <button type="button" class="icon-action-btn" title="编辑" @click="onEdit(row)">
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
                    <button type="button" class="icon-action-btn" title="重置密码" @click="onResetPwd(row)">
                      <svg class="icon-svg icon-svg--primary" width="18" height="18" viewBox="0 0 24 24" fill="none">
                        <rect x="5" y="11" width="14" height="10" rx="2" stroke="currentColor" stroke-width="1.8" />
                        <path
                          d="M8 11V7a4 4 0 0 1 8 0v4"
                          stroke="currentColor"
                          stroke-width="1.8"
                          stroke-linecap="round"
                          fill="none"
                        />
                      </svg>
                    </button>
                    <button type="button" class="icon-action-btn icon-action-btn--danger" title="删除" @click="onDelete(row)">
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
          <div v-if="!filteredUsers.length" class="table-empty">暂无数据</div>
          <div v-else class="table-footer">共 {{ filteredUsers.length }} 条</div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, watch, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home'])

const DEPT_TREE = [
  {
    id: 'pub',
    label: '社会大众',
    children: [],
  },
  {
    id: 'node-mgmt',
    label: '节点管理部',
    children: [
      { id: 'node-ops', label: '运维组', children: [] },
      { id: 'node-audit', label: '审核组', children: [] },
    ],
  },
]

const ALL_USERS = [
  {
    id: 1,
    userName: 'SICCAS-111',
    realName: '李义伟',
    phone: '13812345678',
    email: 'very-long-example-address-name@institution.org.cn',
    roles: ['普通角色'],
    department: '社会大众',
    deptId: 'pub',
    status: '启用',
  },
  {
    id: 2,
    userName: 'LiYiwei',
    realName: '李义伟',
    phone: '13987654321',
    email: 'liyiwei@gmail.com',
    roles: ['普通角色'],
    department: '节点管理部',
    deptId: 'node-mgmt',
    status: '启用',
  },
  {
    id: 3,
    userName: 'admin-node',
    realName: '王管理',
    phone: '13600001111',
    email: 'wang@node.gov.cn',
    roles: ['普通角色', '审核员'],
    department: '节点管理部',
    deptId: 'node-mgmt',
    status: '启用',
  },
  {
    id: 4,
    userName: 'guest-01',
    realName: '赵访客',
    phone: '13500002222',
    email: 'guest@example.com',
    roles: ['普通角色'],
    department: '运维组',
    deptId: 'node-ops',
    status: '停用',
  },
]

function pruneDeptTree(nodes, kw) {
  if (!kw.trim()) return nodes
  const lower = kw.trim().toLowerCase()
  const walk = (list) => {
    const out = []
    for (const n of list) {
      const hit = String(n.label).toLowerCase().includes(lower)
      const kids = walk(n.children || [])
      if (hit || kids.length) out.push({ ...n, children: kids })
    }
    return out
  }
  return walk(nodes)
}

function collectExpandedDept(nodes, acc = {}) {
  for (const n of nodes) {
    if (n.children?.length) {
      acc[n.id] = true
      collectExpandedDept(n.children, acc)
    }
  }
  return acc
}

function subtreeDeptIds(tree, targetId) {
  const find = (nodes) => {
    for (const n of nodes) {
      if (n.id === targetId) return n
      const sub = find(n.children || [])
      if (sub) return sub
    }
    return null
  }
  const node = find(tree)
  if (!node) return new Set([targetId])
  const set = new Set()
  const walk = (n) => {
    set.add(n.id)
    ;(n.children || []).forEach(walk)
  }
  walk(node)
  return set
}

const deptKeyword = ref('')
const filteredDeptTree = computed(() => pruneDeptTree(DEPT_TREE, deptKeyword.value))

const deptExpanded = ref(collectExpandedDept(DEPT_TREE))

watch(
  () => deptKeyword.value,
  () => {
    deptExpanded.value = { ...deptExpanded.value, ...collectExpandedDept(filteredDeptTree.value) }
  },
)

const activeDeptId = ref('all')

function selectDept(id) {
  activeDeptId.value = id
}

function toggleDeptExpand(id) {
  deptExpanded.value = {
    ...deptExpanded.value,
    [id]: deptExpanded.value[id] !== true,
  }
}

const searchForm = ref({ userName: '', realName: '', status: '' })
const statusOpen = ref(false)

const statusLabel = computed(() => {
  if (!searchForm.value.status) return '请选择状态'
  return searchForm.value.status
})

const filteredUsers = computed(() => {
  let list = ALL_USERS

  if (activeDeptId.value === 'all') {
    // 展示全部用户
  } else {
    const allowed = subtreeDeptIds(DEPT_TREE, activeDeptId.value)
    list = list.filter((u) => allowed.has(u.deptId))
  }

  const { userName, realName, status } = searchForm.value
  const uKw = userName.trim().toLowerCase()
  const rKw = realName.trim().toLowerCase()
  if (uKw) list = list.filter((u) => String(u.userName).toLowerCase().includes(uKw))
  if (rKw) list = list.filter((u) => String(u.realName).toLowerCase().includes(rKw))
  if (status) list = list.filter((u) => u.status === status)

  return list
})

function pickStatus(v) {
  searchForm.value.status = v
  statusOpen.value = false
}

function handleSearch() {}

function handleReset() {
  searchForm.value = { userName: '', realName: '', status: '' }
}

function goHome() {
  emit('go-home')
}

function onDocClick(e) {
  if (!e.target.closest?.('.filter-select')) statusOpen.value = false
}

function onAdd() {
  alert('新增用户')
}

function onBatchAdd() {
  alert('批量新增用户')
}

function onView(row) {
  alert(`查看用户：${row.userName}`)
}

function onEdit(row) {
  alert(`编辑用户：${row.userName}`)
}

function onResetPwd(row) {
  if (!confirm(`确定重置用户「${row.userName}」的密码？`)) return
  alert('已提交重置密码（示例）')
}

function onDelete(row) {
  if (!confirm(`确定删除用户「${row.userName}」？`)) return
  alert('删除请求已提交（示例）')
}

onMounted(() => document.addEventListener('click', onDocClick))
onBeforeUnmount(() => document.removeEventListener('click', onDocClick))
</script>

<style scoped>
.user-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.user-breadcrumb {
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

.user-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: start;
}

.user-sidebar {
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
  flex: 1;
  align-items: center;
  gap: 6px;
  min-width: 0;
}

.sidebar-input {
  flex: 1;
  min-width: 0;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 8px 10px;
  font-size: 13px;
}

.sidebar-search-btn,
.sidebar-filter-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 32px;
  height: 32px;
  padding: 0;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  flex-shrink: 0;
}

.sidebar-tree {
  list-style: none;
  padding: 0;
  margin: 0;
}

.tree-all {
  padding: 10px 8px;
  margin: 0 -8px 4px;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  border-left: 3px solid transparent;
}

.tree-all:hover {
  background: #fafafa;
}

.tree-all.active {
  background: #e8f4ff;
  color: #1a5ce6;
  font-weight: 500;
  border-left-color: #1a5ce6;
}

.sidebar-empty {
  padding: 12px 8px;
  font-size: 13px;
  color: #999;
}

.dept-item {
  font-size: 13px;
  color: #666;
  cursor: pointer;
  list-style: none;
}

.dept-leaf {
  padding: 10px 8px;
  margin: 0 -8px;
  border-radius: 4px;
  border-left: 3px solid transparent;
}

.dept-leaf:hover {
  background: #fafafa;
}

.dept-leaf.active {
  background: #e8f4ff;
  color: #1a5ce6;
  font-weight: 500;
  border-left-color: #1a5ce6;
}

.dept-parent-row {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 8px 8px 8px 4px;
  margin: 0 -8px;
  border-radius: 4px;
}

.dept-parent-row:hover {
  background: #fafafa;
}

.dept-chevron {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 22px;
  height: 22px;
  padding: 0;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  flex-shrink: 0;
}

.dept-chevron svg {
  display: block;
  transition: transform 0.15s;
}

.dept-chevron.is-open svg {
  transform: rotate(90deg);
}

.dept-parent-label {
  flex: 1;
  padding: 4px 0;
  border-radius: 4px;
  border-left: 3px solid transparent;
  padding-left: 6px;
  margin-left: -2px;
}

.dept-parent-label:hover {
  color: #1a5ce6;
}

.dept-parent-label.active {
  color: #1a5ce6;
  font-weight: 500;
  background: #e8f4ff;
  border-left-color: #1a5ce6;
}

.dept-children {
  list-style: none;
  padding: 0 0 4px 0;
  margin: 0 0 0 18px;
}

.dept-leaf-sub {
  padding-left: 8px;
}

.user-main {
  background: #fff;
  border-radius: 4px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.main-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.title-accent {
  width: 4px;
  height: 18px;
  background: #1a5ce6;
  border-radius: 2px;
  flex-shrink: 0;
}

.main-title-text {
  margin: 0;
  font-size: 16px;
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
  width: 150px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  min-width: 130px;
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

.toolbar {
  display: flex;
  flex-wrap: wrap;
  gap: 10px;
  margin-bottom: 16px;
}

.tool-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
}

.tool-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.tool-btn.primary:hover {
  background: #1246bb;
}

.tool-btn.outline {
  background: #f5f9ff;
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.tool-btn.outline:hover {
  background: #e8f4ff;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 980px;
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
  padding: 10px 8px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
  vertical-align: middle;
}

.cell-email {
  max-width: 200px;
  word-break: break-all;
  line-height: 1.4;
}

.role-tag {
  display: inline-block;
  margin: 2px;
  padding: 2px 8px;
  font-size: 12px;
  color: #1a5ce6;
  background: #e6f0ff;
  border: 1px solid #adcfff;
  border-radius: 4px;
}

.status-on {
  color: #389e0d;
  font-weight: 500;
}

.status-off {
  color: #cf1322;
  font-weight: 500;
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

.table-empty {
  padding: 32px;
  text-align: center;
  font-size: 13px;
  color: #999;
}

.table-footer {
  padding: 12px 8px 0;
  font-size: 13px;
  color: #666;
}

@media (max-width: 1100px) {
  .user-layout {
    grid-template-columns: 1fr;
  }
}
</style>
