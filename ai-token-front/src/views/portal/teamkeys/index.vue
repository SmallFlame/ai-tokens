<template>
  <div>
    <div style="display:flex;justify-content:space-between;align-items:center;margin-bottom:20px">
      <div class="page-title">团队密钥</div>
      <div style="display:flex;align-items:center;gap:12px">
        <span class="money-badge">
          金额池：<strong>¥{{ fmtMoney(allocatableMoney) }}</strong>
        </span>
        <el-button type="primary" size="small" icon="el-icon-plus" @click="openCreate">
          创建密钥
        </el-button>
      </div>
    </div>

    <el-card shadow="never">
      <el-table :data="keys" stripe v-loading="loading">
        <el-table-column label="归属" width="120">
          <template slot-scope="{ row }">
            <el-tag :type="row._isSelf ? 'success' : 'info'" size="mini">
              {{ row._ownerName || row.userId }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="名称" prop="name" min-width="110" />
        <el-table-column label="密钥" min-width="260">
          <template slot-scope="{ row }">
            <span class="key-text">{{ visibleKeys[row.id] ? row.keyValue : maskKey(row.keyValue) }}</span>
            <el-button type="text" :icon="visibleKeys[row.id] ? 'el-icon-view' : 'el-icon-hide'" @click="toggleVisible(row.id)" />
            <el-button type="text" icon="el-icon-copy-document" @click="copy(row.keyValue)" />
          </template>
        </el-table-column>
        <el-table-column label="类型" width="120">
          <template slot-scope="{ row }">
            <el-tag :type="row.keyType === 1 ? 'warning' : 'primary'" size="mini">
              {{ row.keyType === 1 ? 'Token额度型' : '金额消费型' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="状态" width="80">
          <template slot-scope="{ row }">
            <el-tag :type="row.status === 1 ? 'success' : 'danger'" size="mini">
              {{ row.status === 1 ? '正常' : '已禁用' }}
            </el-tag>
          </template>
        </el-table-column>
        <el-table-column label="操作" width="80" fixed="right">
          <template slot-scope="{ row }">
            <el-popconfirm title="确认吊销该密钥？" @confirm="revoke(row.id)">
              <el-button slot="reference" type="text" style="color:#F56C6C">吊销</el-button>
            </el-popconfirm>
          </template>
        </el-table-column>
      </el-table>
    </el-card>

    <!-- 创建密钥对话框 -->
    <el-dialog title="为团队成员创建密钥" :visible.sync="showCreate" width="460px" @close="resetCreate">
      <el-form :model="createForm" :rules="createRules" ref="createRef" label-width="90px">
        <el-form-item label="归属成员" prop="targetUserId">
          <el-select v-model="createForm.targetUserId" placeholder="选择组员（留空=自己）" clearable style="width:100%">
            <el-option :value="selfId" label="我自己（组长）" />
            <el-option
              v-for="m in members"
              :key="m.id"
              :value="m.id"
              :label="m.username"
            />
          </el-select>
        </el-form-item>
        <el-form-item label="密钥名称" prop="name">
          <el-input v-model="createForm.name" placeholder="如：工作用Key" />
        </el-form-item>
        <el-form-item label="过期时间">
          <el-date-picker
            v-model="createForm.expiredAt"
            type="datetime"
            placeholder="不填则永不过期"
            value-format="yyyy-MM-dd HH:mm:ss"
            style="width:220px"
          />
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="showCreate = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="doCreate">创建</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getTeamMembers, getTeamKeys, createTeamKey, revokeTeamKey, getMyQuota } from '@/api/team'
import { copyText } from '@/utils/clipboard'
import { mapState } from 'vuex'

export default {
  name: 'TeamKeysPage',
  data() {
    return {
      loading: false,
      keys: [],
      members: [],
      allocatableMoney: 0,
      selfId: null,
      visibleKeys: {},
      // 创建
      showCreate: false,
      saving: false,
      createForm: { targetUserId: null, name: '', expiredAt: null },
      createRules: {
        name: [{ required: true, message: '请输入密钥名称', trigger: 'blur' }]
      }
    }
  },
  computed: {
    ...mapState({ userInfo: 'userInfo' })
  },
  created() {
    this.selfId = this.userInfo && this.userInfo.userId
    this.load()
    this.loadQuota()
    this.loadMembers()
  },
  methods: {
    async load() {
      this.loading = true
      try {
        const rawKeys = await getTeamKeys()
        this.keys = rawKeys.map(k => ({
          ...k,
          _isSelf: k.userId === this.selfId,
          _ownerName: k.userId === this.selfId ? '我（组长）' : this.getMemberName(k.userId)
        }))
      } finally {
        this.loading = false
      }
    },
    async loadQuota() {
      const data = await getMyQuota()
      this.allocatableMoney = data.allocatableMoney || 0
    },
    async loadMembers() {
      this.members = await getTeamMembers()
    },
    getMemberName(userId) {
      const m = this.members.find(x => x.id === userId)
      return m ? m.username : userId
    },
    openCreate() {
      this.createForm = { targetUserId: this.selfId, name: '', expiredAt: null }
      this.showCreate = true
    },
    resetCreate() {
      this.createForm = { targetUserId: null, name: '', expiredAt: null }
    },
    async doCreate() {
      this.$refs.createRef.validate(async valid => {
        if (!valid) return
        this.saving = true
        try {
          const targetId = this.createForm.targetUserId || this.selfId
          await createTeamKey(targetId, {
            name: this.createForm.name,
            keyType: 2,
            expiredAt: this.createForm.expiredAt
          })
          this.$message.success('创建成功')
          this.showCreate = false
          this.load()
        } finally {
          this.saving = false
        }
      })
    },
    async revoke(id) {
      await revokeTeamKey(id)
      this.$message.success('已吊销')
      this.load()
    },
    fmtMoney(val) {
      if (val == null) return '0.00'
      return Number(val).toFixed(2)
    },
    copy(text) {
      copyText(text).then(() => {
        this.$message.success('已复制')
      }).catch(() => this.$message.error('复制失败'))
    },
    maskKey(key) {
      if (!key) return '****'
      if (key.length <= 12) return key.slice(0, 4) + '****'
      return key.slice(0, 8) + '****' + key.slice(-4)
    },
    toggleVisible(id) {
      this.$set(this.visibleKeys, id, !this.visibleKeys[id])
    }
  }
}
</script>

<style scoped>
.page-title { font-size: 18px; font-weight: 600; color: #303133; }
.money-badge {
  background: #f0f9eb;
  color: #67C23A;
  border: 1px solid #c2e7b0;
  border-radius: 6px;
  padding: 4px 12px;
  font-size: 13px;
}
.money-badge strong { font-size: 15px; }
.key-text {
  font-family: monospace;
  font-size: 13px;
  background: #f5f7fa;
  padding: 2px 6px;
  border-radius: 4px;
  margin-right: 4px;
}
</style>
