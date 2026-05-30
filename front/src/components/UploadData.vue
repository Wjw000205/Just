<template>
  <section class="upload-page">
    <div class="upload-breadcrumb">
      当前位置：<span class="crumb-main">首页</span> &gt;
      <span class="crumb-main">数据上传</span> &gt;
      <span class="crumb-now">上传数据</span>
    </div>

    <div class="upload-hero"></div>

    <div class="upload-card">
      <h1 class="upload-title">上传数据</h1>

      <!-- 选择信息 -->
      <section class="upload-section">
        <h2 class="upload-section-title">选择信息</h2>

        <div class="upload-form-row">
          <label class="upload-label required">科学分类：</label>
          <div class="upload-field upload-field-with-btn">
            <input
              v-model="form.scienceCategory"
              type="text"
              class="upload-input"
              placeholder="请点击选择"
              readonly
            />
            <button type="button" class="upload-select-btn" @click="openScienceCategory">
              选择
            </button>
          </div>
        </div>

        <div class="upload-form-row">
          <label class="upload-label required">选择数据集：</label>
          <div class="upload-field">
            <select v-model="form.datasetId" class="upload-select">
              <option value="" disabled>请选择数据集</option>
              <option
                v-for="opt in datasetOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
          </div>
        </div>

        <div class="upload-form-row upload-mode-row">
          <label class="upload-label">上传方式</label>
          <div class="upload-mode-switch">
            <button
              type="button"
              class="upload-mode-btn"
              :class="{ active: form.mode === 'online' }"
              @click="changeMode('online')"
            >
              在线填写
            </button>
            <button
              type="button"
              class="upload-mode-btn"
              :class="{ active: form.mode === 'batch' }"
              @click="changeMode('batch')"
            >
              批量上传
            </button>
          </div>
        </div>
      </section>

      <!-- 数据信息 -->
      <section class="upload-section">
        <h2 class="upload-section-title">数据信息</h2>

        <!-- 在线填写：根据后端返回的表单结构动态渲染 -->
        <div v-if="form.mode === 'online'" class="upload-online-area">
          <div v-if="schemaLoading" class="upload-schema-status">表单结构加载中…</div>
          <div v-else-if="schemaError" class="upload-schema-status error">
            {{ schemaError }}
          </div>
          <template v-else-if="datasetSchema && datasetSchema.sections?.length">
            <div
              v-for="section in datasetSchema.sections"
              :key="section.id"
              class="upload-section-card"
            >
              <div class="upload-section-card-header">
                <span class="upload-section-card-title">{{ section.title }}</span>
                <span v-if="section.subtitle" class="upload-section-card-subtitle">
                  {{ section.subtitle }}
                </span>
              </div>
              <div class="upload-section-card-body">
                <div
                  v-for="field in section.fields"
                  :key="field.id"
                  class="upload-field-row"
                >
                  <label class="upload-field-label" :class="{ required: field.required }">
                    {{ field.label }}
                  </label>
                  <div class="upload-field-control">
                    <input
                      v-if="!field.type || field.type === 'text'"
                      v-model="onlineForm[field.id]"
                      type="text"
                      class="upload-input"
                      :placeholder="field.placeholder || ''"
                    />
                    <select
                      v-else-if="field.type === 'select'"
                      v-model="onlineForm[field.id]"
                      class="upload-input"
                    >
                      <option value="" disabled>请选择</option>
                      <option
                        v-for="opt in field.options || []"
                        :key="opt.value"
                        :value="opt.value"
                      >
                        {{ opt.label }}
                      </option>
                    </select>
                    <textarea
                      v-else-if="field.type === 'textarea'"
                      v-model="onlineForm[field.id]"
                      class="upload-input upload-textarea"
                      :placeholder="field.placeholder || ''"
                      rows="3"
                    />
                    <input
                      v-else
                      v-model="onlineForm[field.id]"
                      type="text"
                      class="upload-input"
                      :placeholder="field.placeholder || ''"
                    />
                    <div v-if="field.description" class="upload-field-desc">
                      {{ field.description }}
                    </div>
                  </div>
                </div>
              </div>
            </div>
          </template>
          <div v-else class="upload-schema-status">
            请先选择科学分类和数据集，并点击“在线填写”加载表单。
          </div>
        </div>

        <!-- 批量上传 -->
        <div v-else class="upload-batch-area">
          <!-- 固定模板导入 + 下载模板 -->
          <div class="upload-batch-header">
            <div class="upload-batch-tab active">
              <span class="dot"></span>
              固定模板导入
              <span class="question-mark">?</span>
            </div>
            <div class="upload-template-download" @click.stop="toggleTemplateMenu">
              <span class="download-icon">↓</span>
              <span class="download-text">下载模板</span>
              <svg class="download-caret" width="10" height="6" viewBox="0 0 10 6">
                <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
              </svg>
              <ul v-if="templateMenuVisible" class="download-menu">
                <li class="download-menu-item" @click.stop="downloadTemplate('excel')">
                  {{ templateDownloadLoading ? '下载中…' : '下载EXCEL模板' }}
                </li>
              </ul>
            </div>
          </div>

          <div class="upload-batch-card">
            <div class="upload-batch-card-title">上传写好的数据文件</div>
            <div class="upload-batch-upload-btn-wrap">
              <input
                ref="batchFileInput"
                type="file"
                class="batch-file-input"
                accept=".xlsx,.xls,.csv,.zip,.rar"
                @change="onBatchFileChange"
              />
              <button type="button" class="upload-btn primary" :disabled="submitLoading" @click="openBatchFilePicker">
                上传
              </button>
            </div>
            <div v-if="batchFileName" class="batch-file-name">
              已选择：{{ batchFileName }}
              <button type="button" class="batch-file-clear" @click="clearBatchFile">清除</button>
            </div>
            <ol class="upload-batch-tips">
              <li>批量上传方式：下载模板，将所上传数据填入模板中后上传。</li>
              <li>图片及文档型数据上传：模板内对应数据填写相对于压缩包的路径，将填好的模板与附件一并压缩成 RAR 或 ZIP 文件后上传。</li>
              <li>压缩包需由填好的模板与文件夹/附件压缩，模板请勿放入压缩包内的文件夹中。</li>
              <li>仅支持上传 1GB 以内文件。</li>
              <li>导入数据必须与模板格式一致。</li>
            </ol>
          </div>
        </div>

        <div class="upload-actions">
          <button type="button" class="upload-btn ghost">暂存</button>
          <button
            type="button"
            class="upload-btn primary"
            :disabled="submitLoading"
            @click="submitUploadData"
          >
            {{ submitLoading ? '提交中…' : '提交' }}
          </button>
        </div>
      </section>
    </div>

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
                @keyup.enter="queryScienceCategory"
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
  </section>
</template>

<script setup>
import { computed, reactive, ref, onMounted, onBeforeUnmount, watch } from 'vue'

const getAuthHeader = () => {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

const form = reactive({
  scienceCategoryId: '',
  scienceCategory: '',
  datasetId: '',
  mode: 'online',
})

const datasetOptions = ref([])
const datasetSchema = ref(null)
const schemaLoading = ref(false)
const schemaError = ref('')
const onlineForm = reactive({})
const submitLoading = ref(false)
const templateMenuVisible = ref(false)
const templateDownloadLoading = ref(false)
const batchFileInput = ref(null)
const batchFile = ref(null)
const batchFileName = ref('')

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
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({
        keyword: scienceCategoryKeyword.value || '',
        page: scienceCategoryPage.value,
        pageSize: scienceCategoryPageSize.value,
      }),
    })
    const json = await resp.json().catch(() => null)
    if (resp.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      return
    }
    if ((json?.code === 0 || json?.code === 200) && Array.isArray(json.data)) {
      const rows = flattenScienceTree(json.data || [])
      const keyword = scienceCategoryKeyword.value.trim()
      scienceCategoryRows.value = keyword
        ? rows.filter((row) => row.name.includes(keyword))
        : rows
    } else {
      scienceCategoryRows.value = []
    }
  } catch (e) {
    console.error('loadScienceCategory error', e)
    alert(e?.message || '获取科学分类目录树失败')
    scienceCategoryRows.value = []
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
  loadScienceCategory()
}

function toggleScienceCategoryRow(row) {
  scienceCategoryCheckedIds.value = new Set([row.id])
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
  const row = scienceCategoryRows.value.find((r) => scienceCategoryCheckedIds.value.has(r.id))
  if (!row) {
    alert('请选择科学分类')
    return
  }
  form.scienceCategoryId = row.id
  form.scienceCategory = row.name
  form.datasetId = ''
  datasetOptions.value = []
  closeScienceCategoryModal()
}

async function loadDatasetOptions() {
  if (!form.scienceCategoryId) {
    datasetOptions.value = []
    form.datasetId = ''
    return
  }
  try {
    const resp = await fetch('/api/datasets/options', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({
        scienceCategoryIds: [Number(form.scienceCategoryId)].filter(Number.isFinite),
        keyword: '',
        page: 1,
        pageSize: 200,
      }),
    })
    const json = await resp.json().catch(() => null)
    if (resp.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      return
    }
    if ((json?.code === 0 || json?.code === 200) && Array.isArray(json.data)) {
      datasetOptions.value = json.data.filter((item) => Number(item.auditStatus) === 1).map((item) => ({
        value: item.id,
        name: item.name,
        label: item.recordCount != null ? `${item.name}（${item.recordCount}条）` : item.name,
      }))
      // 如果当前选中的 datasetId 不在新列表中，则清空
      if (!datasetOptions.value.some((o) => String(o.value) === String(form.datasetId))) {
        form.datasetId = ''
      }
    } else {
      datasetOptions.value = []
      form.datasetId = ''
    }
  } catch (e) {
    console.error('loadDatasetOptions error', e)
    datasetOptions.value = []
    form.datasetId = ''
  }
}

async function loadDatasetSchema() {
  if (!form.datasetId || form.mode !== 'online') {
    datasetSchema.value = null
    if (form.mode === 'online' && !form.datasetId) {
      schemaError.value = '请先选择科学分类和数据集'
    }
    return
  }
  schemaLoading.value = true
  schemaError.value = ''
  try {
    const resp = await fetch('/api/data/online/schema', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({ datasetId: form.datasetId }),
    })
    const json = await resp.json().catch(() => null)
    if (resp.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      datasetSchema.value = null
      return
    }
    if ((json?.code === 0 || json?.code === 200) && json.data) {
      datasetSchema.value = json.data
      // 初始化在线填写表单默认值
      Object.keys(onlineForm).forEach((k) => {
        delete onlineForm[k]
      })
      const sections = json.data.sections || []
      for (const section of sections) {
        for (const field of section.fields || []) {
          onlineForm[field.id] = field.defaultValue ?? ''
        }
      }
    } else {
      schemaError.value = json.message || '表单结构获取失败'
      datasetSchema.value = null
    }
  } catch (e) {
    console.error('loadDatasetSchema error', e)
    schemaError.value = '表单结构获取失败'
    datasetSchema.value = null
  } finally {
    schemaLoading.value = false
  }
}

function changeMode(mode) {
  form.mode = mode
  if (mode === 'online') {
    loadDatasetSchema()
  } else {
    datasetSchema.value = null
    templateMenuVisible.value = false
  }
}

function openBatchFilePicker() {
  if (!form.datasetId) {
    alert('请先选择数据集')
    return
  }
  batchFileInput.value?.click()
}

function onBatchFileChange(event) {
  const file = event.target.files?.[0] || null
  batchFile.value = file
  batchFileName.value = file?.name || ''
}

function clearBatchFile() {
  batchFile.value = null
  batchFileName.value = ''
  if (batchFileInput.value) {
    batchFileInput.value.value = ''
  }
}

function getSchemaFields() {
  const sections = Array.isArray(datasetSchema.value?.sections)
    ? datasetSchema.value.sections
    : []
  return sections.flatMap((section) =>
    Array.isArray(section.fields) ? section.fields : []
  )
}

function validateOnlineForm() {
  if (!form.datasetId) {
    return '请先选择数据集'
  }
  if (!datasetSchema.value) {
    return '请先加载在线填写表单'
  }

  const missing = getSchemaFields().find((field) => {
    if (!field.required) return false
    const value = onlineForm[field.id]
    return value == null || String(value).trim() === ''
  })
  if (missing) {
    return `请填写${missing.label || missing.id}`
  }

  return ''
}

function buildOnlineRecord() {
  const record = {}
  for (const field of getSchemaFields()) {
    record[field.id] = onlineForm[field.id] ?? ''
  }
  return record
}

async function submitOnlineData() {
  const error = validateOnlineForm()
  if (error) {
    alert(error)
    return
  }
  if (submitLoading.value) return

  submitLoading.value = true
  try {
    const resp = await fetch('/api/data/online/submit', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({
        datasetId: Number(form.datasetId),
        records: [buildOnlineRecord()],
      }),
    })
    const json = await resp.json().catch(() => null)
    if (resp.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      return
    }
    if (!json || (json.code !== 0 && json.code !== 200)) {
      alert(json?.message || '在线填写提交失败')
      return
    }

    const data = json.data || {}
    const failedCount = Number(data.failedCount ?? 0)
    if (failedCount > 0) {
      const firstError = Array.isArray(data.errors) ? data.errors[0] : null
      const detail = firstError?.message || firstError?.error || ''
      alert(detail ? `提交失败：${detail}` : '在线填写提交失败')
      return
    }

    alert('在线填写提交成功')
    for (const field of getSchemaFields()) {
      onlineForm[field.id] = field.defaultValue ?? ''
    }
  } catch (e) {
    console.error('submitOnlineData error', e)
    alert(e?.message || '在线填写提交失败')
  } finally {
    submitLoading.value = false
  }
}

async function submitBatchData() {
  if (!form.datasetId) {
    alert('请先选择数据集')
    return
  }
  if (!batchFile.value) {
    alert('请先上传数据文件')
    return
  }
  if (submitLoading.value) return

  submitLoading.value = true
  try {
    const body = new FormData()
    body.append('file', batchFile.value)

    const params = new URLSearchParams({
      datasetId: String(form.datasetId),
    })
    const resp = await fetch(`/api/data/batch/upload?${params.toString()}`, {
      method: 'POST',
      headers: getAuthHeader(),
      body,
    })
    const json = await resp.json().catch(() => null)
    if (resp.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      return
    }
    if (!json || (json.code !== 0 && json.code !== 200)) {
      alert(json?.message || '批量上传失败')
      return
    }

    const data = json.data || {}
    const acceptedCount = Number(data.acceptedCount ?? 0)
    const failedCount = Number(data.failedCount ?? 0)
    if (failedCount > 0) {
      const firstError = Array.isArray(data.errors) ? data.errors[0] : null
      const rowText = firstError?.rowIndex ? `第${firstError.rowIndex}行：` : ''
      const detail = firstError?.message || ''
      alert(detail ? `批量上传完成，存在失败：${rowText}${detail}` : `批量上传完成，失败 ${failedCount} 条`)
      return
    }

    alert(`批量上传成功，成功接收 ${acceptedCount} 条`)
    clearBatchFile()
  } catch (e) {
    console.error('submitBatchData error', e)
    alert(e?.message || '批量上传失败')
  } finally {
    submitLoading.value = false
  }
}

function submitUploadData() {
  if (form.mode === 'online') {
    submitOnlineData()
    return
  }
  submitBatchData()
}

function toggleTemplateMenu() {
  templateMenuVisible.value = !templateMenuVisible.value
}

function getFilenameFromContentDisposition(cd) {
  if (!cd) return ''
  // filename*=UTF-8''xxx  or filename="xxx"
  const matchUtf8 = cd.match(/filename\*\s*=\s*UTF-8''([^;]+)/i)
  if (matchUtf8 && matchUtf8[1]) {
    try {
      return decodeURIComponent(matchUtf8[1].replace(/(^"|"$)/g, ''))
    } catch (_) {
      return matchUtf8[1].replace(/(^"|"$)/g, '')
    }
  }
  const match = cd.match(/filename\s*=\s*("?)([^";]+)\1/i)
  return match && match[2] ? match[2] : ''
}

async function downloadTemplate(format = 'excel') {
  templateMenuVisible.value = false
  const selectedDataset = datasetOptions.value.find((item) => String(item.value) === String(form.datasetId))
<<<<<<< HEAD
  if (!selectedDataset?.name) {
=======
  if (!form.datasetId) {
>>>>>>> 1774c57 (完成模板和数据集接口对接)
    alert('请先选择数据集')
    return
  }
  if (templateDownloadLoading.value) return

  templateDownloadLoading.value = true
  try {
<<<<<<< HEAD
    const url = `/Dataset/export-template?DatasetName=${encodeURIComponent(selectedDataset.name)}`
=======
    const params = new URLSearchParams({
      datasetId: String(form.datasetId),
      format,
    })
    const url = `/api/data/batch/template?${params.toString()}`
>>>>>>> 1774c57 (完成模板和数据集接口对接)

    const resp = await fetch(url, {
      method: 'GET',
      headers: getAuthHeader(),
    })
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

    const blob = await resp.blob()
    const cd = resp.headers.get('content-disposition') || ''
<<<<<<< HEAD
    const fallbackName = `${selectedDataset.name}_template.xlsx`
=======
    const fallbackExt = format === 'csv' ? 'csv' : 'xlsx'
    const fallbackName = `${selectedDataset?.name || 'dataset'}_template.${fallbackExt}`
>>>>>>> 1774c57 (完成模板和数据集接口对接)
    const filename = getFilenameFromContentDisposition(cd) || fallbackName

    const blobUrl = URL.createObjectURL(blob)
    const a = document.createElement('a')
    a.href = blobUrl
    a.download = filename
    document.body.appendChild(a)
    a.click()
    a.remove()
    URL.revokeObjectURL(blobUrl)
  } catch (e) {
    console.error('downloadTemplate error', e)
    alert(e?.message || '下载模板失败')
  } finally {
    templateDownloadLoading.value = false
  }
}

function onDocClickCloseMenus(e) {
  if (!templateMenuVisible.value) return
  // 点击到菜单区域之外就关闭
  const el = e.target
  if (!el.closest('.upload-template-download')) {
    templateMenuVisible.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClickCloseMenus)
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClickCloseMenus)
})

watch(
  () => form.scienceCategoryId,
  () => {
    loadDatasetOptions()
  }
)

watch(
  () => form.datasetId,
  () => {
    clearBatchFile()
    if (form.mode === 'online') {
      loadDatasetSchema()
    }
  }
)
</script>

<style scoped>
.upload-page {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-breadcrumb {
  font-size: 13px;
  color: var(--text-gray);
}

.crumb-main {
  color: var(--text-main);
}

.crumb-now {
  color: var(--primary);
}

.upload-hero {
  height: 80px;
  border-radius: 10px;
  background: linear-gradient(135deg, #1a5ce6, #4a8fe6);
  opacity: 0.25;
}

.upload-card {
  margin-top: -48px;
  background: var(--white);
  border-radius: 10px;
  box-shadow: 0 3px 14px rgba(26, 78, 158, 0.09);
  padding: 28px 40px 36px;
}

.upload-title {
  font-size: 20px;
  font-weight: 700;
  text-align: center;
  margin-bottom: 24px;
}

.upload-section {
  margin-bottom: 24px;
}

.upload-section-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 16px;
}

.upload-form-row {
  display: flex;
  align-items: center;
  margin-bottom: 16px;
  gap: 12px;
}

.upload-label {
  width: 110px;
  text-align: right;
  font-size: 14px;
  color: var(--text-main);
}

.upload-label.required::before {
  content: '*';
  color: #e74c3c;
  margin-right: 2px;
}

.upload-field {
  flex: 1;
}

.upload-field-with-btn {
  display: flex;
  align-items: center;
  gap: 8px;
}

.upload-select {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  outline: none;
  appearance: none;
  background: var(--white);
}

.upload-select:focus {
  border-color: var(--primary);
}

.upload-select-btn {
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

.upload-select-btn:hover {
  background: var(--primary-dark);
}

.upload-mode-row {
  margin-top: 4px;
}

.upload-mode-switch {
  display: flex;
  gap: 8px;
}

.upload-mode-btn {
  min-width: 90px;
  padding: 8px 16px;
  font-size: 14px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--white);
  color: var(--text-main);
}

.upload-mode-btn.active {
  background: var(--primary);
  color: var(--white);
  border-color: var(--primary);
}

.upload-online-area {
  margin-top: 8px;
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.upload-schema-status {
  font-size: 13px;
  color: var(--text-gray);
}

.upload-schema-status.error {
  color: #c04030;
}

.upload-section-card {
  border: 1px solid var(--border);
  border-radius: 8px;
  background: #f8fafc;
}

.upload-section-card-header {
  padding: 10px 14px;
  border-bottom: 1px solid var(--border);
  background: #f0f4fa;
  display: flex;
  align-items: center;
  gap: 12px;
}

.upload-section-card-title {
  font-size: 14px;
  font-weight: 600;
}

.upload-section-card-subtitle {
  font-size: 12px;
  color: var(--text-gray);
}

.upload-section-card-body {
  padding: 16px 18px 12px;
}

.upload-field-row {
  display: flex;
  align-items: flex-start;
  gap: 12px;
  margin-bottom: 14px;
}

.upload-field-label {
  width: 110px;
  text-align: right;
  font-size: 14px;
  color: var(--text-main);
  padding-top: 6px;
}

.upload-field-label.required::before {
  content: '*';
  color: #e74c3c;
  margin-right: 2px;
}

.upload-field-control {
  flex: 1;
}

.upload-input {
  width: 100%;
  padding: 8px 12px;
  font-size: 14px;
  border: 1px solid var(--border);
  border-radius: 6px;
  outline: none;
}

.upload-input:focus {
  border-color: var(--primary);
}

.upload-textarea {
  resize: vertical;
  min-height: 80px;
}

.upload-field-desc {
  margin-top: 4px;
  font-size: 12px;
  color: var(--text-gray);
}

.upload-batch-area {
  margin-top: 8px;
}

.upload-batch-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.upload-batch-tab {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 14px;
  color: var(--text-main);
}

.upload-batch-tab .dot {
  width: 8px;
  height: 8px;
  border-radius: 50%;
  background: #1a5ce6;
}

.upload-batch-tab .question-mark {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #f4f6fb;
  font-size: 11px;
  color: var(--text-gray);
}

.upload-template-download {
  position: relative;
  display: inline-flex;
  align-items: center;
  gap: 4px;
  font-size: 13px;
  color: #1a5ce6;
  cursor: pointer;
}

.download-icon {
  font-size: 13px;
}

.download-caret {
  color: #1a5ce6;
}

.download-menu {
  position: absolute;
  right: 0;
  top: 100%;
  margin-top: 4px;
  min-width: 140px;
  padding: 4px 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.12);
  list-style: none;
  z-index: 10;
}

.download-menu-item {
  padding: 6px 12px;
  font-size: 13px;
  color: var(--text-main);
}

.download-menu-item:hover {
  background: #f0f4fa;
}

.upload-batch-card {
  margin-top: 4px;
  border-radius: 10px;
  border: 1px solid var(--border);
  background: #f8fafc;
  padding: 20px 32px 18px;
}

.upload-batch-card-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 18px;
}

.upload-batch-upload-btn-wrap {
  display: flex;
  justify-content: center;
  margin-bottom: 16px;
}

.batch-file-input {
  display: none;
}

.batch-file-name {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  margin: -4px 0 14px;
  font-size: 13px;
  color: var(--text-gray);
  word-break: break-all;
}

.batch-file-clear {
  border: none;
  background: transparent;
  color: #1a5ce6;
  cursor: pointer;
  font-size: 13px;
}

.upload-batch-tips {
  font-size: 13px;
  color: var(--text-gray);
  padding-left: 18px;
  line-height: 1.7;
}

.upload-actions {
  margin-top: 40px;
  display: flex;
  justify-content: center;
  gap: 16px;
}

.upload-btn {
  min-width: 96px;
  padding: 8px 24px;
  font-size: 14px;
  border-radius: 4px;
  border: 1px solid var(--border);
  background: var(--white);
}

.upload-btn.primary {
  background: var(--primary);
  border-color: var(--primary);
  color: var(--white);
}

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
  padding: 8px 24px;
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
</style>
