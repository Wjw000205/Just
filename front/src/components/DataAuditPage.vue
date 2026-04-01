<template>
  <section class="data-audit-page">
    <div class="audit-layout">
      <!-- 左侧边栏 -->
      <aside class="audit-sidebar">
        <div class="sidebar-search">
          <input
            v-model="categorySearch"
            class="sidebar-search-input"
            placeholder="请输入科学分类名称"
          />
          <button class="sidebar-search-btn">
            <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <circle cx="11" cy="11" r="8"></circle>
              <line x1="21" y1="21" x2="16.65" y2="16.65"></line>
            </svg>
          </button>
        </div>

        <ul class="category-list">
          <li
            class="category-item"
            :class="{ active: selectedCategory === 'all' }"
            @click="selectedCategory = 'all'"
          >
            全部
          </li>
          <li
            v-for="category in categoryList"
            :key="category.id"
            class="category-item"
            :class="{ active: selectedCategory === category.id, expanded: category.expanded }"
          >
            <span class="category-arrow" @click.stop="toggleCategory(category)">
              <svg v-if="category.children" width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="9 18 15 12 9 6"></polyline>
              </svg>
            </span>
            <span class="category-name" @click="selectedCategory = category.id">{{ category.name }}</span>
            <ul v-if="category.expanded && category.children" class="subcategory-list">
              <li
                v-for="child in category.children"
                :key="child.id"
                class="subcategory-item"
                :class="{ active: selectedCategory === child.id }"
                @click.stop="selectedCategory = child.id"
              >
                {{ child.name }}
              </li>
            </ul>
          </li>
        </ul>
      </aside>

      <!-- 主内容区 -->
      <main class="audit-main">
        <!-- 面包屑 -->
        <div class="audit-breadcrumb">
          当前位置：<span class="crumb-link" @click="goHome">首页</span> &gt;
          <span class="crumb-link" @click="goAuditManage">审核管理</span> &gt;
          <span class="crumb-current">数据审核</span>
        </div>

        <!-- 页面标题 -->
        <div class="audit-title">数据审核</div>

        <!-- 搜索筛选区 -->
        <div class="search-filter-area">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">数据集名称</label>
              <input v-model="searchForm.datasetName" class="filter-input" placeholder="请输入数据集名称" />
            </div>
            <div class="filter-item">
              <label class="filter-label">审核状态</label>
              <div class="filter-select" @click.stop="toggleDropdown('auditStatus')">
                <span class="filter-select-text">{{ searchForm.auditStatus || '请选择审核状态' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.auditStatus" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '')">请选择审核状态</li>
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '待审核')">待审核</li>
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '已通过')">已通过</li>
                  <li class="filter-select-item" @click.stop="selectOption('auditStatus', '已驳回')">已驳回</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input v-model="searchForm.templateName" class="filter-input" placeholder="请输入模板名称" />
            </div>
            <div class="filter-item">
              <label class="filter-label">所属部门</label>
              <div class="filter-select" @click.stop="toggleDropdown('department')">
                <span class="filter-select-text">{{ searchForm.department || '请选择部门' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.department" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('department', '')">请选择部门</li>
                  <li class="filter-select-item" @click.stop="selectOption('department', '节点管理部')">节点管理部</li>
                  <li class="filter-select-item" @click.stop="selectOption('department', '社会大众')">社会大众</li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-row second-row">
            <div class="filter-item">
              <label class="filter-label">数据级别</label>
              <div class="filter-select" @click.stop="toggleDropdown('dataLevel')">
                <span class="filter-select-text">{{ searchForm.dataLevel || '请选择数据级别' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.dataLevel" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '')">请选择数据级别</li>
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '公开')">公开</li>
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '高值')">高值</li>
                  <li class="filter-select-item" @click.stop="selectOption('dataLevel', '内部')">内部</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">产业分类</label>
              <div class="filter-select" @click.stop="toggleDropdown('industry')">
                <span class="filter-select-text">{{ searchForm.industry || '请选择产业分类' }}</span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="dropdowns.industry" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectOption('industry', '')">请选择产业分类</li>
                  <li class="filter-select-item" @click.stop="selectOption('industry', '生物医用材料制造')">生物医用材料制造</li>
                  <li class="filter-select-item" @click.stop="selectOption('industry', '先进制造基础零部件')">先进制造基础零部件</li>
                </ul>
              </div>
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

        <!-- 批量操作区 -->
        <div class="batch-action-area">
          <div class="batch-left">
            <label class="checkbox-label">
              <input type="checkbox" v-model="selectAll" @change="handleSelectAll" />
              <span>全选</span>
            </label>
            <button class="batch-btn" @click="handleCancelSelect">取消全选</button>
            <span class="selected-count">已选：{{ selectedCount }}</span>
            <button class="batch-audit-btn" @click="handleBatchAudit">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <polyline points="20 6 9 17 4 12"></polyline>
              </svg>
              批量审核
            </button>
          </div>
        </div>

        <!-- 数据表格 -->
        <div class="table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-checkbox"></th>
                <th class="col-index">序号</th>
                <th class="col-dataset">数据集名称</th>
                <th class="col-scientific">科学分类</th>
                <th class="col-industry">产业分类</th>
                <th class="col-product">产品代码</th>
                <th class="col-level">数据级别</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in tableData" :key="item.id">
                <td class="col-checkbox">
                  <input type="checkbox" v-model="item.selected" />
                </td>
                <td class="col-index">{{ index + 1 }}</td>
                <td class="col-dataset" :title="item.datasetName">{{ item.datasetName }}</td>
                <td class="col-scientific">
                  <span class="tag-badge blue">{{ item.scientificCategory }}</span>
                </td>
                <td class="col-industry">
                  <span class="tag-badge light-blue">{{ item.industryCategory }}</span>
                </td>
                <td class="col-product">{{ item.productCode || '-' }}</td>
                <td class="col-level">
                  <span :class="['level-badge', getLevelClass(item.dataLevel)]">
                    {{ item.dataLevel }}
                  </span>
                </td>
                <td class="col-action">
                  <div class="action-icons">
                    <button class="icon-btn view" title="查看" @click="handleView(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                        <circle cx="12" cy="12" r="3"/>
                      </svg>
                    </button>
                    <button class="icon-btn audit" title="审核" @click="handleAudit(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="20 6 9 17 4 12"></polyline>
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
      </main>
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home', 'go-audit-manage'])

// 分类搜索
const categorySearch = ref('')
const selectedCategory = ref('all')

// 分类列表
const categoryList = ref([
  {
    id: 'bio-science',
    name: '生物医用材料（科学）',
    expanded: false,
    children: []
  },
  {
    id: 'bio-industry',
    name: '生物医用材料（产业）',
    expanded: false,
    children: []
  }
])

// 切换分类展开
const toggleCategory = (category) => {
  category.expanded = !category.expanded
}

// 搜索表单
const searchForm = ref({
  datasetName: '',
  auditStatus: '',
  templateName: '',
  department: '',
  dataLevel: '',
  industry: ''
})

// 下拉框状态
const dropdowns = ref({
  auditStatus: false,
  department: false,
  dataLevel: false,
  industry: false
})

// 表格数据
const tableData = ref([
  {
    id: 1,
    datasetName: '纳米羟基磷灰石体表征数据',
    scientificCategory: '羟基磷灰石',
    industryCategory: '生物医用材料制造',
    productCode: '',
    dataLevel: '公开',
    selected: false
  },
  {
    id: 2,
    datasetName: '高通量羟基磷灰石制备与表征数据集',
    scientificCategory: '羟基磷灰石',
    industryCategory: '生物医用材料制造',
    productCode: '',
    dataLevel: '公开',
    selected: false
  },
  {
    id: 3,
    datasetName: 'DLP3D磷酸陶瓷打印高值数据',
    scientificCategory: '双相磷酸钙',
    industryCategory: '生物医用材料制造',
    productCode: '',
    dataLevel: '高值',
    selected: false
  },
  {
    id: 4,
    datasetName: '0318生物医用对接数据集',
    scientificCategory: 'α-磷酸三钙',
    industryCategory: '铝及铝合金制造',
    productCode: '',
    dataLevel: '高值',
    selected: false
  },
  {
    id: 5,
    datasetName: '表格1',
    scientificCategory: '羟基磷灰石',
    industryCategory: '先进制造基础零部件',
    productCode: '',
    dataLevel: '高值',
    selected: false
  },
  {
    id: 6,
    datasetName: '22',
    scientificCategory: '羟基磷灰石',
    industryCategory: '先进制造基础零部件',
    productCode: '',
    dataLevel: '高值',
    selected: false
  }
])

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(6)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 全选
const selectAll = computed({
  get: () => tableData.value.length > 0 && tableData.value.every(item => item.selected),
  set: (value) => tableData.value.forEach(item => item.selected = value)
})

// 已选数量
const selectedCount = computed(() => tableData.value.filter(item => item.selected).length)

// 获取数据级别样式
const getLevelClass = (level) => {
  const map = {
    '公开': 'public',
    '高值': 'high',
    '内部': 'internal'
  }
  return map[level] || 'public'
}

// 下拉框控制
const toggleDropdown = (name) => {
  Object.keys(dropdowns.value).forEach(key => {
    if (key !== name) dropdowns.value[key] = false
  })
  dropdowns.value[name] = !dropdowns.value[name]
}

const selectOption = (name, value) => {
  searchForm.value[name] = value
  dropdowns.value[name] = false
}

// 操作函数
const handleSearch = () => {
  console.log('搜索:', searchForm.value)
}

const handleReset = () => {
  searchForm.value = {
    datasetName: '',
    auditStatus: '',
    templateName: '',
    department: '',
    dataLevel: '',
    industry: ''
  }
}

const handleSelectAll = () => {
  const newValue = !selectAll.value
  tableData.value.forEach(item => item.selected = newValue)
}

const handleCancelSelect = () => {
  tableData.value.forEach(item => item.selected = false)
}

const handleBatchAudit = () => {
  const selected = tableData.value.filter(item => item.selected)
  if (selected.length === 0) {
    alert('请先选择要审核的数据')
    return
  }
  console.log('批量审核:', selected)
  alert(`批量审核 ${selected.length} 条数据`)
}

const handleView = (item) => {
  console.log('查看:', item)
  alert(`查看: ${item.datasetName}`)
}

const handleAudit = (item) => {
  console.log('审核:', item)
  alert(`审核: ${item.datasetName}`)
}

const goHome = () => {
  emit('go-home')
}

const goAuditManage = () => {
  emit('go-audit-manage')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
    Object.keys(dropdowns.value).forEach(key => {
      dropdowns.value[key] = false
    })
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
.data-audit-page {
  background: #f0f2f5;
  min-height: calc(100vh - 110px);
  padding: 16px 20px 20px;
}

.audit-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: start;
}

/* 左侧边栏 */
.audit-sidebar {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.sidebar-search {
  display: flex;
  gap: 8px;
  margin-bottom: 16px;
}

.sidebar-search-input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 13px;
}

.sidebar-search-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.sidebar-search-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 36px;
  height: 36px;
  border: 1px solid #e0e0e0;
  background: #fff;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
}

.sidebar-search-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.category-list {
  list-style: none;
  padding: 0;
  margin: 0;
}

.category-item {
  padding: 10px 12px;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  transition: all 0.2s;
  font-size: 13px;
  display: flex;
  align-items: center;
  gap: 6px;
}

.category-item:hover {
  background: #f5f7fb;
  color: #1a5ce6;
}

.category-item.active {
  background: #e6f7ff;
  color: #1a5ce6;
  font-weight: 500;
}

.category-arrow {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  transition: transform 0.2s;
}

.category-item.expanded .category-arrow {
  transform: rotate(90deg);
}

.category-name {
  flex: 1;
}

.subcategory-list {
  list-style: none;
  padding: 0;
  margin: 4px 0 0 22px;
}

.subcategory-item {
  padding: 8px 12px;
  border-radius: 4px;
  color: #666;
  cursor: pointer;
  font-size: 13px;
  transition: all 0.2s;
}

.subcategory-item:hover {
  color: #1a5ce6;
}

.subcategory-item.active {
  color: #1a5ce6;
  font-weight: 500;
}

/* 主内容区 */
.audit-main {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.audit-breadcrumb {
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

.audit-title {
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
  color: #666;
  white-space: nowrap;
}

.filter-input {
  width: 160px;
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

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #e0e0e0;
  background: #fff;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  min-width: 140px;
}

.filter-select-text {
  flex: 1;
  color: #666;
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

/* 批量操作区 */
.batch-action-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
  padding: 12px 0;
  border-bottom: 1px solid #e8ecf4;
}

.batch-left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}

.checkbox-label input {
  width: 14px;
  height: 14px;
  accent-color: #1a5ce6;
}

.batch-btn {
  padding: 4px 12px;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
}

.batch-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.selected-count {
  font-size: 13px;
  color: #666;
}

.batch-audit-btn {
  display: flex;
  align-items: center;
  gap: 4px;
  padding: 6px 14px;
  border: none;
  background: #1a5ce6;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.batch-audit-btn:hover {
  background: #1246bb;
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
.col-checkbox {
  width: 40px;
  text-align: center;
}

.col-index {
  width: 50px;
  text-align: center;
}

.col-dataset {
  min-width: 200px;
  max-width: 300px;
}

.col-scientific,
.col-industry {
  min-width: 120px;
}

.col-product {
  width: 100px;
}

.col-level {
  width: 80px;
  text-align: center;
}

.col-action {
  width: 100px;
  text-align: center;
}

/* 标签样式 */
.tag-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.tag-badge.blue {
  color: #1a5ce6;
  background: #e8f0fe;
}

.tag-badge.light-blue {
  color: #1a5ce6;
  background: #e6f7ff;
}

.level-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.level-badge.public {
  color: #67c23a;
  background: #f0f9eb;
}

.level-badge.high {
  color: #f56c6c;
  background: #fef0f0;
}

.level-badge.internal {
  color: #909399;
  background: #f4f4f5;
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

.icon-btn.audit:hover {
  background: #f0f9eb;
  color: #67c23a;
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
