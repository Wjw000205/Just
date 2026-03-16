<template>
  <section class="create-dataset-page">
    <h1 class="create-dataset-title">创建数据集</h1>

    <form class="create-dataset-form" @submit.prevent="onSubmit">
      <!-- 数据集名称 -->
      <div class="form-row">
        <label class="form-label required">数据集名称</label>
        <div class="form-field-wrap">
          <input
            v-model="form.name"
            type="text"
            class="form-input"
            placeholder="请输入数据集名称"
            maxlength="200"
          />
          <span class="form-counter">{{ form.name.length }}/200</span>
        </div>
      </div>

      <!-- 数据集摘要 -->
      <div class="form-row">
        <label class="form-label required">数据集摘要</label>
        <div class="form-field-wrap form-field-textarea">
          <textarea
            v-model="form.summary"
            class="form-textarea"
            placeholder="请输入数据集摘要"
            maxlength="500"
            rows="4"
          />
          <span class="form-counter">{{ form.summary.length }}/500</span>
        </div>
      </div>

      <!-- 封面 -->
      <div class="form-row">
        <label class="form-label">封面</label>
        <div class="form-upload" @click="triggerCoverUpload">
          <input
            ref="coverInputRef"
            type="file"
            accept="image/*"
            class="form-upload-input"
            @change="onCoverChange"
          />
          <div v-if="!coverPreview" class="form-upload-placeholder">
            <span class="form-upload-plus">+</span>
            <span class="form-upload-tip">请上传226px*170px</span>
            <span class="form-upload-tip">比例的封面图</span>
          </div>
          <img v-else :src="coverPreview" alt="封面" class="form-upload-preview" />
        </div>
      </div>

      <!-- 科学分类 -->
      <div class="form-row">
        <label class="form-label required">科学分类</label>
        <div class="form-field-with-btn">
          <input
            v-model="form.scienceCategory"
            type="text"
            class="form-input"
            placeholder="请点击选择"
            readonly
          />
          <button type="button" class="form-select-btn" @click="openScienceCategory">
            选择
          </button>
        </div>
      </div>

      <!-- 产品分类 -->
      <div class="form-row">
        <label class="form-label required">产品分类</label>
        <div class="form-field-with-btn">
          <input
            v-model="form.productCategory"
            type="text"
            class="form-input"
            placeholder="请点击选择"
            readonly
          />
          <button type="button" class="form-select-btn" @click="openProductCategory">
            选择
          </button>
        </div>
      </div>

      <!-- 数据级别 -->
      <div class="form-row">
        <label class="form-label required">数据级别</label>
        <div class="form-field-wrap" ref="dataLevelDropRef">
          <div
            class="form-select form-select-custom"
            :class="{ open: dataLevelDropOpen }"
            @click="dataLevelDropOpen = !dataLevelDropOpen"
          >
            <span v-if="form.dataLevel" :class="['data-level-tag', dataLevelOption?.colorClass]">
              {{ dataLevelOption?.label }}
            </span>
            <span v-else class="form-select-placeholder">请选择数据级别</span>
            <svg class="form-select-caret" width="10" height="6" viewBox="0 0 10 6">
              <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
            </svg>
          </div>
          <ul v-if="dataLevelDropOpen" class="form-select-dropdown" @click.stop>
            <li
              v-for="opt in dataLevelOptions"
              :key="opt.value"
              class="form-select-option"
              @click="form.dataLevel = opt.value; dataLevelDropOpen = false"
            >
              <span :class="['data-level-tag', opt.colorClass]">{{ opt.label }}</span>
            </li>
          </ul>
        </div>
      </div>

      <!-- 数据类别 -->
      <div class="form-row">
        <label class="form-label required">数据类别</label>
        <div class="form-field-with-info">
          <input
            v-model="form.dataCategory"
            type="text"
            class="form-input"
            readonly
          />
          <span class="form-info-icon" title="数据集">ⓘ</span>
        </div>
      </div>

      <!-- 模板标签 -->
      <div class="form-row">
        <label class="form-label required">模板标签</label>
        <div class="form-field-wrap">
          <select v-model="form.templateTag" class="form-select">
            <option value="" disabled>请选择模板标签</option>
            <option
              v-for="opt in templateTagOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </option>
          </select>
        </div>
      </div>

      <!-- 选择模板 -->
      <div class="form-row">
        <label class="form-label required">选择模板</label>
        <div class="form-field-wrap">
          <select v-model="form.template" class="form-select">
            <option value="" disabled>请选择模板</option>
            <option
              v-for="opt in templateOptions"
              :key="opt.value"
              :value="opt.value"
            >
              {{ opt.label }}
            </option>
          </select>
        </div>
      </div>

      <!-- 数据集标签 -->
      <div class="form-row">
        <label class="form-label">数据集标签</label>
        <div class="form-field-with-btn">
          <input
            v-model="form.datasetTags"
            type="text"
            class="form-input"
            placeholder="请点击选择标签"
            readonly
          />
          <button type="button" class="form-select-btn" @click="openDatasetTags">
            选择
          </button>
        </div>
      </div>

      <!-- 提交 -->
      <div class="form-actions">
        <button type="submit" class="form-submit-btn">提交</button>
      </div>
    </form>

    <!-- 科学分类选择弹窗 -->
    <Teleport to="body">
      <div v-if="scienceCategoryModalVisible" class="select-modal-mask" @click.self="closeScienceCategoryModal">
        <div class="select-modal">
          <div class="select-modal-header">
            <span class="select-modal-title">选择</span>
            <button type="button" class="select-modal-close" @click="closeScienceCategoryModal">×</button>
          </div>
          <div class="select-modal-body">
            <div class="select-modal-search">
              <label class="select-modal-label">科学分类</label>
              <input
                v-model="scienceCategoryKeyword"
                type="text"
                class="select-modal-input"
                placeholder="请输入科学分类"
              />
              <button type="button" class="select-modal-btn primary" @click="queryScienceCategory">查询</button>
              <button type="button" class="select-modal-btn ghost" @click="resetScienceCategoryQuery">重置</button>
            </div>
            <div class="select-modal-current">
              当前选中：<span class="select-modal-tags">{{ scienceCategorySelectedLabels || ' ' }}</span>
            </div>
            <div class="select-modal-table-wrap">
              <table class="select-modal-table">
                <thead>
                  <tr>
                    <th class="col-check"><input type="checkbox" :checked="scienceCategoryAllChecked" @change="toggleScienceCategoryAll" /></th>
                    <th class="col-num">序号</th>
                    <th class="col-name">科学分类</th>
                    <th class="col-count">数据集数</th>
                    <th class="col-count">模板数</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, index) in scienceCategoryTableRows"
                    :key="row.id"
                    :class="{ selected: scienceCategoryCheckedIds.has(row.id) }"
                  >
                    <td class="col-check">
                      <input
                        type="checkbox"
                        :checked="scienceCategoryCheckedIds.has(row.id)"
                        @change="toggleScienceCategoryRow(row)"
                      />
                    </td>
                    <td class="col-num">{{ (scienceCategoryPage - 1) * scienceCategoryPageSize + index + 1 }}</td>
                    <td class="col-name" :style="{ paddingLeft: (row.level * 20 + 12) + 'px' }">
                      <span v-if="row.hasChildren" class="tree-chevron" @click="toggleScienceCategoryRowExpand(row)">
                        {{ row.expanded ? '▼' : '▶' }}
                      </span>
                      <span v-else class="tree-chevron-placeholder"></span>
                      {{ row.name }}
                    </td>
                    <td class="col-count">{{ row.datasetCount }}</td>
                    <td class="col-count">{{ row.templateCount }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="select-modal-pagination">
              <span class="select-modal-total">共{{ scienceCategoryTotal }}条</span>
              <div class="select-modal-pager">
                <button type="button" class="pager-btn" :disabled="scienceCategoryPage <= 1" @click="scienceCategoryPage--">上一页</button>
                <span class="pager-num">{{ scienceCategoryPage }}</span>
                <button type="button" class="pager-btn" :disabled="scienceCategoryPage >= scienceCategoryTotalPages" @click="scienceCategoryPage++">下一页</button>
              </div>
            </div>
          </div>
          <div class="select-modal-footer">
            <button type="button" class="select-modal-footer-btn ghost" @click="closeScienceCategoryModal">取消</button>
            <button type="button" class="select-modal-footer-btn primary" @click="confirmScienceCategory">提交</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 产品分类选择弹窗 -->
    <Teleport to="body">
      <div v-if="productCategoryModalVisible" class="select-modal-mask" @click.self="closeProductCategoryModal">
        <div class="select-modal select-modal-product">
          <div class="select-modal-header">
            <span class="select-modal-title">选择</span>
            <button type="button" class="select-modal-close" @click="closeProductCategoryModal">×</button>
          </div>
          <div class="select-modal-body">
            <div class="select-modal-search product-search">
              <label class="select-modal-label">产业分类</label>
              <input
                v-model="productCategoryKeywordIndustry"
                type="text"
                class="select-modal-input"
                placeholder="请输入产业分类"
              />
              <label class="select-modal-label">行业分类</label>
              <input
                v-model="productCategoryKeywordSector"
                type="text"
                class="select-modal-input"
                placeholder="请输入行业分类"
              />
              <label class="select-modal-label">产品分类</label>
              <input
                v-model="productCategoryKeywordProduct"
                type="text"
                class="select-modal-input"
                placeholder="请输入产品分类"
              />
              <button type="button" class="select-modal-btn primary" @click="queryProductCategory">查询</button>
              <button type="button" class="select-modal-btn ghost" @click="resetProductCategoryQuery">重置</button>
            </div>
            <div class="select-modal-current">
              当前选中：<span class="select-modal-tags">{{ productCategorySelectedLabels || ' ' }}</span>
            </div>
            <div class="select-modal-table-wrap">
              <table class="select-modal-table product-table">
                <thead>
                  <tr>
                    <th class="col-check"><input type="checkbox" :checked="productCategoryAllChecked" @change="toggleProductCategoryAll" /></th>
                    <th class="col-code">产业代码</th>
                    <th class="col-name">产业分类</th>
                    <th class="col-code">行业代码</th>
                    <th class="col-name">行业分类</th>
                    <th class="col-code">产品代码</th>
                    <th class="col-name">产品分类</th>
                  </tr>
                </thead>
                <tbody>
                  <tr
                    v-for="(row, index) in productCategoryTableRows"
                    :key="row.id"
                    :class="{ selected: productCategoryCheckedIds.has(row.id) }"
                  >
                    <td class="col-check">
                      <input
                        type="checkbox"
                        :checked="productCategoryCheckedIds.has(row.id)"
                        @change="toggleProductCategoryRow(row)"
                      />
                    </td>
                    <td class="col-code">{{ row.industryCode }}</td>
                    <td class="col-name" :style="{ paddingLeft: (row.level * 20 + 12) + 'px' }">
                      <span v-if="row.hasChildren" class="tree-chevron" @click="toggleProductCategoryRowExpand(row)">
                        {{ row.expanded ? '▼' : '▶' }}
                      </span>
                      <span v-else class="tree-chevron-placeholder"></span>
                      {{ row.industryName }}
                    </td>
                    <td class="col-code">{{ row.sectorCode || '' }}</td>
                    <td class="col-name">{{ row.sectorName || '' }}</td>
                    <td class="col-code">{{ row.productCode || '' }}</td>
                    <td class="col-name">{{ row.productName || '' }}</td>
                  </tr>
                </tbody>
              </table>
            </div>
            <div class="select-modal-pagination">
              <span class="select-modal-total">共{{ productCategoryTotal }}条</span>
              <div class="select-modal-pager">
                <button type="button" class="pager-btn" :disabled="productCategoryPage <= 1" @click="productCategoryPage--">上一页</button>
                <span class="pager-num">{{ productCategoryPage }}</span>
                <button type="button" class="pager-btn" :disabled="productCategoryPage >= productCategoryTotalPages" @click="productCategoryPage++">下一页</button>
              </div>
            </div>
          </div>
          <div class="select-modal-footer">
            <button type="button" class="select-modal-footer-btn ghost" @click="closeProductCategoryModal">取消</button>
            <button type="button" class="select-modal-footer-btn primary" @click="confirmProductCategory">提交</button>
          </div>
        </div>
      </div>
    </Teleport>

    <!-- 数据集标签选择弹窗 -->
    <Teleport to="body">
      <div v-if="datasetTagsModalVisible" class="select-modal-mask" @click.self="closeDatasetTagsModal">
        <div class="select-modal select-modal-simple">
          <div class="select-modal-header">
            <span class="select-modal-title">选择</span>
            <button type="button" class="select-modal-close" @click="closeDatasetTagsModal">×</button>
          </div>
          <div class="select-modal-body">
            <div class="select-modal-single-row">
              <label class="select-modal-label">数据集标签</label>
              <div class="select-modal-select-wrap">
                <select v-model="datasetTagsSelectValue" class="select-modal-select">
                  <option value="" disabled>请选择数据集标签</option>
                  <option v-for="opt in datasetTagOptions" :key="opt.value" :value="opt.value">{{ opt.label }}</option>
                </select>
                <svg class="select-modal-select-caret" width="10" height="6" viewBox="0 0 10 6">
                  <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                </svg>
              </div>
            </div>
          </div>
          <div class="select-modal-footer">
            <button type="button" class="select-modal-footer-btn ghost" @click="closeDatasetTagsModal">取消</button>
            <button type="button" class="select-modal-footer-btn primary" @click="confirmDatasetTags">确定</button>
          </div>
        </div>
      </div>
    </Teleport>
  </section>
</template>

<script setup>
import { ref, reactive, computed, onMounted, onBeforeUnmount } from 'vue'

const coverInputRef = ref(null)
const coverPreview = ref('')
const dataLevelDropRef = ref(null)
const dataLevelDropOpen = ref(false)
const templateTagOptions = ref([])
const templateOptions = ref([])

const form = reactive({
  name: '',
  summary: '',
  scienceCategory: '',
  productCategory: '',
  dataLevel: '',
  dataCategory: '数据集',
  templateTag: '',
  template: '',
  datasetTags: '',
})

// 数据级别选项：公益-绿，高值-红，私有-蓝
const dataLevelOptions = [
  { value: 'public', label: '公益', colorClass: 'data-level-public' },
  { value: 'highvalue', label: '高值', colorClass: 'data-level-highvalue' },
  { value: 'private', label: '私有', colorClass: 'data-level-private' },
]
const dataLevelOption = computed(() =>
  dataLevelOptions.find((o) => o.value === form.dataLevel)
)

async function loadTemplateTags() {
  try {
    const resp = await fetch('/api/dicts/template-tags')
    const json = await resp.json()
    if (json.code === 0 && Array.isArray(json.data)) {
      templateTagOptions.value = json.data.map((item) => ({
        value: item.id,
        label: item.name,
      }))
    }
  } catch (e) {
    console.error('loadTemplateTags error', e)
  }
}

async function loadTemplates() {
  try {
    const resp = await fetch('/api/dicts/templates')
    const json = await resp.json()
    if (json.code === 0 && Array.isArray(json.data)) {
      templateOptions.value = json.data.map((item) => ({
        value: item.id,
        label: item.name,
      }))
    }
  } catch (e) {
    console.error('loadTemplates error', e)
  }
}

function onDocClickDataLevel(e) {
  if (dataLevelDropRef.value && !dataLevelDropRef.value.contains(e.target)) {
    dataLevelDropOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClickDataLevel)
  loadTemplateTags()
  loadTemplates()
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClickDataLevel)
})

// 科学分类弹窗
const scienceCategoryModalVisible = ref(false)
const scienceCategoryKeyword = ref('')
const scienceCategoryPage = ref(1)
const scienceCategoryPageSize = ref(10)
const scienceCategoryCheckedIds = ref(new Set())
const scienceCategoryRows = ref([])

function flattenScienceTree(nodes, level = 0, acc = []) {
  if (!Array.isArray(nodes)) return acc
  for (const node of nodes) {
    const item = {
      id: node.id,
      name: node.name,
      level,
      datasetCount: node.datasetCount ?? 0,
      templateCount: node.templateCount ?? 0,
      hasChildren: Array.isArray(node.children) && node.children.length > 0,
      expanded: true,
    }
    acc.push(item)
    if (item.hasChildren) {
      flattenScienceTree(node.children, level + 1, acc)
    }
  }
  return acc
}

async function loadScienceCategory() {
  try {
    const resp = await fetch('/api/categories/science/tree', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        keyword: scienceCategoryKeyword.value || '',
        page: scienceCategoryPage.value,
        pageSize: scienceCategoryPageSize.value,
      }),
    })
    const json = await resp.json()
    if (json.code === 0) {
      scienceCategoryRows.value = flattenScienceTree(json.data || [])
    }
  } catch (e) {
    console.error('loadScienceCategory error', e)
  }
}

const scienceCategoryVisibleRows = computed(() => {
  const rows = scienceCategoryRows.value
  const visible = []
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i]
    if (row.level === 0) {
      visible.push(row)
      continue
    }
    let parentExpanded = true
    for (let j = i - 1; j >= 0; j--) {
      if (rows[j].level < row.level) {
        parentExpanded = rows[j].expanded !== false
        break
      }
    }
    if (parentExpanded) visible.push(row)
  }
  return visible
})

const scienceCategoryTableRows = computed(() => {
  const visible = scienceCategoryVisibleRows.value
  const start = (scienceCategoryPage.value - 1) * scienceCategoryPageSize.value
  return visible.slice(start, start + scienceCategoryPageSize.value)
})

const scienceCategoryTotal = computed(() => scienceCategoryVisibleRows.value.length)

const scienceCategoryTotalPages = computed(() =>
  Math.max(1, Math.ceil(scienceCategoryTotal.value / scienceCategoryPageSize.value))
)

const scienceCategorySelectedLabels = computed(() => {
  const ids = scienceCategoryCheckedIds.value
  const names = scienceCategoryRows.value.filter((r) => ids.has(r.id)).map((r) => r.name)
  return names.join('、') || ''
})

const scienceCategoryAllChecked = computed(() => {
  const rows = scienceCategoryTableRows.value
  return rows.length > 0 && rows.every((r) => scienceCategoryCheckedIds.value.has(r.id))
})

function openScienceCategory() {
  scienceCategoryModalVisible.value = true
  scienceCategoryKeyword.value = ''
  scienceCategoryPage.value = 1
  loadScienceCategory()
}

function closeScienceCategoryModal() {
  scienceCategoryModalVisible.value = false
}

function queryScienceCategory() {
  scienceCategoryPage.value = 1
  loadScienceCategory()
}

function resetScienceCategoryQuery() {
  scienceCategoryKeyword.value = ''
  scienceCategoryPage.value = 1
}

function toggleScienceCategoryRow(row) {
  const set = new Set(scienceCategoryCheckedIds.value)
  if (set.has(row.id)) set.delete(row.id)
  else set.add(row.id)
  scienceCategoryCheckedIds.value = set
}

function toggleScienceCategoryAll() {
  const rows = scienceCategoryTableRows.value
  const set = new Set(scienceCategoryCheckedIds.value)
  const allChecked = rows.every((r) => set.has(r.id))
  if (allChecked) rows.forEach((r) => set.delete(r.id))
  else rows.forEach((r) => set.add(r.id))
  scienceCategoryCheckedIds.value = set
}

function toggleScienceCategoryRowExpand(row) {
  row.expanded = !row.expanded
}

function confirmScienceCategory() {
  const names = scienceCategoryRows.value
    .filter((r) => scienceCategoryCheckedIds.value.has(r.id))
    .map((r) => r.name)
  form.scienceCategory = names.join('、')
  closeScienceCategoryModal()
}

// 产品分类弹窗
const productCategoryModalVisible = ref(false)
const productCategoryKeywordIndustry = ref('')
const productCategoryKeywordSector = ref('')
const productCategoryKeywordProduct = ref('')
const productCategoryPage = ref(1)
const productCategoryPageSize = ref(10)
const productCategoryCheckedIds = ref(new Set())
const productCategoryRows = ref([])

function flattenProductTree(nodes, level = 0, acc = []) {
  if (!Array.isArray(nodes)) return acc
  for (const node of nodes) {
    const item = {
      id: node.id,
      level,
      industryCode: node.industryCode || '',
      industryName: node.industryName || '',
      sectorCode: node.sectorCode || '',
      sectorName: node.sectorName || '',
      productCode: node.productCode || '',
      productName: node.productName || '',
      hasChildren: Array.isArray(node.children) && node.children.length > 0,
      expanded: true,
    }
    acc.push(item)
    if (item.hasChildren) {
      flattenProductTree(node.children, level + 1, acc)
    }
  }
  return acc
}

async function loadProductCategory() {
  try {
    const resp = await fetch('/api/categories/product/tree', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        industryKeyword: productCategoryKeywordIndustry.value || '',
        sectorKeyword: productCategoryKeywordSector.value || '',
        productKeyword: productCategoryKeywordProduct.value || '',
        page: productCategoryPage.value,
        pageSize: productCategoryPageSize.value,
      }),
    })
    const json = await resp.json()
    if (json.code === 0) {
      productCategoryRows.value = flattenProductTree(json.data || [])
    }
  } catch (e) {
    console.error('loadProductCategory error', e)
  }
}

const productCategoryVisibleRows = computed(() => {
  const rows = productCategoryRows.value
  const visible = []
  for (let i = 0; i < rows.length; i++) {
    const row = rows[i]
    if (row.level === 0) {
      visible.push(row)
      continue
    }
    let parentExpanded = true
    for (let j = i - 1; j >= 0; j--) {
      if (rows[j].level < row.level) {
        parentExpanded = rows[j].expanded !== false
        break
      }
    }
    if (parentExpanded) visible.push(row)
  }
  return visible
})

const productCategoryTableRows = computed(() => {
  const visible = productCategoryVisibleRows.value
  const start = (productCategoryPage.value - 1) * productCategoryPageSize.value
  return visible.slice(start, start + productCategoryPageSize.value)
})

const productCategoryTotal = computed(() => productCategoryVisibleRows.value.length)

const productCategoryTotalPages = computed(() =>
  Math.max(1, Math.ceil(productCategoryTotal.value / productCategoryPageSize.value))
)

const productCategorySelectedLabels = computed(() => {
  const ids = productCategoryCheckedIds.value
  const names = productCategoryRows.value.filter((r) => ids.has(r.id)).map((r) => r.industryName || r.sectorName || r.productName || '')
  return names.join('、') || ''
})

const productCategoryAllChecked = computed(() => {
  const rows = productCategoryTableRows.value
  return rows.length > 0 && rows.every((r) => productCategoryCheckedIds.value.has(r.id))
})

function openProductCategory() {
  productCategoryModalVisible.value = true
  productCategoryKeywordIndustry.value = ''
  productCategoryKeywordSector.value = ''
  productCategoryKeywordProduct.value = ''
  productCategoryPage.value = 1
  loadProductCategory()
}

function closeProductCategoryModal() {
  productCategoryModalVisible.value = false
}

function queryProductCategory() {
  productCategoryPage.value = 1
  loadProductCategory()
}

function resetProductCategoryQuery() {
  productCategoryKeywordIndustry.value = ''
  productCategoryKeywordSector.value = ''
  productCategoryKeywordProduct.value = ''
  productCategoryPage.value = 1
}

function toggleProductCategoryRow(row) {
  const set = new Set(productCategoryCheckedIds.value)
  if (set.has(row.id)) set.delete(row.id)
  else set.add(row.id)
  productCategoryCheckedIds.value = set
}

function toggleProductCategoryAll() {
  const rows = productCategoryTableRows.value
  const set = new Set(productCategoryCheckedIds.value)
  const allChecked = rows.every((r) => set.has(r.id))
  if (allChecked) rows.forEach((r) => set.delete(r.id))
  else rows.forEach((r) => set.add(r.id))
  productCategoryCheckedIds.value = set
}

function toggleProductCategoryRowExpand(row) {
  row.expanded = !row.expanded
}

function confirmProductCategory() {
  const names = productCategoryRows.value
    .filter((r) => productCategoryCheckedIds.value.has(r.id))
    .map((r) => r.industryName || r.sectorName || r.productName)
  form.productCategory = names.join('、')
  closeProductCategoryModal()
}

function triggerCoverUpload() {
  coverInputRef.value?.click()
}

function onCoverChange(e) {
  const file = e.target.files?.[0]
  if (!file) return
  const reader = new FileReader()
  reader.onload = () => {
    coverPreview.value = reader.result
  }
  reader.readAsDataURL(file)
}

// 数据集标签弹窗
const datasetTagsModalVisible = ref(false)
const datasetTagsSelectValue = ref('')
const datasetTagOptions = ref([
  { value: 'tag1', label: '标签一' },
  { value: 'tag2', label: '标签二' },
  { value: 'tag3', label: '标签三' },
])

function openDatasetTags() {
  datasetTagsModalVisible.value = true
  const found = datasetTagOptions.value.find((o) => o.label === form.datasetTags)
  datasetTagsSelectValue.value = found ? found.value : ''
}

function closeDatasetTagsModal() {
  datasetTagsModalVisible.value = false
}

function confirmDatasetTags() {
  const opt = datasetTagOptions.value.find((o) => o.value === datasetTagsSelectValue.value)
  form.datasetTags = opt ? opt.label : datasetTagsSelectValue.value
  closeDatasetTagsModal()
}

function onSubmit() {
  // TODO: 校验并提交
  console.log('提交', form)
}
</script>

<style scoped>
.create-dataset-page {
  background: var(--white);
  border-radius: 10px;
  padding: 32px 40px 40px;
  box-shadow: 0 3px 14px rgba(26, 78, 158, 0.09);
  max-width: 1800px;
  margin: 0 auto;
}

.create-dataset-title {
  font-size: 20px;
  font-weight: 700;
  color: var(--text-main);
  text-align: center;
  margin-bottom: 28px;
}

.create-dataset-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.form-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
}

.form-label {
  flex-shrink: 0;
  width: 100px;
  padding-top: 8px;
  font-size: 14px;
  color: var(--text-main);
}

.form-label.required::before {
  content: '*';
  color: #e74c3c;
  margin-right: 2px;
}

.form-field-wrap {
  flex: 1;
  position: relative;
  display: flex;
  align-items: center;
}

.form-field-textarea {
  align-items: flex-end;
}

.form-input,
.form-textarea,
.form-select {
  flex: 1;
  padding: 8px 12px;
  font-size: 14px;
  color: var(--text-main);
  border: 1px solid var(--border);
  border-radius: 6px;
  outline: none;
  transition: border-color 0.2s;
}

.form-input:focus,
.form-textarea:focus,
.form-select:focus {
  border-color: var(--primary);
}

.form-input::placeholder,
.form-textarea::placeholder {
  color: var(--text-gray);
}

.form-textarea {
  resize: vertical;
  min-height: 88px;
}

.form-counter {
  position: absolute;
  right: 12px;
  bottom: 8px;
  font-size: 12px;
  color: var(--text-gray);
}

.form-field-wrap:not(.form-field-textarea) .form-counter {
  bottom: auto;
  top: 50%;
  transform: translateY(-50%);
}

.form-field-with-btn,
.form-field-with-info {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 8px;
}

.form-field-with-btn .form-input,
.form-field-with-info .form-input {
  flex: 1;
}

.form-select-btn {
  flex-shrink: 0;
  padding: 8px 16px;
  font-size: 14px;
  color: var(--white);
  background: var(--primary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.form-select-btn:hover {
  background: var(--primary-dark);
}

.form-info-icon {
  flex-shrink: 0;
  width: 20px;
  height: 20px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: var(--text-gray);
  background: #eef2f8;
  border-radius: 50%;
  cursor: help;
}

.form-select {
  appearance: none;
  background-image: url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='10' height='6' viewBox='0 0 10 6'%3E%3Cpath fill='%237a8ba3' d='M1 1l4 4 4-4' stroke='%237a8ba3' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E");
  background-repeat: no-repeat;
  background-position: right 12px center;
  padding-right: 32px;
}

/* 数据级别自定义下拉 */
.form-select-custom {
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  padding-right: 32px;
}

.form-select-custom .form-select-caret {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-gray);
  transition: transform 0.2s;
}

.form-select-custom.open .form-select-caret {
  transform: translateY(-50%) rotate(180deg);
}

.form-select-placeholder {
  color: var(--text-gray);
}

.form-select-dropdown {
  position: absolute;
  left: 0;
  right: 0;
  top: 100%;
  margin: 4px 0 0;
  padding: 4px 0;
  list-style: none;
  background: var(--white);
  border: 1px solid var(--border);
  border-radius: 6px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.1);
  z-index: 10;
}

.form-select-option {
  padding: 8px 12px;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.15s;
}

.form-select-option:hover {
  background: #f0f4fa;
}

/* 数据级别标签：圆角底色 + 白字 */
.data-level-tag {
  display: inline-block;
  padding: 4px 14px;
  font-size: 13px;
  font-weight: 500;
  color: #fff;
  border-radius: 999px;
}

.data-level-public {
  background: #2a7e3c;
}

.data-level-highvalue {
  background: #c04030;
}

.data-level-private {
  background: #6b7280;
}

/* 封面上传 */
.form-upload {
  flex: 1;
  max-width: 226px;
  height: 170px;
  border: 1px dashed var(--border);
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  cursor: pointer;
  overflow: hidden;
  position: relative;
  background: #fafbfc;
  transition: border-color 0.2s, background 0.2s;
}

.form-upload:hover {
  border-color: var(--primary);
  background: rgba(26, 92, 230, 0.04);
}

.form-upload-input {
  position: absolute;
  width: 0;
  height: 0;
  opacity: 0;
  pointer-events: none;
}

.form-upload-placeholder {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 6px;
  color: var(--text-gray);
  font-size: 13px;
}

.form-upload-plus {
  font-size: 28px;
  font-weight: 300;
  color: var(--text-gray);
  line-height: 1;
}

.form-upload-tip {
  display: block;
  text-align: center;
}

.form-upload-preview {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.form-actions {
  display: flex;
  justify-content: center;
  padding-top: 16px;
  margin-top: 8px;
}

.form-submit-btn {
  padding: 10px 48px;
  font-size: 15px;
  font-weight: 500;
  color: var(--white);
  background: var(--primary);
  border: none;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s;
}

.form-submit-btn:hover {
  background: var(--primary-dark);
}

/* 科学分类选择弹窗 */
.select-modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 24px;
}

.select-modal {
  background: var(--white);
  border-radius: 8px;
  box-shadow: 0 6px 24px rgba(0, 0, 0, 0.15);
  width: 100%;
  max-width: 960px;
  max-height: 90vh;
  display: flex;
  flex-direction: column;
}

.select-modal-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 16px 20px;
  border-bottom: 1px solid var(--border);
}

.select-modal-title {
  font-size: 16px;
  font-weight: 600;
  color: var(--text-main);
}

.select-modal-close {
  width: 28px;
  height: 28px;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 20px;
  line-height: 1;
  color: var(--text-gray);
  background: none;
  border: none;
  cursor: pointer;
  border-radius: 4px;
  transition: color 0.2s, background 0.2s;
}

.select-modal-close:hover {
  color: var(--text-main);
  background: #f0f2f5;
}

.select-modal-body {
  padding: 20px;
  overflow: auto;
  flex: 1;
}

.select-modal-search {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.select-modal-label {
  flex-shrink: 0;
  font-size: 14px;
  color: var(--text-main);
}

.select-modal-input {
  flex: 1;
  max-width: 280px;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  outline: none;
}

.select-modal-input:focus {
  border-color: var(--primary);
}

.select-modal-btn {
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.select-modal-btn.primary {
  color: var(--white);
  background: var(--primary);
  border: none;
}

.select-modal-btn.primary:hover {
  background: var(--primary-dark);
}

.select-modal-btn.ghost {
  color: var(--text-main);
  background: var(--white);
  border: 1px solid var(--border);
}

.select-modal-btn.ghost:hover {
  border-color: var(--primary);
  color: var(--primary);
}

.select-modal-current {
  font-size: 13px;
  color: var(--text-gray);
  margin-bottom: 12px;
}

.select-modal-tags {
  color: var(--text-main);
}

.select-modal-table-wrap {
  border: 1px solid var(--border);
  border-radius: 6px;
  overflow: hidden;
  margin-bottom: 12px;
}

.select-modal-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.select-modal-table th,
.select-modal-table td {
  padding: 10px 12px;
  text-align: left;
  border-bottom: 1px solid var(--border);
}

.select-modal-table thead {
  background: #fafbfc;
}

.select-modal-table th {
  font-weight: 600;
  color: var(--text-main);
}

.select-modal-table tbody tr:hover {
  background: #f8fafd;
}

.select-modal-table tbody tr.selected {
  background: #e8f2fe;
}

.col-check {
  width: 48px;
  text-align: center;
}

.col-num {
  width: 56px;
}

.col-name {
  min-width: 200px;
}

.col-count {
  width: 90px;
}

.col-code {
  width: 90px;
}

/* 产品分类弹窗：更宽 + 三列搜索 */
.select-modal-product {
  max-width: 1100px;
}

.select-modal-search.product-search {
  flex-wrap: wrap;
  gap: 8px 16px;
}

.select-modal-search.product-search .select-modal-label {
  margin-right: 0;
}

.select-modal-search.product-search .select-modal-input {
  max-width: 160px;
}

.product-table .col-name {
  min-width: 160px;
}

.tree-chevron,
.tree-chevron-placeholder {
  display: inline-block;
  width: 16px;
  margin-right: 4px;
  font-size: 10px;
  color: var(--text-gray);
  cursor: pointer;
}

.tree-chevron-placeholder {
  cursor: default;
  visibility: hidden;
}

.select-modal-pagination {
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: var(--text-gray);
}

.select-modal-total {
  margin-right: 12px;
}

.select-modal-pager {
  display: flex;
  align-items: center;
  gap: 8px;
}

.pager-btn {
  padding: 4px 10px;
  font-size: 13px;
  color: var(--text-main);
  background: var(--white);
  border: 1px solid var(--border);
  border-radius: 4px;
  cursor: pointer;
}

.pager-btn:hover:not(:disabled) {
  border-color: var(--primary);
  color: var(--primary);
}

.pager-btn:disabled {
  cursor: not-allowed;
  opacity: 0.6;
}

.pager-num {
  padding: 4px 10px;
  font-weight: 500;
  color: var(--primary);
}

.select-modal-footer {
  display: flex;
  justify-content: flex-end;
  gap: 12px;
  padding: 16px 20px;
  border-top: 1px solid var(--border);
}

.select-modal-footer-btn {
  padding: 8px 20px;
  font-size: 14px;
  border-radius: 6px;
  cursor: pointer;
  transition: background 0.2s, color 0.2s;
}

.select-modal-footer-btn.primary {
  color: var(--white);
  background: var(--primary);
  border: none;
}

.select-modal-footer-btn.primary:hover {
  background: var(--primary-dark);
}

.select-modal-footer-btn.ghost {
  color: var(--text-main);
  background: var(--white);
  border: 1px solid var(--border);
}

.select-modal-footer-btn.ghost:hover {
  border-color: var(--primary);
  color: var(--primary);
}

/* 数据集标签简易弹窗 */
.select-modal-simple {
  max-width: 480px;
}

.select-modal-single-row {
  display: flex;
  align-items: center;
  gap: 12px;
}

.select-modal-single-row .select-modal-label {
  flex-shrink: 0;
  width: 90px;
}

.select-modal-select-wrap {
  flex: 1;
  position: relative;
}

.select-modal-select {
  width: 100%;
  padding: 8px 32px 8px 12px;
  font-size: 14px;
  color: var(--text-main);
  border: 1px solid var(--border);
  border-radius: 6px;
  outline: none;
  appearance: none;
  background: var(--white);
  cursor: pointer;
}

.select-modal-select:focus {
  border-color: var(--primary);
}

.select-modal-select-caret {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: var(--text-gray);
  pointer-events: none;
}
</style>
