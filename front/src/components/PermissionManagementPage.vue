<template>
  <section class="perm-page">
    <div class="perm-breadcrumb">
      当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt;
      <span class="crumb-parent">系统管理</span> &gt;
      <span class="crumb-now">权限管理</span>
    </div>

    <section class="perm-card">
      <div class="main-title-row">
        <span class="title-accent" aria-hidden="true" />
        <h2 class="main-title-text">权限管理</h2>
      </div>

      <div class="filter-row">
        <div class="filter-item">
          <label class="filter-label">权限角色</label>
          <input v-model.trim="queryForm.roleName" class="filter-input" placeholder="请输入权限角色" />
        </div>
        <div class="filter-item">
          <label class="filter-label">权限描述</label>
          <input v-model.trim="queryForm.roleDesc" class="filter-input" placeholder="请输入权限描述" />
        </div>
        <div class="filter-item">
          <label class="filter-label">状态</label>
          <select v-model="queryForm.status" class="filter-select">
            <option value="">请选择状态</option>
            <option value="启用">启用</option>
            <option value="停用">停用</option>
          </select>
        </div>
        <div class="filter-actions">
          <button type="button" class="search-btn primary" @click="handleSearch">查询</button>
          <button type="button" class="search-btn ghost" @click="handleReset">重置</button>
        </div>
      </div>

      <div class="toolbar">
        <button type="button" class="tool-btn primary" @click="onAdd">新增</button>
      </div>

      <div class="table-wrap">
        <table class="data-table">
          <thead>
            <tr>
              <th>序号</th>
              <th>权限角色</th>
              <th>权限描述</th>
              <th>创建人</th>
              <th>创建时间</th>
              <th>更新人</th>
              <th>更新时间</th>
              <th>状态</th>
              <th>操作</th>
            </tr>
          </thead>
          <tbody>
            <tr v-for="(row, index) in pagedRows" :key="row.id">
              <td>{{ (page - 1) * pageSize + index + 1 }}</td>
              <td>{{ row.roleName }}</td>
              <td>{{ row.roleDesc }}</td>
              <td>{{ row.creator }}</td>
              <td>{{ row.createTime }}</td>
              <td>{{ row.updater }}</td>
              <td>{{ row.updateTime }}</td>
              <td>
                <button
                  type="button"
                  class="status-switch"
                  :class="{ on: row.status === '启用' }"
                  @click="toggleStatus(row)"
                >
                  <span class="status-switch-dot" />
                </button>
              </td>
              <td>
                <div class="action-icons">
                  <button type="button" class="icon-btn" title="查看" @click="onView(row)">👁</button>
                  <button type="button" class="icon-btn" title="编辑" @click="onEdit(row)">✎</button>
                  <button type="button" class="icon-btn" title="分配人员" @click="onGrant(row)">⚗</button>
                  <button type="button" class="icon-btn danger" title="删除" @click="onDelete(row)">🗑</button>
                </div>
              </td>
            </tr>
          </tbody>
        </table>

        <div v-if="!filteredRows.length" class="table-empty">暂无数据</div>

        <div class="table-footer">
          <span>共 {{ filteredRows.length }} 条</span>
          <div class="pager">
            <button type="button" class="pager-btn" :disabled="page <= 1" @click="prevPage">上一页</button>
            <button type="button" class="pager-num">{{ page }}</button>
            <button type="button" class="pager-btn" :disabled="page >= totalPages" @click="nextPage">下一页</button>
          </div>
        </div>
      </div>
    </section>

    <div v-if="showAddModal" class="modal-mask" @click.self="closeAddModal">
      <section class="add-modal">
        <header class="add-modal-header">
          <h3>新增权限角色</h3>
        </header>

        <div class="add-modal-body">
          <div class="form-line">
            <label><span class="required">*</span> 权限角色</label>
            <div class="line-main">
              <input v-model.trim="addForm.roleName" maxlength="20" placeholder="请输入权限角色" />
              <span class="counter">{{ addForm.roleName.length }}/20</span>
            </div>
          </div>
          <div class="form-line">
            <label>权限描述</label>
            <input v-model.trim="addForm.roleDesc" maxlength="80" placeholder="请输入权限描述" />
          </div>
          <div class="form-line">
            <label><span class="required">*</span> 状态</label>
            <select v-model="addForm.status">
              <option value="">请选择状态</option>
              <option value="启用">启用</option>
              <option value="停用">停用</option>
            </select>
          </div>

          <div class="perm-tree-wrap">
            <div class="tree-top">
              <span class="tree-label">菜单权限</span>
              <span class="tree-actions">
                <button type="button" class="link-btn" @click="toggleTreeExpand">{{ treeExpanded ? '收起' : '展开' }}</button>
                <button type="button" class="link-btn" @click="checkAllMenus">全选/全不选</button>
                <label class="inherit-check">
                  <input v-model="addForm.includeChildren" type="checkbox" />
                  父子联动
                </label>
              </span>
            </div>
            <ul v-show="treeExpanded" class="tree-box">
              <li v-for="node in menuTree" :key="node.id">
                <label>
                  <input
                    type="checkbox"
                    :checked="isMenuChecked(node.id)"
                    @change="onMenuChange(node.id, $event.target.checked)"
                  />
                  {{ node.label }}
                </label>
                <ul v-if="node.children?.length" class="tree-sub">
                  <li v-for="sub in node.children" :key="sub.id">
                    <label>
                      <input
                        type="checkbox"
                        :checked="isMenuChecked(sub.id)"
                        @change="onMenuChange(sub.id, $event.target.checked, node.id)"
                      />
                      {{ sub.label }}
                    </label>
                  </li>
                </ul>
              </li>
            </ul>
          </div>

          <div class="user-transfer">
            <div class="panel">
              <div class="panel-title">待选用户（{{ candidateUsers.length }}）</div>
              <div class="panel-filters">
                <input v-model.trim="candidateKeyword" placeholder="用户名或姓名" />
                <input v-model.trim="candidateDept" placeholder="请选择所属部门" />
              </div>
              <table class="mini-table">
                <thead>
                  <tr><th></th><th>序号</th><th>用户名</th><th>姓名</th><th>所属部门</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(u, i) in candidateUsers" :key="u.id">
                    <td><input v-model="candidateSelected" type="checkbox" :value="u.id" /></td>
                    <td>{{ i + 1 }}</td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.realName }}</td>
                    <td>{{ u.dept }}</td>
                  </tr>
                  <tr v-if="!candidateUsers.length"><td colspan="5" class="mini-empty">暂无数据</td></tr>
                </tbody>
              </table>
            </div>

            <div class="transfer-btns">
              <button type="button" @click="toSelected">></button>
              <button type="button" @click="toCandidate"><</button>
            </div>

            <div class="panel">
              <div class="panel-title">已选用户（{{ selectedUsers.length }}）</div>
              <div class="panel-filters">
                <input v-model.trim="selectedKeyword" placeholder="用户名或姓名" />
                <input v-model.trim="selectedDept" placeholder="请选择所属部门" />
              </div>
              <table class="mini-table">
                <thead>
                  <tr><th></th><th>序号</th><th>用户名</th><th>姓名</th><th>所属部门</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(u, i) in selectedUsers" :key="u.id">
                    <td><input v-model="selectedSelected" type="checkbox" :value="u.id" /></td>
                    <td>{{ i + 1 }}</td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.realName }}</td>
                    <td>{{ u.dept }}</td>
                  </tr>
                  <tr v-if="!selectedUsers.length"><td colspan="5" class="mini-empty">暂无数据</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <footer class="add-modal-footer">
          <button type="button" class="footer-btn ghost" @click="closeAddModal">取消</button>
          <button type="button" class="footer-btn primary" @click="submitAdd">确定</button>
        </footer>
      </section>
    </div>

    <div v-if="showViewModal" class="modal-mask" @click.self="closeViewModal">
      <section class="add-modal">
        <header class="add-modal-header">
          <h3>查看权限角色</h3>
        </header>

        <div class="add-modal-body">
          <div class="form-line">
            <label><span class="required">*</span> 权限角色</label>
            <input :value="viewForm.roleName" disabled />
          </div>
          <div class="form-line">
            <label>权限描述</label>
            <input :value="viewForm.roleDesc" disabled />
          </div>
          <div class="form-line">
            <label><span class="required">*</span> 状态</label>
            <input :value="viewForm.status" disabled />
          </div>

          <div class="perm-tree-wrap">
            <div class="tree-top">
              <span class="tree-label">菜单权限</span>
              <span class="tree-actions view-text-actions">
                <span>展开/折叠</span>
                <span>全选/全不选</span>
                <span>父子联动</span>
              </span>
            </div>
            <ul class="tree-box view-tree-box">
              <li v-for="node in menuTree" :key="node.id">
                <label>
                  <input type="checkbox" :checked="viewCheckedMenus.has(node.id)" disabled />
                  {{ node.label }}
                </label>
                <ul v-if="node.children?.length" class="tree-sub">
                  <li v-for="sub in node.children" :key="sub.id">
                    <label>
                      <input type="checkbox" :checked="viewCheckedMenus.has(sub.id)" disabled />
                      {{ sub.label }}
                    </label>
                  </li>
                </ul>
              </li>
            </ul>
          </div>

          <div class="form-line">
            <label>角色用户</label>
            <input :value="viewUsersText" disabled />
          </div>
        </div>

        <footer class="add-modal-footer single">
          <button type="button" class="footer-btn ghost" @click="closeViewModal">取消</button>
        </footer>
      </section>
    </div>

    <div v-if="showEditModal" class="modal-mask" @click.self="closeEditModal">
      <section class="add-modal">
        <header class="add-modal-header">
          <h3>编辑权限角色</h3>
        </header>

        <div class="add-modal-body">
          <div class="form-line">
            <label><span class="required">*</span> 权限角色</label>
            <div class="line-main">
              <input v-model.trim="editForm.roleName" maxlength="20" placeholder="请输入权限角色" />
              <span class="counter">{{ editForm.roleName.length }}/20</span>
            </div>
          </div>
          <div class="form-line">
            <label>权限描述</label>
            <input v-model.trim="editForm.roleDesc" maxlength="80" placeholder="请输入权限描述" />
          </div>
          <div class="form-line">
            <label><span class="required">*</span> 状态</label>
            <select v-model="editForm.status">
              <option value="">请选择状态</option>
              <option value="启用">正常</option>
              <option value="停用">停用</option>
            </select>
          </div>

          <div class="perm-tree-wrap">
            <div class="tree-top">
              <span class="tree-label">菜单权限</span>
              <span class="tree-actions">
                <button type="button" class="link-btn" @click="toggleEditTreeExpand">{{ editTreeExpanded ? '收起' : '展开' }}</button>
                <button type="button" class="link-btn" @click="checkAllEditMenus">全选/全不选</button>
                <label class="inherit-check">
                  <input v-model="editForm.includeChildren" type="checkbox" />
                  父子联动
                </label>
              </span>
            </div>
            <ul v-show="editTreeExpanded" class="tree-box">
              <li v-for="node in menuTree" :key="`e-${node.id}`">
                <label>
                  <input
                    type="checkbox"
                    :checked="isEditMenuChecked(node.id)"
                    @change="onEditMenuChange(node.id, $event.target.checked)"
                  />
                  {{ node.label }}
                </label>
                <ul v-if="node.children?.length" class="tree-sub">
                  <li v-for="sub in node.children" :key="`e-${sub.id}`">
                    <label>
                      <input
                        type="checkbox"
                        :checked="isEditMenuChecked(sub.id)"
                        @change="onEditMenuChange(sub.id, $event.target.checked, node.id)"
                      />
                      {{ sub.label }}
                    </label>
                  </li>
                </ul>
              </li>
            </ul>
          </div>

          <div class="user-transfer">
            <div class="panel">
              <div class="panel-title">待选用户（{{ editCandidateUsers.length }}）</div>
              <div class="panel-filters">
                <input v-model.trim="editCandidateKeyword" placeholder="用户名或姓名" />
                <input v-model.trim="editCandidateDept" placeholder="请选择所属部门" />
              </div>
              <table class="mini-table">
                <thead>
                  <tr><th></th><th>序号</th><th>用户名</th><th>姓名</th><th>所属部门</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(u, i) in editCandidateUsers" :key="`ec-${u.id}`">
                    <td><input v-model="editCandidateSelected" type="checkbox" :value="u.id" /></td>
                    <td>{{ i + 1 }}</td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.realName }}</td>
                    <td>{{ u.dept }}</td>
                  </tr>
                  <tr v-if="!editCandidateUsers.length"><td colspan="5" class="mini-empty">暂无数据</td></tr>
                </tbody>
              </table>
            </div>

            <div class="transfer-btns">
              <button type="button" @click="editToSelected">></button>
              <button type="button" @click="editToCandidate"><</button>
            </div>

            <div class="panel">
              <div class="panel-title">已选用户（{{ editSelectedUsers.length }}）</div>
              <div class="panel-filters">
                <input v-model.trim="editSelectedKeyword" placeholder="用户名或姓名" />
                <input v-model.trim="editSelectedDept" placeholder="请选择所属部门" />
              </div>
              <table class="mini-table">
                <thead>
                  <tr><th></th><th>序号</th><th>用户名</th><th>姓名</th><th>所属部门</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(u, i) in editSelectedUsers" :key="`es-${u.id}`">
                    <td><input v-model="editSelectedSelected" type="checkbox" :value="u.id" /></td>
                    <td>{{ i + 1 }}</td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.realName }}</td>
                    <td>{{ u.dept }}</td>
                  </tr>
                  <tr v-if="!editSelectedUsers.length"><td colspan="5" class="mini-empty">暂无数据</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>

        <footer class="add-modal-footer">
          <button type="button" class="footer-btn ghost" @click="closeEditModal">取消</button>
          <button type="button" class="footer-btn primary" @click="submitEdit">确定</button>
        </footer>
      </section>
    </div>

    <div v-if="showAssignModal" class="modal-mask" @click.self="closeAssignModal">
      <section class="add-modal assign-modal">
        <header class="add-modal-header">
          <h3>分配人员</h3>
        </header>
        <div class="add-modal-body">
          <div class="form-line">
            <label>角色用户</label>
            <select>
              <option>{{ assignRoleName || '请选择角色' }}</option>
            </select>
          </div>

          <div class="user-transfer">
            <div class="panel">
              <div class="panel-title">待选用户（{{ assignCandidateUsers.length }}）</div>
              <div class="panel-filters">
                <input v-model.trim="assignCandidateKeyword" placeholder="用户名或姓名" />
                <input v-model.trim="assignCandidateDept" placeholder="请选择所属部门" />
              </div>
              <div class="panel-actions">
                <button type="button" class="mini-btn primary">搜索</button>
                <button type="button" class="mini-btn">重置</button>
              </div>
              <table class="mini-table">
                <thead>
                  <tr><th></th><th>序号</th><th>用户名</th><th>姓名</th><th>所属部门</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(u, i) in assignCandidateUsers" :key="`ac-${u.id}`">
                    <td><input v-model="assignCandidateSelected" type="checkbox" :value="u.id" /></td>
                    <td>{{ i + 1 }}</td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.realName }}</td>
                    <td>{{ u.dept }}</td>
                  </tr>
                  <tr v-if="!assignCandidateUsers.length"><td colspan="5" class="mini-empty">暂无数据</td></tr>
                </tbody>
              </table>
            </div>

            <div class="transfer-btns">
              <button type="button" @click="assignToSelected">></button>
              <button type="button" @click="assignToCandidate"><</button>
            </div>

            <div class="panel">
              <div class="panel-title">已选用户（{{ assignSelectedUsers.length }}）</div>
              <div class="panel-filters">
                <input v-model.trim="assignSelectedKeyword" placeholder="用户名或姓名" />
                <input v-model.trim="assignSelectedDept" placeholder="请选择所属部门" />
              </div>
              <div class="panel-actions">
                <button type="button" class="mini-btn primary">搜索</button>
                <button type="button" class="mini-btn">重置</button>
              </div>
              <table class="mini-table">
                <thead>
                  <tr><th></th><th>序号</th><th>用户名</th><th>姓名</th><th>所属部门</th></tr>
                </thead>
                <tbody>
                  <tr v-for="(u, i) in assignSelectedUsers" :key="`as-${u.id}`">
                    <td><input v-model="assignSelectedSelected" type="checkbox" :value="u.id" /></td>
                    <td>{{ i + 1 }}</td>
                    <td>{{ u.username }}</td>
                    <td>{{ u.realName }}</td>
                    <td>{{ u.dept }}</td>
                  </tr>
                  <tr v-if="!assignSelectedUsers.length"><td colspan="5" class="mini-empty">暂无数据</td></tr>
                </tbody>
              </table>
            </div>
          </div>
        </div>
        <footer class="add-modal-footer">
          <button type="button" class="footer-btn ghost" @click="closeAssignModal">取消</button>
          <button type="button" class="footer-btn primary" @click="submitAssign">确定</button>
        </footer>
      </section>
    </div>
  </section>
</template>

<script setup>
import { computed, ref, watch } from 'vue'

const emit = defineEmits(['go-home'])

const allRows = ref([
  {
    id: 1,
    roleName: '初审员',
    roleDesc: '',
    creator: 'admin',
    createTime: '2025-08-05 18:06:26',
    updater: '张三',
    updateTime: '2026-01-23 11:56:09',
    status: '启用',
    menuIds: ['template', 'upload', 'publish', 'library', 'database'],
    userIds: [1, 2],
  },
  {
    id: 2,
    roleName: '超级管理员',
    roleDesc: '超级管理员',
    creator: 'admin',
    createTime: '2021-05-26 18:56:28',
    updater: '',
    updateTime: '',
    status: '停用',
    menuIds: ['template', 'upload', 'publish', 'library', 'database', 'audit', 'audit-template', 'audit-fragment', 'system', 'sys-user', 'sys-perm'],
    userIds: [],
  },
  {
    id: 3,
    roleName: '普通角色',
    roleDesc: '普通角色',
    creator: 'admin',
    createTime: '2021-05-26 18:56:28',
    updater: '王五',
    updateTime: '2025-11-12 14:17:02',
    status: '启用',
    menuIds: ['template', 'library', 'database'],
    userIds: [3],
  },
])

const queryForm = ref({
  roleName: '',
  roleDesc: '',
  status: '',
})

const showAddModal = ref(false)
const showViewModal = ref(false)
const showEditModal = ref(false)
const showAssignModal = ref(false)
const addForm = ref({ roleName: '', roleDesc: '', status: '启用', includeChildren: true })
const viewForm = ref({ roleName: '', roleDesc: '', status: '' })
const viewCheckedMenus = ref(new Set())
const viewUserIds = ref(new Set())
const editForm = ref({ roleName: '', roleDesc: '', status: '启用', includeChildren: true })
const editTreeExpanded = ref(true)
const editCheckedMenus = ref(new Set())
const editSelectedUserIds = ref(new Set())
const editRowId = ref(null)
const editCandidateKeyword = ref('')
const editCandidateDept = ref('')
const editSelectedKeyword = ref('')
const editSelectedDept = ref('')
const editCandidateSelected = ref([])
const editSelectedSelected = ref([])
const assignRoleName = ref('')
const assignRoleId = ref(null)
const assignSelectedUserIds = ref(new Set())
const assignCandidateKeyword = ref('')
const assignCandidateDept = ref('')
const assignSelectedKeyword = ref('')
const assignSelectedDept = ref('')
const assignCandidateSelected = ref([])
const assignSelectedSelected = ref([])
const treeExpanded = ref(true)
const checkedMenus = ref(new Set())
const candidateKeyword = ref('')
const candidateDept = ref('')
const selectedKeyword = ref('')
const selectedDept = ref('')
const candidateSelected = ref([])
const selectedSelected = ref([])

const menuTree = [
  { id: 'template', label: '模板创建' },
  { id: 'upload', label: '数据上传' },
  { id: 'publish', label: '数据发布' },
  { id: 'library', label: '模板库' },
  { id: 'database', label: '数据库' },
  {
    id: 'audit',
    label: '审核管理',
    children: [
      { id: 'audit-template', label: '模板审核' },
      { id: 'audit-fragment', label: '片段审核' },
    ],
  },
  {
    id: 'system',
    label: '系统管理',
    children: [
      { id: 'sys-user', label: '用户管理' },
      { id: 'sys-perm', label: '权限管理' },
    ],
  },
]

const allUsers = ref([
  { id: 1, username: 'zhangsan', realName: '张三', dept: '节点管理部' },
  { id: 2, username: 'lisi', realName: '李四', dept: '节点管理部' },
  { id: 3, username: 'wangwu', realName: '王五', dept: '社会大众' },
])
const selectedUserIds = ref(new Set())

const page = ref(1)
const pageSize = 10

const filteredRows = computed(() => {
  const roleName = queryForm.value.roleName.toLowerCase()
  const roleDesc = queryForm.value.roleDesc.toLowerCase()
  const status = queryForm.value.status
  return allRows.value.filter((row) => {
    const hitRoleName = !roleName || String(row.roleName).toLowerCase().includes(roleName)
    const hitRoleDesc = !roleDesc || String(row.roleDesc).toLowerCase().includes(roleDesc)
    const hitStatus = !status || row.status === status
    return hitRoleName && hitRoleDesc && hitStatus
  })
})

const candidateUsers = computed(() => {
  const kw = candidateKeyword.value.toLowerCase()
  const dept = candidateDept.value.toLowerCase()
  return allUsers.value.filter((u) => {
    if (selectedUserIds.value.has(u.id)) return false
    const hitKw = !kw || u.username.toLowerCase().includes(kw) || u.realName.toLowerCase().includes(kw)
    const hitDept = !dept || u.dept.toLowerCase().includes(dept)
    return hitKw && hitDept
  })
})

const selectedUsers = computed(() => {
  const kw = selectedKeyword.value.toLowerCase()
  const dept = selectedDept.value.toLowerCase()
  return allUsers.value.filter((u) => {
    if (!selectedUserIds.value.has(u.id)) return false
    const hitKw = !kw || u.username.toLowerCase().includes(kw) || u.realName.toLowerCase().includes(kw)
    const hitDept = !dept || u.dept.toLowerCase().includes(dept)
    return hitKw && hitDept
  })
})

const editCandidateUsers = computed(() => {
  const kw = editCandidateKeyword.value.toLowerCase()
  const dept = editCandidateDept.value.toLowerCase()
  return allUsers.value.filter((u) => {
    if (editSelectedUserIds.value.has(u.id)) return false
    const hitKw = !kw || u.username.toLowerCase().includes(kw) || u.realName.toLowerCase().includes(kw)
    const hitDept = !dept || u.dept.toLowerCase().includes(dept)
    return hitKw && hitDept
  })
})

const editSelectedUsers = computed(() => {
  const kw = editSelectedKeyword.value.toLowerCase()
  const dept = editSelectedDept.value.toLowerCase()
  return allUsers.value.filter((u) => {
    if (!editSelectedUserIds.value.has(u.id)) return false
    const hitKw = !kw || u.username.toLowerCase().includes(kw) || u.realName.toLowerCase().includes(kw)
    const hitDept = !dept || u.dept.toLowerCase().includes(dept)
    return hitKw && hitDept
  })
})

const assignCandidateUsers = computed(() => {
  const kw = assignCandidateKeyword.value.toLowerCase()
  const dept = assignCandidateDept.value.toLowerCase()
  return allUsers.value.filter((u) => {
    if (assignSelectedUserIds.value.has(u.id)) return false
    const hitKw = !kw || u.username.toLowerCase().includes(kw) || u.realName.toLowerCase().includes(kw)
    const hitDept = !dept || u.dept.toLowerCase().includes(dept)
    return hitKw && hitDept
  })
})

const assignSelectedUsers = computed(() => {
  const kw = assignSelectedKeyword.value.toLowerCase()
  const dept = assignSelectedDept.value.toLowerCase()
  return allUsers.value.filter((u) => {
    if (!assignSelectedUserIds.value.has(u.id)) return false
    const hitKw = !kw || u.username.toLowerCase().includes(kw) || u.realName.toLowerCase().includes(kw)
    const hitDept = !dept || u.dept.toLowerCase().includes(dept)
    return hitKw && hitDept
  })
})

const viewUsersText = computed(() => {
  const names = allUsers.value
    .filter((u) => viewUserIds.value.has(u.id))
    .map((u) => `${u.username}(${u.realName})`)
  return names.join('、')
})

const totalPages = computed(() => Math.max(1, Math.ceil(filteredRows.value.length / pageSize)))

const pagedRows = computed(() => {
  const start = (page.value - 1) * pageSize
  return filteredRows.value.slice(start, start + pageSize)
})

watch(
  () => filteredRows.value.length,
  () => {
    if (page.value > totalPages.value) page.value = totalPages.value
  },
)

function handleSearch() {
  page.value = 1
}

function handleReset() {
  queryForm.value = { roleName: '', roleDesc: '', status: '' }
  page.value = 1
}

function prevPage() {
  if (page.value > 1) page.value -= 1
}

function nextPage() {
  if (page.value < totalPages.value) page.value += 1
}

function toggleStatus(row) {
  row.status = row.status === '启用' ? '停用' : '启用'
}

function onView(row) {
  viewForm.value = {
    roleName: row.roleName || '',
    roleDesc: row.roleDesc || '',
    status: row.status === '启用' ? '正常' : '停用',
  }
  viewCheckedMenus.value = new Set(Array.isArray(row.menuIds) ? row.menuIds : [])
  viewUserIds.value = new Set(Array.isArray(row.userIds) ? row.userIds : [])
  showViewModal.value = true
}

function onEdit(row) {
  editRowId.value = row.id
  editForm.value = {
    roleName: row.roleName || '',
    roleDesc: row.roleDesc || '',
    status: row.status || '启用',
    includeChildren: true,
  }
  editCheckedMenus.value = new Set(Array.isArray(row.menuIds) ? row.menuIds : [])
  editSelectedUserIds.value = new Set(Array.isArray(row.userIds) ? row.userIds : [])
  showEditModal.value = true
}

function onGrant(row) {
  assignRoleName.value = row.roleName
  assignRoleId.value = row.id
  assignSelectedUserIds.value = new Set(Array.isArray(row.userIds) ? row.userIds : [])
  assignCandidateSelected.value = []
  assignSelectedSelected.value = []
  showAssignModal.value = true
}

function onDelete(row) {
  if (!confirm(`确定删除角色「${row.roleName}」？`)) return
  allRows.value = allRows.value.filter((x) => x.id !== row.id)
}

function onAdd() {
  showAddModal.value = true
}

function goHome() {
  emit('go-home')
}

function closeAddModal() {
  showAddModal.value = false
}

function closeViewModal() {
  showViewModal.value = false
}

function closeEditModal() {
  showEditModal.value = false
}

function closeAssignModal() {
  showAssignModal.value = false
}

function toggleTreeExpand() {
  treeExpanded.value = !treeExpanded.value
}

function flattenMenuIds() {
  const ids = []
  for (const node of menuTree) {
    ids.push(node.id)
    for (const sub of node.children || []) ids.push(sub.id)
  }
  return ids
}

function checkAllMenus() {
  const allIds = flattenMenuIds()
  if (checkedMenus.value.size === allIds.length) {
    checkedMenus.value = new Set()
  } else {
    checkedMenus.value = new Set(allIds)
  }
}

function isMenuChecked(id) {
  return checkedMenus.value.has(id)
}

function onMenuChange(id, checked, parentId = '') {
  const next = new Set(checkedMenus.value)
  if (checked) next.add(id)
  else next.delete(id)

  if (addForm.value.includeChildren) {
    const parent = menuTree.find((x) => x.id === id || x.id === parentId)
    if (parent?.children?.length && parent.id === id) {
      for (const c of parent.children) {
        if (checked) next.add(c.id)
        else next.delete(c.id)
      }
    }
    if (parentId) {
      const p = menuTree.find((x) => x.id === parentId)
      const children = p?.children || []
      if (children.length && children.every((c) => next.has(c.id))) next.add(parentId)
      if (children.length && children.every((c) => !next.has(c.id))) next.delete(parentId)
    }
  }

  checkedMenus.value = next
}

function toSelected() {
  const next = new Set(selectedUserIds.value)
  candidateSelected.value.forEach((id) => next.add(id))
  selectedUserIds.value = next
  candidateSelected.value = []
}

function toCandidate() {
  const next = new Set(selectedUserIds.value)
  selectedSelected.value.forEach((id) => next.delete(id))
  selectedUserIds.value = next
  selectedSelected.value = []
}

function submitAdd() {
  if (!addForm.value.roleName) {
    alert('请输入权限角色')
    return
  }
  if (!addForm.value.status) {
    alert('请选择状态')
    return
  }
  const now = new Date()
  const fmt = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(
    now.getDate(),
  ).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(
    2,
    '0',
  )}:${String(now.getSeconds()).padStart(2, '0')}`
  allRows.value.unshift({
    id: Date.now(),
    roleName: addForm.value.roleName,
    roleDesc: addForm.value.roleDesc,
    creator: 'admin',
    createTime: fmt,
    updater: '',
    updateTime: '',
    status: addForm.value.status,
    menuIds: Array.from(checkedMenus.value),
    userIds: Array.from(selectedUserIds.value),
  })
  addForm.value = { roleName: '', roleDesc: '', status: '启用', includeChildren: true }
  checkedMenus.value = new Set()
  selectedUserIds.value = new Set()
  closeAddModal()
}

function toggleEditTreeExpand() {
  editTreeExpanded.value = !editTreeExpanded.value
}

function isEditMenuChecked(id) {
  return editCheckedMenus.value.has(id)
}

function checkAllEditMenus() {
  const allIds = flattenMenuIds()
  if (editCheckedMenus.value.size === allIds.length) editCheckedMenus.value = new Set()
  else editCheckedMenus.value = new Set(allIds)
}

function onEditMenuChange(id, checked, parentId = '') {
  const next = new Set(editCheckedMenus.value)
  if (checked) next.add(id)
  else next.delete(id)

  if (editForm.value.includeChildren) {
    const parent = menuTree.find((x) => x.id === id || x.id === parentId)
    if (parent?.children?.length && parent.id === id) {
      for (const c of parent.children) {
        if (checked) next.add(c.id)
        else next.delete(c.id)
      }
    }
    if (parentId) {
      const p = menuTree.find((x) => x.id === parentId)
      const children = p?.children || []
      if (children.length && children.every((c) => next.has(c.id))) next.add(parentId)
      if (children.length && children.every((c) => !next.has(c.id))) next.delete(parentId)
    }
  }
  editCheckedMenus.value = next
}

function editToSelected() {
  const next = new Set(editSelectedUserIds.value)
  editCandidateSelected.value.forEach((id) => next.add(id))
  editSelectedUserIds.value = next
  editCandidateSelected.value = []
}

function editToCandidate() {
  const next = new Set(editSelectedUserIds.value)
  editSelectedSelected.value.forEach((id) => next.delete(id))
  editSelectedUserIds.value = next
  editSelectedSelected.value = []
}

function submitEdit() {
  if (!editForm.value.roleName) {
    alert('请输入权限角色')
    return
  }
  if (!editForm.value.status) {
    alert('请选择状态')
    return
  }
  const index = allRows.value.findIndex((x) => x.id === editRowId.value)
  if (index < 0) return
  const old = allRows.value[index]
  const now = new Date()
  const fmt = `${now.getFullYear()}-${String(now.getMonth() + 1).padStart(2, '0')}-${String(
    now.getDate(),
  ).padStart(2, '0')} ${String(now.getHours()).padStart(2, '0')}:${String(now.getMinutes()).padStart(
    2,
    '0',
  )}:${String(now.getSeconds()).padStart(2, '0')}`
  allRows.value[index] = {
    ...old,
    roleName: editForm.value.roleName,
    roleDesc: editForm.value.roleDesc,
    status: editForm.value.status,
    updater: 'admin',
    updateTime: fmt,
    menuIds: Array.from(editCheckedMenus.value),
    userIds: Array.from(editSelectedUserIds.value),
  }
  closeEditModal()
}

function assignToSelected() {
  const next = new Set(assignSelectedUserIds.value)
  assignCandidateSelected.value.forEach((id) => next.add(id))
  assignSelectedUserIds.value = next
  assignCandidateSelected.value = []
}

function assignToCandidate() {
  const next = new Set(assignSelectedUserIds.value)
  assignSelectedSelected.value.forEach((id) => next.delete(id))
  assignSelectedUserIds.value = next
  assignSelectedSelected.value = []
}

function submitAssign() {
  const index = allRows.value.findIndex((x) => x.id === assignRoleId.value)
  if (index < 0) return
  allRows.value[index] = {
    ...allRows.value[index],
    userIds: Array.from(assignSelectedUserIds.value),
  }
  closeAssignModal()
}
</script>

<style scoped>
.perm-page {
  display: flex;
  flex-direction: column;
  gap: 12px;
  background: #f0f2f5;
  padding: 12px 20px 20px;
  min-height: calc(100vh - 110px);
}

.perm-breadcrumb {
  font-size: 13px;
  color: #666;
}

.crumb-main {
  color: #1a5ce6;
  cursor: pointer;
}

.crumb-main:hover {
  text-decoration: underline;
}

.crumb-parent,
.crumb-now {
  color: #333;
}

.crumb-now {
  font-weight: 500;
}

.perm-card {
  background: #fff;
  border-radius: 4px;
  padding: 20px 24px 24px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.08);
}

.main-title-row {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-bottom: 18px;
}

.title-accent {
  width: 4px;
  height: 18px;
  background: #1a5ce6;
  border-radius: 2px;
  flex-shrink: 0;
}

.main-title-text {
  margin: 0;
  font-size: 16px;
  font-weight: 600;
  color: #333;
}

.filter-row {
  display: flex;
  flex-wrap: wrap;
  align-items: flex-end;
  gap: 16px;
  margin-bottom: 14px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: #666;
  white-space: nowrap;
}

.filter-input,
.filter-select {
  width: 160px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
  color: #333;
  background: #fff;
}

.filter-actions {
  display: flex;
  gap: 8px;
}

.search-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
}

.search-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.search-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

.toolbar {
  margin-bottom: 12px;
}

.tool-btn {
  padding: 6px 18px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid #1a5ce6;
  background: #1a5ce6;
  color: #fff;
}

.table-wrap {
  overflow-x: auto;
}

.data-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
  min-width: 980px;
}

.data-table thead th {
  padding: 10px 8px;
  text-align: center;
  font-weight: 500;
  color: #333;
  background: #fafafa;
  border-bottom: 1px solid #e8e8e8;
  white-space: nowrap;
}

.data-table tbody td {
  padding: 10px 8px;
  text-align: center;
  border-bottom: 1px solid #f0f0f0;
  color: #333;
  vertical-align: middle;
}

.status-switch {
  width: 36px;
  height: 20px;
  border: none;
  border-radius: 10px;
  background: #cfd6e4;
  position: relative;
  cursor: pointer;
  transition: background 0.2s;
}

.status-switch.on {
  background: #1a5ce6;
}

.status-switch-dot {
  width: 16px;
  height: 16px;
  border-radius: 50%;
  background: #fff;
  position: absolute;
  top: 2px;
  left: 2px;
  transition: left 0.2s;
}

.status-switch.on .status-switch-dot {
  left: 18px;
}

.action-icons {
  display: flex;
  justify-content: center;
  gap: 6px;
}

.icon-btn {
  border: none;
  background: transparent;
  cursor: pointer;
  color: #1a5ce6;
  font-size: 18px;
  line-height: 1;
  width: 24px;
  height: 24px;
  display: inline-flex;
  align-items: center;
  justify-content: center;
}

.icon-btn.danger {
  color: #cf1322;
}

.table-empty {
  padding: 24px;
  text-align: center;
  color: #999;
  font-size: 13px;
}

.table-footer {
  margin-top: 10px;
  display: flex;
  align-items: center;
  justify-content: space-between;
  font-size: 13px;
  color: #666;
}

.pager {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.pager-btn,
.pager-num {
  border: 1px solid #d9dfe9;
  background: #fff;
  color: #666;
  border-radius: 3px;
  height: 28px;
  padding: 0 12px;
  cursor: pointer;
}

.pager-num {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.pager-btn:disabled {
  color: #bbb;
  cursor: not-allowed;
}

.modal-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.38);
  z-index: 600;
  display: flex;
  align-items: flex-start;
  justify-content: center;
  padding: 32px;
  overflow: auto;
}

.add-modal {
  width: min(1120px, 95vw);
  background: #fff;
  border-radius: 4px;
  border: 1px solid #e7ebf3;
}

.add-modal-header {
  padding: 12px 16px;
  border-bottom: 1px solid #e9edf3;
  font-size: 15px;
}

.add-modal-header h3 {
  margin: 0;
}

.add-modal-body {
  padding: 14px 16px;
}

.form-line {
  display: grid;
  grid-template-columns: 90px 1fr;
  align-items: center;
  gap: 10px;
  margin-bottom: 12px;
}

.form-line > label {
  font-size: 13px;
  color: #333;
  text-align: right;
}

.required {
  color: #d94848;
}

.form-line input,
.form-line select {
  width: 100%;
  height: 30px;
  border: 1px solid #d7dde8;
  border-radius: 3px;
  padding: 0 10px;
}

.line-main {
  position: relative;
}

.counter {
  position: absolute;
  right: 10px;
  top: 7px;
  font-size: 12px;
  color: #9aa4b3;
}

.perm-tree-wrap {
  margin-top: 4px;
  margin-bottom: 12px;
}

.tree-top {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 8px;
}

.tree-label {
  font-size: 13px;
  color: #333;
}

.tree-actions {
  display: inline-flex;
  align-items: center;
  gap: 10px;
}

.link-btn {
  border: none;
  background: transparent;
  color: #1a5ce6;
  cursor: pointer;
  font-size: 12px;
}

.inherit-check {
  font-size: 12px;
  color: #1a5ce6;
}

.tree-box {
  border: 1px solid #e1e6ef;
  border-radius: 3px;
  padding: 8px 10px;
  margin: 0;
  list-style: none;
  max-height: 170px;
  overflow: auto;
  font-size: 13px;
}

.tree-box > li + li {
  margin-top: 6px;
}

.tree-sub {
  margin: 6px 0 0 20px;
  padding: 0;
  list-style: none;
}

.tree-sub li + li {
  margin-top: 4px;
}

.user-transfer {
  display: grid;
  grid-template-columns: 1fr 54px 1fr;
  gap: 12px;
  align-items: center;
}

.panel {
  border: 1px solid #e4e9f1;
  padding: 10px;
}

.panel-title {
  font-size: 14px;
  color: #333;
  margin-bottom: 10px;
}

.panel-filters {
  display: grid;
  gap: 8px;
  margin-bottom: 10px;
}

.panel-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 8px;
}

.mini-btn {
  height: 28px;
  padding: 0 12px;
  border-radius: 3px;
  border: 1px solid #d8dfeb;
  background: #fff;
  color: #333;
  cursor: pointer;
  font-size: 12px;
}

.mini-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.panel-filters input {
  height: 30px;
  border: 1px solid #d7dde8;
  border-radius: 3px;
  padding: 0 10px;
}

.mini-table {
  width: 100%;
  border-collapse: collapse;
  font-size: 13px;
}

.mini-table th,
.mini-table td {
  border: 1px solid #edf1f6;
  padding: 6px 4px;
  text-align: center;
}

.mini-table thead th {
  background: #fafbfd;
}

.mini-empty {
  color: #98a2b2;
  padding: 24px 0;
}

.transfer-btns {
  display: flex;
  flex-direction: column;
  gap: 10px;
  align-items: center;
}

.transfer-btns button {
  width: 30px;
  height: 30px;
  border: none;
  border-radius: 50%;
  background: #8bc0f6;
  color: #fff;
  cursor: pointer;
}

.add-modal-footer {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 14px;
  padding: 12px 16px 16px;
}

.add-modal-footer.single {
  grid-template-columns: 1fr;
}

.footer-btn {
  height: 34px;
  border-radius: 3px;
  border: 1px solid #d9e0ea;
  cursor: pointer;
}

.footer-btn.ghost {
  background: #fff;
  color: #333;
}

.footer-btn.primary {
  background: #1a5ce6;
  color: #fff;
  border-color: #1a5ce6;
}

.view-text-actions {
  color: #a6adba;
  font-size: 12px;
}

.view-tree-box {
  max-height: 140px;
}

.assign-modal {
  width: min(1000px, 94vw);
}
</style>
