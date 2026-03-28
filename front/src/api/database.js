/**
 * 数据库页面相关接口（与 Vite proxy 的 /api -> 后端一致）
 */

function getAuthHeader() {
  const token = localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

/**
 * @typedef {{ label: string, value: string }} OptionItem
 */

/**
 * @param {any} raw
 * @returns {OptionItem}
 */
function normalizeOption(raw) {
  if (typeof raw === 'string') return { label: raw, value: raw }
  const value = raw?.value != null ? String(raw.value) : ''
  const label = raw?.label != null ? String(raw.label) : value
  return { label, value: value || label }
}

/**
 * 数据库筛选下拉框选项拉取（聚合接口）
 * GET /api/database/filter-options
 *
 * @returns {Promise<{
 *   industryCategories: OptionItem[],
 *   dataCategories: OptionItem[],
 *   departments: OptionItem[]
 * }>}
 */
export async function fetchDatabaseFilterOptions() {
  const resp = await fetch('/api/database/filter-options', {
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
    throw new Error(json?.message || '加载下拉选项失败')
  }

  const d = json.data
  return {
    industryCategories: Array.isArray(d.industryCategories) ? d.industryCategories.map(normalizeOption) : [],
    dataCategories: Array.isArray(d.dataCategories) ? d.dataCategories.map(normalizeOption) : [],
    departments: Array.isArray(d.departments) ? d.departments.map(normalizeOption) : [],
  }
}

