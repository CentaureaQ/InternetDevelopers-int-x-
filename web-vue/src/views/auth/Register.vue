<template>
  <div class="auth-page">
    <div class="auth-decor">
      <span class="bubble bubble-1" />
      <span class="bubble bubble-2" />
      <span class="bubble bubble-3" />
    </div>

    <div class="auth-wrapper reverse">
      <section class="intro-panel">
        <span class="intro-badge">新成员欢迎加入</span>
        <h1>注册您的创作工作区</h1>
        <p>完善基础信息，即可开启智能体的创造旅程</p>
        <div class="steps">
          <div>
            <span>01</span>
            <strong>初始化团队</strong>
            <p>配置成员与权限，保持协作安全</p>
          </div>
          <div>
            <span>02</span>
            <strong>接入插件</strong>
            <p>快速组合业务插件，扩展能力</p>
          </div>
        </div>
      </section>

      <div class="form-panel">
        <header>
          <h2>创建账号</h2>
          <p>
            已经有账号？
            <el-button link type="primary" @click="goToLogin">立即登录</el-button>
          </p>
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
            <el-input v-model="formData.username" placeholder="请输入用户名" />
          </el-form-item>
          <el-form-item label="密码" prop="password">
            <el-input v-model="formData.password" type="password" placeholder="请输入密码" />
          </el-form-item>
          <el-form-item label="确认密码" prop="confirmPassword">
            <el-input v-model="formData.confirmPassword" type="password" placeholder="请再次输入密码" />
          </el-form-item>
          <el-form-item label="昵称" prop="nickname">
            <el-input v-model="formData.nickname" placeholder="请输入昵称" />
          </el-form-item>
          <el-form-item label="手机号" prop="phone">
            <el-input v-model="formData.phone" placeholder="请输入手机号（选填）" />
          </el-form-item>
          <el-form-item label="邮箱" prop="email">
            <el-input v-model="formData.email" type="email" placeholder="请输入邮箱（选填）" />
          </el-form-item>
          <el-form-item>
            <el-button type="primary" class="submit-btn" :loading="loading" @click="onSubmit">注册</el-button>
          </el-form-item>
        </el-form>
      </div>
    </div>
  </div>
</template>

<script setup lang="ts">
import { ref, reactive } from 'vue'
import { useRouter } from 'vue-router'
import { register } from '@/api/user'
import { ElMessage, FormInstance, FormRules } from 'element-plus'

const router = useRouter()

const loading = ref(false)
const formRef = ref<FormInstance>()
const formData = reactive({
  username: '',
  password: '',
  confirmPassword: '',
  nickname: '',
  phone: '',
  email: ''
})

const validatePassword = (_rule: any, value: any, callback: any) => {
  if (!value) {
    callback(new Error('请输入确认密码'))
  } else if (value !== formData.password) {
    callback(new Error('两次密码输入不一致'))
  } else {
    callback()
  }
}

const rules: FormRules = {
  username: [
    { required: true, message: '请输入用户名', trigger: 'blur' },
    { pattern: /^[a-zA-Z0-9_]{4,16}$/, message: '用户名4-16位字母数字下划线', trigger: 'blur' }
  ],
  password: [
    { required: true, message: '请输入密码', trigger: 'blur' },
    { min: 6, message: '密码至少6位', trigger: 'blur' }
  ],
  confirmPassword: [{ validator: validatePassword, trigger: 'blur' }],
  nickname: [{ required: true, message: '请输入昵称', trigger: 'blur' }],
  phone: [
    {
      pattern: /^(?:\+?86)?1[3-9]\d{9}$/,
      message: '请输入有效的手机号',
      trigger: 'blur'
    }
  ],
  email: [{ type: 'email', message: '邮箱格式不正确', trigger: 'blur' }]
}

async function onSubmit() {
  if (!formRef.value) return

  await formRef.value.validate(async (valid) => {
    if (!valid) return

    try {
      loading.value = true
      await register({
        username: formData.username,
        password: formData.password,
        nickname: formData.nickname,
        phone: formData.phone || undefined,
        email: formData.email || undefined
      })

      ElMessage.success('注册成功，请登录')
      setTimeout(() => {
        router.replace('/login')
      }, 1500)
    } catch (error: any) {
      console.error('注册失败:', error)
      ElMessage.error(error.message || '注册失败，请重试')
    } finally {
      loading.value = false
    }
  })
}

function goToLogin() {
  router.push('/login')
}
</script>

<style scoped>
.auth-page {
  min-height: 100vh;
  background: radial-gradient(circle at 80% 0%, #fef5f9 0%, #f7fbff 35%, #e6ecff 100%);
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
  opacity: 0.3;
  background: linear-gradient(135deg, #ffc3e4, #b6c3ff);
}

.bubble-1 {
  width: 240px;
  height: 240px;
  top: -80px;
  left: -40px;
}

.bubble-2 {
  width: 160px;
  height: 160px;
  bottom: 12%;
  right: 6%;
}

.bubble-3 {
  width: 120px;
  height: 120px;
  bottom: 0;
  left: 35%;
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

.auth-wrapper.reverse {
  direction: rtl;
}

.auth-wrapper.reverse > * {
  direction: ltr;
}

.intro-panel {
  color: #1d1f3b;
}

.intro-badge {
  display: inline-flex;
  align-items: center;
  font-size: 13px;
  font-weight: 600;
  color: #e14d73;
  background: rgba(225, 77, 115, 0.1);
  border-radius: 999px;
  padding: 6px 14px;
}

.intro-panel h1 {
  font-size: 38px;
  margin: 20px 0 12px;
  font-weight: 700;
}

.intro-panel p {
  font-size: 15px;
  color: #5d6585;
  margin-bottom: 28px;
}

.steps {
  display: grid;
  gap: 18px;
}

.steps div {
  padding: 18px;
  border-radius: 20px;
  background: rgba(255, 255, 255, 0.9);
  border: 1px solid rgba(255, 255, 255, 0.6);
  box-shadow: 0 15px 40px rgba(225, 77, 115, 0.15);
}

.steps span {
  display: inline-flex;
  justify-content: center;
  align-items: center;
  width: 32px;
  height: 32px;
  border-radius: 999px;
  background: #ffe5ef;
  color: #e14d73;
  font-weight: 700;
}

.steps strong {
  display: block;
  margin: 10px 0 4px;
  font-size: 16px;
  color: #1f244d;
}

.steps p {
  margin: 0;
  font-size: 14px;
  color: #6b7290;
}

.form-panel {
  backdrop-filter: blur(16px);
  background: rgba(255, 255, 255, 0.95);
  border-radius: 28px;
  padding: 40px 36px;
  box-shadow: 0 25px 70px rgba(133, 67, 136, 0.15);
  border: 1px solid rgba(255, 255, 255, 0.6);
}

.form-panel header {
  display: flex;
  justify-content: space-between;
  align-items: center;
  gap: 12px;
}

.form-panel header h2 {
  margin: 0;
  font-size: 28px;
  color: #111a34;
}

.form-panel header p {
  margin: 0;
  color: #7c859e;
  font-size: 14px;
}

.auth-form :deep(.el-form-item__label) {
  color: #5a6175;
  font-weight: 500;
}

.submit-btn {
  width: 100%;
  border-radius: 12px;
  box-shadow: 0 12px 20px rgba(225, 77, 115, 0.35);
}

@media (max-width: 768px) {
  .auth-page {
    padding: 32px 16px;
  }

  .form-panel {
    padding: 32px 24px;
  }

  .form-panel header {
    flex-direction: column;
    align-items: flex-start;
  }

  .auth-wrapper.reverse {
    direction: ltr;
  }

  .intro-panel {
    text-align: center;
  }

  .steps div {
    text-align: left;
  }
}
</style>

