<template>
  <div class="login-wrap">
    <div class="login-box">
      <div class="login-header">
        <i class="el-icon-magic-stick logo-icon" />
        <h1 class="title">AI Token 管理平台</h1>
        <p class="subtitle">AI 接口调用与 Token 用量管理</p>
      </div>

      <el-form :model="form" :rules="rules" ref="formRef" @keyup.enter.native="handleLogin">
        <el-form-item prop="username">
          <el-input
            v-model="form.username"
            prefix-icon="el-icon-user"
            placeholder="用户名"
            size="medium"
          />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            prefix-icon="el-icon-lock"
            show-password
            placeholder="密码"
            size="medium"
          />
        </el-form-item>
        <el-form-item>
          <el-button
            type="primary"
            size="medium"
            class="login-btn"
            :loading="loading"
            @click="handleLogin"
          >
            登 录
          </el-button>
        </el-form-item>
      </el-form>

      <!-- <p class="tip">默认账号: admin / admin123</p> -->
      <p class="register-link">
        没有账号？
        <el-link type="primary" @click="$router.push('/register')">立即注册</el-link>
      </p>
    </div>
  </div>
</template>

<script>
import { login } from '@/api/auth'

export default {
  name: 'LoginPage',
  data() {
    return {
      loading: false,
      form: { username: '', password: '' },
      //form: { username: 'admin', password: 'admin123' },
      rules: {
        username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
        password: [{ required: true, message: '请输入密码',   trigger: 'blur' }]
      }
    }
  },
  methods: {
    async handleLogin() {
      try {
        await this.$refs.formRef.validate()
      } catch {
        return
      }
      this.loading = true
      try {
        const data = await login(this.form)
        this.$store.commit('SET_LOGIN_INFO', {
          token: data.token,
          userInfo: {
            userId: data.userId,
            username: data.username,
            nickname: data.nickname,
            roleCode: data.roleCode,
            roleName: data.roleName,
            groupId: data.groupId,
            groupName: data.groupName,
            studentId: data.studentId,
            avatar: data.avatar,
            email: data.email
          }
        })

        const isAdmin = ['super_admin', 'advisor'].includes(data.roleCode)
        const missing = !(data.nickname || '').trim() || !(data.email || '').trim()

        // 普通用户缺真实姓名或邮箱：引导去个人中心补全
        if (!isAdmin && missing) {
          try {
            await this.$alert(
              '请先完善真实姓名和邮箱，完成后方可使用平台其它功能。',
              '需要完善个人信息',
              { confirmButtonText: '去填写', type: 'warning',
                showClose: false, closeOnClickModal: false, closeOnPressEscape: false }
            )
          } catch { /* 忽略关闭异常，仍然强制跳转 */ }
          this.$router.push('/portal/profile')
          return
        }

        this.$message.success('登录成功')
        // 超管/导师 → 管理后台，其他 → 用户门户
        this.$router.push(isAdmin ? '/dashboard' : '/portal/overview')
      } finally {
        this.loading = false
      }
    }
  }
}
</script>

<style scoped>
.login-wrap {
  min-height: 100vh;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1a1a2e 0%, #16213e 50%, #0f3460 100%);
}
.login-box {
  width: 420px;
  background: #fff;
  border-radius: 12px;
  padding: 52px 44px 36px;
  box-shadow: 0 20px 60px rgba(0, 0, 0, 0.35);
}
.login-header { text-align: center; margin-bottom: 36px; }
.logo-icon { font-size: 60px; color: #409EFF; }
.title { font-size: 26px; margin: 12px 0 6px; color: #303133; font-weight: 600; }
.subtitle { color: #909399; font-size: 16px; }
.login-btn { width: 100%; margin-top: 8px; font-size: 16px; }
.tip { text-align: center; font-size: 14px; color: #c0c4cc; margin-top: 20px; }
.register-link { text-align: center; font-size: 14px; color: #909399; margin-top: 8px; }
</style>
