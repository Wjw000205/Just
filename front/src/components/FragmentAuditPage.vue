<template>
  <section class="fragment-audit-page">
    <!-- 面包屑导航 -->
    <div class="audit-header">
      <div class="audit-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-parent">审核管理</span> &gt;
        <span class="crumb-now">模板片段审核</span>
      </div>
    </div>

    <div class="audit-content">
      <div class="main-title">模板片段审核</div>

      <!-- 搜索区域 -->
      <div class="search-area">
        <div class="search-item">
          <label class="search-label">模板片段名称</label>
          <input
            v-model="searchForm.name"
            class="search-input"
            placeholder="请输入模板名称"
          />
        </div>
        <div class="search-item">
          <label class="search-label">状态</label>
          <div class="search-select" @click.stop="toggleStatusDropdown">
            <span class="search-select-text">
              {{ searchForm.status || '请选择状态' }}
            </span>
            <svg class="search-select-caret" width="10" height="6" viewBox="0 0 10 6">
              <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
            </svg>
            <ul v-if="statusDropdownVisible" class="search-select-dropdown">
              <li class="search-select-item" @click.stop="selectStatus('')">请选择状态</li>
              <li class="search-select-item" @click.stop="selectStatus('审核中')">审核中</li>
              <li class="search-select-item" @click.stop="selectStatus('已通过')">已通过</li>
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
        <button class="batch-btn primary" @click="handleBatchAudit">批量审核</button>
      </div>

      <!-- 表格区域 -->
      <div class="table-area">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-checkbox"></th>
              <th class="col-index">序号</th>
              <th class="col-name">模板片段名称</th>
              <th class="col-status">状态</th>
              <th class="col-time">最近审核时间</th>
              <th class="col-creator">创建人</th>
              <th class="col-create-time">创建时间</th>
              <th class="col-update-time">更新时间</th>
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
              <td class="col-status">
                <span class="status-badge" :class="item.status === '审核中' ? 'pending' : 'approved'">
                  {{ item.status }}
                </span>
              </td>
              <td class="col-time">{{ item.auditTime || '-' }}</td>
              <td class="col-creator">{{ item.creator }}</td>
              <td class="col-create-time">{{ item.createTime }}</td>
              <td class="col-update-time">{{ item.updateTime }}</td>
              <td class="col-action">
                <div class="action-btns">
                  <button class="action-btn" title="查看" @click="handleView(item)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  </button>
                  <button class="action-btn" title="编辑" @click="handleEdit(item)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
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
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home'])

// 搜索表单
const searchForm = ref({
  name: '',
  status: ''
})

// 下拉框状态
const statusDropdownVisible = ref(false)

// 分页相关
const page = ref(1)
const pageSize = ref(10)
const total = ref(9)

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
  { id: 1, name: '表征结果模板', status: '已通过', auditTime: '2026-03-14 11:33:12', creator: '朱向东', createTime: '2026-03-11 21:41:51', updateTime: '2026-03-14 11:33:12', selected: false },
  { id: 2, name: '表征模板', status: '已通过', auditTime: '2026-03-14 11:33:05', creator: '朱向东', createTime: '2026-03-11 21:39:51', updateTime: '2026-03-14 11:33:05', selected: false },
  { id: 3, name: '表征模板', status: '已通过', auditTime: '2026-03-11 21:43:07', creator: '朱向东', createTime: '2026-03-11 21:39:51', updateTime: '2026-03-11 21:43:07', selected: false },
  { id: 4, name: '表征结果模板', status: '已通过', auditTime: '2026-03-11 21:43:07', creator: '朱向东', createTime: '2026-03-11 21:43:07', updateTime: '2026-03-11 21:43:07', selected: false },
  { id: 5, name: '设备名称/设备厂家/设备型号', status: '已通过', auditTime: '2026-03-08 22:07:19', creator: '朱向东', createTime: '2026-03-08 22:07:03', updateTime: '2026-03-08 22:07:19', selected: false },
  { id: 6, name: '材料基本信息', status: '已通过', auditTime: '2026-01-26 07:25:46', creator: '朱向东', createTime: '2026-01-26 07:22:07', updateTime: '2026-01-26 07:25:46', selected: false },
  { id: 7, name: '材料基本信息', status: '已通过', auditTime: '2026-01-26 07:25:46', creator: '朱向东', createTime: '2026-01-26 07:22:07', updateTime: '2026-01-26 07:25:46', selected: false },
  { id: 8, name: '单轴压缩测试', status: '已通过', auditTime: '2026-01-23 11:57:30', creator: '朱向东', createTime: '2026-01-23 11:57:17', updateTime: '2026-01-23 11:57:30', selected: false },
  { id: 9, name: '单轴压缩', status: '审核中', auditTime: '2026-01-23 11:54:39', creator: '朱向东', createTime: '2026-01-23 11:53:32', updateTime: '2026-01-23 11:54:39', selected: false }
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
const toggleStatusDropdown = () => {
  statusDropdownVisible.value = !statusDropdownVisible.value
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
  searchForm.value = { name: '', status: '' }
  page.value = 1
}

// 分页控制
const goToPage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
}

// 批量操作
const handleBatchAudit = () => {
  const selected = tableData.value.filter(item => item.selected)
  if (selected.length === 0) {
    alert('请先选择要审核的模板片段')
    return
  }
  console.log('批量审核：', selected)
}

// 操作按钮
const handleView = (item) => {
  console.log('查看：', item)
}

const handleEdit = (item) => {
  console.log('编辑：', item)
}

const goHome = () => {
  emit('go-home')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.search-select')) {
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
.fragment-audit-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background-color: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.audit-header {
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

.audit-content {
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
  gap: 16px;
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
  min-width: 160px;
  max-width: 220px;
}

.col-status {
  min-width: 80px;
}

.col-time {
  min-width: 140px;
}

.col-creator {
  min-width: 70px;
}

.col-create-time,
.col-update-time {
  min-width: 130px;
}

.col-action {
  min-width: 80px;
  text-align: center;
}

.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.status-badge.pending {
  background: #fff7e6;
  color: #fa8c16;
}

.status-badge.approved {
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
