/**
 * 科学分类管理 / 模板目录 API（与 App.vue 中后端地址一致）
 */

const API_BASE_URL = 'http://localhost:8083'

function getAuthHeader() {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

/**
 * 查询模板目录树（GET /Dataset/getManuList）
 * @returns {Promise<{ code: number, message: string, data: Array<Record<string, unknown>> }>}
 */
/**
 * 创建模板目录（POST /Dataset/create-menu）
 * @param {{ name: string, creator: string, parent?: string }} payload
 */
export async function createMenu(payload) {
  const body = {
    name: String(payload.name || '').trim(),
    creator: String(payload.creator || '').trim(),
  }
  const p = payload.parent != null ? String(payload.parent).trim() : ''
  if (p) {
    body.parent = p
  }
  const resp = await fetch(`${API_BASE_URL}/Dataset/create-menu`, {
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
    throw new Error(json?.message || '创建目录失败')
  }
  if (!json || (json.code !== 200 && json.code !== 0)) {
    throw new Error(json?.message || '创建目录失败')
  }
  return json
}

export async function getManuList() {
  const resp = await fetch(`${API_BASE_URL}/Dataset/getManuList`, {
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
  if (!resp.ok) {
    throw new Error(json?.message || '获取模板目录树失败')
  }
  if (!json || (json.code !== 200 && json.code !== 0)) {
    throw new Error(json?.message || '获取模板目录树失败')
  }
  return json
}

/**
 * @deprecated 请使用 getManuList
 */
export function getCategoryTree() {
  return getManuList()
}

/**
 * 新增分类
 * @param {Object} data - 分类数据
 * @returns {Promise}
 */
export function addCategory(data) {
  return fetch(`${API_BASE_URL}/category`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    },
    body: JSON.stringify(data)
  }).then(response => {
    if (!response.ok) {
      throw new Error('新增分类失败')
    }
    return response.json()
  })
}

/**
 * 批量新增分类
 * @param {Array} dataList - 分类数据数组
 * @returns {Promise}
 */
export function batchAddCategory(dataList) {
  return fetch(`${API_BASE_URL}/category/batch`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    },
    body: JSON.stringify(dataList)
  }).then(response => {
    if (!response.ok) {
      throw new Error('批量新增分类失败')
    }
    return response.json()
  })
}

/**
 * 更新分类
 * @param {number} id - 分类ID
 * @param {Object} data - 分类数据
 * @returns {Promise}
 */
export function updateCategory(id, data) {
  return fetch(`${API_BASE_URL}/category/${id}`, {
    method: 'PUT',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    },
    body: JSON.stringify(data)
  }).then(response => {
    if (!response.ok) {
      throw new Error('更新分类失败')
    }
    return response.json()
  })
}

/**
 * 删除分类
 * @param {number} id - 分类ID
 * @returns {Promise}
 */
export function deleteCategory(id) {
  return fetch(`${API_BASE_URL}/category/${id}`, {
    method: 'DELETE',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    }
  }).then(response => {
    if (!response.ok) {
      throw new Error('删除分类失败')
    }
    return response.json()
  })
}

/**
 * 从外部系统获取目录
 * @returns {Promise}
 */
export function fetchCatalogFromExternal() {
  return fetch(`${API_BASE_URL}/category/fetch-external`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    }
  }).then(response => {
    if (!response.ok) {
      throw new Error('获取外部目录失败')
    }
    return response.json()
  })
}

/**
 * 获取分类详情
 * @param {number} id - 分类ID
 * @returns {Promise}
 */
export function getCategoryDetail(id) {
  return fetch(`${API_BASE_URL}/category/${id}`, {
    method: 'GET',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${localStorage.getItem('token') || ''}`
    }
  }).then(response => {
    if (!response.ok) {
      throw new Error('获取分类详情失败')
    }
    return response.json()
  })
}
