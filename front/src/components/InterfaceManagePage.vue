<template>
  <section class="interface-manage-page">
    <!-- 面包屑导航 -->
    <div class="page-header">
      <div class="breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-main" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-now">界面管理</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="page-content">
      <!-- 页面标题 -->
      <div class="page-title">界面管理</div>

      <!-- LOGO配置区域 -->
      <div class="config-section">
        <!-- 顶部主LOGO -->
        <div class="logo-config-item">
          <label class="config-label">顶部主LOGO</label>
          <div class="logo-upload-area">
            <div class="logo-preview-box">
              <img v-if="formData.topMainLogo" :src="formData.topMainLogo" class="logo-preview-img" alt="顶部主LOGO" />
              <div v-else class="logo-placeholder">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="M21 15l-5-5L5 21" />
                </svg>
              </div>
            </div>
            <div class="logo-upload-actions">
              <input
                type="file"
                ref="topMainLogoInput"
                accept="image/*"
                style="display: none"
                @change="handleLogoUpload('topMainLogo', $event)"
              />
              <button class="upload-btn" @click="triggerUpload('topMainLogo')">
                {{ formData.topMainLogo ? '重新上传' : '上传' }}
              </button>
            </div>
          </div>
          <div class="logo-rules">
            <p>1.当顶部只添加一个LOGO时，图片高度不超过60px，宽度不超过700px</p>
            <p>2.当顶部添加两个LOGO时，高度不超过60px时，宽度相加不超过680px</p>
            <p>3.支持大小在300kb以内的图片</p>
          </div>
        </div>

        <!-- 顶部副LOGO -->
        <div class="logo-config-item">
          <label class="config-label">顶部副LOGO</label>
          <div class="logo-upload-area">
            <div class="logo-preview-box multi-logo">
              <template v-if="formData.topSubLogos.length > 0">
                <img
                  v-for="(logo, index) in formData.topSubLogos"
                  :key="index"
                  :src="logo"
                  class="logo-preview-img sub-logo"
                  alt="顶部副LOGO"
                />
              </template>
              <div v-else class="logo-placeholder">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="M21 15l-5-5L5 21" />
                </svg>
              </div>
            </div>
            <div class="logo-upload-actions">
              <input
                type="file"
                ref="topSubLogoInput"
                accept="image/*"
                style="display: none"
                @change="handleSubLogoUpload($event)"
              />
              <button class="upload-btn" @click="triggerUpload('topSubLogo')">
                {{ formData.topSubLogos.length > 0 ? '重新上传' : '上传' }}
              </button>
              <button v-if="formData.topSubLogos.length > 0" class="clear-btn" @click="clearSubLogos">
                清空
              </button>
            </div>
          </div>
          <div class="logo-rules">
            <p>1.当顶部只添加一个LOGO时，图片高度不超过60px，宽度不超过700px</p>
            <p>2.当顶部添加两个LOGO时，高度不超过60px时，宽度相加不超过680px</p>
            <p>3.支持大小在300kb以内的图片</p>
          </div>
        </div>

        <!-- 底部LOGO -->
        <div class="logo-config-item">
          <label class="config-label">底部LOGO</label>
          <div class="logo-upload-area">
            <div class="logo-preview-box">
              <img v-if="formData.bottomMainLogo" :src="formData.bottomMainLogo" class="logo-preview-img" alt="底部LOGO" />
              <div v-else class="logo-placeholder">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="M21 15l-5-5L5 21" />
                </svg>
              </div>
            </div>
            <div class="logo-upload-actions">
              <input
                type="file"
                ref="bottomMainLogoInput"
                accept="image/*"
                style="display: none"
                @change="handleLogoUpload('bottomMainLogo', $event)"
              />
              <button class="upload-btn" @click="triggerUpload('bottomMainLogo')">
                {{ formData.bottomMainLogo ? '重新上传' : '上传' }}
              </button>
            </div>
          </div>
          <div class="logo-rules">
            <p>1.当顶部只添加一个LOGO时，图片高度不超过60px，宽度不超过700px</p>
            <p>2.当顶部添加两个LOGO时，高度不超过60px时，宽度相加不超过680px</p>
            <p>3.支持大小在300kb以内的图片</p>
          </div>
        </div>

        <!-- 底部副LOGO -->
        <div class="logo-config-item">
          <label class="config-label">底部副LOGO</label>
          <div class="logo-upload-area">
            <div class="logo-preview-box multi-logo">
              <template v-if="formData.bottomSubLogos.length > 0">
                <img
                  v-for="(logo, index) in formData.bottomSubLogos"
                  :key="index"
                  :src="logo"
                  class="logo-preview-img sub-logo"
                  alt="底部副LOGO"
                />
              </template>
              <div v-else class="logo-placeholder">
                <svg width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#ccc" stroke-width="1.5">
                  <rect x="3" y="3" width="18" height="18" rx="2" />
                  <circle cx="8.5" cy="8.5" r="1.5" />
                  <path d="M21 15l-5-5L5 21" />
                </svg>
              </div>
            </div>
            <div class="logo-upload-actions">
              <input
                type="file"
                ref="bottomSubLogoInput"
                accept="image/*"
                style="display: none"
                @change="handleBottomSubLogoUpload($event)"
              />
              <button class="upload-btn" @click="triggerUpload('bottomSubLogo')">
                {{ formData.bottomSubLogos.length > 0 ? '重新上传' : '上传' }}
              </button>
              <button v-if="formData.bottomSubLogos.length > 0" class="clear-btn" @click="clearBottomSubLogos">
                清空
              </button>
            </div>
          </div>
          <div class="logo-rules">
            <p>1.当顶部只添加一个LOGO时，图片高度不超过60px，宽度不超过700px</p>
            <p>2.当顶部添加两个LOGO时，高度不超过60px时，宽度相加不超过680px</p>
            <p>3.支持大小在300kb以内的图片</p>
          </div>
        </div>
      </div>

      <!-- 系统配置表单 -->
      <div class="config-form">
        <!-- 系统名称 -->
        <div class="form-item">
          <label class="form-label">系统名称</label>
          <div class="form-content">
            <textarea
              v-model="formData.systemName"
              class="form-textarea"
              rows="2"
              placeholder="请输入系统名称"
              maxlength="15"
            ></textarea>
            <span class="char-count">{{ formData.systemName.length }}/15</span>
          </div>
          <div class="form-visibility">
            <label class="radio-label">
              <input type="radio" v-model="formData.systemNameVisible" :value="true" />
              <span class="radio-text">显示</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="formData.systemNameVisible" :value="false" />
              <span class="radio-text">隐藏</span>
            </label>
          </div>
        </div>

        <!-- 访问量 -->
        <div class="form-item inline-item">
          <label class="form-label">访问量</label>
          <div class="form-visibility">
            <label class="radio-label">
              <input type="radio" v-model="formData.visitCountVisible" :value="true" />
              <span class="radio-text">显示</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="formData.visitCountVisible" :value="false" />
              <span class="radio-text">隐藏</span>
            </label>
          </div>
        </div>

        <!-- 反馈意见 -->
        <div class="form-item inline-item">
          <label class="form-label">反馈意见</label>
          <div class="form-input-wrap">
            <input
              v-model="formData.feedbackEmail"
              type="text"
              class="form-input"
              placeholder="请输入反馈邮箱"
            />
          </div>
          <div class="form-visibility">
            <label class="radio-label">
              <input type="radio" v-model="formData.feedbackVisible" :value="true" />
              <span class="radio-text">显示</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="formData.feedbackVisible" :value="false" />
              <span class="radio-text">隐藏</span>
            </label>
          </div>
        </div>

        <!-- 备案号 -->
        <div class="form-item inline-item">
          <label class="form-label">备案号</label>
          <div class="form-input-group">
            <input
              v-model="formData.icpNumber"
              type="text"
              class="form-input"
              placeholder="请输入备案号"
            />
            <input
              v-model="formData.icpUrl"
              type="text"
              class="form-input"
              placeholder="请输入URL"
            />
          </div>
          <div class="form-visibility">
            <label class="radio-label">
              <input type="radio" v-model="formData.icpVisible" :value="true" />
              <span class="radio-text">显示</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="formData.icpVisible" :value="false" />
              <span class="radio-text">隐藏</span>
            </label>
          </div>
        </div>

        <!-- 公网安备 -->
        <div class="form-item inline-item">
          <label class="form-label">公网安备</label>
          <div class="form-input-group">
            <input
              v-model="formData.policeNumber"
              type="text"
              class="form-input"
              placeholder="请输入公网安备号"
            />
            <input
              v-model="formData.policeUrl"
              type="text"
              class="form-input"
              placeholder="请输入URL"
            />
          </div>
          <div class="form-visibility">
            <label class="radio-label">
              <input type="radio" v-model="formData.policeVisible" :value="true" />
              <span class="radio-text">显示</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="formData.policeVisible" :value="false" />
              <span class="radio-text">隐藏</span>
            </label>
          </div>
        </div>

        <!-- 版权所有 -->
        <div class="form-item inline-item">
          <label class="form-label">版权所有</label>
          <div class="form-input-wrap">
            <input
              v-model="formData.copyright"
              type="text"
              class="form-input"
              placeholder="请输入版权信息"
            />
          </div>
          <div class="form-visibility">
            <label class="radio-label">
              <input type="radio" v-model="formData.copyrightVisible" :value="true" />
              <span class="radio-text">显示</span>
            </label>
            <label class="radio-label">
              <input type="radio" v-model="formData.copyrightVisible" :value="false" />
              <span class="radio-text">隐藏</span>
            </label>
          </div>
        </div>

        <!-- 网站标题 -->
        <div class="form-item inline-item">
          <label class="form-label required">网站标题</label>
          <div class="form-input-wrap">
            <input
              v-model="formData.siteTitle"
              type="text"
              class="form-input"
              placeholder="请输入网站标题"
            />
          </div>
        </div>

        <!-- 操作手册 -->
        <div class="form-item file-item">
          <label class="form-label">操作手册</label>
          <div class="file-upload-area">
            <input
              type="file"
              ref="manualInput"
              accept=".pdf"
              style="display: none"
              @change="handleFileUpload('manual', $event)"
            />
            <button class="file-select-btn" @click="triggerFileUpload('manual')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              选择操作手册
            </button>
            <p class="file-hint">支持 PDF 格式，文件大小不超过 500MB</p>
            <div v-if="formData.manualFile" class="file-item-info">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2">
                <path d="M14 2H6a2 2 0 0 0-2 2v16a2 2 0 0 0 2 2h12a2 2 0 0 0 2-2V8z" />
                <polyline points="14 2 14 8 20 8" />
              </svg>
              <span class="file-name">{{ formData.manualFile.name }}</span>
              <button class="file-remove-btn" @click="removeFile('manual')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
              </button>
            </div>
          </div>
        </div>

        <!-- 操作视频 -->
        <div class="form-item file-item">
          <label class="form-label">操作视频</label>
          <div class="file-upload-area">
            <input
              type="file"
              ref="videoInput"
              accept="video/*"
              style="display: none"
              @change="handleFileUpload('video', $event)"
            />
            <button class="file-select-btn" @click="triggerFileUpload('video')">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4" />
                <polyline points="17 8 12 3 7 8" />
                <line x1="12" y1="3" x2="12" y2="15" />
              </svg>
              选择操作视频
            </button>
            <p class="file-hint">支持 MP4、WEBM 格式，文件大小不超过 500MB</p>
            <div v-if="formData.videoFile" class="file-item-info">
              <svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="#67c23a" stroke-width="2">
                <rect x="2" y="2" width="20" height="20" rx="2.18" ry="2.18" />
                <line x1="7" y1="2" x2="7" y2="22" />
                <line x1="17" y1="2" x2="17" y2="22" />
                <line x1="2" y1="12" x2="22" y2="12" />
                <polyline points="7 12 12 7 17 12" />
              </svg>
              <span class="file-name">{{ formData.videoFile.name }}</span>
              <button class="file-remove-btn" @click="removeFile('video')">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="#f56c6c" stroke-width="2">
                  <circle cx="12" cy="12" r="10" />
                  <line x1="15" y1="9" x2="9" y2="15" />
                  <line x1="9" y1="9" x2="15" y2="15" />
                </svg>
              </button>
            </div>
          </div>
        </div>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-area">
        <button class="submit-btn" @click="handleSubmit">提交</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, reactive } from 'vue'

const emit = defineEmits(['go-home', 'go-system-manage'])

// 表单数据
const formData = reactive({
  // LOGO
  topMainLogo: '',
  topSubLogos: [],
  bottomMainLogo: '',
  bottomSubLogos: [],
  // 系统配置
  systemName: '新一代生物医用材料数据资源节点',
  systemNameVisible: true,
  visitCountVisible: true,
  feedbackEmail: '@ustb.edu.cn',
  feedbackVisible: true,
  icpNumber: '',
  icpUrl: '',
  icpVisible: true,
  policeNumber: '',
  policeUrl: '',
  policeVisible: true,
  copyright: '新材料大数据中心',
  copyrightVisible: true,
  siteTitle: '生物医用材料数据资源节点',
  // 文件
  manualFile: null,
  videoFile: null,
})

// refs
const topMainLogoInput = ref(null)
const topSubLogoInput = ref(null)
const bottomMainLogoInput = ref(null)
const bottomSubLogoInput = ref(null)
const manualInput = ref(null)
const videoInput = ref(null)

// 触发上传
const triggerUpload = (type) => {
  switch (type) {
    case 'topMainLogo':
      topMainLogoInput.value?.click()
      break
    case 'topSubLogo':
      topSubLogoInput.value?.click()
      break
    case 'bottomMainLogo':
      bottomMainLogoInput.value?.click()
      break
    case 'bottomSubLogo':
      bottomSubLogoInput.value?.click()
      break
  }
}

// 处理LOGO上传
const handleLogoUpload = (field, event) => {
  const file = event.target.files[0]
  if (!file) return

  if (file.size > 300 * 1024) {
    alert('图片大小不能超过300KB')
    return
  }

  const reader = new FileReader()
  reader.onload = (e) => {
    formData[field] = e.target.result
  }
  reader.readAsDataURL(file)
}

// 处理顶部副LOGO上传（支持多选）
const handleSubLogoUpload = (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  const validFiles = files.filter(file => file.size <= 300 * 1024)
  if (validFiles.length < files.length) {
    alert('部分图片超过300KB，已被过滤')
  }

  validFiles.forEach(file => {
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.topSubLogos.push(e.target.result)
    }
    reader.readAsDataURL(file)
  })
}

// 清空顶部副LOGO
const clearSubLogos = () => {
  formData.topSubLogos = []
}

// 处理底部副LOGO上传
const handleBottomSubLogoUpload = (event) => {
  const files = Array.from(event.target.files)
  if (files.length === 0) return

  const validFiles = files.filter(file => file.size <= 300 * 1024)
  if (validFiles.length < files.length) {
    alert('部分图片超过300KB，已被过滤')
  }

  validFiles.forEach(file => {
    const reader = new FileReader()
    reader.onload = (e) => {
      formData.bottomSubLogos.push(e.target.result)
    }
    reader.readAsDataURL(file)
  })
}

// 清空底部副LOGO
const clearBottomSubLogos = () => {
  formData.bottomSubLogos = []
}

// 触发文件上传
const triggerFileUpload = (type) => {
  if (type === 'manual') {
    manualInput.value?.click()
  } else if (type === 'video') {
    videoInput.value?.click()
  }
}

// 处理文件上传
const handleFileUpload = (type, event) => {
  const file = event.target.files[0]
  if (!file) return

  const maxSize = 500 * 1024 * 1024 // 500MB
  if (file.size > maxSize) {
    alert('文件大小不能超过500MB')
    return
  }

  if (type === 'manual') {
    if (file.type !== 'application/pdf') {
      alert('请上传PDF格式的文件')
      return
    }
    formData.manualFile = file
  } else if (type === 'video') {
    const validTypes = ['video/mp4', 'video/webm']
    if (!validTypes.includes(file.type)) {
      alert('请上传MP4或WEBM格式的视频')
      return
    }
    formData.videoFile = file
  }
}

// 删除文件
const removeFile = (type) => {
  if (type === 'manual') {
    formData.manualFile = null
    if (manualInput.value) manualInput.value.value = ''
  } else if (type === 'video') {
    formData.videoFile = null
    if (videoInput.value) videoInput.value.value = ''
  }
}

// 提交
const handleSubmit = () => {
  // 验证必填项
  if (!formData.siteTitle.trim()) {
    alert('请填写网站标题')
    return
  }

  console.log('提交的界面配置：', formData)
  alert('提交成功！')
}

const goHome = () => {
  emit('go-home')
}

const goSystemManage = () => {
  emit('go-system-manage')
}
</script>

<style scoped>
.interface-manage-page {
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
  margin-bottom: 24px;
  padding-left: 10px;
  border-left: 4px solid #1a5ce6;
}

/* LOGO配置区域 */
.config-section {
  display: flex;
  flex-direction: column;
  gap: 24px;
  margin-bottom: 32px;
}

.logo-config-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
  padding: 16px;
  background: #fafbfc;
  border-radius: 6px;
}

.config-label {
  width: 90px;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
  padding-top: 8px;
}

.logo-upload-area {
  display: flex;
  flex-direction: column;
  gap: 12px;
  flex: 1;
  max-width: 400px;
}

.logo-preview-box {
  width: 100%;
  min-height: 120px;
  background: #fff;
  border: 1px dashed #d4dae6;
  border-radius: 6px;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.logo-preview-box.multi-logo {
  flex-wrap: wrap;
  gap: 12px;
  padding: 16px;
}

.logo-placeholder {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  height: 120px;
}

.logo-preview-img {
  max-width: 100%;
  max-height: 100px;
  object-fit: contain;
}

.logo-preview-img.sub-logo {
  max-height: 60px;
}

.logo-upload-actions {
  display: flex;
  gap: 10px;
}

.upload-btn {
  padding: 6px 16px;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.upload-btn:hover {
  background: #1246bb;
  border-color: #1246bb;
}

.clear-btn {
  padding: 6px 16px;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.clear-btn:hover {
  border-color: #f56c6c;
  color: #f56c6c;
}

.logo-rules {
  flex: 1;
  font-size: 12px;
  color: #666;
  line-height: 1.8;
}

.logo-rules p {
  margin: 0;
}

/* 表单区域 */
.config-form {
  display: flex;
  flex-direction: column;
  gap: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8ecf4;
}

.form-item {
  display: flex;
  align-items: flex-start;
  gap: 16px;
}

.form-item.inline-item {
  align-items: center;
}

.form-label {
  width: 90px;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
  padding-top: 8px;
}

.form-label.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.form-content {
  position: relative;
  flex: 1;
  max-width: 400px;
}

.form-textarea {
  width: 100%;
  padding: 10px 12px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
  resize: vertical;
  font-family: inherit;
}

.form-textarea:focus {
  outline: none;
  border-color: #1a5ce6;
}

.char-count {
  position: absolute;
  right: 10px;
  bottom: 8px;
  font-size: 12px;
  color: #999;
}

.form-input-wrap {
  flex: 1;
  max-width: 400px;
}

.form-input {
  width: 100%;
  padding: 8px 12px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
}

.form-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.form-input-group {
  display: flex;
  gap: 12px;
  flex: 1;
  max-width: 400px;
}

.form-input-group .form-input {
  flex: 1;
}

.form-visibility {
  display: flex;
  gap: 20px;
  align-items: center;
}

.radio-label {
  display: flex;
  align-items: center;
  gap: 6px;
  cursor: pointer;
  font-size: 13px;
  color: #333;
}

.radio-label input[type="radio"] {
  width: 14px;
  height: 14px;
  accent-color: #1a5ce6;
  cursor: pointer;
}

.radio-text {
  color: #333;
}

/* 文件上传 */
.file-upload-area {
  flex: 1;
  max-width: 400px;
}

.file-select-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border: 1px solid #1a5ce6;
  background: #e8f4ff;
  color: #1a5ce6;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.file-select-btn:hover {
  background: #d1e9ff;
}

.file-select-btn svg {
  display: block;
}

.file-hint {
  margin: 8px 0 0;
  font-size: 12px;
  color: #666;
}

.file-item-info {
  display: flex;
  align-items: center;
  gap: 8px;
  margin-top: 12px;
  padding: 8px 12px;
  background: #f5f7fb;
  border-radius: 4px;
}

.file-name {
  flex: 1;
  font-size: 13px;
  color: #333;
}

.file-remove-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 24px;
  height: 24px;
  border: none;
  background: transparent;
  cursor: pointer;
  padding: 0;
}

.file-remove-btn:hover svg {
  stroke: #c45656;
}

/* 提交按钮 */
.submit-area {
  display: flex;
  justify-content: center;
  margin-top: 32px;
  padding-top: 24px;
  border-top: 1px solid #e8ecf4;
}

.submit-btn {
  padding: 12px 48px;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  min-width: 140px;
}

.submit-btn:hover {
  background: #1246bb;
  border-color: #1246bb;
}
</style>
