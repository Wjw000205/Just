<template>
  <section class="catalog-publish-page">
    <div class="catalog-main">
      <!-- 面包屑 -->
      <div class="catalog-breadcrumb">
        当前位置：<span class="crumb-link" @click="goHome">首页</span> &gt;
        <span class="crumb-link" @click="goAuditManage">审核管理</span> &gt;
        <span class="crumb-current">目录发布</span>
      </div>

      <!-- 页面标题 -->
      <div class="catalog-title">目录发布</div>

      <!-- 搜索筛选区 -->
      <div class="search-filter-area">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">科学分类名称</label>
            <input v-model="searchForm.categoryName" class="filter-input" placeholder="请输入科学分类名称" />
          </div>
          <div class="filter-item">
            <label class="filter-label">科学分类等级</label>
            <div class="filter-select" @click.stop="toggleDropdown('categoryLevel')">
              <span class="filter-select-text">{{ searchForm.categoryLevel || '请选择科学分类等级' }}</span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="dropdowns.categoryLevel" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="selectOption('categoryLevel', '')">请选择科学分类等级</li>
                <li class="filter-select-item" @click.stop="selectOption('categoryLevel', '一级')">一级</li>
                <li class="filter-select-item" @click.stop="selectOption('categoryLevel', '二级')">二级</li>
                <li class="filter-select-item" @click.stop="selectOption('categoryLevel', '三级')">三级</li>
                <li class="filter-select-item" @click.stop="selectOption('categoryLevel', '四级')">四级</li>
              </ul>
            </div>
          </div>
          <div class="filter-item">
            <label class="filter-label">所属科学分类</label>
            <div class="filter-select" @click.stop="toggleDropdown('parentCategory')">
              <span class="filter-select-text">{{ searchForm.parentCategory || '请选择' }}</span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="dropdowns.parentCategory" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="selectOption('parentCategory', '')">请选择</li>
                <li class="filter-select-item" @click.stop="selectOption('parentCategory', '生物医用材料（科学）')">生物医用材料（科学）</li>
                <li class="filter-select-item" @click.stop="selectOption('parentCategory', '生物医用无机材料')">生物医用无机材料</li>
                <li class="filter-select-item" @click.stop="selectOption('parentCategory', '磷酸钙陶瓷')">磷酸钙陶瓷</li>
              </ul>
            </div>
          </div>
          <div class="filter-item">
            <label class="filter-label">科学分类来源</label>
            <div class="filter-select" @click.stop="toggleDropdown('categorySource')">
              <span class="filter-select-text">{{ searchForm.categorySource || '请选择科学分类来源' }}</span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="dropdowns.categorySource" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="selectOption('categorySource', '')">请选择科学分类来源</li>
                <li class="filter-select-item" @click.stop="selectOption('categorySource', '数据库')">数据库</li>
                <li class="filter-select-item" @click.stop="selectOption('categorySource', '人工录入')">人工录入</li>
              </ul>
            </div>
          </div>
        </div>
        <div class="filter-row second-row">
          <div class="filter-item">
            <label class="filter-label">发布状态</label>
            <div class="filter-select" @click.stop="toggleDropdown('publishStatus')">
              <span class="filter-select-text">{{ searchForm.publishStatus || '请选择发布状态' }}</span>
              <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
              </svg>
              <ul v-if="dropdowns.publishStatus" class="filter-select-dropdown">
                <li class="filter-select-item" @click.stop="selectOption('publishStatus', '')">请选择发布状态</li>
                <li class="filter-select-item" @click.stop="selectOption('publishStatus', '成功发布')">成功发布</li>
                <li class="filter-select-item" @click.stop="selectOption('publishStatus', '发布审核中')">发布审核中</li>
                <li class="filter-select-item" @click.stop="selectOption('publishStatus', '已驳回')">已驳回</li>
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

      <!-- 数据表格 -->
      <div class="table-area">
        <table class="data-table tree-table">
          <thead>
            <tr>
              <th class="col-index">序号</th>
              <th class="col-category-name">科学分类名称</th>
              <th class="col-category-level">科学分类等级</th>
              <th class="col-parent-category">所属科学分类</th>
              <th class="col-source">科学分类来源</th>
              <th class="col-status">发布状态</th>
              <th class="col-reason">驳回理由</th>
              <th class="col-publisher">预发布人</th>
              <th class="col-time">预发布时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(item, index) in flattenedData" :key="item.id">
              <tr :class="{ 'parent-row': item.isParent, 'child-row': !item.isParent }">
                <td class="col-index">{{ item.isParent ? index + 1 : '' }}</td>
                <td class="col-category-name">
                  <div class="category-cell" :style="{ paddingLeft: (item.level * 20) + 'px' }">
                    <span
                      v-if="item.children && item.children.length > 0"
                      class="expand-icon"
                      :class="{ expanded: item.expanded }"
                      @click="toggleExpand(item)"
                    >
                      <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="9 18 15 12 9 6"></polyline>
                      </svg>
                    </span>
                    <span v-else class="expand-placeholder"></span>
                    <span class="category-name">{{ item.name }}</span>
                  </div>
                </td>
                <td class="col-category-level">{{ item.levelText }}</td>
                <td class="col-parent-category">{{ item.parentName || '-' }}</td>
                <td class="col-source">{{ item.source }}</td>
                <td class="col-status">
                  <span :class="['status-badge', getStatusClass(item.status)]">
                    {{ item.status }}
                  </span>
                </td>
                <td class="col-reason">{{ item.rejectReason || '-' }}</td>
                <td class="col-publisher">{{ item.publisher }}</td>
                <td class="col-time">{{ item.publishTime }}</td>
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
                    <button class="icon-btn delete" title="删除" @click="handleDelete(item)">
                      <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                        <polyline points="3 6 5 6 21 6"/>
                        <path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/>
                      </svg>
                    </button>
                  </div>
                </td>
              </tr>
            </template>
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
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home', 'go-audit-manage'])

// 搜索表单
const searchForm = ref({
  categoryName: '',
  categoryLevel: '',
  parentCategory: '',
  categorySource: '',
  publishStatus: ''
})

// 下拉框状态
const dropdowns = ref({
  categoryLevel: false,
  parentCategory: false,
  categorySource: false,
  publishStatus: false
})

// 树形数据
const treeData = ref([
  {
    id: 1,
    name: '生物医用材料（科学）',
    level: 0,
    levelText: '一级',
    parentName: '',
    source: '数据库',
    status: '成功发布',
    rejectReason: '',
    publisher: 'zhuxiangdong',
    publishTime: '2026-02-04 18:01:51',
    expanded: true,
    children: [
      {
        id: 11,
        name: '生物医用无机材料',
        level: 1,
        levelText: '二级',
        parentName: '生物医用材料（科学）',
        source: '数据库',
        status: '发布审核中',
        rejectReason: '',
        publisher: 'zhuxiangdong',
        publishTime: '2026-02-04 18:04:05',
        expanded: true,
        children: [
          {
            id: 111,
            name: '磷酸钙陶瓷',
            level: 2,
            levelText: '三级',
            parentName: '生物医用无机材料',
            source: '数据库',
            status: '发布审核中',
            rejectReason: '',
            publisher: 'zhuxiangdong',
            publishTime: '2026-02-04 14:50:03',
            expanded: true,
            children: [
              {
                id: 1111,
                name: '羟基磷灰石',
                level: 3,
                levelText: '四级',
                parentName: '磷酸钙陶瓷',
                source: '数据库',
                status: '发布审核中',
                rejectReason: '',
                publisher: 'zhuxiangdong',
                publishTime: '2026-02-04 16:10:55',
                children: []
              },
              {
                id: 1112,
                name: 'β-磷酸三钙',
                level: 3,
                levelText: '四级',
                parentName: '磷酸钙陶瓷',
                source: '数据库',
                status: '发布审核中',
                rejectReason: '',
                publisher: 'zhuxiangdong',
                publishTime: '2026-02-04 16:12:41',
                children: []
              },
              {
                id: 1113,
                name: 'α-磷酸三钙',
                level: 3,
                levelText: '四级',
                parentName: '磷酸钙陶瓷',
                source: '数据库',
                status: '发布审核中',
                rejectReason: '',
                publisher: 'zhuxiangdong',
                publishTime: '2026-02-04 19:25:28',
                children: []
              },
              {
                id: 1114,
                name: '双相磷酸钙',
                level: 3,
                levelText: '四级',
                parentName: '磷酸钙陶瓷',
                source: '数据库',
                status: '发布审核中',
                rejectReason: '',
                publisher: 'zhuxiangdong',
                publishTime: '2026-02-04 19:32:04',
                children: []
              }
            ]
          }
        ]
      },
      {
        id: 12,
        name: '生物医用金属材料',
        level: 1,
        levelText: '二级',
        parentName: '生物医用材料（科学）',
        source: '数据库',
        status: '发布审核中',
        rejectReason: '',
        publisher: 'zhuxiangdong',
        publishTime: '2026-02-04 19:32:38',
        expanded: false,
        children: []
      }
    ]
  }
])

// 扁平化树形数据（用于显示）
const flattenedData = computed(() => {
  const result = []
  const flatten = (items, isParent = true) => {
    items.forEach(item => {
      result.push({ ...item, isParent })
      if (item.children && item.children.length > 0 && item.expanded) {
        flatten(item.children, false)
      }
    })
  }
  flatten(treeData.value)
  return result
})

// 分页
const currentPage = ref(1)
const pageSize = ref(10)
const total = ref(8)
const totalPages = computed(() => Math.ceil(total.value / pageSize.value))

// 获取状态样式
const getStatusClass = (status) => {
  const map = {
    '成功发布': 'success',
    '发布审核中': 'warning',
    '已驳回': 'danger'
  }
  return map[status] || 'default'
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

// 展开/收起
const toggleExpand = (item) => {
  item.expanded = !item.expanded
}

// 操作函数
const handleSearch = () => {
  console.log('搜索:', searchForm.value)
}

const handleReset = () => {
  searchForm.value = {
    categoryName: '',
    categoryLevel: '',
    parentCategory: '',
    categorySource: '',
    publishStatus: ''
  }
}

const handleView = (item) => {
  console.log('查看:', item)
  alert(`查看: ${item.name}`)
}

const handleEdit = (item) => {
  console.log('编辑:', item)
  alert(`编辑: ${item.name}`)
}

const handleDelete = (item) => {
  console.log('删除:', item)
  if (confirm(`确定要删除 "${item.name}" 吗？`)) {
    alert(`已删除: ${item.name}`)
  }
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
.catalog-publish-page {
  background: #f0f2f5;
  min-height: calc(100vh - 110px);
  padding: 16px 20px 20px;
}

.catalog-main {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.catalog-breadcrumb {
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

.catalog-title {
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
  width: 50px;
  text-align: center;
}

.col-category-name {
  min-width: 200px;
}

.col-category-level {
  width: 100px;
}

.col-parent-category {
  min-width: 150px;
}

.col-source {
  width: 100px;
}

.col-status {
  width: 100px;
}

.col-reason {
  width: 120px;
}

.col-publisher {
  width: 100px;
}

.col-time {
  width: 140px;
}

.col-action {
  width: 100px;
  text-align: center;
}

/* 树形表格样式 */
.parent-row {
  font-weight: 500;
}

.child-row {
  color: #666;
}

.category-cell {
  display: flex;
  align-items: center;
}

.expand-icon {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  margin-right: 6px;
  cursor: pointer;
  transition: transform 0.2s;
  color: #999;
}

.expand-icon.expanded {
  transform: rotate(90deg);
}

.expand-placeholder {
  width: 16px;
  height: 16px;
  margin-right: 6px;
}

.category-name {
  flex: 1;
}

/* 状态标签 */
.status-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
  white-space: nowrap;
}

.status-badge.success {
  color: #67c23a;
  background: #f0f9eb;
}

.status-badge.warning {
  color: #e6a23c;
  background: #fdf6ec;
}

.status-badge.danger {
  color: #f56c6c;
  background: #fef0f0;
}

.status-badge.default {
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
