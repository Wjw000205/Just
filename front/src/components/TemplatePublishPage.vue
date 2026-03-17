<template>
  <section class="template-publish-page">
    <!-- 面包屑导航 -->
    <div class="publish-header">
      <div class="publish-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-parent">审核管理</span> &gt;
        <span class="crumb-now">模板发布</span>
      </div>
    </div>

    <div class="publish-layout">
      <!-- 左侧分类树 -->
      <aside class="publish-sidebar">
        <div class="sidebar-search">
          <input class="sidebar-input" placeholder="请输入模板标签名称" />
          <button class="sidebar-search-btn">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"/>
              <path d="M21 21l-4.35-4.35"/>
            </svg>
          </button>
        </div>
        <div class="sidebar-menu-btn">
          <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="3" y1="6" x2="21" y2="6"/>
            <line x1="3" y1="12" x2="21" y2="12"/>
            <line x1="3" y1="18" x2="21" y2="18"/>
          </svg>
        </div>
        <ul class="sidebar-list">
          <li class="sidebar-item active">
            <span class="item-text">全部</span>
          </li>
          <li class="sidebar-item">
            <span class="item-text">生物医用材料（科学）-通用</span>
          </li>
          <li class="sidebar-item">
            <span class="item-text">生物医用材料（产业）-通用</span>
          </li>
        </ul>
      </aside>

      <!-- 右侧主内容区 -->
      <section class="publish-main">
        <div class="main-title">模板发布</div>

        <!-- 搜索区域 -->
        <div class="search-area">
          <div class="search-item">
            <label class="search-label">模板名称</label>
            <input
              v-model="searchForm.name"
              class="search-input"
              placeholder="请输入模板名称"
            />
          </div>
          <div class="search-item">
            <label class="search-label">模板类型</label>
            <div class="search-select" @click.stop="toggleTypeDropdown">
              <span class="search-select-text">
                {{ searchForm.type || '请选择模板...' }}
              </span>
              <svg class="search-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="typeDropdownVisible" class="search-select-dropdown">
                <li class="search-select-item" @click.stop="selectType('')">请选择模板...</li>
                <li class="search-select-item" @click.stop="selectType('模板')">模板</li>
                <li class="search-select-item" @click.stop="selectType('模板片段')">模板片段</li>
              </ul>
            </div>
          </div>
          <div class="search-item">
            <label class="search-label">发布状态</label>
            <div class="search-select" @click.stop="toggleStatusDropdown">
              <span class="search-select-text">
                {{ searchForm.status || '请选择状态' }}
              </span>
              <svg class="search-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="statusDropdownVisible" class="search-select-dropdown">
                <li class="search-select-item" @click.stop="selectStatus('')">请选择状态</li>
                <li class="search-select-item" @click.stop="selectStatus('待预发布')">待预发布</li>
                <li class="search-select-item" @click.stop="selectStatus('已发布')">已发布</li>
              </ul>
            </div>
          </div>
          <button class="search-btn primary" @click="handleSearch">查询</button>
          <button class="search-btn ghost" @click="handleReset">重置</button>
        </div>

        <!-- 批量操作区域 -->
        <div class="batch-area">
          <label class="batch-checkbox">
            <input type="checkbox" v-model="selectAll" @change="handleSelectAll" />
            <span>全选</span>
          </label>
          <button class="batch-btn" @click="cancelSelectAll">取消全选</button>
          <span class="batch-info">已选：<span class="batch-count">{{ selectedCount }}</span></span>
          <button class="batch-btn primary" @click="handleBatchPublish">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px;">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            批量发布
          </button>
          <button class="batch-btn ghost download" @click="handleDownload">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2" style="margin-right: 4px;">
              <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
              <polyline points="7 10 12 15 17 10"/>
              <line x1="12" y1="15" x2="12" y2="3"/>
            </svg>
            下载评审意见表
          </button>
        </div>

        <!-- 表格区域 -->
        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-checkbox"></th>
                <th class="col-index">序号</th>
                <th class="col-name">模板名称</th>
                <th class="col-tag">模板标签</th>
                <th class="col-type">模板类型</th>
                <th class="col-agree">是否同意发布</th>
                <th class="col-status">发布状态</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in tableData" :key="item.id">
                <td class="col-checkbox">
                  <input type="checkbox" v-model="item.selected" />
                </td>
                <td class="col-index">{{ (page - 1) * pageSize + index + 1 }}</td>
                <td class="col-name">{{ item.name }}</td>
                <td class="col-tag">
                  <span class="tag-badge" v-if="item.tag">{{ item.tag }}</span>
                </td>
                <td class="col-type">{{ item.type }}</td>
                <td class="col-agree">
                  <span class="agree-badge">{{ item.agree }}</span>
                </td>
                <td class="col-status">
                  <span class="status-badge" :class="item.status === '待预发布' ? 'pending' : 'published'">
                    {{ item.status }}
                  </span>
                </td>
                <td class="col-action">
                  <div class="action-btns">
                    <button class="action-btn" title="查看" @click="handleView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>
                    <button class="action-btn" title="下载" @click="handleDownloadItem(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                        <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                        <polyline points="7 10 12 15 17 10"/>
                        <line x1="12" y1="15" x2="12" y2="3"/>
                      </svg>
                    </button>
                    <button
                      class="action-btn favorite"
                      :class="{ active: item.isFavorited }"
                      :title="item.isFavorited ? '取消收藏' : '收藏'"
                      @click="handleFavorite(item)"
                    >
                      <svg width="16" height="16" viewBox="0 0 24 24" :fill="item.isFavorited ? 'currentColor' : 'none'" stroke="#1a5ce6" stroke-width="2">
                        <polygon points="12 2 15.09 8.26 22 9.27 17 14.14 18.18 21.02 12 17.77 5.82 21.02 7 14.14 2 9.27 8.91 8.26 12 2"/>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </tbody>
          </table>

          <!-- 分页 -->
          <div class="pagination">
            <span class="pagination-total">共 {{ total }} 条</span>
            <div class="pagination-nav">
              <button
                class="pagination-btn"
                :disabled="page <= 1"
                @click="goToPage(page - 1)"
              >
                上一页
              </button>
              <div class="pagination-pages">
                <button
                  v-for="p in displayedPages"
                  :key="p"
                  class="page-number"
                  :class="{ active: p === page }"
                  @click="goToPage(p)"
                >
                  {{ p }}
                </button>
              </div>
              <button
                class="pagination-btn"
                :disabled="page >= totalPages"
                @click="goToPage(page + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home'])

// 搜索表单
const searchForm = ref({
  name: '',
  type: '',
  status: ''
})

// 下拉框状态
const typeDropdownVisible = ref(false)
const statusDropdownVisible = ref(false)

// 分页相关
const page = ref(1)
const pageSize = ref(10)
const total = ref(8)

const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 显示的页码
const displayedPages = computed(() => {
  const pages = []
  const maxDisplay = 5
  let start = Math.max(1, page.value - 2)
  let end = Math.min(totalPages.value, start + maxDisplay - 1)

  if (end - start < maxDisplay - 1) {
    start = Math.max(1, end - maxDisplay + 1)
  }

  for (let i = start; i <= end; i++) {
    pages.push(i)
  }
  return pages
})

// 模拟表格数据
const tableData = ref([
  { id: 1, name: '生物材料临床试验表征通用模板', tag: '生物医用材料（科…', type: '模板', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 2, name: '生物材料体内实验表征通用模板', tag: '生物医用材料（科…', type: '模板', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 3, name: '生物材料体外实验表征通用模板', tag: '生物医用材料（科…', type: '模板', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 4, name: '生物材料力学性能表征通用模板', tag: '生物医用材料（科…', type: '模板', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 5, name: '表征结果模板', tag: '', type: '模板片段', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 6, name: '表征模板', tag: '', type: '模板片段', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 7, name: '生物材料通用模板', tag: '生物医用材料（科…', type: '模板', agree: '同意', status: '待预发布', isFavorited: false, selected: false },
  { id: 8, name: '自组装羟基磷灰石纳米材料表征模板', tag: '生物医用材料（科…', type: '模板', agree: '同意', status: '待预发布', isFavorited: false, selected: false }
])

// 全选相关
const selectAll = ref(false)
const selectedCount = computed(() => tableData.value.filter(item => item.selected).length)

const handleSelectAll = () => {
  tableData.value.forEach(item => item.selected = selectAll.value)
}

const cancelSelectAll = () => {
  selectAll.value = false
  tableData.value.forEach(item => item.selected = false)
}

// 下拉框控制
const toggleTypeDropdown = () => {
  typeDropdownVisible.value = !typeDropdownVisible.value
  statusDropdownVisible.value = false
}

const toggleStatusDropdown = () => {
  statusDropdownVisible.value = !statusDropdownVisible.value
  typeDropdownVisible.value = false
}

const selectType = (type) => {
  searchForm.value.type = type
  typeDropdownVisible.value = false
}

const selectStatus = (status) => {
  searchForm.value.status = status
  statusDropdownVisible.value = false
}

// 搜索和重置
const handleSearch = () => {
  page.value = 1
}

const handleReset = () => {
  searchForm.value = { name: '', type: '', status: '' }
  page.value = 1
}

// 分页控制
const goToPage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
}

// 批量操作
const handleBatchPublish = () => {
  const selected = tableData.value.filter(item => item.selected)
  if (selected.length === 0) {
    alert('请先选择要发布的模板')
    return
  }
  console.log('批量发布：', selected)
}

const handleDownload = () => {
  console.log('下载评审意见表')
}

// 操作按钮
const handleView = (item) => {
  console.log('查看：', item)
}

const handleDownloadItem = (item) => {
  console.log('下载：', item)
}

const handleFavorite = (item) => {
  item.isFavorited = !item.isFavorited
}

const goHome = () => {
  emit('go-home')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.search-select')) {
    typeDropdownVisible.value = false
    statusDropdownVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  document.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.template-publish-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.publish-header {
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

.crumb-parent {
  color: #333;
}

.crumb-now {
  color: #333;
  font-weight: 500;
}

.publish-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: stretch;
}

/* 左侧边栏 */
.publish-sidebar {
  background: #fff;
  border-radius: 4px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.sidebar-search {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 12px;
}

.sidebar-input {
  flex: 1;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.sidebar-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
}

.sidebar-menu-btn {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  margin-bottom: 8px;
  color: #666;
  cursor: pointer;
}

.sidebar-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.sidebar-item {
  padding: 8px 0;
  font-size: 13px;
  color: #666;
  cursor: pointer;
  border-bottom: 1px solid #f0f0f0;
}

.sidebar-item.active {
  color: #1a5ce6;
  font-weight: 500;
}

.sidebar-item:last-child {
  border-bottom: none;
}

.item-text {
  line-height: 1.4;
}

/* 右侧主内容区 */
.publish-main {
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

/* 搜索区域 */
.search-area {
  display: flex;
  align-items: center;
  gap: 16px;
  margin-bottom: 16px;
  flex-wrap: wrap;
}

.search-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.search-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.search-input {
  width: 160px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
}

.search-select {
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
  min-width: 140px;
}

.search-select-text {
  flex: 1;
}

.search-select-caret {
  margin-left: 4px;
  color: #999;
}

.search-select-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  min-width: 140px;
  padding: 4px 0;
  margin: 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 10;
}

.search-select-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.search-select-item:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.search-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
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

/* 批量操作区域 */
.batch-area {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #f0f0f0;
}

.batch-checkbox {
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.batch-btn {
  display: flex;
  align-items: center;
  padding: 4px 12px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
}

.batch-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.batch-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.batch-btn.primary:hover {
  background: #1246bb;
}

.batch-btn.ghost.download {
  color: #1a5ce6;
  border-color: #d4dae6;
}

.batch-btn.ghost.download:hover {
  border-color: #1a5ce6;
}

.batch-info {
  font-size: 13px;
  color: #666;
}

.batch-count {
  color: #1a5ce6;
  font-weight: 500;
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
  border-bottom: 1px solid #f0f0f0;
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

.col-checkbox {
  width: 40px;
  text-align: center;
}

.col-index {
  width: 50px;
  text-align: center;
}

.col-name {
  min-width: 180px;
  max-width: 240px;
}

.col-tag {
  min-width: 120px;
}

.col-type {
  min-width: 80px;
}

.col-agree {
  min-width: 100px;
}

.col-status {
  min-width: 80px;
}

.col-action {
  min-width: 100px;
  text-align: center;
}

.tag-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #e8f0fe;
  color: #1a5ce6;
  border-radius: 3px;
  font-size: 12px;
}

.agree-badge {
  display: inline-block;
  padding: 2px 8px;
  background: #f6ffed;
  color: #52c41a;
  border-radius: 3px;
  font-size: 12px;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.status-badge.pending {
  background: #e6f7ff;
  color: #1890ff;
}

.status-badge.published {
  background: #f6ffed;
  color: #52c41a;
}

.action-btns {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.action-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 26px;
  height: 26px;
  border-radius: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  transition: background 0.2s;
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

.action-btn.favorite.active:hover {
  background: #fff5e6;
}

.action-btn svg {
  display: block;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 0 0;
  font-size: 13px;
  color: #666;
}

.pagination-total {
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
  transition: all 0.2s;
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
  align-items: center;
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
  transition: all 0.2s;
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
</style>
