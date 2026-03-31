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

/**
 * @param {any} raw
 * @returns {{ id: string, label: string, children: any[] }}
 */
function normalizeSidebarTreeNode(raw, depth = 0) {
  const id =
    raw?.id != null
      ? String(raw.id)
      : `tmp-${depth}-${String(raw?.label ?? raw?.name ?? '')}`
  const label =
    raw?.label != null ? String(raw.label) : raw?.name != null ? String(raw.name) : id
  const children = Array.isArray(raw?.children)
    ? raw.children.map((c, i) => normalizeSidebarTreeNode(c, depth + 1 + i))
    : []
  return { id, label, children }
}

function normalizeDataLevel(raw) {
  if (raw == null) return { text: '', kind: 'public' }
  if (typeof raw === 'object' && raw.text != null) {
    const text = String(raw.text)
    const kind =
      raw.kind === 'high' || text === '高值' ? 'high' : 'public'
    return { text, kind }
  }
  const text = String(raw)
  const kind = text === '高值' ? 'high' : 'public'
  return { text, kind }
}

function parseStringArray(v) {
  if (!v) return []
  if (Array.isArray(v)) return v.map(String)
  return [String(v)]
}

/**
 * 列表行规范化（与 DatabasePage 表格列一致）
 * @param {any} raw
 */
export function normalizeDatabaseRow(raw) {
  const sci = raw.scienceCategories ?? raw.sciCategories ?? raw.scienceCategory
  const ind = raw.industryCategories ?? raw.industryTags ?? raw.industryCategory
  const codes = raw.productCodes ?? raw.codes ?? raw.productCode
  return {
    id: raw.id,
    datasetName:
      raw.datasetName != null
        ? String(raw.datasetName)
        : raw.name != null
          ? String(raw.name)
          : '',
    scienceCategories: parseStringArray(sci),
    industryCategories: parseStringArray(ind),
    productCodes: Array.isArray(codes)
      ? codes.map(String)
      : codes != null && codes !== ''
        ? [String(codes)]
        : [],
    dataLevel: normalizeDataLevel(raw.dataLevel ?? raw.level),
    dataCount:
      raw.dataCount ?? raw.dataQuantity ?? raw.data_num ?? raw.count ?? '',
    templateName:
      raw.templateName != null ? String(raw.templateName) : '',
    dataCategory:
      raw.dataCategory != null
        ? String(raw.dataCategory)
        : raw.datasetType != null
          ? String(raw.datasetType)
          : '',
    department:
      raw.department != null
        ? String(raw.department)
        : raw.deptName != null
          ? String(raw.deptName)
          : '',
    creator:
      raw.creator != null
        ? String(raw.creator)
        : raw.createdBy != null
          ? String(raw.createdBy)
          : '',
    createTime:
      raw.createTime != null ? String(raw.createTime) : raw.createdAt != null ? String(raw.createdAt) : '',
    updateTime:
      raw.updateTime != null ? String(raw.updateTime) : raw.updatedAt != null ? String(raw.updatedAt) : '',
    favorited: Boolean(raw.favorited ?? raw.isFavorited ?? false),
  }
}

function parseDatasetListPayload(d) {
  const total = d?.total != null ? Number(d.total) : 0
  const list = Array.isArray(d?.list) ? d.list.map(normalizeDatabaseRow) : []
  return { total, list }
}

/**
 * 数据库页首次进入：聚合返回左侧树 + 筛选项 + 首屏列表
 * POST /api/database/page/init
 *
 * @param {{ page?: number, pageSize?: number } & Record<string, any>} body
 */
export async function fetchDatabasePageInit(body = {}) {
  const resp = await fetch('/api/database/page/init', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({
      page: body.page ?? 1,
      pageSize: body.pageSize ?? 10,
      ...body,
    }),
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '加载数据库页失败')
  }

  const d = json.data
  const treeRaw = d.tree ?? d.sidebarTree ?? []
  const tree = Array.isArray(treeRaw) ? treeRaw.map((n) => normalizeSidebarTreeNode(n)) : []

  const fo = d.filterOptions ?? d
  const industryCategories = Array.isArray(fo.industryCategories)
    ? fo.industryCategories.map(normalizeOption)
    : []
  const dataCategories = Array.isArray(fo.dataCategories)
    ? fo.dataCategories.map(normalizeOption)
    : []
  const departments = Array.isArray(fo.departments) ? fo.departments.map(normalizeOption) : []

  const { total, list } = parseDatasetListPayload(d)

  return {
    tree,
    industryCategories,
    dataCategories,
    departments,
    total,
    list,
  }
}

/**
 * 列表查询（筛选、侧栏节点、侧栏搜索、分页）
 * POST /api/database/datasets/query
 *
 * @param {Record<string, any>} body
 */
export async function queryDatabaseDatasets(body) {
  const resp = await fetch('/api/database/datasets/query', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify(body ?? {}),
  })

  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '查询列表失败')
  }
  return parseDatasetListPayload(json.data)
}

/**
 * 获取“部分字段下载”的可选字段（由后端决定）
 * GET /api/database/datasets/{id}/download-fields
 *
 * @param {string|number} id
 * @returns {Promise<OptionItem[]>}
 */
export async function fetchDatabaseDownloadFields(id) {
  const resp = await fetch(`/api/database/datasets/${encodeURIComponent(String(id))}/download-fields`, {
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
    throw new Error(json?.message || '加载下载字段失败')
  }

  const d = json.data
  // 兼容后端直接返回数组或包一层 fields
  const fields = Array.isArray(d) ? d : Array.isArray(d.fields) ? d.fields : []
  return fields.map(normalizeOption)
}

function parseFilenameFromContentDisposition(cd) {
  if (!cd) return ''
  // filename*=UTF-8''xxx
  const mStar = /filename\*\s*=\s*UTF-8''([^;]+)/i.exec(cd)
  if (mStar && mStar[1]) {
    try {
      return decodeURIComponent(mStar[1].trim().replace(/^"|"$/g, ''))
    } catch (_) {
      return mStar[1].trim().replace(/^"|"$/g, '')
    }
  }
  const m = /filename\s*=\s*([^;]+)/i.exec(cd)
  if (m && m[1]) return m[1].trim().replace(/^"|"$/g, '')
  return ''
}

/**
 * 直接下载文件（方案A：后端返回文件流）
 * POST /api/database/datasets/{id}/download
 *
 * @param {string|number} id
 * @param {{
 *   fieldMode: 'all'|'partial',
 *   fields: string[],
 *   fileType: 'json'|'xlsx',
 *   dataShape: 'nested'|'flat'
 * }} body
 * @returns {Promise<{ blob: Blob, filename: string, contentType: string }>}
 */
export async function downloadDatabaseDatasetFile(id, body) {
  const resp = await fetch(`/api/database/datasets/${encodeURIComponent(String(id))}/download`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify(body ?? {}),
  })

  if (resp.status === 401) {
    // 兼容后端返回 json 的 401
    const json = await resp.json().catch(() => null)
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!resp.ok) {
    const text = await resp.text().catch(() => '')
    throw new Error(text || `下载失败（HTTP ${resp.status}）`)
  }
  const blob = await resp.blob()
  const contentType = resp.headers.get('content-type') || ''
  const cd = resp.headers.get('content-disposition') || ''
  const filename = parseFilenameFromContentDisposition(cd)
  return { blob, filename, contentType }
}

