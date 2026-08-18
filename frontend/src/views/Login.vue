<template>
  <div class="login-page">
    <div class="login-card">
      <div class="brand">
        <el-icon :size="40" color="#fff"><ScaleToOriginal /></el-icon>
        <h1>律所数字化办公系统</h1>
        <p>案件 · 客户 · 计费 · 文档 · 审批 一体化管理</p>
      </div>
      <el-form ref="formRef" :model="form" :rules="rules" size="large" @keyup.enter="onLogin">
        <el-form-item prop="username">
          <el-input v-model="form.username" placeholder="用户名" :prefix-icon="User" />
        </el-form-item>
        <el-form-item prop="password">
          <el-input
            v-model="form.password"
            type="password"
            placeholder="密码"
            show-password
            :prefix-icon="Lock"
          />
        </el-form-item>
        <el-button type="primary" class="login-btn" :loading="loading" @click="onLogin">
          登 录
        </el-button>
      </el-form>
    </div>
  </div>
</template>

<script setup>
import { ref } from 'vue'
import { useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { User, Lock } from '@element-plus/icons-vue'
import { login } from '@/api/auth'
import { useUserStore } from '@/store/user'

const router = useRouter()
const store = useUserStore()

const formRef = ref()
const loading = ref(false)
const form = ref({ username: '', password: '' })
const rules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onLogin() {
  await formRef.value.validate()
  loading.value = true
  try {
    const data = await login(form.value)
    store.setLogin(data.token, data.user)
    ElMessage.success(`欢迎回来，${data.user.realName}`)
    router.push('/dashboard')
  } finally {
    loading.value = false
  }
}
</script>

<style scoped>
.login-page {
  height: 100%;
  display: flex;
  align-items: center;
  justify-content: center;
  background: linear-gradient(135deg, #1f3b73 0%, #2c5aa0 50%, #3f7bc4 100%);
}

.login-card {
  width: 400px;
  background: #fff;
  border-radius: 12px;
  padding: 40px 36px 24px;
  box-shadow: 0 12px 40px rgba(0, 0, 0, 0.25);
}

.brand {
  text-align: center;
  margin-bottom: 28px;
  background: linear-gradient(135deg, #1f3b73, #3f7bc4);
  margin: -40px -36px 28px;
  padding: 32px 36px;
  border-radius: 12px 12px 0 0;
  color: #fff;
}

.brand h1 {
  font-size: 20px;
  margin: 10px 0 6px;
}

.brand p {
  font-size: 13px;
  opacity: 0.85;
  margin: 0;
}

.login-btn {
  width: 100%;
  margin-top: 4px;
}

.tips {
  margin-top: 16px;
  font-size: 12px;
  color: #909399;
  text-align: center;
  line-height: 1.8;
}
</style>
