<template>
  <div class="login-overlay">
    <div class="login-card">
      <div class="login-left">
        <div class="hi-badge">Hi</div>
        <div class="illus-stack">
          <div class="illus-base"></div>
          <div class="illus-middle"></div>
          <div class="illus-top"></div>
        </div>
      </div>

      <div class="login-right">
        <h2 class="login-title">欢迎登录材料数据库系统</h2>

        <form class="login-form" @submit.prevent="handleLogin">
          <div class="form-row">
            <input
              v-model="username"
              class="form-input"
              placeholder="请输入用户名"
              autocomplete="username"
            />
          </div>
          <div class="form-row">
            <input
              v-model="password"
              type="password"
              class="form-input"
              placeholder="请输入密码"
              autocomplete="current-password"
            />
          </div>

          <div class="form-row verify-row">
            <input
              v-model="captcha"
              class="form-input"
              placeholder="请输入右侧算式结果"
            />
            <div
              class="verify-img"
              @click="refreshCaptcha"
              title="看不清？点击换一题"
            >
              {{ captchaQuestion }}
            </div>
          </div>

          <button
            type="submit"
            class="login-btn"
            :disabled="submitting"
          >
            {{ submitting ? '登录中…' : '登录' }}
          </button>

          <div class="login-extra">
            <button
              type="button"
              class="text-btn"
              @click="emit('go-register')"
            >
              注册
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
import { ref, onMounted } from 'vue'

const emit = defineEmits(['login-success', 'go-register'])

const username = ref('')
const password = ref('')
const captcha = ref('')
const submitting = ref(false)
const captchaQuestion = ref('')
let captchaAnswer = 0

function generateCaptcha() {
  const a = Math.floor(Math.random() * 9) + 1
  const b = Math.floor(Math.random() * 9) + 1
  const ops = ['+', '-', '×']
  const op = ops[Math.floor(Math.random() * ops.length)]

  if (op === '+') {
    captchaAnswer = a + b
    captchaQuestion.value = `${a} + ${b} = ?`
  } else if (op === '-') {
    const max = Math.max(a, b)
    const min = Math.min(a, b)
    captchaAnswer = max - min
    captchaQuestion.value = `${max} - ${min} = ?`
  } else {
    captchaAnswer = a * b
    captchaQuestion.value = `${a} × ${b} = ?`
  }
}

function refreshCaptcha() {
  captcha.value = ''
  generateCaptcha()
}

onMounted(() => {
  generateCaptcha()
})

async function handleLogin() {
  if (!username.value.trim() || !password.value.trim()) {
    alert('请输入用户名和密码')
    return
  }
  if (!captcha.value.trim()) {
    alert('请输入验证码答案')
    return
  }
  if (Number(captcha.value) !== captchaAnswer) {
    alert('验证码错误，请重新输入')
    refreshCaptcha()
    return
  }
  if (submitting.value) return
  submitting.value = true
  try {
    const res = await fetch('http://localhost:8083/user/login', {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify({
        username: username.value.trim(),
        password: password.value,
      }),
    })

    let json = null
    try {
      json = await res.json()
    } catch (_) {
      // 后端可能返回空体或非 JSON，忽略解析错误
    }

    if (!res.ok) {
      alert((json && json.message) || `登录接口请求失败（HTTP ${res.status}）`)
      return
    }

    const okCode = json && (json.code === 0 || json.code === 200)
    if (okCode) {
      // token 兼容：后端可能把 token 放在 data 或 message
      const tokenCandidate =
        (typeof json?.data === 'string' ? json.data : '') ||
        (typeof json?.message === 'string' ? json.message : '')
      const token =
        typeof tokenCandidate === 'string' && tokenCandidate.split('.').length === 3
          ? tokenCandidate
          : ''
      if (token) {
        localStorage.setItem('token', token)
        sessionStorage.removeItem('token')
      }
      // 先通知父组件跳转，再提示（alert 会阻塞）
      emit('login-success', { token, raw: json })
      alert('登录成功')
    } else {
      alert((json && json.message) || '登录失败')
    }
  } catch (e) {
    console.error('login error', e)
    alert('登录请求失败，请稍后重试')
  } finally {
    submitting.value = false
  }
}
</script>

<style scoped>
.login-overlay {
  position: fixed;
  inset: 112px 0 0;
  display: flex;
  align-items: center;
  justify-content: center;
  background: radial-gradient(circle at 20% 0, rgba(26, 92, 230, 0.08), transparent 55%),
    radial-gradient(circle at 80% 100%, rgba(26, 92, 230, 0.12), transparent 55%);
  z-index: 200;
}

.login-card {
  width: 760px;
  min-height: 360px;
  border-radius: 12px;
  background: #fff;
  box-shadow: 0 20px 40px rgba(26, 78, 158, 0.3);
  display: grid;
  grid-template-columns: 1.1fr 1.2fr;
  overflow: hidden;
}

.login-left {
  position: relative;
  background: linear-gradient(140deg, #2f7ff0, #64a8ff);
  display: flex;
  align-items: center;
  justify-content: center;
}

.hi-badge {
  position: absolute;
  top: 18px;
  left: 22px;
  width: 40px;
  height: 40px;
  border-radius: 50%;
  background: #fff;
  color: #2f7ff0;
  display: flex;
  align-items: center;
  justify-content: center;
  font-weight: 700;
  font-size: 18px;
}

.illus-stack {
  width: 200px;
  height: 180px;
  position: relative;
}

.illus-base,
.illus-middle,
.illus-top {
  position: absolute;
  left: 20px;
  right: 20px;
  border-radius: 18px;
  background: rgba(255, 255, 255, 0.14);
}

.illus-base {
  bottom: 25px;
  height: 40px;
}

.illus-middle {
  bottom: 60px;
  height: 44px;
  background: rgba(255, 255, 255, 0.2);
}

.illus-top {
  bottom: 96px;
  height: 48px;
  background: rgba(255, 255, 255, 0.9);
}

.login-right {
  padding: 32px 40px 30px;
  display: flex;
  flex-direction: column;
}

.login-title {
  font-size: 18px;
  font-weight: 600;
  margin-bottom: 24px;
  color: #333;
}

.login-form {
  display: flex;
  flex-direction: column;
  gap: 12px;
}

.form-row {
  display: flex;
  align-items: center;
  gap: 10px;
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

.verify-row .form-input {
  flex: 1.1;
}

.verify-img {
  width: 110px;
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

.login-btn {
  margin-top: 2px;
  width: 100%;
  border-radius: 4px;
  border: none;
  padding: 10px 0;
  background: #1a5ce6;
  color: #fff;
  font-size: 15px;
  font-weight: 500;
}

.login-extra {
  margin-top: 6px;
  display: flex;
  justify-content: space-between;
  font-size: 12px;
}

.text-btn {
  border: none;
  background: transparent;
  padding: 0;
  color: #1a5ce6;
}
</style>

