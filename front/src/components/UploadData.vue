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
          <div class="upload-field">
            <select v-model="form.scienceCategoryId" class="upload-select" @change="onScienceCategoryChange">
              <option value="" disabled>请选择</option>
              <option
                v-for="opt in scienceCategoryOptions"
                :key="opt.value"
                :value="opt.value"
              >
                {{ opt.label }}
              </option>
            </select>
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
                <li class="download-menu-item" @click="downloadTemplate('json')">下载JSON模板</li>
                <li class="download-menu-item" @click="downloadTemplate('excel')">下载EXCEL模板</li>
              </ul>
            </div>
          </div>

          <div class="upload-batch-card">
            <div class="upload-batch-card-title">上传写好的数据文件</div>
            <div class="upload-batch-upload-btn-wrap">
              <button type="button" class="upload-btn primary">上传</button>
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
          <button type="button" class="upload-btn primary">提交</button>
        </div>
      </section>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted, onBeforeUnmount, watch } from 'vue'

const getAuthHeader = () => {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

const form = reactive({
  scienceCategoryId: '',
  datasetId: '',
  mode: 'online',
})

const scienceCategoryOptions = ref([])
const datasetOptions = ref([])
const datasetSchema = ref(null)
const schemaLoading = ref(false)
const schemaError = ref('')
const onlineForm = reactive({})
const templateMenuVisible = ref(false)
const templateDownloadLoading = ref(false)

async function loadScienceCategoryOptions() {
  try {
    const resp = await fetch('/api/categories/science/tree', {
      method: 'POST',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
      body: JSON.stringify({ keyword: '', page: 1, pageSize: 1000 }),
    })
    const json = await resp.json().catch(() => null)
    if (resp.status === 401 || json?.code === 401) {
      alert(json?.message || '未登录或无权限')
      return
    }
    if (json?.code === 200 && Array.isArray(json.data)) {
      // 简单展平为一级下拉：只取叶子节点或所有节点名称
      const opts = []
      const walk = (nodes) => {
        if (!Array.isArray(nodes)) return
        for (const n of nodes) {
          opts.push({ value: n.id, label: n.name })
          if (Array.isArray(n.children) && n.children.length) {
            walk(n.children)
          }
        }
      }
      walk(json.data)
      scienceCategoryOptions.value = opts
    }
  } catch (e) {
    console.error('loadScienceCategoryOptions error', e)
  }
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
        scienceCategoryId: form.scienceCategoryId,
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
    if (json?.code === 200 && Array.isArray(json.data)) {
      datasetOptions.value = json.data.map((item) => ({
        value: item.id,
        label: item.name,
      }))
      // 如果当前选中的 datasetId 不在新列表中，则清空
      if (!datasetOptions.value.some((o) => o.value === form.datasetId)) {
        form.datasetId = ''
      }
    }
  } catch (e) {
    console.error('loadDatasetOptions error', e)
  }
}

function onScienceCategoryChange() {
  loadDatasetOptions()
}

async function loadDatasetSchema() {
  if (!form.datasetId || form.mode !== 'online') {
    datasetSchema.value = null
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
    if (json?.code === 200 && json.data) {
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

async function downloadTemplate(type) {
  templateMenuVisible.value = false
  if (!form.datasetId) return
  if (templateDownloadLoading.value) return

  templateDownloadLoading.value = true
  try {
    const format = type === 'excel' ? 'excel' : 'json'
    const url = `/api/data/batch/template?datasetId=${encodeURIComponent(
      form.datasetId
    )}&format=${encodeURIComponent(format)}`

    const resp = await fetch(url, {
      method: 'GET',
      headers: getAuthHeader(),
    })
    if (!resp.ok) throw new Error(`HTTP ${resp.status}`)

    const blob = await resp.blob()
    const cd = resp.headers.get('content-disposition') || ''
    const fallbackName = `template_${form.datasetId}.${format === 'excel' ? 'xlsx' : 'json'}`
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
  loadScienceCategoryOptions()
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
</style>

