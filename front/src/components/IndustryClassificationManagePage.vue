<template>
  <section class="industry-class-page">
    <!-- 面包屑导航 -->
    <div class="page-header">
      <div class="breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-main" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-now">产业分类管理</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="page-content">
      <!-- 页面标题 -->
      <div class="page-title">产业分类管理</div>

      <!-- 搜索筛选区域 -->
      <div class="search-filter-area">
        <div class="filter-row">
          <div class="filter-item">
            <label class="filter-label">产业分类</label>
            <input
              v-model="searchForm.industryName"
              class="filter-input"
              placeholder="请输入产业分类"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">行业分类</label>
            <input
              v-model="searchForm.sectorName"
              class="filter-input"
              placeholder="请输入行业分类"
            />
          </div>
          <div class="filter-item">
            <label class="filter-label">产品分类</label>
            <input
              v-model="searchForm.productName"
              class="filter-input"
              placeholder="请输入产品分类"
            />
          </div>
          <div class="filter-buttons">
            <button class="filter-btn primary" @click="handleSearch">查询</button>
            <button class="filter-btn ghost" @click="handleReset">重置</button>
          </div>
        </div>
      </div>

      <!-- 操作按钮区域 -->
      <div class="action-area">
        <button class="action-btn primary" @click="handleFetch">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"></path>
            <polyline points="7 10 12 15 17 10"></polyline>
            <line x1="12" y1="15" x2="12" y2="3"></line>
          </svg>
          获取
        </button>
      </div>

      <p v-if="loadError" class="load-error">{{ loadError }}</p>
      <p v-else-if="loading" class="load-hint">加载中…</p>

      <!-- 表格区域 -->
      <div class="table-area">
        <table v-if="!loading" class="data-table">
          <thead>
            <tr>
              <th class="col-code">产业代码</th>
              <th class="col-name">产业分类</th>
              <th class="col-sector-code">行业代码</th>
              <th class="col-sector-name">行业分类</th>
              <th class="col-product-code">产品代码</th>
              <th class="col-product-name">产品分类</th>
              <th class="col-count">数据条数</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <template v-for="(item, index) in flattenedData" :key="item.id">
              <tr :class="{ 
                'level-1': item.level === 1, 
                'level-2': item.level === 2, 
                'level-3': item.level === 3 
              }">
                <td class="col-code">{{ item.industryCode || '' }}</td>
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
                    <span class="name-text" :title="item.industryName">{{ item.industryName }}</span>
                  </div>
                </td>
                <td class="col-sector-code">{{ item.sectorCode || '' }}</td>
                <td class="col-sector-name">{{ item.sectorName || '' }}</td>
                <td class="col-product-code">{{ item.productCode || '' }}</td>
                <td class="col-product-name">{{ item.productName || '' }}</td>
                <td class="col-count">{{ item.dataCount || 0 }}</td>
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
  </section>
</template>

<script setup>
import { ref, computed, onMounted } from 'vue'

const emit = defineEmits(['go-home', 'go-system-manage'])

const loading = ref(false)
const loadError = ref('')

// 搜索表单
const searchForm = ref({
  industryName: '',
  sectorName: '',
  productName: '',
})

// 查询生效条件
const activeFilters = ref({
  industryName: '',
  sectorName: '',
  productName: '',
})

// 示例数据结构
const INITIAL_DATA = [
  {
    id: '3',
    industryCode: '3',
    industryName: '新材料产业',
    sectorCode: '',
    sectorName: '',
    productCode: '',
    productName: '',
    dataCount: 0,
    level: 1,
    expanded: true,
    children: [
      {
        id: '3.1',
        industryCode: '3.1',
        industryName: '先进钢铁材料',
        sectorCode: '',
        sectorName: '',
        productCode: '',
        productName: '',
        dataCount: 0,
        level: 2,
        expanded: true,
        children: [
          {
            id: '3.1.1',
            industryCode: '3.1.1',
            industryName: '先进制造基础零部件用钢制造',
            sectorCode: '3130*',
            sectorName: '钢压延加工',
            productCode: '',
            productName: '',
            dataCount: 7,
            level: 3,
            expanded: true,
            children: [
              {
                id: '3.1.1.1',
                industryCode: '3.1.1',
                industryName: '高性能轴承用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130003',
                productName: '高碳铬轴承钢（GB/T18254）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.2',
                industryCode: '3.1.1',
                industryName: '高性能轴承用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130004',
                productName: '渗碳轴承钢（GB/T3203-2016）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.3',
                industryCode: '3.1.1',
                industryName: '高性能轴承用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130005',
                productName: '中碳轴承钢（G56Mn、G42CrMo）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.4',
                industryCode: '3.1.1',
                industryName: '高性能轴承用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130006',
                productName: '不锈轴承钢（高温不锈轴承钢G...）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.5',
                industryCode: '3.1.1',
                industryName: '高性能轴承用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130007',
                productName: '高温轴承钢（YB/T4105、YB/T...）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.6',
                industryCode: '3.1.1',
                industryName: '高性能齿轮用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130008',
                productName: '快速重载铁路机车用齿轮钢',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.7',
                industryCode: '3.1.1',
                industryName: '高性能齿轮用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130009',
                productName: '汽车变速箱齿轮钢（20MnCr...）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.8',
                industryCode: '3.1.1',
                industryName: '高性能齿轮用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130010',
                productName: '汽车后桥齿轮钢（22CrMoH）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.9',
                industryCode: '3.1.1',
                industryName: '高性能齿轮用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130011',
                productName: '风电齿轮钢（18CrNiMo7-6）',
                dataCount: 0,
                level: 3,
              },
              {
                id: '3.1.1.10',
                industryCode: '3.1.1',
                industryName: '高性能齿轮用钢加工',
                sectorCode: '3130*',
                sectorName: '钢压延加工',
                productCode: '3130012',
                productName: '机器人谐波减速器齿轮钢',
                dataCount: 0,
                level: 3,
              },
            ],
          },
        ],
      },
    ],
  },
]

// 原始树数据
const treeData = ref(JSON.parse(JSON.stringify(INITIAL_DATA)))

function nodeMatchesFilters(node, filters) {
  const industryKw = (filters.industryName || '').trim().toLowerCase()
  const sectorKw = (filters.sectorName || '').trim().toLowerCase()
  const productKw = (filters.productName || '').trim().toLowerCase()

  if (industryKw && !String(node.industryName || '').toLowerCase().includes(industryKw)) return false
  if (sectorKw && !String(node.sectorName || '').toLowerCase().includes(sectorKw)) return false
  if (productKw && !String(node.productName || '').toLowerCase().includes(productKw)) return false
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
    industryName: searchForm.value.industryName,
    sectorName: searchForm.value.sectorName,
    productName: searchForm.value.productName,
  }
}

const handleReset = () => {
  searchForm.value = {
    industryName: '',
    sectorName: '',
    productName: '',
  }
  activeFilters.value = {
    industryName: '',
    sectorName: '',
    productName: '',
  }
}

// 操作按钮
const handleFetch = () => {
  loading.value = true
  setTimeout(() => {
    treeData.value = JSON.parse(JSON.stringify(INITIAL_DATA))
    loading.value = false
  }, 500)
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

onMounted(() => {
  // 初始化加载数据
})
</script>

<style scoped>
.industry-class-page {
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

.col-code {
  width: 80px;
  text-align: center;
}

.col-name {
  min-width: 180px;
}

.col-sector-code {
  width: 80px;
  text-align: center;
}

.col-sector-name {
  min-width: 120px;
}

.col-product-code {
  width: 90px;
  text-align: center;
}

.col-product-name {
  min-width: 200px;
}

.col-count {
  width: 80px;
  text-align: center;
}

.col-action {
  width: 100px;
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

.action-btn-icon.delete:hover {
  background: #fff0f0;
  color: #e64a4a;
}

.action-btn-icon svg {
  display: block;
}
</style>
