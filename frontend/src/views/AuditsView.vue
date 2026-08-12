<script setup lang="ts">
import {computed,onMounted,ref} from 'vue'
import {ElMessage} from 'element-plus'
import {http,dataOf} from '../api/http'
import {stepUpHeader,withStepUp} from '../api/stepUp'
import {useAuthStore} from '../stores/auth'

const auth=useAuthStore()
const rows=ref<any[]>([]),total=ref(0),loading=ref(false),exporting=ref(false)
const module=ref(''),username=ref(''),operation=ref(''),keyword=ref(''),sortOrder=ref('DESC')
const timeRange=ref<[Date,Date]|[]>([]),pageNum=ref(1),pageSize=ref(20)
const verify=ref<any|null>(null),verifying=ref(false)
const canExport=computed(()=>auth.can('audit:export'))
const modules=['AUTH','USER','TEMPLATE','DATASET','TRACE','DEVICE','FILE','AUDIT','SEARCH','INTEGRATION']

function params(withPage=true){const value:any={module:module.value||undefined,username:username.value.trim()||undefined,operation:operation.value.trim().toUpperCase()||undefined,keyword:keyword.value.trim()||undefined,sortOrder:sortOrder.value};if(timeRange.value.length===2){value.from=timeRange.value[0].toISOString();value.to=timeRange.value[1].toISOString()}if(withPage){value.pageNum=pageNum.value;value.pageSize=pageSize.value}return value}
async function load(){loading.value=true;try{const page=dataOf<any>(await http.get('/audits',{params:params()}));rows.value=page.list;total.value=page.total}finally{loading.value=false}}
async function search(){pageNum.value=1;await load()}
async function reset(){module.value='';username.value='';operation.value='';keyword.value='';sortOrder.value='DESC';timeRange.value=[];pageNum.value=1;await load()}
async function check(){if(verifying.value)return;verifying.value=true;try{verify.value=dataOf(await http.post('/audits/verify'));verify.value.valid?ElMessage.success('审计签名链验证通过'):ElMessage.error('审计链验证失败')}finally{verifying.value=false}}
async function exportFile(format:'csv'|'xlsx'){exporting.value=true;try{const query=new URLSearchParams();Object.entries({...params(false),format}).forEach(([key,value])=>{if(value!=null&&value!=='')query.set(key,String(value))});const endpoint=`/audits/export?${query.toString()}`;const blob=await withStepUp({purpose:'EXPORT',method:'GET',target:endpoint,label:`按当前筛选导出${format==='xlsx'?' Excel':' CSV'}审计快照`},token=>http.get(endpoint,{responseType:'blob',headers:stepUpHeader(token)})) as unknown as Blob;const url=URL.createObjectURL(blob);const link=document.createElement('a');link.href=url;link.download=`audit-events-${new Date().toISOString().slice(0,10)}.${format}`;link.click();window.setTimeout(()=>URL.revokeObjectURL(url),1000);ElMessage.success('已按当前筛选条件导出审计快照')}finally{exporting.value=false}}
onMounted(load)
</script>

<template>
 <div class="page">
  <div class="page-head"><div><h1 class="page-title">审计中心</h1><div class="page-subtitle">不可篡改的追加写入日志与 HMAC 摘要链，共 {{total}} 条事件</div></div><div class="head-actions"><el-dropdown v-if="canExport" @command="exportFile"><el-button type="success" plain :loading="exporting">导出当前筛选<el-icon class="el-icon--right"><arrow-down/></el-icon></el-button><template #dropdown><el-dropdown-menu><el-dropdown-item command="csv">CSV</el-dropdown-item><el-dropdown-item command="xlsx">Excel</el-dropdown-item></el-dropdown-menu></template></el-dropdown><el-button type="primary" plain :loading="verifying" @click="check">验证审计链</el-button></div></div>
  <el-alert v-if="verify" :type="verify.valid?'success':'error'" :title="verify.message" :description="`已验证 ${verify.verifiedRecords} 条记录${verify.failureId?'，失败记录 #'+verify.failureId:''}`" show-icon class="verify"/>
  <div class="surface filters">
   <el-select v-model="module" placeholder="全部模块" clearable><el-option v-for="m in modules" :key="m" :value="m"/></el-select>
   <el-input v-model="username" placeholder="用户名" clearable @keyup.enter="search"/>
   <el-input v-model="operation" placeholder="操作类型，如 EXPORT" clearable @keyup.enter="search"/>
   <el-date-picker v-model="timeRange" type="datetimerange" start-placeholder="开始时间（含）" end-placeholder="结束时间（不含）"/>
   <el-input v-model="keyword" placeholder="说明或详情关键词" clearable @keyup.enter="search"/>
   <el-select v-model="sortOrder"><el-option label="时间倒序" value="DESC"/><el-option label="时间正序" value="ASC"/></el-select>
   <div class="filter-actions"><el-button type="primary" @click="search">查询</el-button><el-button @click="reset">重置</el-button></div>
  </div>
  <div class="surface"><el-table :data="rows" v-loading="loading"><el-table-column prop="id" label="#" width="75"/><el-table-column prop="createdTime" label="发生时间" width="190"/><el-table-column label="主体" width="130"><template #default="{row}"><b>{{row.username}}</b></template></el-table-column><el-table-column prop="module" label="模块" width="100"/><el-table-column prop="operation" label="操作" width="150"/><el-table-column prop="description" label="说明" min-width="250"/><el-table-column label="签名摘要" width="180"><template #default="{row}"><span class="mono digest">{{row.recordDigest.slice(0,16)}}…</span></template></el-table-column><el-table-column label="密钥" prop="auditKeyId" width="100"/></el-table><el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[10,20,50,100]" :total="total" layout="total, sizes, prev, pager, next" @current-change="load" @size-change="search"/></div>
 </div>
</template>

<style scoped>
.verify{margin-bottom:14px}.digest{font-size:10px;color:#3d6c8b}.head-actions,.filter-actions{display:flex;gap:8px}.filters{display:grid;grid-template-columns:150px 150px 190px minmax(360px,1fr);gap:10px;margin-bottom:14px;align-items:center}.filters>.el-input:nth-of-type(2){grid-column:1/3}.filters>.el-select:nth-of-type(2){width:150px}.filter-actions{justify-content:flex-end}.el-pagination{justify-content:flex-end;padding:16px}
@media(max-width:1100px){.filters{grid-template-columns:1fr 1fr}.filters>.el-input:nth-of-type(2){grid-column:auto}.filters .el-date-editor{width:100%}}
@media(max-width:650px){.filters{grid-template-columns:1fr}.head-actions{flex-wrap:wrap}.filters>.el-input:nth-of-type(2){grid-column:auto}}
</style>
