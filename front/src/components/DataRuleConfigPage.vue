<template>
  <section class="rule-page">
    <!-- 顶部步骤条 -->
    <div class="rule-header">
      <div class="header-left">
        <h1 class="page-title">创建模板 <span class="template-id">(123)</span></h1>
      </div>
      <div class="header-center">
        <div class="step-item done">
          <div class="step-icon">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </div>
          <span class="step-text">基础设置</span>
        </div>
        <div class="step-line done"></div>
        <div class="step-item done">
          <div class="step-icon">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </div>
          <span class="step-text">模板设计</span>
        </div>
        <div class="step-line done"></div>
        <div class="step-item active">
          <div class="step-icon">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </div>
          <span class="step-text">数据量规则配置</span>
        </div>
      </div>
      <div class="header-right">
        <button class="btn-secondary" @click="goBack">返回</button>
        <button class="btn-secondary" @click="preview">模拟</button>
        <button class="btn-primary" @click="createTemplate">创建</button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="rule-body">
      <!-- 左侧：模板大纲 -->
      <aside class="outline-panel">
        <div class="panel-header">
          <span>模板大纲</span>
          <button class="btn-icon">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/>
            </svg>
          </button>
        </div>
        <div class="outline-content">
          <div class="outline-section">
            <div class="section-title">
              <span class="toggle-icon">▼</span>
              <span>对象</span>
            </div>
            <div class="section-items">
              <div class="outline-item">
                <span class="item-dot"></span>
                <span class="item-name">3333</span>
                <span class="item-type">（字符串型）</span>
              </div>
            </div>
          </div>
          <div class="outline-section">
            <div class="section-title">
              <span class="toggle-icon">▼</span>
              <span>操作</span>
            </div>
          </div>
          <div class="outline-section">
            <div class="section-title">
              <span class="toggle-icon">▼</span>
              <span>结果</span>
            </div>
            <div class="section-items">
              <div class="outline-item active">
                <span class="item-dot"></span>
                <span class="item-name">3333</span>
                <span class="item-type">（字符串型）</span>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中间：规则配置区域 -->
      <main class="rule-config-area">
        <!-- 标题 -->
        <div class="config-header">
          <div class="config-title">
            <span class="title-line"></span>
            <span>模板规则配置</span>
            <span class="info-icon" title="配置数据量计算规则">
              <svg viewBox="0 0 16 16" width="14" height="14">
                <path fill="currentColor" d="M8 1a7 7 0 100 14A7 7 0 008 1zm0 11a1 1 0 110-2 1 1 0 010 2zm0-3a1 1 0 01-1-1V4a1 1 0 112 0v4a1 1 0 01-1 1z"/>
              </svg>
            </span>
          </div>
        </div>

        <!-- 整体计入选项 -->
        <div class="global-rule">
          <label class="checkbox-label">
            <input type="checkbox" v-model="countAsWhole" />
            <span>按整个模板计入</span>
            <span class="info-icon-small">
              <svg viewBox="0 0 16 16" width="12" height="12">
                <path fill="currentColor" d="M8 1a7 7 0 100 14A7 7 0 008 1zm0 11a1 1 0 110-2 1 1 0 010 2zm0-3a1 1 0 01-1-1V4a1 1 0 112 0v4a1 1 0 01-1 1z"/>
              </svg>
            </span>
          </label>
        </div>

        <!-- 区域规则配置 -->
        <div class="zone-rules">
          <!-- 对象区域 -->
          <div class="zone-rule-item">
            <div class="zone-header" @click="toggleZone('object')">
              <span class="zone-name">对象</span>
              <span class="toggle-arrow" :class="{ collapsed: !zones.object }">▼</span>
            </div>
            <div v-show="zones.object" class="zone-content">
              <div class="empty-zone">暂无数据</div>
            </div>
          </div>

          <!-- 操作区域 -->
          <div class="zone-rule-item">
            <div class="zone-header" @click="toggleZone('operation')">
              <span class="zone-name">操作</span>
              <span class="toggle-arrow" :class="{ collapsed: !zones.operation }">▼</span>
            </div>
            <div v-show="zones.operation" class="zone-content">
              <div class="empty-zone">暂无数据</div>
            </div>
          </div>

          <!-- 结果区域 -->
          <div class="zone-rule-item">
            <div class="zone-header" @click="toggleZone('result')">
              <span class="zone-name">结果</span>
              <span class="toggle-arrow" :class="{ collapsed: !zones.result }">▼</span>
            </div>
            <div v-show="zones.result" class="zone-content">
              <div class="field-list">
                <div class="field-item">
                  <div class="field-header">
                    <span class="required-mark">*</span>
                    <span class="field-name">3333</span>
                  </div>
                  <div class="field-input">
                    <input type="text" placeholder="请输入规则" />
                  </div>
                </div>
              </div>
            </div>
          </div>
        </div>

        <!-- 总计显示 -->
        <div class="total-count" v-if="showTotal">
          计 {{ totalCount }} 个
        </div>
      </main>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'

const emit = defineEmits(['back', 'create'])

// 区域展开状态
const zones = reactive({
  object: false,
  operation: false,
  result: true
})

// 是否按整个模板计入
const countAsWhole = ref(false)

// 是否显示总计
const showTotal = ref(true)
const totalCount = ref(1)

// 切换区域展开
const toggleZone = (zone) => {
  zones[zone] = !zones[zone]
}

// 返回
const goBack = () => {
  emit('back')
}

// 模拟
const preview = () => {
  alert('模拟功能')
}

// 创建模板
const createTemplate = () => {
  if (confirm('确定要创建模板吗？')) {
    emit('create')
  }
}
</script>

<style scoped>
.rule-page {
  min-height: 100vh;
  background: #f5f7fa;
  display: flex;
  flex-direction: column;
}

/* 顶部步骤条 */
.rule-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.header-left {
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.template-id {
  font-size: 14px;
  color: #999;
  font-weight: normal;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 8px;
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

.step-item.done {
  color: #1a5ce6;
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
  width: 40px;
  height: 1px;
  background: #e0e0e0;
}

.step-line.done {
  background: #1a5ce6;
}

.header-right {
  display: flex;
  gap: 8px;
}

.btn-secondary {
  padding: 6px 16px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.btn-primary {
  padding: 6px 16px;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-primary:hover {
  background: #1246bb;
}

/* 主体区域 */
.rule-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

/* 左侧大纲面板 */
.outline-panel {
  width: 220px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e8e8e8;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-weight: 500;
}

.btn-icon {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #666;
}

.btn-icon:hover {
  background: #f0f0f0;
}

.outline-content {
  flex: 1;
  padding: 8px;
  overflow-y: auto;
}

.outline-section {
  margin-bottom: 4px;
}

.section-title {
  padding: 8px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 14px;
  color: #333;
  border-radius: 4px;
}

.section-title:hover {
  background: #f5f5f5;
}

.toggle-icon {
  font-size: 10px;
  color: #999;
}

.section-items {
  padding-left: 32px;
}

.outline-item {
  padding: 6px 12px;
  cursor: pointer;
  display: flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  border-radius: 4px;
}

.outline-item:hover {
  background: #f0f5ff;
}

.outline-item.active {
  background: #e6f7ff;
  color: #1a5ce6;
}

.item-dot {
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #999;
}

.outline-item.active .item-dot {
  background: #1a5ce6;
}

.item-name {
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-type {
  color: #999;
  font-size: 12px;
}

/* 中间规则配置区域 */
.rule-config-area {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
  padding: 20px;
  overflow-y: auto;
}

.config-header {
  margin-bottom: 16px;
}

.config-title {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 16px;
  font-weight: 500;
  color: #333;
}

.title-line {
  width: 4px;
  height: 18px;
  background: #1a5ce6;
  border-radius: 2px;
}

.info-icon {
  color: #999;
  cursor: help;
  display: flex;
  align-items: center;
}

/* 全局规则 */
.global-rule {
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #f0f0f0;
}

.checkbox-label {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  font-size: 14px;
  color: #333;
}

.checkbox-label input[type="checkbox"] {
  width: 16px;
  height: 16px;
  cursor: pointer;
  accent-color: #1a5ce6;
}

.info-icon-small {
  color: #999;
  display: flex;
  align-items: center;
}

/* 区域规则 */
.zone-rules {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.zone-rule-item {
  border: 1px solid #e8e8e8;
  border-radius: 8px;
  overflow: hidden;
}

.zone-header {
  padding: 12px 16px;
  background: #f5f5f5;
  display: flex;
  align-items: center;
  justify-content: space-between;
  cursor: pointer;
  transition: background 0.2s;
}

.zone-header:hover {
  background: #e8e8e8;
}

.zone-name {
  font-size: 14px;
  color: #666;
}

.toggle-arrow {
  font-size: 12px;
  color: #999;
  transition: transform 0.2s;
}

.toggle-arrow.collapsed {
  transform: rotate(-90deg);
}

.zone-content {
  padding: 16px;
  background: #fff;
  min-height: 100px;
}

.empty-zone {
  text-align: center;
  color: #999;
  font-size: 14px;
  padding: 20px;
}

/* 字段列表 */
.field-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.field-item {
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px;
  background: #fafafa;
}

.field-header {
  display: flex;
  align-items: center;
  gap: 4px;
  margin-bottom: 8px;
}

.required-mark {
  color: #ff4d4f;
}

.field-name {
  font-size: 14px;
  color: #333;
}

.field-input input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  transition: border-color 0.2s;
}

.field-input input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.field-input input::placeholder {
  color: #999;
}

/* 总计 */
.total-count {
  text-align: center;
  color: #1a5ce6;
  font-size: 14px;
  margin-top: 20px;
  padding: 12px;
  background: #f0f5ff;
  border-radius: 4px;
}
</style>
