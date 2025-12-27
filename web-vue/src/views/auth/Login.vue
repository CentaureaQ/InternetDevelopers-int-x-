<template>
  <div class="auth-page">
    <div class="auth-decor">
      <span class="bubble bubble-1" />
      <span class="bubble bubble-2" />
      <span class="bubble bubble-3" />
    </div>

    <div class="auth-wrapper">
      <section class="intro-panel">
        <span class="intro-badge">AI 生产力工具集</span>
        <h1>智能体创作平台</h1>
        <p>一站式构建、调试和分发，助力业务快速落地</p>
        <ul>
          <li>拖拽式流程编排</li>
          <li>插件级能力扩展</li>
          <li>实时监控与调优</li>
        </ul>
      </section>

      <div class="form-panel">
        <header>
          <h2>欢迎回来</h2>
          <p>登录以继续创作智能体</p>
        </header>
        <el-form
          class="auth-form"
          label-position="top"
          :model="formData"
          :rules="rules"
          ref="formRef"
          @submit.prevent="onSubmit"
        >
          <el-form-item label="用户名" prop="username">
            <el-input v-model="formData.username" placeholder="请输入用户名" size="large" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input
              v-model="formData.password"
              type="password"
              placeholder="请输入密码"
              size="large"
              @keyup.enter="onSubmit"
            />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" size="large" class="submit-btn" :loading="loading" @click="onSubmit">
              登录
            </el-button>
          </el-form-item>
          <p class="switch-tip">
            还没有账号？
            <el-button link type="primary" @click="goToRegister">立即注册</el-button>
          </p>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter, useRoute } from 'vue-router'
import { useUserStore } from '@/stores/user'
import { login } from '@/api/user'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

const router = useRouter()
const route = useRoute()
const userStore = useUserStore()

const loading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  username: '',
  password: ''
})

const rules: FormRules = {
  username: [{ required: true, message: '请输入用户名', trigger: 'blur' }],
  password: [{ required: true, message: '请输入密码', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      const data = await login(formData)

      if (!data || !data.token) {
        ElMessage.error('登录失败：未获取到token')
        return
      }

      userStore.login(data)
      ElMessage.success('登录成功')

      setTimeout(() => {
        const redirect = (route.query.redirect as string) || '/console/agents'
        router.replace(redirect)
      }, 500)
    } catch (error: any) {
      console.error('登录失败:', error)
    } finally {
      loading.value = false
    }
  })
}

function goToRegister() {
  router.push('/register')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: radial-gradient(circle at 10% 20%, #fef9f7 0%, #f3f6ff 45%, #e7ecff 100%);
  padding: 40px 24px;
  position: relative;
  overflow: hidden;
  display: flex;
  align-items: center;
  justify-content: center;
}

.auth-decor {
  position: absolute;
  inset: 0;
  pointer-events: none;
}

.bubble {
  position: absolute;
  border-radius: 50%;
  opacity: 0.35;
  filter: blur(0.5px);
  background: linear-gradient(135deg, #b3c7ff, #dfd1ff);
}

.bubble-1 {
  width: 260px;
  height: 260px;
  top: -60px;
  right: -40px;
}

.bubble-2 {
  width: 180px;
  height: 180px;
  bottom: 10%;
  left: -60px;
}

.bubble-3 {
  width: 140px;
  height: 140px;
  bottom: 0;
  right: 10%;
}

.auth-wrapper {
  position: relative;
  z-index: 1;
  max-width: 1080px;
  width: 100%;
  margin: 0 auto;
  display: grid;
  grid-template-columns: repeat(auto-fit, minmax(320px, 1fr));
  gap: 36px;
  align-items: center;
}

.intro-panel {
  color: #1f2a44;
}

.intro-badge {
  display: inline-flex;
  align-items: center;
  gap: 6px;
  font-size: 13px;
  font-weight: 600;
  color: #4153ef;
  background: rgba(65, 83, 239, 0.1);
  border-radius: 999px;
  padding: 6px 14px;
}

.intro-panel h1 {
  font-size: 40px;
  margin: 20px 0 12px;
  font-weight: 700;
}

.intro-panel p {
  font-size: 16px;
  color: #516178;
  margin-bottom: 24px;
}

.intro-panel ul {
  list-style: none;
  padding: 0;
  margin: 0;
  display: grid;
  gap: 12px;
}

.intro-panel li {
  display: flex;
  align-items: center;
  gap: 8px;
  font-size: 15px;
  color: #2f3b52;
}

.intro-panel li::before {
  content: '';
  width: 10px;
  height: 10px;
  border-radius: 50%;
  background: #5f6bff;
  box-shadow: 0 0 0 5px rgba(95, 107, 255, 0.12);
}

.form-panel {
  backdrop-filter: blur(16px);
  background: rgba(255, 255, 255, 0.9);
  border-radius: 28px;
  padding: 40px 36px;
  box-shadow: 0 25px 70px rgba(38, 66, 180, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.form-panel header h2 {
  margin: 0;
  font-size: 28px;
  color: #111a34;
}

.form-panel header p {
  margin: 6px 0 26px;
  color: #697a98;
  font-size: 14px;
}

.auth-form :deep(.el-form-item__label) {
  color: #5a6175;
  font-weight: 500;
}

.submit-btn {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 12px 20px rgba(65, 83, 239, 0.35);
}

.switch-tip {
  text-align: center;
  margin: 4px 0 0;
  color: #7c859e;
  font-size: 14px;
}

@media (max-width: 768px) {
  .auth-page {
    padding: 32px 16px;
  }

  .form-panel {
    padding: 32px 24px;
  }

  .intro-panel {
    text-align: center;
  }

  .intro-panel ul {
    justify-items: center;
  }
}
</style>



