<template>
  <section class="dept-manage-page">
    <!-- 面包屑导航 -->
    <div class="page-header">
      <div class="breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-main" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-now">部门管理</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="page-content">
      <!-- 页面标题 -->
      <div class="page-title">部门管理</div>

      <!-- 标签页切换 -->
      <div class="tab-header">
        <button
          class="tab-btn"
          :class="{ active: currentTab === 'config' }"
          @click="currentTab = 'config'"
        >
          部门配置
        </button>
        <button
          class="tab-btn"
          :class="{ active: currentTab === 'audit' }"
          @click="currentTab = 'audit'"
        >
          部门审核
        </button>
      </div>

      <!-- 部门配置标签页 -->
      <div v-if="currentTab === 'config'" class="tab-panel">
        <!-- 搜索筛选区域 -->
        <div class="search-filter-area">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">部门名称</label>
              <input
                v-model="configSearchForm.name"
                class="filter-input"
                placeholder="请输入部门名称"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">部门等级</label>
              <div class="filter-select" @click.stop="toggleLevelDropdown">
                <span class="filter-select-text">
                  {{ configSearchForm.level || '请选择部门等级' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="levelDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectLevel('')">请选择部门等级</li>
                  <li class="filter-select-item" @click.stop="selectLevel('一级部门')">一级部门</li>
                  <li class="filter-select-item" @click.stop="selectLevel('二级部门')">二级部门</li>
                  <li class="filter-select-item" @click.stop="selectLevel('三级部门')">三级部门</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">所属部门</label>
              <div class="filter-select" @click.stop="toggleParentDropdown">
                <span class="filter-select-text">
                  {{ configSearchForm.parent || '请选择' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="parentDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectParent('')">请选择</li>
                  <li
                    v-for="name in parentOptions"
                    :key="name"
                    class="filter-select-item"
                    @click.stop="selectParent(name)"
                  >
                    {{ name }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-buttons">
              <button class="filter-btn primary" @click="handleConfigSearch">查询</button>
              <button class="filter-btn ghost" @click="handleConfigReset">重置</button>
            </div>
          </div>
        </div>

        <!-- 操作按钮区域 -->
        <div class="action-area">
          <button class="action-btn primary" @click="handleAdd">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="12" y1="5" x2="12" y2="19"></line>
              <line x1="5" y1="12" x2="19" y2="12"></line>
            </svg>
            新增
          </button>
        </div>

        <p v-if="configLoadError" class="load-error">{{ configLoadError }}</p>
        <p v-else-if="configLoading" class="load-hint">加载中…</p>

        <!-- 表格区域 -->
        <div class="table-area">
          <table v-if="!configLoading" class="data-table">
            <thead>
              <tr>
                <th class="col-name">部门名称</th>
                <th class="col-level">部门等级</th>
                <th class="col-parent">所属部门</th>
                <th class="col-children">子部门数</th>
                <th class="col-users">部门用户数</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <template v-for="(item, index) in configFlattenedData" :key="item.id">
                <tr :class="{ 
                  'level-1': item.level === 1, 
                  'level-2': item.level === 2, 
                  'level-3': item.level === 3 
                }">
                  <td class="col-name">
                    <div class="name-cell" :style="{ paddingLeft: (item.level - 1) * 20 + 'px' }">
                      <span
                        v-if="item.hasChildren"
                        class="expand-icon"
                        :class="{ expanded: item.expanded }"
                        @click="toggleConfigExpand(item)"
                      >
                        <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="9 18 15 12 9 6"></polyline>
                        </svg>
                      </span>
                      <span v-else class="expand-icon-placeholder"></span>
                      <span class="name-text" :title="item.name">{{ item.name }}</span>
                    </div>
                  </td>
                  <td class="col-level">{{ getLevelText(item.level) }}</td>
                  <td class="col-parent">{{ item.parentName || '-' }}</td>
                  <td class="col-children">{{ item.childrenCount || 0 }}</td>
                  <td class="col-users">{{ item.userCount || 0 }}</td>
                  <td class="col-action">
                    <div class="action-btns">
                      <button class="action-btn-icon view" title="查看" @click="handleConfigView(item)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                          <circle cx="12" cy="12" r="3"/>
                        </svg>
                      </button>
                      <button class="action-btn-icon edit" title="编辑" @click="handleConfigEdit(item)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                          <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                        </svg>
                      </button>
                      <button class="action-btn-icon delete" title="删除" @click="handleConfigDelete(item)">
                        <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                          <polyline points="3 6 5 6 21 6"></polyline>
                          <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"></path>
                        </svg>
                      </button>
                    </div>
                  </td>
                </tr>
              </template>
            </tbody>
          </table>
        </div>
      </div>

      <!-- 部门审核标签页 -->
      <div v-if="currentTab === 'audit'" class="tab-panel">
        <!-- 搜索筛选区域 -->
        <div class="search-filter-area">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">用户名</label>
              <input
                v-model="auditSearchForm.username"
                class="filter-input"
                placeholder="请输入用户名"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">真实姓名</label>
              <input
                v-model="auditSearchForm.realName"
                class="filter-input"
                placeholder="请输入真实姓名"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">审核人</label>
              <input
                v-model="auditSearchForm.auditor"
                class="filter-input"
                placeholder="请输入审核人"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">申请部门</label>
              <div class="filter-select" @click.stop="toggleAuditDeptDropdown">
                <span class="filter-select-text">
                  {{ auditSearchForm.dept || '请选择' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="auditDeptDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectAuditDept('')">请选择</li>
                  <li class="filter-select-item" @click.stop="selectAuditDept('节点管理部')">节点管理部</li>
                  <li class="filter-select-item" @click.stop="selectAuditDept('社会大众')">社会大众</li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-row second-row">
            <div class="filter-item">
              <label class="filter-label">状态</label>
              <div class="filter-select" @click.stop="toggleStatusDropdown">
                <span class="filter-select-text">
                  {{ auditSearchForm.status || '请选择状态' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="statusDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectStatus('')">请选择状态</li>
                  <li class="filter-select-item" @click.stop="selectStatus('待审核')">待审核</li>
                  <li class="filter-select-item" @click.stop="selectStatus('已通过')">已通过</li>
                  <li class="filter-select-item" @click.stop="selectStatus('已驳回')">已驳回</li>
                </ul>
              </div>
            </div>
            <div class="filter-buttons">
              <button class="filter-btn primary" @click="handleAuditSearch">查询</button>
              <button class="filter-btn ghost" @click="handleAuditReset">重置</button>
            </div>
          </div>
        </div>

        <!-- 批量操作按钮区域 -->
        <div class="action-area">
          <button class="action-btn success" @click="handleBatchApprove">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <polyline points="20 6 9 17 4 12"></polyline>
            </svg>
            批量通过
          </button>
          <button class="action-btn danger" @click="handleBatchReject">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <line x1="18" y1="6" x2="6" y2="18"></line>
              <line x1="6" y1="6" x2="18" y2="18"></line>
            </svg>
            批量驳回
          </button>
        </div>

        <p v-if="auditLoadError" class="load-error">{{ auditLoadError }}</p>
        <p v-else-if="auditLoading" class="load-hint">加载中…</p>

        <!-- 表格区域 -->
        <div class="table-area">
          <table v-if="!auditLoading" class="data-table audit-table">
            <thead>
              <tr>
                <th class="col-checkbox">
                  <input type="checkbox" v-model="selectAll" @change="handleSelectAll" />
                </th>
                <th class="col-index">序号</th>
                <th class="col-username">用户名</th>
                <th class="col-realname">真实姓名</th>
                <th class="col-dept">申请部门</th>
                <th class="col-reason">申请理由</th>
                <th class="col-transfer">
                  是否转移数据
                  <span class="transfer-hint" title="转移数据到目标部门">?</span>
                </th>
                <th class="col-time">申请时间</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in auditPagedData" :key="item.id">
                <td class="col-checkbox">
                  <input type="checkbox" v-model="item.selected" />
                </td>
                <td class="col-index">{{ (auditCurrentPage - 1) * auditPageSize + index + 1 }}</td>
                <td class="col-username">{{ item.username }}</td>
                <td class="col-realname">{{ item.realName }}</td>
                <td class="col-dept">{{ item.dept }}</td>
                <td class="col-reason">{{ item.reason }}</td>
                <td class="col-transfer">{{ item.transferData ? '是' : '否' }}</td>
                <td class="col-time">{{ item.applyTime }}</td>
                <td class="col-action">
                  <div class="action-btns">
                    <button class="action-btn-icon view" title="查看" @click="handleAuditView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>
                    <button class="action-btn-icon approve" title="通过" @click="handleAuditApprove(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="20 6 9 17 4 12"></polyline>
                      </svg>
                    </button>
                    <button class="action-btn-icon reject" title="驳回" @click="handleAuditReject(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <circle cx="12" cy="12" r="10"/>
                        <line x1="15" y1="9" x2="9" y2="15"></line>
                        <line x1="9" y1="9" x2="15" y2="15"></line>
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
          <span class="total">共 {{ auditTotal }} 条</span>
          <button class="page-btn" :disabled="auditCurrentPage === 1" @click="auditCurrentPage--">
            上一页
          </button>
          <span class="page-current">{{ auditCurrentPage }}</span>
          <button class="page-btn" :disabled="auditCurrentPage >= auditTotalPages" @click="auditCurrentPage++">
            下一页
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home', 'go-system-manage'])

// 当前标签页
const currentTab = ref('config')

// ============== 部门配置 ==============
const configLoading = ref(false)
const configLoadError = ref('')

// 下拉框状态
const levelDropdownVisible = ref(false)
const parentDropdownVisible = ref(false)

// 搜索表单
const configSearchForm = ref({
  name: '',
  level: '',
  parent: '',
})

// 所属部门选项
const parentOptions = ref(['社会大众', '节点管理部', '钢铁研究总院', '上硅所', '川大生材中心'])

// 查询生效条件
const configActiveFilters = ref({
  name: '',
  level: '',
  parent: '',
})

// 示例部门数据
const INITIAL_DEPT_DATA = [
  {
    id: '1',
    name: '社会大众',
    level: 1,
    parentName: '',
    childrenCount: 0,
    userCount: 14,
    expanded: true,
    children: [],
  },
  {
    id: '2',
    name: '节点管理部',
    level: 1,
    parentName: '',
    childrenCount: 10,
    userCount: 4,
    expanded: true,
    children: [
      {
        id: '2-1',
        name: '知网',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-2',
        name: '川大生材中心',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-3',
        name: '中南大学',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-4',
        name: '解放军第四医院',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-5',
        name: '西南交大',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-6',
        name: '上硅所',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 1,
        expanded: true,
        children: [],
      },
      {
        id: '2-7',
        name: '嘉思特',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-8',
        name: '锦波生物',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-9',
        name: '西京医院',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
      {
        id: '2-10',
        name: '钢铁研究总院',
        level: 2,
        parentName: '节点管理部',
        childrenCount: 0,
        userCount: 0,
        expanded: true,
        children: [],
      },
    ],
  },
]

const deptTreeData = ref(JSON.parse(JSON.stringify(INITIAL_DEPT_DATA)))

function configNodeMatchesFilters(node, filters) {
  const nameQ = (filters.name || '').trim()
  if (nameQ && !String(node.name || '').includes(nameQ)) return false
  if (filters.level && getLevelText(node.level) !== filters.level) return false
  if (filters.parent && String(node.parentName || '') !== filters.parent) return false
  return true
}

function configFilterTree(nodes, filters) {
  if (!nodes?.length) return []
  const out = []
  for (const node of nodes) {
    const nextChildren = configFilterTree(node.children || [], filters)
    const selfOk = configNodeMatchesFilters(node, filters)
    if (selfOk || nextChildren.length > 0) {
      out.push({
        ...node,
        children: nextChildren,
      })
    }
  }
  return out
}

const configDisplayTree = computed(() =>
  configFilterTree(deptTreeData.value, configActiveFilters.value),
)

const configFlattenedData = computed(() => {
  const result = []
  const flatten = (items) => {
    items.forEach((item) => {
      result.push({
        ...item,
        hasChildren: item.children && item.children.length > 0,
      })
      if (item.expanded && item.children) {
        flatten(item.children)
      }
    })
  }
  flatten(configDisplayTree.value)
  return result
})

function getLevelText(level) {
  const map = { 1: '一级部门', 2: '二级部门', 3: '三级部门' }
  return map[level] || ''
}

const toggleConfigExpand = (item) => {
  const findAndToggle = (items) => {
    for (let i = 0; i < items.length; i++) {
      if (items[i].id === item.id) {
        items[i].expanded = !items[i].expanded
        return true
      }
      if (items[i].children && findAndToggle(items[i].children)) {
        return true
      }
    }
    return false
  }
  findAndToggle(deptTreeData.value)
}

const toggleLevelDropdown = () => {
  levelDropdownVisible.value = !levelDropdownVisible.value
  parentDropdownVisible.value = false
}

const toggleParentDropdown = () => {
  parentDropdownVisible.value = !parentDropdownVisible.value
  levelDropdownVisible.value = false
}

const selectLevel = (value) => {
  configSearchForm.value.level = value
  levelDropdownVisible.value = false
}

const selectParent = (value) => {
  configSearchForm.value.parent = value
  parentDropdownVisible.value = false
}

const handleConfigSearch = () => {
  configActiveFilters.value = {
    name: configSearchForm.value.name,
    level: configSearchForm.value.level,
    parent: configSearchForm.value.parent,
  }
}

const handleConfigReset = () => {
  configSearchForm.value = { name: '', level: '', parent: '' }
  configActiveFilters.value = { name: '', level: '', parent: '' }
}

const handleAdd = () => {
  console.log('新增部门')
}

const handleConfigView = (item) => {
  console.log('查看部门：', item)
}

const handleConfigEdit = (item) => {
  console.log('编辑部门：', item)
}

const handleConfigDelete = (item) => {
  console.log('删除部门：', item)
}

// ============== 部门审核 ==============
const auditLoading = ref(false)
const auditLoadError = ref('')

// 下拉框状态
const auditDeptDropdownVisible = ref(false)
const statusDropdownVisible = ref(false)

// 搜索表单
const auditSearchForm = ref({
  username: '',
  realName: '',
  auditor: '',
  dept: '',
  status: '',
})

// 分页
const auditCurrentPage = ref(1)
const auditPageSize = ref(10)
const auditTotal = ref(3)

// 示例审核数据
const INITIAL_AUDIT_DATA = [
  {
    id: '1',
    username: 'haha111',
    realName: '谢飞',
    dept: '节点管理部',
    reason: 'test',
    transferData: false,
    applyTime: '2026-03-25 23:25:34',
    selected: false,
  },
  {
    id: '2',
    username: 'haha111',
    realName: '谢飞',
    dept: '节点管理部',
    reason: 'test',
    transferData: false,
    applyTime: '2026-03-25 23:25:34',
    selected: false,
  },
  {
    id: '3',
    username: 'zhuxiangdong',
    realName: '朱向东',
    dept: '社会大众',
    reason: '11',
    transferData: false,
    applyTime: '2026-01-22 16:22:35',
    selected: false,
  },
]

const auditData = ref(JSON.parse(JSON.stringify(INITIAL_AUDIT_DATA)))

const selectAll = computed({
  get() {
    return auditData.value.length > 0 && auditData.value.every(item => item.selected)
  },
  set(value) {
    auditData.value.forEach(item => item.selected = value)
  }
})

const auditTotalPages = computed(() => Math.ceil(auditTotal.value / auditPageSize.value))

const auditPagedData = computed(() => {
  const start = (auditCurrentPage.value - 1) * auditPageSize.value
  const end = start + auditPageSize.value
  return auditData.value.slice(start, end)
})

const toggleAuditDeptDropdown = () => {
  auditDeptDropdownVisible.value = !auditDeptDropdownVisible.value
  statusDropdownVisible.value = false
}

const toggleStatusDropdown = () => {
  statusDropdownVisible.value = !statusDropdownVisible.value
  auditDeptDropdownVisible.value = false
}

const selectAuditDept = (value) => {
  auditSearchForm.value.dept = value
  auditDeptDropdownVisible.value = false
}

const selectStatus = (value) => {
  auditSearchForm.value.status = value
  statusDropdownVisible.value = false
}

const handleAuditSearch = () => {
  auditCurrentPage.value = 1
  console.log('审核查询：', auditSearchForm.value)
}

const handleAuditReset = () => {
  auditSearchForm.value = { username: '', realName: '', auditor: '', dept: '', status: '' }
  auditCurrentPage.value = 1
}

const handleSelectAll = () => {
  const newValue = !selectAll.value
  auditData.value.forEach(item => item.selected = newValue)
}

const handleBatchApprove = () => {
  const selected = auditData.value.filter(item => item.selected)
  if (selected.length === 0) {
    alert('请先选择要通过的申请')
    return
  }
  console.log('批量通过：', selected)
}

const handleBatchReject = () => {
  const selected = auditData.value.filter(item => item.selected)
  if (selected.length === 0) {
    alert('请先选择要驳回的申请')
    return
  }
  console.log('批量驳回：', selected)
}

const handleAuditView = (item) => {
  console.log('查看审核：', item)
}

const handleAuditApprove = (item) => {
  console.log('通过审核：', item)
}

const handleAuditReject = (item) => {
  console.log('驳回审核：', item)
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
    levelDropdownVisible.value = false
    parentDropdownVisible.value = false
    auditDeptDropdownVisible.value = false
    statusDropdownVisible.value = false
  }
}

const goHome = () => {
  emit('go-home')
}

const goSystemManage = () => {
  emit('go-system-manage')
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.dept-manage-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #f5f7fb;
  padding: 16px 20px 20px;
  border-radius: 8px;
}

.page-header {
  font-size: 13px;
  color: #666;
}

.breadcrumb {
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
}

.page-content {
  background: #fff;
  border-radius: 6px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
  padding-left: 10px;
  border-left: 4px solid #1a5ce6;
}

/* 标签页 */
.tab-header {
  display: flex;
  gap: 0;
  margin-bottom: 20px;
  border-bottom: 1px solid #e8ecf4;
}

.tab-btn {
  padding: 10px 24px;
  font-size: 14px;
  color: #666;
  background: #fff;
  border: 1px solid #d4dae6;
  border-bottom: none;
  border-radius: 4px 4px 0 0;
  cursor: pointer;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #1a5ce6;
}

.tab-btn.active {
  color: #fff;
  background: #1a5ce6;
  border-color: #1a5ce6;
}

.tab-panel {
  animation: fadeIn 0.3s ease;
}

@keyframes fadeIn {
  from { opacity: 0; transform: translateY(5px); }
  to { opacity: 1; transform: translateY(0); }
}

.load-error {
  margin: 0 0 12px;
  font-size: 13px;
  color: #c04030;
}

.load-hint {
  margin: 0 0 12px;
  font-size: 13px;
  color: #666;
}

/* 搜索筛选区域 */
.search-filter-area {
  background: #f8f9fc;
  border-radius: 6px;
  padding: 16px 20px;
  margin-bottom: 20px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 20px;
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
  color: #333;
  white-space: nowrap;
}

.filter-input {
  width: 160px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
  background: #fff;
}

.filter-input::placeholder {
  color: #b0b6c6;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  min-width: 140px;
}

.filter-select-text {
  flex: 1;
  color: #666;
}

.filter-select-text:not(:empty) {
  color: #333;
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
  padding: 6px 20px;
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

.filter-btn.primary:hover {
  background: #1246bb;
  border-color: #1246bb;
}

.filter-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

.filter-btn.ghost:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

/* 操作按钮区域 */
.action-area {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 20px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
}

.action-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.action-btn.primary:hover {
  background: #1246bb;
  border-color: #1246bb;
}

.action-btn.success {
  background: #67c23a;
  border-color: #67c23a;
  color: #fff;
}

.action-btn.success:hover {
  background: #529b2e;
  border-color: #529b2e;
}

.action-btn.danger {
  background: #f56c6c;
  border-color: #f56c6c;
  color: #fff;
}

.action-btn.danger:hover {
  background: #c45656;
  border-color: #c45656;
}

.action-btn svg {
  display: block;
}

/* 表格区域 */
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

.data-table tbody tr:hover {
  background: #f8f9fc;
}

.data-table tbody tr.level-1 {
  background: #fafbfc;
}

.data-table tbody tr.level-1:hover {
  background: #f0f5ff;
}

/* 部门配置表格列宽 */
.col-name {
  min-width: 180px;
}

.col-level {
  min-width: 100px;
}

.col-parent {
  min-width: 120px;
}

.col-children {
  min-width: 80px;
  text-align: center;
}

.col-users {
  min-width: 100px;
  text-align: center;
}

/* 部门审核表格列宽 */
.audit-table .col-checkbox {
  width: 40px;
  text-align: center;
}

.audit-table .col-index {
  width: 60px;
  text-align: center;
}

.audit-table .col-username {
  min-width: 100px;
}

.audit-table .col-realname {
  min-width: 100px;
}

.audit-table .col-dept {
  min-width: 120px;
}

.audit-table .col-reason {
  min-width: 120px;
}

.audit-table .col-transfer {
  min-width: 120px;
  text-align: center;
}

.transfer-hint {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #909399;
  color: #fff;
  font-size: 11px;
  margin-left: 4px;
  cursor: help;
}

.audit-table .col-time {
  min-width: 160px;
}

.col-action {
  min-width: 100px;
  text-align: center;
}

.name-cell {
  display: flex;
  align-items: center;
  gap: 6px;
}

.expand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  cursor: pointer;
  color: #666;
  transition: transform 0.2s;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.expand-icon:hover {
  color: #1a5ce6;
}

.expand-icon-placeholder {
  width: 18px;
  height: 18px;
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.action-btn-icon:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.action-btn-icon.approve:hover {
  background: #f0f9eb;
  color: #67c23a;
}

.action-btn-icon.reject:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.action-btn-icon.delete:hover {
  background: #fff0f0;
  color: #e64a4a;
}

.action-btn-icon svg {
  display: block;
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
</style>
