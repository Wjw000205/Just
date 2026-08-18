import { defineStore } from 'pinia'
import { ref,computed } from 'vue'
import { ElMessage } from 'element-plus'
import { AccountSessionMismatchError,http,dataOf,setAccessToken,hasAccessToken,setSessionUserId,validateRefreshIdentity } from '../api/http'
export interface UserInfo{id:number;username:string;realName:string;roles:string[];permissions:string[];assignedScopes?:number[];dataScopes:number[];mustChangePassword?:boolean}
export const useAuthStore=defineStore('auth',()=>{
  const storageKey='rdp_user'
  // 旧版 localStorage 在同域标签页间共享，会造成账号资料串用；升级后只保留标签页级状态。
  localStorage.removeItem(storageKey)
  let initial:UserInfo|null=null
  try{initial=JSON.parse(sessionStorage.getItem(storageKey)||'null')}catch{sessionStorage.removeItem(storageKey)}
  setSessionUserId(initial?.id??null)
  const user=ref<UserInfo|null>(initial)
  const authenticated=computed(()=>hasAccessToken()&&!!user.value)
  const can=(permission:string)=>user.value?.permissions.includes(permission)||false
  function persistUser(value:UserInfo|null){
   user.value=value
   setSessionUserId(value?.id??null)
   if(value)sessionStorage.setItem(storageKey,JSON.stringify(value));else sessionStorage.removeItem(storageKey)
  }
  async function login(body:any){
   const result=dataOf<any>(await http.post('/auth/login',body))
   setAccessToken(result.accessToken)
   persistUser(result.userInfo)
  }
  async function ensureSession(){
   if(authenticated.value)return true
   try{
    const result=validateRefreshIdentity(dataOf<any>(await http.post('/auth/refresh')))
    setAccessToken(result.accessToken)
    persistUser(result.userInfo)
    return true
   }catch(error){
    setAccessToken('')
    persistUser(null)
    if(error instanceof AccountSessionMismatchError){
     ElMessage.error('检测到另一个账号的刷新会话，为防止账号串用，本标签页需要重新登录')
    }
    return false
   }
  }
  async function logout(){try{await http.post('/auth/logout')}finally{setAccessToken('');persistUser(null)}}
  function updateProfile(realName:string){if(!user.value)return;persistUser({...user.value,realName})}
  function clearSession(){setAccessToken('');persistUser(null)}
  return{user,authenticated,can,login,ensureSession,logout,updateProfile,clearSession}
})
