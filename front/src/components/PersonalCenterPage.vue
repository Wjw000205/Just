<template>
  <section class="personal-center-page">
    <div class="pc-layout">
      <!-- 左侧边栏 -->
      <aside class="pc-sidebar">
        <div class="pc-user-info">
          <div class="pc-avatar">
            <img v-if="userProfile.avatar" :src="userProfile.avatar" alt="头像" />
            <svg v-else width="48" height="48" viewBox="0 0 24 24" fill="none" stroke="#1a5ce6" stroke-width="1.5">
              <circle cx="12" cy="8" r="4" />
              <path d="M6 21v-2a4 4 0 0 1 4-4h4a4 4 0 0 1 4 4v2" />
            </svg>
          </div>
          <div class="pc-user-name">您好，{{ userProfile.realName || currentUserName || '用户' }}</div>
        </div>

        <div class="pc-quick-actions">
          <button class="quick-btn" @click="currentMenu = 'edit-profile'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/>
              <path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/>
            </svg>
            完善资料
          </button>
          <button class="quick-btn" @click="currentMenu = 'change-password'">
            <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
              <rect x="3" y="11" width="18" height="11" rx="2" ry="2"/>
              <circle cx="12" cy="16" r="1"/>
              <path d="M7 11V7a5 5 0 0 1 10 0v4"/>
            </svg>
            修改密码
          </button>
        </div>

        <ul class="pc-menu">
          <li
            v-for="menu in menuList"
            :key="menu.key"
            class="pc-menu-item"
            :class="{ active: currentMenu === menu.key }"
            @click="currentMenu = menu.key"
          >
            {{ menu.label }}
          </li>
        </ul>
      </aside>

      <!-- 主内容区 -->
      <main class="pc-main">
        <!-- ==================== 我的数据集 ==================== -->
        <template v-if="currentMenu === 'datasets'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">我的数据集</span>
          </div>
          <div class="pc-title">我的数据集</div>

          <!-- 搜索筛选 -->
          <div class="search-filter-area">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">数据集名称</label>
                <input v-model="datasetSearchForm.name" class="filter-input" placeholder="请输入" />
              </div>
              <div class="filter-item">
                <label class="filter-label">科学分类</label>
                <div class="filter-select" @click.stop="toggleDropdown('scientific')">
                  <span class="filter-select-text">{{ datasetSearchForm.scientific || '请选择分类' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.scientific" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectOption('scientific', '')">请选择分类</li>
                    <li class="filter-select-item" @click.stop="selectOption('scientific', '材料基因工程')">材料基因工程</li>
                    <li class="filter-select-item" @click.stop="selectOption('scientific', '生物医用材料')">生物医用材料</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item">
                <label class="filter-label">产业分类</label>
                <div class="filter-select" @click.stop="toggleDropdown('industry')">
                  <span class="filter-select-text">{{ datasetSearchForm.industry || '请选择产业分类' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.industry" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectOption('industry', '')">请选择产业分类</li>
                    <li class="filter-select-item" @click.stop="selectOption('industry', '钢铁产业')">钢铁产业</li>
                    <li class="filter-select-item" @click.stop="selectOption('industry', '新材料')">新材料</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="filter-row second-row">
              <div class="filter-item">
                <label class="filter-label">审核状态</label>
                <div class="filter-select" @click.stop="toggleDropdown('auditStatus')">
                  <span class="filter-select-text">{{ datasetSearchForm.auditStatus || '请选择审核状态' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.auditStatus" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectOption('auditStatus', '')">请选择审核状态</li>
                    <li class="filter-select-item" @click.stop="selectOption('auditStatus', '已完成')">已完成</li>
                    <li class="filter-select-item" @click.stop="selectOption('auditStatus', '待提交审核')">待提交审核</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item">
                <label class="filter-label">领域</label>
                <input v-model="datasetSearchForm.field" class="filter-input" placeholder="请输入" />
              </div>
              <div class="filter-item">
                <label class="filter-label">所属部门</label>
                <div class="filter-select" @click.stop="toggleDropdown('department')">
                  <span class="filter-select-text">{{ datasetSearchForm.department || '请选择所属部门' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.department" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectOption('department', '')">请选择所属部门</li>
                    <li class="filter-select-item" @click.stop="selectOption('department', '节点管理部')">节点管理部</li>
                    <li class="filter-select-item" @click.stop="selectOption('department', '社会大众')">社会大众</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="filter-row third-row">
              <div class="filter-item">
                <label class="filter-label">数据级别</label>
                <div class="filter-select" @click.stop="toggleDropdown('dataLevel')">
                  <span class="filter-select-text">{{ datasetSearchForm.dataLevel || '请选择数据级别' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.dataLevel" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectOption('dataLevel', '')">请选择数据级别</li>
                    <li class="filter-select-item" @click.stop="selectOption('dataLevel', '公开')">公开</li>
                    <li class="filter-select-item" @click.stop="selectOption('dataLevel', '内部')">内部</li>
                  </ul>
                </div>
              </div>
              <div class="filter-buttons">
                <button class="filter-btn primary" @click="handleDatasetSearch">查询</button>
                <button class="filter-btn ghost" @click="handleDatasetReset">重置</button>
              </div>
            </div>
          </div>

          <!-- 操作按钮 -->
          <div class="dataset-action-area">
            <div class="left-actions">
              <button class="action-btn ghost" @click="handleBatchDownload">
                <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                  <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                  <polyline points="7 10 12 15 17 10"/>
                  <line x1="12" y1="15" x2="12" y2="3"/>
                </svg>
                批量下载
              </button>
              <button class="action-btn ghost" @click="handleCustomColumns">自定义列</button>
            </div>
            <button class="action-btn primary" @click="handleAddDataset">新增</button>
          </div>

          <!-- 表格 -->
          <div class="pc-table-area">
            <table class="data-table dataset-table">
              <thead>
                <tr>
                  <th class="col-checkbox"><input type="checkbox" v-model="selectAll" @change="handleSelectAll" /></th>
                  <th class="col-index">序号</th>
                  <th class="col-dataset-name">数据集名称</th>
                  <th class="col-level">数据级别</th>
                  <th class="col-data-count">数据量</th>
                  <th class="col-row-count">数据行</th>
                  <th class="col-audit-status">审核状态</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in datasetList" :key="item.id">
                  <td class="col-checkbox"><input type="checkbox" v-model="item.selected" /></td>
                  <td class="col-index">{{ index + 1 }}</td>
                  <td class="col-dataset-name" :title="item.name">{{ item.name }}</td>
                  <td class="col-level">
                    <span :class="['level-tag', item.level === '公开' ? 'level-public' : 'level-private']">{{ item.level }}</span>
                  </td>
                  <td class="col-data-count">{{ item.dataCount }}</td>
                  <td class="col-row-count">{{ item.rowCount }}</td>
                  <td class="col-audit-status">
                    <span :class="['audit-tag', item.auditStatus === '已完成' ? 'audit-done' : 'audit-pending']">{{ item.auditStatus }}</span>
                  </td>
                  <td class="col-action">
                    <div class="action-icons">
                      <button class="icon-btn view" @click="handleViewDataset(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></button>
                      <button class="icon-btn edit" @click="handleEditDataset(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
                      <button class="icon-btn download" @click="handleDownloadDataset(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg></button>
                      <button class="icon-btn delete" @click="handleDeleteDataset(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <span class="total">共 {{ datasetTotal }} 条</span>
            <button class="page-btn" :disabled="datasetCurrentPage === 1" @click="datasetCurrentPage--">上一页</button>
            <span class="page-current">{{ datasetCurrentPage }}</span>
            <button class="page-btn" :disabled="datasetCurrentPage >= datasetTotalPages" @click="datasetCurrentPage++">下一页</button>
          </div>
        </template>

        <!-- ==================== 我的模板 ==================== -->
        <template v-if="currentMenu === 'templates'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">我的模板</span>
          </div>
          <div class="pc-title">我的模板</div>

          <!-- 搜索筛选 -->
          <div class="search-filter-area">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">模板名称</label>
                <input v-model="templateSearchForm.name" class="filter-input" placeholder="请输入" />
              </div>
              <div class="filter-item">
                <label class="filter-label">模板标签</label>
                <div class="filter-select" @click.stop="toggleDropdown('templateTag')">
                  <span class="filter-select-text">{{ templateSearchForm.tag || '请选择分类' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.templateTag" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectTemplateOption('tag', '')">请选择分类</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('tag', '生物医用材料')">生物医用材料</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('tag', '材料基因工程')">材料基因工程</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item">
                <label class="filter-label">可见范围</label>
                <div class="filter-select" @click.stop="toggleDropdown('templateScope')">
                  <span class="filter-select-text">{{ templateSearchForm.scope || '请选择可见范围' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.templateScope" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectTemplateOption('scope', '')">请选择可见范围</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('scope', '公开')">公开</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('scope', '内部')">内部</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item">
                <label class="filter-label">审核状态</label>
                <div class="filter-select" @click.stop="toggleDropdown('templateAudit')">
                  <span class="filter-select-text">{{ templateSearchForm.audit || '请选择状态' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.templateAudit" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectTemplateOption('audit', '')">请选择状态</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('audit', '已通过')">已通过</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('audit', '待审核')">待审核</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="filter-row second-row">
              <div class="filter-item">
                <label class="filter-label">模板状态</label>
                <div class="filter-select" @click.stop="toggleDropdown('templateStatus')">
                  <span class="filter-select-text">{{ templateSearchForm.status || '请选择模板状态' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.templateStatus" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectTemplateOption('status', '')">请选择模板状态</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('status', '正常')">正常</li>
                    <li class="filter-select-item" @click.stop="selectTemplateOption('status', '停用')">停用</li>
                  </ul>
                </div>
              </div>
              <div class="filter-buttons">
                <button class="filter-btn primary" @click="handleTemplateSearch">查询</button>
                <button class="filter-btn ghost" @click="handleTemplateReset">重置</button>
              </div>
            </div>
          </div>

          <!-- 新增按钮 -->
          <div class="action-bar">
            <button class="action-btn primary" @click="handleAddTemplate">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              新增
            </button>
          </div>

          <!-- 模板表格 -->
          <div class="pc-table-area">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-index">序号</th>
                  <th class="col-tpl-name">模板名称</th>
                  <th class="col-tpl-tag">模板标签</th>
                  <th class="col-tpl-scope">可见范围</th>
                  <th class="col-tpl-desc">模板说明</th>
                  <th class="col-tpl-time">创建时间</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in templateData" :key="item.id">
                  <td class="col-index">{{ index + 1 }}</td>
                  <td class="col-tpl-name" :title="item.name">{{ item.name }}</td>
                  <td class="col-tpl-tag"><span class="tag-badge">{{ item.tag }}</span></td>
                  <td class="col-tpl-scope">{{ item.scope }}</td>
                  <td class="col-tpl-desc" :title="item.description">{{ item.description || '-' }}</td>
                  <td class="col-tpl-time">{{ item.createTime }}</td>
                  <td class="col-action">
                    <div class="action-icons">
                      <button class="icon-btn view" @click="handleViewTemplate(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></button>
                      <button class="icon-btn edit" @click="handleEditTemplate(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
                      <button class="icon-btn delete" @click="handleDeleteTemplate(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <span class="total">共 {{ templateTotal }} 条</span>
            <button class="page-btn" :disabled="templateCurrentPage === 1" @click="templateCurrentPage--">上一页</button>
            <span class="page-current">{{ templateCurrentPage }}</span>
            <button class="page-btn" :disabled="templateCurrentPage >= templateTotalPages" @click="templateCurrentPage++">下一页</button>
          </div>
        </template>

        <!-- ==================== 我的模板片段 ==================== -->
        <template v-if="currentMenu === 'fragments'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">我的模板片段</span>
          </div>
          <div class="pc-title">我的模板片段</div>

          <!-- 搜索筛选 -->
          <div class="search-filter-area">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">模板名称</label>
                <input v-model="fragmentSearchForm.name" class="filter-input" placeholder="输入模板名称" />
              </div>
              <div class="filter-item">
                <label class="filter-label">可见范围</label>
                <div class="filter-select" @click.stop="toggleDropdown('fragmentScope')">
                  <span class="filter-select-text">{{ fragmentSearchForm.scope || '请选择可见范围' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.fragmentScope" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectFragmentOption('scope', '')">请选择可见范围</li>
                    <li class="filter-select-item" @click.stop="selectFragmentOption('scope', '公开')">公开</li>
                    <li class="filter-select-item" @click.stop="selectFragmentOption('scope', '内部')">内部</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="filter-row second-row">
              <div class="filter-item">
                <label class="filter-label">审核状态</label>
                <div class="filter-select" @click.stop="toggleDropdown('fragmentAudit')">
                  <span class="filter-select-text">{{ fragmentSearchForm.audit || '请选择审核状态' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.fragmentAudit" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectFragmentOption('audit', '')">请选择审核状态</li>
                    <li class="filter-select-item" @click.stop="selectFragmentOption('audit', '已通过')">已通过</li>
                    <li class="filter-select-item" @click.stop="selectFragmentOption('audit', '待审核')">待审核</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item">
                <label class="filter-label">模板片段状态</label>
                <div class="filter-select" @click.stop="toggleDropdown('fragmentStatus')">
                  <span class="filter-select-text">{{ fragmentSearchForm.status || '请选择模板状态' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.fragmentStatus" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectFragmentOption('status', '')">请选择模板状态</li>
                    <li class="filter-select-item" @click.stop="selectFragmentOption('status', '正常')">正常</li>
                    <li class="filter-select-item" @click.stop="selectFragmentOption('status', '停用')">停用</li>
                  </ul>
                </div>
              </div>
              <div class="filter-buttons">
                <button class="filter-btn primary" @click="handleFragmentSearch">查询</button>
                <button class="filter-btn ghost" @click="handleFragmentReset">重置</button>
              </div>
            </div>
          </div>

          <!-- 新增按钮 -->
          <div class="action-bar">
            <button class="action-btn primary" @click="handleAddFragment">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="12" y1="5" x2="12" y2="19"></line>
                <line x1="5" y1="12" x2="19" y2="12"></line>
              </svg>
              新增
            </button>
          </div>

          <!-- 模板片段表格 -->
          <div class="pc-table-area">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-index">序号</th>
                  <th class="col-frag-name">模板名称</th>
                  <th class="col-frag-scope">可见范围</th>
                  <th class="col-frag-desc">模板说明</th>
                  <th class="col-frag-time">创建时间</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in fragmentData" :key="item.id">
                  <td class="col-index">{{ index + 1 }}</td>
                  <td class="col-frag-name" :title="item.name">{{ item.name }}</td>
                  <td class="col-frag-scope">{{ item.scope }}</td>
                  <td class="col-frag-desc" :title="item.description">{{ item.description || '-' }}</td>
                  <td class="col-frag-time">{{ item.createTime }}</td>
                  <td class="col-action">
                    <div class="action-icons">
                      <button class="icon-btn view" @click="handleViewFragment(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></button>
                      <button class="icon-btn edit" @click="handleEditFragment(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M11 4H4a2 2 0 0 0-2 2v14a2 2 0 0 0 2 2h14a2 2 0 0 0 2-2v-7"/><path d="M18.5 2.5a2.121 2.121 0 0 1 3 3L12 15l-4 1 1-4 9.5-9.5z"/></svg></button>
                      <button class="icon-btn delete" @click="handleDeleteFragment(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <span class="total">共 {{ fragmentTotal }} 条</span>
            <button class="page-btn" :disabled="fragmentCurrentPage === 1" @click="fragmentCurrentPage--">上一页</button>
            <span class="page-current">{{ fragmentCurrentPage }}</span>
            <button class="page-btn" :disabled="fragmentCurrentPage >= fragmentTotalPages" @click="fragmentCurrentPage++">下一页</button>
          </div>
        </template>

        <!-- ==================== 我的收藏 ==================== -->
        <template v-if="currentMenu === 'favorites'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">我的收藏</span>
          </div>
          <div class="pc-title">我的收藏</div>

          <!-- 标签切换 -->
          <div class="fav-tabs">
            <button v-for="tab in favoriteTabs" :key="tab.key" class="fav-tab" :class="{ active: currentFavTab === tab.key }" @click="currentFavTab = tab.key">
              {{ tab.label }}
            </button>
          </div>

          <!-- 搜索筛选 -->
          <div class="search-filter-area compact">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">{{ currentFavTab === 'datasets' ? '数据集名称' : currentFavTab === 'data' ? '数据名称' : '模板名称' }}</label>
                <input v-model="favoriteSearchForm.name" class="filter-input" placeholder="请输入" />
              </div>
              <div class="filter-item" v-if="currentFavTab === 'datasets' || currentFavTab === 'data'">
                <label class="filter-label">科学分类</label>
                <div class="filter-select" @click.stop="toggleDropdown('favScientific')">
                  <span class="filter-select-text">{{ favoriteSearchForm.scientific || '请选择科学分类' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.favScientific" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectFavOption('scientific', '')">请选择科学分类</li>
                    <li class="filter-select-item" @click.stop="selectFavOption('scientific', '材料基因工程')">材料基因工程</li>
                    <li class="filter-select-item" @click.stop="selectFavOption('scientific', '生物医用材料')">生物医用材料</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item" v-if="currentFavTab === 'templates' || currentFavTab === 'fragments'">
                <label class="filter-label">所属部门</label>
                <div class="filter-select" @click.stop="toggleDropdown('favDepartment')">
                  <span class="filter-select-text">{{ favoriteSearchForm.department || '请选择所属部门' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.favDepartment" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectFavOption('department', '')">请选择所属部门</li>
                    <li class="filter-select-item" @click.stop="selectFavOption('department', '节点管理部')">节点管理部</li>
                    <li class="filter-select-item" @click.stop="selectFavOption('department', '社会大众')">社会大众</li>
                  </ul>
                </div>
              </div>
              <div class="filter-buttons">
                <button class="filter-btn primary" @click="handleFavoriteSearch">查询</button>
                <button class="filter-btn ghost" @click="handleFavoriteReset">重置</button>
              </div>
            </div>
          </div>

          <!-- 批量下载 -->
          <div class="action-bar">
            <button class="action-btn ghost" @click="handleFavBatchDownload">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/>
                <polyline points="7 10 12 15 17 10"/>
                <line x1="12" y1="15" x2="12" y2="3"/>
              </svg>
              批量下载
            </button>
          </div>

          <!-- 收藏表格 -->
          <div class="pc-table-area">
            <!-- 数据集收藏 -->
            <table v-if="currentFavTab === 'datasets'" class="data-table">
              <thead>
                <tr>
                  <th class="col-checkbox"><input type="checkbox" /></th>
                  <th class="col-index">序号</th>
                  <th class="col-fav-name">数据集名称</th>
                  <th class="col-fav-category">科学分类</th>
                  <th class="col-fav-count">数据量</th>
                  <th class="col-fav-favcount">收藏数据量</th>
                  <th class="col-fav-abstract">摘要</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="favoriteData.length === 0">
                  <td colspan="8" class="empty-cell">暂无数据</td>
                </tr>
              </tbody>
            </table>

            <!-- 数据收藏 -->
            <table v-if="currentFavTab === 'data'" class="data-table">
              <thead>
                <tr>
                  <th class="col-checkbox"><input type="checkbox" /></th>
                  <th class="col-index">序号</th>
                  <th class="col-fav-name">数据名称</th>
                  <th class="col-fav-category">科学分类</th>
                  <th class="col-fav-source">数据来源</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="favoriteData.length === 0">
                  <td colspan="6" class="empty-cell">暂无数据</td>
                </tr>
              </tbody>
            </table>

            <!-- 模板/片段收藏 -->
            <table v-if="currentFavTab === 'templates' || currentFavTab === 'fragments'" class="data-table">
              <thead>
                <tr>
                  <th class="col-checkbox"><input type="checkbox" /></th>
                  <th class="col-index">序号</th>
                  <th class="col-fav-name">{{ currentFavTab === 'templates' ? '模板名称' : '模板片段名称' }}</th>
                  <th class="col-fav-dept">所属部门</th>
                  <th class="col-fav-tag">标签</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-if="favoriteData.length === 0">
                  <td colspan="6" class="empty-cell">暂无数据</td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <span class="total">共 {{ favoriteTotal }} 条</span>
            <button class="page-btn" :disabled="favoriteCurrentPage === 1" @click="favoriteCurrentPage--">上一页</button>
            <span class="page-current">{{ favoriteCurrentPage }}</span>
            <button class="page-btn" :disabled="favoriteCurrentPage >= favoriteTotalPages" @click="favoriteCurrentPage++">下一页</button>
          </div>
        </template>

        <!-- ==================== 上传下载记录 ==================== -->
        <template v-if="currentMenu === 'records'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">上传下载记录</span>
          </div>
          <div class="pc-title">上传下载记录</div>

          <!-- 类型标签 -->
          <div class="record-tabs">
            <button class="record-tab" :class="{ active: recordType === 'all' }" @click="recordType = 'all'">全部</button>
            <button class="record-tab" :class="{ active: recordType === 'upload' }" @click="recordType = 'upload'">上传</button>
            <button class="record-tab" :class="{ active: recordType === 'download' }" @click="recordType = 'download'">下载</button>
          </div>

          <!-- 搜索筛选 -->
          <div class="search-filter-area compact">
            <div class="filter-row">
              <div class="filter-item">
                <label class="filter-label">文件名称</label>
                <input v-model="recordSearchForm.name" class="filter-input" placeholder="请输入" />
              </div>
              <div class="filter-item">
                <label class="filter-label">任务类型</label>
                <div class="filter-select" @click.stop="toggleDropdown('recordTaskType')">
                  <span class="filter-select-text">{{ recordSearchForm.taskType || '请选择任务类型' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.recordTaskType" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectRecordOption('taskType', '')">请选择任务类型</li>
                    <li class="filter-select-item" @click.stop="selectRecordOption('taskType', '数据上传')">数据上传</li>
                    <li class="filter-select-item" @click.stop="selectRecordOption('taskType', '数据下载')">数据下载</li>
                  </ul>
                </div>
              </div>
              <div class="filter-item">
                <label class="filter-label">上传状态</label>
                <div class="filter-select" @click.stop="toggleDropdown('recordStatus')">
                  <span class="filter-select-text">{{ recordSearchForm.status || '请选择上传状态' }}</span>
                  <svg class="filter-select-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/>
                  </svg>
                  <ul v-if="dropdowns.recordStatus" class="filter-select-dropdown">
                    <li class="filter-select-item" @click.stop="selectRecordOption('status', '')">请选择上传状态</li>
                    <li class="filter-select-item" @click.stop="selectRecordOption('status', '成功')">成功</li>
                    <li class="filter-select-item" @click.stop="selectRecordOption('status', '失败')">失败</li>
                    <li class="filter-select-item" @click.stop="selectRecordOption('status', '处理中')">处理中</li>
                  </ul>
                </div>
              </div>
            </div>
            <div class="filter-row second-row">
              <div class="filter-item">
                <label class="filter-label">创建时间</label>
                <input type="date" v-model="recordSearchForm.startTime" class="filter-input date-input" placeholder="开始时间" />
                <span class="date-sep">至</span>
                <input type="date" v-model="recordSearchForm.endTime" class="filter-input date-input" placeholder="结束时间" />
              </div>
              <div class="filter-buttons">
                <button class="filter-btn primary" @click="handleRecordSearch">查询</button>
                <button class="filter-btn ghost" @click="handleRecordReset">重置</button>
              </div>
            </div>
          </div>

          <!-- 记录表格 -->
          <div class="pc-table-area">
            <table class="data-table">
              <thead>
                <tr>
                  <th class="col-index">序号</th>
                  <th class="col-rec-name">文件名称</th>
                  <th class="col-rec-size">文件大小</th>
                  <th class="col-rec-type">任务类型</th>
                  <th class="col-rec-status">上传状态</th>
                  <th class="col-rec-parse">解析状态</th>
                  <th class="col-rec-reason">失败原因</th>
                  <th class="col-action">操作</th>
                </tr>
              </thead>
              <tbody>
                <tr v-for="(item, index) in recordData" :key="item.id">
                  <td class="col-index">{{ index + 1 }}</td>
                  <td class="col-rec-name" :title="item.name">{{ item.name }}</td>
                  <td class="col-rec-size">{{ item.size }}</td>
                  <td class="col-rec-type">{{ item.taskType }}</td>
                  <td class="col-rec-status">
                    <span :class="['status-dot', item.status === '成功' ? 'success' : item.status === '失败' ? 'error' : 'processing']"></span>
                    {{ item.status }}
                  </td>
                  <td class="col-rec-parse">
                    <span :class="['status-dot', item.parseStatus === '成功' ? 'success' : item.parseStatus === '失败' ? 'error' : 'processing']"></span>
                    {{ item.parseStatus }}
                  </td>
                  <td class="col-rec-reason">{{ item.reason || '-' }}</td>
                  <td class="col-action">
                    <div class="action-icons">
                      <button class="icon-btn view" @click="handleViewRecord(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M1 12s4-8 11-8 11 8 11 8-4 8-11 8-11-8-11-8z"/><circle cx="12" cy="12" r="3"/></svg></button>
                      <button class="icon-btn delete" @click="handleDeleteRecord(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><polyline points="3 6 5 6 21 6"/><path d="M19 6v14a2 2 0 0 1-2 2H7a2 2 0 0 1-2-2V6m3 0V4a2 2 0 0 1 2-2h4a2 2 0 0 1 2 2v2"/></svg></button>
                      <button class="icon-btn download" @click="handleDownloadRecord(item)"><svg width="16" height="16" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2"><path d="M21 15v4a2 2 0 0 1-2 2H5a2 2 0 0 1-2-2v-4"/><polyline points="7 10 12 15 17 10"/><line x1="12" y1="15" x2="12" y2="3"/></svg></button>
                    </div>
                  </td>
                </tr>
              </tbody>
            </table>
          </div>

          <!-- 分页 -->
          <div class="pagination">
            <span class="total">共 {{ recordTotal }} 条</span>
            <button class="page-btn" :disabled="recordCurrentPage === 1" @click="recordCurrentPage--">上一页</button>
            <span class="page-current">{{ recordCurrentPage }}</span>
            <button class="page-btn" :disabled="recordCurrentPage >= recordTotalPages" @click="recordCurrentPage++">下一页</button>
          </div>
        </template>

        <!-- ==================== 编辑资料 ==================== -->
        <template v-if="currentMenu === 'edit-profile'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">修改账号信息</span>
          </div>
          <div class="page-header-with-back">
            <div class="pc-title">编辑资料</div>
            <button class="back-btn" @click="goProfile">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              返回
            </button>
          </div>

          <div class="edit-profile-form">
            <div class="form-item">
              <label class="form-label required">用户名</label>
              <input type="text" v-model="editProfileForm.username" class="form-input" disabled placeholder="用户名不可修改" />
            </div>
            <div class="form-item">
              <label class="form-label required">真实姓名</label>
              <input type="text" v-model="editProfileForm.realName" class="form-input" placeholder="请输入真实姓名" />
            </div>
            <div class="form-item">
              <label class="form-label">电子邮箱</label>
              <input type="email" v-model="editProfileForm.email" class="form-input" placeholder="请输入电子邮箱" />
            </div>
            <div class="form-item">
              <label class="form-label">手机号码</label>
              <input type="tel" v-model="editProfileForm.phone" class="form-input" placeholder="请输入手机号码" />
            </div>
          </div>

          <div class="form-actions">
            <button class="submit-btn" @click="handleSaveProfile">提交</button>
          </div>
        </template>

        <!-- ==================== 修改密码 ==================== -->
        <template v-if="currentMenu === 'change-password'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">修改密码</span>
          </div>
          <div class="page-header-with-back">
            <div class="pc-title">修改密码</div>
            <button class="back-btn" @click="goProfile">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              返回
            </button>
          </div>

          <div class="password-form">
            <div class="form-item">
              <label class="form-label required">当前密码</label>
              <input type="password" v-model="passwordForm.oldPassword" class="form-input" placeholder="请输入当前密码" />
            </div>
            <div class="form-item">
              <label class="form-label required">登录密码</label>
              <input type="password" v-model="passwordForm.newPassword" class="form-input" placeholder="请设置新的登录密码" />
            </div>
            <div class="form-item">
              <label class="form-label required">确认密码</label>
              <input type="password" v-model="passwordForm.confirmPassword" class="form-input" placeholder="请再次确认新的密码" />
            </div>
          </div>

          <div class="form-actions">
            <button class="submit-btn" @click="handleSavePassword">立即修改</button>
          </div>
        </template>

        <!-- ==================== 修改二级密码 ==================== -->
        <template v-if="currentMenu === 'change-secondary-password'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main" @click="goProfile">个人中心</span> &gt; <span class="crumb-now">修改二级密码</span>
          </div>
          <div class="page-header-with-back">
            <div class="pc-title">修改二级密码</div>
            <button class="back-btn" @click="goProfile">
              <svg width="14" height="14" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2">
                <line x1="19" y1="12" x2="5" y2="12"></line>
                <polyline points="12 19 5 12 12 5"></polyline>
              </svg>
              返回
            </button>
          </div>

          <div class="password-form">
            <div class="form-item">
              <label class="form-label required">当前二级密码</label>
              <input type="password" v-model="secondaryPasswordForm.oldPassword" class="form-input" placeholder="请输入当前二级密码" />
            </div>
            <div class="form-item">
              <label class="form-label required">新的二级密码</label>
              <input type="password" v-model="secondaryPasswordForm.newPassword" class="form-input" placeholder="请设置新的二级密码（6位数字和字母组合）" />
            </div>
            <div class="form-item">
              <label class="form-label required">确认二级密码</label>
              <input type="password" v-model="secondaryPasswordForm.confirmPassword" class="form-input" placeholder="请再次确认新的二级密码" />
            </div>
          </div>

          <div class="form-actions">
            <button class="submit-btn" @click="handleSaveSecondaryPassword">立即修改</button>
          </div>
        </template>

        <!-- ==================== 我的资料（查看模式） ==================== -->
        <template v-if="currentMenu === 'profile'">
          <div class="pc-breadcrumb">
            当前位置：<span class="crumb-main" @click="goHome">首页</span> &gt; <span class="crumb-main">个人中心</span> &gt; <span class="crumb-now">我的资料</span>
          </div>
          <div class="pc-title">我的资料</div>

          <div class="profile-view">
            <div class="profile-item">
              <label class="profile-label required">用户名</label>
              <div class="profile-value">{{ userProfile.username }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label required">真实姓名</label>
              <div class="profile-value">{{ userProfile.realName }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label">电子邮箱</label>
              <div class="profile-value">{{ userProfile.email || '-' }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label">手机号码</label>
              <div class="profile-value">{{ userProfile.phone || '-' }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label">权限</label>
              <div class="profile-value">{{ userProfile.role }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label">所属部门</label>
              <div class="profile-value">{{ userProfile.department }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label">申请部门</label>
              <div class="profile-value">{{ userProfile.applyDept }}</div>
            </div>
            <div class="profile-item">
              <label class="profile-label">申请状态</label>
              <div class="profile-value">
                <span :class="['apply-status-tag', userProfile.applyStatus === '已通过' ? 'approved' : 'pending']">{{ userProfile.applyStatus }}</span>
              </div>
            </div>
          </div>

          <div class="profile-view-actions">
            <button class="action-btn primary" @click="currentMenu = 'edit-profile'">编辑资料</button>
            <button class="action-btn primary" @click="currentMenu = 'change-password'">修改密码</button>
            <button class="action-btn primary" @click="currentMenu = 'change-secondary-password'">修改二级密码</button>
          </div>
        </template>
      </main>
    </div>
  </section>
</template>

<script setup>
import { onMounted, ref, computed } from 'vue'
import { getMyModules } from '../api/module.js'

const emit = defineEmits(['go-home'])

// 当前选中的菜单
const currentMenu = ref('datasets')

// 菜单列表
const menuList = [
  { key: 'datasets', label: '我的数据集' },
  { key: 'templates', label: '我的模板' },
  { key: 'fragments', label: '我的模板片段' },
  { key: 'favorites', label: '我的收藏' },
  { key: 'records', label: '上传下载记录' },
  { key: 'profile', label: '我的资料' },
]

// 用户信息
const currentUserName = ref('')
const currentUserId = ref(null)

// 用户资料
const userProfile = ref({
  username: 'zhuxiangdong',
  realName: '朱向东',
  email: 'ry@163.com',
  phone: '15888888888',
  role: '超级管理员',
  department: '节点管理部',
  applyDept: '社会大众',
  applyStatus: '待审核',
  avatar: ''
})

// 编辑资料表单
const editProfileForm = ref({
  username: 'zhuxiangdong',
  realName: '朱向东',
  email: 'ry@163.com',
  phone: '15888888888'
})

// 密码表单
const passwordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })
const secondaryPasswordForm = ref({ oldPassword: '', newPassword: '', confirmPassword: '' })

// 下拉框状态
const dropdowns = ref({
  scientific: false, industry: false, auditStatus: false, department: false, dataLevel: false,
  templateTag: false, templateScope: false, templateAudit: false, templateStatus: false,
  fragmentScope: false, fragmentAudit: false, fragmentStatus: false,
  favScientific: false, favDepartment: false,
  recordTaskType: false, recordStatus: false,
})

// 数据集
const datasetSearchForm = ref({ name: '', scientific: '', industry: '', auditStatus: '', field: '', department: '', dataLevel: '' })
const datasetList = ref([
  { id: 1, name: '123123', level: '公开', dataCount: 0, rowCount: 0, auditStatus: '已完成', selected: false },
  { id: 2, name: '纳米羟基磷灰石/聚乳酸复合支架的制备与性能研究数据集', level: '公开', dataCount: 872, rowCount: 146, auditStatus: '已完成', selected: false },
  { id: 3, name: '高通量计算材料科学数据集', level: '公开', dataCount: 5184, rowCount: 324, auditStatus: '已完成', selected: false },
  { id: 4, name: '纳米羟基磷灰石生物材料性能数据集', level: '公开', dataCount: 30, rowCount: 3, auditStatus: '待提交审核', selected: false },
  { id: 5, name: 'DLP3D打印颅骨修复体数据集', level: '内部', dataCount: 7462, rowCount: 574, auditStatus: '已完成', selected: false },
])
const datasetTotal = ref(5)
const datasetCurrentPage = ref(1)
const datasetTotalPages = computed(() => Math.ceil(datasetTotal.value / 10))
const selectAll = computed({ get: () => datasetList.value.length > 0 && datasetList.value.every(i => i.selected), set: (v) => datasetList.value.forEach(i => i.selected = v) })

// 模板
const templateSearchForm = ref({ name: '', tag: '', scope: '', audit: '', status: '' })
const templateData = ref([
  { id: 1, name: '1113', tag: '生物医用材料（科学...', scope: '公开', description: '23', createTime: '2026-03-22' },
  { id: 2, name: '111', tag: '生物医用材料（科学...', scope: '公开', description: '', createTime: '2026-03-22' },
  { id: 3, name: '高通量羟基...', tag: '生物医用材料（科学...', scope: '公开', description: '', createTime: '2026-03-20' },
  { id: 4, name: '高通量羟基...', tag: '生物医用材料（科学...', scope: '公开', description: '', createTime: '2026-03-20' },
  { id: 5, name: 'DLP3D颅颌...', tag: '生物医用材料（科学...', scope: '公开', description: '', createTime: '2026-03-20' },
])
const templateTotal = ref(5)
const templateCurrentPage = ref(1)
const templateTotalPages = computed(() => Math.ceil(templateTotal.value / 10))

// 模板片段
const fragmentSearchForm = ref({ name: '', scope: '', audit: '', status: '' })
const fragmentData = ref([
  { id: 1, name: 'SEM表征', scope: '公开', description: '', createTime: '2026-03-22' },
  { id: 2, name: 'Zeta电位表征', scope: '公开', description: '', createTime: '2026-03-22' },
  { id: 3, name: '烧结收缩率...', scope: '公开', description: '', createTime: '2026-03-20' },
  { id: 4, name: '压缩强度测试', scope: '公开', description: '', createTime: '2026-03-20' },
  { id: 5, name: '3D打印制...', scope: '公开', description: '', createTime: '2026-03-20' },
])
const fragmentTotal = ref(5)
const fragmentCurrentPage = ref(1)
const fragmentTotalPages = computed(() => Math.ceil(fragmentTotal.value / 10))

// 收藏
const favoriteTabs = [{ key: 'datasets', label: '数据集' }, { key: 'data', label: '数据' }, { key: 'templates', label: '模板' }, { key: 'fragments', label: '模板片段' }]
const currentFavTab = ref('datasets')
const favoriteSearchForm = ref({ name: '', scientific: '', department: '' })
const favoriteData = ref([])
const favoriteTotal = ref(0)
const favoriteCurrentPage = ref(1)
const favoriteTotalPages = computed(() => Math.ceil(favoriteTotal.value / 10))

// 记录
const recordType = ref('all')
const recordSearchForm = ref({ name: '', taskType: '', status: '', startTime: '', endTime: '' })
const recordData = ref([
  { id: 1, name: '0826fd27d...', size: '22.87 KB', taskType: '上传', status: '成功', parseStatus: '成功', reason: '' },
  { id: 2, name: 'acbd7f4a6...', size: '42.36 KB', taskType: '上传', status: '成功', parseStatus: '成功', reason: '' },
  { id: 3, name: '0826fd27d...', size: '22.87 KB', taskType: '上传', status: '成功', parseStatus: '成功', reason: '' },
  { id: 4, name: '57060be85...', size: '22.87 KB', taskType: '上传', status: '成功', parseStatus: '成功', reason: '' },
])
const recordTotal = ref(4)
const recordCurrentPage = ref(1)
const recordTotalPages = computed(() => Math.ceil(recordTotal.value / 10))

// 下拉框控制
const toggleDropdown = (name) => {
  Object.keys(dropdowns.value).forEach(key => { if (key !== name) dropdowns.value[key] = false })
  dropdowns.value[name] = !dropdowns.value[name]
}
const selectOption = (name, value) => { datasetSearchForm.value[name] = value; dropdowns.value[name] = false }
const selectTemplateOption = (name, value) => { templateSearchForm.value[name] = value; dropdowns.value[`template${name.charAt(0).toUpperCase() + name.slice(1)}`] = false }
const selectFragmentOption = (name, value) => { fragmentSearchForm.value[name] = value; dropdowns.value[`fragment${name.charAt(0).toUpperCase() + name.slice(1)}`] = false }
const selectFavOption = (name, value) => { favoriteSearchForm.value[name] = value; dropdowns.value[`fav${name.charAt(0).toUpperCase() + name.slice(1)}`] = false }
const selectRecordOption = (name, value) => { recordSearchForm.value[name] = value; dropdowns.value[`record${name.charAt(0).toUpperCase() + name.slice(1)}`] = false }

// 数据集操作
const handleDatasetSearch = () => console.log('搜索数据集:', datasetSearchForm.value)
const handleDatasetReset = () => { datasetSearchForm.value = { name: '', scientific: '', industry: '', auditStatus: '', field: '', department: '', dataLevel: '' } }
const handleBatchDownload = () => alert('批量下载')
const handleCustomColumns = () => alert('自定义列')
const handleAddDataset = () => alert('新增数据集')
const handleSelectAll = () => { const v = !selectAll.value; datasetList.value.forEach(i => i.selected = v) }
const handleViewDataset = (item) => alert(`查看: ${item.name}`)
const handleEditDataset = (item) => alert(`编辑: ${item.name}`)
const handleDownloadDataset = (item) => alert(`下载: ${item.name}`)
const handleDeleteDataset = (item) => { if (confirm(`删除 ${item.name}?`)) datasetList.value = datasetList.value.filter(d => d.id !== item.id) }

// 模板操作
const handleTemplateSearch = () => console.log('搜索模板:', templateSearchForm.value)
const handleTemplateReset = () => { templateSearchForm.value = { name: '', tag: '', scope: '', audit: '', status: '' } }
const handleAddTemplate = () => alert('新增模板')
const handleViewTemplate = (item) => alert(`查看: ${item.name}`)
const handleEditTemplate = (item) => alert(`编辑: ${item.name}`)
const handleDeleteTemplate = (item) => { if (confirm(`删除 ${item.name}?`)) templateData.value = templateData.value.filter(d => d.id !== item.id) }

// 模板片段操作
const handleFragmentSearch = () => console.log('搜索片段:', fragmentSearchForm.value)
const handleFragmentReset = () => { fragmentSearchForm.value = { name: '', scope: '', audit: '', status: '' } }
const handleAddFragment = () => alert('新增模板片段')
const handleViewFragment = (item) => alert(`查看: ${item.name}`)
const handleEditFragment = (item) => alert(`编辑: ${item.name}`)
const handleDeleteFragment = (item) => { if (confirm(`删除 ${item.name}?`)) fragmentData.value = fragmentData.value.filter(d => d.id !== item.id) }

// 收藏操作
const handleFavoriteSearch = () => console.log('搜索收藏:', favoriteSearchForm.value)
const handleFavoriteReset = () => { favoriteSearchForm.value = { name: '', scientific: '', department: '' } }
const handleFavBatchDownload = () => alert('批量下载收藏')
const handleViewFav = (item) => alert(`查看: ${item.name}`)

// 记录操作
const handleRecordSearch = () => console.log('搜索记录:', recordSearchForm.value)
const handleRecordReset = () => { recordSearchForm.value = { name: '', taskType: '', status: '', startTime: '', endTime: '' } }
const handleViewRecord = (item) => alert(`查看: ${item.name}`)
const handleDeleteRecord = (item) => { if (confirm(`删除记录 ${item.name}?`)) recordData.value = recordData.value.filter(d => d.id !== item.id) }
const handleDownloadRecord = (item) => alert(`下载: ${item.name}`)

// 编辑资料
const handleSaveProfile = () => {
  if (!editProfileForm.value.realName) {
    alert('请输入真实姓名')
    return
  }
  // 更新用户资料
  userProfile.value.realName = editProfileForm.value.realName
  userProfile.value.email = editProfileForm.value.email
  userProfile.value.phone = editProfileForm.value.phone
  alert('资料保存成功')
  currentMenu.value = 'profile'
}

// 修改密码
const handleSavePassword = () => {
  if (!passwordForm.value.oldPassword) {
    alert('请输入当前密码')
    return
  }
  if (!passwordForm.value.newPassword) {
    alert('请输入新密码')
    return
  }
  if (passwordForm.value.newPassword !== passwordForm.value.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  alert('密码修改成功')
  passwordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  currentMenu.value = 'profile'
}

// 修改二级密码
const handleSaveSecondaryPassword = () => {
  if (!secondaryPasswordForm.value.oldPassword) {
    alert('请输入当前二级密码')
    return
  }
  if (!secondaryPasswordForm.value.newPassword) {
    alert('请输入新的二级密码')
    return
  }
  if (secondaryPasswordForm.value.newPassword !== secondaryPasswordForm.value.confirmPassword) {
    alert('两次输入的二级密码不一致')
    return
  }
  alert('二级密码修改成功')
  secondaryPasswordForm.value = { oldPassword: '', newPassword: '', confirmPassword: '' }
  currentMenu.value = 'profile'
}

// 导航
const goHome = () => { emit('go-home') }
const goProfile = () => { currentMenu.value = 'profile' }

// JWT解码
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
    currentUserId.value = payload?.sub ? Number(payload.sub) : null
  } catch (_) {
    currentUserName.value = 'admin'
    currentUserId.value = null
  }
}

// 检查sessionStorage中的菜单设置
function checkSessionMenu() {
  const menu = sessionStorage.getItem('pc-menu')
  if (menu) {
    currentMenu.value = menu
    sessionStorage.removeItem('pc-menu')
  }
}

// 点击外部关闭下拉
const handleClickOutside = (e) => {
  if (!e.target.closest('.filter-select')) {
    Object.keys(dropdowns.value).forEach(key => dropdowns.value[key] = false)
  }
}

onMounted(async () => {
  syncUserFromToken()
  checkSessionMenu()
  document.addEventListener('click', handleClickOutside)
})
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
  padding: 20px 16px;
  box-shadow: 0 1px 3px rgba(0, 0, 0, 0.06);
}

.pc-user-info {
  text-align: center;
  margin-bottom: 16px;
}

.pc-avatar {
  width: 64px;
  height: 64px;
  margin: 0 auto 10px;
  border-radius: 50%;
  background: #e8f0fe;
  display: flex;
  align-items: center;
  justify-content: center;
  overflow: hidden;
}

.pc-avatar img {
  width: 100%;
  height: 100%;
  object-fit: cover;
}

.pc-user-name {
  font-size: 14px;
  color: #333;
  font-weight: 500;
}

.pc-quick-actions {
  display: flex;
  gap: 8px;
  margin-bottom: 20px;
  padding-bottom: 16px;
  border-bottom: 1px solid #e8ecf4;
}

.quick-btn {
  flex: 1;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 4px;
  padding: 8px 10px;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
  font-size: 12px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.quick-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
}

.pc-menu {
  list-style: none;
  padding: 0;
  margin: 0;
}

.pc-menu-item {
  padding: 12px 16px;
  border-radius: 6px;
  color: #666;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
  font-size: 14px;
}

.pc-menu-item:hover {
  background: #f5f7fb;
  color: #1a5ce6;
}

.pc-menu-item.active {
  background: #e6f7ff;
  color: #1a5ce6;
  font-weight: 600;
  border-right: 3px solid #1a5ce6;
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
  cursor: pointer;
}

.crumb-main:hover {
  text-decoration: underline;
}

.crumb-now {
  color: #333;
}

.pc-title {
  font-size: 18px;
  font-weight: 600;
  color: #333;
}

.page-header-with-back {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 20px;
}

.back-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 6px 12px;
  border: 1px solid #d4dae6;
  background: #f5f7fb;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.back-btn:hover {
  border-color: #1a5ce6;
  color: #1a5ce6;
  background: #e6f7ff;
}

/* 编辑资料/修改密码表单 */
.edit-profile-form,
.password-form {
  max-width: 500px;
  margin: 0 auto;
  padding: 20px 0;
}

.form-item {
  display: flex;
  align-items: center;
  margin-bottom: 20px;
}

.form-label {
  width: 100px;
  font-size: 13px;
  color: #333;
  text-align: right;
  padding-right: 16px;
}

.form-label.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.form-input {
  flex: 1;
  padding: 10px 12px;
  border: 1px solid #e0e0e0;
  border-radius: 4px;
  font-size: 13px;
  color: #333;
  background: #fff;
  transition: border-color 0.2s;
}

.form-input:focus {
  outline: none;
  border-color: #1a5ce6;
}

.form-input:disabled {
  background: #f5f5f5;
  color: #999;
  cursor: not-allowed;
}

.form-input::placeholder {
  color: #bbb;
}

.form-actions {
  max-width: 500px;
  margin: 30px auto 0;
  padding-left: 116px;
}

.submit-btn {
  width: 100%;
  padding: 12px 24px;
  border: none;
  background: #1a5ce6;
  color: #fff;
  font-size: 14px;
  cursor: pointer;
  border-radius: 4px;
  transition: background 0.2s;
}

.submit-btn:hover {
  background: #1246bb;
}

/* 我的资料查看模式 */
.profile-view {
  max-width: 600px;
  margin: 0 auto;
}

.profile-item {
  display: flex;
  align-items: center;
  padding: 14px 0;
  border-bottom: 1px solid #f0f0f0;
}

.profile-label {
  width: 100px;
  font-size: 13px;
  color: #666;
  text-align: right;
  padding-right: 16px;
}

.profile-label.required::before {
  content: '*';
  color: #f56c6c;
  margin-right: 4px;
}

.profile-value {
  flex: 1;
  font-size: 13px;
  color: #333;
  padding: 8px 12px;
  background: #f8f9fc;
  border-radius: 4px;
}

.apply-status-tag {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.apply-status-tag.approved {
  color: #67c23a;
  background: #f0f9eb;
}

.apply-status-tag.pending {
  color: #e6a23c;
  background: #fdf6ec;
}

.profile-view-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  margin-top: 30px;
  padding-top: 20px;
  border-top: 1px solid #e8ecf4;
}

/* 搜索筛选 */
.search-filter-area {
  background: #f8f9fc;
  border-radius: 6px;
  padding: 16px 20px;
  margin: 20px 0 16px;
}

.search-filter-area.compact {
  padding: 12px 16px;
}

.filter-row {
  display: flex;
  align-items: center;
  gap: 16px;
  flex-wrap: wrap;
}

.filter-row.second-row,
.filter-row.third-row {
  margin-top: 12px;
}

.filter-item {
  display: flex;
  align-items: center;
  gap: 8px;
}

.filter-label {
  font-size: 13px;
  color: #333;
  white-space: nowrap;
}

.filter-input {
  width: 140px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 6px 10px;
  font-size: 13px;
  background: #fff;
}

.filter-input.date-input {
  width: 120px;
}

.date-sep {
  color: #999;
  font-size: 13px;
}

.filter-select {
  position: relative;
  display: inline-flex;
  align-items: center;
  padding: 6px 10px;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  background: #fff;
  font-size: 13px;
  color: #999;
  cursor: pointer;
  min-width: 120px;
}

.filter-select-text {
  flex: 1;
  color: #666;
}

.filter-select-caret {
  margin-left: 4px;
  color: #999;
}

.filter-select-dropdown {
  position: absolute;
  top: calc(100% + 2px);
  left: 0;
  min-width: 140px;
  padding: 4px 0;
  margin: 0;
  list-style: none;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 8px 16px rgba(0, 0, 0, 0.08);
  border: 1px solid #e0e4f0;
  z-index: 10;
}

.filter-select-item {
  padding: 8px 12px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s;
}

.filter-select-item:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.filter-buttons {
  display: flex;
  align-items: center;
  gap: 10px;
  margin-left: auto;
}

.filter-btn {
  padding: 6px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
}

.filter-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.filter-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

/* 操作按钮 */
.action-bar,
.dataset-action-area {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 16px;
}

.left-actions {
  display: flex;
  gap: 10px;
}

.action-btn {
  display: flex;
  align-items: center;
  gap: 6px;
  padding: 8px 16px;
  border-radius: 4px;
  font-size: 13px;
  cursor: pointer;
  border: 1px solid;
  transition: all 0.2s;
}

.action-btn.primary {
  background: #1a5ce6;
  border-color: #1a5ce6;
  color: #fff;
}

.action-btn.ghost {
  background: #fff;
  border-color: #d4dae6;
  color: #666;
}

/* 表格 */
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

/* 列宽 */
.col-checkbox { width: 40px; text-align: center; }
.col-index { width: 50px; text-align: center; }
.col-dataset-name, .col-tpl-name, .col-frag-name, .col-fav-name, .col-rec-name { min-width: 180px; max-width: 280px; }
.col-level, .col-tpl-scope, .col-frag-scope { width: 80px; text-align: center; }
.col-data-count, .col-row-count { width: 80px; text-align: center; }
.col-audit-status { width: 100px; text-align: center; }
.col-action { width: 140px; text-align: center; }
.col-tpl-tag { min-width: 140px; }
.col-tpl-desc, .col-frag-desc { min-width: 120px; max-width: 200px; }
.col-tpl-time, .col-frag-time { width: 120px; }
.col-fav-category { width: 120px; }
.col-fav-count { width: 80px; text-align: center; }
.col-fav-favcount { width: 100px; text-align: center; }
.col-fav-abstract { min-width: 150px; max-width: 250px; }
.col-rec-size { width: 100px; }
.col-rec-type { width: 100px; }
.col-rec-status, .col-rec-parse { width: 100px; }
.col-rec-reason { min-width: 120px; }

/* 标签样式 */
.level-tag, .audit-tag, .tag-badge {
  display: inline-block;
  padding: 2px 8px;
  border-radius: 3px;
  font-size: 12px;
}

.level-public { color: #67c23a; background: #f0f9eb; }
.level-private { color: #f56c6c; background: #fef0f0; }
.audit-done { color: #67c23a; background: #f0f9eb; }
.audit-pending { color: #e6a23c; background: #fdf6ec; }
.tag-badge { color: #1a5ce6; background: #e8f0fe; }

/* 状态点 */
.status-dot {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  margin-right: 4px;
}

.status-dot.success { background: #67c23a; }
.status-dot.error { background: #f56c6c; }
.status-dot.processing { background: #e6a23c; }

/* 操作图标 */
.action-icons {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
}

.icon-btn {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 28px;
  height: 28px;
  border: none;
  background: transparent;
  color: #666;
  cursor: pointer;
  border-radius: 4px;
  transition: all 0.2s;
}

.icon-btn:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.icon-btn.delete:hover {
  background: #fef0f0;
  color: #f56c6c;
}

.text-btn {
  padding: 0;
  border: none;
  background: transparent;
  color: #1a5ce6;
  font-size: 13px;
  cursor: pointer;
}

/* 收藏标签 */
.fav-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
}

.fav-tab {
  padding: 8px 20px;
  border: 1px solid #d4dae6;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
}

.fav-tab:first-child { border-radius: 4px 0 0 4px; }
.fav-tab:last-child { border-radius: 0 4px 4px 0; }
.fav-tab.active { background: #1a5ce6; border-color: #1a5ce6; color: #fff; }

/* 记录标签 */
.record-tabs {
  display: flex;
  gap: 0;
  margin-bottom: 16px;
}

.record-tab {
  padding: 8px 24px;
  border: none;
  background: #fff;
  color: #666;
  font-size: 13px;
  cursor: pointer;
  transition: all 0.2s;
  border-bottom: 2px solid transparent;
}

.record-tab.active {
  color: #1a5ce6;
  border-bottom-color: #1a5ce6;
  font-weight: 500;
}

/* 分页 */
.pagination {
  display: flex;
  align-items: center;
  justify-content: flex-end;
  gap: 12px;
  margin-top: 20px;
  padding-top: 16px;
  border-top: 1px solid #e8ecf4;
}

.total { font-size: 13px; color: #666; }
.page-btn { padding: 6px 12px; font-size: 13px; color: #666; background: #fff; border: 1px solid #d4dae6; border-radius: 4px; cursor: pointer; }
.page-btn:disabled { opacity: 0.5; cursor: not-allowed; }
.page-current { padding: 6px 12px; font-size: 13px; color: #fff; background: #1a5ce6; border-radius: 4px; min-width: 32px; text-align: center; }

/* 空状态 */
.empty-cell {
  text-align: center;
  color: #999;
  padding: 40px;
}
</style>
