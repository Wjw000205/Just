/**
 * 模板（Module）相关接口
 */
const API_BASE_URL = 'http://localhost:8083'

function getAuthHeader() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

function toInt(v) {
  const n = Number(v)
  if (!Number.isFinite(n)) return null
  return Math.trunc(n)
}

/**
 * 新建 module：POST /module/create
 * @param {{
 *   moduleName: string,
 *   tag: string,
 *   description?: string,
 *   creator: string,
 *   visibleArea: number|string,
 *   agree: number|string
 * }} payload
 */
export async function createModule(payload) {
  const body = {
    moduleName: String(payload?.moduleName ?? '').trim(),
    tag: String(payload?.tag ?? '').trim(),
    description: payload?.description != null ? String(payload.description) : undefined,
    creator: String(payload?.creator ?? '').trim(),
    visibleArea: toInt(payload?.visibleArea),
    agree: toInt(payload?.agree),
  }

  const resp = await fetch(`${API_BASE_URL}/module/create`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify(body),
  })

  const json = await resp.json().catch(() => null)

  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!resp.ok) {
    throw new Error(json?.message || '新建 module 失败')
  }
  return json
}

/**
 * 保存模板设计：POST /module/design
 * @param {{
 *   moduleId: number,
 *   object: Array<{ columnName: string, type: string }>,
 *   operation: Array<{ columnName: string, type: string }>,
 *   result: Array<{ columnName: string, type: string }>
 * }} payload
 */
export async function designModule(payload) {
  const body = {
    moduleId: toInt(payload?.moduleId),
    object: Array.isArray(payload?.object) ? payload.object : [],
    operation: Array.isArray(payload?.operation) ? payload.operation : [],
    result: Array.isArray(payload?.result) ? payload.result : [],
  }

  const resp = await fetch(`${API_BASE_URL}/module/design`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify(body),
  })

  const json = await resp.json().catch(() => null)

  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!resp.ok) {
    throw new Error(json?.message || '保存模板设计失败')
  }
  return json
}

/**
 * 获取我的模板列表：GET /module/my-module?creatorId=
 * @param {number|string} creatorId
 */
export async function getMyModules(creatorId) {
  const id = toInt(creatorId)
  if (!id) {
    throw new Error('creatorId 无效')
  }

  const resp = await fetch(
    `${API_BASE_URL}/module/my-module?creatorId=${encodeURIComponent(String(id))}`,
    {
      method: 'GET',
      headers: {
        'Content-Type': 'application/json',
        ...getAuthHeader(),
      },
    },
  )

  const json = await resp.json().catch(() => null)

  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!resp.ok) {
    throw new Error(json?.message || '获取我的模板失败')
  }
  return json
}


