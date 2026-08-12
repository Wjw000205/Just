import { defineStore } from 'pinia'
import { ref,computed } from 'vue'
import { http,dataOf,setAccessToken,hasAccessToken } from '../api/http'
export interface UserInfo{id:number;username:string;realName:string;roles:string[];permissions:string[];assignedScopes?:number[];dataScopes:number[];mustChangePassword?:boolean}
export const useAuthStore=defineStore('auth',()=>{
  let initial:UserInfo|null=null;try{initial=JSON.parse(localStorage.getItem('rdp_user')||'null')}catch{localStorage.removeItem('rdp_user')}
  const user=ref<UserInfo|null>(initial)
  const authenticated=computed(()=>hasAccessToken()&&!!user.value)
  const can=(permission:string)=>user.value?.permissions.includes(permission)||false
  async function login(body:any){const result=dataOf<any>(await http.post('/auth/login',body));setAccessToken(result.accessToken);user.value=result.userInfo;localStorage.setItem('rdp_user',JSON.stringify(user.value))}
  async function ensureSession(){if(authenticated.value)return true;try{const result=dataOf<any>(await http.post('/auth/refresh'));setAccessToken(result.accessToken);user.value=result.userInfo;localStorage.setItem('rdp_user',JSON.stringify(user.value));return true}catch{setAccessToken('');user.value=null;localStorage.removeItem('rdp_user');return false}}
  async function logout(){try{await http.post('/auth/logout')}finally{setAccessToken('');user.value=null;localStorage.removeItem('rdp_user')}}
  function updateProfile(realName:string){if(!user.value)return;user.value={...user.value,realName};localStorage.setItem('rdp_user',JSON.stringify(user.value))}
  function clearSession(){setAccessToken('');user.value=null;localStorage.removeItem('rdp_user')}
  return{user,authenticated,can,login,ensureSession,logout,updateProfile,clearSession}
})
