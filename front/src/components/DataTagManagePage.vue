<template>
  <section class="tag-manage-page">
    <!-- 面包屑导航 -->
    <div class="page-header">
      <div class="breadcrumb">
        当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
        <span class="crumb-main" @click="goSystemManage">系统管理</span> &gt;
        <span class="crumb-now">数据标签管理</span>
      </div>
    </div>

    <!-- 页面内容卡片 -->
    <div class="page-content">
      <!-- 页面标题 -->
      <div class="page-title">数据标签管理</div>

      <!-- 标签组列表 -->
      <div class="tag-groups">
        <div
          v-for="(group, groupIndex) in tagGroups"
          :key="group.id"
          class="tag-group"
        >
          <!-- 标签组头部 -->
          <div class="tag-group-header">
            <input
              v-model="group.name"
              class="tag-group-name-input"
              placeholder="请输入标签组名称"
            />
            <button
              class="tag-delete-btn"
              @click="removeTagGroup(groupIndex)"
              title="删除标签组"
            >
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="18" y1="6" x2="6" y2="18"></line>
                <line x1="6" y1="6" x2="18" y2="18"></line>
              </svg>
            </button>
          </div>

          <!-- 标签项列表 -->
          <div class="tag-items">
            <div
              v-for="(item, itemIndex) in group.items"
              :key="item.id"
              class="tag-item"
            >
              <input
                v-model="item.name"
                class="tag-item-input"
                placeholder="请输入标签项名称"
              />
              <span class="tag-item-value">{{ item.value || '-' }}</span>
              <button
                class="tag-item-delete-btn"
                @click="removeTagItem(groupIndex, itemIndex)"
                title="删除标签项"
              >
                <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <line x1="18" y1="6" x2="6" y2="18"></line>
                  <line x1="6" y1="6" x2="18" y2="18"></line>
                </svg>
              </button>
            </div>

            <!-- 添加标签项按钮 -->
            <button class="add-tag-item-btn" @click="addTagItem(groupIndex)">
              <svg width="12" height="12" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              添加标签项
            </button>
          </div>
        </div>

        <!-- 添加标签组按钮 -->
        <button class="add-tag-group-btn" @click="addTagGroup">
          <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
            <line x1="12" y1="5" x2="12" y2="19"></line>
            <line x1="5" y1="12" x2="19" y2="12"></line>
          </svg>
          添加标签组
        </button>
      </div>

      <!-- 提交按钮 -->
      <div class="submit-area">
        <button class="submit-btn" @click="handleSubmit">提交</button>
      </div>
    </div>
  </section>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const emit = defineEmits(['go-home', 'go-system-manage'])

// 标签组数据
const tagGroups = ref([
  {
    id: '1',
    name: 'test',
    items: [
      { id: '1-1', name: 'test', value: '111' },
    ],
  },
])

// 生成唯一ID
let groupIdCounter = 2
let itemIdCounter = 2

const generateGroupId = () => String(groupIdCounter++)
const generateItemId = () => String(itemIdCounter++)

// 添加标签组
const addTagGroup = () => {
  tagGroups.value.push({
    id: generateGroupId(),
    name: '',
    items: [],
  })
}

// 删除标签组
const removeTagGroup = (index) => {
  if (tagGroups.value.length <= 1) {
    alert('至少需要保留一个标签组')
    return
  }
  if (confirm('确定要删除该标签组吗？')) {
    tagGroups.value.splice(index, 1)
  }
}

// 添加标签项
const addTagItem = (groupIndex) => {
  const group = tagGroups.value[groupIndex]
  group.items.push({
    id: generateItemId(),
    name: '',
    value: '',
  })
}

// 删除标签项
const removeTagItem = (groupIndex, itemIndex) => {
  const group = tagGroups.value[groupIndex]
  if (confirm('确定要删除该标签项吗？')) {
    group.items.splice(itemIndex, 1)
  }
}

// 提交
const handleSubmit = () => {
  // 验证数据
  for (const group of tagGroups.value) {
    if (!group.name.trim()) {
      alert('请填写标签组名称')
      return
    }
    for (const item of group.items) {
      if (!item.name.trim()) {
        alert('请填写标签项名称')
        return
      }
    }
  }

  console.log('提交的标签数据：', tagGroups.value)
  alert('提交成功！')
}

const goHome = () => {
  emit('go-home')
}

const goSystemManage = () => {
  emit('go-system-manage')
}

onMounted(() => {
  // 可以在这里加载后端数据
  console.log('数据标签管理页面已加载')
})
</script>

<style scoped>
.tag-manage-page {
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

.tag-groups {
  display: flex;
  flex-direction: column;
  gap: 20px;
}

.tag-group {
  border: 1px solid #e8ecf4;
  border-radius: 6px;
  padding: 16px 20px;
  background: #fafbfc;
}

.tag-group-header {
  display: flex;
  align-items: center;
  gap: 12px;
  margin-bottom: 16px;
}

.tag-group-name-input {
  flex: 1;
  max-width: 200px;
  padding: 8px 12px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 14px;
  color: #333;
  background: #fff;
}

.tag-group-name-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.tag-group-name-input::placeholder {
  color: #b0b6c6;
}

.tag-delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #999;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.tag-delete-btn:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.tag-items {
  display: flex;
  flex-wrap: wrap;
  gap: 12px;
  align-items: center;
}

.tag-item {
  display: flex;
  align-items: center;
  gap: 8px;
  padding: 8px 12px;
  background: #fff;
  border: 1px solid #e8ecf4;
  border-radius: 4px;
}

.tag-item-input {
  width: 140px;
  padding: 6px 10px;
  border: 1px solid #d4dae6;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
  background: #fff;
}

.tag-item-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.tag-item-input::placeholder {
  color: #b0b6c6;
}

.tag-item-value {
  font-size: 13px;
  color: #666;
  min-width: 60px;
  text-align: center;
}

.tag-item-delete-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 20px;
  height: 20px;
  border: none;
  background: transparent;
  color: #ccc;
  cursor: pointer;
  border-radius: 3px;
  transition: all 0.2s;
}

.tag-item-delete-btn:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.add-tag-item-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 14px;
  border: 1px dashed #1a5ce6;
  background: #fff;
  color: #1a5ce6;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.add-tag-item-btn:hover {
  background: #f0f5ff;
  border-style: solid;
}

.add-tag-group-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 10px 16px;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
  align-self: flex-start;
}

.add-tag-group-btn:hover {
  background: #1246bb;
  border-color: #1246bb;
}

.add-tag-group-btn svg {
  display: block;
}

.add-tag-item-btn svg {
  display: block;
}

.submit-area {
  display: flex;
  justify-content: center;
  margin-top: 24px;
  padding-top: 20px;
  border-top: 1px solid #e8ecf4;
}

.submit-btn {
  padding: 10px 32px;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.submit-btn:hover {
  background: #1246bb;
  border-color: #1246bb;
}
</style>
