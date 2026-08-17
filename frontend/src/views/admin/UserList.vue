<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索用户名 / 姓名" clearable style="width: 200px" @keyup.enter="load" @clear="load">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.role" placeholder="角色" clearable style="width: 130px" @change="load">
        <el-option v-for="(label, key) in ROLE_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>新增成员</el-button>
    </div>

    <el-table v-loading="loading" :data="items">
      <el-table-column prop="realName" label="姓名" width="110" />
      <el-table-column prop="username" label="用户名" width="120" />
      <el-table-column label="角色" width="110">
        <template #default="{ row }">
          <el-tag size="small" :type="row.role === 'ADMIN' ? 'danger' : row.role === 'PARTNER' ? 'warning' : row.role === 'LAWYER' ? 'primary' : 'info'">
            {{ roleLabel(row.role) }}
          </el-tag>
        </template>
      </el-table-column>
      <el-table-column prop="department" label="部门" width="120" />
      <el-table-column prop="title" label="职务" width="120" />
      <el-table-column prop="phone" label="电话" width="130" />
      <el-table-column prop="email" label="邮箱" min-width="160" show-overflow-tooltip />
      <el-table-column label="状态" width="80">
        <template #default="{ row }">
          <el-tag :type="row.enabled ? 'success' : 'info'" size="small">{{ row.enabled ? '启用' : '停用' }}</el-tag>
        </template>
      </el-table-column>
      <el-table-column label="操作" width="200" fixed="right">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="openEdit(row)">编辑</el-button>
          <el-button link type="warning" size="small" @click="openReset(row)">重置密码</el-button>
          <el-button v-if="row.enabled" link type="danger" size="small" @click="toggleStatus(row, false)">停用</el-button>
          <el-button v-else link type="success" size="small" @click="toggleStatus(row, true)">启用</el-button>
        </template>
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

    <el-dialog v-model="dialogVisible" :title="editing ? '编辑成员' : '新增成员'" width="520px">
      <el-form ref="formRef" :model="form" :rules="rules" label-width="90px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="用户名" prop="username">
              <el-input v-model="form.username" :disabled="!!editing" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item v-if="!editing" label="初始密码" prop="password">
              <el-input v-model="form.password" type="password" show-password />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="姓名" prop="realName">
              <el-input v-model="form.realName" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="角色" prop="role">
              <el-select v-model="form.role" style="width: 100%">
                <el-option v-for="(label, key) in ROLE_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="部门">
              <el-input v-model="form.department" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="职务">
              <el-input v-model="form.title" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电话">
              <el-input v-model="form.phone" />
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="邮箱">
              <el-input v-model="form.email" />
            </el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="dialogVisible = false">取消</el-button>
        <el-button type="primary" :loading="saving" @click="save">保存</el-button>
      </template>
    </el-dialog>

    <el-dialog v-model="resetVisible" title="重置密码" width="400px">
      <el-form :model="resetForm" label-width="90px">
        <el-form-item label="新密码">
          <el-input v-model="resetForm.newPassword" type="password" show-password placeholder="至少 6 位" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="resetVisible = false">取消</el-button>
        <el-button type="primary" @click="doReset">确定</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createUser, pageUsers, resetPassword, setUserStatus, updateUser } from '@/api/user'
import { ROLE_MAP, roleLabel } from '@/utils/dict'

const loading = ref(false)
const items = ref([])
const total = ref(0)
const query = reactive({ keyword: '', role: '', page: 1, size: 10 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    const data = await pageUsers(params)
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
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入初始密码', trigger: 'blur' }],
  realName: [{ required: true, message: '请输入姓名', trigger: 'blur' }],
  role: [{ required: true, message: '请选择角色', trigger: 'change' }]
}

function openCreate() {
  editing.value = null
  form.value = { role: 'LAWYER' }
  dialogVisible.value = true
}

function openEdit(row) {
  editing.value = row
  form.value = {
    username: row.username,
    realName: row.realName,
    role: row.role,
    department: row.department,
    title: row.title,
    phone: row.phone,
    email: row.email
  }
  dialogVisible.value = true
}

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (editing.value) {
      await updateUser(editing.value.id, form.value)
    } else {
      await createUser(form.value)
    }
    ElMessage.success('保存成功')
    dialogVisible.value = false
    load()
  } finally {
    saving.value = false
  }
}

async function toggleStatus(row, enabled) {
  await setUserStatus(row.id, enabled)
  ElMessage.success(enabled ? '已启用' : '已停用')
  load()
}

const resetVisible = ref(false)
const resetTarget = ref(null)
const resetForm = ref({ newPassword: '' })

function openReset(row) {
  resetTarget.value = row
  resetForm.value = { newPassword: '' }
  resetVisible.value = true
}

async function doReset() {
  if (!resetForm.value.newPassword || resetForm.value.newPassword.length < 6) {
    ElMessage.warning('新密码至少 6 位')
    return
  }
  await resetPassword(resetTarget.value.id, resetForm.value)
  ElMessage.success('密码已重置')
  resetVisible.value = false
}

onMounted(load)
</script>
