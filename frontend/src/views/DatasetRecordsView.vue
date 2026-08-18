<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { useRoute, useRouter } from "vue-router";
import { http, dataOf } from "../api/http";
import { stepUpHeader, withStepUp } from "../api/stepUp";
import { isLargeFile, resumableUpload } from "../api/resumableUpload";
import { ElMessage, ElMessageBox } from "element-plus";
import { useAuthStore } from "../stores/auth";
const auth = useAuthStore(),
  route = useRoute(),
  router = useRouter(),
  id = Number(route.params.id),
  targetRecord = String(route.query.record || ""),
  dataset = ref<any>({ fieldDefinition: [] }),
  receivedShares = ref<any[]>([]),
  rows = ref<any[]>([]),
  total = ref(0),
  loading = ref(false),
  dialog = ref(false),
  editing = ref<any | null>(null),
  reason = ref(""),
  form = reactive<Record<string, any>>({}),
  submitting = ref(false),
  operationKey = ref(""),
  page = ref(1),
  importDialog = ref(false),
  importing = ref(false),
  importFile = ref<File | null>(null),
  importFileInput = ref<HTMLInputElement | null>(null),
  templateDownloading = ref(""),
  importJobs = ref<any[]>([]),
  selectedJob = ref<any | null>(null),
  workflowDialog = ref(false),
  workflowAction = ref<"submit" | "review" | "publish">("submit"),
  workflowRow = ref<any>(null),
  exportDialog = ref(false),
  exportFields = ref<any[]>([]),
  selectedExportFields = ref<string[]>([]),
  exportFormat = ref("csv"),
  includeAttachments = ref(false),
  exporting = ref(false),
  workflowDetailVisible = ref(false),
  workflowDetail = ref<any | null>(null),
  recordFilesVisible = ref(false),
  recordFileRow = ref<any | null>(null),
  recordFiles = ref<any[]>([]),
  recordFilesLoading = ref(false),
  recordFileSelection = ref<File[]>([]),
  recordFileInput = ref<HTMLInputElement | null>(null),
  recordFileUploading = ref(false),
  recordUploadProgress = ref(0),
  recordUploadPhase = ref("");
const workflow = reactive({
  reason: "",
  secondaryPassword: "",
  approved: true,
});
const fields = computed<any[]>(() => dataset.value.fieldDefinition || []);
const placeholderSchema = computed(
  () =>
    fields.value.length === 1 &&
    fields.value[0]?.key === "fieldKey" &&
    fields.value[0]?.label === "字段名称",
);
const isAdmin = computed(() => auth.user?.roles.includes("ADMIN") || false);
const directAccess = computed(
  () =>
    isAdmin.value ||
    auth.user?.assignedScopes?.includes(Number(dataset.value.dataScopeId)) ||
    false,
);
const sharedOperations = computed(() => {
  const result = new Set<string>();
  for (const rule of receivedShares.value) {
    if (rule.status !== "ACTIVE") continue;
    const matches =
      (rule.resourceType === "DATA_SCOPE" &&
        Number(rule.resourceId) === Number(dataset.value.dataScopeId)) ||
      (rule.resourceType === "DATASET" && Number(rule.resourceId) === id);
    if (matches)
      for (const operation of rule.operations || [])
        result.add(String(operation));
  }
  return result;
});
const canWrite = computed(() => directAccess.value);
const canImport = computed(() => auth.can("dataset:import") && canWrite.value);
const canExport = computed(
  () =>
    auth.can("dataset:export") &&
    (directAccess.value || sharedOperations.value.has("EXPORT")),
);
const canDownload = computed(
  () =>
    auth.can("file:read") &&
    (directAccess.value || sharedOperations.value.has("DOWNLOAD")),
);
const canAudit = computed(() => auth.can("dataset:audit") && canWrite.value);
async function load() {
  loading.value = true;
  try {
    dataset.value = dataOf(await http.get(`/datasets/${id}`));
    const [result, shares] = await Promise.all([
      http.get(`/datasets/${id}/records`, {
        params: { pageNum: page.value, pageSize: 50 },
      }),
      http.get("/shares/mine"),
    ]);
    receivedShares.value = dataOf(shares);
    const pageResult = dataOf<any>(result);
    rows.value = pageResult.list;
    total.value = pageResult.total;
    if (canImport.value)
      importJobs.value = dataOf(await http.get(`/datasets/${id}/imports`));
    else importJobs.value = [];
    if (targetRecord && !rows.value.some((row) => row.id === targetRecord)) {
      const target = dataOf<any>(
        await http.get(`/datasets/${id}/records/${targetRecord}`),
      );
      rows.value.unshift(target);
    }
  } finally {
    loading.value = false;
  }
}
function rowClass({ row }: any) {
  return row.id === targetRecord ? "target-record" : "";
}
function reset() {
  Object.keys(form).forEach((k) => delete form[k]);
  operationKey.value = crypto.randomUUID();
}
function openCreate() {
  editing.value = null;
  reason.value = "";
  reset();
  fields.value.forEach(
    (f) =>
      (form[f.key] =
        f.type === "number"
          ? null
          : f.type === "boolean"
            ? false
            : f.type === "object"
              ? "{}"
              : f.type === "array"
                ? "[]"
                : ""),
  );
  dialog.value = true;
}
function openEdit(row: any) {
  editing.value = row;
  reason.value = "数据修正";
  reset();
  fields.value.forEach((f) => {
    const value = row.data?.[f.key];
    form[f.key] =
      (f.type === "object" || f.type === "array") && value != null
        ? JSON.stringify(value, null, 2)
        : (value ?? (f.type === "boolean" ? false : ""));
  });
  dialog.value = true;
}
function payload() {
  const result: any = {};
  for (const f of fields.value) {
    const value = form[f.key];
    if (f.type === "object" || f.type === "array") {
      try {
        result[f.key] = JSON.parse(value);
      } catch {
        throw new Error(`${f.label}不是有效JSON`);
      }
    } else result[f.key] = value;
  }
  return result;
}
async function save() {
  if (submitting.value) return;
  submitting.value = true;
  try {
    const data = payload();
    if (editing.value)
      await http.put(
        `/datasets/${id}/records/${editing.value.id}`,
        { reason: reason.value, data },
        {
          params: { version: editing.value.version },
          headers: { "X-Idempotency-Key": operationKey.value },
        },
      );
    else
      await http.post(`/datasets/${id}/records`, data, {
        headers: { "X-Idempotency-Key": operationKey.value },
      });
    ElMessage.success("记录已保存");
    dialog.value = false;
    load();
  } catch (e: any) {
    if (!e?.response) ElMessage.error(e.message);
  } finally {
    submitting.value = false;
  }
}
async function openExport() {
  const result = dataOf<any>(await http.get(`/datasets/${id}/download-fields`));
  exportFields.value = result.fields || [];
  selectedExportFields.value = exportFields.value.map(
    (field: any) => field.value,
  );
  exportFormat.value = "csv";
  includeAttachments.value = false;
  exportDialog.value = true;
}
async function exportSelected() {
  if (!selectedExportFields.value.length)
    return ElMessage.warning("至少选择一个导出字段");
  if (includeAttachments.value && !canDownload.value)
    return ElMessage.warning("附件压缩包还需要附件下载授权");
  exporting.value = true;
  try {
    const endpoint = `/datasets/${id}/exports`,
      body = {
        format: exportFormat.value,
        fields: [...selectedExportFields.value],
        includeAttachments: includeAttachments.value,
      };
    const issued = dataOf<any>(
      await withStepUp(
        {
          purpose: "EXPORT",
          method: "POST",
          target: endpoint,
          payload: body,
          label: `导出数据集“${dataset.value.name}”的 ${selectedExportFields.value.length} 个字段${includeAttachments.value ? "及可见附件" : ""}`,
        },
        (token) => http.post(endpoint, body, { headers: stepUpHeader(token) }),
      ),
    );
    const link = document.createElement("a");
    link.href = issued.downloadUrl;
    link.rel = "noopener noreferrer";
    link.download = "";
    document.body.appendChild(link);
    link.click();
    link.remove();
    ElMessage.success("一次性流式下载已开始；敏感字段已按字段策略脱敏");
    exportDialog.value = false;
  } finally {
    exporting.value = false;
  }
}
function chooseImport(event: Event) {
  importFile.value = (event.target as HTMLInputElement).files?.[0] || null;
}
async function downloadImportTemplate(format: "csv" | "json" | "xlsx") {
  templateDownloading.value = format;
  try {
    const blob = (await http.get(`/datasets/${id}/imports/template`, {
      params: { format },
      responseType: "blob",
    })) as unknown as Blob;
    const url = URL.createObjectURL(blob),
      link = document.createElement("a");
    link.href = url;
    link.download = `${dataset.value.name || "数据集"}-导入模板.${format}`;
    link.click();
    window.setTimeout(() => URL.revokeObjectURL(url), 1000);
    ElMessage.success(`${format.toUpperCase()} 导入模板已下载`);
  } finally {
    templateDownloading.value = "";
  }
}
function rawDataSummary(value: any) {
  const text = JSON.stringify(value ?? {});
  return text.length > 180 ? `${text.slice(0, 180)}…` : text;
}
async function runImport() {
  if (!importFile.value) return;
  importing.value = true;
  try {
    const body = new FormData();
    body.append("file", importFile.value);
    const result = dataOf<any>(
      await http.post(`/datasets/${id}/imports`, body),
    );
    ElMessage.success(
      `导入完成：成功 ${result.successCount}，失败 ${result.failureCount}`,
    );
    selectedJob.value = result;
    importFile.value = null;
    if (importFileInput.value) importFileInput.value.value = "";
    await load();
  } finally {
    importing.value = false;
  }
}
async function inspectJob(row: any) {
  selectedJob.value = dataOf(await http.get(`/datasets/imports/${row.id}`));
}
async function inspectWorkflow(row: any) {
  workflowDetail.value = dataOf(
    await http.get(`/datasets/${id}/records/${row.id}/workflow`),
  );
  workflowDetailVisible.value = true;
}
async function verifySignature(row: any) {
  row.verifying = true;
  try {
    const result = dataOf<any>(
      await http.get(
        `/datasets/${id}/records/${workflowDetail.value.recordId}/workflow/signatures/${row.id}/verify`,
      ),
    );
    row.verification = result;
    result.valid
      ? ElMessage.success(`签名验真通过 · 密钥 ${result.signatureKeyId}`)
      : ElMessage.error(result.message || "签名验真失败");
  } finally {
    row.verifying = false;
  }
}
async function deleteRecord(row: any) {
  try {
    const result = await ElMessageBox.prompt(
      "请输入删除原因。删除将保留原记录、操作人和审计证据。",
      "受控删除记录",
      {
        confirmButtonText: "确认删除",
        cancelButtonText: "取消",
        inputPattern: /\S+/,
        inputErrorMessage: "删除原因不能为空",
        type: "warning",
      },
    );
    const params = new URLSearchParams({
      version: String(row.version),
      reason: result.value,
    });
    const endpoint = `/datasets/${id}/records/${row.id}?${params.toString()}`;
    await withStepUp(
      {
        purpose: "DELETE",
        method: "DELETE",
        target: endpoint,
        label: `删除数据集“${dataset.value.name}”中的记录 ${row.id}`,
      },
      (token) =>
        http.delete(endpoint, {
          headers: {
            ...stepUpHeader(token),
            "X-Idempotency-Key": crypto.randomUUID(),
          },
        }),
    );
    ElMessage.success("记录已受控删除");
    await load();
  } catch (error: any) {
    if (error !== "cancel" && error !== "close" && !error?.response)
      throw error;
  }
}
function openWorkflow(action: "submit" | "review" | "publish", row: any) {
  workflowAction.value = action;
  workflowRow.value = row;
  workflow.reason =
    action === "submit"
      ? "数据完整，提交审核"
      : action === "review"
        ? "数据审核结论"
        : "批准发布";
  workflow.secondaryPassword = "";
  workflow.approved = true;
  workflowDialog.value = true;
}
async function executeWorkflow() {
  const row = workflowRow.value;
  const endpoint = `/datasets/${id}/records/${row.id}/workflow/${workflowAction.value}`;
  const body: any = { version: row.version, reason: workflow.reason };
  if (workflowAction.value !== "submit")
    body.secondaryPassword = workflow.secondaryPassword;
  if (workflowAction.value === "review") body.approved = workflow.approved;
  const state = dataOf<any>(await http.post(endpoint, body));
  row.workflowStatus = state.status;
  ElMessage.success(
    workflowAction.value === "submit"
      ? "已提交审核"
      : workflowAction.value === "review"
        ? "审核已签名"
        : "发布已签名",
  );
  workflowDialog.value = false;
  load();
}
function statusLabel(status: string) {
  return (
    (
      {
        DRAFT: "草稿",
        PENDING_REVIEW: "待审核",
        APPROVED: "审核通过",
        PUBLISHED: "已发布",
        REJECTED: "已驳回",
      } as any
    )[status] || status
  );
}
function statusType(status: string) {
  return status === "PUBLISHED"
    ? "success"
    : status === "REJECTED"
      ? "danger"
      : status === "PENDING_REVIEW"
        ? "warning"
        : status === "APPROVED"
          ? "primary"
          : "info";
}
function recordRef() {
  return `${id}:${recordFileRow.value?.id || ""}`;
}
async function loadRecordFiles() {
  if (!recordFileRow.value) return;
  recordFilesLoading.value = true;
  try {
    recordFiles.value = dataOf(
      await http.get("/files", {
        params: { businessType: "DATASET_RECORD", businessRef: recordRef() },
      }),
    );
  } finally {
    recordFilesLoading.value = false;
  }
}
async function openRecordFiles(row: any) {
  recordFileRow.value = row;
  recordFileSelection.value = [];
  recordFilesVisible.value = true;
  await loadRecordFiles();
}
function chooseRecordFile(event: Event) {
  recordFileSelection.value = Array.from(
    (event.target as HTMLInputElement).files || [],
  );
}
async function uploadRecordFile() {
  if (!recordFileSelection.value.length) return;
  if (recordFileSelection.value.length > 20)
    return ElMessage.warning("单批最多20个附件");
  recordFileUploading.value = true;
  recordUploadProgress.value = 0;
  try {
    const target = {
        businessType: "DATASET_RECORD",
        businessRef: recordRef(),
        dataScopeId: Number(dataset.value.dataScopeId),
      },
      large = recordFileSelection.value.filter(isLargeFile),
      small = recordFileSelection.value.filter((file) => !isLargeFile(file));
    for (const file of large)
      await resumableUpload(file, target, (value) => {
        recordUploadProgress.value = value.percent;
        recordUploadPhase.value =
          value.phase === "hashing"
            ? "完整性摘要"
            : value.phase === "uploading"
              ? `分片 ${value.uploadedChunks}/${value.totalChunks}`
              : "合并校验";
      });
    if (small.length) {
      const body = new FormData();
      small.forEach((file) => body.append("files", file));
      body.append("businessType", "DATASET_RECORD");
      body.append("businessRef", recordRef());
      body.append("dataScopeId", String(dataset.value.dataScopeId));
      const result = dataOf<any>(await http.post("/files/batch", body));
      if (result.failureCount)
        ElMessage.warning(
          `成功 ${result.successCount} 个，失败 ${result.failureCount} 个`,
        );
    }
    ElMessage.success("记录附件已关联并校验");
    recordFileSelection.value = [];
    if (recordFileInput.value) recordFileInput.value.value = "";
    await loadRecordFiles();
  } finally {
    recordFileUploading.value = false;
  }
}
async function fetchRecordFile(row: any, preview = false) {
  const blob = (await http.get(`/files/${row.id}`, {
    params: { preview },
    responseType: "blob",
  })) as unknown as Blob;
  const url = URL.createObjectURL(blob);
  if (preview) window.open(url, "_blank", "noopener,noreferrer");
  else {
    const link = document.createElement("a");
    link.href = url;
    link.download = row.originalName;
    link.click();
  }
  window.setTimeout(() => URL.revokeObjectURL(url), 60000);
}
async function deleteRecordFile(row: any) {
  await ElMessageBox.confirm(
    `确认删除记录附件“${row.originalName}”？删除会写入审计。`,
    "删除记录附件",
    { type: "warning" },
  );
  const endpoint = `/files/${row.id}`;
  await withStepUp(
    {
      purpose: "DELETE",
      method: "DELETE",
      target: endpoint,
      label: `删除记录附件“${row.originalName}”`,
    },
    (token) => http.delete(endpoint, { headers: stepUpHeader(token) }),
  );
  ElMessage.success("记录附件已删除");
  await loadRecordFiles();
}
function fileSize(value: number) {
  if (value < 1024) return `${value} B`;
  if (value < 1048576) return `${(value / 1024).toFixed(1)} KB`;
  return `${(value / 1048576).toFixed(1)} MB`;
}
function recordPreviewable(type: string) {
  return [
    "text/plain",
    "application/pdf",
    "image/png",
    "image/jpeg",
    "image/gif",
    "image/webp",
    "image/bmp",
  ].includes(String(type).toLowerCase());
}
onMounted(load);
</script>
<template>
  <div class="page">
    <div class="back-row">
      <el-button class="back-button" @click="router.push('/datasets')"
        >← 返回数据集</el-button
      ><el-breadcrumb separator="/"
        ><el-breadcrumb-item to="/datasets">数据资产</el-breadcrumb-item
        ><el-breadcrumb-item>{{
          dataset.name
        }}</el-breadcrumb-item></el-breadcrumb
      >
    </div>
    <div class="page-head">
      <div>
        <h1 class="page-title record-title">{{ dataset.name }}</h1>
        <div class="page-subtitle">
          {{ dataset.description }} · {{ total }} 条有效记录 · 业务码前缀
          {{ dataset.recordCodePrefix }} · v{{ dataset.version
          }}<template v-if="dataset.templateId">
            · 来源模板 #{{ dataset.templateId }} v{{
              dataset.templateVersion
            }}</template
          >
        </div>
      </div>
      <div>
        <el-button v-if="canImport" @click="importDialog = true"
          >批量导入</el-button
        ><el-button v-if="canExport" :disabled="!total" @click="openExport"
          >选择字段导出</el-button
        ><el-button
          v-if="auth.can('dataset:create') && canWrite"
          type="primary"
          @click="openCreate"
          >新增记录</el-button
        >
      </div>
    </div>
    <el-alert
      v-if="placeholderSchema"
      title="该数据集绑定的模板版本只包含默认占位字段“字段名称 / fieldKey”。请先完善模板，再创建新的数据集；已有记录的数据集不会自动替换字段，以免破坏数据对应关系。"
      type="warning"
      :closable="false"
      class="target-alert"
    /><el-alert
      v-if="!directAccess"
      :title="`当前通过选择性共享访问；允许操作：${[...sharedOperations].join('、') || 'READ'}。共享不会授予写入、审核或发布权限。`"
      type="info"
      :closable="false"
      class="target-alert"
    /><el-alert
      v-if="targetRecord"
      title="已从检索结果定位目标记录"
      type="success"
      :closable="false"
      class="target-alert"
    />
    <div class="surface schema-bar">
      <div class="schema-summary">
        <b>字段结构</b
        ><small
          >{{ fields.length }} 个字段{{
            dataset.templateId ? " · 模板快照" : ""
          }}</small
        >
      </div>
      <span v-for="f in fields" :key="f.key"
        ><b>{{ f.label }}</b
        ><small
          >{{ f.key }} · {{ f.type }}{{ f.required ? " · 必填" : "" }}</small
        ></span
      >
    </div>
    <div class="surface">
      <el-table
        class="records-table"
        :data="rows"
        v-loading="loading"
        :row-class-name="rowClass"
        ><el-table-column prop="businessCode" label="业务标识码" width="220"
          ><template #default="{ row }"
            ><el-tooltip :content="`技术记录ID：${row.id}`"
              ><span class="mono short-id">{{
                row.businessCode
              }}</span></el-tooltip
            ></template
          ></el-table-column
        ><el-table-column
          v-for="f in fields"
          :key="f.key"
          :label="f.label"
          min-width="130"
          ><template #default="{ row }">{{
            typeof row.data?.[f.key] === "object"
              ? JSON.stringify(row.data?.[f.key])
              : (row.data?.[f.key] ?? "—")
          }}</template></el-table-column
        ><el-table-column
          prop="createdByName"
          label="录入人"
          width="100"
        /><el-table-column label="状态" width="100"
          ><template #default="{ row }"
            ><el-tag :type="statusType(row.workflowStatus)">{{
              statusLabel(row.workflowStatus)
            }}</el-tag></template
          ></el-table-column
        ><el-table-column
          prop="version"
          label="版本"
          width="70"
        /><el-table-column label="操作" width="390" fixed="right"
          ><template #default="{ row }"
            ><div class="record-actions">
              <el-button
                v-if="auth.can('file:read')"
                link
                @click="openRecordFiles(row)"
                >记录附件</el-button
              ><el-button link @click="inspectWorkflow(row)">签名证据</el-button
              ><el-button
                v-if="
                  auth.can('dataset:update') &&
                  canWrite &&
                  (row.workflowStatus === 'DRAFT' ||
                    row.workflowStatus === 'REJECTED')
                "
                link
                type="primary"
                @click="openEdit(row)"
                >受控更正</el-button
              ><el-button
                v-if="
                  auth.can('dataset:update') &&
                  canWrite &&
                  (row.workflowStatus === 'DRAFT' ||
                    row.workflowStatus === 'REJECTED')
                "
                link
                @click="openWorkflow('submit', row)"
                >提交审核</el-button
              ><el-button
                v-if="canAudit && row.workflowStatus === 'PENDING_REVIEW'"
                link
                type="warning"
                @click="openWorkflow('review', row)"
                >独立审核</el-button
              ><el-button
                v-if="
                  auth.can('dataset:publish') &&
                  canAudit &&
                  row.workflowStatus === 'APPROVED'
                "
                link
                type="success"
                @click="openWorkflow('publish', row)"
                >签名发布</el-button
              ><el-button
                v-if="
                  auth.can('dataset:delete') &&
                  canWrite &&
                  (row.workflowStatus === 'DRAFT' ||
                    row.workflowStatus === 'REJECTED')
                "
                link
                type="danger"
                @click="deleteRecord(row)"
                >删除</el-button
              >
            </div></template
          ></el-table-column
        ></el-table
      ><el-pagination
        v-if="total > 50"
        v-model:current-page="page"
        :page-size="50"
        :total="total"
        layout="prev,pager,next,total"
        @current-change="load"
      />
    </div>
    <el-dialog
      v-model="dialog"
      :title="editing ? `受控更正 · ${editing.businessCode}` : '新增数据记录'"
      width="620"
      ><el-alert
        v-if="editing"
        title="更正将保留修改前后值、操作人、时间和原因；业务标识码保持不变。"
        type="warning"
        :closable="false"
      /><el-form label-position="top" class="record-form"
        ><el-form-item
          v-for="f in fields"
          :key="f.key"
          :label="f.label + (f.required ? ' *' : '')"
          ><el-input-number
            v-if="f.type === 'number'"
            v-model="form[f.key]"
            controls-position="right" /><el-switch
            v-else-if="f.type === 'boolean'"
            v-model="form[f.key]" /><el-date-picker
            v-else-if="f.type === 'date'"
            v-model="form[f.key]"
            type="date"
            value-format="YYYY-MM-DD" /><el-input
            v-else-if="f.type === 'object' || f.type === 'array'"
            v-model="form[f.key]"
            type="textarea"
            :rows="4"
            class="mono" /><el-input
            v-else
            v-model="form[f.key]" /></el-form-item
        ><el-form-item v-if="editing" label="更正原因 *"
          ><el-input v-model="reason" type="textarea" /></el-form-item></el-form
      ><template #footer
        ><el-button @click="dialog = false">取消</el-button
        ><el-button type="primary" :loading="submitting" @click="save"
          >确认保存</el-button
        ></template
      ></el-dialog
    >
    <el-dialog
      v-model="importDialog"
      title="批量导入 CSV / JSON / XLSX"
      width="820"
      ><el-alert
        :title="`每条成功记录自动生成 ${dataset.recordCodePrefix || '业务'}-日期-流水号；同一数据集前缀固定，不同业务数据集使用不同前缀。`"
        type="info"
        :closable="false"
        class="target-alert" />
      <div class="import-guide">
        <div>
          <b>第一步：下载当前数据集的标准模板</b
          ><span
            >模板已包含
            {{ fields.length }} 个字段及约束说明，无需自行猜测表头。</span
          >
        </div>
        <el-button-group
          ><el-button
            :loading="templateDownloading === 'csv'"
            @click="downloadImportTemplate('csv')"
            >CSV 模板</el-button
          ><el-button
            :loading="templateDownloading === 'json'"
            @click="downloadImportTemplate('json')"
            >JSON 模板</el-button
          ><el-button
            type="primary"
            :loading="templateDownloading === 'xlsx'"
            @click="downloadImportTemplate('xlsx')"
            >XLSX 模板</el-button
          ></el-button-group
        >
      </div>
      <div class="import-upload">
        <input
          id="dataset-import-file"
          ref="importFileInput"
          class="import-file-input"
          type="file"
          accept=".csv,.json,.xlsx"
          @change="chooseImport"
        /><label class="import-file-picker" for="dataset-import-file"
          >选择导入文件</label
        ><span class="import-file-name">{{
          importFile?.name ||
          "尚未选择文件 · 支持 CSV、JSON、XLSX，单次最多 5000 行"
        }}</span
        ><el-button
          type="primary"
          :loading="importing"
          :disabled="!importFile"
          @click="runImport"
          >执行导入</el-button
        >
      </div>
      <div class="import-job-heading">
        <b>导入作业</b
        ><span>合法行独立落库，错误行保留行号、原始值摘要和失败原因</span>
      </div>
      <el-table :data="importJobs" max-height="250"
        ><el-table-column
          prop="fileName"
          label="文件"
          min-width="160"
        /><el-table-column
          prop="status"
          label="状态"
          width="90"
        /><el-table-column
          prop="totalCount"
          label="总数"
          width="72"
        /><el-table-column
          prop="successCount"
          label="成功"
          width="72"
        /><el-table-column
          prop="failureCount"
          label="失败"
          width="72"
        /><el-table-column label="操作" width="90"
          ><template #default="{ row }"
            ><el-button
              link
              :disabled="!row.failureCount && !row.successCount"
              @click="inspectJob(row)"
              >作业详情</el-button
            ></template
          ></el-table-column
        ></el-table
      >
      <div v-if="selectedJob?.generatedRecords?.length" class="generated-list">
        <h4>
          本次生成的业务标识码<span v-if="selectedJob.generatedRecordsTruncated"
            >（仅展示前100条）</span
          >
        </h4>
        <el-table :data="selectedJob.generatedRecords" max-height="220">
          <el-table-column prop="rowNumber" label="源文件行" width="90" />
          <el-table-column prop="businessCode" label="业务标识码">
            <template #default="{ row }"
              ><span class="mono">{{ row.businessCode }}</span></template
            >
          </el-table-column>
        </el-table>
      </div>
      <div v-if="selectedJob?.errors?.length" class="error-list">
        <h4>错误明细</h4>
        <el-table :data="selectedJob.errors" max-height="220"
          ><el-table-column
            prop="rowNumber"
            label="行号"
            width="70" /><el-table-column label="原始值摘要" min-width="230"
            ><template #default="{ row }"
              ><code>{{ rawDataSummary(row.rawData) }}</code></template
            ></el-table-column
          ><el-table-column
            prop="errorMessage"
            label="失败原因"
            min-width="220"
        /></el-table></div
    ></el-dialog>
    <el-dialog
      v-model="workflowDialog"
      :title="
        workflowAction === 'submit'
          ? '提交审核'
          : workflowAction === 'review'
            ? '独立审核与电子签名'
            : '发布电子签名'
      "
      width="520"
      ><el-alert
        v-if="workflowAction !== 'submit'"
        title="二级密码仅用于本次身份复核，不会写入签名；签名绑定记录、附件摘要、密钥版本、来源IP、含义、原因和时间。"
        type="warning"
        :closable="false"
      /><el-form label-position="top"
        ><el-form-item v-if="workflowAction === 'review'" label="审核结论"
          ><el-radio-group v-model="workflow.approved"
            ><el-radio-button :value="true">通过</el-radio-button
            ><el-radio-button :value="false"
              >驳回</el-radio-button
            ></el-radio-group
          ></el-form-item
        ><el-form-item label="原因/意见"
          ><el-input v-model="workflow.reason" type="textarea" /></el-form-item
        ><el-form-item v-if="workflowAction !== 'submit'" label="6位二级密码"
          ><el-input
            v-model="workflow.secondaryPassword"
            type="password"
            maxlength="6"
            show-password /></el-form-item></el-form
      ><template #footer
        ><el-button @click="workflowDialog = false">取消</el-button
        ><el-button
          type="primary"
          :disabled="
            !workflow.reason.trim() ||
            (workflowAction !== 'submit' &&
              !/^[0-9]{6}$/.test(workflow.secondaryPassword))
          "
          @click="executeWorkflow"
          >确认</el-button
        ></template
      ></el-dialog
    >
    <el-dialog v-model="exportDialog" title="选择导出字段" width="560"
      ><el-alert
        title="手机号、邮箱、证件号及标记为敏感的字段会在 CSV、JSON、XLSX 中统一脱敏。"
        type="info"
        :closable="false"
      /><el-form label-position="top" style="margin-top: 14px"
        ><el-form-item label="文件格式"
          ><el-radio-group v-model="exportFormat"
            ><el-radio-button value="csv">CSV</el-radio-button
            ><el-radio-button value="xlsx">Excel XLSX</el-radio-button
            ><el-radio-button value="json"
              >JSON</el-radio-button
            ></el-radio-group
          ></el-form-item
        ><el-form-item label="字段（至少一项）"
          ><el-checkbox-group
            v-model="selectedExportFields"
            class="export-fields"
            ><el-checkbox
              v-for="field in exportFields"
              :key="field.value"
              :value="field.value"
              >{{ field.label
              }}<el-tag v-if="field.sensitive" size="small" type="warning"
                >{{ field.maskType }}脱敏</el-tag
              ></el-checkbox
            ></el-checkbox-group
          ></el-form-item
        ><el-form-item label="附件"
          ><el-checkbox v-model="includeAttachments" :disabled="!canDownload"
            >生成数据、附件清单及可见附件 ZIP</el-checkbox
          ><small v-if="!canDownload" class="form-help"
            >当前共享未包含 DOWNLOAD，不能生成附件包。</small
          ></el-form-item
        ></el-form
      ><template #footer
        ><el-button @click="exportDialog = false">取消</el-button
        ><el-button
          type="primary"
          :loading="exporting"
          :disabled="!selectedExportFields.length"
          @click="exportSelected"
          >生成导出</el-button
        ></template
      ></el-dialog
    >
    <el-dialog
      v-model="recordFilesVisible"
      :title="`记录附件 · ${recordFileRow?.id?.slice(-10) || ''}`"
      width="820"
      ><div
        v-if="auth.can('file:upload') && canWrite"
        class="record-file-upload"
      >
        <input
          ref="recordFileInput"
          type="file"
          multiple
          @change="chooseRecordFile"
        /><span
          >{{
            recordFileSelection.length
              ? `已选择 ${recordFileSelection.length} 个文件`
              : "批量选择报告、照片、曲线或仿真文件"
          }}<small v-if="recordFileUploading"
            >{{ recordUploadPhase }} · {{ recordUploadProgress }}%</small
          ></span
        ><el-button
          type="primary"
          :loading="recordFileUploading"
          :disabled="!recordFileSelection.length"
          @click="uploadRecordFile"
          >上传、续传并关联</el-button
        >
      </div>
      <el-table
        :data="recordFiles"
        v-loading="recordFilesLoading"
        max-height="380"
        ><el-table-column
          prop="originalName"
          label="文件名"
          min-width="200"
        /><el-table-column
          prop="contentType"
          label="类型"
          width="150"
        /><el-table-column label="大小" width="90"
          ><template #default="{ row }">{{
            fileSize(row.sizeBytes)
          }}</template></el-table-column
        ><el-table-column
          prop="uploadedByName"
          label="上传人"
          width="100"
        /><el-table-column
          prop="createdTime"
          label="时间"
          width="170"
        /><el-table-column label="操作" width="190"
          ><template #default="{ row }"
            ><el-button
              v-if="canDownload && recordPreviewable(row.contentType)"
              link
              type="primary"
              @click="fetchRecordFile(row, true)"
              >安全预览</el-button
            ><el-button v-if="canDownload" link @click="fetchRecordFile(row)"
              >下载</el-button
            ><el-button
              v-if="auth.can('file:delete') && canWrite"
              link
              type="danger"
              @click="deleteRecordFile(row)"
              >删除</el-button
            ></template
          ></el-table-column
        ></el-table
      >
      <div v-if="!recordFiles.length && !recordFilesLoading" class="empty-note">
        当前记录暂无附件
      </div></el-dialog
    >
    <el-drawer
      v-model="workflowDetailVisible"
      title="记录工作流与签名证据"
      size="720"
      ><template v-if="workflowDetail"
        ><el-descriptions :column="2" border
          ><el-descriptions-item label="记录版本"
            >v{{ workflowDetail.recordVersion }}</el-descriptions-item
          ><el-descriptions-item label="状态">{{
            statusLabel(workflowDetail.status)
          }}</el-descriptions-item
          ><el-descriptions-item label="提交时间">{{
            workflowDetail.submittedTime || "—"
          }}</el-descriptions-item
          ><el-descriptions-item label="审核时间">{{
            workflowDetail.reviewedTime || "—"
          }}</el-descriptions-item
          ><el-descriptions-item label="审核意见" :span="2">{{
            workflowDetail.reviewComment || "—"
          }}</el-descriptions-item
          ><el-descriptions-item label="发布时间" :span="2">{{
            workflowDetail.publishedTime || "—"
          }}</el-descriptions-item></el-descriptions
        >
        <h3 class="signature-title">电子签名</h3>
        <el-table :data="workflowDetail.signatures || []"
          ><el-table-column
            prop="meaning"
            label="含义"
            width="120"
          /><el-table-column
            prop="signerUsername"
            label="签名人"
            width="90"
          /><el-table-column
            prop="targetVersion"
            label="版本"
            width="65"
          /><el-table-column prop="reason" label="原因" /><el-table-column
            prop="signatureKeyId"
            label="密钥版本"
            width="110"
          /><el-table-column label="验真" width="95"
            ><template #default="{ row }"
              ><el-button
                link
                :type="row.verification?.valid ? 'success' : 'primary'"
                :loading="row.verifying"
                @click="verifySignature(row)"
                >{{
                  row.verification?.valid ? "已通过" : "立即验签"
                }}</el-button
              ></template
            ></el-table-column
          ></el-table
        >
        <div v-if="!workflowDetail.signatures?.length" class="empty-note">
          尚无电子签名
        </div></template
      ></el-drawer
    >
  </div>
</template>
<style scoped>
.back-row {
  display: flex;
  align-items: center;
  gap: 14px;
  margin-bottom: 18px;
}
.back-button {
  border-color: #28516d;
  background: rgba(10, 36, 57, 0.72);
  color: #a8c9dc;
}
.record-title {
  margin-top: 0;
}
.target-alert {
  margin-bottom: 12px;
}
.schema-bar {
  padding: 13px 18px;
  display: flex;
  align-items: stretch;
  gap: 10px;
  margin-bottom: 14px;
  overflow: auto;
}
.schema-summary {
  min-width: 125px;
  padding-right: 16px;
  border-right: 1px solid #21435c;
}
.schema-summary b,
.schema-summary small,
.schema-bar span b,
.schema-bar span small {
  display: block;
}
.schema-summary b {
  font-size: 11px;
  color: #c8e5f2;
}
.schema-summary small {
  font-size: 9px;
  color: #598199;
  margin-top: 4px;
}
.schema-bar span {
  min-width: 145px;
  padding-right: 10px;
  border-right: 1px solid #21435c;
}
.schema-bar span b {
  font-size: 11px;
}
.schema-bar span small {
  font-size: 9px;
  color: #7895aa;
  margin-top: 3px;
}
.record-actions {
  display: flex;
  align-items: center;
  white-space: nowrap;
  overflow: hidden;
}
.record-actions :deep(.el-button) {
  margin-left: 12px;
}
.record-actions :deep(.el-button:first-child) {
  margin-left: 0;
}
.records-table :deep(th.el-table-fixed-column--right) {
  background: #0c2238 !important;
}
.records-table :deep(td.el-table-fixed-column--right) {
  background: #081a2d !important;
}
.records-table :deep(.el-table__row:hover td.el-table-fixed-column--right) {
  background: #15334e !important;
}
.records-table :deep(.el-table-fixed-column--right) {
  box-shadow: -8px 0 18px rgba(0, 8, 18, 0.34);
}
.short-id {
  font-size: 11px;
  color: #45708e;
}
.record-form {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 0 16px;
  margin-top: 18px;
}
.record-form .el-form-item:last-child {
  grid-column: 1/-1;
}
.import-guide {
  display: flex;
  align-items: center;
  justify-content: space-between;
  gap: 20px;
  padding: 16px 18px;
  border: 1px solid #27516d;
  border-radius: 10px;
  background: linear-gradient(
    135deg,
    rgba(14, 52, 77, 0.9),
    rgba(7, 29, 48, 0.92)
  );
}
.import-guide b,
.import-guide span {
  display: block;
}
.import-guide b {
  color: #d8edf8;
  font-size: 13px;
}
.import-guide span {
  margin-top: 5px;
  color: #789bb0;
  font-size: 10px;
}
.import-upload {
  display: flex;
  align-items: center;
  gap: 12px;
  margin: 18px 0;
  padding: 14px 16px;
  border: 1px dashed #2d5c78;
  border-radius: 10px;
  background: #091d30;
}
.import-file-input {
  position: absolute;
  width: 1px;
  height: 1px;
  opacity: 0;
  pointer-events: none;
}
.import-file-picker {
  flex: none;
  padding: 9px 16px;
  border: 1px solid #2f7598;
  border-radius: 7px;
  background: #0d3651;
  color: #bfe7f7;
  cursor: pointer;
}
.import-file-picker:hover {
  border-color: #35c9ed;
  color: #fff;
}
.import-file-name {
  flex: 1;
  min-width: 0;
  color: #789bb0;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}
.import-job-heading {
  display: flex;
  align-items: baseline;
  gap: 14px;
  margin: 22px 0 10px;
}
.import-job-heading b {
  color: #d8edf8;
}
.import-job-heading span {
  color: #63879d;
  font-size: 10px;
}
.generated-list {
  margin-top: 15px;
  padding: 14px;
  border: 1px solid #235e54;
  border-radius: 8px;
  background: rgba(14, 58, 52, 0.45);
}
.generated-list h4 {
  margin: 0 0 10px;
  color: #9ce2d2;
}
.generated-list h4 span {
  margin-left: 6px;
  color: #729e96;
  font-weight: normal;
}
.error-list {
  margin-top: 15px;
  padding: 14px;
  border: 1px solid #633b45;
  border-radius: 8px;
  background: #1b1a27;
  max-height: 300px;
  overflow: auto;
}
.error-list h4 {
  margin: 0 0 10px;
  color: #ff9b9b;
}
.error-list code {
  color: #d9b5b5;
  white-space: normal;
  word-break: break-all;
}
.export-fields {
  display: grid;
  grid-template-columns: 1fr 1fr;
  gap: 8px;
}
.record-file-upload {
  display: flex;
  gap: 14px;
  align-items: center;
  margin-bottom: 16px;
  padding: 14px 16px;
  border: 1px dashed #2b5875;
  border-radius: 9px;
  background: linear-gradient(
    135deg,
    rgba(11, 42, 65, 0.86),
    rgba(6, 25, 43, 0.92)
  );
  box-shadow: inset 0 0 22px rgba(40, 158, 204, 0.035);
}
.record-file-upload input {
  width: 230px;
  color: #7899af;
  font-size: 12px;
}
.record-file-upload input::file-selector-button {
  margin-right: 10px;
  padding: 8px 13px;
  border: 1px solid #2d7395;
  border-radius: 6px;
  background: #0c3551;
  color: #bde9f8;
  font: inherit;
  cursor: pointer;
  transition: 0.18s ease;
}
.record-file-upload input::file-selector-button:hover {
  border-color: #41cfee;
  background: #104765;
  color: #f2fdff;
}
.record-file-upload span {
  flex: 1;
  min-width: 0;
  color: #8ba8bb;
}
.record-file-upload small {
  display: block;
  margin-top: 4px;
  color: #4ed8f5;
}
.signature-title {
  font-size: 15px;
  margin-top: 24px;
}
:deep(.target-record td) {
  background: #edf9f4 !important;
  box-shadow:
    inset 0 1px #71c4a3,
    inset 0 -1px #71c4a3;
}
@media (max-width: 700px) {
  .back-row {
    align-items: flex-start;
    flex-direction: column;
  }
  .page-head > div:last-child {
    display: flex;
    flex-wrap: wrap;
    gap: 8px;
  }
  .page-head > div:last-child .el-button {
    margin-left: 0;
  }
  .import-guide,
  .import-upload,
  .record-file-upload {
    align-items: stretch;
    flex-direction: column;
  }
  .import-file-name {
    white-space: normal;
  }
  .record-file-upload input {
    width: 100%;
  }
}
</style>
