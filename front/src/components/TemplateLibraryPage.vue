<template>
  <section class="template-library-page">
    <!-- 面包屑导航 -->
    <div class="library-header">
      <div class="library-breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-now">模板库</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="library-content">
      <!-- 页面标题 -->
      <div class="library-title">模板库</div>

      <!-- 标签切换 -->
      <div class="library-tabs">
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'template' }"
          @click="activeTab = 'template'"
        >
          模板
        </button>
        <button
          class="tab-btn"
          :class="{ active: activeTab === 'fragment' }"
          @click="activeTab = 'fragment'"
        >
          模板片段
        </button>
      </div>

      <!-- 搜索筛选区域 -->
      <div class="search-filter-area">
        <!-- 模板标签的搜索条件 -->
        <template v-if="activeTab === 'template'">
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">模板名称</label>
              <input
                v-model="searchForm.name"
                class="filter-input"
                placeholder="请输入模板名称"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">模板标签</label>
              <div class="filter-select" @click.stop="toggleTagDropdown">
                <span class="filter-select-text">
                  {{ searchForm.tag || '请选择模板标签' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="tagDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectTag('')">请选择模板标签</li>
                  <li
                    v-for="tag in tagOptions"
                    :key="tag"
                    class="filter-select-item"
                    @click.stop="selectTag(tag)"
                  >
                    {{ tag }}
                  </li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">是否已发布</label>
              <div class="filter-select" @click.stop="togglePublishDropdown">
                <span class="filter-select-text">
                  {{ searchForm.published || '请选择是否已发布' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="publishDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectPublished('')">请选择是否已发布</li>
                  <li class="filter-select-item" @click.stop="selectPublished('是')">是</li>
                  <li class="filter-select-item" @click.stop="selectPublished('否')">否</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">模板来源</label>
              <div class="filter-select" @click.stop="toggleSourceDropdown">
                <span class="filter-select-text">
                  {{ searchForm.source || '请选择' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="sourceDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectSource('')">请选择</li>
                  <li class="filter-select-item" @click.stop="selectSource('节点管理部')">节点管理部</li>
                  <li class="filter-select-item" @click.stop="selectSource('用户上传')">用户上传</li>
                </ul>
              </div>
            </div>
          </div>
          <div class="filter-row second-row">
            <div class="filter-item">
              <label class="filter-label">创建人</label>
              <input
                v-model="searchForm.creator"
                class="filter-input"
                placeholder="请输入创建人"
              />
            </div>
            <div class="filter-buttons">
              <button class="filter-btn primary" @click="handleSearch">查询</button>
              <button class="filter-btn ghost" @click="handleReset">重置</button>
            </div>
          </div>
        </template>

        <!-- 模板片段标签的搜索条件 -->
        <template v-else>
          <div class="filter-row">
            <div class="filter-item">
              <label class="filter-label">模板片段名称</label>
              <input
                v-model="searchForm.name"
                class="filter-input"
                placeholder="请输入模板片段名称"
              />
            </div>
            <div class="filter-item">
              <label class="filter-label">是否已发布</label>
              <div class="filter-select" @click.stop="togglePublishDropdown">
                <span class="filter-select-text">
                  {{ searchForm.published || '请选择是否已发布' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="publishDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectPublished('')">请选择是否已发布</li>
                  <li class="filter-select-item" @click.stop="selectPublished('是')">是</li>
                  <li class="filter-select-item" @click.stop="selectPublished('否')">否</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">模板来源</label>
              <div class="filter-select" @click.stop="toggleSourceDropdown">
                <span class="filter-select-text">
                  {{ searchForm.source || '请选择' }}
                </span>
                <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
                <ul v-if="sourceDropdownVisible" class="filter-select-dropdown">
                  <li class="filter-select-item" @click.stop="selectSource('')">请选择</li>
                  <li class="filter-select-item" @click.stop="selectSource('节点管理部')">节点管理部</li>
                  <li class="filter-select-item" @click.stop="selectSource('用户上传')">用户上传</li>
                </ul>
              </div>
            </div>
            <div class="filter-item">
              <label class="filter-label">创建人</label>
              <input
                v-model="searchForm.creator"
                class="filter-input"
                placeholder="请输入创建人"
              />
            </div>
            <div class="filter-buttons fragment-buttons">
              <button class="filter-btn primary" @click="handleSearch">查询</button>
              <button class="filter-btn ghost" @click="handleReset">重置</button>
            </div>
          </div>
        </template>
      </div>

      <!-- 表格区域 -->
      <div class="table-area">
        <table class="data-table">
          <thead>
            <tr>
              <th class="col-index">序号</th>
              <th class="col-name">模板名称</th>
              <th class="col-tag">模板标签</th>
              <th class="col-published">是否已发布至新材料大数据中心</th>
              <th class="col-desc">模板说明</th>
              <th class="col-source">模板来源</th>
              <th class="col-creator">创建人</th>
              <th class="col-time">创建时间</th>
              <th class="col-action">操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(item, index) in tableData" :key="item.id">
              <td class="col-index">{{ (page - 1) * pageSize + index + 1 }}</td>
              <td class="col-name" :title="item.name">{{ item.name }}</td>
              <td class="col-tag">
                <span class="tag-badge">{{ item.tag }}</span>
              </td>
              <td class="col-published">{{ item.published }}</td>
              <td class="col-desc" :title="item.description">{{ item.description }}</td>
              <td class="col-source">{{ item.source }}</td>
              <td class="col-creator">{{ item.creator }}</td>
              <td class="col-time">{{ item.createTime }}</td>
              <td class="col-action">
                <div class="action-btns">
                  <button class="action-btn view" title="查看" @click="handleView(item)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/>
                      <circle cx="12" cy="12" r="3"/>
                    </svg>
                  </button>
                  <button class="action-btn edit" title="编辑" @click="handleEdit(item)">
                    <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                      <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
                      <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
                    </svg>
                  </button>
                  <button
                    class="action-btn favorite"
                    :class="{ active: item.isFavorited }"
                    :title="item.isFavorited ? '取消收藏' : '收藏'"
                    @click="handleFavorite(item)"
                  >
                    <svg width="16" height="16" viewBox="0 0 24 24" :fill="item.isFavorited ? 'currentColor' : 'none'" stroke="currentColor" stroke-width="2">
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
    </div>
  </section>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'

const emit = defineEmits(['go-home'])

// 标签切换
const activeTab = ref('template')

// 下拉框状态
const tagDropdownVisible = ref(false)
const publishDropdownVisible = ref(false)
const sourceDropdownVisible = ref(false)

// 搜索表单
const searchForm = ref({
  name: '',
  tag: '',
  published: '',
  source: '',
  creator: ''
})

// 标签选项
const tagOptions = [
  '生物医用材料（科学）',
  '生物医用材料（产业）',
  '先进基础材料',
  '先进钢铁材料'
]

// 分页相关
const page = ref(1)
const pageSize = ref(10)
const total = ref(19)

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
  { id: 1, name: '生物材料临床试验数据采集模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 2, name: '生物材料体内实验数据模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 3, name: '生物材料体外实验数据模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 4, name: '生物材料力学性能测试模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 5, name: '测试-生物医用材料基础数据模板', tag: '生物医用材料（科学）', published: '是', description: '版本升级测试', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: true },
  { id: 6, name: '生物材料通用模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 7, name: '自组装羟基磷灰石纳米材料表征模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 8, name: '纳米羟基磷灰石生物相容性评价模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-15', isFavorited: false },
  { id: 9, name: '自组装羟基磷灰石力学性能测试模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-14', isFavorited: false },
  { id: 10, name: '纳米羟基磷灰石药物缓释性能模板', tag: '生物医用材料（科学）', published: '否', description: '', source: '节点管理部', creator: '朱向东', createTime: '2024-01-14', isFavorited: false }
])

// 下拉框控制
const toggleTagDropdown = () => {
  tagDropdownVisible.value = !tagDropdownVisible.value
  publishDropdownVisible.value = false
  sourceDropdownVisible.value = false
}

const togglePublishDropdown = () => {
  publishDropdownVisible.value = !publishDropdownVisible.value
  tagDropdownVisible.value = false
  sourceDropdownVisible.value = false
}

const toggleSourceDropdown = () => {
  sourceDropdownVisible.value = !sourceDropdownVisible.value
  tagDropdownVisible.value = false
  publishDropdownVisible.value = false
}

const selectTag = (tag) => {
  searchForm.value.tag = tag
  tagDropdownVisible.value = false
}

const selectPublished = (value) => {
  searchForm.value.published = value
  publishDropdownVisible.value = false
}

const selectSource = (value) => {
  searchForm.value.source = value
  sourceDropdownVisible.value = false
}

// 搜索和重置
const handleSearch = () => {
  page.value = 1
  // 这里可以调用API进行搜索
  console.log('搜索条件：', searchForm.value)
}

const handleReset = () => {
  searchForm.value = {
    name: '',
    tag: '',
    published: '',
    source: '',
    creator: ''
  }
  page.value = 1
}

// 分页控制
const goToPage = (p) => {
  if (p < 1 || p > totalPages.value) return
  page.value = p
}

// 操作按钮
const handleView = (item) => {
  console.log('查看模板：', item)
}

const handleEdit = (item) => {
  console.log('编辑模板：', item)
}

const handleFavorite = (item) => {
  item.isFavorited = !item.isFavorited
}

const goHome = () => {
  emit('go-home')
}

// 点击外部关闭下拉框
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
    tagDropdownVisible.value = false
    publishDropdownVisible.value = false
    sourceDropdownVisible.value = false
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
.template-library-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #f5f7fb;
  padding: 16px 20px 20px;
  border-radius: 8px;
}

.library-header {
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

.crumb-now {
  color: #333;
}

.library-content {
  background: #fff;
  border-radius: 6px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.library-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.library-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 20px;
  border-bottom: 1px solid #e3e7f0;
  padding-bottom: 12px;
}

.tab-btn {
  padding: 8px 24px;
  border-radius: 4px;
  border: 1px solid transparent;
  background: transparent;
  font-size: 14px;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.tab-btn:hover {
  color: #1a5ce6;
}

.tab-btn.active {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
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

.filter-buttons.fragment-buttons {
  margin-left: 0;
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

.col-index {
  width: 50px;
  text-align: center;
}

.col-name {
  min-width: 180px;
  max-width: 220px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-tag {
  min-width: 140px;
}

.col-published {
  min-width: 180px;
  text-align: center;
}

.col-desc {
  min-width: 120px;
  max-width: 150px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.col-source {
  min-width: 100px;
}

.col-creator {
  min-width: 80px;
}

.col-time {
  min-width: 100px;
}

.col-action {
  min-width: 100px;
  text-align: center;
}

.tag-badge {
  display: inline-block;
  padding: 3px 10px;
  background: #e8f0fe;
  color: #1a5ce6;
  border-radius: 4px;
  font-size: 12px;
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
  width: 28px;
  height: 28px;
  border-radius: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #666;
  transition: all 0.2s;
}

.action-btn:hover {
  background: #f0f5ff;
  color: #1a5ce6;
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
  padding: 6px 14px;
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
