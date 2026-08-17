<template>
  <el-row :gutter="16">
    <el-col :span="5">
      <el-card shadow="never">
        <template #header>
          <div style="display: flex; justify-content: space-between; align-items: center">
            <span>目录</span>
            <el-button size="small" type="primary" text @click="folderVisible = true">新建目录</el-button>
          </div>
        </template>
        <el-tree
          :data="folders"
          node-key="id"
          highlight-current
          :props="{ label: 'name', children: 'children' }"
          :expand-on-click-node="false"
          @node-click="onFolderClick"
        >
          <template #default="{ node, data }">
            <span style="display: flex; align-items: center; gap: 4px">
              <el-icon><Folder /></el-icon>
              {{ node.label }}
              <el-icon v-if="data.id === query.folderId" @click.stop="clearFolder" style="cursor: pointer"><Close /></el-icon>
            </span>
          </template>
        </el-tree>
      </el-card>
    </el-col>
    <el-col :span="19">
      <div class="page-card">
        <div class="toolbar">
          <el-input v-model="query.keyword" placeholder="搜索文件名" clearable style="width: 200px" @keyup.enter="load" @clear="load">
            <template #prefix><el-icon><Search /></el-icon></template>
          </el-input>
          <el-select v-model="query.category" placeholder="分类" clearable style="width: 120px" @change="load">
            <el-option v-for="(label, key) in DOC_CATEGORY_MAP" :key="key" :label="label" :value="key" />
          </el-select>
          <el-select v-model="query.caseId" placeholder="关联案件" clearable filterable style="width: 180px" @change="load">
            <el-option v-for="c in cases" :key="c.id" :label="`${c.caseNo} ${c.title}`" :value="c.id" />
          </el-select>
          <el-button type="primary" @click="load"><el-icon><Search /></el-icon>查询</el-button>
          <div class="spacer" />
          <el-button type="success" @click="uploadVisible = true"><el-icon><Upload /></el-icon>上传文档</el-button>
        </div>

        <el-table v-loading="loading" :data="items">
          <el-table-column label="文件名" min-width="220" show-overflow-tooltip>
            <template #default="{ row }">
              <el-icon style="margin-right: 4px; color: #909399"><Document /></el-icon>
              {{ row.name }}
            </template>
          </el-table-column>
          <el-table-column label="分类" width="100">
            <template #default="{ row }">{{ docCategoryLabel(row.category) }}</template>
          </el-table-column>
          <el-table-column prop="folderName" label="目录" width="100" />
          <el-table-column label="大小" width="90" align="right">
            <template #default="{ row }">{{ formatSize(row.size) }}</template>
          </el-table-column>
          <el-table-column label="版本" width="70" align="center">
            <template #default="{ row }">v{{ row.version }}</template>
          </el-table-column>
          <el-table-column prop="uploaderName" label="上传人" width="90" />
          <el-table-column label="上传时间" width="140">
            <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
          </el-table-column>
          <el-table-column label="操作" width="190" fixed="right">
            <template #default="{ row }">
              <el-button link type="primary" size="small" @click="download(row)">下载</el-button>
              <el-button link type="success" size="small" @click="openVersions(row)">版本</el-button>
              <el-button link type="warning" size="small" @click="openUploadVersion(row)">新版本</el-button>
              <el-button link type="danger" size="small" @click="remove(row)">删除</el-button>
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
      </div>
    </el-col>
  </el-row>

  <!-- 上传 -->
  <el-dialog v-model="uploadVisible" title="上传文档" width="480px">
    <el-form label-width="90px">
      <el-form-item label="文件" required>
        <el-upload :auto-upload="false" :limit="1" :on-change="onFileChange" :file-list="fileList" drag style="width: 100%">
          <el-icon class="el-icon--upload"><UploadFilled /></el-icon>
          <div class="el-upload__text">拖拽文件到此处，或<em>点击选择</em></div>
        </el-upload>
      </el-form-item>
      <el-form-item label="分类">
        <el-select v-model="uploadForm.category" style="width: 100%">
          <el-option v-for="(label, key) in DOC_CATEGORY_MAP" :key="key" :label="label" :value="key" />
        </el-select>
      </el-form-item>
      <el-form-item label="目录">
        <el-cascader
          v-model="uploadForm.folderId"
          :options="folderCascaderOptions"
          :props="{ value: 'id', label: 'name', children: 'children', checkStrictly: true, emitPath: false }"
          clearable
          style="width: 100%"
        />
      </el-form-item>
      <el-form-item label="关联案件">
        <el-select v-model="uploadForm.caseId" clearable filterable style="width: 100%">
          <el-option v-for="c in cases" :key="c.id" :label="`${c.caseNo} ${c.title}`" :value="c.id" />
        </el-select>
      </el-form-item>
      <el-form-item label="说明">
        <el-input v-model="uploadForm.description" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="uploadVisible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="doUpload">上传</el-button>
    </template>
  </el-dialog>

  <!-- 版本列表 -->
  <el-dialog v-model="versionVisible" title="版本历史" width="560px">
    <el-table :data="versions" size="small">
      <el-table-column label="版本" width="70">
        <template #default="{ row }">v{{ row.version }}</template>
      </el-table-column>
      <el-table-column prop="uploaderName" label="上传人" width="100" />
      <el-table-column label="大小" width="90" align="right">
        <template #default="{ row }">{{ formatSize(row.size) }}</template>
      </el-table-column>
      <el-table-column prop="remark" label="备注" show-overflow-tooltip />
      <el-table-column label="时间" width="140">
        <template #default="{ row }">{{ formatDateTime(row.createdAt) }}</template>
      </el-table-column>
      <el-table-column label="操作" width="80">
        <template #default="{ row }">
          <el-button link type="primary" size="small" @click="download(currentDoc, row.version)">下载</el-button>
        </template>
      </el-table-column>
    </el-table>
  </el-dialog>

  <!-- 上传新版本 -->
  <el-dialog v-model="versionUploadVisible" title="上传新版本" width="440px">
    <el-form label-width="90px">
      <el-form-item label="文件" required>
        <el-upload :auto-upload="false" :limit="1" :on-change="onVersionFileChange" :file-list="versionFileList">
          <el-button>选择文件</el-button>
        </el-upload>
      </el-form-item>
      <el-form-item label="版本说明">
        <el-input v-model="versionRemark" type="textarea" :rows="2" />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="versionUploadVisible = false">取消</el-button>
      <el-button type="primary" :loading="uploading" @click="doUploadVersion">上传</el-button>
    </template>
  </el-dialog>

  <!-- 新建目录 -->
  <el-dialog v-model="folderVisible" title="新建目录" width="400px">
    <el-form :model="folderForm" label-width="90px">
      <el-form-item label="目录名称" required>
        <el-input v-model="folderForm.name" />
      </el-form-item>
      <el-form-item label="上级目录">
        <el-cascader
          v-model="folderForm.parentId"
          :options="folderCascaderOptions"
          :props="{ value: 'id', label: 'name', children: 'children', checkStrictly: true, emitPath: false }"
          clearable
          style="width: 100%"
        />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="folderVisible = false">取消</el-button>
      <el-button type="primary" @click="doCreateFolder">创建</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, onMounted, reactive, ref } from 'vue'
import { ElMessage, ElMessageBox } from 'element-plus'
import { useRoute } from 'vue-router'
import {
  createFolder, deleteDocument, folderTree, getVersions, pageDocuments,
  uploadDocument, uploadVersion, downloadUrl
} from '@/api/document'
import { myCases } from '@/api/case'
import { DOC_CATEGORY_MAP, docCategoryLabel } from '@/utils/dict'
import { formatDateTime, formatSize } from '@/utils/format'

const route = useRoute()
const loading = ref(false)
const items = ref([])
const total = ref(0)
const folders = ref([])
const cases = ref([])
const query = reactive({ keyword: '', category: '', caseId: route.query.caseId || '', folderId: '', page: 1, size: 10 })

const folderCascaderOptions = computed(() => folders.value)

async function loadFolders() {
  folders.value = await folderTree()
}

async function load() {
  loading.value = true
  try {
    const params = { ...query }
    Object.keys(params).forEach((k) => {
      if (params[k] === '' || params[k] === null || params[k] === undefined) delete params[k]
    })
    const data = await pageDocuments(params)
    items.value = data.items
    total.value = data.total
  } finally {
    loading.value = false
  }
}

function onFolderClick(data) {
  query.folderId = data.id
  query.page = 1
  load()
}

function clearFolder() {
  query.folderId = ''
  load()
}

// 上传
const uploadVisible = ref(false)
const uploading = ref(false)
const fileList = ref([])
const uploadForm = ref({ category: 'OTHER', folderId: null, caseId: null, description: '' })

function onFileChange(file) {
  fileList.value = [file]
}

async function doUpload() {
  if (!fileList.value.length) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', fileList.value[0].raw)
    fd.append('category', uploadForm.value.category)
    if (uploadForm.value.folderId) fd.append('folderId', uploadForm.value.folderId)
    if (uploadForm.value.caseId) fd.append('caseId', uploadForm.value.caseId)
    if (uploadForm.value.description) fd.append('description', uploadForm.value.description)
    await uploadDocument(fd)
    ElMessage.success('上传成功')
    uploadVisible.value = false
    fileList.value = []
    uploadForm.value = { category: 'OTHER', folderId: null, caseId: null, description: '' }
    load()
  } finally {
    uploading.value = false
  }
}

// 版本
const versionVisible = ref(false)
const versions = ref([])
const currentDoc = ref(null)

async function openVersions(row) {
  currentDoc.value = row
  versions.value = await getVersions(row.id)
  versionVisible.value = true
}

const versionUploadVisible = ref(false)
const versionFileList = ref([])
const versionRemark = ref('')

function openUploadVersion(row) {
  currentDoc.value = row
  versionFileList.value = []
  versionRemark.value = ''
  versionUploadVisible.value = true
}

function onVersionFileChange(file) {
  versionFileList.value = [file]
}

async function doUploadVersion() {
  if (!versionFileList.value.length) {
    ElMessage.warning('请选择文件')
    return
  }
  uploading.value = true
  try {
    const fd = new FormData()
    fd.append('file', versionFileList.value[0].raw)
    if (versionRemark.value) fd.append('remark', versionRemark.value)
    await uploadVersion(currentDoc.value.id, fd)
    ElMessage.success('新版本已上传')
    versionUploadVisible.value = false
    load()
  } finally {
    uploading.value = false
  }
}

function download(row, version) {
  const url = downloadUrl(row.id, version)
  const token = localStorage.getItem('token')
  window.open(`${url}${url.includes('?') ? '&' : '?'}token=${token}`, '_blank')
}

async function remove(row) {
  await ElMessageBox.confirm(`确定删除文档「${row.name}」及其全部版本？`, '提示', { type: 'warning' })
  await deleteDocument(row.id)
  ElMessage.success('已删除')
  load()
}

// 目录
const folderVisible = ref(false)
const folderForm = ref({ name: '', parentId: null })

async function doCreateFolder() {
  if (!folderForm.value.name) {
    ElMessage.warning('请输入目录名称')
    return
  }
  await createFolder(folderForm.value)
  ElMessage.success('目录已创建')
  folderVisible.value = false
  folderForm.value = { name: '', parentId: null }
  loadFolders()
}

onMounted(async () => {
  loadFolders()
  load()
  const data = await myCases({ page: 1, size: 200 })
  cases.value = data.items
})
</script>
