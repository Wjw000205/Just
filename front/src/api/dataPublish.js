/**
 * 数据发布页相关接口（与 vite 代理 /api -> 后端一致）
 */

function getAuthHeader() {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token) return {}
  return { Authorization: `Bearer ${token}` }
}

/** @typedef {{ label: string, value: string }} OptionItem */
/** @typedef {{ label: string, value: string, kind?: 'high' | 'public' }} DataLevelOption */

/**
 * 将后端单项规范化为 { label, value }
 * @param {string | { label?: string, value?: string }} raw
 * @returns {OptionItem}
 */
function normalizeOption(raw) {
  if (typeof raw === 'string') {
    return { label: raw, value: raw }
  }
  const value = raw.value != null ? String(raw.value) : ''
  const label = raw.label != null ? String(raw.label) : value
  return { label, value: value || label }
}

/**
 * @param {string | { label?: string, value?: string, kind?: string }} raw
 * @returns {DataLevelOption}
 */
function normalizeDataLevelOption(raw) {
  if (typeof raw === 'string') {
    const isHigh = raw === '高值'
    return {
      label: raw,
      value: raw,
      kind: isHigh ? 'high' : 'public',
    }
  }
  const opt = normalizeOption(raw)
  let kind = raw.kind
  if (kind !== 'high' && kind !== 'public') {
    if (opt.label === '高值' || opt.value === 'high') kind = 'high'
    else if (opt.label === '公益' || opt.value === 'public') kind = 'public'
    else kind = 'public'
  }
  return { ...opt, kind }
}

/**
 * 一次性拉取数据发布页筛选区四个下拉框的选项。
 * GET /api/publish/filter-options
 *
 * @returns {Promise<{
 *   industryCategories: OptionItem[],
 *   auditStatuses: OptionItem[],
 *   departments: OptionItem[],
 *   dataLevels: DataLevelOption[],
 * }>}
 */
export async function fetchPublishFilterOptions() {
  const resp = await fetch('/api/publish/filter-options', {
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
    throw new Error(json?.message || '加载筛选项失败')
  }
  const d = json.data
  return {
    industryCategories: Array.isArray(d.industryCategories)
      ? d.industryCategories.map(normalizeOption)
      : [],
    auditStatuses: Array.isArray(d.auditStatuses)
      ? d.auditStatuses.map(normalizeOption)
      : [],
    departments: Array.isArray(d.departments)
      ? d.departments.map(normalizeOption)
      : [],
    dataLevels: Array.isArray(d.dataLevels)
      ? d.dataLevels.map(normalizeDataLevelOption)
      : [],
  }
}

/**
 * 将列表行规范化为页面表格结构
 * @param {Record<string, unknown>} raw
 */
function normalizePublishRow(raw) {
  const sci = raw.sciCategories ?? raw.scienceCategories
  const ind = raw.industryTags ?? raw.industryLabels
  const codes = raw.productCodes
  const levelsRaw = raw.dataLevels
  const dataLevels = (Array.isArray(levelsRaw) ? levelsRaw : []).map((lv) => {
    if (typeof lv === 'string') {
      return {
        text: lv,
        kind: lv === '高值' ? 'high' : 'public',
      }
    }
    const text =
      lv.text != null ? String(lv.text) : lv.label != null ? String(lv.label) : ''
    let kind = lv.kind
    if (kind !== 'high' && kind !== 'public') {
      kind = text === '高值' ? 'high' : 'public'
    }
    return { text, kind }
  })
  return {
    id: raw.id,
    // 与后端可能字段名兼容：用于表格展示（例如你截图中的“模板名称/数据类型/所属部门/创建人/创建时间/最近更新时间”）
    templateName:
      raw.templateName != null
        ? String(raw.templateName)
        : raw.datasetName != null
          ? String(raw.datasetName)
          : raw.name != null
            ? String(raw.name)
            : '',
    dataType:
      raw.dataType != null
        ? String(raw.dataType)
        : raw.datasetType != null
          ? String(raw.datasetType)
          : raw.type != null
            ? String(raw.type)
            : raw.dataCategory != null
              ? String(raw.dataCategory)
              : '',
    department:
      raw.department != null
        ? String(raw.department)
        : raw.deptName != null
          ? String(raw.deptName)
          : raw.departmentName != null
            ? String(raw.departmentName)
            : '',
    creator:
      raw.creator != null
        ? String(raw.creator)
        : raw.createdBy != null
          ? String(raw.createdBy)
          : raw.createUser != null
            ? String(raw.createUser)
            : raw.userName != null
              ? String(raw.userName)
              : '',
    createTime:
      raw.createTime != null
        ? String(raw.createTime)
        : raw.createdAt != null
          ? String(raw.createdAt)
          : raw.create_time != null
            ? String(raw.create_time)
            : '',
    updateTime:
      raw.updateTime != null
        ? String(raw.updateTime)
        : raw.updatedAt != null
          ? String(raw.updatedAt)
          : raw.lastUpdatedAt != null
            ? String(raw.lastUpdatedAt)
            : raw.last_update_time != null
              ? String(raw.last_update_time)
              : '',
    dataCount:
      raw.dataCount != null
        ? raw.dataCount
        : raw.dataQuantity != null
          ? raw.dataQuantity
          : raw.data_num != null
            ? raw.data_num
            : raw.dataNumber != null
              ? raw.dataNumber
              : '',
    status:
      raw.status != null
        ? String(raw.status)
        : raw.auditStatusLabel != null
          ? String(raw.auditStatusLabel)
          : raw.auditStatusText != null
            ? String(raw.auditStatusText)
            : raw.auditStatus != null
              ? String(raw.auditStatus)
              : raw.publishStatus != null
                ? String(raw.publishStatus)
                : '',
    // 审核统计（对应表格列：待审核/已通过/未通过/已通过比例）
    pendingCount:
      raw.pendingCount != null
        ? raw.pendingCount
        : raw.waitingAuditCount != null
          ? raw.waitingAuditCount
          : raw.pending != null
            ? raw.pending
            : 0,
    passedCount:
      raw.passedCount != null
        ? raw.passedCount
        : raw.approvedCount != null
          ? raw.approvedCount
          : raw.passed != null
            ? raw.passed
            : 0,
    rejectedCount:
      raw.rejectedCount != null
        ? raw.rejectedCount
        : raw.deniedCount != null
          ? raw.deniedCount
          : raw.rejected != null
            ? raw.rejected
            : 0,
    passedRatio:
      raw.passedRatio != null
        ? raw.passedRatio
        : raw.approvedRatio != null
          ? raw.approvedRatio
          : raw.approvedPercent != null
            ? raw.approvedPercent
            : raw.passedPercent != null
              ? raw.passedPercent
              : raw.approved_ratio != null
                ? raw.approved_ratio
                : raw.passed_ratio != null
                  ? raw.passed_ratio
                  : raw.passRatio != null
                    ? raw.passRatio
                    : '',
    summary:
      raw.summary != null
        ? String(raw.summary)
        : raw.abstract != null
          ? String(raw.abstract)
          : raw.description != null
            ? String(raw.description)
            : '',
    auditor:
      raw.auditor != null
        ? String(raw.auditor)
        : raw.auditUser != null
          ? String(raw.auditUser)
          : raw.auditPerson != null
            ? String(raw.auditPerson)
            : raw.auditBy != null
              ? String(raw.auditBy)
              : '',
    auditTime:
      raw.auditTime != null
        ? String(raw.auditTime)
        : raw.auditAt != null
          ? String(raw.auditAt)
          : raw.recentAuditTime != null
            ? String(raw.recentAuditTime)
            : raw.lastAuditTime != null
              ? String(raw.lastAuditTime)
              : raw.last_audit_time != null
                ? String(raw.last_audit_time)
                : '',
    prePublishTime:
      raw.prePublishTime != null
        ? String(raw.prePublishTime)
        : raw.preReleaseTime != null
          ? String(raw.preReleaseTime)
          : raw.pre_publish_time != null
            ? String(raw.pre_publish_time)
            : raw.publishTime != null
              ? String(raw.publishTime)
              : '',
    dataIdentifier:
      raw.dataIdentifier != null
        ? String(raw.dataIdentifier)
        : raw.dataFlag != null
          ? String(raw.dataFlag)
          : raw.dataIdFlag != null
            ? String(raw.dataIdFlag)
            : raw.identifier != null
              ? String(raw.identifier)
              : raw.data_tag != null
                ? String(raw.data_tag)
                : '',
    favorited: Boolean(raw.favorited ?? raw.isFavorited ?? raw.favorite ?? raw.starred ?? false),
    datasetName:
      raw.datasetName != null
        ? String(raw.datasetName)
        : raw.name != null
          ? String(raw.name)
          : '',
    sciCategories: Array.isArray(sci) ? sci.map(String) : [],
    industryTags: Array.isArray(ind) ? ind.map(String) : [],
    productCodes: Array.isArray(codes) ? codes.map(String) : [],
    dataLevels,
    selected: false,
  }
}

/**
 * 分页查询数据发布列表（筛选条件与页面表单一致）。
 * POST /api/publish/datasets
 *
 * @param {{
 *   datasetName?: string,
 *   industryCategory?: string,
 *   auditStatus?: string,
 *   templateName?: string,
 *   department?: string,
 *   dataLevel?: string,
 *   scienceCategoryScope?: 'all' | 'sci' | 'industry',
 *   categoryKeyword?: string,
 *   page?: number,
 *   pageSize?: number,
 * }} params
 * @returns {Promise<{ total: number, list: ReturnType<typeof normalizePublishRow>[] }>}
 */
export async function fetchPublishDatasetList(params) {
  const body = {
    datasetName: params.datasetName ?? '',
    industryCategory: params.industryCategory ?? '',
    auditStatus: params.auditStatus ?? '',
    templateName: params.templateName ?? '',
    department: params.department ?? '',
    dataLevel: params.dataLevel ?? '',
    scienceCategoryScope: params.scienceCategoryScope ?? 'all',
    categoryKeyword: params.categoryKeyword ?? '',
    page: params.page ?? 1,
    pageSize: params.pageSize ?? 10,
  }
  const resp = await fetch('/api/publish/datasets', {
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
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '查询失败')
  }
  const d = json.data
  const total =
    typeof d.total === 'number'
      ? d.total
      : typeof d.totalCount === 'number'
        ? d.totalCount
        : 0
  const rawList = Array.isArray(d.list)
    ? d.list
    : Array.isArray(d.records)
      ? d.records
      : []
  return {
    total,
    list: rawList.map((row) => normalizePublishRow(row)),
  }
}

/**
 * 获取某条数据发布详情（用于“查看”按钮）
 * GET /api/publish/datasets/{id}
 *
 * @param {string|number} id
 * @returns {Promise<any>}
 */
export async function fetchPublishDatasetDetail(id) {
  const resp = await fetch(`/api/publish/datasets/${encodeURIComponent(String(id))}`, {
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
  if (!json || json.code !== 200) {
    throw new Error(json?.message || '查询详情失败')
  }
  return json.data
}

/**
 * 下载某条数据发布资源（用于“下载”按钮）
 * POST /api/publish/datasets/{id}/download
 *
 * @param {string|number} id
 * @returns {Promise<{ downloadUrl: string, fileName?: string, mimeType?: string }>}
 */
export async function downloadPublishDataset(id) {
  const resp = await fetch(`/api/publish/datasets/${encodeURIComponent(String(id))}/download`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({}),
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '下载链接获取失败')
  }
  return json.data
}

/**
 * 收藏/取消收藏某条数据发布项（用于“收藏”按钮）
 * POST /api/publish/datasets/{id}/favorite
 *
 * @param {string|number} id
 * @param {boolean} favorited
 * @returns {Promise<{ favorited: boolean }>}
 */
export async function setPublishDatasetFavorite(id, favorited) {
  const resp = await fetch(`/api/publish/datasets/${encodeURIComponent(String(id))}/favorite`, {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({ favorited: Boolean(favorited) }),
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '收藏操作失败')
  }
  return json.data
}

/**
 * 批量预发布（用于顶部“预发布”按钮）
 * POST /api/publish/datasets/prepublish/batch
 *
 * @param {Array<string|number>} ids
 * @returns {Promise<{ successCount: number, failCount: number, failedIds?: Array<string|number> }>}
 */
export async function batchPrePublishDatasets(ids) {
  const resp = await fetch('/api/publish/datasets/prepublish/batch', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      ...getAuthHeader(),
    },
    body: JSON.stringify({ ids }),
  })
  const json = await resp.json().catch(() => null)
  if (resp.status === 401 || json?.code === 401) {
    throw new Error(json?.message || '未登录或无权限')
  }
  if (!json || json.code !== 200 || !json.data) {
    throw new Error(json?.message || '预发布失败')
  }
  return json.data
}
