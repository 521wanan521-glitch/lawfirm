<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="query.keyword"
        placeholder="搜索名称 / 电话 / 证件号"
        clearable
        style="width: 220px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.type" placeholder="类型" clearable style="width: 110px" @change="load">
        <el-option v-for="(label, key) in CLIENT_TYPE_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.level" placeholder="等级" clearable style="width: 110px" @change="load">
        <el-option v-for="(label, key) in CLIENT_LEVEL_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-select v-model="query.ownerId" placeholder="负责人" clearable filterable style="width: 130px" @change="load">
        <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>新增客户</el-button>
    </div>

    <el-table v-loading="loading" :data="items" @row-click="(row) => $router.push(`/clients/${row.id}`)" style="cursor: pointer">
      <el-table-column prop="name" label="客户名称" min-width="180" show-overflow-tooltip />
      <el-table-column label="类型" width="80">
        <template #default="{ row }">
          <el-tag size="small" :type="row.type === 'COMPANY' ? 'primary' : 'success'">{{ CLIENT_TYPE_MAP[row.type] }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="等级" width="90">
        <template #default="{ row }">
          <el-tag size="small" :type="row.level === 'A' ? 'danger' : row.level === 'B' ? 'warning' : 'info'">
            {{ CLIENT_LEVEL_MAP[row.level] }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column label="顾问单位" width="90" align="center">
        <template #default="{ row }">
          <el-tag v-if="row.consultant" size="small" type="warning" effect="dark">VIP</el-tag>
          <span v-else class="text-muted">-</span>
        </template>
      </el-table-column>
      <el-table-column prop="industry" label="行业" width="110" show-overflow-tooltip />
      <el-table-column prop="phone" label="联系电话" width="130" />
      <el-table-column prop="ownerName" label="负责人" width="100" />
      <el-table-column label="联系人" width="80" align="center">
        <template #default="{ row }">{{ row.contactCount }}</template>
      </el-table-column>
      <el-table-column label="案件数" width="80" align="center">
        <template #default="{ row }">{{ row.caseCount }}</template>
      </el-table-column>
      <el-table-column label="创建时间" width="150">
        <template #default="{ row }">{{ formatDate(row.createdAt) }}</template>
      </el-table-column>
    </el-table>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[10, 20, 50]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </div>

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑客户' : '新增客户'" width="560px" destroy-on-close>
      <el-form ref="formRef" :model="form" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name">
              <el-input v-model="form.name" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户类型" prop="type">
              <el-select v-model="form.type" style="width: 100%">
                <el-option v-for="(label, key) in CLIENT_TYPE_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号/信用代码">
              <el-input v-model="form.idNumber" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业">
              <el-input v-model="form.industry" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户等级">
              <el-select v-model="form.level" style="width: 100%">
                <el-option v-for="(label, key) in CLIENT_LEVEL_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户来源">
              <el-input v-model="form.source" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="form.ownerId" clearable filterable style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="法律顾问单位">
              <el-switch v-model="form.consultant" active-text="是" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系地址">
              <el-input v-model="form.address" />
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注">
              <el-input v-model="form.remark" type="textarea" :rows="2" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createClient, pageClients, updateClient } from '@/api/client'
import { userOptions } from '@/api/user'
import { CLIENT_LEVEL_MAP, CLIENT_TYPE_MAP } from '@/utils/dict'
import { formatDate } from '@/utils/format'

const loading = ref(false)
const items = ref([])
const total = ref(0)
const users = ref([])
const query = reactive({ keyword: '', type: '', level: '', ownerId: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    const data = await pageClients(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

const dialogVisible = ref(false)
const editing = ref(null)
const saving = ref(false)
const formRef = ref()
const form = ref({})

const rules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function openCreate() {
  editing.value = null
  form.value = { type: 'COMPANY', level: 'C', consultant: false }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateClient(editing.value.id, form.value)
    } else {
      await createClient(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

onMounted(async () => {
  load()
  users.value = await userOptions()
})
</script>
