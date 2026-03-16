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

        <form class="login-form">
          <div class="form-row">
            <input
              class="form-input"
              placeholder="请输入用户名"
            />
          </div>
          <div class="form-row">
            <input
              type="password"
              class="form-input"
              placeholder="请输入密码"
            />
          </div>

          <div class="form-row verify-row">
            <input
              class="form-input"
              placeholder="请输入验证码"
            />
            <div class="verify-img">3Y2A</div>
          </div>

          <button type="button" class="login-btn">登录</button>

          <div class="login-extra">
            <button type="button" class="text-btn">注册</button>
            <button type="button" class="text-btn">
              忘记密码？
            </button>
          </div>
        </form>
      </div>
    </div>
  </div>
</template>

<script setup>
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

