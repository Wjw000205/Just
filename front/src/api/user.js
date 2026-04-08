/**
 * 用户相关接口（经 Vite /user 代理到后端）
 */

function getAuthHeader() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

/**
 * @param {number|null|undefined} role
 */
export function formatRoleLabel(role) {
  if (role === 2) return '超级管理员'
  if (role === 1) return '管理员'
  return '普通用户'
}

/**
 * 应用初始化：当前登录用户基本信息（需 Token）
 * GET /user/auth/profile
 * @returns {Promise<{ userId: number, name: string, role: string }>}
 */
export async function fetchAuthProfile() {
  const resp = await fetch('/user/auth/profile', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '获取登录信息失败')
  }

  const d = json.data
  return {
    userId: d.userId != null ? Number(d.userId) : null,
    name: d.name != null ? String(d.name) : '',
    role: d.role != null ? String(d.role) : '',
  }
}

/**
 * 个人中心：完整资料（需 Token）
 * GET /user/profile
 */
export async function fetchMyProfile() {
  const resp = await fetch('/user/profile', {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '加载我的资料失败')
  }

  const d = json.data
  const roleNum = d.role != null ? Number(d.role) : null
  return {
    username: d.username != null ? String(d.username) : '',
    realName: d.realName != null ? String(d.realName) : '',
    email: d.email != null ? String(d.email) : '',
    phone: d.telephone != null ? String(d.telephone) : '',
    role: roleNum,
    roleLabel: formatRoleLabel(roleNum),
    department: d.department != null ? String(d.department) : '',
    applyDept: d.applyDepartment != null ? String(d.applyDepartment) : '',
    applyStatus: d.applyState != null ? String(d.applyState) : '',
  }
}

/**
 * 编辑我的资料（需 Token）
 * PUT /user/profile
 * @param {{ realName: string, email: string, telephone: string }} body
 */
export async function updateMyProfile(body) {
  const resp = await fetch('/user/profile', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({
      realName: body.realName ?? '',
      email: body.email ?? '',
      telephone: body.telephone ?? '',
    }),
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200) {
    throw new Error(json?.message || '编辑资料失败')
  }
  return json.message != null ? String(json.message) : '保存成功'
}

/**
 * 修改登录密码（需 Token）
 * PUT /user/password
 * @param {{ currentPassword: string, newPassword: string, confirmPassword: string }} body
 */
export async function updatePassword(body) {
  const resp = await fetch('/user/password', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({
      currentPassword: body.currentPassword ?? '',
      newPassword: body.newPassword ?? '',
      confirmPassword: body.confirmPassword ?? '',
    }),
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200) {
    throw new Error(json?.message || '修改密码失败')
  }
  return json.message != null ? String(json.message) : '修改成功'
}

/**
 * 修改二级密码（需 Token）
 * PUT /user/second-password
 * @param {{ currentSecondPassword: string, newSecondPassword: string, confirmSecondPassword: string }} body
 */
export async function updateSecondPassword(body) {
  const resp = await fetch('/user/second-password', {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({
      currentSecondPassword: body.currentSecondPassword ?? '',
      newSecondPassword: body.newSecondPassword ?? '',
      confirmSecondPassword: body.confirmSecondPassword ?? '',
    }),
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200) {
    throw new Error(json?.message || '修改二级密码失败')
  }
  return json.message != null ? String(json.message) : '修改成功'
}
