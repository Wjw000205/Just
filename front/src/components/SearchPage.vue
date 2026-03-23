



<template>
  <section class="search-page">
    <div class="search-header">
      <div class="search-breadcrumb">
        当前位置：<span class="crumb-main">首页</span> &gt;
        <span class="crumb-now">数据检索</span>
      </div>
    </div>

    <div class="search-layout">
      <!-- 左侧分类树 -->
      <aside class="search-sidebar">
        <div class="sidebar-title">工业战略性新兴产业分类目录</div>
        <div class="sidebar-search">
          <input class="sidebar-input" placeholder="请输入关键字搜索" />
        </div>
        <ul class="sidebar-list">
          <li class="sidebar-item">
            <label><input type="checkbox" /> 先进基础材料</label>
          </li>
          <li class="sidebar-item">
            <label><input type="checkbox" /> 生物医用材料（科学）</label>
          </li>
          <li class="sidebar-item">
            <label><input type="checkbox" /> 生物医用材料（产业）</label>
          </li>
          <li class="sidebar-item disabled">
            <label><input type="checkbox" disabled /> ……（更多分类预留）</label>
          </li>
        </ul>
      </aside>

      <!-- 右侧检索区域 -->
      <section class="search-main">
        <div class="search-main-top">
          <div class="search-main-row">
            <span class="field-label">生物医用材料（科学）</span>
            <span class="field-label">生物医用材料（产业）</span>
          </div>
          <div class="search-main-row">
            <label class="field-label">检索方式：</label>
            <div class="search-mode-select" @click.stop="toggleModeDropdown">
              <span class="search-mode-text">
                {{ currentModeLabel }}
              </span>
              <svg
                class="search-mode-caret"
                width="10"
                height="6"
                viewBox="0 0 10 6"
              >
                <path
                  d="M1 1l4 4 4-4"
                  stroke="currentColor"
                  stroke-width="1.5"
                  fill="none"
                  stroke-linecap="round"
                />
              </svg>
              <ul v-if="modeDropdownVisible" class="search-mode-dropdown">
                <li
                  v-for="mode in searchModes"
                  :key="mode.value"
                  class="search-mode-item"
                  :class="{ active: mode.value === currentMode }"
                  @click.stop="selectMode(mode.value)"
                >
                  <span class="search-mode-item-text">
                    {{ mode.label }}
                  </span>
                  <span
                    v-if="mode.value === currentMode"
                    class="search-mode-check"
                  >
                    ✓
                  </span>
                </li>
              </ul>
            </div>

            <input
              v-model="keyword"
              class="search-input"
              placeholder="请输入检索内容"
            />
            <button class="search-btn primary" @click="doSearch(true)">搜索</button>
            <button class="search-btn ghost">在结果中搜索</button>
            <button
              class="search-btn ghost"
              @click="toggleAdvanced"
            >
              高级检索
            </button>
          </div>
        </div>

        <!-- 高级检索面板 -->
        <div v-if="advancedVisible" class="advanced-panel">
          <div class="advanced-header">
            <span class="advanced-title">高级搜索</span>
            <span class="advanced-tip">可组合多个条件精确检索</span>
          </div>

          <div class="advanced-row">
            <label class="advanced-label">检索字段：</label>
            <div class="advanced-select" @click.stop="toggleAdvancedFieldDropdown">
              <span class="advanced-select-text">
                {{ currentAdvancedFieldLabel }}
              </span>
              <svg
                class="advanced-select-caret"
                width="10"
                height="6"
                viewBox="0 0 10 6"
              >
                <path
                  d="M1 1l4 4 4-4"
                  stroke="currentColor"
                  stroke-width="1.5"
                  fill="none"
                  stroke-linecap="round"
                />
              </svg>
              <ul
                v-if="advancedFieldDropdownVisible"
                class="advanced-select-dropdown"
              >
                <li
                  v-for="mode in advancedFieldOptions"
                  :key="mode.value"
                  class="advanced-select-item"
                  :class="{ active: mode.value === advancedField }"
                  @click.stop="selectAdvancedField(mode.value)"
                >
                  <span class="advanced-select-item-text">
                    {{ mode.label }}
                  </span>
                  <span
                    v-if="mode.value === advancedField"
                    class="advanced-select-check"
                  >
                    ✓
                  </span>
                </li>
              </ul>
            </div>

            <input
              v-model="advancedKeyword"
              class="advanced-input"
              placeholder="请输入您要搜索的内容"
            />
            <button
              class="advanced-add-btn"
              @click="addAdvancedCondition"
            >
              +
            </button>
          </div>

          <div class="advanced-row">
            <label class="advanced-label">时间范围：</label>
            <input
              v-model="dateStart"
              type="date"
              class="advanced-date-input"
              placeholder="开始日期"
            />
            <span class="advanced-date-sep">至</span>
            <input
              v-model="dateEnd"
              type="date"
              class="advanced-date-input"
              placeholder="结束日期"
            />
          </div>

          <div
            v-if="!tagsCollapsed"
            class="advanced-row advanced-tags-row"
          >
            <div class="advanced-tags">
              <template v-if="advancedConditions.length">
                <div
                  v-for="(item, index) in advancedConditions"
                  :key="index"
                  class="advanced-tag"
                >
                  <span class="advanced-tag-field">
                    {{ getModeLabel(item.field) }}：
                  </span>
                  <span class="advanced-tag-text">
                    {{ item.value }}
                  </span>
                  <button
                    class="advanced-tag-close"
                    @click="removeAdvancedCondition(index)"
                  >
                    ×
                  </button>
                </div>
              </template>
              <span
                v-else
                class="advanced-tags-empty"
              >
                暂未添加检索条件
              </span>
            </div>
          </div>

          <div class="advanced-actions">
            <button
              class="advanced-btn ghost"
              @click="toggleAdvanced"
            >
              返回
            </button>
            <button
              class="advanced-btn ghost"
              @click="resetAdvanced"
            >
              重置
            </button>
            <button
              class="advanced-btn ghost"
              @click="toggleTags"
            >
              {{ tagsCollapsed ? '展开标签' : '收起标签' }}
            </button>
            <button class="advanced-btn primary" @click="doSearch(true)">
              检索
            </button>
          </div>
        </div>

        <!-- 结果类型标签 -->
        <div class="result-tabs">
          <button
            v-for="tab in resultTypes"
            :key="tab.value"
            class="tab-btn"
            :class="{ active: tab.value === currentResultType }"
            @click="
              currentResultType = tab.value;
              doSearch(true);
            "
          >
            {{ tab.label }}
          </button>
        </div>

        <!-- 加载中 -->
        <div v-if="loading" class="search-loading">
          <span class="loading-text">加载中…</span>
        </div>

        <!-- 结果列表 -->
        <template v-else-if="hasSearched && resultItems.length > 0">
          <ul class="result-list">
            <li
              v-for="item in resultItems"
              :key="item.id"
              class="result-item"
            >
              <h4 class="result-item-title">{{ item.title }}</h4>
              <p v-if="item.abstract" class="result-item-abstract">
                {{ item.abstract }}
              </p>
              <div v-if="item.tags?.length" class="result-item-tags">
                <span
                  v-for="(tag, i) in item.tags"
                  :key="i"
                  class="result-tag"
                >
                  {{ tag }}
                </span>
              </div>
              <div class="result-item-meta">
                <span v-if="item.publishTime">{{ item.publishTime }}</span>
                <span v-if="item.creator">{{ item.creator }}</span>
                <span v-if="item.organization">{{ item.organization }}</span>
              </div>
            </li>
          </ul>

          <!-- 分页 -->
          <div class="pagination">
            <span class="pagination-total">共 {{ total }} 条</span>
            <span class="pagination-size">
              每页
              <select
                :value="pageSize"
                class="pagination-select"
                @change="(e) => changePageSize(Number(e.target.value))"
              >
                <option
                  v-for="opt in pageSizeOptions"
                  :key="opt"
                  :value="opt"
                >
                  {{ opt }}
                </option>
              </select>
              条
            </span>
            <div class="pagination-nav">
              <button
                type="button"
                class="pagination-btn"
                :disabled="page <= 1"
                @click="goToPage(page - 1)"
              >
                上一页
              </button>
              <span class="pagination-pages">
                第 {{ page }} / {{ totalPages }} 页
              </span>
              <button
                type="button"
                class="pagination-btn"
                :disabled="page >= totalPages"
                @click="goToPage(page + 1)"
              >
                下一页
              </button>
            </div>
          </div>
        </template>

        <!-- 空状态 -->
        <div
          v-else-if="hasSearched && resultItems.length === 0"
          class="search-empty"
        >
          <div class="empty-icon">
            <div class="empty-paper"></div>
            <div class="empty-circle"></div>
          </div>
          <div class="empty-text">
            当前选择的检索条件下，暂时没有搜索结果
          </div>
          <div class="empty-sub">请调整检索条件，或稍后再试</div>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { onMounted, onBeforeUnmount, ref, computed } from 'vue'

const getAuthHeader = () => {
  // 后端 JwtInterceptor 从请求头 Authorization 读取 token
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

const keyword = ref('')

const searchModes = [
  { label: '全文检索', value: 'fulltext' },
  { label: '题名', value: 'title' },
  { label: '关键词', value: 'keyword_field' },
  { label: '摘要', value: 'abstract' },
  { label: '篇关键词', value: 'paragraph_keyword' },
]

// 高级检索下拉框字段（题名、关键词、摘要、发布者、机构）
const advancedFieldOptions = [
  { label: '题名', value: 'title' },
  { label: '关键词', value: 'keyword_field' },
  { label: '摘要', value: 'abstract' },
  { label: '发布者', value: 'publisher' },
  { label: '机构', value: 'organization' },
]

// 结果类型 Tab
const resultTypes = [
  { label: '数据集', value: 'dataset' },
  { label: '科技论文', value: 'paper' },
  { label: '专利文献', value: 'patent' },
  { label: '标准规范', value: 'standard' },
  { label: '学术专著', value: 'monograph' },
  { label: '学位论文', value: 'thesis' },
]
const currentResultType = ref('dataset')

// 分页与结果
const page = ref(1)
const pageSize = ref(10)
const pageSizeOptions = [10, 20, 50]
const total = ref(0)
const resultItems = ref([])
const loading = ref(false)
const hasSearched = ref(false)

const totalPages = computed(() =>
  Math.max(1, Math.ceil(total.value / pageSize.value))
)

const currentMode = ref('fulltext')
const modeDropdownVisible = ref(false)

const currentModeLabel = computed(() => {
  const found = searchModes.find((m) => m.value === currentMode.value)
  return found ? found.label : ''
})

const toggleModeDropdown = () => {
  modeDropdownVisible.value = !modeDropdownVisible.value
}

const selectMode = (value) => {
  currentMode.value = value
  modeDropdownVisible.value = false
}

// 高级检索相关
const advancedVisible = ref(false)
const advancedField = ref('title')
const advancedKeyword = ref('')
const advancedConditions = ref([])
const dateStart = ref('')
const dateEnd = ref('')
const advancedFieldDropdownVisible = ref(false)
const tagsCollapsed = ref(false)

const currentAdvancedFieldLabel = computed(() => {
  const found = advancedFieldOptions.find((m) => m.value === advancedField.value)
  return found ? found.label : ''
})

const toggleAdvanced = () => {
  advancedVisible.value = !advancedVisible.value
}

const toggleAdvancedFieldDropdown = () => {
  advancedFieldDropdownVisible.value = !advancedFieldDropdownVisible.value
}

const selectAdvancedField = (value) => {
  advancedField.value = value
  advancedFieldDropdownVisible.value = false
}

const addAdvancedCondition = () => {
  if (!advancedKeyword.value.trim()) return
  advancedConditions.value.push({
    field: advancedField.value,
    value: advancedKeyword.value.trim(),
  })
  advancedKeyword.value = ''
}

const removeAdvancedCondition = (index) => {
  advancedConditions.value.splice(index, 1)
}

const resetAdvanced = () => {
  advancedField.value = 'title'
  advancedKeyword.value = ''
  advancedConditions.value = []
  dateStart.value = ''
  dateEnd.value = ''
  tagsCollapsed.value = false
}

const toggleTags = () => {
  tagsCollapsed.value = !tagsCollapsed.value
}

const getModeLabel = (value) => {
  const found =
    advancedFieldOptions.find((m) => m.value === value) ||
    searchModes.find((m) => m.value === value)
  return found ? found.label : value
}

// 调用 3.1 接口：POST /api/search/datasets
const doSearch = async (resetPage = false) => {
  if (resetPage) page.value = 1
  loading.value = true
  hasSearched.value = true
  try {
    const body = {
      keyword: keyword.value?.trim() || undefined,
      searchField: currentMode.value,
      categoryIds: [], // 后续与左侧分类勾选联动
      searchScope: 'all',
      resultType: currentResultType.value,
      page: page.value,
      pageSize: pageSize.value,
      sortField: 'relevance',
      sortOrder: 'desc',
    }
    if (
      advancedConditions.value.length > 0 ||
      dateStart.value ||
      dateEnd.value
    ) {
      body.advanced = {
        conditions:
          advancedConditions.value.length > 0
            ? advancedConditions.value.map(({ field, value }) => ({
                field,
                value,
              }))
            : undefined,
        publishDateStart: dateStart.value || undefined,
        publishDateEnd: dateEnd.value || undefined,
      }
    }
    const res = await fetch('/api/search/datasets', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify(body),
    })

    const json = await res.json().catch(() => null)
    if (res.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      total.value = 0
      resultItems.value = []
      return
    }

    if (json?.code === 200 && json?.data) {
      total.value = json.data.total ?? 0
      resultItems.value = json.data.items ?? []
    } else {
      total.value = 0
      resultItems.value = []
    }
  } catch (_) {
    total.value = 0
    resultItems.value = []
  } finally {
    loading.value = false
  }
}

const goToPage = (p) => {
  const next = Math.max(1, Math.min(p, totalPages.value))
  if (next === page.value) return
  page.value = next
  doSearch()
}

const changePageSize = (size) => {
  if (size === pageSize.value) return
  pageSize.value = size
  page.value = 1
  doSearch()
}

const handleClickOutside = (e) => {
  const target = e.target
  if (!target.closest('.search-mode-select')) {
    modeDropdownVisible.value = false
  }
  if (!target.closest('.advanced-select')) {
    advancedFieldDropdownVisible.value = false
  }
}

onMounted(() => {
  window.addEventListener('click', handleClickOutside)
})

onBeforeUnmount(() => {
  window.removeEventListener('click', handleClickOutside)
})
</script>

<style scoped>
.search-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
  background-color: #f5f7fb;
  padding: 16px 20px 20px;
  border-radius: 8px;
}

.search-header {
  font-size: 13px;
  color: #666;
}

.crumb-main {
  color: #1a5ce6;
}

.crumb-now {
  color: #333;
}

.search-layout {
  display: grid;
  grid-template-columns: 260px 1fr;
  gap: 16px;
  align-items: stretch;
}

.search-sidebar {
  background: #fff;
  border-radius: 6px;
  padding: 14px 12px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  height: 100%;
}

.sidebar-title {
  font-size: 13px;
  font-weight: 600;
  color: #333;
  margin-bottom: 10px;
}

.sidebar-search {
  margin-bottom: 10px;
}

.sidebar-input {
  width: 100%;
  box-sizing: border-box;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 8px;
  font-size: 12px;
}

.sidebar-input::placeholder {
  color: #b0b6c6;
}

.sidebar-list {
  list-style: none;
  padding: 0;
  margin: 0;
  overflow-y: auto;
  flex: 1;
}

.sidebar-item {
  font-size: 12px;
  color: #333;
  padding: 4px 0;
}

.sidebar-item label {
  display: flex;
  align-items: center;
  gap: 4px;
}

.sidebar-item.disabled {
  color: #b0b6c6;
}

.search-main {
  background: #fff;
  border-radius: 6px;
  padding: 16px 18px 22px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  min-height: 360px;
}

.search-main-top {
  border-bottom: 1px solid #e3e7f0;
  padding-bottom: 12px;
  margin-bottom: 12px;
}

.search-main-row {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 8px;
  margin-bottom: 8px;
}

.field-label {
  font-size: 12px;
  color: #333;
  margin-right: 8px;
}

.search-mode-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 4px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 12px;
  color: #333;
  cursor: pointer;
  margin-right: 8px;
  min-width: 96px;
}

.search-mode-text {
  flex: 1;
}

.search-mode-caret {
  margin-left: 4px;
  color: #999;
}

.search-mode-dropdown {
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

.search-mode-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 12px;
  color: #333;
}

.search-mode-item + .search-mode-item {
  border-top: 1px solid #f2f3f7;
}

.search-mode-item.active {
  color: #1a5ce6;
  background: #f5f8ff;
}

.search-mode-item-text {
  flex: 1;
}

.search-mode-check {
  font-size: 12px;
  color: #1a5ce6;
  margin-left: 8px;
}

.search-input {
  flex: 1;
  min-width: 220px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 8px;
  font-size: 12px;
}

.search-input::placeholder {
  color: #b0b6c6;
}

.search-btn {
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 5px 10px;
  font-size: 12px;
  margin-left: 4px;
  cursor: pointer;
  background: #fff;
  color: #333;
}

.search-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.search-btn.ghost {
  background: #f6f7fb;
}

.result-tabs {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 18px;
}

.tab-btn {
  border-radius: 18px;
  border: 1px solid transparent;
  background: #f5f7fb;
  padding: 5px 14px;
  font-size: 12px;
  cursor: pointer;
  color: #555;
}

.tab-btn.active {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.search-loading {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 40px 0;
  color: #8b92a6;
  font-size: 13px;
}

.loading-text {
  display: inline-block;
}

.result-list {
  list-style: none;
  margin: 0 0 16px 0;
  padding: 0;
}

.result-item {
  background: #fff;
  border-radius: 8px;
  border: 1px solid #e8ecf4;
  padding: 14px 16px;
  margin-bottom: 10px;
}

.result-item-title {
  margin: 0 0 8px 0;
  font-size: 14px;
  font-weight: 600;
  color: #1a1a2e;
}

.result-item-abstract {
  margin: 0 0 8px 0;
  font-size: 12px;
  color: #555;
  line-height: 1.5;
}

.result-item-tags {
  margin-bottom: 6px;
}

.result-tag {
  display: inline-block;
  margin-right: 6px;
  padding: 2px 8px;
  font-size: 11px;
  background: #e8f0fe;
  color: #1a5ce6;
  border-radius: 4px;
}

.result-item-meta {
  font-size: 11px;
  color: #8b92a6;
}

.result-item-meta span + span::before {
  content: ' · ';
}

.pagination {
  display: flex;
  align-items: center;
  flex-wrap: wrap;
  gap: 16px;
  padding: 12px 0;
  font-size: 12px;
  color: #555;
}

.pagination-total {
  margin-right: 8px;
}

.pagination-size {
  display: inline-flex;
  align-items: center;
  gap: 4px;
}

.pagination-select {
  padding: 4px 8px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 12px;
  background: #fff;
  cursor: pointer;
}

.pagination-nav {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-left: auto;
}

.pagination-btn {
  padding: 4px 12px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 12px;
  background: #fff;
  cursor: pointer;
  color: #333;
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
  color: #8b92a6;
}

.search-empty {
  flex: 1;
  display: flex;
  flex-direction: column;
  align-items: center;
  justify-content: center;
  text-align: center;
  color: #8b92a6;
  font-size: 13px;
  padding: 24px 0;
}

.empty-icon {
  width: 120px;
  height: 80px;
  position: relative;
  margin-bottom: 8px;
}

.empty-paper {
  position: absolute;
  inset: 8px 26px 18px;
  border-radius: 8px;
  background: #f5f7fb;
  box-shadow: 0 6px 12px rgba(44, 79, 148, 0.12);
}

.empty-circle {
  position: absolute;
  inset: auto 36px 4px;
  height: 22px;
  border-radius: 999px;
  background: radial-gradient(circle at 50% 0%, #d0d8f0, transparent 70%);
  opacity: 0.7;
}

.empty-text {
  margin-bottom: 4px;
}

.empty-sub {
  font-size: 12px;
  color: #a0a6b8;
}

/* 高级检索样式 */
.advanced-panel {
  margin-top: 16px;
  background: #f7f9ff;
  border-radius: 10px;
  border: 1px solid #d9e4ff;
  padding: 18px 22px 20px;
}

.advanced-header {
  display: flex;
  align-items: center;
  justify-content: center;
  margin-bottom: 18px;
  font-size: 14px;
  font-weight: 600;
  color: #1a5ce6;
}

.advanced-title {
  margin-right: 6px;
}

.advanced-tip {
  font-size: 12px;
  color: #8b92a6;
}

.advanced-row {
  display: flex;
  align-items: center;
  margin-bottom: 12px;
  font-size: 12px;
}

.advanced-label {
  width: 72px;
  flex-shrink: 0;
  text-align: right;
  margin-right: 8px;
  color: #333;
}

.advanced-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px 0 0 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 12px;
  color: #333;
  cursor: pointer;
  min-width: 96px;
}

.advanced-select-text {
  flex: 1;
}

.advanced-select-caret {
  margin-left: 4px;
  color: #999;
}

.advanced-select-dropdown {
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
  z-index: 20;
}

.advanced-select-item {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 6px 12px;
  font-size: 12px;
  color: #333;
}

.advanced-select-item + .advanced-select-item {
  border-top: 1px solid #f2f3f7;
}

.advanced-select-item.active {
  color: #1a5ce6;
  background: #f5f8ff;
}

.advanced-select-item-text {
  flex: 1;
}

.advanced-select-check {
  font-size: 12px;
  color: #1a5ce6;
  margin-left: 8px;
}

.advanced-input {
  flex: 1;
  border-radius: 0 4px 4px 0;
  border: 1px solid #d4dae6;
  border-left: none;
  padding: 6px 8px;
  font-size: 12px;
  min-width: 220px;
}

.advanced-input::placeholder {
  color: #b0b6c6;
}

.advanced-add-btn {
  margin-left: 10px;
  width: 32px;
  height: 32px;
  border-radius: 4px;
  border: 1px solid #1a5ce6;
  background: #fff;
  color: #1a5ce6;
  font-size: 20px;
  line-height: 1;
  cursor: pointer;
}

.advanced-date-input {
  width: 150px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 8px;
  font-size: 12px;
}

.advanced-date-sep {
  margin: 0 8px;
  color: #666;
}

.advanced-tags-row {
  align-items: flex-start;
}

.advanced-tags {
  flex: 1;
  min-height: 110px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fdfdff;
  padding: 10px 12px;
  display: flex;
  flex-wrap: wrap;
  gap: 6px;
}

.advanced-tags-empty {
  font-size: 12px;
  color: #a0a6b8;
}

.advanced-tag {
  display: inline-flex;
  align-items: center;
  border-radius: 4px;
  background: #eef3ff;
  padding: 4px 8px;
  font-size: 12px;
  color: #333;
}

.advanced-tag-field {
  color: #666;
  margin-right: 4px;
}

.advanced-tag-text {
  max-width: 220px;
  white-space: nowrap;
  text-overflow: ellipsis;
  overflow: hidden;
}

.advanced-tag-close {
  margin-left: 4px;
  border: none;
  background: transparent;
  cursor: pointer;
  color: #999;
}

.advanced-actions {
  margin-top: 14px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.advanced-btn {
  min-width: 92px;
  padding: 6px 20px;
  border-radius: 4px;
  font-size: 13px;
  border: 1px solid #1a5ce6;
  cursor: pointer;
}

.advanced-btn.ghost {
  background: #fff;
  color: #1a5ce6;
}

.advanced-btn.primary {
  background: #1a5ce6;
  color: #fff;
}
</style>

