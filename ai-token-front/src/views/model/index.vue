<template>
  <div class="page-card">
    <!-- 筛选 + 操作栏 -->
    <div class="toolbar">
      <el-select v-model="filterChannelId" placeholder="全部渠道" clearable filterable style="width:180px" @change="loadList">
        <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
      </el-select>
      <el-button type="primary" icon="el-icon-plus" @click="openDialog()">新增模型</el-button>
    </div>

    <el-table :data="list" border stripe v-loading="loading">
      <el-table-column prop="id"           label="ID"    width="60" />
      <el-table-column label="所属渠道" min-width="120">
        <template slot-scope="{ row }">
          {{ channelName(row.channelId) }}
        </template>
      </el-table-column>
      <el-table-column prop="modelName"    label="模型名称" min-width="160" />
      <el-table-column prop="alias"        label="别名"  min-width="120" />
      <el-table-column label="输入(/百万T)" min-width="140">
        <template slot-scope="{ row }">¥ {{ row.inputPrice }}</template>
      </el-table-column>
      <el-table-column label="输出(/百万T)" min-width="140">
        <template slot-scope="{ row }">¥ {{ row.outputPrice }}</template>
      </el-table-column>
      <el-table-column label="缓存创建(/百万T)" min-width="140">
        <template slot-scope="{ row }">
          <span v-if="row.cacheCreationPrice != null">¥ {{ row.cacheCreationPrice }}</span>
          <span v-else style="color:#909399">= 输入价</span>
        </template>
      </el-table-column>
      <el-table-column label="缓存命中(/百万T)" min-width="140">
        <template slot-scope="{ row }">
          <span v-if="row.cacheReadPrice != null">¥ {{ row.cacheReadPrice }}</span>
          <span v-else style="color:#909399">= 输入×0.1</span>
        </template>
      </el-table-column>
      <el-table-column prop="maxTokens"    label="Max Tokens" min-width="120" />
      <el-table-column prop="weight"       label="权重" min-width="70" align="center" />
      <el-table-column label="状态" width="80">
        <template slot-scope="{ row }">
          <el-tag :type="row.status === 1 ? 'success' : 'info'" size="mini">
            {{ row.status === 1 ? '启用' : '禁用' }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" min-width="220" fixed="right">
        <template slot-scope="{ row }">
          <el-button type="text" @click="openDialog(row)">编辑</el-button>
          <el-button type="text" class="danger" @click="doDelete(row)">删除</el-button>
        </template>
      </el-table-column>
    </el-table>

    <!-- 弹窗 -->
    <el-dialog
      :title="form.id ? '编辑模型' : '新增模型'"
      :visible.sync="dialogVisible"
      width="1200px"
      @close="resetForm"
    >
      <el-form :model="form" :rules="rules" ref="formRef" label-width="100px">
        <el-form-item label="所属渠道" prop="channelId">
          <el-select v-model="form.channelId" filterable style="width:100%">
            <el-option v-for="c in channels" :key="c.id" :label="c.name" :value="c.id" />
          </el-select>
        </el-form-item>
        <el-form-item label="模型名称" prop="modelName">
          <el-input v-model="form.modelName" placeholder="如 gpt-4o" />
        </el-form-item>
        <el-form-item label="别名">
          <el-input v-model="form.alias" placeholder="用户请求时的名称（可选）" />
        </el-form-item>
        <el-form-item label="输入单价">
          <el-input-number v-model="form.inputPrice"  :min="0" :precision="6" :step="0.1" style="width:100%" />
          <span class="form-tip">元 / 百万 Token</span>
        </el-form-item>
        <el-form-item label="输出单价">
          <el-input-number v-model="form.outputPrice" :min="0" :precision="6" :step="0.1" style="width:100%" />
          <span class="form-tip">元 / 百万 Token</span>
        </el-form-item>
        <el-form-item label="缓存创建单价">
          <el-input-number v-model="form.cacheCreationPrice" :min="0" :precision="6" :step="0.001" style="width:100%" />
          <span class="form-tip">留空 = 等于输入单价（Anthropic 默认）</span>
        </el-form-item>
        <el-form-item label="缓存命中单价">
          <el-input-number v-model="form.cacheReadPrice" :min="0" :precision="6" :step="0.001" style="width:100%" />
          <span class="form-tip">留空 = 输入单价 × 0.1（Anthropic 默认）；OpenAI 通常为 × 0.5</span>
        </el-form-item>
        <el-form-item label="Max Tokens">
          <el-input-number v-model="form.maxTokens" :min="1" :step="1024" style="width:100%" />
        </el-form-item>
        <el-form-item label="权重">
          <el-input-number v-model="form.weight" :min="1" :max="100" style="width:100%" />
          <div class="form-tip">同一模型名有多条记录时，按权重比例分配流量，例如 3:1 表示 75% 走该渠道</div>
        </el-form-item>
      </el-form>
      <div slot="footer">
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="submit">确定</el-button>
      </div>
    </el-dialog>
  </div>
</template>

<script>
import { getChannelList } from '@/api/channel'
import { getModelList, createModel, updateModel, deleteModel } from '@/api/model'

const defaultForm = () => ({
  id: null, channelId: null, modelName: '', alias: '',
  inputPrice: 0, outputPrice: 0, cacheCreationPrice: null, cacheReadPrice: null,
  maxTokens: 4096, weight: 1
})

export default {
  name: 'ModelPage',
  data() {
    return {
      loading: false, saving: false,
      list: [], channels: [],
      filterChannelId: null,
      dialogVisible: false,
      form: defaultForm(),
      rules: {
        channelId: [{ required: true, message: '请选择渠道' }],
        modelName: [{ required: true, message: '请输入模型名称' }]
      }
    }
  },
  async created() {
    try {
      this.channels = await getChannelList()
    } catch (e) {
      // 渠道加载失败不影响模型列表展示
    }
    this.loadList()
  },
  methods: {
    channelName(channelId) {
      const c = this.channels.find(c => c.id === channelId)
      return c ? c.name : '-'
    },
    async loadList() {
      this.loading = true
      try {
        this.list = await getModelList(this.filterChannelId)
      } finally {
        this.loading = false
      }
    },
    openDialog(row) {
      this.form = row ? { ...row } : defaultForm()
      this.dialogVisible = true
    },
    resetForm() { this.$refs.formRef && this.$refs.formRef.resetFields() },
    async submit() {
      try { await this.$refs.formRef.validate() } catch { return }
      this.saving = true
      try {
        if (this.form.id) {
          await updateModel(this.form.id, this.form)
          this.$message.success('更新成功')
        } else {
          await createModel(this.form)
          this.$message.success('创建成功')
        }
        this.dialogVisible = false
        this.loadList()
      } finally {
        this.saving = false
      }
    },
    doDelete(row) {
      this.$confirm(`确认删除模型「${row.modelName}」？`, '警告', { type: 'warning' }).then(async () => {
        await deleteModel(row.id)
        this.$message.success('删除成功')
        this.loadList()
      }).catch(() => {})
    }
  }
}
</script>

<style scoped>
.form-tip { font-size: 12px; color: #909399; margin-left: 6px; }
</style>
