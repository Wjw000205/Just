<script setup lang="ts">
import {ref,computed,onMounted,onUnmounted} from 'vue';import {useRoute,useRouter} from 'vue-router';import {useAuthStore} from '../stores/auth';import {http,dataOf} from '../api/http'
const route=useRoute(),router=useRouter(),auth=useAuthStore(),collapsed=ref(false),search=ref(''),results=ref<any|null>(null),searching=ref(false),healthy=ref(false),configuredMenus=ref<any[]>([]),navigationLoaded=ref(false);let healthTimer=0
const searchGroups=computed(()=>[{key:'templates',label:'模板',show:auth.can('template:read')},{key:'datasets',label:'数据集',show:auth.can('dataset:read')},{key:'entities',label:'追溯实体',show:auth.can('trace:read')}].filter(group=>group.show))
const fallbackMenus=computed(()=>[
 {path:'/dashboard',label:'工作台',mark:'◫',show:auth.can('dashboard:read')},
 {path:'/search',label:'高级检索',mark:'⌕',show:true},
 {path:'/templates',label:'模板中心',mark:'◇',show:auth.can('template:read')},
 {path:'/datasets',label:'数据资产',mark:'▦',show:auth.can('dataset:read')},
 {path:'/trace',label:'全链路追溯',mark:'⌘',show:auth.can('trace:read')},
 {path:'/devices',label:'设备与采集',mark:'⌁',show:auth.can('device:read')},
 {path:'/files',label:'附件中心',mark:'▧',show:auth.can('file:read')},
 {path:'/lifecycle',label:'数据生命周期',mark:'◷',show:auth.can('lifecycle:read')},
 {path:'/integrations',label:'系统集成',mark:'⇄',show:auth.can('integration:read')||auth.can('integration:manage')},
 {path:'/audits',label:'审计中心',mark:'◎',show:auth.can('audit:read')},
 {path:'/admin',label:'系统与共享',mark:'⚙',show:auth.can('user:manage')||auth.can('share:read')},
 {path:'/governance',label:'治理配置',mark:'⚒',show:auth.can('governance:read')}
].filter(i=>i.show))
const menus=computed(()=>navigationLoaded.value?configuredMenus.value.filter(item=>item.route).map(item=>({path:item.route,label:item.name,mark:item.icon||'•',show:true})):fallbackMenus.value)
async function globalSearch(){if(search.value.trim().length<2)return;searching.value=true;try{results.value=dataOf(await http.get('/dashboard/search',{params:{keyword:search.value}}))}finally{searching.value=false}}
function openResult(group:string,id:number){results.value=null;search.value='';if(group==='templates')router.push({path:'/templates',query:{focus:String(id)}});else if(group==='datasets')router.push(`/datasets/${id}`);else router.push({path:'/trace',query:{root:String(id)}})}
async function logout(){await auth.logout();router.push('/login')}
async function checkHealth(){try{const response=await fetch('/actuator/health');healthy.value=response.ok&&(await response.json()).status==='UP'}catch{healthy.value=false}}
async function loadNavigation(){try{configuredMenus.value=dataOf<any[]>(await http.get('/governance/navigation'));navigationLoaded.value=true;const config=dataOf<any>(await http.get('/governance/ui-config'));const root=document.documentElement;root.style.setProperty('--rdp-primary',config.primaryColor);root.style.setProperty('--rdp-font-size',`${config.fontSize}px`);root.style.setProperty('--rdp-radius',`${config.borderRadius}px`);root.style.setProperty('--rdp-spacing',`${config.contentSpacing}px`)}catch{configuredMenus.value=[];navigationLoaded.value=false}}
onMounted(()=>{loadNavigation();checkHealth();healthTimer=window.setInterval(checkHealth,30000)});onUnmounted(()=>clearInterval(healthTimer))
</script>
<template><div class="shell" :class="{collapsed}">
 <aside class="sidebar"><div class="brand"><div class="brand-glyph">J</div><div v-if="!collapsed"><strong>嘉思特数据平台</strong><small>R&D · PRODUCTION</small></div></div>
  <nav><router-link v-for="item in menus" :key="item.path" :to="item.path"><span class="menu-mark">{{item.mark}}</span><span v-if="!collapsed">{{item.label}}</span></router-link></nav>
  <button class="collapse" @click="collapsed=!collapsed">{{collapsed?'›':'‹ 收起导航'}}</button>
 </aside>
 <main><header class="topbar"><div class="crumb"><span>研发与生产大数据平台</span><b>/</b><strong>{{route.meta.title}}</strong></div>
  <div class="top-actions"><el-popover :visible="!!results" placement="bottom" :width="430"><template #reference><el-input v-model="search" class="global-search" placeholder="搜索模板、数据集、批次…" clearable @keyup.enter="globalSearch" @clear="results=null"><template #prefix>⌕</template></el-input></template>
   <div class="search-results" v-loading="searching"><template v-if="results"><div v-for="group in searchGroups" :key="group.key"><h4>{{group.label}}</h4><button v-for="r in results[group.key]||[]" :key="r.id" class="result-row" @click="openResult(group.key,r.id)"><b>{{r.name}}</b><small>{{r.secondary}}</small></button></div><button class="close-results" @click="results=null">关闭</button></template></div>
  </el-popover><span class="health" :class="{down:!healthy}"><i></i> {{healthy?'服务正常':'服务异常'}}</span><el-dropdown><div class="avatar">{{auth.user?.realName.slice(0,1)}}</div><template #dropdown><el-dropdown-menu><el-dropdown-item disabled>{{auth.user?.realName}} · {{auth.user?.roles.join(',')}}</el-dropdown-item><el-dropdown-item divided @click="router.push('/profile')">个人中心</el-dropdown-item><el-dropdown-item @click="logout">安全退出</el-dropdown-item></el-dropdown-menu></template></el-dropdown></div>
 </header><router-view/></main>
</div></template>
<style scoped>
.shell{display:grid;grid-template-columns:238px 1fr;min-height:100vh;transition:.2s}.shell.collapsed{grid-template-columns:72px 1fr}.sidebar{position:sticky;top:0;height:100vh;background:linear-gradient(180deg,#102f51,#0b233d);color:#fff;padding:0 14px;display:flex;flex-direction:column;overflow:hidden}.brand{height:76px;display:flex;align-items:center;gap:11px;padding:0 8px;border-bottom:1px solid rgba(255,255,255,.09);white-space:nowrap}.brand-glyph{width:35px;height:35px;border-radius:10px;background:linear-gradient(135deg,#36a2d7,#7ed8ce);display:grid;place-items:center;font-weight:900;font-size:20px}.brand strong{display:block;font-size:14px;letter-spacing:.04em}.brand small{font-size:9px;letter-spacing:.18em;color:#87a9c6}.sidebar nav{padding-top:18px;flex:1}.sidebar a{height:46px;padding:0 13px;margin:4px 0;color:#aac1d7;text-decoration:none;display:flex;align-items:center;gap:13px;border-radius:9px;font-size:14px;white-space:nowrap}.sidebar a:hover{background:rgba(255,255,255,.06);color:#fff}.sidebar a.router-link-active{background:linear-gradient(90deg,rgba(55,146,198,.34),rgba(55,146,198,.12));color:#fff;box-shadow:inset 3px 0 #51b4d7}.menu-mark{font-size:20px;width:20px;text-align:center}.collapse{height:46px;margin:12px 2px 20px;border:0;border-top:1px solid rgba(255,255,255,.08);background:none;color:#7f9bb5;cursor:pointer}.topbar{height:76px;background:#fff;border-bottom:1px solid #e5ebf1;display:flex;align-items:center;justify-content:space-between;padding:0 30px;position:sticky;top:0;z-index:10}.crumb{font-size:13px;color:#7d8a9d;display:flex;gap:9px}.crumb strong{color:#2c405a}.top-actions{display:flex;align-items:center;gap:20px}.global-search{width:290px}.health{font-size:12px;color:#5b6d82}.health i{display:inline-block;width:7px;height:7px;border-radius:50%;background:#22b573;margin-right:6px;box-shadow:0 0 0 4px #e3f7ef}.avatar{width:35px;height:35px;border-radius:10px;background:#dceaf6;color:#174f7d;display:grid;place-items:center;font-weight:700;cursor:pointer}.search-results h4{font-size:12px;color:#7d8a9d;margin:10px 0 5px}.result-row{display:block;width:100%;padding:8px;border:0;background:none;border-radius:7px;text-align:left;cursor:pointer;color:inherit}.result-row:hover{background:#f3f6fa}.result-row b,.result-row small{display:block}.result-row small{color:#8a97a7;margin-top:2px}.close-results{border:0;background:none;color:#26689f;cursor:pointer;float:right}
.health.down i{background:#d34f4f;box-shadow:0 0 0 4px #fae8e8}
main{min-width:0}
@media(max-width:900px){.shell,.shell.collapsed{grid-template-columns:72px minmax(0,1fr)}.sidebar{padding:0 8px}.brand{padding:0 10px}.brand>div:not(.brand-glyph),.sidebar a span:not(.menu-mark){display:none}.sidebar a{justify-content:center;padding:0}.collapse{font-size:0}.collapse:after{content:'›';font-size:22px}.topbar{height:64px;padding:0 14px}.crumb>span,.crumb>b,.health{display:none}.top-actions{gap:10px}.global-search{width:min(40vw,220px)}}
@media(max-width:560px){.global-search{display:none}.topbar{justify-content:flex-end}.crumb{margin-right:auto}.sidebar{position:fixed;left:0;z-index:20;width:72px}.shell,.shell.collapsed{display:block;padding-left:72px}}
</style>
