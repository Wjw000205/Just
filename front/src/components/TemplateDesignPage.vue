<template>
  <section class="design-page">
    <!-- 顶部步骤条 -->
    <div class="design-header">
      <div class="header-left">
        <h1 class="page-title">
          创建模板 <span class="template-id">({{ moduleId }})</span>
        </h1>
      </div>
      <div class="header-center">
        <div class="step-item done">
          <div class="step-icon">
            <svg viewBox="0 0 24 24" width="14" height="14">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </div>
          <span class="step-text">基础设置</span>
        </div>
        <div class="step-line done"></div>
        <div class="step-item active">
          <div class="step-icon">
            <svg viewBox="0 0 24 24" width="14" height="14">
              <path fill="currentColor" d="M9 16.17L4.83 12l-1.42 1.41L9 19 21 7l-1.41-1.41z"/>
            </svg>
          </div>
          <span class="step-text">模板设计</span>
        </div>
        <!-- 第三步（数据量规则配置）已暂时取消 -->
      </div>
      <div class="header-right">
        <button class="btn-secondary" @click="goBack">返回</button>
        <button class="btn-secondary" @click="parseJSON">JSON解析</button>
        <button class="btn-secondary" @click="clearAll">清空</button>
        <button class="btn-secondary" @click="importData">导入</button>
        <button class="btn-secondary" @click="preview">预览</button>
        <button class="btn-secondary" @click="saveDraft">暂存</button>
        <button class="btn-primary" @click="nextStep">
          完成
        </button>
      </div>
    </div>

    <!-- 主体区域 -->
    <div class="design-body">
      <!-- 左侧：模板大纲 -->
      <aside class="outline-panel">
        <div class="panel-header">
          <span class="panel-title">模板大纲</span>
          <button class="btn-icon" @click="toggleOutline">
            <svg viewBox="0 0 24 24" width="16" height="16">
              <path fill="currentColor" d="M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z"/>
            </svg>
          </button>
        </div>
        <div class="outline-content">
          <div class="outline-tree">
            <!-- 对象 -->
            <div class="tree-node">
              <div class="node-header" @click="toggleSection('object')">
                <span class="expand-icon" :class="{ collapsed: !sections.object }">
                  <svg viewBox="0 0 24 24" width="12" height="12">
                    <path fill="currentColor" d="M7 10l5 5 5-5z"/>
                  </svg>
                </span>
                <span class="node-label">对象</span>
              </div>
              <div v-show="sections.object" class="node-children">
                <div 
                  v-for="(item, index) in objectComponents" 
                  :key="item.id"
                  class="tree-item"
                  :class="{ active: selectedComponent?.id === item.id }"
                  @click="selectComponent(item)"
                >
                  <span class="item-icon">
                    <svg v-if="item.type === 'string'" viewBox="0 0 24 24" width="12" height="12">
                      <rect x="4" y="6" width="16" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="2"/>
                    </svg>
                    <svg v-else-if="item.type === 'number'" viewBox="0 0 24 24" width="12" height="12">
                      <text x="12" y="17" text-anchor="middle" fill="currentColor" font-size="14" font-weight="bold">123</text>
                    </svg>
                    <svg v-else viewBox="0 0 24 24" width="12" height="12">
                      <rect x="4" y="6" width="16" height="12" rx="2" fill="none" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </span>
                  <span class="item-name">{{ item.name || '未命名' }}</span>
                  <span class="item-type">({{ getTypeLabel(item.type) }})</span>
                </div>
              </div>
            </div>
            <!-- 操作 -->
            <div class="tree-node">
              <div class="node-header" @click="toggleSection('operation')">
                <span class="expand-icon" :class="{ collapsed: !sections.operation }">
                  <svg viewBox="0 0 24 24" width="12" height="12">
                    <path fill="currentColor" d="M7 10l5 5 5-5z"/>
                  </svg>
                </span>
                <span class="node-label">操作</span>
              </div>
              <div v-show="sections.operation" class="node-children">
                <div 
                  v-for="(item, index) in operationComponents" 
                  :key="item.id"
                  class="tree-item"
                  :class="{ active: selectedComponent?.id === item.id }"
                  @click="selectComponent(item)"
                >
                  <span class="item-icon">
                    <svg viewBox="0 0 24 24" width="12" height="12">
                      <text x="12" y="17" text-anchor="middle" fill="currentColor" font-size="14" font-weight="bold">123</text>
                    </svg>
                  </span>
                  <span class="item-name">{{ item.name || '未命名' }}</span>
                  <span class="item-type">({{ getTypeLabel(item.type) }})</span>
                </div>
              </div>
            </div>
            <!-- 结果 -->
            <div class="tree-node">
              <div class="node-header" @click="toggleSection('result')">
                <span class="expand-icon" :class="{ collapsed: !sections.result }">
                  <svg viewBox="0 0 24 24" width="12" height="12">
                    <path fill="currentColor" d="M7 10l5 5 5-5z"/>
                  </svg>
                </span>
                <span class="node-label">结果</span>
              </div>
              <div v-show="sections.result" class="node-children">
                <div 
                  v-for="(item, index) in resultComponents" 
                  :key="item.id"
                  class="tree-item"
                  :class="{ active: selectedComponent?.id === item.id }"
                  @click="selectComponent(item)"
                >
                  <span class="item-icon">
                    <svg viewBox="0 0 24 24" width="12" height="12">
                      <rect x="4" y="8" width="16" height="8" rx="2" fill="none" stroke="currentColor" stroke-width="2"/>
                    </svg>
                  </span>
                  <span class="item-name">{{ item.name || '未命名' }}</span>
                  <span class="item-type">({{ getTypeLabel(item.type) }})</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </aside>

      <!-- 中间：设计区域 -->
      <main class="design-area">
        <!-- 组件类型工具栏 -->
        <div class="component-toolbar">
          <div 
            v-for="type in componentTypes" 
            :key="type.value"
            class="component-type"
            draggable="true"
            @dragstart="handleDragStart($event, type)"
            @dragend="handleDragEnd"
          >
            <div class="type-icon-wrapper">
              <div class="type-icon" :style="{ background: type.color }">
                <svg v-if="type.icon" viewBox="0 0 24 24" width="20" height="20">
                  <path fill="currentColor" :d="type.icon"/>
                </svg>
              </div>
              <span v-if="type.count > 0" class="type-badge">{{ type.count }}</span>
            </div>
            <span class="type-name">{{ type.label }}</span>
          </div>
        </div>

        <!-- 区域头部 -->
        <div class="area-header">
          <div class="area-title">
            <span class="title-indicator"></span>
            <span class="title-text">模板设计</span>
          </div>
          <div class="area-actions">
            <span class="action-label">开启模板片段推荐</span>
            <label class="switch">
              <input type="checkbox" v-model="enableRecommendation">
              <span class="slider"></span>
            </label>
            <button class="btn-text" @click="showFieldSettings">字段设置</button>
            <button class="btn-icon-action" @click="toggleFullscreen">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path fill="currentColor" d="M7 14H5v5h5v-2H7v-3zm-2-4h2V7h3V5H5v5zm12 7h-3v2h5v-5h-2v3zM14 5v2h3v3h2V5h-5z"/>
              </svg>
            </button>
          </div>
        </div>

        <!-- 可滚动的内容区 -->
        <div class="design-content">
          <!-- 拖拽区域 -->
          <div class="drop-areas">
            <!-- 对象区域 -->
            <div
              class="drop-area"
              :class="{ 'drag-over': dragOverZone === 'object', 'has-error': hasError('object') }"
              @dragover.prevent="handleDragOver($event, 'object')"
              @dragleave="handleDragLeave"
              @drop="handleDrop($event, 'object')"
            >
              <div class="area-label">对象</div>
              <div class="area-content">
                <template v-if="objectComponents.length > 0">
                  <div 
                    v-for="(component, index) in objectComponents" 
                    :key="component.id"
                    class="dropped-component"
                    :class="{ 
                      'selected': selectedComponent?.id === component.id,
                      'has-error': hasComponentError(component.id)
                    }"
                    :id="`component-${component.id}`"
                    @click="selectComponent(component)"
                  >
                    <div class="component-header">
                      <span class="component-type-tag" :style="{ background: getComponentColor(component.type), color: getComponentTextColor(component.type) }">
                        {{ getTypeLabel(component.type) }}
                      </span>
                      <span class="component-name">{{ component.name || '未命名' }}</span>
                      <div class="component-actions">
                        <button class="btn-icon-small" @click.stop="copyComponent(component)" title="复制">
                          <svg viewBox="0 0 24 24" width="14" height="14">
                            <path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                          </svg>
                        </button>
                        <button class="btn-icon-small" @click.stop="deleteComponent('object', index)" title="删除">
                          <svg viewBox="0 0 24 24" width="14" height="14">
                            <path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                          </svg>
                        </button>
                      </div>
                    </div>
                    <div class="component-preview">
                      <input 
                        v-if="component.type === 'string'" 
                        type="text" 
                        class="preview-input" 
                        :placeholder="component.placeholder || '请输入'"
                        disabled
                      />
                      <div v-else-if="component.type === 'range'" class="preview-range">
                        <input type="text" class="preview-input-small" placeholder="最小值" disabled />
                        <span class="range-separator">~</span>
                        <input type="text" class="preview-input-small" placeholder="最大值" disabled />
                        <span class="range-unit">{{ component.unit || '单位' }}</span>
                      </div>
                      <div v-else-if="component.type === 'number'" class="preview-number">
                        <input type="number" class="preview-input" :placeholder="component.placeholder || '请输入数值'" disabled />
                        <span class="number-unit">{{ component.unit || '单位' }}</span>
                      </div>
                    </div>
                  </div>
                </template>
                <div v-else class="empty-hint">
                  <span class="add-icon">+</span>
                  <span>将控件拖入此区域</span>
                </div>
              </div>
            </div>

            <!-- 操作区域 -->
            <div 
              class="drop-area"
              :class="{ 'drag-over': dragOverZone === 'operation', 'has-error': hasError('operation') }"
              @dragover.prevent="handleDragOver($event, 'operation')"
              @dragleave="handleDragLeave"
              @drop="handleDrop($event, 'operation')"
            >
              <div class="area-label">操作</div>
              <div class="area-content">
                <template v-if="operationComponents.length > 0">
                  <div 
                    v-for="(component, index) in operationComponents" 
                    :key="component.id"
                    class="dropped-component"
                    :class="{ 
                      'selected': selectedComponent?.id === component.id,
                      'has-error': hasComponentError(component.id)
                    }"
                    :id="'component-' + component.id"
                    @click="selectComponent(component)"
                  >
                    <div class="component-header">
                      <span class="component-type-tag" :style="{ background: getComponentColor(component.type), color: getComponentTextColor(component.type) }">
                        {{ getTypeLabel(component.type) }}
                      </span>
                      <span class="component-name">{{ component.name || '未命名' }}</span>
                      <div class="component-actions">
                        <button class="btn-icon-small" @click.stop="copyComponent(component)" title="复制">
                          <svg viewBox="0 0 24 24" width="14" height="14">
                            <path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                          </svg>
                        </button>
                        <button class="btn-icon-small" @click.stop="deleteComponent('operation', index)" title="删除">
                          <svg viewBox="0 0 24 24" width="14" height="14">
                            <path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                          </svg>
                        </button>
                      </div>
                    </div>
                  </div>
                </template>
                <div v-else class="empty-hint">
                  <span class="add-icon">+</span>
                  <span>将控件拖入此区域</span>
                </div>
              </div>
            </div>

            <!-- 结果区域 -->
            <div 
              class="drop-area"
              :class="{ 'drag-over': dragOverZone === 'result', 'has-error': hasError('result') }"
              @dragover.prevent="handleDragOver($event, 'result')"
              @dragleave="handleDragLeave"
              @drop="handleDrop($event, 'result')"
            >
              <div class="area-label">结果</div>
              <div class="area-content">
                <template v-if="resultComponents.length > 0">
                  <div 
                    v-for="(component, index) in resultComponents" 
                    :key="component.id"
                    class="dropped-component"
                    :class="{ 
                      'selected': selectedComponent?.id === component.id,
                      'has-error': hasComponentError(component.id)
                    }"
                    :id="`component-${component.id}`"
                    @click="selectComponent(component)"
                  >
                    <div class="component-header">
                      <span class="component-type-tag" :style="{ background: getComponentColor(component.type), color: getComponentTextColor(component.type) }">
                        {{ getTypeLabel(component.type) }}
                      </span>
                      <span class="component-name">{{ component.name || '未命名' }}</span>
                      <div class="component-actions">
                        <button class="btn-icon-small" @click.stop="copyComponent(component)" title="复制">
                          <svg viewBox="0 0 24 24" width="14" height="14">
                            <path fill="currentColor" d="M16 1H4c-1.1 0-2 .9-2 2v14h2V3h12V1zm3 4H8c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h11c1.1 0 2-.9 2-2V7c0-1.1-.9-2-2-2zm0 16H8V7h11v14z"/>
                          </svg>
                        </button>
                        <button class="btn-icon-small" @click.stop="deleteComponent('result', index)" title="删除">
                          <svg viewBox="0 0 24 24" width="14" height="14">
                            <path fill="currentColor" d="M6 19c0 1.1.9 2 2 2h8c1.1 0 2-.9 2-2V7H6v12zM19 4h-3.5l-1-1h-5l-1 1H5v2h14V4z"/>
                          </svg>
                        </button>
                      </div>
                    </div>
                  </div>
                </template>
                <div v-else class="empty-hint">
                  <span class="add-icon">+</span>
                  <span>将控件拖入此区域</span>
                </div>
              </div>
            </div>
          </div>
        </div>
      </main>

      <!-- 右侧：字段设置 + 模板片段库 -->
      <aside class="right-panel">
        <!-- 字段设置 -->
        <div v-if="selectedComponent" class="field-settings">
          <div class="panel-header">
            <span>字段设置</span>
            <button class="btn-icon" @click="closeFieldSettings">
              <svg viewBox="0 0 24 24" width="16" height="16">
                <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
              </svg>
            </button>
          </div>
          <div class="settings-content">
            <div class="setting-group">
              <label>字段类型</label>
              <div class="setting-value">{{ getTypeLabel(selectedComponent.type) }}</div>
            </div>
            <div class="setting-group">
              <label><span class="required">*</span>字段名称</label>
              <input 
                type="text" 
                v-model="selectedComponent.name"
                class="setting-input"
                placeholder="请输入字段名称"
                :class="{ 'has-error': hasComponentError(selectedComponent.id, 'name') }"
              />
            </div>
            <div class="setting-group">
              <label>字段描述</label>
              <textarea 
                v-model="selectedComponent.description"
                class="setting-textarea"
                placeholder="请输入字段描述"
                rows="3"
              />
            </div>
            <div class="setting-group">
              <label>单位</label>
              <input 
                type="text" 
                v-model="selectedComponent.unit"
                class="setting-input"
                placeholder="请输入单位"
              />
            </div>
            <div class="setting-group" v-if="selectedComponent.type === 'number' || selectedComponent.type === 'range'">
              <label>类型</label>
              <div class="radio-group">
                <label class="radio-label">
                  <input type="radio" v-model="selectedComponent.rangeType" value="range" />
                  <span>范围</span>
                </label>
                <label class="radio-label">
                  <input type="radio" v-model="selectedComponent.rangeType" value="value" />
                  <span>误差</span>
                </label>
              </div>
            </div>
            <div class="setting-group" v-if="selectedComponent.type === 'number'">
              <label>小数位数</label>
              <input 
                type="number" 
                v-model="selectedComponent.decimal"
                class="setting-input"
                placeholder="请输入小数位数"
                min="0"
              />
            </div>
          </div>
        </div>

        <!-- 模板片段库 -->
        <div class="fragment-library">
          <div class="library-tabs">
            <button 
              :class="{ active: activeTab === 'library' }"
              @click="activeTab = 'library'"
            >
              模板片段库
            </button>
            <button 
              :class="{ active: activeTab === 'recommend' }"
              @click="activeTab = 'recommend'"
            >
              模板片段推荐
            </button>
          </div>
          <div class="library-search">
            <input type="text" placeholder="请输入模板片段名称" />
            <button class="btn-search">搜索</button>
          </div>
          <div class="library-content">
            <div v-if="activeTab === 'library'" class="library-list">
              <div class="library-item">
                <div class="item-header">
                  <span class="item-name">表征结果模板</span>
                  <span class="item-badge">1</span>
                </div>
              </div>
              <div class="library-item">
                <div class="item-header">
                  <span class="item-name">表征模板</span>
                  <span class="item-badge">1</span>
                </div>
              </div>
              <div class="library-item expanded">
                <div class="item-header">
                  <span class="item-name">设备名称/设备厂家/设备型号</span>
                  <span class="item-badge">1</span>
                </div>
              </div>
            </div>
            <div v-else class="recommend-list">
              <div class="recommend-empty">暂无数据</div>
            </div>
          </div>
          <div class="preview-section">
            <div class="preview-title">模板片段（预览）</div>
            <div class="preview-empty">暂无数据</div>
          </div>
        </div>
      </aside>
    </div>

    <!-- 错误弹窗 -->
    <div v-if="showErrorModal" class="error-modal-overlay" @click="closeErrorModal">
      <div class="error-modal" @click.stop>
        <div class="modal-header">
          <h3>表单验证错误 (共 {{ validationErrors.length }} 个错误)</h3>
          <button class="btn-close" @click="closeErrorModal">
            <svg viewBox="0 0 24 24" width="20" height="20">
              <path fill="currentColor" d="M19 6.41L17.59 5 12 10.59 6.41 5 5 6.41 10.59 12 5 17.59 6.41 19 12 13.41 17.59 19 19 17.59 13.41 12z"/>
            </svg>
          </button>
        </div>
        <div class="modal-body">
          <div class="error-stats">
            <div class="stats-title">错误统计：</div>
            <div class="stats-content">
              <span v-if="errorStats.emptyName > 0">表单项名称为空: {{ errorStats.emptyName }}</span>
              <span v-if="errorStats.duplicateName > 0">表单项名称重复: {{ errorStats.duplicateName }}</span>
              <span v-if="errorStats.emptyZone > 0">区域组件为空: {{ errorStats.emptyZone }}</span>
            </div>
          </div>
          <div class="error-list">
            <div 
              v-for="(error, index) in validationErrors" 
              :key="index"
              class="error-item"
              :class="{ 'has-location': error.componentId }"
              @click="locateError(error)"
            >
              <div class="error-icon" v-if="index === 0">
                <svg viewBox="0 0 24 24" width="20" height="20">
                  <path fill="#ff4d4f" d="M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm1 15h-2v-2h2v2zm0-4h-2V7h2v6z"/>
                </svg>
              </div>
              <div class="error-content">
                <div class="error-title">{{ index + 1 }}. {{ error.title }}</div>
                <div v-if="error.componentId" class="error-hint">点击此处定位到组件</div>
              </div>
            </div>
          </div>
        </div>
        <div class="modal-footer">
          <button class="btn-secondary" @click="locateFirstError">定位首个错误</button>
          <button class="btn-primary" @click="closeErrorModal">确定</button>
        </div>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive, computed } from 'vue'
import { designModule } from '../api/module.js'

const props = defineProps({
  templateType: {
    type: String,
    default: 'dataset' // 'dataset' 或 'fragment'
  },
  moduleId: {
    type: Number,
    default: 123,
  },
})

// 组件类型定义 - 根据参考图片更新颜色和图标
const componentTypes = [
  { value: 'string', label: '字符串型', color: '#e8f4fd', icon: 'M4 6h16v2H4zm0 5h16v2H4zm0 5h16v2H4z', count: 0 },
  { value: 'number', label: '数值型', color: '#fff7e6', icon: 'M7 15h2v-2H7v2zm0-8h2V5H7v2zm4 8h2v-2h-2v2zm0-8h2V5h-2v2zm4 8h2v-2h-2v2zm0-8h2V5h-2v2z', count: 0 },
  { value: 'range', label: '范围型', color: '#f6ffed', icon: 'M9 11H7v2h2v-2zm4 0h-2v2h2v-2zm4 0h-2v2h2v-2zM3 5h18v2H3V5zm0 12h18v2H3v-2z', count: 0 },
  { value: 'select', label: '候选型', color: '#fff0f6', icon: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm-2 15l-5-5 1.41-1.41L10 14.17l7.59-7.59L19 8l-9 9z', count: 0 },
  { value: 'image', label: '图片型', color: '#f9f0ff', icon: 'M21 19V5c0-1.1-.9-2-2-2H5c-1.1 0-2 .9-2 2v14c0 1.1.9 2 2 2h14c1.1 0 2-.9 2-2zM8.5 13.5l2.5 3.01L14.5 12l4.5 6H5l3.5-4.5z', count: 0 },
  { value: 'file', label: '文件型', color: '#fff2e8', icon: 'M14 2H6c-1.1 0-1.99.9-1.99 2L4 20c0 1.1.89 2 1.99 2H18c1.1 0 2-.9 2-2V8l-6-6zm2 16H8v-2h8v2zm0-4H8v-2h8v2zm-3-5V3.5L18.5 9H13z', count: 0 },
  { value: 'array', label: '数组型', color: '#e6fffb', icon: 'M4 6h4v2H4zm0 5h4v2H4zm0 5h4v2H4zm6-10h10v2H10zm0 5h10v2H10zm0 5h10v2H10z', count: 0 },
  { value: 'table', label: '表格型', color: '#f0f5ff', icon: 'M3 3h18v18H3V3zm16 16V5H5v14h14zM7 7h4v4H7V7zm0 6h4v4H7v-4zm6-6h4v4h-4V7zm0 6h4v4h-4v-4z', count: 0 },
  { value: 'container', label: '容器型', color: '#fff2f0', icon: 'M3 5h18v2H3zm0 6h18v2H3zm0 6h18v2H3z', count: 0 },
  { value: 'generator', label: '生成器型', color: '#fffbe6', icon: 'M12 2C6.48 2 2 6.48 2 12s4.48 10 10 10 10-4.48 10-10S17.52 2 12 2zm5 11h-4v4h-2v-4H7v-2h4V7h2v4h4v2z', count: 0 },
]

// 组件类型文字颜色映射
const componentTextColors = {
  string: '#1890ff',
  number: '#faad14',
  range: '#52c41a',
  select: '#eb2f96',
  image: '#722ed1',
  file: '#fa8c16',
  array: '#13c2c2',
  table: '#2f54eb',
  container: '#f5222d',
  generator: '#faad14'
}

// 区域展开状态
const sections = reactive({
  object: true,
  operation: true,
  result: true
})

// 组件数据
const objectComponents = ref([])
const operationComponents = ref([])
const resultComponents = ref([])

// 拖拽状态
const dragOverZone = ref(null)
const draggedType = ref(null)

// 选中组件
const selectedComponent = ref(null)

// 推荐开关
const enableRecommendation = ref(false)

// 标签页
const activeTab = ref('library')

// 错误弹窗
const showErrorModal = ref(false)
const validationErrors = ref([])

// 错误统计
const errorStats = computed(() => {
  const stats = { emptyName: 0, duplicateName: 0, emptyZone: 0 }
  validationErrors.value.forEach(error => {
    if (error.type === 'emptyName') stats.emptyName++
    if (error.type === 'duplicateName') stats.duplicateName++
    if (error.type === 'emptyZone') stats.emptyZone++
  })
  return stats
})

// 切换区域展开
const toggleSection = (section) => {
  sections[section] = !sections[section]
}

// 获取类型标签
const getTypeLabel = (type) => {
  const found = componentTypes.find(t => t.value === type)
  return found ? found.label : type
}

// 获取组件颜色
const getComponentColor = (type) => {
  const found = componentTypes.find(t => t.value === type)
  return found ? found.color : '#e8f4fd'
}

// 获取组件文字颜色
const getComponentTextColor = (type) => {
  return componentTextColors[type] || '#333'
}

// 生成唯一ID
const generateId = () => {
  return Date.now().toString(36) + Math.random().toString(36).substr(2)
}

// 拖拽开始
const handleDragStart = (e, type) => {
  draggedType.value = type
  e.dataTransfer.effectAllowed = 'copy'
  e.dataTransfer.setData('componentType', type.value)
}

// 拖拽结束
const handleDragEnd = () => {
  draggedType.value = null
  dragOverZone.value = null
}

// 拖拽悬停
const handleDragOver = (e, zone) => {
  e.preventDefault()
  dragOverZone.value = zone
}

// 拖拽离开
const handleDragLeave = () => {
  dragOverZone.value = null
}

// 放置组件
const handleDrop = (e, zone) => {
  e.preventDefault()
  const type = e.dataTransfer.getData('componentType')
  if (!type) return

  const newComponent = {
    id: generateId(),
    type: type,
    name: '',
    description: '',
    unit: '',
    placeholder: '',
    rangeType: 'range',
    decimal: 2
  }

  if (zone === 'object') {
    objectComponents.value.push(newComponent)
  } else if (zone === 'operation') {
    operationComponents.value.push(newComponent)
  } else if (zone === 'result') {
    resultComponents.value.push(newComponent)
  }

  // 自动选中新添加的组件
  selectedComponent.value = newComponent
  dragOverZone.value = null
}

// 选择组件
const selectComponent = (component) => {
  selectedComponent.value = component
}

// 关闭字段设置
const closeFieldSettings = () => {
  selectedComponent.value = null
}

// 删除组件
const deleteComponent = (zone, index) => {
  if (zone === 'object') {
    const deleted = objectComponents.value.splice(index, 1)[0]
    if (selectedComponent.value?.id === deleted.id) {
      selectedComponent.value = null
    }
  } else if (zone === 'operation') {
    const deleted = operationComponents.value.splice(index, 1)[0]
    if (selectedComponent.value?.id === deleted.id) {
      selectedComponent.value = null
    }
  } else if (zone === 'result') {
    const deleted = resultComponents.value.splice(index, 1)[0]
    if (selectedComponent.value?.id === deleted.id) {
      selectedComponent.value = null
    }
  }
}

// 复制组件
const copyComponent = (component) => {
  const newComponent = {
    ...component,
    id: generateId(),
    name: component.name + '_复制'
  }
  
  // 根据当前选中的组件找到所在区域并添加
  if (objectComponents.value.find(c => c.id === component.id)) {
    objectComponents.value.push(newComponent)
  } else if (operationComponents.value.find(c => c.id === component.id)) {
    operationComponents.value.push(newComponent)
  } else if (resultComponents.value.find(c => c.id === component.id)) {
    resultComponents.value.push(newComponent)
  }
  
  selectedComponent.value = newComponent
}

// 检查区域是否有错误
const hasError = (zone) => {
  return validationErrors.value.some(e => e.zone === zone && e.type === 'emptyZone')
}

// 检查组件是否有错误
const hasComponentError = (componentId, field = null) => {
  return validationErrors.value.some(e => {
    if (field) {
      return e.componentId === componentId && e.field === field
    }
    return e.componentId === componentId
  })
}

// 验证表单
const validateForm = () => {
  const errors = []
  const allNames = new Set()
  const duplicateNames = new Set()

  // 检查对象区域
  if (objectComponents.value.length === 0) {
    errors.push({
      type: 'emptyZone',
      zone: 'object',
      title: '对象区域必须添加至少一个组件'
    })
  }
  
  // 检查操作区域
  if (operationComponents.value.length === 0) {
    errors.push({
      type: 'emptyZone',
      zone: 'operation',
      title: '操作区域必须添加至少一个组件'
    })
  }
  
  // 检查结果区域
  if (resultComponents.value.length === 0) {
    errors.push({
      type: 'emptyZone',
      zone: 'result',
      title: '结果区域必须添加至少一个组件'
    })
  }

  // 检查所有组件
  const checkComponents = (components, zoneName) => {
    components.forEach(comp => {
      // 检查名称是否为空
      if (!comp.name || comp.name.trim() === '') {
        errors.push({
          type: 'emptyName',
          zone: zoneName,
          componentId: comp.id,
          field: 'name',
          title: `${getTypeLabel(comp.type)}组件：请填写名称`
        })
      } else {
        // 检查名称是否重复
        if (allNames.has(comp.name)) {
          duplicateNames.add(comp.name)
          errors.push({
            type: 'duplicateName',
            zone: zoneName,
            componentId: comp.id,
            field: 'name',
            title: `${getTypeLabel(comp.type)}组件：名称存在重复值 "${comp.name}"`
          })
        } else {
          allNames.add(comp.name)
        }
      }
    })
  }

  checkComponents(objectComponents.value, 'object')
  checkComponents(operationComponents.value, 'operation')
  checkComponents(resultComponents.value, 'result')

  // 如果有重复名称，更新所有重复项的错误信息
  if (duplicateNames.size > 0) {
    errors.forEach(error => {
      if (error.type === 'duplicateName' && duplicateNames.has(error.title.match(/"([^"]+)"/)?.[1])) {
        // 确保重复错误被正确标记
      }
    })
  }

  return errors
}

// 定义事件
const emit = defineEmits(['back', 'next', 'submit'])

// 下一步（保存模板设计）
const nextStep = async () => {
  const errors = validateForm()
  if (errors.length > 0) {
    validationErrors.value = errors
    showErrorModal.value = true
  } else {
    if (!props.moduleId) {
      alert('模板id缺失，无法保存设计')
      return
    }

    const componentTypeToColumnType = (t) => {
      // 后端 DatasetColumnEntity 示例：varchar / int
      if (t === 'number' || t === 'range') return 'int'
      return 'varchar'
    }

    const toColumnItems = (list) =>
      (list || []).map((c) => ({
        columnName: String(c.name || '').trim(),
        type: componentTypeToColumnType(c.type),
      }))

    const payload = {
      moduleId: props.moduleId,
      object: toColumnItems(objectComponents.value),
      operation: toColumnItems(operationComponents.value),
      result: toColumnItems(resultComponents.value),
    }

    try {
      await designModule(payload)
    } catch (e) {
      alert(e?.message || '保存模板设计失败')
      return
    }

    // 到此即完成：由父组件弹出成功提示并引导去查看
    emit('submit', { moduleId: props.moduleId })
  }
}

// 定位到错误组件
const locateError = (error) => {
  if (error.componentId) {
    // 展开对应区域
    sections[error.zone] = true
    
    // 选中组件
    const allComponents = [...objectComponents.value, ...operationComponents.value, ...resultComponents.value]
    const component = allComponents.find(c => c.id === error.componentId)
    if (component) {
      selectedComponent.value = component
      
      // 滚动到组件
      setTimeout(() => {
        const element = document.getElementById(`component-${error.componentId}`)
        if (element) {
          element.scrollIntoView({ behavior: 'smooth', block: 'center' })
          element.classList.add('highlight-error')
          setTimeout(() => {
            element.classList.remove('highlight-error')
          }, 2000)
        }
      }, 100)
    }
  } else if (error.zone) {
    // 如果是区域错误，滚动到区域
    sections[error.zone] = true
  }
  
  closeErrorModal()
}

// 定位首个错误
const locateFirstError = () => {
  if (validationErrors.value.length > 0) {
    locateError(validationErrors.value[0])
  }
}

// 关闭错误弹窗
const closeErrorModal = () => {
  showErrorModal.value = false
}

// 其他功能按钮
const goBack = () => {
  if (confirm('确定要返回吗？未保存的更改将丢失。')) {
    emit('back')
  }
}

const parseJSON = () => {
  alert('JSON解析功能')
}

const clearAll = () => {
  if (confirm('确定要清空所有组件吗？')) {
    objectComponents.value = []
    operationComponents.value = []
    resultComponents.value = []
    selectedComponent.value = null
  }
}

const importData = () => {
  alert('导入功能')
}

const preview = () => {
  alert('预览功能')
}

const saveDraft = () => {
  alert('暂存成功')
}

const toggleOutline = () => {
  // 切换大纲显示
}

const showFieldSettings = () => {
  // 显示字段设置
}

const toggleFullscreen = () => {
  // 全屏切换
}
</script>

<style scoped>
.design-page {
  min-height: 100vh;
  background: #f0f2f5;
  display: flex;
  flex-direction: column;
  overflow-x: hidden;
}

/* 顶部步骤条 */
.design-header {
  background: #fff;
  border-bottom: 1px solid #e8e8e8;
  padding: 12px 24px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  flex-shrink: 0;
}

.header-left {
  flex-shrink: 0;
}

.page-title {
  font-size: 18px;
  font-weight: 700;
  color: #1a1a1a;
}

.template-id {
  font-size: 14px;
  color: #666;
  font-weight: normal;
  margin-left: 4px;
}

.header-center {
  display: flex;
  align-items: center;
  gap: 12px;
}

.step-item {
  display: flex;
  align-items: center;
  gap: 6px;
  color: #666;
  font-size: 14px;
  font-weight: 500;
}

.step-item.active {
  color: #1890ff;
  font-weight: 600;
}

.step-item.done {
  color: #1890ff;
  font-weight: 600;
}

.step-icon {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  background: #1890ff;
  color: #fff;
  display: flex;
  align-items: center;
  justify-content: center;
}

.step-circle {
  width: 22px;
  height: 22px;
  border-radius: 50%;
  border: 2px solid #d9d9d9;
  display: flex;
  align-items: center;
  justify-content: center;
  font-size: 11px;
  color: #999;
  font-weight: 500;
}

.step-line {
  width: 40px;
  height: 1px;
  background: #e8e8e8;
}

.step-line.done {
  background: #1890ff;
}

.header-right {
  display: flex;
  gap: 8px;
}

.btn-secondary {
  padding: 8px 18px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-secondary:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.btn-primary {
  padding: 8px 22px;
  border: none;
  background: #1890ff;
  color: #fff;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 600;
  cursor: pointer;
  transition: background 0.2s;
}

.btn-primary:hover {
  background: #096dd9;
}

/* 主体区域 */
.design-body {
  flex: 1;
  display: flex;
  overflow: hidden;
  padding: 16px;
  gap: 16px;
}

/* 左侧面板 */
.outline-panel {
  width: 220px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.panel-header {
  padding: 12px 16px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.panel-title {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
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
  color: #999;
  transition: all 0.2s;
}

.btn-icon:hover {
  background: #f5f5f5;
  color: #666;
}

.outline-content {
  flex: 1;
  overflow-y: auto;
  padding: 8px;
}

/* 树形结构 */
.outline-tree {
  padding: 4px;
}

.tree-node {
  margin-bottom: 2px;
}

.node-header {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.node-header:hover {
  background: #f5f5f5;
}

.expand-icon {
  display: flex;
  align-items: center;
  color: #999;
  transition: transform 0.2s;
}

.expand-icon.collapsed {
  transform: rotate(-90deg);
}

.node-label {
  font-size: 14px;
  color: #1a1a1a;
  font-weight: 600;
}

.node-children {
  padding-left: 16px;
}

.tree-item {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 10px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  margin-bottom: 2px;
}

.tree-item:hover {
  background: #e6f7ff;
}

.tree-item.active {
  background: #e6f7ff;
}

.item-icon {
  display: flex;
  align-items: center;
  color: #1890ff;
}

.item-name {
  font-size: 13px;
  color: #262626;
  font-weight: 500;
  flex: 1;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.item-type {
  font-size: 12px;
  color: #8c8c8c;
}

/* 中间设计区域 */
.design-area {
  flex: 1;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

/* 组件工具栏 */
.component-toolbar {
  flex-shrink: 0;
  background: #fff;
  padding: 16px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  gap: 24px;
  overflow-x: auto;
}

.component-type {
  display: flex;
  flex-direction: column;
  align-items: center;
  gap: 8px;
  cursor: move;
  padding: 4px;
  border-radius: 6px;
  transition: all 0.2s;
  min-width: 56px;
}

.component-type:hover {
  background: #f5f5f5;
  transform: translateY(-2px);
}

.type-icon-wrapper {
  position: relative;
}

.type-icon {
  width: 36px;
  height: 36px;
  border-radius: 8px;
  display: flex;
  align-items: center;
  justify-content: center;
  color: #333;
}

.type-badge {
  position: absolute;
  top: -6px;
  right: -6px;
  min-width: 16px;
  height: 16px;
  border-radius: 8px;
  background: #faad14;
  color: #fff;
  font-size: 10px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 4px;
  font-weight: 500;
}

.type-name {
  font-size: 13px;
  color: #595959;
  font-weight: 500;
  white-space: nowrap;
}

/* 区域头部 */
.area-header {
  flex-shrink: 0;
  background: #fff;
  padding: 12px 24px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.area-title {
  display: flex;
  align-items: center;
  gap: 8px;
}

.title-indicator {
  width: 3px;
  height: 16px;
  background: #1890ff;
  border-radius: 2px;
}

.title-text {
  font-size: 15px;
  font-weight: 600;
  color: #1a1a1a;
}

.area-actions {
  display: flex;
  align-items: center;
  gap: 16px;
}

.action-label {
  font-size: 14px;
  color: #595959;
  font-weight: 500;
}

.switch {
  position: relative;
  width: 36px;
  height: 18px;
}

.switch input {
  opacity: 0;
  width: 0;
  height: 0;
}

.slider {
  position: absolute;
  cursor: pointer;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background-color: #ccc;
  border-radius: 18px;
  transition: 0.3s;
}

.slider:before {
  position: absolute;
  content: "";
  height: 14px;
  width: 14px;
  left: 2px;
  bottom: 2px;
  background-color: white;
  border-radius: 50%;
  transition: 0.3s;
}

input:checked + .slider {
  background-color: #1890ff;
}

input:checked + .slider:before {
  transform: translateX(18px);
}

.btn-text {
  border: none;
  background: transparent;
  color: #1890ff;
  cursor: pointer;
  font-size: 14px;
  font-weight: 500;
  padding: 4px 8px;
  border-radius: 4px;
  transition: background 0.2s;
}

.btn-text:hover {
  background: #e6f7ff;
}

.btn-icon-action {
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #999;
  transition: all 0.2s;
}

.btn-icon-action:hover {
  background: #f5f5f5;
  color: #666;
}

/* 中间内容区 */
.design-content {
  flex: 1;
  overflow-y: auto;
  padding: 16px 24px;
  background: #fafafa;
}

.drop-areas {
  display: flex;
  flex-direction: column;
  gap: 16px;
}

.drop-area {
  background: #fff;
  border: 2px dashed #d9d9d9;
  border-radius: 8px;
  min-height: 120px;
  transition: all 0.2s;
  position: relative;
  overflow: hidden;
}

.drop-area:hover {
  border-color: #bfbfbf;
}

.drop-area.drag-over {
  border-color: #1890ff;
  background: #e6f7ff;
}

.drop-area.has-error {
  border-color: #ff4d4f;
}

.area-label {
  position: absolute;
  top: 0;
  left: 0;
  padding: 6px 14px;
  background: #1890ff;
  color: #fff;
  font-size: 13px;
  font-weight: 600;
  border-radius: 0 0 8px 0;
}

.area-content {
  padding: 40px 20px 20px;
}

.empty-hint {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #1890ff;
  font-size: 15px;
  font-weight: 500;
  min-height: 60px;
}

.add-icon {
  font-size: 20px;
  font-weight: 400;
}

.dropped-component {
  background: #fff;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 12px;
  cursor: pointer;
  transition: all 0.2s;
}

.dropped-component:hover {
  border-color: #1890ff;
  box-shadow: 0 2px 8px rgba(24, 144, 255, 0.1);
}

.dropped-component.selected {
  border-color: #1890ff;
  box-shadow: 0 0 0 2px rgba(24, 144, 255, 0.15);
}

.dropped-component.has-error {
  border-color: #ff4d4f;
  background: #fff2f0;
}

.dropped-component.highlight-error {
  animation: shake 0.5s;
  border-color: #ff4d4f;
  box-shadow: 0 0 0 3px rgba(255, 77, 79, 0.3);
}

@keyframes shake {
  0%, 100% { transform: translateX(0); }
  25% { transform: translateX(-5px); }
  75% { transform: translateX(5px); }
}

.component-header {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 8px;
}

.component-type-tag {
  padding: 3px 10px;
  border-radius: 4px;
  font-size: 13px;
  font-weight: 600;
}

.component-name {
  flex: 1;
  font-weight: 600;
  color: #262626;
  font-size: 15px;
}

.component-actions {
  display: flex;
  gap: 4px;
  opacity: 0;
  transition: opacity 0.2s;
}

.dropped-component:hover .component-actions {
  opacity: 1;
}

.btn-icon-small {
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  display: flex;
  align-items: center;
  justify-content: center;
  border-radius: 4px;
  color: #999;
  transition: all 0.2s;
}

.btn-icon-small:hover {
  background: #f0f0f0;
  color: #666;
}

.component-preview {
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 4px;
}

.preview-input {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  background: #fff;
  color: #8c8c8c;
}

.preview-input:disabled {
  background: #f5f5f5;
}

.preview-range {
  display: flex;
  align-items: center;
  gap: 8px;
}

.preview-input-small {
  width: 80px;
  padding: 10px 10px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
}

.range-separator {
  color: #999;
}

.range-unit,
.number-unit {
  font-size: 13px;
  color: #8c8c8c;
  font-weight: 500;
}

.preview-number {
  display: flex;
  align-items: center;
  gap: 8px;
}

/* 右侧面板 */
.right-panel {
  width: 300px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 8px rgba(0, 0, 0, 0.06);
  display: flex;
  flex-direction: column;
  flex-shrink: 0;
  overflow: hidden;
}

.field-settings {
  border-bottom: 1px solid #f0f0f0;
}

.settings-content {
  padding: 16px;
}

.setting-group {
  margin-bottom: 16px;
}

.setting-group label {
  display: block;
  margin-bottom: 8px;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.setting-value {
  padding: 10px 12px;
  background: #f5f5f5;
  border-radius: 4px;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.setting-input,
.setting-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  color: #262626;
  transition: all 0.2s;
  box-sizing: border-box;
}

.setting-input:focus,
.setting-textarea:focus {
  outline: none;
  border-color: #1890ff;
}

.setting-input.has-error,
.setting-textarea.has-error {
  border-color: #ff4d4f;
  background: #fff2f0;
}

.setting-textarea {
  resize: vertical;
  font-family: inherit;
  min-height: 60px;
}

.required {
  color: #ff4d4f;
  margin-right: 4px;
}

.radio-group {
  display: flex;
  gap: 16px;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.radio-label input {
  accent-color: #1890ff;
}

/* 模板片段库 */
.fragment-library {
  flex: 1;
  display: flex;
  flex-direction: column;
  overflow: hidden;
}

.library-tabs {
  display: flex;
  border-bottom: 1px solid #f0f0f0;
}

.library-tabs button {
  flex: 1;
  padding: 12px;
  border: none;
  background: transparent;
  cursor: pointer;
  font-size: 14px;
  color: #595959;
  font-weight: 500;
  border-bottom: 2px solid transparent;
  transition: all 0.2s;
}

.library-tabs button:hover {
  color: #1890ff;
}

.library-tabs button.active {
  color: #1890ff;
  border-bottom-color: #1890ff;
  font-weight: 600;
}

.library-search {
  padding: 12px;
  display: flex;
  gap: 8px;
}

.library-search input {
  flex: 1;
  padding: 8px 12px;
  border: 1px solid #d9d9d9;
  border-radius: 4px;
  font-size: 14px;
  color: #262626;
}

.library-search input:focus {
  outline: none;
  border-color: #1890ff;
}

.btn-search {
  padding: 8px 14px;
  border: 1px solid #d9d9d9;
  background: #fff;
  border-radius: 4px;
  font-size: 14px;
  font-weight: 500;
  color: #595959;
  cursor: pointer;
  transition: all 0.2s;
}

.btn-search:hover {
  border-color: #1890ff;
  color: #1890ff;
}

.library-content {
  flex: 1;
  overflow-y: auto;
  padding: 0 12px;
}

.library-item {
  padding: 10px 12px;
  border: 1px solid #e8e8e8;
  border-radius: 6px;
  margin-bottom: 8px;
  cursor: pointer;
  transition: all 0.2s;
}

.library-item:hover {
  border-color: #1890ff;
}

.library-item.expanded {
  border-color: #1890ff;
  background: #e6f7ff;
}

.item-header {
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.item-name {
  font-size: 14px;
  color: #262626;
  font-weight: 500;
}

.item-badge {
  min-width: 18px;
  height: 18px;
  border-radius: 9px;
  background: #1890ff;
  color: #fff;
  font-size: 11px;
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 0 5px;
  font-weight: 500;
}

.recommend-empty,
.preview-empty {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.preview-section {
  border-top: 1px solid #f0f0f0;
  padding: 12px;
}

.preview-title {
  font-size: 14px;
  color: #595959;
  margin-bottom: 12px;
  font-weight: 600;
}

/* 错误弹窗 */
.error-modal-overlay {
  position: fixed;
  top: 0;
  left: 0;
  right: 0;
  bottom: 0;
  background: rgba(0, 0, 0, 0.5);
  display: flex;
  align-items: center;
  justify-content: center;
  z-index: 1000;
}

.error-modal {
  background: #fff;
  border-radius: 8px;
  width: 520px;
  max-height: 80vh;
  display: flex;
  flex-direction: column;
  box-shadow: 0 4px 20px rgba(0, 0, 0, 0.15);
}

.modal-header {
  padding: 16px 20px;
  border-bottom: 1px solid #f0f0f0;
  display: flex;
  align-items: center;
  justify-content: space-between;
}

.modal-header h3 {
  font-size: 16px;
  font-weight: 500;
  color: #333;
  margin: 0;
}

.btn-close {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #999;
  padding: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  transition: color 0.2s;
}

.btn-close:hover {
  color: #666;
}

.modal-body {
  padding: 20px;
  overflow-y: auto;
  flex: 1;
}

.error-stats {
  background: #fff2f0;
  border-radius: 6px;
  padding: 12px 16px;
  margin-bottom: 16px;
  border-left: 4px solid #ff4d4f;
}

.stats-title {
  font-weight: 500;
  color: #333;
  margin-bottom: 8px;
}

.stats-content {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  font-size: 13px;
  color: #666;
}

.stats-content span {
  color: #ff4d4f;
}

.error-list {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.error-item {
  display: flex;
  gap: 12px;
  padding: 12px 16px;
  background: #fff2f0;
  border: 1px solid #ffccc7;
  border-radius: 6px;
  border-left: 4px solid #ff4d4f;
}

.error-item.has-location {
  cursor: pointer;
}

.error-item.has-location:hover {
  background: #ffe5e5;
}

.error-icon {
  flex-shrink: 0;
  display: flex;
  align-items: center;
}

.error-content {
  flex: 1;
}

.error-title {
  color: #333;
  font-size: 14px;
  margin-bottom: 4px;
}

.error-hint {
  color: #999;
  font-size: 12px;
}

.modal-footer {
  padding: 16px 20px;
  border-top: 1px solid #f0f0f0;
  display: flex;
  justify-content: flex-end;
  gap: 12px;
}
</style>