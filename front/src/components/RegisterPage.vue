<template>
  <section class="register-wrapper">
    <div class="register-card">
      <header class="register-header">
        <h1 class="register-title">欢迎注册账号</h1>
      </header>

      <form class="register-form">
        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            用户名
          </label>
          <input
            class="form-input"
            placeholder="请输入5-64位用户名，支持字母、数字与“_@”符号"
          />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            真实姓名
          </label>
          <input class="form-input" placeholder="请输入真实姓名" />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            电子邮箱
          </label>
          <input class="form-input" placeholder="请输入电子邮箱" />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            手机号码
          </label>
          <input class="form-input" placeholder="请输入手机号码" />
        </div>

        <div class="form-row">
          <label class="form-label">
            <span class="required">*</span>
            登录密码
          </label>
          <input
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
            <input class="form-input" placeholder="请输入验证码" />
            <div class="verify-img">1234</div>
          </div>
        </div>

        <button type="button" class="submit-btn">
          提交注册
        </button>
      </form>
    </div>
  </section>
</template>

<script setup>
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

