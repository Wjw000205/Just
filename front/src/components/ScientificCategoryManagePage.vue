<template>
  <section class="category-manage-page">
    <!-- 面包屑导航 -->
    <div class="page-header">
      <div class="breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-main" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-now">科学分类管理</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="page-content">
      <!-- 页面标题 -->
      <div class="page-title">科学分类管理</div>

      <!-- 搜索筛选区域 -->
      <div class="search-filter-area">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">目录名称</label>
            <input
              v-model="searchForm.name"
              class="filter-input"
              placeholder="请输入目录名称"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">目录等级</label>
            <div class="filter-select" @click.stop="toggleLevelDropdown">
              <span class="filter-select-text">
                {{ searchForm.level || '请选择目录等级' }}
              </span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="levelDropdownVisible" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="selectLevel('')">请选择目录等级</li>
                <li class="filter-select-item" @click.stop="selectLevel('一级目录')">一级目录</li>
                <li class="filter-select-item" @click.stop="selectLevel('二级目录')">二级目录</li>
                <li class="filter-select-item" @click.stop="selectLevel('三级目录')">三级目录</li>
                <li class="filter-select-item" @click.stop="selectLevel('四级目录')">四级目录</li>
              </ul>
            </div>
          </div>
          <div class="filter-item">
            <label class="filter-label">所属目录</label>
            <div class="filter-select" @click.stop="toggleParentDropdown">
              <span class="filter-select-text">
                {{ searchForm.parent || '请选择' }}
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
        </div>
        <div class="filter-row second-row">
          <div class="filter-buttons">
            <button class="filter-btn primary" @click="handleSearch">查询</button>
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
        <button class="action-btn primary" @click="handleBatchAdd">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          批量新增
        </button>
        <button class="action-btn secondary" @click="handleFetchCatalog">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          目录获取
        </button>
      </div>

      <p v-if="loadError" class="load-error">{{ loadError }}</p>
      <p v-else-if="loading" class="load-hint">加载中…</p>

      <!-- 表格区域 -->
      <div class="table-area">
        <table v-if="!loading" class="data-table">
          <thead>
            <tr>
              <th class="col-index">序号</th>
              <th class="col-name">目录名称</th>
              <th class="col-level">目录等级</th>
              <th class="col-parent">所属目录</th>
              <th class="col-children">子目录数</th>
              <th class="col-data">目录数据集数</th>
              <th class="col-template">目录模板数</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(item, index) in flattenedData" :key="item.id">
              <tr :class="{ 'level-1': item.level === 1, 'level-2': item.level === 2, 'level-3': item.level === 3, 'level-4': item.level === 4 }">
                <td class="col-index">{{ index + 1 }}</td>
                <td class="col-name">
                  <div class="name-cell" :style="{ paddingLeft: (item.level - 1) * 20 + 'px' }">
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
                    <span class="name-text" :title="item.name">{{ item.name }}</span>
                  </div>
                </td>
                <td class="col-level">{{ getLevelText(item.level) }}</td>
                <td class="col-parent">{{ item.parentName || '-' }}</td>
                <td class="col-children">{{ item.childrenCount || 0 }}</td>
                <td class="col-data">{{ item.dataCount || 0 }}</td>
                <td class="col-template">{{ item.templateCount || 0 }}</td>
                <td class="col-action">
                  <div class="action-btns">
                    <button class="action-btn-icon view" title="查看" @click="handleView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>
                    <button class="action-btn-icon edit" title="编辑" @click="handleEdit(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                        <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                      </svg>
                    </button>
                    <button class="action-btn-icon delete" title="删除" @click="handleDelete(item)">
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

    <!-- 新增目录 -->
    <div v-if="addModalVisible" class="modal-mask" @click.self="closeAddModal">
      <div class="modal-panel" @click.stop>
        <div class="modal-header">
          <span class="modal-title">新增目录</span>
          <button type="button" class="modal-close" aria-label="关闭" @click="closeAddModal">×</button>
        </div>
        <div class="modal-body">
          <div class="modal-field">
            <label class="modal-label"><span class="req">*</span>目录名称</label>
            <input
              v-model="addForm.name"
              type="text"
              :class="['modal-input', { 'is-error': addNameError }]"
              placeholder="请输入目录名称，添加多个目录时请用逗号隔开"
              @input="addNameError = ''"
            />
            <p v-if="addNameError" class="field-error">{{ addNameError }}</p>
          </div>
          <div class="modal-field">
            <label class="modal-label">所属目录</label>
            <select
              v-model="addForm.parent"
              class="modal-select modal-select-tree"
              :disabled="!modalParentFlatOptions.length"
            >
              <option
                v-for="(opt, idx) in modalParentFlatOptions"
                :key="idx + '-' + opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>
          <div class="modal-field">
            <label class="modal-label">目录等级</label>
            <input type="text" class="modal-input modal-input-readonly" readonly :value="addModalLevelText" />
          </div>
        </div>
        <div class="modal-footer">
          <button type="button" class="modal-btn ghost" :disabled="addSubmitting" @click="closeAddModal">
            取消
          </button>
          <button type="button" class="modal-btn primary" :disabled="addSubmitting" @click="confirmAdd">
            {{ addSubmitting ? '提交中…' : '确定' }}
          </button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import { getManuList, createMenu } from '../api/category.js'

const emit = defineEmits(['go-home', 'go-system-manage'])

const loading = ref(false)
const loadError = ref('')

const addModalVisible = ref(false)
const addSubmitting = ref(false)
const addNameError = ref('')
const addForm = ref({
  name: '',
  parent: '',
})

// 下拉框状态
const levelDropdownVisible = ref(false)
const parentDropdownVisible = ref(false)

// 搜索表单
const searchForm = ref({
  name: '',
  level: '',
  parent: '',
})

// 所属目录选项（从当前树收集目录名称）
const parentOptions = ref([])

/** 接口 ManuDatasetTreeVO → 页面展示节点 */
function mapApiToViewNode(apiNode, level = 1) {
  const rawChildren = Array.isArray(apiNode.children) ? apiNode.children : []
  const children = rawChildren.map((ch) => mapApiToViewNode(ch, level + 1))
  return {
    id: apiNode.id,
    name: apiNode.name ?? '',
    level,
    parentId: apiNode.parent ?? 0,
    parentName: '',
    childrenCount: children.length,
    dataCount: '—',
    templateCount: '—',
    expanded: true,
    creator: apiNode.creator,
    createTime: apiNode.createTime,
    isMenu: apiNode.isMenu,
    children,
  }
}

function buildIdNameMap(nodes, map = new Map()) {
  for (const n of nodes) {
    if (n.id != null && n.name) {
      map.set(Number(n.id), n.name)
    }
    if (n.children?.length) buildIdNameMap(n.children, map)
  }
  return map
}

function fillParentNameById(nodes, idNameMap) {
  for (const n of nodes) {
    const pid = Number(n.parentId ?? 0)
    n.parentName = pid > 0 ? idNameMap.get(pid) || '-' : '-'
    if (n.children?.length) fillParentNameById(n.children, idNameMap)
  }
}

function collectNamesForParentSelect(nodes, acc = new Set()) {
  for (const n of nodes) {
    if (n.name) acc.add(n.name)
    if (n.children?.length) collectNamesForParentSelect(n.children, acc)
  }
  return [...acc].sort((a, b) => a.localeCompare(b, 'zh-CN'))
}

/** 前序遍历树，生成带缩进标签的下拉项（展示层级关系） */
function buildIndentedParentOptions(nodes, depth = 0, acc = []) {
  for (const n of nodes) {
    if (!n.name || n.id == null) continue
    const indent = depth > 0 ? `${'\u3000'.repeat(depth)}` : ''
    acc.push({
      value: String(n.id),
      label: `${indent}${n.name}`,
    })
    if (n.children?.length) {
      buildIndentedParentOptions(n.children, depth + 1, acc)
    }
  }
  return acc
}

function getLevelText(level) {
  const levelMap = {
    1: '一级目录',
    2: '二级目录',
    3: '三级目录',
    4: '四级目录',
  }
  if (levelMap[level]) return levelMap[level]
  if (level > 4) return `${level}级目录`
  return ''
}

function findNodeById(nodes, id) {
  if (id == null || !nodes?.length) return null
  const target = Number(id)
  if (!Number.isFinite(target)) return null
  for (const n of nodes) {
    if (Number(n.id) === target) return n
    const found = findNodeById(n.children || [], target)
    if (found) return found
  }
  return null
}

function base64UrlDecode(input) {
  const s = String(input || '')
    .replace(/-/g, '+')
    .replace(/_/g, '/')
  const pad = s.length % 4 === 0 ? '' : '='.repeat(4 - (s.length % 4))
  const b64 = s + pad
  const binary = atob(b64)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}

function getCreatorForRequest() {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token || token.split('.').length !== 3) return 'admin'
  try {
    const payloadText = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadText)
    let name = payload?.userName != null ? String(payload.userName).trim() : ''
    try {
      name = name ? decodeURIComponent(name) : ''
    } catch {
      /* 已是明文 */
    }
    return name || 'admin'
  } catch {
    return 'admin'
  }
}

// 原始树（接口，须在 addModalLevelText 等计算属性之前声明）
const treeData = ref([])

/** 新增弹窗：所属目录下拉（分层缩进） */
const modalParentFlatOptions = computed(() =>
  buildIndentedParentOptions(treeData.value, 0, []),
)

const addModalLevelText = computed(() => {
  const p = addForm.value.parent
  if (p === '' || p == null) {
    return '—'
  }
  const node = findNodeById(treeData.value, p)
  if (!node || node.level == null) return '—'
  return getLevelText(node.level)
})

// 查询生效条件（点击「查询」后写入；接口未支持服务端筛选时在前端过滤）
const activeFilters = ref({
  name: '',
  level: '',
  parent: '',
})

function nodeMatchesFilters(node, filters) {
  const nameQ = (filters.name || '').trim()
  if (nameQ && !String(node.name || '').includes(nameQ)) return false
  if (filters.level && getLevelText(node.level) !== filters.level) return false
  if (filters.parent && String(node.parentName || '') !== filters.parent) return false
  return true
}

function filterTree(nodes, filters) {
  if (!nodes?.length) return []
  const out = []
  for (const node of nodes) {
    const nextChildren = filterTree(node.children || [], filters)
    const selfOk = nodeMatchesFilters(node, filters)
    if (selfOk || nextChildren.length > 0) {
      out.push({
        ...node,
        children: nextChildren,
      })
    }
  }
  return out
}

const displayTree = computed(() =>
  filterTree(treeData.value, activeFilters.value),
)

// 扁平化树形数据（只显示展开的数据）
const flattenedData = computed(() => {
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
  flatten(displayTree.value)
  return result
})

async function loadTree() {
  loading.value = true
  loadError.value = ''
  try {
    const res = await getManuList()
    const list = Array.isArray(res.data) ? res.data : []
    treeData.value = list.map((n) => mapApiToViewNode(n, 1))
    const idNameMap = buildIdNameMap(treeData.value)
    fillParentNameById(treeData.value, idNameMap)
    parentOptions.value = collectNamesForParentSelect(treeData.value)
  } catch (e) {
    loadError.value = e?.message || '加载模板目录树失败'
    treeData.value = []
    parentOptions.value = []
  } finally {
    loading.value = false
  }
}

// 下拉框控制
const toggleLevelDropdown = () => {
  levelDropdownVisible.value = !levelDropdownVisible.value
  parentDropdownVisible.value = false
}

const toggleParentDropdown = () => {
  parentDropdownVisible.value = !parentDropdownVisible.value
  levelDropdownVisible.value = false
}

const selectLevel = (value) => {
  searchForm.value.level = value
  levelDropdownVisible.value = false
}

const selectParent = (value) => {
  searchForm.value.parent = value
  parentDropdownVisible.value = false
}

// 展开/收起
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
  findAndToggle(treeData.value)
}

// 搜索和重置
const handleSearch = () => {
  activeFilters.value = {
    name: searchForm.value.name,
    level: searchForm.value.level,
    parent: searchForm.value.parent,
  }
}

const handleReset = () => {
  searchForm.value = {
    name: '',
    level: '',
    parent: '',
  }
  activeFilters.value = {
    name: '',
    level: '',
    parent: '',
  }
}

function openAddModal() {
  addNameError.value = ''
  const opts = buildIndentedParentOptions(treeData.value, 0, [])
  const defaultParent = opts[0]?.value ?? ''
  addForm.value = { name: '', parent: defaultParent }
  addModalVisible.value = true
}

function closeAddModal() {
  if (addSubmitting.value) return
  addModalVisible.value = false
}

async function confirmAdd() {
  const raw = (addForm.value.name || '').trim()
  if (!raw) {
    addNameError.value = '请输入目录名称'
    return
  }
  const names = raw
    .split(',')
    .map((s) => s.trim())
    .filter(Boolean)
  if (!names.length) {
    addNameError.value = '请输入目录名称'
    return
  }

  if (!modalParentFlatOptions.value.length) {
    addNameError.value = '暂无目录数据，无法新增'
    return
  }
  const p = addForm.value.parent
  if (p === '' || p == null) {
    addNameError.value = '请选择所属目录'
    return
  }
  const parentForApi = Number(p)
  if (!Number.isInteger(parentForApi) || parentForApi <= 0) {
    addNameError.value = '所属目录无效，请重新选择'
    return
  }

  const creator = getCreatorForRequest()

  addSubmitting.value = true
  addNameError.value = ''
  try {
    for (const name of names) {
      await createMenu({
        name,
        creator,
        parent: parentForApi,
      })
    }
    addModalVisible.value = false
    await loadTree()
  } catch (e) {
    addNameError.value = e?.message || '创建失败'
  } finally {
    addSubmitting.value = false
  }
}

// 操作按钮
const handleAdd = () => {
  openAddModal()
}

const handleBatchAdd = () => {
  console.log('批量新增')
}

const handleFetchCatalog = () => {
  loadTree()
}

const handleView = (item) => {
  console.log('查看：', item)
}

const handleEdit = (item) => {
  console.log('编辑：', item)
}

const handleDelete = (item) => {
  console.log('删除：', item)
}

const goHome = () => {
  emit('go-home')
}

const goSystemManage = () => {
  emit('go-system-manage')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
    levelDropdownVisible.value = false
    parentDropdownVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
  loadTree()
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.category-manage-page {
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
  margin-bottom: 12px;
}

.filter-row.second-row {
  margin-bottom: 0;
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

.action-btn.secondary {
  background: #fff;
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.action-btn.secondary:hover {
  background: #f0f5ff;
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

.col-index {
  width: 50px;
  text-align: center;
}

.col-name {
  min-width: 200px;
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

.col-level {
  min-width: 80px;
}

.col-parent {
  min-width: 140px;
}

.col-children {
  min-width: 70px;
  text-align: center;
}

.col-data {
  min-width: 100px;
  text-align: center;
}

.col-template {
  min-width: 100px;
  text-align: center;
}

.col-action {
  min-width: 100px;
  text-align: center;
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

.action-btn-icon.delete:hover {
  background: #fff0f0;
  color: #e64a4a;
}

.action-btn-icon svg {
  display: block;
}

/* 新增目录弹窗 */
.modal-mask {
  position: fixed;
  inset: 0;
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  background: rgba(0, 0, 0, 0.45);
}

.modal-panel {
  width: 480px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.18);
  overflow: hidden;
}

.modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid #e8ecf4;
}

.modal-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.modal-close {
  border: none;
  background: transparent;
  font-size: 22px;
  line-height: 1;
  color: #999;
  cursor: pointer;
  padding: 0 4px;
}

.modal-close:hover {
  color: #333;
}

.modal-body {
  padding: 20px 24px 8px;
}

.modal-field {
  margin-bottom: 18px;
}

.modal-label {
  display: block;
  font-size: 13px;
  color: #333;
  margin-bottom: 8px;
}

.modal-label .req {
  color: #e64a4a;
  margin-right: 2px;
}

.modal-input,
.modal-select {
  width: 100%;
  box-sizing: border-box;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 8px 10px;
  font-size: 13px;
  background: #fff;
}

.modal-input::placeholder {
  color: #b0b6c6;
}

.modal-input.is-error {
  border-color: #e64a4a;
}

.modal-input-readonly {
  background: #f5f7fb;
  color: #666;
  cursor: default;
}

.modal-select {
  cursor: pointer;
}

/* 所属目录分层缩进（option 内为全角空格前缀） */
.modal-select-tree {
  font-family: inherit;
}

.field-error {
  margin: 6px 0 0;
  font-size: 12px;
  color: #e64a4a;
}

.modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 24px 20px;
  border-top: 1px solid #e8ecf4;
}

.modal-btn {
  padding: 8px 22px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: background 0.2s, border-color 0.2s;
}

.modal-btn:disabled {
  opacity: 0.6;
  cursor: not-allowed;
}

.modal-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #333;
}

.modal-btn.ghost:hover:not(:disabled) {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.modal-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.modal-btn.primary:hover:not(:disabled) {
  background: #1246bb;
  border-color: #1246bb;
}
</style>
