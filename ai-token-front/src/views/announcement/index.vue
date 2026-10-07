<template>
  <div class="page-card">
    <div class="toolbar">
      <el-button type="primary" icon="el-icon-plus" @click="openDialog()">新增公告</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="id"        label="ID"       width="60" />
      <el-table-column prop="title"     label="标题"     min-width="200" show-overflow-tooltip />
      <el-table-column prop="publisher" label="发布人"   width="120" />
      <el-table-column label="主页展示" width="100" align="center">
        <template slot-scope="{ row }">
          <el-tag :type="row.showOnHome === 1 ? 'success' : 'info'" size="mini">
            {{ row.showOnHome === 1 ? '展示中' : '未展示' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="createdAt" label="发布时间" width="180" />
      <el-table-column label="操作" width="160" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" @click="openDialog(row)">编辑</el-button>
          <el-button type="text" @click="preview(row)">预览</el-button>
          <el-button type="text" class="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 新增/编辑弹窗 -->
    <el-dialog :title="form.id ? '编辑公告' : '新增公告'" :visible.sync="dialogVisible" width="700px" @close="resetForm">
      <el-form :model="form" :rules="rules" ref="formRef" label-width="90px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="form.title" placeholder="公告标题" maxlength="200" show-word-limit />
        </el-form-item>
        <el-form-item label="发布人" prop="publisher">
          <el-input v-model="form.publisher" disabled />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input
            v-model="form.content"
            type="textarea"
            :rows="10"
            placeholder="支持 HTML 格式，如 <strong>加粗</strong>、<ul><li>列表</li></ul>"
          />
        </el-form-item>
        <el-form-item label="主页展示">
          <el-switch v-model="form.showOnHome" :active-value="1" :inactive-value="0" />
          <span style="margin-left:10px;font-size:12px;color:#909399">开启后将替换当前主页公告（全局唯一）</span>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </div>
    </el-dialog>

    <!-- 预览弹窗 -->
    <el-dialog title="📢 系统公告（预览）" :visible.sync="previewVisible" width="700px">
      <div v-if="previewRow" class="notice-body">
        <div class="notice-tag">{{ previewRow.title }}</div>
        <div class="notice-content" v-html="previewRow.content" />
        <div class="notice-footer">
          <span>发布人：<strong>{{ previewRow.publisher }}</strong></span>
          <span style="margin-left:24px">{{ previewRow.createdAt }}</span>
        </div>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getAnnouncementList, createAnnouncement, updateAnnouncement, deleteAnnouncement } from '@/api/announcement'
import { mapGetters } from 'vuex'

const defaultForm = (nickname) => ({ id: null, title: '', content: '', publisher: nickname || '', showOnHome: 0 })

export default {
  name: 'AnnouncementPage',
  computed: {
    ...mapGetters(['nickname'])
  },
  data() {
    return {
      loading: false, saving: false,
      list: [],
      dialogVisible: false,
      previewVisible: false,
      previewRow: null,
      form: defaultForm(this.$store.getters.nickname),
      rules: {
        title:   [{ required: true, message: '请输入标题', trigger: 'blur' }],
        content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
      }
    }
  },
  created() { this.loadList() },
  methods: {
    async loadList() {
      this.loading = true
      try { this.list = await getAnnouncementList() }
      finally { this.loading = false }
    },
    openDialog(row) {
      this.form = row ? { ...row } : defaultForm(this.nickname)
      this.dialogVisible = true
    },
    resetForm() { this.$refs.formRef && this.$refs.formRef.resetFields() },
    async submit() {
      try { await this.$refs.formRef.validate() } catch { return }
      this.saving = true
      try {
        if (this.form.id) {
          await updateAnnouncement(this.form.id, this.form)
          this.$message.success('更新成功')
        } else {
          await createAnnouncement(this.form)
          this.$message.success('创建成功')
        }
        this.dialogVisible = false
        this.loadList()
      } finally { this.saving = false }
    },
    doDelete(row) {
      this.$confirm(`确认删除公告「${row.title}」？`, '警告', { type: 'warning' })
        .then(async () => {
          await deleteAnnouncement(row.id)
          this.$message.success('已删除')
          this.loadList()
        }).catch(() => {})
    },
    preview(row) { this.previewRow = row; this.previewVisible = true }
  }
}
</script>

<style scoped>
.danger { color: #F56C6C; }
.notice-body { font-size: 15px; color: #303133; line-height: 1.8; }
.notice-tag {
  display: inline-block;
  background: #fef0f0; color: #F56C6C; border: 1px solid #fde2e2;
  border-radius: 6px; font-size: 14px; padding: 4px 14px; margin-bottom: 16px;
}
.notice-content { margin: 12px 0 20px; }
.notice-footer {
  margin-top: 20px; padding-top: 14px; border-top: 1px solid #f0f0f0;
  font-size: 14px; color: #909399;
}
</style>
