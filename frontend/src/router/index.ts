import {createRouter,createWebHistory} from 'vue-router'
import MainLayout from '../layouts/MainLayout.vue'
import {useAuthStore} from '../stores/auth'
const router=createRouter({history:createWebHistory(),routes:[
  {path:'/login',component:()=>import('../views/LoginView.vue'),meta:{public:true}},
  {path:'/register',component:()=>import('../views/RegisterView.vue'),meta:{public:true}},
  {path:'/change-password',component:()=>import('../views/ChangePasswordView.vue')},
  {path:'/',component:MainLayout,children:[
    {path:'',redirect:'/dashboard'},
    {path:'dashboard',component:()=>import('../views/DashboardView.vue'),meta:{title:'工作台',permission:'dashboard:read'}},
    {path:'search',component:()=>import('../views/SearchView.vue'),meta:{title:'高级检索'}},
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
  ]}
]})
function landingPath(auth:ReturnType<typeof useAuthStore>){
  const candidates=[
    ['dashboard:read','/dashboard'],['template:read','/templates'],['dataset:read','/datasets'],
    ['trace:read','/trace'],['device:read','/devices'],['file:read','/files'],['lifecycle:read','/lifecycle'],
    ['integration:read','/integrations'],['integration:manage','/integrations'],['audit:read','/audits'],['governance:read','/governance'],['user:manage','/admin'],['share:read','/admin']
  ]
  return candidates.find(([permission])=>auth.can(permission))?.[1]||'/profile'
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
