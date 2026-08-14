import {createRouter,createWebHistory,type RouteRecordRaw} from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import {useAuthStore} from '../stores/auth'

const prototype=(path:string,key:string,title:string,group:string):RouteRecordRaw=>({
  path,component:()=>import('../views/PrototypeModuleView.vue'),meta:{title,group,prototype:key}
})
const children:RouteRecordRaw[]=[
  {path:'',redirect:'/dashboard'},
  {path:'dashboard',component:()=>import('../views/DashboardView.vue'),meta:{title:'数据驾驶舱',group:'01 数据驾驶舱',permission:'dashboard:read'}},
  {path:'search',component:()=>import('../views/SearchView.vue'),meta:{title:'高级检索'}},

  prototype('assets/overview','assets-overview','资产总览','02 数据资产中心'),
  prototype('assets/materials','materials','材料库','02 数据资产中心'),
  prototype('assets/processes','processes','工艺库','02 数据资产中心'),
  prototype('assets/products','products','产品库','02 数据资产中心'),
  prototype('assets/performance','performance','性能数据库','02 数据资产中心'),
  {path:'assets/devices',component:()=>import('../views/DevicesView.vue'),meta:{title:'设备数据库',group:'02 数据资产中心',permission:'device:read'}},
  {path:'assets/files',component:()=>import('../views/FilesView.vue'),meta:{title:'文件资料库',group:'02 数据资产中心',permission:'file:read'}},
  {path:'assets/datasets',component:()=>import('../views/DatasetsView.vue'),meta:{title:'通用数据集',group:'02 数据资产中心',permission:'dataset:read'}},

  {path:'trace/search',component:()=>import('../views/TraceView.vue'),meta:{title:'追溯搜索',group:'03 全链路追溯',permission:'trace:read'}},
  prototype('trace/history','trace-history','追溯记录','03 全链路追溯'),

  prototype('research/projects','rnd-projects','研发项目','04 研发数据中心'),
  prototype('research/experiments','experiments','实验管理','04 研发数据中心'),
  prototype('research/process-experiments','process-experiments','工艺实验','04 研发数据中心'),
  prototype('research/simulations','simulations','仿真管理','04 研发数据中心'),

  prototype('production/overview','production-overview','生产总览','05 生产数据中心'),
  prototype('production/work-orders','work-orders','工单','05 生产数据中心'),
  prototype('production/operations','operations','工序','05 生产数据中心'),
  prototype('production/batches','production-batches','生产批次','05 生产数据中心'),
  {path:'production/devices',component:()=>import('../views/DevicesView.vue'),meta:{title:'设备运行',group:'05 生产数据中心',permission:'device:read'}},
  prototype('production/realtime','realtime','实时数据','05 生产数据中心'),

  prototype('quality/inspections','inspections','检验记录','06 质量数据中心'),
  prototype('quality/ct-metallography','ct-metallography','CT / 金相','06 质量数据中心'),
  prototype('quality/hardness-thickness','hardness-thickness','硬度 / 厚度','06 质量数据中心'),
  prototype('quality/fatigue','fatigue','疲劳试验','06 质量数据中心'),
  prototype('quality/defects','defects','缺陷分析','06 质量数据中心'),

  prototype('analytics/process','analysis-process','工艺对比','07 数据分析'),
  prototype('analytics/performance','analysis-performance','性能对比','07 数据分析'),
  prototype('analytics/batches','analysis-batch','批次对比','07 数据分析'),
  prototype('analytics/correlation','correlation','参数相关性','07 数据分析'),
  prototype('analytics/trends','trend-analysis','趋势分析','07 数据分析'),

  prototype('integration/overview','integration-overview','集成总览','08 数据集成中心'),
  {path:'integration/systems',component:()=>import('../views/IntegrationsView.vue'),meta:{title:'外部系统',group:'08 数据集成中心',anyPermissions:['integration:read','integration:manage']}},
  {path:'integration/mappings',component:()=>import('../views/IntegrationsView.vue'),meta:{title:'API 与字段映射',group:'08 数据集成中心',anyPermissions:['integration:read','integration:manage']}},
  prototype('integration/tasks','sync-tasks','同步任务','08 数据集成中心'),
  prototype('integration/logs','sync-logs','同步日志','08 数据集成中心'),
  prototype('integration/exceptions','integration-exceptions','异常队列','08 数据集成中心'),

  prototype('governance/master-data','master-data','主数据','09 数据治理'),
  prototype('governance/metadata','metadata','元数据','09 数据治理'),
  {path:'governance/taxonomy',component:()=>import('../views/GovernanceView.vue'),meta:{title:'分类与标签',group:'09 数据治理',permission:'governance:read'}},
  {path:'governance/dictionaries',component:()=>import('../views/GovernanceView.vue'),meta:{title:'数据字典',group:'09 数据治理',permission:'governance:read'}},
  prototype('governance/units','units','单位管理','09 数据治理'),
  prototype('governance/data-quality','data-quality','数据质量','09 数据治理'),
  {path:'governance/templates',component:()=>import('../views/TemplatesView.vue'),meta:{title:'模板中心',group:'09 数据治理',permission:'template:read'}},
  {path:'governance/lifecycle',component:()=>import('../views/LifecycleView.vue'),meta:{title:'生命周期',group:'09 数据治理',permission:'lifecycle:read'}},

  {path:'system/users',component:()=>import('../views/AdminView.vue'),meta:{title:'用户与组织',group:'10 系统管理',permission:'user:manage'}},
  {path:'system/roles',component:()=>import('../views/GovernanceView.vue'),meta:{title:'角色权限',group:'10 系统管理',permission:'governance:read'}},
  {path:'system/scopes',component:()=>import('../views/GovernanceView.vue'),meta:{title:'数据域',group:'10 系统管理',permission:'governance:read'}},
  {path:'system/menus',component:()=>import('../views/GovernanceView.vue'),meta:{title:'菜单权限',group:'10 系统管理',permission:'governance:read'}},
  {path:'system/sharing',component:()=>import('../views/AdminView.vue'),meta:{title:'共享管理',group:'10 系统管理',permission:'share:read'}},
  {path:'system/audits',component:()=>import('../views/AuditsView.vue'),meta:{title:'审计日志',group:'10 系统管理',permission:'audit:read'}},
  prototype('system/logs','system-logs','系统日志','10 系统管理'),
  {path:'system/settings',component:()=>import('../views/GovernanceView.vue'),meta:{title:'平台配置',group:'10 系统管理',permission:'governance:read'}},

  {path:'templates',component:()=>import('../views/TemplatesView.vue'),meta:{title:'模板中心',permission:'template:read'}},
  {path:'datasets',component:()=>import('../views/DatasetsView.vue'),meta:{title:'数据集',permission:'dataset:read'}},
  {path:'datasets/:id',component:()=>import('../views/DatasetRecordsView.vue'),meta:{title:'数据记录',permission:'dataset:read'}},
  {path:'trace',component:()=>import('../views/TraceView.vue'),meta:{title:'全链路追溯',permission:'trace:read'}},
  {path:'devices',component:()=>import('../views/DevicesView.vue'),meta:{title:'设备与采集',permission:'device:read'}},
  {path:'files',component:()=>import('../views/FilesView.vue'),meta:{title:'附件中心',permission:'file:read'}},
  {path:'lifecycle',component:()=>import('../views/LifecycleView.vue'),meta:{title:'数据生命周期',permission:'lifecycle:read'}},
  {path:'integrations',component:()=>import('../views/IntegrationsView.vue'),meta:{title:'系统集成',anyPermissions:['integration:read','integration:manage']}},
  {path:'audits',component:()=>import('../views/AuditsView.vue'),meta:{title:'审计中心',permission:'audit:read'}},
  {path:'admin',component:()=>import('../views/AdminView.vue'),meta:{title:'系统与共享管理',anyPermissions:['user:manage','share:read']}},
  {path:'governance',component:()=>import('../views/GovernanceView.vue'),meta:{title:'治理配置',permission:'governance:read'}},
  {path:'profile',component:()=>import('../views/ProfileView.vue'),meta:{title:'个人中心'}}
]

const router=createRouter({history:createWebHistory(),routes:[
  {path:'/login',component:()=>import('../views/LoginView.vue'),meta:{public:true}},
  {path:'/register',component:()=>import('../views/RegisterView.vue'),meta:{public:true}},
  {path:'/change-password',component:()=>import('../views/ChangePasswordView.vue')},
  {path:'/',component:MainLayout,children}
]})

function landingPath(auth:ReturnType<typeof useAuthStore>){
  const candidates=[
    ['dashboard:read','/dashboard'],['dataset:read','/assets/overview'],['trace:read','/trace/search'],
    ['device:read','/assets/devices'],['integration:read','/integration/overview'],['integration:manage','/integration/overview'],
    ['governance:read','/governance/master-data'],['user:manage','/system/users'],['share:read','/system/sharing']
  ]
  return candidates.find(([permission])=>auth.can(permission))?.[1]||'/research/projects'
}
router.beforeEach(async to=>{
  const auth=useAuthStore()
  if(!to.meta.public&&!await auth.ensureSession())return'/login'
  if(auth.user?.mustChangePassword&&to.path!='/change-password')return'/change-password'
  if((to.path==='/login'||to.path==='/register')&&auth.authenticated)return auth.user?.mustChangePassword?'/change-password':landingPath(auth)
  if(to.path==='/'&&auth.authenticated)return landingPath(auth)
  if(to.meta.permission&&!auth.can(String(to.meta.permission)))return landingPath(auth)
  if(Array.isArray(to.meta.anyPermissions)&&!to.meta.anyPermissions.some(permission=>auth.can(String(permission))))return landingPath(auth)
})
export default router
