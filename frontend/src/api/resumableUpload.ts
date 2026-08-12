import {dataOf,http} from './http'

const LARGE_FILE=8*1024*1024

class Sha256 {
  private h=new Uint32Array([0x6a09e667,0xbb67ae85,0x3c6ef372,0xa54ff53a,0x510e527f,0x9b05688c,0x1f83d9ab,0x5be0cd19]);private buffer=new Uint8Array(0);private bytes=0
  private static k=new Uint32Array([0x428a2f98,0x71374491,0xb5c0fbcf,0xe9b5dba5,0x3956c25b,0x59f111f1,0x923f82a4,0xab1c5ed5,0xd807aa98,0x12835b01,0x243185be,0x550c7dc3,0x72be5d74,0x80deb1fe,0x9bdc06a7,0xc19bf174,0xe49b69c1,0xefbe4786,0x0fc19dc6,0x240ca1cc,0x2de92c6f,0x4a7484aa,0x5cb0a9dc,0x76f988da,0x983e5152,0xa831c66d,0xb00327c8,0xbf597fc7,0xc6e00bf3,0xd5a79147,0x06ca6351,0x14292967,0x27b70a85,0x2e1b2138,0x4d2c6dfc,0x53380d13,0x650a7354,0x766a0abb,0x81c2c92e,0x92722c85,0xa2bfe8a1,0xa81a664b,0xc24b8b70,0xc76c51a3,0xd192e819,0xd6990624,0xf40e3585,0x106aa070,0x19a4c116,0x1e376c08,0x2748774c,0x34b0bcb5,0x391c0cb3,0x4ed8aa4a,0x5b9cca4f,0x682e6ff3,0x748f82ee,0x78a5636f,0x84c87814,0x8cc70208,0x90befffa,0xa4506ceb,0xbef9a3f7,0xc67178f2])
  update(part:Uint8Array){this.bytes+=part.length;const data=new Uint8Array(this.buffer.length+part.length);data.set(this.buffer);data.set(part,this.buffer.length);let offset=0;while(offset+64<=data.length){this.block(data.subarray(offset,offset+64));offset+=64}this.buffer=data.slice(offset)}
  private block(b:Uint8Array){const w=new Uint32Array(64);for(let i=0;i<16;i++)w[i]=(b[i*4]<<24)|(b[i*4+1]<<16)|(b[i*4+2]<<8)|b[i*4+3];for(let i=16;i<64;i++){const x=w[i-15],y=w[i-2],s0=((x>>>7)|(x<<25))^((x>>>18)|(x<<14))^(x>>>3),s1=((y>>>17)|(y<<15))^((y>>>19)|(y<<13))^(y>>>10);w[i]=(w[i-16]+s0+w[i-7]+s1)>>>0}let[a,bv,c,d,e,f,g,h]=this.h;for(let i=0;i<64;i++){const s1=((e>>>6)|(e<<26))^((e>>>11)|(e<<21))^((e>>>25)|(e<<7)),ch=(e&f)^(~e&g),t1=(h+s1+ch+Sha256.k[i]+w[i])>>>0,s0=((a>>>2)|(a<<30))^((a>>>13)|(a<<19))^((a>>>22)|(a<<10)),maj=(a&bv)^(a&c)^(bv&c),t2=(s0+maj)>>>0;h=g;g=f;f=e;e=(d+t1)>>>0;d=c;c=bv;bv=a;a=(t1+t2)>>>0}const v=[a,bv,c,d,e,f,g,h];for(let i=0;i<8;i++)this.h[i]=(this.h[i]+v[i])>>>0}
  hex(){const bitLength=this.bytes*8,pad=(64-((this.buffer.length+9)%64))%64,tail=new Uint8Array(this.buffer.length+1+pad+8);tail.set(this.buffer);tail[this.buffer.length]=0x80;const view=new DataView(tail.buffer);view.setUint32(tail.length-8,Math.floor(bitLength/0x100000000));view.setUint32(tail.length-4,bitLength>>>0);for(let i=0;i<tail.length;i+=64)this.block(tail.subarray(i,i+64));return Array.from(this.h).map(v=>v.toString(16).padStart(8,'0')).join('')}
}

export type UploadTarget={businessType:string;businessRef:string;dataScopeId:number}
export type UploadProgress={phase:'hashing'|'uploading'|'completing';percent:number;uploadedChunks:number;totalChunks:number}
export const isLargeFile=(file:File)=>file.size>=LARGE_FILE
const hex=(buffer:ArrayBuffer)=>Array.from(new Uint8Array(buffer)).map(v=>v.toString(16).padStart(2,'0')).join('')

async function wholeHash(file:File,onProgress?:(value:UploadProgress)=>void,signal?:AbortSignal){const hash=new Sha256(),step=4*1024*1024,total=Math.ceil(file.size/step);for(let i=0;i<total;i++){if(signal?.aborted)throw new DOMException('上传已暂停','AbortError');hash.update(new Uint8Array(await file.slice(i*step,Math.min(file.size,(i+1)*step)).arrayBuffer()));onProgress?.({phase:'hashing',percent:Math.round((i+1)/total*10),uploadedChunks:0,totalChunks:total})}return hash.hex()}

export async function resumableUpload(file:File,target:UploadTarget,onProgress?:(value:UploadProgress)=>void,signal?:AbortSignal){
  const fingerprint=`${target.businessType}:${target.businessRef}:${target.dataScopeId}:${file.name}:${file.size}:${file.lastModified}`
  const storageKey=`rdp-upload:${fingerprint}`;let uploadId=localStorage.getItem(storageKey)||crypto.randomUUID();localStorage.setItem(storageKey,uploadId)
  const sha256=await wholeHash(file,onProgress,signal)
  const initiate=()=>http.post('/files/uploads',{uploadId,originalName:file.name,contentType:file.type||'application/octet-stream',sizeBytes:file.size,sha256,...target},{signal});let state:any;try{state=dataOf<any>(await initiate())}catch(error:any){if(![409,410].includes(Number(error?.response?.status)))throw error;uploadId=crypto.randomUUID();localStorage.setItem(storageKey,uploadId);state=dataOf<any>(await initiate())}
  const received=new Set<number>(state.uploadedChunks||[]);const chunkSize=Number(state.chunkSize),total=Number(state.totalChunks);let done=received.size,completing=false
  try{for(let index=0;index<total;index++){if(received.has(index))continue;const start=index*chunkSize,end=Math.min(file.size,start+chunkSize),bytes=await file.slice(start,end).arrayBuffer();const chunkSha=hex(await crypto.subtle.digest('SHA-256',bytes));await http.put(`/files/uploads/${uploadId}/chunks/${index}`,bytes,{signal,headers:{'Content-Type':'application/octet-stream','X-Chunk-SHA256':chunkSha,'Content-Range':`bytes ${start}-${end-1}/${file.size}`}});done++;onProgress?.({phase:'uploading',percent:10+Math.round(done/total*88),uploadedChunks:done,totalChunks:total})}
  completing=true;onProgress?.({phase:'completing',percent:99,uploadedChunks:total,totalChunks:total});const completed=dataOf<any>(await http.post(`/files/uploads/${uploadId}/complete`,null,{signal}));localStorage.removeItem(storageKey);onProgress?.({phase:'completing',percent:100,uploadedChunks:total,totalChunks:total});return completed}catch(error:any){const status=Number(error?.response?.status);if(status===410||completing&&status===400){localStorage.removeItem(storageKey);try{await cancelResumable(uploadId)}catch{} }throw error}
}

export async function cancelResumable(uploadId:string){await http.delete(`/files/uploads/${uploadId}`)}
export async function cancelResumableFile(file:File,target:UploadTarget){const fingerprint=`${target.businessType}:${target.businessRef}:${target.dataScopeId}:${file.name}:${file.size}:${file.lastModified}`,key=`rdp-upload:${fingerprint}`,uploadId=localStorage.getItem(key);if(uploadId)try{await cancelResumable(uploadId)}catch(error:any){if(error?.response?.status!==404)throw error}localStorage.removeItem(key)}
