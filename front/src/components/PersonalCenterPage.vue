<template>
  <section class="personal-center-page">
    <div class="pc-layout">
      <aside class="pc-sidebar">
        <div class="pc-sidebar-title">个人中心</div>
        <ul class="pc-menu">
          <li class="pc-menu-item active">
            我的模板
          </li>
        </ul>
      </aside>

      <main class="pc-main">
        <div class="pc-breadcrumb">
          当前位置：<span class="crumb-main">首页</span> &gt; <span class="crumb-now">我的模板</span>
        </div>

        <div class="pc-title">我的模板</div>

        <div class="pc-table-area">
          <table class="data-table">
            <thead>
              <tr>
                <th class="col-index">序号</th>
                <th class="col-name">模板名称</th>
                <th class="col-tag">模板标签</th>
                <th class="col-published">是否已发布至新材料大数据中心</th>
                <th class="col-creator">创建人</th>
                <th class="col-time">创建时间</th>
                <th class="col-action">操作</th>
              </tr>
            </thead>
            <tbody>
              <tr v-for="(item, index) in tableData" :key="item.id">
                <td class="col-index">{{ index + 1 }}</td>
                <td class="col-name" :title="item.name">{{ item.name }}</td>
                <td class="col-tag">
                  <span class="tag-badge">{{ item.tag }}</span>
                </td>
                <td class="col-published">{{ item.published }}</td>
                <td class="col-creator">{{ item.creator }}</td>
                <td class="col-time">{{ item.createTime }}</td>
                <td class="col-action">
                  <button type="button" class="action-btn" @click="handleView(item)">
                    查看
                  </button>
                </td>
              </tr>
            </tbody>
          </table>
        </div>
      </main>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { getMyModules } from '../api/module.js'

const loading = ref(false)
const loadError = ref('')
const tableData = ref([])

const currentUserName = ref('')
const currentUserId = ref(null)

function base64UrlDecode(input) {
  const s = String(input || '').replace(/-/g, '+').replace(/_/g, '/')
  const pad = s.length % 4 === 0 ? '' : '='.repeat(4 - (s.length % 4))
  const b64 = s + pad
  const binary = atob(b64)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}

function syncUserFromToken() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token || token.split('.').length !== 3) {
    currentUserName.value = 'admin'
    currentUserId.value = null
    return
  }
  try {
    const payloadText = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadText)
    currentUserName.value = payload?.userName ? String(payload.userName) : 'admin'
    // JWT subject 即 userId
    currentUserId.value = payload?.sub ? Number(payload.sub) : null
  } catch (_) {
    currentUserName.value = 'admin'
    currentUserId.value = null
  }
}

async function loadMyModules() {
  if (!currentUserId.value) return
  loading.value = true
  loadError.value = ''
  try {
    const resp = await getMyModules(currentUserId.value)
    const list = Array.isArray(resp.data) ? resp.data : []
    tableData.value = list.map((m) => ({
      id: m.id,
      name: m.moduleName,
      tag: m.tag,
      // 后端暂未返回发布状态，这里统一显示“否”
      published: '否',
      creator: currentUserName.value || String(m.creator ?? ''),
      createTime: m.createTime ?? '',
    }))
  } catch (e) {
    loadError.value = e?.message || '加载我的模板失败'
    tableData.value = []
  } finally {
    loading.value = false
  }
}

onMounted(async () => {
  syncUserFromToken()
  await loadMyModules()
})

function handleView(_item) {
  // 先占位：后续可对接“查看模板详情”
  alert('查看模板（占位）')
}
</script>

<style scoped>
.personal-center-page {
  background: #f0f2f5;
  min-height: calc(100vh - 110px);
  padding: 16px 20px 20px;
}

.pc-layout {
  display: grid;
  grid-template-columns: 240px 1fr;
  gap: 16px;
  align-items: start;
}

.pc-sidebar {
  background: #fff;
  border-radius: 8px;
  padding: 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.pc-sidebar-title {
  font-size: 16px;
  font-weight: 600;
  color: #333;
  margin-bottom: 12px;
}

.pc-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.pc-menu-item {
  padding: 10px 12px;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.pc-menu-item.active {
  background: #e6f7ff;
  color: #1a5ce6;
  font-weight: 600;
}

.pc-main {
  background: #fff;
  border-radius: 8px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.pc-breadcrumb {
  font-size: 13px;
  color: #666;
  margin-bottom: 12px;
}

.crumb-main {
  color: #1a5ce6;
}

.crumb-now {
  color: #333;
}

.pc-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
  margin-bottom: 16px;
}

.pc-table-area {
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

.col-index {
  width: 60px;
  text-align: center;
}

.col-name {
  min-width: 180px;
  max-width: 240px;
}

.col-tag {
  min-width: 140px;
}

.col-published {
  min-width: 180px;
}

.col-creator {
  min-width: 80px;
}

.col-time {
  min-width: 120px;
}

.col-action {
  min-width: 90px;
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

.action-btn {
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #1a5ce6;
  cursor: pointer;
}

.action-btn:hover {
  border-color: #1a5ce6;
}
</style>

