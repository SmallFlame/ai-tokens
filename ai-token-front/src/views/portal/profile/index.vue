<template>
  <div class="profile-wrap">
    <div class="page-title">个人中心</div>

    <!-- 信息未完善提示 -->
    <el-alert
      v-if="needsProfile"
      title="请先完善真实姓名和邮箱，填写完成后才能使用平台其它功能"
      type="warning"
      :closable="false"
      show-icon
      style="max-width:480px;margin-bottom:20px"
    />

    <!-- 基本信息 -->
    <el-card shadow="never" style="max-width:480px;margin-bottom:20px">
      <div slot="header">基本信息</div>
      <el-form label-width="90px">
        <el-form-item label="用户名">
          <span>{{ userInfo.username }}</span>
        </el-form-item>
        <el-form-item label="真实姓名">
          <div style="display:flex;align-items:center;gap:8px">
            <el-input
              v-if="editingNickname"
              v-model="nicknameValue"
              style="width:200px"
              placeholder="输入真实姓名"
              maxlength="20"
              @keyup.enter.native="saveNickname"
            />
            <span v-else>{{ userInfo.nickname || '未设置' }}</span>
            <el-button v-if="!editingNickname" type="text" @click="startEditNickname">修改</el-button>
            <template v-else>
              <el-button type="text" :loading="savingNickname" @click="saveNickname">保存</el-button>
              <el-button type="text" @click="editingNickname = false">取消</el-button>
            </template>
          </div>
        </el-form-item>
        <el-form-item label="邮箱">
          <div style="display:flex;align-items:center;gap:8px">
            <el-input
              v-if="editingEmail"
              v-model="emailValue"
              style="width:240px"
              placeholder="输入邮箱地址"
              @keyup.enter.native="saveEmail"
            />
            <span v-else>{{ userInfo.email || '未设置' }}</span>
            <el-button v-if="!editingEmail" type="text" @click="startEditEmail">修改</el-button>
            <template v-else>
              <el-button type="text" :loading="savingEmail" @click="saveEmail">保存</el-button>
              <el-button type="text" @click="editingEmail = false">取消</el-button>
            </template>
          </div>
        </el-form-item>
      </el-form>
    </el-card>

    <!-- 修改密码 -->
    <el-card shadow="never" style="max-width:480px">
      <div slot="header">修改密码</div>
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="原密码" prop="oldPassword">
          <el-input v-model="form.oldPassword" show-password placeholder="输入当前密码" />
        </el-form-item>
        <el-form-item label="新密码" prop="newPassword">
          <el-input v-model="form.newPassword" show-password placeholder="至少 6 位" />
        </el-form-item>
        <el-form-item label="确认密码" prop="confirmPassword">
          <el-input v-model="form.confirmPassword" show-password placeholder="再次输入新密码" />
        </el-form-item>
        <el-form-item>
          <el-button type="primary" :loading="saving" @click="submit">保存修改</el-button>
        </el-form-item>
      </el-form>
    </el-card>
  </div>
</template>

<script>
import { getMyInfo, changePassword, updateNickname, updateEmail } from '@/api/my'
import { mapGetters } from 'vuex'

export default {
  name: 'PortalProfile',
  computed: {
    ...mapGetters(['needsProfile'])
  },
  data() {
    const confirmValidator = (rule, value, cb) => {
      if (value !== this.form.newPassword) cb(new Error('两次密码不一致'))
      else cb()
    }
    return {
      userInfo: {},
      editingNickname: false,
      savingNickname: false,
      nicknameValue: '',
      editingEmail: false,
      savingEmail: false,
      emailValue: '',
      saving: false,
      form: { oldPassword: '', newPassword: '', confirmPassword: '' },
      rules: {
        oldPassword:     [{ required: true, message: '请输入原密码', trigger: 'blur' }],
        newPassword:     [{ required: true, min: 6, message: '新密码至少 6 位', trigger: 'blur' }],
        confirmPassword: [{ required: true, validator: confirmValidator, trigger: 'blur' }]
      }
    }
  },
  async created() {
    try {
      this.userInfo = await getMyInfo()
      // 同步到 store，避免刷新后状态不一致
      this.$store.commit('UPDATE_USER_INFO', {
        nickname: this.userInfo.nickname,
        email: this.userInfo.email
      })
    } catch (e) {}
  },
  methods: {
    startEditNickname() {
      this.nicknameValue = this.userInfo.nickname || ''
      this.editingNickname = true
    },
    async saveNickname() {
      this.savingNickname = true
      try {
        await updateNickname(this.nicknameValue)
        this.userInfo.nickname = this.nicknameValue
        this.$store.commit('UPDATE_USER_INFO', { nickname: this.nicknameValue })
        this.editingNickname = false
        this.$message.success('已更新')
      } finally {
        this.savingNickname = false
      }
    },
    startEditEmail() {
      this.emailValue = this.userInfo.email || ''
      this.editingEmail = true
    },
    async saveEmail() {
      this.savingEmail = true
      try {
        await updateEmail(this.emailValue)
        this.userInfo.email = this.emailValue
        this.$store.commit('UPDATE_USER_INFO', { email: this.emailValue })
        this.editingEmail = false
        this.$message.success('已更新')
      } finally {
        this.savingEmail = false
      }
    },
    async submit() {
      try { await this.$refs.formRef.validate() } catch { return }
      this.saving = true
      try {
        await changePassword({ oldPassword: this.form.oldPassword, newPassword: this.form.newPassword })
        this.$message.success('密码修改成功，请重新登录')
        this.$store.commit('CLEAR_AUTH')
        this.$router.push('/login')
      } finally {
        this.saving = false
      }
    }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; margin-bottom: 20px; }
</style>
