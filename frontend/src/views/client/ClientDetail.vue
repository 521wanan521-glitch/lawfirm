<template>
  <div v-loading="loading">
    <el-page-header @back="$router.back()" style="margin-bottom: 16px">
      <template #content>
        <b>{{ detail.name }}</b>
        <el-tag size="small" style="margin-left: 8px" :type="detail.level === 'A' ? 'danger' : detail.level === 'B' ? 'warning' : 'info'">
          {{ CLIENT_LEVEL_MAP[detail.level] }}
        </el-tag>
      </template>
      <template #extra>
        <el-button size="small" @click="$router.push('/cases')">查看相关案件</el-button>
        <el-button size="small" type="primary" plain @click="openEdit">编辑</el-button>
      </template>
    </el-page-header>

    <el-row :gutter="16">
      <el-col :span="12">
        <el-card shadow="never">
          <template #header>基本信息</template>
          <el-descriptions :column="1" border>
            <el-descriptions-item label="客户名称">{{ detail.name }}</el-descriptions-item>
            <el-descriptions-item label="客户类型">{{ CLIENT_TYPE_MAP[detail.type] }}</el-descriptions-item>
            <el-descriptions-item label="证件号/信用代码">{{ detail.idNumber || '-' }}</el-descriptions-item>
            <el-descriptions-item label="行业">{{ detail.industry || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系电话">{{ detail.phone || '-' }}</el-descriptions-item>
            <el-descriptions-item label="电子邮箱">{{ detail.email || '-' }}</el-descriptions-item>
            <el-descriptions-item label="客户来源">{{ detail.source || '-' }}</el-descriptions-item>
            <el-descriptions-item label="负责人">{{ detail.ownerName || '-' }}</el-descriptions-item>
            <el-descriptions-item label="联系地址">{{ detail.address || '-' }}</el-descriptions-item>
            <el-descriptions-item label="备注">{{ detail.remark || '-' }}</el-descriptions-item>
          </el-descriptions>
        </el-card>

        <el-card shadow="never" style="margin-top: 16px">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>联系人（{{ contacts.length }}）</span>
              <el-button size="small" type="primary" @click="openContact">添加联系人</el-button>
            </div>
          </template>
          <el-table :data="contacts" size="small">
            <el-table-column prop="name" label="姓名" width="100" />
            <el-table-column prop="position" label="职务" width="110" show-overflow-tooltip />
            <el-table-column prop="phone" label="电话" width="130" />
            <el-table-column prop="email" label="邮箱" show-overflow-tooltip />
            <el-table-column label="主要" width="70">
              <template #default="{ row }">
                <el-tag v-if="row.primaryContact" size="small" type="success">主要</el-tag>
              </template>
            </el-table-column>
            <el-table-column label="操作" width="90">
              <template #default="{ row }">
                <el-button link type="primary" size="small" @click="openContact(row)">编辑</el-button>
                <el-button link type="danger" size="small" @click="removeContact(row)">删除</el-button>
              </template>
            </el-table-column>
          </el-table>
        </el-card>
      </el-col>

      <el-col :span="12">
        <el-card shadow="never">
          <template #header>
            <div style="display: flex; justify-content: space-between; align-items: center">
              <span>跟进记录</span>
              <el-button size="small" type="primary" @click="interactionVisible = true">添加跟进</el-button>
            </div>
          </template>
          <el-timeline v-if="interactions.length">
            <el-timeline-item
              v-for="i in interactions"
              :key="i.id"
              :timestamp="formatDateTime(i.createdAt)"
              placement="top"
            >
              <div>
                <el-tag size="small" style="margin-right: 6px">{{ INTERACTION_TYPE_MAP[i.type] }}</el-tag>
                {{ i.content }}
              </div>
              <div class="text-muted">
                {{ i.userName }}
                <span v-if="i.nextFollowDate"> · 下次跟进：{{ formatDate(i.nextFollowDate) }}</span>
              </div>
            </el-timeline-item>
          </el-timeline>
          <el-empty v-else description="暂无跟进记录" :image-size="60" />
          <div class="pagination-wrap">
            <el-pagination
              v-model:current-page="interactionQuery.page"
              :page-size="interactionQuery.size"
              :total="interactionTotal"
              layout="total, prev, pager, next"
              small
              @current-change="loadInteractions"
            />
          </div>
        </el-card>
      </el-col>
    </el-row>

    <!-- 编辑客户 -->
    <el-dialog v-model="editVisible" title="编辑客户" width="560px">
      <el-form ref="editFormRef" :model="editForm" :rules="rules" label-width="110px">
        <el-row :gutter="12">
          <el-col :span="12">
            <el-form-item label="客户名称" prop="name"><el-input v-model="editForm.name" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户类型" prop="type">
              <el-select v-model="editForm.type" style="width: 100%">
                <el-option v-for="(label, key) in CLIENT_TYPE_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="证件号/信用代码"><el-input v-model="editForm.idNumber" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="所属行业"><el-input v-model="editForm.industry" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="联系电话"><el-input v-model="editForm.phone" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="电子邮箱"><el-input v-model="editForm.email" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户等级">
              <el-select v-model="editForm.level" style="width: 100%">
                <el-option v-for="(label, key) in CLIENT_LEVEL_MAP" :key="key" :label="label" :value="key" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="客户来源"><el-input v-model="editForm.source" /></el-form-item>
          </el-col>
          <el-col :span="12">
            <el-form-item label="负责人">
              <el-select v-model="editForm.ownerId" clearable filterable style="width: 100%">
                <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
              </el-select>
            </el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="联系地址"><el-input v-model="editForm.address" /></el-form-item>
          </el-col>
          <el-col :span="24">
            <el-form-item label="备注"><el-input v-model="editForm.remark" type="textarea" :rows="2" /></el-form-item>
          </el-col>
        </el-row>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="saveEdit">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加/编辑联系人 -->
    <el-dialog v-model="contactVisible" :title="contactForm.id ? '编辑联系人' : '添加联系人'" width="440px">
      <el-form ref="contactFormRef" :model="contactForm" :rules="contactRules" label-width="80px">
        <el-form-item label="姓名" prop="name"><el-input v-model="contactForm.name" /></el-form-item>
        <el-form-item label="职务"><el-input v-model="contactForm.position" /></el-form-item>
        <el-form-item label="电话"><el-input v-model="contactForm.phone" /></el-form-item>
        <el-form-item label="邮箱"><el-input v-model="contactForm.email" /></el-form-item>
        <el-form-item label="主要联系人">
          <el-switch v-model="contactForm.primaryContact" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="contactVisible = false">取消</el-button>
        <el-button type="primary" @click="saveContact">保存</el-button>
      </template>
    </el-dialog>

    <!-- 添加跟进 -->
    <el-dialog v-model="interactionVisible" title="添加跟进记录" width="460px">
      <el-form ref="interactionFormRef" :model="interactionForm" :rules="interactionRules" label-width="90px">
        <el-form-item label="跟进方式" prop="type">
          <el-select v-model="interactionForm.type" style="width: 100%">
            <el-option v-for="(label, key) in INTERACTION_TYPE_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="跟进内容" prop="content">
          <el-input v-model="interactionForm.content" type="textarea" :rows="4" maxlength="2000" show-word-limit />
        </el-form-item>
        <el-form-item label="下次跟进">
          <el-date-picker v-model="interactionForm.nextFollowDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="interactionVisible = false">取消</el-button>
        <el-button type="primary" @click="saveInteraction">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { useRoute } from 'vue-router'
import { ElMessage, ElMessageBox } from 'element-plus'
import {
  addContact, addInteraction, deleteContact, getClient, getContacts, pageInteractions,
  updateClient, updateContact
} from '@/api/client'
import { userOptions } from '@/api/user'
import { CLIENT_LEVEL_MAP, CLIENT_TYPE_MAP, INTERACTION_TYPE_MAP } from '@/utils/dict'
import { formatDate, formatDateTime } from '@/utils/format'

const route = useRoute()
const clientId = route.params.id

const loading = ref(false)
const detail = ref({})
const contacts = ref([])
const interactions = ref([])
const interactionTotal = ref(0)
const interactionQuery = reactive({ page: 1, size: 10 })
const users = ref([])

async function load() {
  loading.value = true
  try {
    detail.value = await getClient(clientId)
    contacts.value = await getContacts(clientId)
  } finally {
    loading.value = false
  }
}

async function loadInteractions() {
  const data = await pageInteractions(clientId, interactionQuery)
  interactions.value = data.items
  interactionTotal.value = data.total
}

// 编辑客户
const editVisible = ref(false)
const editFormRef = ref()
const editForm = ref({})
const rules = {
  name: [{ required: true, message: '请输入客户名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择类型', trigger: 'change' }]
}

function openEdit() {
  editForm.value = { ...detail.value }
  editVisible.value = true
}

async function saveEdit() {
  await editFormRef.value.validate()
  await updateClient(clientId, editForm.value)
  ElMessage.success('已保存')
  editVisible.value = false
  load()
}

// 联系人
const contactVisible = ref(false)
const contactFormRef = ref()
const contactForm = ref({})
const contactRules = {
  name: [{ required: true, message: '请输入姓名', trigger: 'blur' }]
}

function openContact(row) {
  contactForm.value = row
    ? { id: row.id, name: row.name, position: row.position, phone: row.phone, email: row.email, primaryContact: row.primaryContact }
    : { primaryContact: false }
  contactVisible.value = true
}

async function saveContact() {
  await contactFormRef.value.validate()
  if (contactForm.value.id) {
    await updateContact(clientId, contactForm.value.id, contactForm.value)
  } else {
    await addContact(clientId, contactForm.value)
  }
  ElMessage.success('已保存')
  contactVisible.value = false
  load()
}

async function removeContact(row) {
  await ElMessageBox.confirm(`确定删除联系人「${row.name}」？`, '提示', { type: 'warning' })
  await deleteContact(clientId, row.id)
  ElMessage.success('已删除')
  load()
}

// 跟进记录
const interactionVisible = ref(false)
const interactionFormRef = ref()
const interactionForm = ref({})
const interactionRules = {
  type: [{ required: true, message: '请选择跟进方式', trigger: 'change' }],
  content: [{ required: true, message: '请输入跟进内容', trigger: 'blur' }]
}

async function saveInteraction() {
  await interactionFormRef.value.validate()
  await addInteraction(clientId, interactionForm.value)
  ElMessage.success('已记录')
  interactionVisible.value = false
  interactionForm.value = {}
  loadInteractions()
}

onMounted(async () => {
  load()
  loadInteractions()
  users.value = await userOptions()
})
</script>
