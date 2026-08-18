<script setup lang="ts">
import {computed,reactive,ref} from 'vue'
import {useRouter} from 'vue-router'
import {ElMessage} from 'element-plus'
import {http} from '../api/http'
import {useAuthStore} from '../stores/auth'

const router=useRouter()
const auth=useAuthStore()
const loading=ref(false)
const form=reactive({currentPassword:'',newPassword:'',confirmPassword:''})

const passwordRules=computed(()=>[
  {label:'8–20 位字符',valid:form.newPassword.length>=8&&form.newPassword.length<=20},
  {label:'包含大写字母',valid:/[A-Z]/.test(form.newPassword)},
  {label:'包含小写字母',valid:/[a-z]/.test(form.newPassword)},
  {label:'包含数字',valid:/\d/.test(form.newPassword)},
  {label:'不同于引导密码',valid:Boolean(form.currentPassword)&&form.newPassword!==form.currentPassword}
])
const passwordReady=computed(()=>passwordRules.value.every(item=>item.valid))
const confirmed=computed(()=>Boolean(form.confirmPassword)&&form.confirmPassword===form.newPassword)
const canSubmit=computed(()=>Boolean(form.currentPassword)&&passwordReady.value&&confirmed.value&&!loading.value)

async function submit(){
  if(!canSubmit.value){ElMessage.warning('请完成所有密码安全要求');return}
  loading.value=true
  try{
    await http.put('/user/password',form)
    auth.clearSession()
    ElMessage.success('密码已修改，请使用新密码重新登录')
    router.replace('/login')
  }finally{loading.value=false}
}

async function logout(){await auth.logout();router.replace('/login')}
</script>

<template>
  <main class="change-page">
    <div class="ambient ambient-one"></div>
    <div class="ambient ambient-two"></div>
    <section class="change-shell">
      <aside class="security-story">
        <div class="brand-row">
          <div class="brand-glyph">J</div>
          <div><b>嘉思特数据平台</b><span>JUSTEAM DATA FABRIC</span></div>
        </div>
        <div class="story-copy">
          <p class="eyebrow">SECURE ONBOARDING</p>
          <h1>完成首次<br>安全初始化</h1>
          <p>引导密码只承担一次身份确认。设置个人密码后，平台将撤销当前会话，避免临时凭据继续使用。</p>
        </div>
        <ol class="security-steps">
          <li class="done"><i>1</i><div><b>身份验证</b><span>已通过账号与引导密码登录</span></div></li>
          <li class="active"><i>2</i><div><b>设置个人密码</b><span>创建符合安全策略的新密码</span></div></li>
          <li><i>3</i><div><b>重新登录</b><span>使用新密码建立安全会话</span></div></li>
        </ol>
        <div class="trust-note"><i></i><span>密码修改操作受安全审计保护</span></div>
      </aside>

      <section class="change-form-panel">
        <header class="form-header">
          <div class="lock-icon"><span></span></div>
          <div><p>首次登录</p><h2>设置你的新密码</h2></div>
        </header>
        <p class="form-lead">创建仅由你掌握的新密码。修改成功后，需要重新登录平台。</p>

        <el-form label-position="top" @keyup.enter="submit">
          <el-form-item label="当前引导密码">
            <el-input v-model="form.currentPassword" type="password" show-password size="large" autocomplete="current-password" placeholder="请输入当前引导密码"/>
          </el-form-item>
          <el-form-item label="新密码">
            <el-input v-model="form.newPassword" type="password" show-password size="large" autocomplete="new-password" placeholder="请输入 8–20 位新密码"/>
          </el-form-item>

          <div class="rule-grid">
            <span v-for="rule in passwordRules" :key="rule.label" :class="{passed:rule.valid}"><i>{{rule.valid?'✓':'·'}}</i>{{rule.label}}</span>
          </div>

          <el-form-item label="确认新密码" class="confirm-field">
            <el-input v-model="form.confirmPassword" type="password" show-password size="large" autocomplete="new-password" placeholder="请再次输入新密码"/>
          </el-form-item>
          <p v-if="form.confirmPassword" class="match-tip" :class="{passed:confirmed}">{{confirmed?'✓ 两次输入一致':'两次输入的新密码不一致'}}</p>

          <el-button type="primary" size="large" :loading="loading" :disabled="!canSubmit" class="submit-button" @click="submit">设置密码并重新登录</el-button>
          <el-button text class="exit-button" @click="logout">退出当前账号</el-button>
        </el-form>
        <footer><span>安全提示</span> 平台工作人员不会向你索要密码</footer>
      </section>
    </section>
  </main>
</template>

<style scoped>
.change-page{position:relative;min-height:100vh;display:grid;place-items:center;box-sizing:border-box;padding:34px;overflow:hidden;background:linear-gradient(145deg,#061322 0%,#0a1b2e 48%,#071522 100%);color:#e8f2ff}
.change-page:before{content:'';position:absolute;inset:0;background-image:linear-gradient(rgba(66,177,220,.035) 1px,transparent 1px),linear-gradient(90deg,rgba(66,177,220,.035) 1px,transparent 1px);background-size:36px 36px;mask-image:linear-gradient(to bottom,black,transparent 90%)}
.ambient{position:absolute;border-radius:50%;filter:blur(2px);pointer-events:none}.ambient-one{width:560px;height:560px;left:-220px;top:-260px;background:radial-gradient(circle,rgba(31,180,227,.17),transparent 68%)}.ambient-two{width:620px;height:620px;right:-250px;bottom:-340px;background:radial-gradient(circle,rgba(25,111,172,.19),transparent 68%)}
.change-shell{position:relative;z-index:1;width:min(1040px,calc(100vw - 68px));min-height:660px;display:grid;grid-template-columns:.86fr 1.14fr;overflow:hidden;border:1px solid rgba(102,193,230,.22);border-radius:24px;background:rgba(9,25,43,.92);box-shadow:0 35px 90px rgba(0,0,0,.42),inset 0 1px rgba(255,255,255,.035);backdrop-filter:blur(20px)}
.security-story{position:relative;display:flex;flex-direction:column;padding:42px 46px;background:linear-gradient(160deg,rgba(17,53,80,.94),rgba(8,31,51,.96));border-right:1px solid rgba(99,185,221,.15)}
.security-story:after{content:'';position:absolute;width:270px;height:270px;right:-130px;top:130px;border:1px solid rgba(60,187,230,.1);border-radius:50%;box-shadow:0 0 0 45px rgba(60,187,230,.025),0 0 0 90px rgba(60,187,230,.018)}
.brand-row{position:relative;z-index:1;display:flex;align-items:center;gap:13px}.brand-glyph{width:38px;height:38px;display:grid;place-items:center;border-radius:11px;background:linear-gradient(145deg,#24b4dd,#14799f);color:white;font:800 20px/1 sans-serif;box-shadow:0 8px 22px rgba(31,177,221,.22)}.brand-row div:last-child{display:flex;flex-direction:column;gap:2px}.brand-row b{font-size:14px;letter-spacing:.08em}.brand-row span{font-size:8px;letter-spacing:.2em;color:#69a2ba}
.story-copy{position:relative;z-index:1;margin-top:75px}.eyebrow{margin:0 0 15px;color:#3fb9e3;font-size:10px;letter-spacing:.25em}.story-copy h1{margin:0;font-size:40px;line-height:1.25;letter-spacing:.04em;color:#f0f7ff}.story-copy>p:last-child{max-width:315px;margin:22px 0 0;color:#8ca5b8;font-size:13px;line-height:1.9}
.security-steps{position:relative;z-index:1;list-style:none;margin:48px 0 0;padding:0}.security-steps li{position:relative;display:flex;align-items:center;gap:14px;padding-bottom:22px;color:#607b8f}.security-steps li:not(:last-child):after{content:'';position:absolute;left:14px;top:31px;width:1px;height:20px;background:#24445c}.security-steps i{width:28px;height:28px;display:grid;place-items:center;border:1px solid #31566f;border-radius:50%;font-style:normal;font-size:11px}.security-steps div{display:flex;flex-direction:column;gap:3px}.security-steps b{font-size:13px;font-weight:600}.security-steps span{font-size:10px}.security-steps .done{color:#6f9bad}.security-steps .done i{background:#173e54;border-color:#2b91b4;color:#5dd0f5}.security-steps .active{color:#e5f4ff}.security-steps .active i{background:#1689b1;border-color:#32c5ef;color:white;box-shadow:0 0 0 5px rgba(41,182,225,.1)}
.trust-note{position:relative;z-index:1;margin-top:auto;display:flex;align-items:center;gap:9px;color:#64869c;font-size:10px}.trust-note i{width:7px;height:7px;border-radius:50%;background:#39c491;box-shadow:0 0 0 4px rgba(57,196,145,.09)}
.change-form-panel{padding:48px 58px 32px;background:linear-gradient(180deg,rgba(10,27,46,.98),rgba(8,23,40,.98))}.form-header{display:flex;align-items:center;gap:16px}.form-header>div:last-child p{margin:0 0 3px;color:#39bce8;font-size:10px;letter-spacing:.18em}.form-header h2{margin:0;font-size:25px;color:#edf7ff}.lock-icon{width:46px;height:46px;display:grid;place-items:center;border:1px solid rgba(70,192,232,.25);border-radius:14px;background:rgba(29,125,163,.13)}.lock-icon span{position:relative;width:17px;height:14px;border:2px solid #4ac6ec;border-radius:4px}.lock-icon span:before{content:'';position:absolute;width:10px;height:9px;left:1.5px;top:-10px;border:2px solid #4ac6ec;border-bottom:0;border-radius:8px 8px 0 0}.form-lead{margin:20px 0 27px;color:#819bae;font-size:12px;line-height:1.7}
.el-form-item{margin-bottom:20px}:deep(.el-form-item__label){padding-bottom:7px;color:#adc0cf;font-size:12px}:deep(.el-input__wrapper){min-height:46px;padding:0 15px;border:1px solid rgba(75,142,176,.28);border-radius:10px;background:rgba(3,15,28,.72);box-shadow:none}:deep(.el-input__wrapper:hover){border-color:rgba(61,184,224,.52)}:deep(.el-input__wrapper.is-focus){border-color:#31bce8;box-shadow:0 0 0 3px rgba(49,188,232,.1)}:deep(.el-input__inner){color:#e9f5ff;font-size:13px}:deep(.el-input__inner::placeholder){color:#456175}
.rule-grid{display:grid;grid-template-columns:1fr 1fr;gap:7px 18px;margin:-7px 0 22px}.rule-grid span{display:flex;align-items:center;gap:7px;color:#587387;font-size:10px;transition:.2s}.rule-grid i{width:14px;height:14px;display:grid;place-items:center;border:1px solid #35536a;border-radius:50%;font-style:normal;font-size:9px}.rule-grid span.passed{color:#61cba6}.rule-grid span.passed i{border-color:#2f9e7a;background:rgba(47,158,122,.12)}.confirm-field{margin-bottom:6px}.match-tip{margin:0 0 13px;color:#e07e7e;font-size:10px}.match-tip.passed{color:#61cba6}
.submit-button{width:100%;height:47px;margin-top:8px;border-radius:10px;font-weight:650;letter-spacing:.03em;background:linear-gradient(90deg,#137da7,#1ba8d2);border-color:#28bde9;box-shadow:0 10px 24px rgba(19,135,174,.18)}.submit-button:not(.is-disabled):hover{transform:translateY(-1px);filter:brightness(1.08)}.submit-button.is-disabled{background:#18364b;border-color:#244a61;color:#58788d}.exit-button{width:100%;margin:9px 0 0;color:#7893a6}.exit-button:hover{color:#c2d6e4;background:rgba(71,129,161,.08)}
.change-form-panel footer{margin-top:20px;padding-top:16px;border-top:1px solid rgba(89,145,176,.12);text-align:center;color:#526f82;font-size:9px}.change-form-panel footer span{margin-right:6px;color:#7095aa}
@media (max-width:820px){.change-page{padding:18px}.change-shell{width:min(520px,calc(100vw - 36px));min-height:auto;grid-template-columns:1fr}.security-story{display:none}.change-form-panel{padding:38px 32px 28px}}
@media (max-width:480px){.change-page{align-items:start;padding-top:18px}.change-shell{border-radius:18px}.change-form-panel{padding:30px 22px 24px}.form-header h2{font-size:21px}.rule-grid{grid-template-columns:1fr}.form-lead{margin-bottom:22px}}
</style>
