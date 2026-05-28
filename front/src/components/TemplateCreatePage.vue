<template>
  <section class="tpl-page">
    <div class="tpl-breadcrumb">
      当前位置：<span class="crumb-main">首页</span> &gt;
      <span class="crumb-now">模板创建</span>
    </div>

    <div class="tpl-main">
      <!-- 步骤条 -->
      <div class="tpl-steps">
        <div class="step-item active">
          <div class="step-icon">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </div>
          <span class="step-text">基础设置</span>
        </div>
        <div class="step-line"></div>
        <div class="step-item">
          <div class="step-circle">2</div>
          <span class="step-text">模板设计</span>
        </div>
        <template v-if="currentType === 'dataset'">
          <div class="step-line"></div>
          <div class="step-item">
            <div class="step-circle">3</div>
            <span class="step-text">数据量规则配置</span>
          </div>
        </template>
      </div>

      <header class="tpl-header">
        <h1 class="tpl-title">创建模板</h1>
      </header>

      <section class="tpl-body">
        <!-- 操作标题 -->
        <div class="section-title">
          <span class="title-line"></span>
          <span class="title-text">请选择要进行的操作</span>
        </div>

        <!-- 提示信息 -->
        <div class="tpl-tip">
          <span class="tip-icon">
            <svg viewBox="0 0 16 16" width="14" height="14">
              <path fill="currentColor" d="M8 1a7 7 0 100 14A7 7 0 008 1zm0 11a1 1 0 110-2 1 1 0 010 2zm0-3a1 1 0 01-1-1V4a1 1 0 112 0v4a1 1 0 01-1 1z"/>
            </svg>
          </span>
          <span>建议优先完善已有模板再进行创建，避免重复。公开库需要审核通过后方可启用。</span>
        </div>

        <!-- 搜索栏 -->
        <div class="tpl-search-row">
          <input
            class="tpl-input search-input"
            placeholder="模板名称 / 模板片段名称"
          />
          <button class="tpl-search-btn">
            <span class="tpl-search-icon">
              <svg viewBox="0 0 16 16" width="14" height="14">
                <path fill="currentColor" d="M11.7422 10.3439C12.5329 9.2673 13 7.9382 13 6.5C13 2.91015 10.0899 0 6.5 0C2.91015 0 0 2.91015 0 6.5C0 10.0899 2.91015 13 6.5 13C7.9382 13 9.2673 12.5329 10.3439 11.7422L14.1464 15.5446C14.3417 15.7399 14.6583 15.7399 14.8536 15.5446C15.0488 15.3493 15.0488 15.0328 14.8536 14.8375L11.7422 10.3439ZM6.5 11.5C3.73858 11.5 1.5 9.26142 1.5 6.5C1.5 3.73858 3.73858 1.5 6.5 1.5C9.26142 1.5 11.5 3.73858 11.5 6.5C11.5 9.26142 9.26142 11.5 6.5 11.5Z"/>
              </svg>
            </span>
            <span>搜索</span>
          </button>
        </div>

        <!-- 模板类型选择 -->
        <div class="tpl-row type-row">
          <label class="tpl-label">选择创建模板类型：</label>
          <div class="tpl-type-tabs">
            <button
              class="tpl-type-btn"
              :class="{ active: currentType === 'dataset' }"
              @click="currentType = 'dataset'"
            >
              数据模板
            </button>
            <button
              class="tpl-type-btn"
              :class="{ active: currentType === 'fragment' }"
              @click="currentType = 'fragment'"
            >
              模板片段
            </button>
          </div>
        </div>

        <!-- 模板名称 -->
        <div class="tpl-row">
          <label class="tpl-label">
            <span class="required">*</span>
            模板名称：
          </label>
          <div class="tpl-field">
            <input
              v-model="form.moduleName"
              class="tpl-input"
              :placeholder="currentType === 'dataset' ? '请输入模板名称' : '请输入片段名称'"
            />
          </div>
        </div>

        <!-- 模板标签 - 仅在数据模板时显示 -->
        <div v-if="currentType === 'dataset'" class="tpl-row">
          <label class="tpl-label">
            <span class="required">*</span>
            模板标签：
          </label>
          <div class="tpl-field">
            <div class="tpl-select-wrapper">
              <select v-model="form.tag" class="tpl-select">
                <option value="" disabled>请选择模板标签</option>
                <option value="生物医用材料（科学）">生物医用材料（科学）</option>
                <option value="生物医用材料（产业）">生物医用材料（产业）</option>
              </select>
              <span class="select-arrow">
                <svg viewBox="0 0 12 12" width="10" height="10">
                  <path fill="currentColor" d="M6 8L1 3h10z"/>
                </svg>
              </span>
            </div>
          </div>
        </div>

        <!-- 模板说明 -->
        <div class="tpl-row">
          <label class="tpl-label">
            模板说明：
          </label>
          <div class="tpl-field">
            <textarea
              v-model="form.description"
              class="tpl-textarea"
              :placeholder="currentType === 'dataset' ? '请输入模板说明' : '请输入片段说明'"
            />
          </div>
        </div>

        <!-- 可见范围 -->
        <div class="tpl-row">
          <label class="tpl-label">
            <span class="required">*</span>
            可见范围：
          </label>
          <div class="tpl-field">
            <div class="tpl-select-wrapper">
              <select v-model="form.visibleArea" class="tpl-select">
                <option value="" disabled>请选择可见范围</option>
                <option value="0">私有模板</option>
                <option value="1">公共模板</option>
              </select>
              <span class="select-arrow">
                <svg viewBox="0 0 12 12" width="10" height="10">
                  <path fill="currentColor" d="M6 8L1 3h10z"/>
                </svg>
              </span>
            </div>
            <div class="tpl-hint">
              公共模板在审核通过之后将进入模板库，可供当前账号或其他账号使用；私有模板无需审核，仅支持当前账号使用
            </div>
          </div>
        </div>

        <!-- 是否同意上架 - 仅在数据模板时显示 -->
        <div v-if="currentType === 'dataset'" class="tpl-row">
          <label class="tpl-label multi-line">
            <span class="required">*</span>
            <span>是否同意发布至新材料<br>大数据中心：</span>
          </label>
          <div class="tpl-radio-group">
            <label class="radio-label">
              <input v-model="form.agree" type="radio" name="agree" value="1" />
              <span class="radio-text">同意</span>
            </label>
            <label class="radio-label">
              <input v-model="form.agree" type="radio" name="agree" value="0" />
              <span class="radio-text">不同意</span>
            </label>
          </div>
        </div>

        <footer class="tpl-footer">
          <button class="tpl-next-btn" :disabled="creating" @click="nextStep">
            {{ creating ? '提交中…' : '下一步' }}
          </button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script setup>
import { ref } from 'vue'
import { createModule } from '../api/module.js'

const currentType = ref('dataset')
const creating = ref(false)

const form = ref({
  moduleName: '',
  tag: '生物医用材料（科学）',
  description: '',
  visibleArea: '1', // 1: public, 0: private
  agree: '1', // 1: yes, 0: no
})

const emit = defineEmits(['next'])

function base64UrlDecode(input) {
  const s = String(input || '').replace(/-/g, '+').replace(/_/g, '/')
  const pad = s.length % 4 === 0 ? '' : '='.repeat(4 - (s.length % 4))
  const b64 = s + pad
  const binary = atob(b64)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}

function getCreatorForRequest() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token || token.split('.').length !== 3) return '2'
  try {
    const payloadText = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadText)
    const userId = payload?.sub != null ? String(payload.sub).trim() : ''
    return userId || '2'
  } catch {
    return '2'
  }
}

const nextStep = async () => {
  const moduleName = String(form.value.moduleName || '').trim()
  const tag = String(form.value.tag || '').trim()
  if (!moduleName) {
    alert('请输入模板名称')
    return
  }
  if (!tag) {
    alert('请选择模板标签')
    return
  }

  creating.value = true
  try {
    const creator = getCreatorForRequest()
    const resp = await createModule({
      moduleName,
      tag,
      description: String(form.value.description || '').trim(),
      creator,
      visibleArea: Number(form.value.visibleArea || 1),
      agree: currentType.value === 'dataset' ? Number(form.value.agree || 1) : 1,
    })
    const moduleId = Number(resp?.data)
    if (!Number.isFinite(moduleId) || moduleId <= 0) {
      throw new Error('创建模板成功，但未返回有效 moduleId')
    }

    emit('next', {
      templateType: currentType.value,
      moduleId,
    })
  } catch (e) {
    alert(e?.message || '创建模板失败')
  } finally {
    creating.value = false
  }
}
</script>

<style scoped>
.tpl-page {
  background: #f5f7fa;
  padding: 16px 0 40px;
  min-height: 100vh;
}

.tpl-breadcrumb {
  font-size: 13px;
  color: #666;
  margin-bottom: 16px;
  padding: 0 40px;
}

.crumb-main {
  color: #1a5ce6;
  cursor: pointer;
}

.crumb-now {
  color: #333;
}

.tpl-main {
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.08);
  padding: 24px 40px 32px;
  max-width: 1000px;
  margin: 0 auto;
}

/* 步骤条 */
.tpl-steps {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #999;
  font-size: 14px;
}

.step-item.active {
  color: #1a5ce6;
  font-weight: 500;
}

.step-icon {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  background: #1a5ce6;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-circle {
  width: 24px;
  height: 24px;
  border-radius: 50%;
  border: 2px solid #ddd;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 12px;
  color: #999;
}

.step-line {
  width: 60px;
  height: 1px;
  background: #e0e0e0;
}

.tpl-header {
  margin-bottom: 20px;
}

.tpl-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.tpl-body {
  padding-top: 8px;
}

/* 操作标题 */
.section-title {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-bottom: 16px;
}

.title-line {
  width: 4px;
  height: 16px;
  background: #1a5ce6;
  border-radius: 2px;
}

.title-text {
  font-size: 14px;
  font-weight: 500;
  color: #333;
}

/* 搜索栏 */
.tpl-search-row {
  display: flex;
  align-items: center;
  gap: 0;
  margin-bottom: 16px;
}

.search-input {
  flex: 1;
  height: 36px;
  border-radius: 4px 0 0 4px;
  border-right: none;
}

/* 提示信息 */
.tpl-tip {
  background: #fff8e6;
  border-radius: 4px;
  border: 1px solid #ffe2a4;
  padding: 10px 12px;
  font-size: 13px;
  color: #8b5a00;
  margin-bottom: 16px;
  display: flex;
  align-items: center;
  gap: 8px;
}

.tip-icon {
  color: #e6a23c;
  display: flex;
  align-items: center;
}

/* 表单行 */
.tpl-row {
  display: flex;
  align-items: flex-start;
  margin-bottom: 16px;
  font-size: 14px;
}

.tpl-row.type-row {
  align-items: center;
}

.tpl-label {
  width: 150px;
  flex-shrink: 0;
  text-align: right;
  margin-right: 16px;
  color: #333;
  padding-top: 8px;
}

.tpl-label.multi-line {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  padding-top: 0;
  line-height: 1.4;
}

.required {
  color: #ff4d4f;
  margin-right: 4px;
}

/* 模板类型标签 */
.tpl-type-tabs {
  display: inline-flex;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  overflow: hidden;
  background: #f5f7fa;
}

.tpl-type-btn {
  padding: 6px 20px;
  font-size: 14px;
  border: none;
  background: transparent;
  color: #555;
  cursor: pointer;
  transition: all 0.2s;
}

.tpl-type-btn.active {
  background: #1a5ce6;
  color: #fff;
}

.tpl-type-btn:hover:not(.active) {
  background: #e8eaf0;
}

/* 输入框 */
.tpl-field {
  flex: 1;
  display: flex;
  flex-direction: column;
  gap: 6px;
}

.tpl-input,
.tpl-textarea,
.tpl-select {
  border-radius: 4px;
  border: 1px solid #dcdfe6;
  padding: 8px 12px;
  font-size: 14px;
  box-sizing: border-box;
  transition: border-color 0.2s;
}

.tpl-input:focus,
.tpl-textarea:focus,
.tpl-select:focus {
  outline: none;
  border-color: #1a5ce6;
}

.tpl-input::placeholder,
.tpl-textarea::placeholder {
  color: #b0b6c6;
}

.tpl-textarea {
  min-height: 80px;
  resize: vertical;
  font-family: inherit;
}

/* 搜索按钮 */
.tpl-search-btn {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  padding: 8px 24px;
  border-radius: 0 4px 4px 0;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
  font-size: 14px;
  white-space: nowrap;
  cursor: pointer;
  transition: background 0.2s;
  height: 36px;
}

.tpl-search-btn:hover {
  background: #1246bb;
}

.tpl-search-icon {
  display: flex;
  align-items: center;
}

/* 下拉选择框 */
.tpl-select-wrapper {
  position: relative;
  width: 100%;
}

.tpl-select {
  width: 100%;
  height: 36px;
  appearance: none;
  padding-right: 32px;
  background: #fff;
  cursor: pointer;
  color: #999;
}

.tpl-select option:not(:first-child) {
  color: #333;
}

.select-arrow {
  position: absolute;
  right: 12px;
  top: 50%;
  transform: translateY(-50%);
  color: #999;
  font-size: 10px;
  pointer-events: none;
  display: flex;
  align-items: center;
}

/* 提示文字 */
.tpl-hint {
  font-size: 12px;
  color: #999;
  line-height: 1.5;
}

/* 单选按钮组 */
.tpl-radio-group {
  display: flex;
  align-items: center;
  gap: 24px;
  padding-top: 8px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
}

.radio-label input[type="radio"] {
  width: 14px;
  height: 14px;
  cursor: pointer;
  accent-color: #1a5ce6;
}

.radio-text {
  font-size: 14px;
  color: #333;
}

/* 底部按钮 */
.tpl-footer {
  margin-top: 32px;
  display: flex;
  justify-content: center;
}

.tpl-next-btn {
  min-width: 120px;
  padding: 10px 32px;
  border-radius: 4px;
  border: none;
  background: #1a5ce6;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  transition: background 0.2s;
}

.tpl-next-btn:hover {
  background: #1246bb;
}
</style>
