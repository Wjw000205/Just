<template>
  <section class="register-wrapper">
    <div class="register-card">
      <header class="register-header">
        <h1 class="register-title">欢迎注册账号</h1>
      </header>

      <form class="register-form" @submit.prevent="handleRegister">
        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            用户名
          </label>
          <input
            v-model="form.username"
            class="form-input"
            placeholder="请输入5-64位用户名，支持字母、数字与“_@”符号"
          />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            真实姓名
          </label>
          <input
            v-model="form.realName"
            class="form-input"
            placeholder="请输入真实姓名"
          />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            电子邮箱
          </label>
          <input
            v-model="form.email"
            class="form-input"
            placeholder="请输入电子邮箱"
          />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            手机号码
          </label>
          <input
            v-model="form.telephone"
            class="form-input"
            placeholder="请输入手机号码"
          />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            登录密码
          </label>
          <input
            v-model="form.password"
            type="password"
            class="form-input"
            placeholder="密码至少6位，必须包含字母、数字、特殊符号"
          />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            确认密码
          </label>
          <input
            v-model="form.confirmPassword"
            type="password"
            class="form-input"
            placeholder="请再次输入密码"
          />
        </div>

        <div class="form-row verify-row">
          <label class="form-label">
            <span class="required">*</span>
            验证码
          </label>
          <div class="verify-group">
            <input
              v-model="form.captcha"
              class="form-input"
              placeholder="请输入右侧算式结果"
            />
            <div class="verify-img" @click="refreshCaptcha" title="看不清？点击换一题">
              {{ captchaQuestion }}
            </div>
          </div>
        </div>

        <button type="submit" class="submit-btn" :disabled="submitting">
          {{ submitting ? '注册中…' : '提交注册' }}
        </button>
      </form>
    </div>
  </section>
</template>

<script setup>
import { reactive, ref, onMounted } from 'vue'

const emit = defineEmits(['register-success'])

const form = reactive({
  username: '',
  realName: '',
  email: '',
  telephone: '',
  password: '',
  confirmPassword: '',
  captcha: '',
})

const submitting = ref(false)
const captchaQuestion = ref('')
let captchaAnswer = 0

function generateCaptcha() {
  const a = Math.floor(Math.random() * 9) + 1 // 1-9
  const b = Math.floor(Math.random() * 9) + 1
  const ops = ['+', '-', '×']
  const op = ops[Math.floor(Math.random() * ops.length)]

  if (op === '+') {
    captchaAnswer = a + b
  } else if (op === '-') {
    // 保证结果非负
    const max = Math.max(a, b)
    const min = Math.min(a, b)
    captchaAnswer = max - min
    captchaQuestion.value = `${max} - ${min} = ?`
    return
  } else {
    captchaAnswer = a * b
  }

  if (op !== '-') {
    captchaQuestion.value = `${a} ${op} ${b} = ?`
  }
}

function refreshCaptcha() {
  form.captcha = ''
  generateCaptcha()
}

onMounted(() => {
  generateCaptcha()
})

async function handleRegister() {
  if (!form.username.trim()) {
    alert('请输入用户名')
    return
  }
  if (!form.password || form.password.length < 6) {
    alert('请输入至少6位密码')
    return
  }
  if (form.password !== form.confirmPassword) {
    alert('两次输入的密码不一致')
    return
  }
  if (!form.telephone.trim()) {
    alert('请输入手机号码')
    return
  }
  if (!form.email.trim()) {
    alert('请输入电子邮箱')
    return
  }
  if (!form.captcha.trim()) {
    alert('请输入验证码答案')
    return
  }
  if (Number(form.captcha) !== captchaAnswer) {
    alert('验证码错误，请重新输入')
    refreshCaptcha()
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await fetch('http://localhost:8083/user/register', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: form.username.trim(),
        password: form.password,
        telephone: form.telephone.trim(),
        email: form.email.trim(),
      }),
    })

    let json = null
    try {
      json = await res.json()
    } catch (_) {
      // 后端可能返回空体或非 JSON，忽略解析错误
    }

    if (!res.ok) {
      alert((json && json.message) || `注册接口请求失败（HTTP ${res.status}）`)
      return
    }

    if (json && json.code === 0) {
      alert(json.message || '注册成功')
      emit('register-success', json)
    } else {
      alert((json && json.message) || '注册失败')
    }
  } catch (e) {
    console.error('register error', e)
    alert('注册请求失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.register-wrapper {
  min-height: calc(100vh - 112px);
  display: flex;
  align-items: center;
  justify-content: center;
  background: #f5f7fb;
}

.register-card {
  width: 760px;
  background: #fff;
  border-radius: 10px;
  box-shadow: 0 10px 30px rgba(26, 78, 158, 0.18);
  padding: 32px 40px 36px;
}

.register-header {
  margin-bottom: 22px;
}

.register-title {
  font-size: 20px;
  font-weight: 600;
  color: #333;
}

.register-form {
  display: flex;
  flex-direction: column;
  gap: 14px;
}

.form-row {
  display: flex;
  align-items: center;
}

.form-label {
  width: 96px;
  font-size: 13px;
  color: #333;
  flex-shrink: 0;
  text-align: right;
  margin-right: 10px;
}

.required {
  color: #ff4d4f;
  margin-right: 4px;
}

.form-input {
  flex: 1;
  border-radius: 4px;
  border: 1px solid #d4dae6;
  padding: 8px 10px;
  font-size: 13px;
}

.form-input::placeholder {
  color: #b0b6c6;
}

.verify-row {
  align-items: flex-start;
}

.verify-group {
  flex: 1;
  display: flex;
  align-items: center;
  gap: 10px;
}

.verify-img {
  width: 120px;
  height: 34px;
  border-radius: 4px;
  background: linear-gradient(120deg, #1a5ce6, #4f9efc);
  color: #fff;
  font-weight: 600;
  font-size: 18px;
  letter-spacing: 4px;
  display: flex;
  align-items: center;
  justify-content: center;
  user-select: none;
}

.submit-btn {
  margin-top: 10px;
  width: 100%;
  border-radius: 4px;
  border: none;
  padding: 10px 0;
  background: #1a5ce6;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}
</style>

