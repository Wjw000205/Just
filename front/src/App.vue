<template>
  <div class="app">
    <!-- 顶部标题栏：白底，Logo + 机构 + 用户 -->
    <header class="site-header">
      <div class="site-header-inner">
        <!-- 左：Logo -->
        <div class="logo-area">
          <div class="logo-badge">
            <svg width="54" height="44" viewBox="0 0 54 44" fill="none">
              <rect width="54" height="44" rx="7" fill="#1a5ce6"/>
              <path d="M8 34V10l12 15V10h4v24h-4L8 19v15z" fill="white"/>
              <text x="29" y="30" fill="white" font-size="13" font-weight="700" font-family="Arial, sans-serif">&amp;D</text>
            </svg>
          </div>
          <div class="logo-text">
            <div class="logo-zh">新材料大数据中心</div>
            <div class="logo-en">National Materials Big Data Center</div>
          </div>
        </div>

        <!-- 中：机构联合标识 -->
        <div class="org-area">
          <div class="org-badges">
            <div class="org-circle org-c1">
              <svg viewBox="0 0 36 36" fill="none">
                <circle cx="18" cy="18" r="17" stroke="#c04030" stroke-width="1.5" fill="white"/>
                <circle cx="18" cy="18" r="10" fill="#c04030" opacity="0.15"/>
                <text x="18" y="22" text-anchor="middle" fill="#c04030" font-size="9" font-weight="600">机构</text>
              </svg>
            </div>
            <div class="org-circle org-c2">
              <svg viewBox="0 0 36 36" fill="none">
                <circle cx="18" cy="18" r="17" stroke="#2a7e3c" stroke-width="1.5" fill="white"/>
                <circle cx="18" cy="18" r="10" fill="#2a7e3c" opacity="0.15"/>
                <text x="18" y="22" text-anchor="middle" fill="#2a7e3c" font-size="9" font-weight="600">官方</text>
              </svg>
            </div>
            <div class="org-tag-badge">
              <span class="ne3m-text">NE3M</span>
            </div>
          </div>
          <div class="org-name">新一代生物医用材料数据资源节点</div>
        </div>

        <!-- 右：帮助 + 用户 / 登录注册 -->
        <div class="header-right">
          <button class="help-btn">
            <svg width="12" height="12" viewBox="0 0 12 12" fill="none">
              <text x="6" y="10" text-anchor="middle" fill="white" font-size="11" font-weight="700" font-family="Arial, sans-serif">?</text>
            </svg>
          </button>
          <div class="auth-links">
            <svg class="auth-icon" width="18" height="18" viewBox="0 0 24 24">
              <circle cx="12" cy="8" r="4.2" fill="none" stroke="#1a5ce6" stroke-width="1.6" />
              <path
                d="M6.2 18.4C7 16 9.3 14.5 12 14.5c2.7 0 5 1.5 5.8 3.9"
                fill="none"
                stroke="#1a5ce6"
                stroke-width="1.6"
                stroke-linecap="round"
              />
              <circle cx="12" cy="12" r="10" fill="none" stroke="#1a5ce6" stroke-width="1.2" />
            </svg>
            <template v-if="isLoggedIn">
              <div class="user-dropdown" ref="userDropdownRef">
                <button class="auth-link auth-username-btn" type="button" @click="toggleUserDropdown">
                  <span class="auth-username">{{ currentUserName || '已登录' }}</span>
                  <svg class="user-dropdown-caret" width="10" height="6" viewBox="0 0 10 6">
                    <path
                      d="M1 1l4 4 4-4"
                      fill="none"
                      stroke="currentColor"
                      stroke-width="1.5"
                      stroke-linecap="round"
                    />
                  </svg>
                </button>
                <div v-if="userDropdownOpen" class="user-dropdown-menu">
                  <button type="button" class="user-dropdown-item" @click="goPersonalCenter">
                    个人中心
                  </button>
                  <button type="button" class="user-dropdown-item" @click="goEditProfile">
                    编辑资料
                  </button>
                  <button type="button" class="user-dropdown-item" @click="goChangePassword">
                    修改密码
                  </button>
                  <button type="button" class="user-dropdown-item" @click="goChangeSecondaryPassword">
                    修改二级密码
                  </button>
                  <div class="user-dropdown-sep"></div>
                  <button type="button" class="user-dropdown-item" @click="handleLogout">
                    退出登录
                  </button>
                </div>
              </div>
            </template>
            <template v-else>
              <button class="auth-link" @click="goPage('login')">登录</button>
              <span class="auth-sep">|</span>
              <button class="auth-link" @click="goPage('register')">注册</button>
            </template>
          </div>
        </div>
      </div>
    </header>

    <!-- 蓝色导航栏 -->
    <nav class="navbar">
      <div class="navbar-inner">
        <a
          class="nav-item"
          :class="{ active: currentPage === 'home' }"
          @click="goPage('home')"
        >
          首页
        </a>
        <a
          class="nav-item"
          :class="{ active: currentPage === 'search' }"
          @click="goPage('search')"
        >
          数据检索
        </a>
        <a
          class="nav-item"
          :class="{ active: currentPage === 'template' }"
          @click="goPage('template')"
        >
          模板创建
        </a>
        <div class="nav-upload-wrap" ref="uploadDropRef">
          <a
            class="nav-item has-drop"
            :class="{ active: uploadDropOpen || currentPage === 'create-dataset' || currentPage === 'upload-dataset' }"
            @click="toggleUploadDrop"
          >
            数据上传
            <svg width="10" height="6" viewBox="0 0 10 6"><path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg>
          </a>
          <ul v-if="uploadDropOpen" class="nav-dropdown">
            <li class="nav-dropdown-item" @click="onUploadDropSelect('create')">创建数据集</li>
            <li class="nav-dropdown-item" @click="onUploadDropSelect('upload')">上传数据集</li>
          </ul>
        </div>
        <a
          class="nav-item"
          :class="{ active: currentPage === 'data-publish' }"
          @click="goPage('data-publish')"
        >
          数据发布
        </a>
        <a
          class="nav-item"
          :class="{ active: currentPage === 'template-library' }"
          @click="goPage('template-library')"
        >
          模板库
        </a>
        <a
          class="nav-item"
          :class="{ active: currentPage === 'database' }"
          @click="goPage('database')"
        >
          数据库
        </a>
        <div
          class="nav-audit-wrap"
          ref="auditDropRef"
          @mouseenter="auditDropOpen = true"
          @mouseleave="auditDropOpen = false"
        >
          <a
            class="nav-item has-drop"
            :class="{ active: auditDropOpen || isAuditPage }"
          >
            审核管理
            <svg width="10" height="6" viewBox="0 0 10 6"><path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round"/></svg>
          </a>
          <ul v-if="auditDropOpen" class="nav-dropdown">
            <li class="nav-dropdown-item" @click="onAuditSelect('template-audit')">模板审核</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('template-disable')">模板停用</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('fragment-audit')">模板片段审核</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('fragment-disable')">模板片段停用</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('data-audit')">数据审核</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('data-disable')">数据停用</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('template-publish')">模板发布</li>
            <li class="nav-dropdown-item" @click="onAuditSelect('catalog-publish')">目录发布</li>
          </ul>
        </div>
        <div class="nav-sys-wrap" ref="sysDropRef">
          <a
            class="nav-item has-drop"
            :class="{ active: sysDropOpen || isSystemMgmtPage }"
            @click.stop="toggleSysDrop"
          >
            系统管理
            <svg
              class="nav-drop-chevron"
              :class="{ 'nav-drop-chevron--open': sysDropOpen }"
              width="10"
              height="6"
              viewBox="0 0 10 6"
            >
              <path d="M1 1l4 4 4-4" stroke="currentColor" stroke-width="1.5" fill="none" stroke-linecap="round" />
            </svg>
          </a>
          <div v-if="sysDropOpen" class="nav-sys-panel" @click.stop>
            <ul class="nav-sys-list">
              <li
                v-for="row in sysMenuItems"
                :key="row.page"
                class="nav-sys-item"
                :class="{ 'nav-sys-item--active': currentPage === row.page }"
                @click="onSysSelect(row.page)"
              >
                {{ row.label }}
              </li>
            </ul>
          </div>
        </div>
      </div>
    </nav>

    <!-- 主内容区 -->
    <main class="main-content">
      <!-- 首页：统计数据条 + 三步流程 -->
      <section v-if="currentPage === 'home'" class="home-page">
        <!-- 统计数据条 -->
        <div class="stats-bar">
        <div class="stat-item">
          <div class="stat-num orange">14</div>
          <div class="stat-label">模板量</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <div class="stat-num orange">5</div>
          <div class="stat-label">模板片段量</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <div class="stat-num orange">5</div>
          <div class="stat-label">数据集量</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <div class="stat-num orange big">1,772</div>
          <div class="stat-label">数据量</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <div class="stat-num">0</div>
          <div class="stat-label">已发布数据量</div>
        </div>
        <div class="stat-sep"></div>
        <div class="stat-item">
          <div class="stat-num">0</div>
          <div class="stat-label">发布中数据量</div>
        </div>
      </div>

        <!-- 三步流程 -->
        <div class="steps-area">
        <!-- 卡片 1：模板创建 -->
        <div class="step-card">
          <div class="step-watermark">1</div>
          <h2 class="step-title">模板创建</h2>
          <p class="step-desc">可自定义模板或模板片段，模板用于创建数据集，模板片段用于创建模板</p>
          <div class="step-illus">
            <svg viewBox="0 0 260 155" fill="none" class="illus-svg">
              <defs>
                <filter id="s1" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="rgba(74,144,226,0.25)"/>
                </filter>
                <linearGradient id="g1" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#d6eaff"/>
                  <stop offset="100%" stop-color="#eef6ff"/>
                </linearGradient>
                <linearGradient id="g1b" x1="0" y1="0" x2="0" y2="1">
                  <stop offset="0%" stop-color="#5ba3f5"/>
                  <stop offset="100%" stop-color="#3578d8"/>
                </linearGradient>
              </defs>
              <!-- 显示器 -->
              <rect x="18" y="15" width="152" height="100" rx="10" fill="url(#g1)" filter="url(#s1)"/>
              <rect x="26" y="23" width="136" height="84" rx="6" fill="#f4f9ff"/>
              <!-- 图表柱 -->
              <rect x="42" y="60" width="16" height="38" rx="3" fill="url(#g1b)" opacity="0.75"/>
              <rect x="66" y="48" width="16" height="50" rx="3" fill="url(#g1b)"/>
              <rect x="90" y="38" width="16" height="60" rx="3" fill="url(#g1b)" opacity="0.85"/>
              <rect x="114" y="53" width="16" height="45" rx="3" fill="url(#g1b)" opacity="0.65"/>
              <rect x="138" y="43" width="16" height="55" rx="3" fill="url(#g1b)" opacity="0.9"/>
              <!-- 底座 -->
              <rect x="75" y="115" width="38" height="6" rx="3" fill="#b8d4f0"/>
              <rect x="58" y="121" width="72" height="7" rx="3.5" fill="#a8c8e8"/>
              <!-- 悬浮卡 -->
              <rect x="178" y="12" width="70" height="60" rx="9" fill="white" filter="url(#s1)"/>
              <rect x="178" y="12" width="70" height="14" rx="9" fill="#4a8fe6" opacity="0.15"/>
              <rect x="188" y="10" width="50" height="14" rx="7" fill="#4a8fe6"/>
              <text x="213" y="21" text-anchor="middle" fill="white" font-size="7.5" font-weight="600">模板预览</text>
              <rect x="186" y="32" width="46" height="4" rx="2" fill="#e0ecf8"/>
              <rect x="186" y="42" width="32" height="4" rx="2" fill="#4a8fe6" opacity="0.5"/>
              <rect x="186" y="52" width="38" height="4" rx="2" fill="#e0ecf8"/>
              <rect x="186" y="62" width="24" height="4" rx="2" fill="#e0ecf8"/>
              <!-- 装饰圆点 -->
              <circle cx="10" cy="40" r="4" fill="#b8d8f8" opacity="0.6"/>
              <circle cx="5" cy="60" r="3" fill="#b8d8f8" opacity="0.5"/>
              <circle cx="250" cy="100" r="4" fill="#b8d8f8" opacity="0.6"/>
            </svg>
          </div>
          <div class="step-btns">
            <button class="step-btn">去创建</button>
          </div>
        </div>

        <!-- 中间箭头 -->
        <div class="step-arrow">
          <span>»</span>
        </div>

        <!-- 卡片 2：数据上传 -->
        <div class="step-card">
          <div class="step-watermark">2</div>
          <h2 class="step-title">数据上传</h2>
          <p class="step-desc">使用定义好的模板创建数据集，并上传数据</p>
          <div class="step-illus">
            <svg viewBox="0 0 260 155" fill="none" class="illus-svg">
              <defs>
                <filter id="s2" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="4" stdDeviation="6" flood-color="rgba(74,144,226,0.22)"/>
                </filter>
                <linearGradient id="g2" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#c8e4ff"/>
                  <stop offset="100%" stop-color="#e8f4ff"/>
                </linearGradient>
                <linearGradient id="g2b" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#5ba3f5"/>
                  <stop offset="100%" stop-color="#3070d8"/>
                </linearGradient>
              </defs>
              <!-- 云朵 -->
              <path d="M78 98 Q58 98 58 80 Q58 64 76 62 Q80 46 96 44 Q106 30 124 38 Q138 26 155 38 Q176 36 178 58 Q194 60 194 78 Q194 98 174 98 Z" fill="url(#g2)" filter="url(#s2)"/>
              <!-- 上传箭头 -->
              <line x1="126" y1="90" x2="126" y2="55" stroke="#4a8fe6" stroke-width="3" stroke-linecap="round"/>
              <path d="M115 66 L126 53 L137 66" stroke="#4a8fe6" stroke-width="3" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 左侧文件卡 -->
              <rect x="28" y="22" width="44" height="58" rx="6" fill="white" filter="url(#s2)"/>
              <rect x="28" y="22" width="44" height="14" rx="6" fill="#4a8fe6" opacity="0.2"/>
              <rect x="36" y="46" width="28" height="3.5" rx="1.8" fill="#c8dff5"/>
              <rect x="36" y="55" width="20" height="3.5" rx="1.8" fill="#c8dff5"/>
              <rect x="36" y="64" width="24" height="3.5" rx="1.8" fill="#4a8fe6" opacity="0.45"/>
              <!-- 右侧文件卡 -->
              <rect x="192" y="18" width="40" height="52" rx="6" fill="white" filter="url(#s2)"/>
              <rect x="192" y="18" width="40" height="12" rx="6" fill="#4a8fe6" opacity="0.2"/>
              <rect x="199" y="38" width="24" height="3.5" rx="1.8" fill="#c8dff5"/>
              <rect x="199" y="47" width="16" height="3.5" rx="1.8" fill="#c8dff5"/>
              <rect x="199" y="56" width="20" height="3.5" rx="1.8" fill="#4a8fe6" opacity="0.4"/>
              <!-- 底部平台 -->
              <ellipse cx="130" cy="128" rx="70" ry="12" fill="#4a8fe6" opacity="0.12"/>
              <rect x="82" y="112" width="96" height="18" rx="9" fill="url(#g2b)" opacity="0.7"/>
              <!-- 装饰点 -->
              <circle cx="18" cy="55" r="4" fill="#b8d8f8" opacity="0.55"/>
              <circle cx="240" cy="40" r="4" fill="#b8d8f8" opacity="0.55"/>
              <circle cx="246" cy="90" r="3" fill="#b8d8f8" opacity="0.5"/>
            </svg>
          </div>
          <div class="step-btns two-btns">
            <button class="step-btn">去创建</button>
            <button class="step-btn">去上传</button>
          </div>
        </div>

        <!-- 中间箭头 -->
        <div class="step-arrow">
          <span>»</span>
        </div>

        <!-- 卡片 3：数据发布 -->
        <div class="step-card">
          <div class="step-watermark">3</div>
          <h2 class="step-title">数据发布</h2>
          <p class="step-desc">选择已通过审核的数据集发布至中心</p>
          <div class="step-illus">
            <svg viewBox="0 0 260 155" fill="none" class="illus-svg">
              <defs>
                <filter id="s3" x="-20%" y="-20%" width="140%" height="140%">
                  <feDropShadow dx="0" dy="4" stdDeviation="7" flood-color="rgba(74,144,226,0.28)"/>
                </filter>
                <linearGradient id="g3t" x1="0" y1="0" x2="1" y2="1">
                  <stop offset="0%" stop-color="#7ec0f8"/>
                  <stop offset="100%" stop-color="#5aa5f0"/>
                </linearGradient>
                <linearGradient id="g3l" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" stop-color="#3c88e0"/>
                  <stop offset="100%" stop-color="#2870c8"/>
                </linearGradient>
                <linearGradient id="g3r" x1="0" y1="0" x2="1" y2="0">
                  <stop offset="0%" stop-color="#4e98e8"/>
                  <stop offset="100%" stop-color="#6ab0f5"/>
                </linearGradient>
              </defs>
              <!-- 等轴立方体 -->
              <!-- 顶面 -->
              <path d="M130 22 L184 52 L130 82 L76 52 Z" fill="url(#g3t)" filter="url(#s3)"/>
              <!-- 左面 -->
              <path d="M76 52 L130 82 L130 128 L76 98 Z" fill="url(#g3l)"/>
              <!-- 右面 -->
              <path d="M184 52 L130 82 L130 128 L184 98 Z" fill="url(#g3r)"/>
              <!-- 顶面高亮线 -->
              <path d="M130 22 L184 52" stroke="white" stroke-width="0.8" opacity="0.4"/>
              <path d="M130 22 L76 52" stroke="white" stroke-width="0.8" opacity="0.4"/>
              <!-- 对号徽章 -->
              <circle cx="184" cy="36" r="22" fill="white" filter="url(#s3)"/>
              <circle cx="184" cy="36" r="18" fill="#3c8de6"/>
              <path d="M175 36 L181 43 L194 25" stroke="white" stroke-width="2.8" stroke-linecap="round" stroke-linejoin="round"/>
              <!-- 小装饰块 -->
              <rect x="48" y="68" width="18" height="18" rx="5" fill="#d0e8ff" opacity="0.7"/>
              <rect x="54" y="74" width="6" height="6" rx="2" fill="#4a8fe6" opacity="0.5"/>
              <rect x="196" y="95" width="16" height="16" rx="4" fill="#d0e8ff" opacity="0.7"/>
              <!-- 装饰圆点 -->
              <circle cx="38" cy="45" r="4" fill="#b8d8f8" opacity="0.55"/>
              <circle cx="28" cy="65" r="3" fill="#b8d8f8" opacity="0.5"/>
              <circle cx="228" cy="50" r="3.5" fill="#b8d8f8" opacity="0.55"/>
              <circle cx="236" cy="72" r="3" fill="#b8d8f8" opacity="0.5"/>
              <!-- 底部阴影 -->
              <ellipse cx="130" cy="130" rx="62" ry="10" fill="#4a8fe6" opacity="0.1"/>
            </svg>
          </div>
          <div class="step-btns">
            <button class="step-btn">去发布</button>
          </div>
        </div>
      </div>
      </section>

      <!-- 数据检索页面 -->
      <SearchPage v-else-if="currentPage === 'search'" />
      <!-- 模板创建页面 - 基础设置 -->
      <TemplateCreatePage
        v-else-if="currentPage === 'template'"
        @next="handleTemplateBaseNext"
      />

      <!-- 模板设计页面 -->
      <TemplateDesignPage
        v-else-if="currentPage === 'template-design'"
        :template-type="templateType"
        :module-id="currentModuleId"
        @back="goPage('template')"
        @next="goPage('data-rule')"
        @submit="handleCreate"
      />

      <!-- 数据量规则配置页面 -->
      <DataRuleConfigPage v-else-if="currentPage === 'data-rule'" @back="goPage('template-design')" @create="handleCreate" />

      <!-- 注册页面 -->
      <RegisterPage v-else-if="currentPage === 'register'" />
      <!-- 创建数据集页面 -->
      <CreateDataset v-else-if="currentPage === 'create-dataset'" />
      <!-- 上传数据页面 -->
      <UploadData v-else-if="currentPage === 'upload-data'" />

      <!-- 模板库页面 -->
      <TemplateLibraryPage v-else-if="currentPage === 'template-library'" @go-home="goPage('home')" />

      <!-- 模板审核页面 -->
      <TemplateAuditPage v-else-if="currentPage === 'template-audit'" @go-home="goPage('home')" />

      <!-- 模板停用页面 -->
      <TemplateDisablePage v-else-if="currentPage === 'template-disable'" @go-home="goPage('home')" />

      <!-- 模板片段审核页面 -->
      <FragmentAuditPage v-else-if="currentPage === 'fragment-audit'" @go-home="goPage('home')" />

      <!-- 模板片段停用页面 -->
      <FragmentDisablePage v-else-if="currentPage === 'fragment-disable'" @go-home="goPage('home')" />

      <!-- 数据审核页面 -->
      <DataAuditPage v-else-if="currentPage === 'data-audit'" @go-home="goPage('home')" @go-audit-manage="goPage('data-audit')" />

      <!-- 数据停用页面 -->
      <DataDisablePage v-else-if="currentPage === 'data-disable'" @go-home="goPage('home')" @go-audit-manage="goPage('data-disable')" />

      <!-- 目录发布页面 -->
      <CatalogPublishPage v-else-if="currentPage === 'catalog-publish'" @go-home="goPage('home')" @go-audit-manage="goPage('catalog-publish')" />

      <!-- 模板发布页面 -->
      <TemplatePublishPage v-else-if="currentPage === 'template-publish'" @go-home="goPage('home')" />

      <!-- 数据发布页面 -->
      <DataPublishPage v-else-if="currentPage === 'data-publish'" @go-home="goPage('home')" />

      <!-- 数据库页面 -->
      <DatabasePage
        v-else-if="currentPage === 'database'"
        @go-home="goPage('home')"
        @view-detail="openDatabaseDetail"
      />

      <!-- 数据库查看详情 -->
      <DatabaseDetailPage
        v-else-if="currentPage === 'database-detail'"
        :dataset="databaseDetailDataset"
        @go-home="goPage('home')"
        @back="goPage('database')"
      />

      <!-- 科学分类管理 -->
      <ScientificCategoryManagePage
        v-else-if="currentPage === 'scientific-category-manage'"
        @go-home="goPage('home')"
        @go-system-manage="goPage('scientific-category-manage')"
      />

      <!-- 产业分类管理 -->
      <IndustryClassificationManagePage
        v-else-if="currentPage === 'sys-industry-class'"
        @go-home="goPage('home')"
        @go-system-manage="goPage('sys-industry-class')"
      />

      <!-- 部门管理 -->
      <DepartmentManagePage
        v-else-if="currentPage === 'sys-depts'"
        @go-home="goPage('home')"
        @go-system-manage="goPage('sys-depts')"
      />

      <!-- 数据标签管理 -->
      <DataTagManagePage
        v-else-if="currentPage === 'sys-data-tags'"
        @go-home="goPage('home')"
        @go-system-manage="goPage('sys-data-tags')"
      />

      <!-- 界面管理 -->
      <InterfaceManagePage
        v-else-if="currentPage === 'sys-ui'"
        @go-home="goPage('home')"
        @go-system-manage="goPage('sys-ui')"
      />

      <!-- 菜单管理 -->
      <MenuManagePage
        v-else-if="currentPage === 'sys-menu'"
        @go-home="goPage('home')"
        @go-system-manage="goPage('sys-menu')"
      />

      <!-- 个人中心 -->
      <PersonalCenterPage v-else-if="currentPage === 'personal-center'" />

      <!-- 用户管理 -->
      <UserManagementPage v-else-if="currentPage === 'sys-users'" @go-home="goPage('home')" />

      <!-- 权限管理 -->
      <PermissionManagementPage v-else-if="currentPage === 'sys-perms'" @go-home="goPage('home')" />

      <!-- 数据字典 -->
      <DataDictionaryPage v-else-if="currentPage === 'data-dictionary'" @go-home="goPage('home')" @go-system-manage="goPage('data-dictionary')" />

      <!-- 系统管理：其余子模块占位 -->
      <section v-else-if="isSystemMgmtPage" class="sys-mgmt-placeholder">
        <h1 class="sys-mgmt-placeholder-title">{{ systemMgmtPageTitle }}</h1>
        <p class="sys-mgmt-placeholder-desc">功能页面建设中。</p>
      </section>
    </main>

    <!-- 模板提交成功提示 -->
    <div
      v-if="showTemplateSubmitModal"
      class="submit-success-mask"
      @click.self="closeTemplateSubmitModal"
    >
      <div class="submit-success-modal" @click.stop>
        <div class="submit-success-title">提示</div>
        <div class="submit-success-body">
          <div class="submit-success-msg">
            <span class="submit-success-icon">!</span>
            <span>模板提交成功</span>
          </div>
        </div>
        <div class="submit-success-actions">
          <button type="button" class="submit-success-btn" @click="handleAgainCreate">
            再次创建
          </button>
          <button type="button" class="submit-success-btn primary" @click="handleGoView">
            去查看
          </button>
        </div>
      </div>
    </div>

    <!-- 登录弹层：覆盖在内容之上 -->
    <LoginPage
      v-if="currentPage === 'login'"
      @go-register="goPage('register')"
      @login-success="onLoginSuccess"
    />
  </div>
</template>

<script setup>
import { ref, computed, onMounted, onBeforeUnmount } from 'vue'
import SearchPage from './components/SearchPage.vue'
import TemplateCreatePage from './components/TemplateCreatePage.vue'
import TemplateDesignPage from './components/TemplateDesignPage.vue'
import DataRuleConfigPage from './components/DataRuleConfigPage.vue'
import RegisterPage from './components/RegisterPage.vue'
import LoginPage from './components/LoginPage.vue'
import CreateDataset from './components/CreateDataset.vue'
import UploadData from './components/UploadData.vue'
import TemplateLibraryPage from './components/TemplateLibraryPage.vue'
import TemplateAuditPage from './components/TemplateAuditPage.vue'
import TemplateDisablePage from './components/TemplateDisablePage.vue'
import FragmentAuditPage from './components/FragmentAuditPage.vue'
import FragmentDisablePage from './components/FragmentDisablePage.vue'
import TemplatePublishPage from './components/TemplatePublishPage.vue'
import DataPublishPage from './components/DataPublishPage.vue'
import DatabasePage from './components/DatabasePage.vue'
import DatabaseDetailPage from './components/DatabaseDetailPage.vue'
import UserManagementPage from './components/UserManagementPage.vue'
import ScientificCategoryManagePage from './components/ScientificCategoryManagePage.vue'
import IndustryClassificationManagePage from './components/IndustryClassificationManagePage.vue'
import DepartmentManagePage from './components/DepartmentManagePage.vue'
import DataTagManagePage from './components/DataTagManagePage.vue'
import InterfaceManagePage from './components/InterfaceManagePage.vue'
import MenuManagePage from './components/MenuManagePage.vue'
import PersonalCenterPage from './components/PersonalCenterPage.vue'
import PermissionManagementPage from './components/PermissionManagementPage.vue'
import DataAuditPage from './components/DataAuditPage.vue'
import DataDisablePage from './components/DataDisablePage.vue'
import CatalogPublishPage from './components/CatalogPublishPage.vue'
import DataDictionaryPage from './components/DataDictionaryPage.vue'

const currentPage = ref('home')
const templateType = ref('dataset') // 'dataset' 或 'fragment'
const currentModuleId = ref(0)
const showTemplateSubmitModal = ref(false)
const uploadDropOpen = ref(false)
const uploadDropRef = ref(null)
const auditDropOpen = ref(false)
const auditDropRef = ref(null)
const sysDropOpen = ref(false)
const sysDropRef = ref(null)

const userDropdownOpen = ref(false)
const userDropdownRef = ref(null)

const sysMenuItems = [
  { page: 'scientific-category-manage', label: '科学分类管理' },
  { page: 'sys-industry-class', label: '产业分类管理' },
  { page: 'sys-data-tags', label: '数据标签管理' },
  { page: 'sys-users', label: '用户管理' },
  { page: 'sys-depts', label: '部门管理' },
  { page: 'sys-perms', label: '权限管理' },
  { page: 'data-dictionary', label: '数据字典' },
  { page: 'sys-ui', label: '界面管理' },
  { page: 'sys-menu', label: '菜单管理' },
]
const systemMgmtPageIds = new Set(sysMenuItems.map((r) => r.page))
const currentUserName = ref('')
const databaseDetailDataset = ref(null)

const isLoggedIn = computed(() => {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  return Boolean(token)
})

// 判断当前是否在审核管理相关页面
const isAuditPage = computed(() => {
  return ['template-audit', 'template-disable', 'fragment-audit', 'fragment-disable',
          'data-audit', 'data-disable', 'template-publish', 'catalog-publish'].includes(currentPage.value)
})

const isSystemMgmtPage = computed(() => systemMgmtPageIds.has(currentPage.value))

const systemMgmtPageTitle = computed(() => {
  const row = sysMenuItems.find((r) => r.page === currentPage.value)
  return row ? row.label : '系统管理'
})

const goPage = (page) => {
  currentPage.value = page
}

const handleTemplateBaseNext = (payload) => {
  templateType.value = payload?.templateType || 'dataset'
  currentModuleId.value = Number(payload?.moduleId || 0)
  currentPage.value = 'template-design'
}

function openDatabaseDetail(row) {
  databaseDetailDataset.value = row || null
  goPage('database-detail')
}

function onLoginSuccess(_payload) {
  // 以收到事件为准：登录组件已做成功判定与 token 写入
  syncUserFromToken()
  goPage('home')
}

function base64UrlDecode(input) {
  const s = String(input || '').replace(/-/g, '+').replace(/_/g, '/')
  const pad = s.length % 4 === 0 ? '' : '='.repeat(4 - (s.length % 4))
  const b64 = s + pad
  const binary = atob(b64)
  const bytes = Uint8Array.from(binary, (c) => c.charCodeAt(0))
  return new TextDecoder('utf-8').decode(bytes)
}

function syncUserFromToken() {
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (!token || token.split('.').length !== 3) {
    currentUserName.value = ''
    return
  }
  try {
    const payloadText = base64UrlDecode(token.split('.')[1])
    const payload = JSON.parse(payloadText)
    currentUserName.value = payload?.userName ? String(payload.userName) : ''
  } catch (_) {
    currentUserName.value = ''
  }
}

function handleLogout() {
  // 尝试通知后端使 token 失效（失败也不影响本地退出）
  const token =
    localStorage.getItem('token') || sessionStorage.getItem('token') || ''
  if (token) {
    fetch('http://localhost:8083/user/logout', {
      method: 'POST',
      headers: { Authorization: `Bearer ${token}` },
    }).catch(() => {})
  }

  localStorage.removeItem('token')
  sessionStorage.removeItem('token')
  currentUserName.value = ''
  userDropdownOpen.value = false
  goPage('login')
}

const handleCreate = (_payload) => {
  showTemplateSubmitModal.value = true
}

const closeTemplateSubmitModal = () => {
  showTemplateSubmitModal.value = false
}

const handleAgainCreate = () => {
  closeTemplateSubmitModal()
  goPage('template')
}

const handleGoView = () => {
  closeTemplateSubmitModal()
  goPage('personal-center')
}

const toggleUserDropdown = () => {
  userDropdownOpen.value = !userDropdownOpen.value
}

const goPersonalCenter = () => {
  userDropdownOpen.value = false
  goPage('personal-center')
}

const goEditProfile = () => {
  userDropdownOpen.value = false
  goPage('personal-center')
  // 通过 sessionStorage 传递要显示的子页面
  sessionStorage.setItem('pc-menu', 'edit-profile')
}

const goChangePassword = () => {
  userDropdownOpen.value = false
  goPage('personal-center')
  sessionStorage.setItem('pc-menu', 'change-password')
}

const goChangeSecondaryPassword = () => {
  userDropdownOpen.value = false
  goPage('personal-center')
  sessionStorage.setItem('pc-menu', 'change-secondary-password')
}

function toggleUploadDrop() {
  uploadDropOpen.value = !uploadDropOpen.value
}

function onUploadDropSelect(type) {
  uploadDropOpen.value = false
  if (type === 'create') {
    currentPage.value = 'create-dataset'
  }
  if (type === 'upload') {
    currentPage.value = 'upload-data'
  }
}

function toggleAuditDrop() {
  auditDropOpen.value = !auditDropOpen.value
}

function onAuditSelect(page) {
  auditDropOpen.value = false
  currentPage.value = page
}

function toggleSysDrop() {
  sysDropOpen.value = !sysDropOpen.value
}

function onSysSelect(page) {
  sysDropOpen.value = false
  currentPage.value = page
}

function onDocClick(e) {
  if (uploadDropRef.value && !uploadDropRef.value.contains(e.target)) {
    uploadDropOpen.value = false
  }
  if (auditDropRef.value && !auditDropRef.value.contains(e.target)) {
    auditDropOpen.value = false
  }
  if (sysDropRef.value && !sysDropRef.value.contains(e.target)) {
    sysDropOpen.value = false
  }

  if (userDropdownOpen.value && userDropdownRef.value && !userDropdownRef.value.contains(e.target)) {
    userDropdownOpen.value = false
  }
}

onMounted(() => {
  document.addEventListener('click', onDocClick)
  syncUserFromToken()
})
onBeforeUnmount(() => {
  document.removeEventListener('click', onDocClick)
})
</script>

<style scoped>
.home-page {
  display: flex;
  flex-direction: column;
  gap: 24px;
}

.nav-upload-wrap,
.nav-audit-wrap,
.nav-sys-wrap,
.nav-system-wrap {
  position: relative;
  display: flex;
  align-items: stretch;
}

.nav-drop-chevron {
  transition: transform 0.2s ease;
}

.nav-drop-chevron--open {
  transform: rotate(180deg);
}

.nav-sys-panel {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 200px;
  margin-top: 0;
  padding: 0;
  background: #fff;
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 120;
  overflow: hidden;
}

.nav-sys-list {
  list-style: none;
  margin: 0;
  padding: 6px 0;
}

.nav-sys-item {
  padding: 9px 14px 9px 15px;
  margin: 0;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  border-left: 3px solid transparent;
  transition: background 0.15s, border-color 0.15s;
}

.nav-sys-item:hover {
  background: #f5f7fa;
}

.nav-sys-item--active {
  border-left-color: #1a5ce6;
  background: #f0f6ff;
}

.sys-mgmt-placeholder {
  max-width: 960px;
  margin: 48px auto;
  padding: 32px 40px;
  background: #fff;
  border-radius: 8px;
  box-shadow: 0 2px 12px rgba(0, 0, 0, 0.06);
}

.sys-mgmt-placeholder-title {
  margin: 0 0 12px;
  font-size: 22px;
  font-weight: 600;
  color: #1a1a1a;
}

.sys-mgmt-placeholder-desc {
  margin: 0;
  font-size: 14px;
  color: #666;
}

.nav-dropdown {
  position: absolute;
  top: 100%;
  left: 0;
  min-width: 140px;
  margin: 0;
  padding: 6px 0;
  list-style: none;
  background: var(--white);
  border-radius: 4px;
  box-shadow: 0 4px 16px rgba(0, 0, 0, 0.15);
  z-index: 100;
}

.nav-dropdown-item {
  padding: 8px 16px;
  font-size: 13px;
  color: #333;
  cursor: pointer;
  transition: background 0.15s, color 0.15s;
}

.nav-dropdown-item:hover {
  background: #1a5ce6;
  color: #fff;
}

.auth-links {
  display: flex;
  align-items: center;
  gap: 10px;
}

.auth-link {
  border: none;
  background: transparent;
  padding: 0;
  color: #1a5ce6;
  font-size: 14px;
  cursor: pointer;
}

.auth-link:hover {
  text-decoration: underline;
}

.auth-sep {
  color: #9aa7bf;
}

.auth-username {
  color: #1a5ce6;
  font-weight: 600;
  max-width: 180px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.user-dropdown {
  position: relative;
  display: inline-flex;
  align-items: center;
}

.auth-username-btn {
  display: inline-flex;
  align-items: center;
  gap: 8px;
}

.user-dropdown-caret {
  color: #1a5ce6;
}

.user-dropdown-menu {
  position: absolute;
  top: calc(100% + 8px);
  right: 0;
  min-width: 140px;
  background: #fff;
  border-radius: 6px;
  border: 1px solid #e8ecf4;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.12);
  z-index: 200;
  padding: 6px 0;
}

.user-dropdown-item {
  width: 100%;
  background: transparent;
  border: none;
  padding: 10px 14px;
  text-align: left;
  font-size: 13px;
  color: #333;
  cursor: pointer;
}

.user-dropdown-item:hover {
  background: #f0f5ff;
  color: #1a5ce6;
}

.user-dropdown-sep {
  height: 1px;
  background: #e8ecf4;
  margin: 6px 0;
}

.submit-success-mask {
  position: fixed;
  inset: 0;
  background: rgba(0, 0, 0, 0.45);
  z-index: 1000;
  display: flex;
  align-items: center;
  justify-content: center;
}

.submit-success-modal {
  width: 420px;
  max-width: calc(100vw - 32px);
  background: #fff;
  border-radius: 10px;
  padding: 16px 18px 18px;
  box-shadow: 0 18px 60px rgba(0, 0, 0, 0.25);
}

.submit-success-title {
  font-size: 16px;
  font-weight: 700;
  color: #333;
  margin-bottom: 10px;
}

.submit-success-body {
  padding: 10px 0 14px;
  border-top: 1px solid #f0f0f0;
  border-bottom: 1px solid #f0f0f0;
}

.submit-success-msg {
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 10px;
  color: #333;
  font-size: 14px;
}

.submit-success-icon {
  display: inline-flex;
  align-items: center;
  justify-content: center;
  width: 18px;
  height: 18px;
  border-radius: 999px;
  background: #faad14;
  color: #fff;
  font-weight: 700;
  font-size: 12px;
}

.submit-success-actions {
  display: flex;
  justify-content: center;
  gap: 16px;
  padding-top: 14px;
}

.submit-success-btn {
  border: 1px solid #d4dae6;
  background: #fff;
  color: #333;
  border-radius: 6px;
  padding: 8px 18px;
  cursor: pointer;
  font-size: 13px;
}

.submit-success-btn.primary {
  border-color: #1a5ce6;
  background: #1a5ce6;
  color: #fff;
}

.submit-success-btn:hover:not(:disabled) {
  opacity: 0.95;
}

</style>
