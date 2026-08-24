<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input
        v-model="keyword"
        placeholder="搜索顾问单位名称 / 电话"
        clearable
        style="width: 260px"
        @keyup.enter="load"
        @clear="load"
      >
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="goCreate"><el-icon><Plus /></el-icon>新增顾问单位</el-button>
    </div>

    <el-row :gutter="16">
      <el-col v-for="c in items" :key="c.id" :xs="24" :sm="12" :md="8">
        <el-card shadow="hover" class="consultant-card" @click="$router.push(`/clients/${c.id}`)">
          <div class="card-head">
            <span class="name text-ellipsis">{{ c.name }}</span>
            <el-tag size="small" type="warning" effect="dark">VIP</el-tag>
          </div>
          <div class="card-body">
            <div class="row"><span class="k">类型</span><span class="v">{{ CLIENT_TYPE_MAP[c.type] }}</span></div>
            <div class="row"><span class="k">行业</span><span class="v text-ellipsis">{{ c.industry || '-' }}</span></div>
            <div class="row"><span class="k">负责人</span><span class="v">{{ c.ownerName || '-' }}</span></div>
            <div class="row"><span class="k">电话</span><span class="v">{{ c.phone || '-' }}</span></div>
            <div class="row"><span class="k">邮箱</span><span class="v text-ellipsis">{{ c.email || '-' }}</span></div>
            <div class="row"><span class="k">地址</span><span class="v text-ellipsis">{{ c.address || '-' }}</span></div>
            <div class="row"><span class="k">等级</span><span class="v">{{ CLIENT_LEVEL_MAP[c.level] }}</span></div>
            <div class="row"><span class="k">案件数</span><span class="v">{{ c.caseCount }}</span></div>
          </div>
          <div v-if="c.remark" class="card-remark">{{ c.remark }}</div>
        </el-card>
      </el-col>
    </el-row>

    <div v-if="!items.length && !loading" class="empty-tip">
      暂无法律顾问单位。可到「客户管理」新增客户时，打开「法律顾问单位」开关即可加入此处。
    </div>

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="page"
        v-model:page-size="size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </div>
  </div>
</template>

<script setup>
import { onMounted, ref } from 'vue'
import { useRouter } from 'vue-router'
import { pageClients } from '@/api/client'
import { CLIENT_LEVEL_MAP, CLIENT_TYPE_MAP } from '@/utils/dict'

const router = useRouter()
const items = ref([])
const total = ref(0)
const keyword = ref('')
const page = ref(1)
const size = ref(12)
const loading = ref(false)

async function load() {
  loading.value = true
  try {
    const params = { consultant: true, page: page.value, size: size.value }
    if (keyword.value) params.keyword = keyword.value
    const data = await pageClients(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function goCreate() {
  router.push('/clients')
}

onMounted(load)
</script>

<style scoped>
.consultant-card {
  margin-bottom: 16px;
  cursor: pointer;
  border-top: 3px solid #e6a23c;
}

.consultant-card .card-head {
  display: flex;
  align-items: center;
  justify-content: space-between;
  margin-bottom: 12px;
}

.consultant-card .name {
  font-size: 16px;
  font-weight: 600;
  color: #303133;
  max-width: 220px;
}

.card-body .row {
  display: flex;
  padding: 4px 0;
  font-size: 13px;
}

.card-body .k {
  width: 56px;
  color: #909399;
  flex-shrink: 0;
}

.card-body .v {
  color: #606266;
  flex: 1;
  min-width: 0;
}

.card-remark {
  margin-top: 10px;
  padding-top: 10px;
  border-top: 1px dashed #ebeef5;
  font-size: 12px;
  color: #909399;
  display: -webkit-box;
  -webkit-box-orient: vertical;
  -webkit-line-clamp: 2;
  overflow: hidden;
}

.empty-tip {
  text-align: center;
  color: #909399;
  padding: 60px 0;
  font-size: 14px;
}
</style>
