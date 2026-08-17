<template>
  <div class="page-card">
    <div class="toolbar">
      <el-input v-model="query.keyword" placeholder="搜索标题 / 标签" clearable style="width: 220px" @keyup.enter="load" @clear="load">
        <template #prefix><el-icon><Search /></el-icon></template>
      </el-input>
      <el-select v-model="query.category" placeholder="分类" clearable style="width: 130px" @change="load">
        <el-option v-for="(label, key) in KNOWLEDGE_CATEGORY_MAP" :key="key" :label="label" :value="key" />
      </el-select>
      <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
      <div class="spacer" />
      <el-button type="success" @click="openCreate"><el-icon><Plus /></el-icon>发布文章</el-button>
    </div>

    <el-row :gutter="12">
      <el-col v-for="article in items" :key="article.id" :xs="24" :sm="12" :md="8" :lg="6">
        <el-card shadow="hover" class="article-card" @click="openDetail(article)">
          <div class="article-title">{{ article.title }}</div>
          <div class="article-meta">
            <el-tag size="small" type="info">{{ knowledgeCategoryLabel(article.category) }}</el-tag>
            <span v-if="!article.published" style="margin-left: 6px">
              <el-tag size="small" type="warning">草稿</el-tag>
            </span>
          </div>
          <div class="article-desc">{{ stripHtml(article.content) }}</div>
          <div class="article-foot">
            <span>{{ article.authorName }}</span>
            <span>{{ formatDate(article.createdAt) }} · {{ article.viewCount }} 阅读</span>
          </div>
        </el-card>
      </el-col>
    </el-row>
    <el-empty v-if="!loading && !items.length" description="暂无文章" />

    <div class="pagination-wrap">
      <el-pagination
        v-model:current-page="query.page"
        v-model:page-size="query.size"
        :total="total"
        :page-sizes="[12, 24, 48]"
        layout="total, sizes, prev, pager, next"
        @change="load"
      />
    </div>

    <!-- 详情 -->
    <el-dialog v-model="detailVisible" width="720px" top="5vh">
      <template v-if="current">
        <h2 style="margin-top: 0">{{ current.title }}</h2>
        <div class="text-muted" style="margin-bottom: 12px">
          {{ current.authorName }} · {{ formatDate(current.createdAt) }} ·
          <el-tag size="small" type="info">{{ knowledgeCategoryLabel(current.category) }}</el-tag>
          <el-tag v-if="current.tags" size="small" style="margin-left: 6px">{{ current.tags }}</el-tag>
        </div>
        <div class="article-content">{{ current.content }}</div>
        <div class="text-muted" style="margin-top: 12px">阅读 {{ current.viewCount }} 次</div>
      </template>
      <template #footer>
        <el-button v-if="canEdit(current)" type="primary" plain @click="openEdit">编辑</el-button>
        <el-button v-if="canEdit(current)" type="danger" plain @click="remove">删除</el-button>
        <el-button @click="detailVisible = false">关闭</el-button>
      </template>
    </el-dialog>

    <!-- 编辑/发布 -->
    <el-dialog v-model="editVisible" :title="editing ? '编辑文章' : '发布文章'" width="720px" top="5vh">
      <el-form ref="editFormRef" :model="editForm" :rules="editRules" label-width="80px">
        <el-form-item label="标题" prop="title">
          <el-input v-model="editForm.title" maxlength="200" />
        </el-form-item>
        <el-form-item label="分类" prop="category">
          <el-select v-model="editForm.category" style="width: 200px">
            <el-option v-for="(label, key) in KNOWLEDGE_CATEGORY_MAP" :key="key" :label="label" :value="key" />
          </el-select>
        </el-form-item>
        <el-form-item label="标签">
          <el-input v-model="editForm.tags" placeholder="多个标签用逗号分隔" />
        </el-form-item>
        <el-form-item label="内容" prop="content">
          <el-input v-model="editForm.content" type="textarea" :rows="12" maxlength="20000" show-word-limit />
        </el-form-item>
        <el-form-item label="发布">
          <el-switch v-model="editForm.published" active-text="立即发布" inactive-text="存为草稿" />
        </el-form-item>
      </el-form>
      <template #footer>
        <el-button @click="editVisible = false">取消</el-button>
        <el-button type="primary" @click="save">保存</el-button>
      </template>
    </el-dialog>
  </div>
</template>

<script setup>
import { onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { createArticle, deleteArticle, getArticle, pageArticles, updateArticle } from '@/api/knowledge'
import { useUserStore } from '@/store/user'
import { KNOWLEDGE_CATEGORY_MAP, knowledgeCategoryLabel } from '@/utils/dict'
import { formatDate } from '@/utils/format'

const store = useUserStore()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const query = reactive({ keyword: '', category: '', page: 1, size: 12 })

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    const data = await pageArticles(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function stripHtml(text) {
  return (text || '').replace(/<[^>]+>/g, '').replace(/\n/g, ' ').slice(0, 80)
}

const detailVisible = ref(false)
const current = ref(null)

async function openDetail(article) {
  const data = await getArticle(article.id)
  current.value = data
  detailVisible.value = true
  if (items.value.find((i) => i.id === article.id)) {
    article.viewCount = data.viewCount
  }
}

function canEdit(article) {
  return article && (article.authorId === store.user?.id || store.isAdmin)
}

const editVisible = ref(false)
const editing = ref(null)
const editFormRef = ref()
const editForm = ref({})
const editRules = {
  title: [{ required: true, message: '请输入标题', trigger: 'blur' }],
  category: [{ required: true, message: '请选择分类', trigger: 'change' }],
  content: [{ required: true, message: '请输入内容', trigger: 'blur' }]
}

function openCreate() {
  editing.value = null
  editForm.value = { category: 'EXPERIENCE', published: true }
  editVisible.value = true
}

function openEdit() {
  editing.value = current.value
  editForm.value = {
    title: current.value.title,
    category: current.value.category,
    content: current.value.content,
    tags: current.value.tags,
    published: current.value.published
  }
  editVisible.value = true
}

async function save() {
  await editFormRef.value.validate()
  if (editing.value) {
    await updateArticle(editing.value.id, editForm.value)
  } else {
    await createArticle(editForm.value)
  }
  ElMessage.success('已保存')
  editVisible.value = false
  detailVisible.value = false
  load()
}

async function remove() {
  await ElMessageBox.confirm('确定删除该文章？', '提示', { type: 'warning' })
  await deleteArticle(current.value.id)
  ElMessage.success('已删除')
  detailVisible.value = false
  load()
}

onMounted(load)
</script>

<style scoped>
.article-card {
  margin-bottom: 12px;
  cursor: pointer;
}

.article-title {
  font-size: 15px;
  font-weight: 600;
  margin-bottom: 8px;
  overflow: hidden;
  text-overflow: ellipsis;
  white-space: nowrap;
}

.article-desc {
  font-size: 13px;
  color: #909399;
  height: 40px;
  overflow: hidden;
  margin: 8px 0;
}

.article-foot {
  display: flex;
  justify-content: space-between;
  font-size: 12px;
  color: #c0c4cc;
}

.article-content {
  white-space: pre-wrap;
  line-height: 1.8;
  font-size: 14px;
  max-height: 60vh;
  overflow: auto;
  padding: 12px;
  background: #fafafa;
  border-radius: 6px;
}
</style>
