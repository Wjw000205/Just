/**
 * 数据集相关接口
 */

function getAuthHeader() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

/**
 * 获取我创建的数据集（从 token 识别当前用户）
 * GET /api/datasets/my
 * @returns {Promise<Array>}
 */
export async function fetchMyDatasets() {
  const resp = await fetch('/api/datasets/my', {
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
  if (!json || json.code !== 0) {
    throw new Error(json?.message || '获取我的数据集失败')
  }

  return Array.isArray(json.data) ? json.data : []
}
