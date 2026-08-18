<script setup lang="ts">
import { computed, onMounted, reactive, ref, toRaw } from "vue";
import { useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import { http, dataOf } from "../api/http";
import { stepUpHeader, withStepUp } from "../api/stepUp";
import { useAuthStore } from "../stores/auth";

type DatasetField = Record<string, any> & {
  __uiId: string;
  key: string;
  label: string;
  type: string;
  required: boolean;
  sensitive: boolean;
  maskType: string;
  __optionsText: string;
};
const auth = useAuthStore(),
  router = useRouter();
const rows = ref<any[]>([]),
  templates = ref<any[]>([]),
  governance = ref<any>({ categories: [], categoryMappings: [], tags: [] }),
  total = ref(0),
  loading = ref(false),
  saving = ref(false),
  templateLoading = ref(false);
const keyword = ref(""),
  category = ref(""),
  dialog = ref(false);
const scopeIds = computed(() => auth.user?.assignedScopes || []);
const isAdmin = computed(() => auth.user?.roles.includes("ADMIN") || false);
const fieldTypes = [
  ["string", "文本"],
  ["number", "数值"],
  ["date", "日期"],
  ["boolean", "布尔"],
  ["file", "文件"],
  ["object", "对象"],
  ["array", "数组"],
];
const form = reactive<any>({
  id: null,
  name: "",
  description: "",
  category: "研发数据",
  tagValues: [],
  scientificCategoryId: null,
  industryCategoryId: null,
  recordCodePrefix: "",
  dataScopeId: 1,
  templateId: null,
  dataCount: 0,
  hasAttachments: false,
  hasRecoverableArchive: false,
  fields: [],
});
const scientificCategories = computed(() =>
  governance.value.categories.filter((v: any) => v.type === "SCIENTIFIC"),
);
const industryCategories = computed(() => {
  if (!form.scientificCategoryId)
    return governance.value.categories.filter(
      (v: any) => v.type === "INDUSTRY",
    );
  const allowed = new Set(
    governance.value.categoryMappings
      .filter((v: any) => v.scientificCategoryId === form.scientificCategoryId)
      .map((v: any) => v.industryCategoryId),
  );
  return governance.value.categories.filter(
    (v: any) => v.type === "INDUSTRY" && allowed.has(v.id),
  );
});
const structureLocked = computed(
  () => Number(form.dataCount) > 0 || Boolean(form.hasRecoverableArchive),
);
const scopeLocked = computed(
  () => structureLocked.value || Boolean(form.hasAttachments),
);

function uid() {
  return crypto.randomUUID();
}
function clonePlain<T>(value: T): T {
  return structuredClone(toRaw(value));
}
function blankField(index = 1): DatasetField {
  return {
    __uiId: uid(),
    __optionsText: "",
    key: `field${index}`,
    label: `字段 ${index}`,
    type: "string",
    required: false,
    sensitive: false,
    maskType: "GENERIC",
  };
}
function hydrateField(raw: any): DatasetField {
  return {
    ...clonePlain(raw),
    __uiId: uid(),
    __optionsText: Array.isArray(raw?.options)
      ? JSON.stringify(raw.options)
      : "",
    type: raw?.type === "text" ? "string" : String(raw?.type || "string"),
    key: String(raw?.key || ""),
    label: String(raw?.label || ""),
    required: Boolean(raw?.required),
    sensitive: Boolean(raw?.sensitive),
    maskType: String(raw?.maskType || "GENERIC"),
  };
}
function reset() {
  Object.assign(form, {
    id: null,
    name: "",
    description: "",
    category: "研发数据",
    tagValues: [],
    scientificCategoryId: null,
    industryCategoryId: null,
    recordCodePrefix: "",
    dataScopeId: scopeIds.value[0] || 1,
    templateId: null,
    dataCount: 0,
    hasAttachments: false,
    hasRecoverableArchive: false,
    fields: [blankField()],
  });
}
function owns(row: any) {
  return isAdmin.value || Number(row.creatorId) === Number(auth.user?.id);
}
function canEdit(row: any) {
  return auth.can("dataset:update") && owns(row);
}
function deleteBlockReason(row: any) {
  if (Number(row.dataCount) > 0)
    return `已有 ${Number(row.dataCount).toLocaleString()} 条在线记录`;
  if (row.hasRecoverableArchive) return "存在可恢复归档历史";
  if (row.hasAttachments) return "存在数据集附件";
  return "";
}
function canDelete(row: any) {
  return auth.can("dataset:delete") && owns(row) && !deleteBlockReason(row);
}
function shareDataset(row: any) {
  router.push({ path: "/admin", query: { shareDataset: String(row.id) } });
}

async function load() {
  loading.value = true;
  try {
    const page = dataOf<any>(
      await http.get("/datasets", {
        params: {
          keyword: keyword.value || undefined,
          category: category.value || undefined,
          pageSize: 50,
        },
      }),
    );
    rows.value = page.list;
    total.value = page.total;
  } finally {
    loading.value = false;
  }
}
async function loadTemplates() {
  if (!auth.can("template:read")) return;
  const page = dataOf<any>(
    await http.get("/templates", {
      params: { usable: true, type: "template", pageSize: 100 },
    }),
  );
  templates.value = page.list;
}
async function loadGovernance() {
  governance.value = dataOf<any>(await http.get("/governance/options"));
}
function flattenTemplate(template: any) {
  const properties = template?.schemaDefinition?.properties || {};
  return (template?.content?.sections || [])
    .flatMap((section: any) => section.fields || [])
    .map((field: any) => {
      const schema = clonePlain(properties[field.key] || {});
      const merged: any = { ...clonePlain(field), schema };
      if (merged.min == null && schema.minimum != null)
        merged.min = schema.minimum;
      if (merged.max == null && schema.maximum != null)
        merged.max = schema.maximum;
      if (merged.minLength == null && schema.minLength != null)
        merged.minLength = schema.minLength;
      if (merged.maxLength == null && schema.maxLength != null)
        merged.maxLength = schema.maxLength;
      if (merged.pattern == null && schema.pattern != null)
        merged.pattern = schema.pattern;
      if (merged.options == null && Array.isArray(schema.enum))
        merged.options = clonePlain(schema.enum);
      return hydrateField(merged);
    });
}
function templateFields(template: any) {
  return (template?.content?.sections || [])
    .flatMap((section: any) => section.fields || [])
    .map((field: any) => clonePlain(field));
}
function isPlaceholderTemplate(fields: any[]) {
  return (
    fields.length === 1 &&
    String(fields[0]?.key || "") === "fieldKey" &&
    String(fields[0]?.label || "") === "字段名称"
  );
}
async function applyTemplate(id: number | null) {
  if (id == null) {
    form.fields = [blankField()];
    return;
  }
  templateLoading.value = true;
  try {
    const detail = dataOf<any>(await http.get(`/templates/${id}`));
    if (Number(form.templateId) !== Number(id)) return;
    const index = templates.value.findIndex(
      (item) => Number(item.id) === Number(id),
    );
    if (index >= 0) templates.value[index] = detail;
    else templates.value.push(detail);
    form.fields = flattenTemplate(detail);
    if (isPlaceholderTemplate(templateFields(detail)))
      ElMessage.warning(
        "所选模板当前版本仍只有默认占位字段，请先完善模板字段后再创建数据集",
      );
    if (!form.name) form.name = `${detail.name}数据集`;
    if (!form.tagValues.length)
      form.tagValues = String(detail.tag || "")
        .split(",")
        .map((v: string) => v.trim())
        .filter(Boolean);
  } catch {
    if (Number(form.templateId) === Number(id)) {
      form.templateId = null;
      form.fields = [blankField()];
    }
  } finally {
    templateLoading.value = false;
  }
}
function create() {
  if (!scopeIds.value.length)
    return ElMessage.warning(
      "当前账号没有直接数据域授权，选择性共享不能用于创建数据集",
    );
  reset();
  dialog.value = true;
}
async function edit(row: any) {
  const value = dataOf<any>(await http.get(`/datasets/${row.id}`));
  Object.assign(form, {
    id: value.id,
    name: value.name,
    description: value.description || "",
    category: value.category || "研发数据",
    tagValues: String(value.tags || "")
      .split(",")
      .map((v: string) => v.trim())
      .filter(Boolean),
    scientificCategoryId:
      value.scientificCategoryId == null
        ? null
        : Number(value.scientificCategoryId),
    industryCategoryId:
      value.industryCategoryId == null
        ? null
        : Number(value.industryCategoryId),
    recordCodePrefix: String(value.recordCodePrefix || ""),
    dataScopeId: Number(value.dataScopeId),
    templateId: value.templateId == null ? null : Number(value.templateId),
    dataCount: Number(value.dataCount),
    hasAttachments: Boolean(value.hasAttachments),
    hasRecoverableArchive: Boolean(value.hasRecoverableArchive),
    fields: (value.fieldDefinition || []).map(hydrateField),
  });
  dialog.value = true;
}
function scientificChanged() {
  if (
    !industryCategories.value.some((v: any) => v.id === form.industryCategoryId)
  )
    form.industryCategoryId = null;
}
function addField() {
  form.fields.push(blankField(form.fields.length + 1));
}
function removeField(index: number) {
  if (form.fields.length === 1)
    return ElMessage.warning("数据集至少需要一个字段");
  form.fields.splice(index, 1);
}
function parseOptions(text: string, label: string) {
  const source = text.trim();
  if (!source) return undefined;
  if (source.startsWith("[")) {
    try {
      const value = JSON.parse(source);
      if (!Array.isArray(value)) throw new Error();
      return value;
    } catch {
      throw new Error(`${label}的可选值须为 JSON 数组，或使用逗号分隔文本`);
    }
  }
  return source
    .split(",")
    .map((v) => v.trim())
    .filter(Boolean);
}
function cleanField(field: DatasetField) {
  const value: any = { ...field };
  delete value.__uiId;
  delete value.__optionsText;
  value.key = value.key.trim();
  value.label = value.label.trim();
  const options = parseOptions(field.__optionsText, value.label || value.key);
  if (options) value.options = options;
  else delete value.options;
  for (const key of ["unit", "pattern"])
    if (value[key] != null && !String(value[key]).trim()) delete value[key];
  for (const key of ["min", "max", "minLength", "maxLength"])
    if (value[key] == null || value[key] === "") delete value[key];
  return value;
}
function body() {
  const template =
    form.templateId == null
      ? null
      : templates.value.find(
          (item) => Number(item.id) === Number(form.templateId),
        );
  if (form.templateId != null && !template)
    throw new Error("模板详情尚未加载完成，请重新选择模板");
  const fields = template
    ? templateFields(template)
    : form.fields.map(cleanField);
  if (template && isPlaceholderTemplate(fields))
    throw new Error(
      "所选模板当前版本只有默认占位字段，请先到模板页面完善字段后再创建数据集",
    );
  const recordCodePrefix = String(form.recordCodePrefix || "")
    .trim()
    .toUpperCase();
  if (!form.id && !/^[A-Z][A-Z0-9]{1,15}$/.test(recordCodePrefix))
    throw new Error("记录标识前缀须为2至16位大写字母或数字，并以字母开头");
  const keys = new Set<string>();
  for (const field of fields) {
    if (!/^[A-Za-z][A-Za-z0-9_]{0,63}$/.test(field.key))
      throw new Error(`字段 key“${field.key}”格式不正确`);
    if (keys.has(field.key)) throw new Error(`字段 key“${field.key}”重复`);
    keys.add(field.key);
    if (!field.label) throw new Error("字段名称不能为空");
  }
  return {
    name: form.name.trim(),
    description: form.description.trim(),
    category: form.category,
    tags: form.tagValues.join(","),
    scientificCategoryId: form.scientificCategoryId,
    industryCategoryId: form.industryCategoryId,
    recordCodePrefix,
    dataScopeId: form.dataScopeId,
    templateId: form.templateId,
    fieldDefinition: fields,
  };
}
async function save() {
  if (!form.name.trim()) return ElMessage.warning("请输入数据集名称");
  if (templateLoading.value)
    return ElMessage.warning("模板字段正在加载，请稍候");
  saving.value = true;
  try {
    const payload = body();
    if (form.id) await http.put(`/datasets/${form.id}`, payload);
    else await http.post("/datasets", payload);
    ElMessage.success(form.id ? "数据集已更新" : "数据集已创建");
    dialog.value = false;
    await load();
  } catch (error: any) {
    if (!error?.response) ElMessage.error(error.message || "字段定义不正确");
  } finally {
    saving.value = false;
  }
}
async function remove(row: any) {
  await ElMessageBox.confirm(`确认删除空数据集“${row.name}”？`, "删除数据集", {
    type: "warning",
  });
  const endpoint = `/datasets/${row.id}`;
  await withStepUp(
    {
      purpose: "DELETE",
      method: "DELETE",
      target: endpoint,
      label: `删除数据集“${row.name}”`,
    },
    (token) => http.delete(endpoint, { headers: stepUpHeader(token) }),
  );
  ElMessage.success("数据集已删除");
  await load();
}
async function favorite(row: any) {
  if (row.favorited) await http.delete(`/datasets/${row.id}/favorite`);
  else await http.put(`/datasets/${row.id}/favorite`);
  row.favorited = !row.favorited;
}
const go = (row: any) => router.push("/datasets/" + row.id);
onMounted(async () => {
  await Promise.all([
    load(),
    loadGovernance(),
    auth.can("template:read") ? loadTemplates() : Promise.resolve(),
  ]);
});
</script>

<template>
  <div class="page">
    <div class="page-head">
      <div>
        <h1 class="page-title">数据资产</h1>
        <div class="page-subtitle">
          从已发布公共模板或本人私有模板建立受控数据集，也支持明确标识的自定义结构，共
          {{ total }} 个数据集
        </div>
      </div>
      <el-button
        v-if="auth.can('dataset:create')"
        type="primary"
        @click="create"
        >创建数据集</el-button
      >
    </div>
    <div class="surface toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索数据集、标签"
        clearable
        style="width: 300px"
        @keyup.enter="load"
      /><el-select
        v-model="category"
        placeholder="全部分类"
        clearable
        style="width: 150px"
        ><el-option label="研发数据" value="研发数据" /><el-option
          label="生产数据"
          value="生产数据" /><el-option
          label="质量数据"
          value="质量数据" /></el-select
      ><el-button @click="load">查询</el-button>
    </div>
    <div class="surface">
      <el-table :data="rows" v-loading="loading" @row-click="go"
        ><el-table-column label="数据集" min-width="280"
          ><template #default="{ row }"
            ><div class="dataset-name">
              <i>▦</i>
              <div>
                <b>{{ row.name }}</b
                ><span>{{ row.description || "暂无说明" }}</span>
              </div>
            </div></template
          ></el-table-column
        ><el-table-column prop="recordCodePrefix" label="记录码前缀" width="120"
          ><template #default="{ row }"
            ><span class="mono">{{ row.recordCodePrefix }}</span></template
          ></el-table-column
        ><el-table-column
          prop="category"
          label="分类"
          width="110"
        /><el-table-column label="来源" min-width="155"
          ><template #default="{ row }"
            ><el-tag v-if="row.templateId" type="primary" effect="plain"
              >模板 #{{ row.templateId }} · v{{ row.templateVersion }}</el-tag
            ><el-tag v-else type="info" effect="plain"
              >自定义结构</el-tag
            ></template
          ></el-table-column
        ><el-table-column label="记录数" width="100"
          ><template #default="{ row }"
            ><b>{{ Number(row.dataCount).toLocaleString() }}</b></template
          ></el-table-column
        ><el-table-column label="版本" width="75"
          ><template #default="{ row }"
            >v{{ row.version }}</template
          ></el-table-column
        ><el-table-column label="收藏" width="70"
          ><template #default="{ row }"
            ><el-tooltip :content="row.favorited ? '取消收藏' : '收藏数据集'"
              ><button
                class="favorite"
                :aria-label="row.favorited ? '取消收藏' : '收藏数据集'"
                @click.stop="favorite(row)"
              >
                {{ row.favorited ? "★" : "☆" }}
              </button></el-tooltip
            ></template
          ></el-table-column
        ><el-table-column
          label="负责人"
          prop="creatorName"
          width="105"
        /><el-table-column label="操作" width="290" fixed="right"
          ><template #default="{ row }"
            ><el-button link type="primary" @click.stop="go(row)"
              >进入</el-button
            ><el-button
              v-if="auth.can('share:manage')"
              link
              @click.stop="shareDataset(row)"
              >共享</el-button
            ><el-button v-if="canEdit(row)" link @click.stop="edit(row)"
              >编辑</el-button
            ><el-button
              v-if="canDelete(row)"
              link
              type="danger"
              @click.stop="remove(row)"
              >删除</el-button
            ><el-tooltip
              v-else-if="auth.can('dataset:delete') && owns(row)"
              :content="`${deleteBlockReason(row)}，不能直接删除`"
              ><span class="disabled-action danger-disabled" @click.stop
                >删除受限</span
              ></el-tooltip
            ></template
          ></el-table-column
        ></el-table
      >
    </div>

    <el-dialog
      v-model="dialog"
      class="dataset-editor-dialog"
      width="min(1080px,94vw)"
      top="3vh"
      append-to-body
      destroy-on-close
    >
      <template #header
        ><div class="editor-title">
          <span class="editor-title-icon">▦</span>
          <div>
            <h2>{{ form.id ? "编辑数据集" : "创建数据集" }}</h2>
            <p>配置数据集基本信息、结构来源与字段规则</p>
          </div>
          <span class="dataset-badge">{{
            form.templateId ? "模板结构" : "自定义结构"
          }}</span>
        </div></template
      >
      <div class="editor-progress">
        <div class="progress-item is-active">
          <i>1</i><span><b>基本信息</b><small>名称、分类与范围</small></span>
        </div>
        <div class="progress-line"></div>
        <div class="progress-item is-active">
          <i>2</i
          ><span
            ><b>字段结构</b><small>{{ form.fields.length }} 个字段</small></span
          >
        </div>
      </div>
      <el-alert
        v-if="structureLocked || scopeLocked"
        class="freeze-alert"
        :title="`受保护状态：${Number(form.dataCount) > 0 ? '存在在线记录；' : ''}${form.hasRecoverableArchive ? '存在可恢复归档；' : ''}${form.hasAttachments ? '存在附件；' : ''}字段结构、模板或数据域将按对应规则锁定。`"
        type="warning"
        :closable="false"
      />
      <el-form label-position="top" class="editor-form">
        <section class="editor-panel">
          <div class="panel-heading">
            <div>
              <span class="section-kicker">BASIC INFORMATION</span>
              <h3>基本信息</h3>
            </div>
            <p><em>*</em> 为必填项</p>
          </div>
          <div class="meta-grid meta-grid-primary">
            <el-form-item label="数据集名称 *"
              ><el-input
                v-model="form.name"
                maxlength="200"
                show-word-limit
                placeholder="例如：发动机试验数据集" /></el-form-item
            ><el-form-item label="记录标识前缀 *"
              ><el-input
                v-model="form.recordCodePrefix"
                :disabled="!!form.id"
                maxlength="16"
                class="mono"
                placeholder="例如 MAT、PROC、QA"
                @blur="
                  form.recordCodePrefix = String(form.recordCodePrefix || '')
                    .trim()
                    .toUpperCase()
                "
              /><small class="form-help"
                >记录将生成 MAT-20260818-000123；前缀创建后不可修改。</small
              ></el-form-item
            ><el-form-item label="业务分类"
              ><el-select v-model="form.category"
                ><el-option label="研发数据" value="研发数据" /><el-option
                  label="生产数据"
                  value="生产数据" /><el-option
                  label="质量数据"
                  value="质量数据" /></el-select></el-form-item
            ><el-form-item label="数据域"
              ><el-select v-model="form.dataScopeId" :disabled="scopeLocked"
                ><el-option
                  v-for="scope in scopeIds"
                  :key="scope"
                  :label="`数据域 #${scope}`"
                  :value="scope" /></el-select
            ></el-form-item>
          </div>
          <div class="meta-grid">
            <el-form-item label="科学分类"
              ><el-select
                v-model="form.scientificCategoryId"
                clearable
                placeholder="请选择"
                @change="scientificChanged"
                ><el-option
                  v-for="v in scientificCategories"
                  :key="v.id"
                  :label="v.name"
                  :value="v.id" /></el-select></el-form-item
            ><el-form-item label="产业分类"
              ><el-select
                v-model="form.industryCategoryId"
                clearable
                placeholder="请选择"
                ><el-option
                  v-for="v in industryCategories"
                  :key="v.id"
                  :label="v.name"
                  :value="v.id" /></el-select></el-form-item
            ><el-form-item label="标签"
              ><el-select
                v-model="form.tagValues"
                multiple
                filterable
                allow-create
                placeholder="选择或输入标签"
                ><el-option
                  v-for="v in governance.tags"
                  :key="v.id"
                  :label="`${v.name} · ${v.group}`"
                  :value="v.name" /></el-select
            ></el-form-item>
          </div>
          <el-form-item label="数据集说明"
            ><el-input
              v-model="form.description"
              type="textarea"
              :rows="2"
              maxlength="500"
              show-word-limit
              placeholder="说明数据集的内容、用途和适用范围"
          /></el-form-item>
        </section>
        <section class="editor-panel source-panel">
          <div class="panel-heading">
            <div>
              <span class="section-kicker">STRUCTURE SOURCE</span>
              <h3>结构来源</h3>
            </div>
            <p>选择模板可快速复用已审核的字段规则</p>
          </div>
          <el-form-item v-if="auth.can('template:read')"
            ><el-select
              v-model="form.templateId"
              clearable
              :disabled="structureLocked || templateLoading"
              placeholder="不选模板：创建自定义结构"
              @change="applyTemplate"
              ><el-option
                v-for="template in templates"
                :key="template.id"
                :label="`${template.name} · v${template.version} · ${template.visibility === 'PRIVATE' ? '本人私有' : '公共发布'}`"
                :value="template.id"
            /></el-select>
            <div class="source-tip">
              <span>{{ form.templateId ? "◇" : "＋" }}</span>
              <p>
                {{
                  templateLoading
                    ? "正在读取模板的完整字段定义…"
                    : form.templateId
                      ? "创建时将直接使用该模板版本的原始字段与约束。"
                      : "当前为自定义结构，可在下方直接添加和配置字段。"
                }}
              </p>
            </div></el-form-item
          ><el-alert
            v-else
            title="当前角色没有模板读取权限，本次只能创建自定义结构。"
            type="info"
            :closable="false"
          />
        </section>
      </el-form>
      <section class="structure-panel">
        <div class="field-head">
          <div>
            <span class="section-kicker">SCHEMA DESIGN</span>
            <h3>字段结构</h3>
            <p>
              {{
                form.templateId
                  ? "字段、约束和脱敏策略来自模板，不能在数据集内单独改写"
                  : "配置字段名称、Key、类型、校验约束与导出脱敏策略"
              }}
            </p>
          </div>
          <el-button
            v-if="!form.templateId && !structureLocked"
            type="primary"
            plain
            @click="addField"
            >＋ 添加字段</el-button
          >
        </div>
        <div
          class="field-editor"
          v-for="(field, index) in form.fields"
          :key="field.__uiId"
        >
          <div class="field-toolbar">
            <div>
              <span>FIELD {{ String(Number(index) + 1).padStart(2, "0") }}</span
              ><b>{{ field.label || "未命名字段" }}</b>
            </div>
            <el-button
              v-if="!form.templateId && !structureLocked"
              type="danger"
              link
              @click="removeField(Number(index))"
              >删除字段</el-button
            >
          </div>
          <div class="field-main">
            <label
              ><span>字段名称</span
              ><el-input
                v-model="field.label"
                :disabled="!!form.templateId || structureLocked"
                placeholder="例如：设备名称" /></label
            ><label
              ><span>字段 Key</span
              ><el-input
                v-model="field.key"
                :disabled="!!form.templateId || structureLocked"
                class="mono"
                placeholder="fieldKey" /></label
            ><label
              ><span>数据类型</span
              ><el-select
                v-model="field.type"
                :disabled="!!form.templateId || structureLocked"
                ><el-option
                  v-for="type in fieldTypes"
                  :key="type[0]"
                  :label="type[1]"
                  :value="type[0]" /></el-select
            ></label>
            <div class="switch-box">
              <span>必填字段</span
              ><el-switch
                v-model="field.required"
                :disabled="!!form.templateId || structureLocked"
                inline-prompt
                active-text="是"
                inactive-text="否"
              />
            </div>
          </div>
          <div class="constraint-title"><span>校验与数据规则</span><i></i></div>
          <div class="constraints">
            <label
              ><span>单位</span
              ><el-input
                v-model="field.unit"
                :disabled="!!form.templateId || structureLocked"
                placeholder="可选" /></label
            ><label v-if="field.type === 'number'"
              ><span>最小值</span
              ><el-input-number
                v-model="field.min"
                :disabled="!!form.templateId || structureLocked"
                placeholder="不限" /></label
            ><label v-if="field.type === 'number'"
              ><span>最大值</span
              ><el-input-number
                v-model="field.max"
                :disabled="!!form.templateId || structureLocked"
                placeholder="不限" /></label
            ><label class="constraint-wide"
              ><span>可选值</span
              ><el-input
                v-model="field.__optionsText"
                :disabled="!!form.templateId || structureLocked"
                placeholder="JSON 数组或逗号分隔文本"
            /></label>
            <div class="switch-box">
              <span>导出脱敏</span
              ><el-switch
                v-model="field.sensitive"
                :disabled="!!form.templateId || structureLocked"
                inline-prompt
                active-text="是"
                inactive-text="否"
              />
            </div>
            <label v-if="field.sensitive"
              ><span>脱敏方式</span
              ><el-select
                v-model="field.maskType"
                :disabled="!!form.templateId || structureLocked"
                ><el-option label="通用掩码" value="GENERIC" /><el-option
                  label="手机号"
                  value="PHONE" /><el-option
                  label="邮箱"
                  value="EMAIL" /><el-option
                  label="证件号"
                  value="ID_CARD" /><el-option
                  label="姓名"
                  value="NAME" /></el-select
            ></label>
          </div>
        </div>
      </section>
      <template #footer
        ><div class="editor-footer">
          <span>{{
            form.id
              ? "修改仅在保存后生效"
              : "创建后即可进入数据集录入和管理记录"
          }}</span>
          <div>
            <el-button @click="dialog = false">取消</el-button
            ><el-button
              type="primary"
              :loading="saving || templateLoading"
              :disabled="templateLoading"
              @click="save"
              >{{ form.id ? "保存修改" : "创建数据集" }}</el-button
            >
          </div>
        </div></template
      >
    </el-dialog>
  </div>
</template>

<style scoped>
.dataset-name {
  display: flex;
  align-items: center;
  gap: 12px;
  cursor: pointer;
}
.dataset-name i {
  width: 35px;
  height: 35px;
  border-radius: 9px;
  background: #eaf3fa;
  color: #2872a0;
  display: grid;
  place-items: center;
  font-style: normal;
}
.dataset-name b,
.dataset-name span {
  display: block;
}
.dataset-name span {
  font-size: 11px;
  color: #8995a4;
  margin-top: 4px;
  white-space: nowrap;
  overflow: hidden;
  text-overflow: ellipsis;
  max-width: 300px;
}
.favorite {
  border: 0;
  background: none;
  color: #e4a11b;
  font-size: 19px;
  cursor: pointer;
}
.disabled-action {
  font-size: 12px;
  margin-left: 10px;
  cursor: not-allowed;
}
.danger-disabled {
  color: #d36a73;
  border-bottom: 1px dashed rgba(211, 106, 115, 0.45);
  padding-bottom: 2px;
}
.el-select {
  width: 100%;
}
.editor-title {
  display: flex;
  align-items: center;
  gap: 12px;
  padding-right: 40px;
}
.editor-title-icon {
  width: 36px;
  height: 36px;
  display: grid;
  place-items: center;
  border: 1px solid #2782a5;
  border-radius: 9px;
  background: rgba(32, 151, 191, 0.12);
  color: #51dbf7;
  font-size: 18px;
}
.editor-title h2 {
  font-size: 17px;
  color: #e3f3ff;
  margin: 0;
}
.editor-title p {
  font-size: 10px;
  color: #66839d;
  margin: 4px 0 0;
}
.dataset-badge {
  margin-left: auto;
  padding: 5px 9px;
  border: 1px solid rgba(61, 190, 220, 0.28);
  border-radius: 5px;
  background: rgba(31, 139, 175, 0.1);
  color: #69d9ed;
  font-size: 9px;
}
.editor-progress {
  display: flex;
  align-items: center;
  justify-content: center;
  padding: 1px 10% 18px;
}
.progress-item {
  display: flex;
  align-items: center;
  gap: 9px;
  min-width: 135px;
}
.progress-item i {
  width: 23px;
  height: 23px;
  border-radius: 50%;
  display: grid;
  place-items: center;
  border: 1px solid #2c6886;
  color: #6b8ba3;
  font: 9px monospace;
  font-style: normal;
}
.progress-item.is-active i {
  color: #a7f1ff;
  border-color: #36c9e7;
  background: rgba(47, 190, 219, 0.13);
  box-shadow: 0 0 12px rgba(49, 201, 231, 0.12);
}
.progress-item span,
.progress-item b,
.progress-item small {
  display: block;
}
.progress-item b {
  font-size: 10px;
  color: #adc9d9;
}
.progress-item small {
  font-size: 8px;
  color: #5d7a91;
  margin-top: 2px;
}
.progress-line {
  height: 1px;
  flex: 1;
  max-width: 210px;
  margin: 0 16px;
  background: linear-gradient(90deg, #2ab6d5, #23516c);
}
.freeze-alert {
  margin-bottom: 14px;
}
.editor-panel,
.structure-panel {
  border: 1px solid rgba(67, 140, 188, 0.2);
  border-radius: 12px;
  background: linear-gradient(
    145deg,
    rgba(11, 30, 50, 0.78),
    rgba(6, 18, 32, 0.74)
  );
  box-shadow: inset 0 1px rgba(255, 255, 255, 0.018);
}
.editor-panel {
  padding: 16px 18px;
}
.source-panel {
  margin-top: 12px;
}
.panel-heading,
.field-head {
  display: flex;
  justify-content: space-between;
  align-items: flex-start;
}
.panel-heading {
  margin-bottom: 12px;
}
.panel-heading h3,
.field-head h3 {
  font-size: 14px;
  color: #dceeff;
  margin: 3px 0 0;
}
.panel-heading > p {
  font-size: 9px;
  color: #617b94;
  margin: 7px 0 0;
}
.panel-heading em {
  color: #f06d7b;
  font-style: normal;
}
.section-kicker {
  font-size: 8px;
  letter-spacing: 0.16em;
  color: #2ca8ce;
}
.meta-grid {
  display: grid;
  grid-template-columns: repeat(3, minmax(0, 1fr));
  gap: 13px;
}
.meta-grid-primary {
  grid-template-columns: 2fr 1fr 1fr 1fr;
}
.editor-form :deep(.el-form-item) {
  margin-bottom: 13px;
}
.editor-panel > .el-form-item:last-child,
.source-panel :deep(.el-form-item) {
  margin-bottom: 0;
}
.source-tip {
  display: flex;
  align-items: center;
  gap: 9px;
  margin-top: 9px;
  padding: 9px 11px;
  border: 1px solid rgba(49, 155, 202, 0.16);
  border-radius: 8px;
  background: rgba(24, 102, 136, 0.08);
}
.source-tip > span {
  color: #43d8f3;
}
.source-tip p {
  font-size: 9px;
  color: #6f8ba4;
  margin: 0;
}
.structure-panel {
  padding: 17px 18px;
  margin-top: 13px;
}
.field-head {
  align-items: center;
}
.field-head p {
  font-size: 9px;
  color: #718ba4;
  margin: 5px 0 0;
}
.field-editor {
  margin-top: 12px;
  border: 1px solid rgba(61, 122, 162, 0.22);
  border-radius: 10px;
  padding: 0 13px 13px;
  background: linear-gradient(
    145deg,
    rgba(13, 34, 55, 0.72),
    rgba(7, 22, 37, 0.65)
  );
  box-shadow: 0 10px 24px rgba(0, 5, 14, 0.14);
}
.field-toolbar {
  height: 42px;
  display: flex;
  justify-content: space-between;
  align-items: center;
  border-bottom: 1px solid rgba(48, 102, 140, 0.18);
  margin-bottom: 12px;
}
.field-toolbar > div {
  display: flex;
  align-items: center;
  gap: 10px;
}
.field-toolbar span {
  font:
    8px "SFMono-Regular",
    Consolas,
    monospace;
  letter-spacing: 0.12em;
  color: #32bddf;
}
.field-toolbar b {
  font-size: 10px;
  color: #9db7cc;
}
.field-main {
  display: grid;
  grid-template-columns: 1.25fr 1fr 0.72fr 96px;
  gap: 11px;
  align-items: end;
}
.field-main label > span,
.constraints label > span,
.switch-box > span {
  display: block;
  font-size: 9px;
  color: #68839d;
  margin: 0 0 6px 2px;
}
.switch-box {
  height: 56px;
  padding: 7px 10px;
  border: 1px solid #193a55;
  border-radius: 9px;
  background: rgba(5, 16, 30, 0.55);
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: flex-start;
}
.switch-box > span {
  margin-left: 0;
}
.constraint-title {
  display: flex;
  align-items: center;
  gap: 10px;
  margin: 14px 0 10px;
  color: #648099;
  font-size: 9px;
}
.constraint-title i {
  height: 1px;
  background: rgba(49, 105, 145, 0.2);
  flex: 1;
}
.constraints {
  display: grid;
  grid-template-columns: repeat(5, minmax(0, 1fr));
  gap: 10px;
  align-items: end;
}
.constraint-wide {
  grid-column: span 2;
}
.constraints :deep(.el-input-number) {
  width: 100%;
}
.editor-footer {
  width: 100%;
  display: flex;
  justify-content: space-between;
  align-items: center;
}
.editor-footer > span {
  font-size: 9px;
  color: #607b94;
}
.editor-footer > div {
  display: flex;
  gap: 8px;
}
.dataset-editor-dialog {
  max-height: 94vh;
  display: flex;
  flex-direction: column;
  overflow: hidden;
  border-color: #20506e !important;
  background: linear-gradient(150deg, #0d2036, #071522) !important;
}
.dataset-editor-dialog :deep(.el-dialog__header) {
  flex: none;
  padding: 15px 20px 13px;
  border-bottom: 1px solid rgba(57, 128, 174, 0.22);
  margin: 0;
}
.dataset-editor-dialog :deep(.el-dialog__headerbtn) {
  top: 11px;
  right: 10px;
}
.dataset-editor-dialog :deep(.el-dialog__body) {
  flex: 1;
  min-height: 0;
  padding: 17px 20px;
  overflow-y: auto;
  overscroll-behavior: contain;
  background: radial-gradient(
    circle at 80% 0,
    rgba(24, 105, 153, 0.08),
    transparent 30rem
  );
}
.dataset-editor-dialog :deep(.el-dialog__footer) {
  flex: none;
  padding: 12px 20px;
  border-top: 1px solid rgba(57, 128, 174, 0.22);
  background: rgba(5, 16, 29, 0.78);
}
@media (max-width: 900px) {
  .meta-grid-primary,
  .meta-grid {
    grid-template-columns: 1fr 1fr;
  }
  .field-main {
    grid-template-columns: 1fr 1fr;
  }
  .constraints {
    grid-template-columns: repeat(2, 1fr);
  }
  .constraint-wide {
    grid-column: span 1;
  }
}
@media (max-width: 650px) {
  .meta-grid-primary,
  .meta-grid,
  .field-main,
  .constraints {
    grid-template-columns: 1fr;
  }
  .editor-progress {
    display: none;
  }
  .editor-panel,
  .structure-panel {
    padding: 14px;
  }
  .field-head,
  .editor-footer {
    align-items: flex-start;
    flex-direction: column;
    gap: 10px;
  }
  .field-head .el-button {
    width: 100%;
  }
  .editor-footer > span {
    display: none;
  }
  .editor-footer > div {
    width: 100%;
    justify-content: flex-end;
  }
  .dataset-badge {
    display: none;
  }
}
</style>
