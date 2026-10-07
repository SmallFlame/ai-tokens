<template>
  <div class="register-wrap">
    <div class="register-box">
      <div class="register-header">
        <i class="el-icon-magic-stick logo-icon" />
        <h1 class="title">创建账号</h1>
        <p class="subtitle">AI Token 管理平台</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="el-icon-user"
            placeholder="用户名（3-32 位）"
            size="medium"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            prefix-icon="el-icon-lock"
            show-password
            placeholder="密码（6-32 位）"
            size="medium"
          />
        </el-form-item>
        <el-form-item prop="confirmPassword">
          <el-input
            v-model="form.confirmPassword"
            prefix-icon="el-icon-lock"
            show-password
            placeholder="确认密码"
            size="medium"
          />
        </el-form-item>
        <el-form-item prop="email">
          <el-input
            v-model="form.email"
            prefix-icon="el-icon-message"
            placeholder="邮箱（选填）"
            size="medium"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="medium"
            class="register-btn"
            :loading="loading"
            @click="handleRegister"
          >
            注 册
          </el-button>
        </el-form-item>
      </el-form>

      <p class="back-link">
        已有账号？
        <el-link type="primary" @click="$router.push('/login')">立即登录</el-link>
      </p>
    </div>
  </div>
</template>

<script>
import { register } from '@/api/auth'

export default {
  name: 'RegisterPage',
  data() {
    const validateConfirm = (rule, value, callback) => {
      if (value !== this.form.password) {
        callback(new Error('两次输入的密码不一致'))
      } else {
        callback()
      }
    }
    return {
      loading: false,
      form: { username: '', password: '', confirmPassword: '', email: '' },
      rules: {
        username: [
          { required: true, message: '请输入用户名', trigger: 'blur' },
          { min: 3, max: 32, message: '用户名长度 3-32 位', trigger: 'blur' }
        ],
        password: [
          { required: true, message: '请输入密码', trigger: 'blur' },
          { min: 6, max: 32, message: '密码长度 6-32 位', trigger: 'blur' }
        ],
        confirmPassword: [
          { required: true, message: '请确认密码', trigger: 'blur' },
          { validator: validateConfirm, trigger: 'blur' }
        ],
        email: [
          { type: 'email', message: '邮箱格式不正确', trigger: 'blur' }
        ]
      }
    }
  },
  methods: {
    async handleRegister() {
      try {
        await this.$refs.formRef.validate()
      } catch {
        return
      }
      this.loading = true
      try {
        await register({
          username: this.form.username,
          password: this.form.password,
          email: this.form.email || undefined
        })
        this.$message.success('注册成功，请登录')
        this.$router.push('/login')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.register-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.register-box {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 52px 44px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
}
.register-header { text-align: center; margin-bottom: 36px; }
.logo-icon { font-size: 60px; color: #409EFF; }
.title { font-size: 26px; margin: 12px 0 6px; color: #303133; font-weight: 600; }
.subtitle { color: #909399; font-size: 16px; }
.register-btn { width: 100%; margin-top: 8px; font-size: 16px; }
.back-link { text-align: center; font-size: 14px; color: #909399; margin-top: 20px; }
</style>
