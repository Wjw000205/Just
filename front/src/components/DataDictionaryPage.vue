<template>
  <section class="data-dictionary-page">
    <div class="dict-main">
      <!-- 面包屑 -->
      <div class="dict-breadcrumb">
        当前位置：<span class="crumb-link" @click="goHome">首页</span> &gt;
        <span class="crumb-link" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-current">数据字典</span>
      </div>

      <!-- 页面标题 -->
      <div class="dict-title">数据字典</div>

      <!-- 搜索筛选区 -->
      <div class="search-filter-area">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">数据名称</label>
            <input v-model="searchForm.dataName" class="filter-input" placeholder="请输入数据名称" />
          </div>
          <div class="filter-item">
            <label class="filter-label">数据描述</label>
            <input v-model="searchForm.dataDesc" class="filter-input" placeholder="请输入数据描述" />
          </div>
          <div class="filter-buttons">
            <button class="filter-btn primary" @click="handleSearch">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <circle cx="11" cy="11" r="8"></circle>
                <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
              </svg>
              查询
            </button>
            <button class="filter-btn ghost" @click="handleReset">重置</button>
          </div>
        </div>
      </div>

      <!-- 数据表格 -->
      <div class="table-area">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-index">序号</th>
              <th class="col-name">数据名称</th>
              <th class="col-desc">权限描述</th>
              <th class="col-count">数据项</th>
              <th class="col-updater">更新人</th>
              <th class="col-time">更新时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="item.id">
              <td class="col-index">{{ index + 1 }}</td>
              <td class="col-name">{{ item.dataName }}</td>
              <td class="col-desc">{{ item.description }}</td>
              <td class="col-count">{{ item.itemCount }}</td>
              <td class="col-updater">{{ item.updater }}</td>
              <td class="col-time">{{ item.updateTime }}</td>
              <td class="col-action">
                <div class="action-icons">
                  <button class="icon-btn view" title="查看" @click="handleView(item)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  </button>
                  <button class="icon-btn edit" title="编辑" @click="handleEdit(item)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
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
        <span class="total">共 {{ total }} 条</span>
        <button class="page-btn" :disabled="currentPage === 1" @click="currentPage--">上一页</button>
        <span class="page-current">{{ currentPage }}</span>
        <button class="page-btn" :disabled="currentPage >= totalPages" @click="currentPage++">下一页</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, computed } from 'vue'

const emit = defineEmits(['go-home', 'go-system-manage'])

// 搜索表单
const searchForm = ref({
  dataName: '',
  dataDesc: ''
})

// 表格数据
const tableData = ref([
  {
    id: 1,
    dataName: '模板创建-图片格式',
    description: '用于配置模板设计中 "图片格式" 字典项',
    itemCount: 10,
    updater: 'admin',
    updateTime: '2025-09-19 14:04:40'
  },
  {
    id: 2,
    dataName: '模板创建-文件格式',
    description: '用于配置模板设计中 "文件格式" 字典项',
    itemCount: 17,
    updater: 'admin',
    updateTime: '2025-10-17 09:46:46'
  },
  {
    id: 3,
    dataName: '模板设计-文件空格式11',
    description: '修改数据项查看',
    itemCount: 7,
    updater: 'admin',
    updateTime: '2025-09-04 10:16:01'
  },
  {
    id: 4,
    dataName: '数据集-数式类型',
    description: '用于配置数据集的 "数据类型" 字典项',
    itemCount: 6,
    updater: 'zhuxiangdong',
    updateTime: '2026-03-08 19:43:29'
  }
])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(4)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 操作函数
const handleSearch = () => {
  console.log('搜索:', searchForm.value)
}

const handleReset = () => {
  searchForm.value = {
    dataName: '',
    dataDesc: ''
  }
}

const handleView = (item) => {
  console.log('查看:', item)
  alert(`查看: ${item.dataName}`)
}

const handleEdit = (item) => {
  console.log('编辑:', item)
  alert(`编辑: ${item.dataName}`)
}

const goHome = () => {
  emit('go-home')
}

const goSystemManage = () => {
  emit('go-system-manage')
}
</script>

<style scoped>
.data-dictionary-page {
  background: #f0f2f5;
  min-height: calc(100vh - 110px);
  padding: 16px 20px 20px;
}

.dict-main {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.dict-breadcrumb {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
}

.crumb-link {
  color: #1a5ce6;
  cursor: pointer;
}

.crumb-link:hover {
  text-decoration: underline;
}

.crumb-current {
  color: #333;
}

.dict-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 20px;
}

/* 搜索筛选区 */
.search-filter-area {
  background: #f8f9fc;
  border-radius: 6px;
  padding: 16px 20px;
  margin-bottom: 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
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
  width: 180px;
  padding: 6px 10px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 13px;
  background: #fff;
}

.filter-input:focus {
  outline: none;
  border-color: #1a5ce6;
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

.filter-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.filter-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

/* 表格 */
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

/* 列宽 */
.col-index {
  width: 60px;
  text-align: center;
}

.col-name {
  min-width: 150px;
}

.col-desc {
  min-width: 250px;
  max-width: 400px;
}

.col-count {
  width: 80px;
  text-align: center;
}

.col-updater {
  width: 100px;
}

.col-time {
  width: 150px;
}

.col-action {
  width: 100px;
  text-align: center;
}

/* 操作图标 */
.action-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.icon-btn.delete:hover {
  background: #fef0f0;
  color: #f56c6c;
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
