<template>
  <el-dialog
    :model-value="true"
    :title="caseRow ? '编辑案件' : '立案'"
    width="640px"
    destroy-on-close
    @close="$emit('close')"
  >
    <el-form ref="formRef" :model="form" :rules="rules" label-width="100px">
      <el-row :gutter="12">
        <el-col :span="12">
          <el-form-item label="客户" prop="clientId">
            <el-select v-model="form.clientId" filterable placeholder="选择客户" style="width: 100%">
              <el-option v-for="c in clients" :key="c.id" :label="c.name" :value="c.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="案件类型" prop="type">
            <el-select v-model="form.type" style="width: 100%">
              <el-option v-for="(label, key) in CASE_TYPE_MAP" :key="key" :label="label" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="主办律师" prop="leadLawyerId">
            <el-select v-model="form.leadLawyerId" style="width: 100%">
              <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="协办律师">
            <el-select v-model="form.coLawyerIds" multiple style="width: 100%">
              <el-option v-for="u in users" :key="u.id" :label="u.realName" :value="u.id" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="优先级">
            <el-select v-model="form.priority" style="width: 100%">
              <el-option v-for="(label, key) in PRIORITY_MAP" :key="key" :label="label" :value="key" />
            </el-select>
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="立案日期">
            <el-date-picker v-model="form.filingDate" type="date" value-format="YYYY-MM-DD" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="受理法院">
            <el-input v-model="form.court" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="标的额(元)">
            <el-input-number v-model="form.caseAmount" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="12">
          <el-form-item label="收费金额(元)">
            <el-input-number v-model="form.fee" :min="0" :precision="2" style="width: 100%" />
          </el-form-item>
        </el-col>
        <el-col :span="24">
          <el-form-item label="案情摘要">
            <el-input v-model="form.description" type="textarea" :rows="3" maxlength="2000" show-word-limit />
          </el-form-item>
        </el-col>
      </el-row>
    </el-form>
    <template #footer>
      <el-button @click="$emit('close')">取消</el-button>
      <el-button type="primary" :loading="saving" @click="save">保存</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage } from 'element-plus'
import { createCase, updateCase } from '@/api/case'
import { pageClients } from '@/api/client'
import { userOptions } from '@/api/user'
import { CASE_TYPE_MAP, PRIORITY_MAP } from '@/utils/dict'

const props = defineProps({
  caseRow: { type: Object, default: null }
})
const emit = defineEmits(['close', 'saved'])

const users = ref([])
const clients = ref([])
const saving = ref(false)
const formRef = ref()
const form = reactive({
  clientId: null,
  title: '',
  type: 'CIVIL',
  priority: 'MEDIUM',
  leadLawyerId: null,
  coLawyerIds: [],
  court: '',
  caseAmount: null,
  filingDate: '',
  description: '',
  fee: null
})

const rules = {
  clientId: [{ required: true, message: '请选择客户', trigger: 'change' }],
  title: [{ required: true, message: '请输入案件名称', trigger: 'blur' }],
  type: [{ required: true, message: '请选择案件类型', trigger: 'change' }],
  leadLawyerId: [{ required: true, message: '请选择主办律师', trigger: 'change' }]
}

onMounted(async () => {
  const [u, c] = await Promise.all([userOptions(), pageClients({ page: 1, size: 100 })])
  users.value = u
  clients.value = c.items
  if (props.caseRow) {
    Object.assign(form, {
      clientId: props.caseRow.clientId,
      title: props.caseRow.title,
      type: props.caseRow.type,
      priority: props.caseRow.priority,
      leadLawyerId: props.caseRow.leadLawyerId,
      coLawyerIds: [...(props.caseRow.coLawyerIds || [])],
      court: props.caseRow.court,
      caseAmount: props.caseRow.caseAmount,
      filingDate: props.caseRow.filingDate,
      description: props.caseRow.description,
      fee: props.caseRow.fee
    })
  }
})

async function save() {
  await formRef.value.validate()
  saving.value = true
  try {
    if (props.caseRow) {
      await updateCase(props.caseRow.id, { ...form })
    } else {
      await createCase({ ...form })
    }
    ElMessage.success('保存成功')
    emit('saved')
  } finally {
    saving.value = false
  }
}
</script>
