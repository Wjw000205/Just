import axios from 'axios'
import { ElMessage } from 'element-plus'

const SESSION_USER_KEY='rdp_user'
let accessToken=''
let sessionUserId:number|null=null
let refreshFlight:Promise<string>|null=null

export class AccountSessionMismatchError extends Error{
 constructor(readonly expectedUserId:number,readonly refreshedUserId:number){
  super('当前标签页的账号与刷新会话不一致')
  this.name='AccountSessionMismatchError'
 }
}

export const setAccessToken=(value:string)=>{accessToken=value}
export const hasAccessToken=()=>!!accessToken
export const setSessionUserId=(value:number|null)=>{sessionUserId=value==null?null:Number(value)}
export function validateRefreshIdentity<T extends {userInfo?:{id?:number}}>(result:T):T{
 const refreshedUserId=Number(result?.userInfo?.id)
 if(!Number.isSafeInteger(refreshedUserId)||refreshedUserId<=0)throw new Error('刷新响应缺少有效用户身份')
 if(sessionUserId!=null&&refreshedUserId!==sessionUserId){
  throw new AccountSessionMismatchError(sessionUserId,refreshedUserId)
 }
 sessionUserId=refreshedUserId
 return result
}
function clearTabSession(){
 setAccessToken('')
 setSessionUserId(null)
 sessionStorage.removeItem(SESSION_USER_KEY)
}

export const http=axios.create({baseURL:'/api',timeout:20000,withCredentials:true})
http.interceptors.request.use(config=>{if(accessToken)config.headers.Authorization=`Bearer ${accessToken}`;return config})
http.interceptors.response.use(response=>response.data,async error=>{
 const original=error.config
 const responseMessage=String(error.response?.data?.message||'')
 if(error.response?.status===403&&responseMessage.includes('首次登录必须修改引导密码')){
  ElMessage.warning('首次登录需要先设置新密码')
  if(location.pathname!='/change-password')location.replace('/change-password')
  return Promise.reject(error)
 }
 if(error.response?.status===401&&!original?._retried&&!original?._skipAuthRefresh&&!String(original?.url).includes('/auth/')){
  original._retried=true
  try{
   refreshFlight??=axios.post('/api/auth/refresh',{}, {withCredentials:true}).then(r=>{
    const result=validateRefreshIdentity(r.data.data)
    setAccessToken(result.accessToken)
    return result.accessToken
   }).finally(()=>refreshFlight=null)
   await refreshFlight
   return http(original)
  }catch(refreshError){
   clearTabSession()
   if(refreshError instanceof AccountSessionMismatchError){
    ElMessage.error('检测到另一个账号的刷新会话，本标签页已停止自动刷新，请重新登录当前账号')
   }
   if(location.pathname!='/login')location.href='/login'
   return Promise.reject(refreshError)
  }
 }
 const message=responseMessage||(
  import.meta.env.DEV
   ? `网络连接异常（${error.code||'UNKNOWN'}：${error.message||'请求失败'}，${original?.baseURL||''}${original?.url||''}）`
   : '网络连接异常'
 );ElMessage.error(message);return Promise.reject(error)
})
export const dataOf=<T>(response:any):T=>response.data as T
