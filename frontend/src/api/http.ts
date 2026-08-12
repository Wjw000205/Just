import axios from 'axios'
import { ElMessage } from 'element-plus'
let accessToken='';let refreshFlight:Promise<string>|null=null
export const setAccessToken=(value:string)=>{accessToken=value};export const hasAccessToken=()=>!!accessToken
export const http=axios.create({baseURL:'/api',timeout:20000,withCredentials:true})
http.interceptors.request.use(config=>{if(accessToken)config.headers.Authorization=`Bearer ${accessToken}`;return config})
http.interceptors.response.use(response=>response.data,async error=>{
 const original=error.config
 if(error.response?.status===401&&!original?._retried&&!original?._skipAuthRefresh&&!String(original?.url).includes('/auth/')){
  original._retried=true
  try{refreshFlight??=axios.post('/api/auth/refresh',{}, {withCredentials:true}).then(r=>{const token=r.data.data.accessToken;setAccessToken(token);return token}).finally(()=>refreshFlight=null);await refreshFlight;return http(original)}catch{setAccessToken('');localStorage.removeItem('rdp_user');if(location.pathname!='/login')location.href='/login';return Promise.reject(error)}
 }
 const message=error.response?.data?.message||(
  import.meta.env.DEV
   ? `网络连接异常（${error.code||'UNKNOWN'}：${error.message||'请求失败'}，${original?.baseURL||''}${original?.url||''}）`
   : '网络连接异常'
 );ElMessage.error(message);return Promise.reject(error)
})
export const dataOf=<T>(response:any):T=>response.data as T
