<template>
  <el-container class="layout">
    <el-aside :width="isCollapse ? '64px' : '220px'" class="aside">
      <div class="logo">
        <el-icon :size="24" color="#fff"><ScaleToOriginal /></el-icon>
        <span v-show="!isCollapse" class="logo-text">律所办公系统</span>
      </div>
      <el-menu
        :default-active="$route.path"
        :collapse="isCollapse"
        :collapse-transition="false"
        router
        background-color="#001529"
        text-color="rgba(255,255,255,0.68)"
        active-text-color="#fff"
        class="menu"
      >
        <el-menu-item v-for="item in menus" :key="item.path" :index="item.path">
          <el-icon><component :is="item.meta.icon" /></el-icon>
          <template #title>{{ item.meta.title }}</template>
        </el-menu-item>
      </el-menu>
    </el-aside>

    <el-container>
      <el-header class="header">
        <div class="left">
          <el-icon class="collapse-btn" @click="isCollapse = !isCollapse">
            <Expand v-if="isCollapse" />
            <Fold v-else />
          </el-icon>
          <el-breadcrumb separator="/">
            <el-breadcrumb-item>{{ $route.meta.title || '' }}</el-breadcrumb-item>
          </el-breadcrumb>
        </div>
        <el-dropdown @command="onCommand">
          <span class="user-info">
            <el-avatar :size="30" style="background: #409eff">
              {{ store.displayName.charAt(0) }}
            </el-avatar>
            <span class="name">{{ store.displayName }}</span>
            <el-tag size="small" type="info">{{ roleLabel(store.user?.role) }}</el-tag>
          </span>
          <template #dropdown>
            <el-dropdown-menu>
              <el-dropdown-item command="password">修改密码</el-dropdown-item>
              <el-dropdown-item command="logout" divided>退出登录</el-dropdown-item>
            </el-dropdown-menu>
          </template>
        </el-dropdown>
      </el-header>

      <el-main class="main">
        <router-view />
      </el-main>
    </el-container>
  </el-container>

  <el-dialog v-model="pwdVisible" title="修改密码" width="420px">
    <el-form ref="pwdFormRef" :model="pwdForm" :rules="pwdRules" label-width="90px">
      <el-form-item label="原密码" prop="oldPassword">
        <el-input v-model="pwdForm.oldPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="新密码" prop="newPassword">
        <el-input v-model="pwdForm.newPassword" type="password" show-password />
      </el-form-item>
      <el-form-item label="确认新密码" prop="confirm">
        <el-input v-model="pwdForm.confirm" type="password" show-password />
      </el-form-item>
    </el-form>
    <template #footer>
      <el-button @click="pwdVisible = false">取消</el-button>
      <el-button type="primary" @click="submitPwd">确定</el-button>
    </template>
  </el-dialog>
</template>

<script setup>
import { computed, ref } from 'vue'
import { useRoute, useRouter } from 'vue-router'
import { ElMessage } from 'element-plus'
import { useUserStore } from '@/store/user'
import { changePassword } from '@/api/auth'
import { roleLabel } from '@/utils/dict'

const route = useRoute()
const router = useRouter()
const store = useUserStore()

const isCollapse = ref(false)

const allMenus = router.options.routes
  .find((r) => r.path === '/')
  .children.filter((r) => !r.meta?.hidden)

const menus = computed(() =>
  allMenus.filter(
    (m) => (!m.meta?.adminOnly || store.isAdmin) && (!m.meta?.managerOnly || store.isManager)
  )
)

function onCommand(cmd) {
  if (cmd === 'logout') {
    store.logout()
    router.push('/login')
  } else if (cmd === 'password') {
    pwdVisible.value = true
  }
}

const pwdVisible = ref(false)
const pwdFormRef = ref()
const pwdForm = ref({ oldPassword: '', newPassword: '', confirm: '' })
const pwdRules = {
  oldPassword: [{ required: true, message: '请输入原密码', trigger: 'blur' }],
  newPassword: [
    { required: true, message: '请输入新密码', trigger: 'blur' },
    { min: 6, message: '密码至少 6 位', trigger: 'blur' }
  ],
  confirm: [
    { required: true, message: '请再次输入新密码', trigger: 'blur' },
    {
      validator: (rule, value, callback) => {
        if (value !== pwdForm.value.newPassword) callback(new Error('两次密码不一致'))
        else callback()
      },
      trigger: 'blur'
    }
  ]
}

async function submitPwd() {
  await pwdFormRef.value.validate()
  await changePassword({
    oldPassword: pwdForm.value.oldPassword,
    newPassword: pwdForm.value.newPassword
  })
  ElMessage.success('密码修改成功，请重新登录')
  pwdVisible.value = false
  store.logout()
  router.push('/login')
}
</script>

<style scoped>
.layout {
  height: 100%;
}

.aside {
  background: #001529;
  transition: width 0.2s;
  overflow-x: hidden;
}

.logo {
  height: 56px;
  display: flex;
  align-items: center;
  justify-content: center;
  gap: 8px;
  color: #fff;
}

.logo-text {
  font-size: 16px;
  font-weight: 600;
  white-space: nowrap;
}

.menu {
  border-right: none;
}

.header {
  background: #fff;
  display: flex;
  align-items: center;
  justify-content: space-between;
  box-shadow: 0 1px 4px rgba(0, 21, 41, 0.08);
  z-index: 10;
}

.header .left {
  display: flex;
  align-items: center;
  gap: 16px;
}

.collapse-btn {
  font-size: 20px;
  cursor: pointer;
}

.user-info {
  display: flex;
  align-items: center;
  gap: 8px;
  cursor: pointer;
  outline: none;
}

.main {
  padding: 16px;
  overflow: auto;
  display: flex;
  flex-direction: column;
}
</style>
