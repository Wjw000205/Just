<script setup lang="ts">
import {computed,onMounted,reactive,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {http,dataOf} from '../api/http'
import {useAuthStore} from '../stores/auth'

type Condition={id:string;field:string;value:string;matchMode:'FUZZY'|'EXACT'}
const auth=useAuthStore(),router=useRouter(),loading=ref(false),searched=ref(false)
const histories=ref<any[]>([])
const rows=ref<any[]>([]),total=ref(0),pageNum=ref(1),pageSize=ref(20)
const resourceTypes=ref<string[]>([]),timeRange=ref<[Date,Date]|[]>([]),sortBy=ref('CREATED_TIME'),sortOrder=ref('DESC')
const conditions=reactive<Condition[]>([{id:crypto.randomUUID(),field:'KEYWORD',value:'',matchMode:'FUZZY'}])
const resourceOptions=computed(()=>[
 {value:'DATASET',label:'数据集',show:auth.can('dataset:read')},{value:'RECORD',label:'数据记录',show:auth.can('dataset:read')},
 {value:'TRACE',label:'追溯实体',show:auth.can('trace:read')},{value:'DEVICE',label:'设备',show:auth.can('device:read')},
 {value:'TEMPLATE',label:'模板',show:auth.can('template:read')}
].filter(item=>item.show))
const fields=[['KEYWORD','任意关键词'],['MATERIAL','材料'],['PROCESS','工艺'],['BATCH','批次'],['DEVICE','设备'],['PRODUCT_MODEL','产品型号'],['CATEGORY','分类'],['TYPE','类型'],['STATUS','状态']]
const resourceLabels:Record<string,string>={DATASET:'数据集',RECORD:'数据记录',TRACE:'追溯实体',DEVICE:'设备',TEMPLATE:'模板'}

function addCondition(){conditions.push({id:crypto.randomUUID(),field:'KEYWORD',value:'',matchMode:'FUZZY'})}
async function fetchSuggestions(query:string,callback:(items:any[])=>void){try{callback(dataOf<any[]>(await http.get('/search/suggestions',{params:{prefix:query,limit:8}})))}catch{callback([])}}
function removeCondition(index:number){if(conditions.length===1){conditions[0].value='';return}conditions.splice(index,1)}
function body(){const value:any={resourceTypes:resourceTypes.value,conditions:conditions.filter(c=>c.value.trim()).map(c=>({field:c.field,value:c.value.trim(),matchMode:c.matchMode})),pageNum:pageNum.value,pageSize:pageSize.value,sortBy:sortBy.value,sortOrder:sortOrder.value};if(timeRange.value.length===2){value.from=timeRange.value[0].toISOString();value.to=timeRange.value[1].toISOString()}return value}
async function search(resetPage=false){if(!resourceTypes.value.length)return ElMessage.warning('请至少选择一种资源');if(resetPage)pageNum.value=1;loading.value=true;searched.value=true;try{const page=dataOf<any>(await http.post('/search',body()));rows.value=page.list;total.value=page.total;if(pageNum.value===1)loadHistory()}finally{loading.value=false}}
function reset(){resourceTypes.value=resourceOptions.value.map(v=>v.value);conditions.splice(0,conditions.length,{id:crypto.randomUUID(),field:'KEYWORD',value:'',matchMode:'FUZZY'});timeRange.value=[];sortBy.value='CREATED_TIME';sortOrder.value='DESC';pageNum.value=1;rows.value=[];total.value=0;searched.value=false}
function open(row:any){router.push(row.route)}
function summary(row:any){const text=String(row.summary||'').replace(/\s+/g,' ');return text.length>150?text.slice(0,150)+'…':text}
async function loadHistory(){histories.value=dataOf<any[]>(await http.get('/search/history'))}
async function clearHistory(){await http.delete('/search/history');histories.value=[];ElMessage.success('检索历史已清空')}
function useHistory(item:any){resourceTypes.value=[...(item.resourceTypes||[])];conditions.splice(0,conditions.length,...(item.conditions||[]).map((c:any)=>({id:crypto.randomUUID(),field:c.field,value:c.value,matchMode:c.matchMode})));if(!conditions.length)addCondition();timeRange.value=item.from&&item.to?[new Date(item.from),new Date(item.to)]:[];pageSize.value=Number(item.pageSize||20);sortBy.value=item.sortBy||'CREATED_TIME';sortOrder.value=item.sortOrder||'DESC';search(true)}
function historyLabel(item:any){return (item.conditions||[]).map((c:any)=>c.value).join(' + ')||'无关键词筛选'}
function highlightParts(value:any){const text=String(value??'');const terms=conditions.filter(c=>c.value.trim()).map(c=>c.value.trim()).sort((a,b)=>b.length-a.length);if(!terms.length)return[{text,hit:false}];const escaped=terms.map(term=>term.replace(/[.*+?^${}()|[\]\\]/g,'\\$&'));const matcher=new RegExp(`(${escaped.join('|')})`,'ig');return text.split(matcher).filter(Boolean).map(part=>({text:part,hit:terms.some(term=>term.toLowerCase()===part.toLowerCase())}))}
resourceTypes.value=resourceOptions.value.map(v=>v.value)
onMounted(loadHistory)
</script>

<template>
 <div class="page">
  <div class="page-head"><div><h1 class="page-title">高级组合检索</h1><div class="page-subtitle">材料、工艺、批次、设备、产品型号与时间范围按 AND 组合，结果严格受模块权限和数据域约束</div></div></div>
  <div class="surface search-panel">
   <section v-if="histories.length" class="history"><div class="condition-title"><label>最近检索</label><el-button link type="danger" @click="clearHistory">清空</el-button></div><div><el-button v-for="item in histories" :key="`${item.searchedAt}-${historyLabel(item)}`" plain round @click="useHistory(item)">{{historyLabel(item)}}</el-button></div></section>
   <section><label>资源范围</label><el-checkbox-group v-model="resourceTypes"><el-checkbox-button v-for="item in resourceOptions" :key="item.value" :value="item.value">{{item.label}}</el-checkbox-button></el-checkbox-group></section>
   <section><div class="condition-title"><label>检索条件</label><el-button link type="primary" @click="addCondition">＋ 添加条件</el-button></div><div v-for="(condition,index) in conditions" :key="condition.id" class="condition-row"><el-select v-model="condition.field"><el-option v-for="field in fields" :key="field[0]" :label="field[1]" :value="field[0]"/></el-select><el-select v-model="condition.matchMode"><el-option label="模糊匹配" value="FUZZY"/><el-option label="精准匹配" value="EXACT"/></el-select><el-autocomplete v-model="condition.value" :fetch-suggestions="fetchSuggestions" value-key="value" placeholder="输入检索值；支持中文分词与热门建议" clearable @keyup.enter="search(true)"/><el-button type="danger" link @click="removeCondition(index)">删除</el-button></div></section>
   <section class="options"><div><label>时间范围</label><el-date-picker v-model="timeRange" type="datetimerange" start-placeholder="开始时间（含）" end-placeholder="结束时间（不含）"/></div><div><label>排序</label><div class="sorts"><el-select v-model="sortBy"><el-option label="创建时间" value="CREATED_TIME"/><el-option label="更新时间" value="UPDATED_TIME"/><el-option label="名称" value="NAME"/><el-option label="类型" value="TYPE"/><el-option label="状态" value="STATUS"/></el-select><el-select v-model="sortOrder"><el-option label="倒序" value="DESC"/><el-option label="正序" value="ASC"/></el-select></div></div></section>
   <footer><el-button @click="reset">重置</el-button><el-button type="primary" :loading="loading" @click="search(true)">执行组合检索</el-button></footer>
  </div>
  <div v-if="searched" class="surface result-panel"><div class="result-head"><div><b>检索结果</b><span>共 {{total}} 条；同值结果按资源类型和编号稳定排序</span></div></div><el-table :data="rows" v-loading="loading" @row-dblclick="open"><el-table-column label="资源" width="105"><template #default="{row}"><el-tag effect="plain">{{resourceLabels[row.resourceType]}}</el-tag></template></el-table-column><el-table-column label="名称 / 编码" min-width="230"><template #default="{row}"><b><template v-for="(part,index) in highlightParts(row.title)" :key="index"><mark v-if="part.hit">{{part.text}}</mark><template v-else>{{part.text}}</template></template></b><small class="code"><template v-for="(part,index) in highlightParts(row.code)" :key="index"><mark v-if="part.hit">{{part.text}}</mark><template v-else>{{part.text}}</template></template></small></template></el-table-column><el-table-column label="内容摘要" min-width="300"><template #default="{row}"><span v-if="row.highlightHtml" class="summary" v-html="row.highlightHtml"></span><span v-else class="summary"><template v-for="(part,index) in highlightParts(summary(row))" :key="index"><mark v-if="part.hit">{{part.text}}</mark><template v-else>{{part.text}}</template></template></span></template></el-table-column><el-table-column prop="category" label="分类/类型" width="120"/><el-table-column prop="status" label="状态" width="110"/><el-table-column prop="createdTime" label="业务时间" width="190"/><el-table-column label="操作" width="90"><template #default="{row}"><el-button link type="primary" @click="open(row)">打开</el-button></template></el-table-column></el-table><el-empty v-if="!loading&&!rows.length" description="没有符合全部条件且当前账号有权查看的结果"/><el-pagination v-model:current-page="pageNum" v-model:page-size="pageSize" :page-sizes="[10,20,50]" :total="total" layout="total, sizes, prev, pager, next" @current-change="search(false)" @size-change="search(true)"/></div>
 </div>
</template>

<style scoped>
.search-panel{padding:20px;display:grid;gap:22px}.search-panel label{display:block;font-size:12px;font-weight:700;color:#44566d;margin-bottom:9px}.condition-title{display:flex;align-items:center;justify-content:space-between}.condition-title label{margin:0}.history>div:last-child{display:flex;gap:7px;overflow:auto;padding-top:9px}.history .el-button{max-width:230px;overflow:hidden;text-overflow:ellipsis}.condition-row{display:grid;grid-template-columns:170px 130px 1fr 52px;gap:10px;margin-top:9px}.options{display:grid;grid-template-columns:1.5fr 1fr;gap:18px}.options>div>.el-date-editor{width:100%}.sorts{display:grid;grid-template-columns:1fr 100px;gap:9px}.search-panel footer{display:flex;justify-content:flex-end;border-top:1px solid #edf1f5;padding-top:16px}.result-panel{margin-top:15px}.result-head{padding:17px 18px;border-bottom:1px solid #edf1f5}.result-head div{display:flex;align-items:center;gap:12px}.result-head span,.code{display:block;color:#8a98a9;font-size:10px;margin-top:4px}.summary{color:#66778b;font-size:11px;line-height:1.5}mark{background:#fff0a8;color:inherit;border-radius:2px;padding:0 1px}.el-pagination{justify-content:flex-end;padding:16px}
@media(max-width:900px){.options{grid-template-columns:1fr}.condition-row{grid-template-columns:1fr 130px}.condition-row>.el-input{grid-column:1/3}}
@media(max-width:600px){.condition-row,.options{grid-template-columns:1fr}.condition-row>.el-input{grid-column:auto}.sorts{grid-template-columns:1fr}.el-checkbox-group{display:flex;flex-wrap:wrap}}
</style>
