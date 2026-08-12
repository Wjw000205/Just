import {ElMessageBox} from 'element-plus'
import {dataOf,http} from './http'

export type StepUpPurpose='DELETE'|'EXPORT'|'PRIVILEGE'

export async function withStepUp<T>(options:{purpose:StepUpPurpose;method:'GET'|'POST'|'PUT'|'PATCH'|'DELETE';target:string;label:string;payload?:unknown},action:(token:string)=>Promise<T>):Promise<T>{
 const {value}=await ElMessageBox.prompt(`敏感操作：${options.label}\n请输入6位二级密码。确认凭证仅绑定本次请求，5分钟内可使用一次。`,'二级安全确认',{confirmButtonText:'验证并继续',cancelButtonText:'取消',inputType:'password',inputPattern:/^\d{6}$/,inputErrorMessage:'请输入6位数字二级密码',closeOnClickModal:false,closeOnPressEscape:true,type:'warning'})
 const target=options.target.startsWith('/api/')?options.target:`/api${options.target.startsWith('/')?'':'/'}${options.target}`
 const result=dataOf<{token:string}>(await http.post('/user/step-up',{purpose:options.purpose,method:options.method,target,secondaryPassword:value,payload:options.payload},{_skipAuthRefresh:true} as any))
 return action(result.token)
}

export const stepUpHeader=(token:string)=>({'X-Step-Up-Token':token})
