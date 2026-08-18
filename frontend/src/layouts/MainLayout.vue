<script setup lang="ts">
import { ref, computed, onMounted, onUnmounted, watch } from "vue";
import { useRoute, useRouter } from "vue-router";
import { useAuthStore } from "../stores/auth";
import { http, dataOf } from "../api/http";
const route = useRoute(),
  router = useRouter(),
  auth = useAuthStore(),
  collapsed = ref(false),
  search = ref(""),
  results = ref<any | null>(null),
  searching = ref(false),
  healthy = ref(false),
  openGroups = ref<string[]>([]);
let healthTimer = 0,
  searchTimer = 0,
  searchRequest = 0;
const searchGroups = computed(() =>
  [
    { key: "templates", label: "模板", show: auth.can("template:read") },
    { key: "datasets", label: "数据集", show: auth.can("dataset:read") },
    { key: "entities", label: "追溯实体", show: auth.can("trace:read") },
  ].filter((group) => group.show),
);
const searchResultCount = computed(() =>
  searchGroups.value.reduce(
    (total, group) => total + (results.value?.[group.key]?.length || 0),
    0,
  ),
);
const groups = computed(() =>
  [
    {
      code: "01",
      label: "数据驾驶舱",
      mark: "◫",
      path: "/dashboard",
      show: auth.can("dashboard:read"),
      children: [],
    },
    {
      code: "02",
      label: "数据资产中心",
      mark: "▦",
      show:
        auth.can("dataset:read") ||
        auth.can("trace:read") ||
        auth.can("device:read") ||
        auth.can("file:read"),
      children: [
        [
          "/assets/overview",
          "资产总览",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        [
          "/assets/materials",
          "材料库",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        [
          "/assets/processes",
          "工艺库",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        [
          "/assets/products",
          "产品库",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        [
          "/assets/performance",
          "性能数据库",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        ["/assets/devices", "设备数据库", auth.can("device:read")],
        ["/assets/files", "文件资料库", auth.can("file:read")],
        ["/assets/datasets", "通用数据集", auth.can("dataset:read")],
      ],
    },
    {
      code: "03",
      label: "全链路追溯",
      mark: "⌘",
      show: auth.can("trace:read"),
      children: [
        ["/trace/search", "追溯搜索", true],
        ["/trace/history", "追溯记录", true],
      ],
    },
    {
      code: "04",
      label: "研发数据中心",
      mark: "⌬",
      show: auth.can("dataset:read") || auth.can("trace:read"),
      children: [
        [
          "/research/projects",
          "研发项目",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        [
          "/research/experiments",
          "实验管理",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        [
          "/research/process-experiments",
          "工艺实验",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
        ["/research/machine-learning", "机器学习", true],
        [
          "/research/simulations",
          "仿真管理",
          auth.can("dataset:read") || auth.can("trace:read"),
        ],
      ],
    },
    {
      code: "05",
      label: "生产数据中心",
      mark: "⌁",
      show: true,
      children: [
        ["/production/overview", "生产总览", true],
        ["/production/work-orders", "工单", true],
        ["/production/operations", "工序", true],
        ["/production/batches", "生产批次", true],
        ["/production/realtime", "实时数据", true],
      ],
    },
    {
      code: "06",
      label: "质量数据中心",
      mark: "◎",
      show: true,
      children: [
        ["/quality/inspections", "检验记录", true],
        ["/quality/ct-metallography", "CT / 金相", true],
        ["/quality/hardness-thickness", "硬度 / 厚度", true],
        ["/quality/fatigue", "疲劳试验", true],
        ["/quality/defects", "缺陷分析", true],
      ],
    },
    {
      code: "07",
      label: "数据分析",
      mark: "⌁",
      show: true,
      children: [
        ["/analytics/process", "工艺对比", true],
        ["/analytics/performance", "性能对比", true],
        ["/analytics/batches", "批次对比", true],
        ["/analytics/correlation", "参数相关性", true],
        ["/analytics/trends", "趋势分析", true],
      ],
    },
    {
      code: "08",
      label: "数据集成中心",
      mark: "⇄",
      show: auth.can("integration:read") || auth.can("integration:manage"),
      platform: true,
      children: [
        ["/integration/overview", "集成总览", true],
        ["/integration/systems", "外部系统", true],
        ["/integration/mappings", "API 与字段映射", true],
        ["/integration/tasks", "同步任务", true],
        ["/integration/logs", "同步日志", true],
        ["/integration/exceptions", "异常队列", true],
      ],
    },
    {
      code: "09",
      label: "数据治理",
      mark: "⚒",
      show: true,
      platform: true,
      children: [
        ["/governance/master-data", "主数据", true],
        ["/governance/metadata", "元数据", true],
        ["/governance/taxonomy", "分类与标签", auth.can("governance:read")],
        ["/governance/dictionaries", "数据字典", auth.can("governance:read")],
        ["/governance/units", "单位管理", true],
        ["/governance/data-quality", "数据质量", true],
        ["/governance/templates", "模板中心", auth.can("template:read")],
        ["/governance/lifecycle", "生命周期", auth.can("lifecycle:read")],
      ],
    },
    {
      code: "10",
      label: "系统管理",
      mark: "⚙",
      show:
        auth.can("user:manage") ||
        auth.can("share:read") ||
        auth.can("audit:read") ||
        auth.can("governance:read"),
      platform: true,
      children: [
        ["/system/users", "用户与组织", auth.can("user:manage")],
        ["/system/roles", "角色权限", auth.can("governance:read")],
        ["/system/scopes", "数据域", auth.can("governance:read")],
        ["/system/menus", "菜单权限", auth.can("governance:read")],
        ["/system/sharing", "共享管理", auth.can("share:read")],
        ["/system/audits", "审计日志", auth.can("audit:read")],
        ["/system/logs", "系统日志", auth.can("audit:read")],
        ["/system/settings", "平台配置", auth.can("governance:read")],
      ],
    },
  ]
    .filter((group) => group.show)
    .map((group) => ({
      ...group,
      children: (group.children || [])
        .filter((item: any) => item[2])
        .map((item: any) => ({ path: item[0], label: item[1] })),
    })),
);
const currentGroup = computed(() => String(route.meta.group || ""));
function toggleGroup(code: string) {
  if (collapsed.value) {
    const group = groups.value.find((item) => item.code === code);
    if (group?.children?.[0]) router.push(group.children[0].path);
    return;
  }
  openGroups.value = openGroups.value.includes(code)
    ? openGroups.value.filter((item) => item !== code)
    : [...openGroups.value, code];
}
function closeSearchResults() {
  searchRequest++;
  results.value = null;
  searching.value = false;
}
async function globalSearch() {
  window.clearTimeout(searchTimer);
  const keyword = search.value.trim();
  if (keyword.length < 2) {
    closeSearchResults();
    return;
  }
  const request = ++searchRequest;
  searching.value = true;
  try {
    const data = dataOf(
      await http.get("/dashboard/search", { params: { keyword } }),
    );
    if (request === searchRequest && search.value.trim() === keyword)
      results.value = data;
  } catch {
    if (request === searchRequest) results.value = null;
  } finally {
    if (request === searchRequest) searching.value = false;
  }
}
function openResult(group: string, id: number) {
  results.value = null;
  search.value = "";
  if (group === "templates")
    router.push({ path: "/templates", query: { focus: String(id) } });
  else if (group === "datasets") router.push(`/datasets/${id}`);
  else router.push({ path: "/trace", query: { root: String(id) } });
}
async function logout() {
  await auth.logout();
  router.push("/login");
}
async function checkHealth() {
  try {
    const response = await fetch("/actuator/health");
    healthy.value = response.ok && (await response.json()).status === "UP";
  } catch {
    healthy.value = false;
  }
}
async function loadNavigation() {
  try {
    await http.get("/governance/navigation");
    const config = dataOf<any>(await http.get("/governance/ui-config"));
    const root = document.documentElement;
    root.style.setProperty("--rdp-primary", config.primaryColor);
    root.style.setProperty("--rdp-font-size", `${config.fontSize}px`);
    root.style.setProperty("--rdp-radius", `${config.borderRadius}px`);
    root.style.setProperty("--rdp-spacing", `${config.contentSpacing}px`);
  } catch {
    /* 使用前端十模块信息架构，不影响现有权限接口 */
  }
}
watch(
  currentGroup,
  (value) => {
    const code = value.slice(0, 2);
    if (code && !openGroups.value.includes(code))
      openGroups.value = [...openGroups.value, code];
  },
  { immediate: true },
);
watch(search, (value) => {
  window.clearTimeout(searchTimer);
  if (value.trim().length < 2) {
    closeSearchResults();
    return;
  }
  searchTimer = window.setTimeout(globalSearch, 300);
});
onMounted(() => {
  loadNavigation();
  checkHealth();
  healthTimer = window.setInterval(checkHealth, 30000);
});
onUnmounted(() => {
  clearInterval(healthTimer);
  window.clearTimeout(searchTimer);
  searchRequest++;
});
</script>
<template>
  <div class="shell" :class="{ collapsed }">
    <aside class="sidebar">
      <div class="sidebar-beam"></div>
      <div class="brand">
        <div class="brand-glyph"><span>J</span></div>
        <div v-if="!collapsed">
          <strong>嘉思特数据平台</strong><small>R&D · PRODUCTION</small>
        </div>
      </div>
      <nav>
        <template v-for="group in groups" :key="group.code"
          ><div
            v-if="
              group.platform &&
              groups.findIndex((g) => g.platform) === groups.indexOf(group)
            "
            class="nav-divider"
          >
            <span v-if="!collapsed">平台能力</span>
          </div>
          <router-link v-if="group.path" :to="group.path" class="group-link"
            ><span class="menu-mark">{{ group.mark }}</span
            ><span v-if="!collapsed" class="group-code">{{ group.code }}</span
            ><span v-if="!collapsed">{{ group.label }}</span></router-link
          >
          <div
            v-else
            class="nav-group"
            :class="{
              open: openGroups.includes(group.code),
              active: currentGroup.startsWith(group.code),
            }"
          >
            <button class="group-link" @click="toggleGroup(group.code)">
              <span class="menu-mark">{{ group.mark }}</span
              ><span v-if="!collapsed" class="group-code">{{ group.code }}</span
              ><span v-if="!collapsed" class="group-label">{{
                group.label
              }}</span
              ><i v-if="!collapsed">⌄</i>
            </button>
            <div v-if="!collapsed" class="subnav">
              <router-link
                v-for="item in group.children"
                :key="item.path"
                :to="item.path"
                ><span></span>{{ item.label }}</router-link
              >
            </div>
          </div></template
        >
      </nav>
      <button class="collapse" @click="collapsed = !collapsed">
        {{ collapsed ? "›" : "‹ 收起导航" }}
      </button>
    </aside>
    <main>
      <header class="topbar">
        <div class="crumb">
          <i></i><span>研发与生产大数据平台</span
          ><template v-if="route.meta.group"
            ><b>/</b><span>{{ route.meta.group }}</span></template
          ><b>/</b><strong>{{ route.meta.title }}</strong>
        </div>
        <div class="top-actions">
          <el-popover :visible="!!results" placement="bottom" :width="430"
            ><template #reference
              ><el-input
                v-model="search"
                class="global-search"
                placeholder="输入至少2个字符，搜索模板、数据集、追溯实体…"
                clearable
                @keyup.enter="globalSearch"
                @clear="closeSearchResults"
                ><template #prefix>⌕</template></el-input
              ></template
            >
            <div class="search-results" v-loading="searching">
              <template v-if="results"
                ><div
                  v-for="group in searchGroups"
                  v-show="(results[group.key] || []).length"
                  :key="group.key"
                >
                  <h4>{{ group.label }}</h4>
                  <button
                    v-for="r in results[group.key] || []"
                    :key="r.id"
                    class="result-row"
                    @click="openResult(group.key, r.id)"
                  >
                    <b>{{ r.name }}</b
                    ><small>{{ r.secondary }}</small>
                  </button>
                </div>
                <el-empty
                  v-if="!searching && searchResultCount === 0"
                  description="没有找到当前账号可访问的结果"
                  :image-size="48"
                /><button class="close-results" @click="closeSearchResults">
                  关闭
                </button></template
              >
            </div> </el-popover
          ><span class="health" :class="{ down: !healthy }"
            ><i></i> {{ healthy ? "服务正常" : "服务异常" }}</span
          ><el-dropdown
            ><div class="avatar">{{ auth.user?.realName.slice(0, 1) }}</div>
            <template #dropdown
              ><el-dropdown-menu
                ><el-dropdown-item disabled
                  >{{ auth.user?.realName }} ·
                  {{ auth.user?.roles.join(",") }}</el-dropdown-item
                ><el-dropdown-item divided @click="router.push('/profile')"
                  >个人中心</el-dropdown-item
                ><el-dropdown-item @click="logout"
                  >安全退出</el-dropdown-item
                ></el-dropdown-menu
              ></template
            ></el-dropdown
          >
        </div>
      </header>
      <router-view />
    </main>
  </div>
</template>
<style scoped>
.shell {
  display: grid;
  grid-template-columns: 248px 1fr;
  min-height: 100vh;
  background: #050b16;
  transition: grid-template-columns 0.25s ease;
}
.shell.collapsed {
  grid-template-columns: 76px 1fr;
}
.sidebar {
  position: sticky;
  top: 0;
  height: 100vh;
  color: #fff;
  padding: 0 14px;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  background: linear-gradient(
    180deg,
    rgba(9, 27, 48, 0.98),
    rgba(4, 13, 25, 0.99)
  );
  border-right: 1px solid rgba(47, 163, 215, 0.18);
  box-shadow: 16px 0 60px rgba(0, 5, 14, 0.34);
  z-index: 11;
}
.sidebar:before {
  content: "";
  position: absolute;
  inset: 0;
  pointer-events: none;
  background-image:
    linear-gradient(rgba(55, 159, 214, 0.04) 1px, transparent 1px),
    linear-gradient(90deg, rgba(55, 159, 214, 0.035) 1px, transparent 1px);
  background-size: 28px 28px;
  mask-image: linear-gradient(black, transparent 70%);
}
.sidebar-beam {
  position: absolute;
  width: 1px;
  height: 150px;
  right: -1px;
  top: 0;
  background: linear-gradient(transparent, #3bd8ff, transparent);
  box-shadow: 0 0 12px #3bd8ff;
  animation: beam 5s ease-in-out infinite;
}
.brand {
  height: 76px;
  display: flex;
  align-items: center;
  gap: 12px;
  padding: 0 8px;
  border-bottom: 1px solid rgba(112, 190, 231, 0.12);
  white-space: nowrap;
  position: relative;
}
.brand-glyph {
  width: 38px;
  height: 38px;
  flex: 0 0 38px;
  display: grid;
  place-items: center;
  position: relative;
  border: 1px solid rgba(68, 221, 255, 0.48);
  background: linear-gradient(
    145deg,
    rgba(31, 136, 178, 0.32),
    rgba(5, 23, 39, 0.8)
  );
  clip-path: polygon(22% 0, 100% 0, 100% 78%, 78% 100%, 0 100%, 0 22%);
  box-shadow: inset 0 0 18px rgba(37, 196, 236, 0.13);
}
.brand-glyph:before,
.brand-glyph:after {
  content: "";
  position: absolute;
  width: 5px;
  height: 5px;
  border-color: #66e9ff;
}
.brand-glyph:before {
  left: 3px;
  top: 3px;
  border-left: 1px solid;
  border-top: 1px solid;
}
.brand-glyph:after {
  right: 3px;
  bottom: 3px;
  border-right: 1px solid;
  border-bottom: 1px solid;
}
.brand-glyph span {
  font-weight: 900;
  font-size: 20px;
  color: #eaffff;
  text-shadow: 0 0 11px #46dcff;
}
.brand strong {
  display: block;
  font-size: 14px;
  letter-spacing: 0.06em;
  color: #e7f6ff;
}
.brand small {
  display: block;
  margin-top: 4px;
  font-size: 9px;
  letter-spacing: 0.22em;
  color: #5087a8;
}
.sidebar nav {
  padding-top: 13px;
  flex: 1;
  position: relative;
  overflow-y: auto;
  overflow-x: hidden;
  margin-right: -8px;
  padding-right: 8px;
}
.sidebar nav::-webkit-scrollbar {
  width: 3px;
}
.sidebar nav::-webkit-scrollbar-thumb {
  background: #17425e;
}
.sidebar a {
  color: #7692ac;
  text-decoration: none;
  white-space: nowrap;
  transition: 0.2s ease;
}
.group-link {
  width: 100%;
  height: 41px;
  padding: 0 11px;
  margin: 3px 0;
  color: #7692ac;
  display: flex;
  align-items: center;
  gap: 9px;
  border: 1px solid transparent;
  border-radius: 6px;
  font-size: 12px;
  background: transparent;
  cursor: pointer;
  text-align: left;
  white-space: nowrap;
}
.group-link:hover,
.nav-group.active > .group-link {
  background: rgba(21, 65, 91, 0.28);
  color: #d4f5ff;
  border-color: rgba(51, 172, 218, 0.1);
}
a.group-link.router-link-active {
  background: linear-gradient(
    90deg,
    rgba(19, 135, 177, 0.34),
    rgba(10, 48, 70, 0.22)
  );
  color: #f2fdff;
  border-color: rgba(54, 200, 243, 0.2);
  box-shadow:
    inset 2px 0 #41dbff,
    0 0 18px rgba(25, 170, 218, 0.06);
}
.group-link .group-code {
  font:
    700 9px "SFMono-Regular",
    monospace;
  color: #3e708d;
  border-right: 1px solid #173b55;
  padding-right: 8px;
}
.group-link .group-label {
  flex: 1;
}
.group-link > i {
  font-style: normal;
  font-size: 11px;
  color: #42657d;
  transition: transform 0.2s;
}
.nav-group.open > .group-link > i {
  transform: rotate(180deg);
}
.subnav {
  max-height: 0;
  opacity: 0;
  overflow: hidden;
  transition:
    max-height 0.28s ease,
    opacity 0.2s ease;
}
.nav-group.open .subnav {
  max-height: 300px;
  opacity: 1;
}
.subnav a {
  min-height: 31px;
  margin-left: 33px;
  padding: 0 10px;
  display: flex;
  align-items: center;
  gap: 8px;
  border-left: 1px solid #153850;
  font-size: 10px;
  color: #59778e;
}
.subnav a:hover {
  color: #a9dbea;
  background: linear-gradient(90deg, rgba(24, 73, 99, 0.22), transparent);
}
.subnav a span {
  width: 4px;
  height: 4px;
  border-radius: 50%;
  background: #2c617e;
}
.subnav a.router-link-active {
  color: #78e8ff;
  border-left-color: #43d8f8;
  background: linear-gradient(90deg, rgba(24, 119, 151, 0.2), transparent);
  text-shadow: 0 0 8px rgba(67, 216, 248, 0.25);
}
.subnav a.router-link-active span {
  background: #59e7ff;
  box-shadow: 0 0 7px #59e7ff;
}
.nav-divider {
  height: 30px;
  display: flex;
  align-items: flex-end;
  padding: 0 11px 6px;
  margin-top: 8px;
  border-bottom: 1px solid rgba(77, 153, 191, 0.1);
  font-size: 8px;
  letter-spacing: 0.2em;
  color: #345b74;
}
.menu-mark {
  font-size: 18px;
  width: 20px;
  text-align: center;
  color: #5acbea;
  text-shadow: 0 0 10px rgba(49, 215, 255, 0.22);
}
.collapse {
  height: 46px;
  margin: 12px 2px 20px;
  border: 0;
  border-top: 1px solid rgba(94, 165, 202, 0.1);
  background: none;
  color: #526e88;
  cursor: pointer;
  position: relative;
}
.collapse:hover {
  color: #7cdff4;
}
main {
  min-width: 0;
  background:
    radial-gradient(
      circle at 75% -10%,
      rgba(29, 100, 157, 0.14),
      transparent 35rem
    ),
    #050b16;
}
.topbar {
  height: 76px;
  background: rgba(5, 14, 26, 0.78);
  border-bottom: 1px solid rgba(74, 160, 205, 0.15);
  backdrop-filter: blur(20px);
  display: flex;
  align-items: center;
  justify-content: space-between;
  padding: 0 30px;
  position: sticky;
  top: 0;
  z-index: 10;
  box-shadow: 0 10px 35px rgba(0, 4, 12, 0.18);
}
.crumb {
  font-size: 12px;
  color: #607d98;
  display: flex;
  align-items: center;
  gap: 9px;
  letter-spacing: 0.03em;
}
.crumb > i {
  width: 7px;
  height: 7px;
  border: 1px solid #39d5ff;
  transform: rotate(45deg);
  box-shadow: 0 0 8px rgba(57, 213, 255, 0.55);
}
.crumb strong {
  color: #b9d4e9;
}
.crumb b {
  color: #2b4c67;
}
.top-actions {
  display: flex;
  align-items: center;
  gap: 20px;
}
.global-search {
  width: 290px;
}
.global-search :deep(.el-input__wrapper) {
  border-radius: 6px;
}
.health {
  font-size: 11px;
  color: #7790a7;
  text-transform: uppercase;
  letter-spacing: 0.06em;
}
.health i {
  display: inline-block;
  width: 6px;
  height: 6px;
  border-radius: 50%;
  background: #4ce7b3;
  margin-right: 7px;
  box-shadow: 0 0 10px #3bd7a6;
  animation: pulse 2.2s ease-out infinite;
}
.avatar {
  width: 36px;
  height: 36px;
  border: 1px solid rgba(65, 204, 241, 0.28);
  border-radius: 8px;
  background: linear-gradient(145deg, #153d5a, #0b2036);
  color: #8cecff;
  display: grid;
  place-items: center;
  font-weight: 700;
  cursor: pointer;
  box-shadow: inset 0 0 14px rgba(45, 194, 231, 0.08);
}
.search-results h4 {
  font-size: 11px;
  color: #66839f;
  margin: 10px 0 5px;
}
.result-row {
  display: block;
  width: 100%;
  padding: 9px;
  border: 0;
  background: none;
  border-radius: 6px;
  text-align: left;
  cursor: pointer;
  color: #b9d0e5;
}
.result-row:hover {
  background: rgba(32, 84, 115, 0.35);
}
.result-row b,
.result-row small {
  display: block;
}
.result-row small {
  color: #68839b;
  margin-top: 2px;
}
.close-results {
  border: 0;
  background: none;
  color: #31c9ef;
  cursor: pointer;
  float: right;
}
.health.down i {
  background: #ff6679;
  box-shadow: 0 0 10px #ff536a;
  animation: none;
}
@keyframes pulse {
  0% {
    box-shadow: 0 0 0 0 rgba(76, 231, 179, 0.45);
  }
  70% {
    box-shadow: 0 0 0 7px rgba(76, 231, 179, 0);
  }
  100% {
    box-shadow: 0 0 0 0 rgba(76, 231, 179, 0);
  }
}
@keyframes beam {
  0%,
  100% {
    transform: translateY(-70px);
    opacity: 0;
  }
  15%,
  85% {
    opacity: 1;
  }
  50% {
    transform: translateY(calc(100vh - 80px));
  }
}
@media (max-width: 900px) {
  .shell,
  .shell.collapsed {
    grid-template-columns: 76px minmax(0, 1fr);
  }
  .sidebar {
    padding: 0 8px;
  }
  .brand {
    padding: 0 11px;
  }
  .brand > div:not(.brand-glyph),
  .group-link > *:not(.menu-mark),
  .subnav,
  .nav-divider {
    display: none;
  }
  .group-link {
    justify-content: center;
    padding: 0;
  }
  .collapse {
    font-size: 0;
  }
  .collapse:after {
    content: "›";
    font-size: 22px;
  }
  .topbar {
    height: 64px;
    padding: 0 14px;
  }
  .crumb > span,
  .crumb > b,
  .health {
    display: none;
  }
  .top-actions {
    gap: 10px;
  }
  .global-search {
    width: min(40vw, 220px);
  }
}
@media (max-width: 560px) {
  .global-search {
    display: none;
  }
  .topbar {
    justify-content: flex-end;
  }
  .crumb {
    margin-right: auto;
  }
  .sidebar {
    position: fixed;
    left: 0;
    z-index: 20;
    width: 76px;
  }
  .shell,
  .shell.collapsed {
    display: block;
    padding-left: 76px;
  }
}
</style>
