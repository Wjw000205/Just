<template>
  <section class="menu-manage-page">
    <!-- 面包屑导航 -->
    <div class="page-header">
      <div class="breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-main" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-now">菜单管理</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="page-content">
      <!-- 页面标题 -->
      <div class="page-title">菜单管理</div>

      <!-- 搜索筛选区域 -->
      <div class="search-filter-area">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">菜单名称</label>
            <input
              v-model="searchForm.name"
              class="filter-input"
              placeholder="请输入菜单名称"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">状态</label>
            <div class="filter-select" @click.stop="toggleStatusDropdown">
              <span class="filter-select-text">
                {{ searchForm.status || '菜单状态' }}
              </span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="statusDropdownVisible" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="selectStatus('')">菜单状态</li>
                <li class="filter-select-item" @click.stop="selectStatus('正常')">正常</li>
                <li class="filter-select-item" @click.stop="selectStatus('停用')">停用</li>
              </ul>
            </div>
          </div>
          <div class="filter-buttons">
            <button class="filter-btn primary" @click="handleSearch">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              搜索
            </button>
            <button class="filter-btn ghost" @click="handleReset">重置</button>
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
        <button class="action-btn ghost" @click="toggleExpandAll">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <polyline points="15 3 21 3 21 9"></polyline>
            <polyline points="9 21 3 21 3 15"></polyline>
            <line x1="21" y1="3" x2="14" y2="10"></line>
            <line x1="3" y1="21" x2="10" y2="14"></line>
          </svg>
          {{ isAllExpanded ? '折叠' : '展开' }}/折叠
        </button>
      </div>

      <!-- 表格区域 -->
      <div class="table-area">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-name">菜单名称</th>
              <th class="col-sort">排序</th>
              <th class="col-permission">权限标识</th>
              <th class="col-path">组件路径</th>
              <th class="col-status">状态</th>
              <th class="col-time">创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="item in flattenedData" :key="item.id">
              <tr :class="{ 'level-1': item.level === 1, 'level-2': item.level === 2, 'level-3': item.level === 3 }">
                <td class="col-name">
                  <div class="name-cell" :style="{ paddingLeft: (item.level - 1) * 24 + 'px' }">
                    <span
                      v-if="item.hasChildren"
                      class="expand-icon"
                      :class="{ expanded: item.expanded }"
                      @click="toggleExpand(item)"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="9 18 15 12 9 6"></polyline>
                      </svg>
                    </span>
                    <span v-else class="expand-icon-placeholder"></span>
                    <span class="menu-icon" v-if="item.icon">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <component :is="item.icon" />
                      </svg>
                    </span>
                    <span class="name-text" :title="item.name">{{ item.name }}</span>
                  </div>
                </td>
                <td class="col-sort">{{ item.sort }}</td>
                <td class="col-permission">{{ item.permission || '-' }}</td>
                <td class="col-path">{{ item.path || '-' }}</td>
                <td class="col-status">
                  <span :class="['status-tag', item.status === '正常' ? 'status-normal' : 'status-disabled']">
                    {{ item.status }}
                  </span>
                </td>
                <td class="col-time">{{ item.createTime }}</td>
                <td class="col-action">
                  <div class="action-btns">
                    <button class="text-btn" @click="handleEdit(item)">修改</button>
                    <span class="action-sep">|</span>
                    <button class="text-btn" @click="handleAddChild(item)">新增</button>
                    <span class="action-sep">|</span>
                    <button class="text-btn delete" @click="handleDelete(item)">删除</button>
                  </div>
                </td>
              </tr>
            </template>
          </tbody>
        </table>
      </div>

      <!-- 空状态 -->
      <div v-if="flattenedData.length === 0" class="empty-state">
        <svg width="64" height="64" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1">
          <rect x="3" y="3" width="18" height="18" rx="2" />
          <line x1="9" y1="3" x2="9" y2="21" />
        </svg>
        <p>暂无数据</p>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home', 'go-system-manage'])

// 搜索表单
const searchForm = ref({
  name: '',
  status: ''
})

// 下拉框状态
const statusDropdownVisible = ref(false)

// 树形数据
const menuTreeData = ref([
  {
    id: '1',
    name: '模板创建',
    sort: 1,
    permission: '',
    path: 'templateManage/index',
    status: '正常',
    createTime: '2025-07-19 16:39:09',
    level: 1,
    expanded: true,
    children: []
  },
  {
    id: '2',
    name: '数据上传',
    sort: 2,
    permission: '',
    path: '',
    status: '正常',
    createTime: '2025-07-19 15:45:56',
    level: 1,
    expanded: false,
    children: [
      {
        id: '2-1',
        name: '创建数据集',
        sort: 1,
        permission: '',
        path: 'dataset/create',
        status: '正常',
        createTime: '2025-07-19 15:46:10',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '2-2',
        name: '上传数据集',
        sort: 2,
        permission: '',
        path: 'dataset/upload',
        status: '正常',
        createTime: '2025-07-19 15:46:25',
        level: 2,
        expanded: true,
        children: []
      }
    ]
  },
  {
    id: '3',
    name: '数据发布',
    sort: 3,
    permission: '',
    path: 'linkManagement/dataSet/index',
    status: '正常',
    createTime: '2025-07-19 16:44:07',
    level: 1,
    expanded: false,
    children: []
  },
  {
    id: '4',
    name: '模板库',
    sort: 4,
    permission: '',
    path: 'templateManage/templateDataLibrary/index',
    status: '正常',
    createTime: '2025-07-19 16:39:36',
    level: 1,
    expanded: false,
    children: []
  },
  {
    id: '6',
    name: '数据库',
    sort: 6,
    permission: '',
    path: 'dataLibrary/index',
    status: '正常',
    createTime: '2025-06-21 16:26:51',
    level: 1,
    expanded: false,
    children: []
  },
  {
    id: '7',
    name: '审核管理',
    sort: 7,
    permission: '',
    path: '',
    status: '正常',
    createTime: '2025-07-19 16:38:21',
    level: 1,
    expanded: false,
    children: [
      {
        id: '7-1',
        name: '模板审核',
        sort: 1,
        permission: '',
        path: 'audit/template',
        status: '正常',
        createTime: '2025-07-19 16:38:35',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '7-2',
        name: '数据审核',
        sort: 2,
        permission: '',
        path: 'audit/data',
        status: '正常',
        createTime: '2025-07-19 16:38:50',
        level: 2,
        expanded: true,
        children: []
      }
    ]
  },
  {
    id: '8',
    name: '系统管理',
    sort: 8,
    permission: '',
    path: '',
    status: '正常',
    createTime: '2025-07-14 17:49:53',
    level: 1,
    expanded: true,
    children: [
      {
        id: '8-1',
        name: '科学分类管理',
        sort: 1,
        permission: '',
        path: 'system/scientificCategory',
        status: '正常',
        createTime: '2025-07-14 17:50:10',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-2',
        name: '产业分类管理',
        sort: 2,
        permission: '',
        path: 'system/industryCategory',
        status: '正常',
        createTime: '2025-07-14 17:50:25',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-3',
        name: '数据标签管理',
        sort: 3,
        permission: '',
        path: 'system/dataTags',
        status: '正常',
        createTime: '2025-07-14 17:50:40',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-4',
        name: '用户管理',
        sort: 4,
        permission: '',
        path: 'system/users',
        status: '正常',
        createTime: '2025-07-14 17:50:55',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-5',
        name: '部门管理',
        sort: 5,
        permission: '',
        path: 'system/departments',
        status: '正常',
        createTime: '2025-07-14 17:51:10',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-6',
        name: '权限管理',
        sort: 6,
        permission: '',
        path: 'system/permissions',
        status: '正常',
        createTime: '2025-07-14 17:51:25',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-7',
        name: '数据字典',
        sort: 7,
        permission: '',
        path: 'system/dictionary',
        status: '正常',
        createTime: '2025-07-14 17:51:40',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-8',
        name: '界面管理',
        sort: 8,
        permission: '',
        path: 'system/interface',
        status: '正常',
        createTime: '2025-07-14 17:51:55',
        level: 2,
        expanded: true,
        children: []
      },
      {
        id: '8-9',
        name: '菜单管理',
        sort: 9,
        permission: '',
        path: 'system/menu',
        status: '正常',
        createTime: '2025-07-14 17:52:10',
        level: 2,
        expanded: true,
        children: []
      }
    ]
  }
])

// 展开/折叠状态
const isAllExpanded = ref(true)

// 过滤树形数据
const filterTree = (nodes, filters) => {
  if (!nodes?.length) return []
  const out = []
  for (const node of nodes) {
    const filteredChildren = filterTree(node.children || [], filters)
    const nameMatch = !filters.name || node.name.includes(filters.name)
    const statusMatch = !filters.status || node.status === filters.status
    
    if ((nameMatch && statusMatch) || filteredChildren.length > 0) {
      out.push({
        ...node,
        children: filteredChildren,
        expanded: isAllExpanded.value
      })
    }
  }
  return out
}

// 当前过滤条件
const activeFilters = ref({
  name: '',
  status: ''
})

// 过滤后的数据
const filteredData = computed(() =>
  filterTree(menuTreeData.value, activeFilters.value)
)

// 扁平化数据用于表格显示
const flattenedData = computed(() => {
  const result = []
  const flatten = (items) => {
    items.forEach((item) => {
      result.push({
        ...item,
        hasChildren: item.children && item.children.length > 0
      })
      if (item.expanded && item.children) {
        flatten(item.children)
      }
    })
  }
  flatten(filteredData.value)
  return result
})

// 展开/折叠节点
const toggleExpand = (item) => {
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
  findAndToggle(menuTreeData.value)
}

// 全部展开/折叠
const toggleExpandAll = () => {
  isAllExpanded.value = !isAllExpanded.value
  const setExpand = (items, expanded) => {
    items.forEach(item => {
      item.expanded = expanded
      if (item.children) {
        setExpand(item.children, expanded)
      }
    })
  }
  setExpand(menuTreeData.value, isAllExpanded.value)
}

// 状态下拉
const toggleStatusDropdown = () => {
  statusDropdownVisible.value = !statusDropdownVisible.value
}

const selectStatus = (value) => {
  searchForm.value.status = value
  statusDropdownVisible.value = false
}

// 搜索和重置
const handleSearch = () => {
  activeFilters.value = {
    name: searchForm.value.name,
    status: searchForm.value.status
  }
}

const handleReset = () => {
  searchForm.value = { name: '', status: '' }
  activeFilters.value = { name: '', status: '' }
}

// 操作按钮
const handleAdd = () => {
  console.log('新增菜单')
  alert('新增菜单功能')
}

const handleEdit = (item) => {
  console.log('修改菜单：', item)
  alert(`修改菜单：${item.name}`)
}

const handleAddChild = (item) => {
  console.log('新增子菜单：', item)
  alert(`为 [${item.name}] 新增子菜单`)
}

const handleDelete = (item) => {
  if (confirm(`确定要删除菜单 [${item.name}] 吗？`)) {
    console.log('删除菜单：', item)
    alert('删除成功！')
  }
}

// 点击外部关闭下拉
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
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
.menu-manage-page {
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
  width: 180px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
  background: #fff;
}

.filter-input::placeholder {
  color: #b0b6c6;
}

.filter-input:focus {
  outline: none;
  border-color: #1a5ce6;
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
  color: #999;
  cursor: pointer;
  min-width: 120px;
}

.filter-select-text {
  flex: 1;
  color: #999;
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
  min-width: 120px;
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
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
}

.filter-btn svg {
  display: block;
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

.action-btn svg {
  display: block;
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

.action-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

.action-btn.ghost:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
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
  background: #fff;
}

.data-table tbody tr.level-2 {
  background: #fafbfc;
}

/* 列宽设置 */
.col-name {
  min-width: 200px;
}

.col-sort {
  width: 60px;
  text-align: center;
}

.col-permission {
  min-width: 120px;
}

.col-path {
  min-width: 280px;
}

.col-status {
  width: 80px;
  text-align: center;
}

.col-time {
  min-width: 160px;
}

.col-action {
  min-width: 150px;
  text-align: center;
}

/* 菜单名称单元格 */
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

.menu-icon {
  display: flex;
  align-items: center;
  color: #666;
}

.name-text {
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

/* 状态标签 */
.status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.status-normal {
  color: #67c23a;
  background: #f0f9eb;
}

.status-disabled {
  color: #909399;
  background: #f4f4f5;
}

/* 操作按钮 */
.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 6px;
}

.text-btn {
  padding: 0;
  border: none;
  background: transparent;
  color: #1a5ce6;
  font-size: 13px;
  cursor: pointer;
  transition: color 0.2s;
}

.text-btn:hover {
  color: #1246bb;
  text-decoration: underline;
}

.text-btn.delete {
  color: #f56c6c;
}

.text-btn.delete:hover {
  color: #c45656;
}

.action-sep {
  color: #d4dae6;
}

/* 空状态 */
.empty-state {
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  padding: 60px 20px;
  color: #999;
}

.empty-state svg {
  margin-bottom: 16px;
}

.empty-state p {
  margin: 0;
  font-size: 14px;
}
</style>
