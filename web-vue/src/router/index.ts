import { createRouter, createWebHistory, RouteRecordRaw } from 'vue-router'
import { useUserStore } from '@/stores/user'

const routes: RouteRecordRaw[] = [
  {
    path: '/',
    redirect: '/login'
  },
  {
    path: '/login',
    name: 'Login',
    component: () => import('@/views/auth/Login.vue'),
    meta: { title: '登录' }
  },
  {
    path: '/register',
    name: 'Register',
    component: () => import('@/views/auth/Register.vue'),
    meta: { title: '注册' }
  },
  {
    path: '/agents/editor/:id?',
    name: 'AgentEditor',
    component: () => import('@/views/agents/editor.vue'),
    meta: { title: '智能体编辑器', requiresAuth: true }
  },
  {
    path: '/workflows/editor/:id?',
    name: 'WorkflowEditor',
    component: () => import('@/views/workflows/editor.vue'),
    meta: { title: '工作流编辑器', requiresAuth: true }
  },
  {
    path: '/',
    component: () => import('@/layouts/MainLayout.vue'),
    meta: { requiresAuth: true },
    children: [
      {
        path: 'agents',
        name: 'Agents',
        component: () => import('@/views/agents/index.vue'),
        meta: { title: '智能体', requiresAuth: true }
      },
      {
        path: 'workflows',
        name: 'Workflows',
        component: () => import('@/views/workflows/index.vue'),
        meta: { title: '工作流', requiresAuth: true }
      },
      {
        path: 'knowledge',
        name: 'Knowledge',
        component: () => import('@/views/knowledge/index.vue'),
        meta: { title: '知识库', requiresAuth: true }
      },
      {
        path: 'knowledge/:id',
        name: 'KnowledgeBaseDetail',
        component: () => import('@/views/knowledge/detail.vue'),
        meta: { title: '知识库详情', requiresAuth: true }
      },
      {
        path: 'plugins',
        name: 'Plugins',
        component: () => import('@/views/plugins/index.vue'),
        meta: { title: '插件', requiresAuth: true }
      },
      {
        path: 'plugins/:id',
        name: 'PluginDetail',
        component: () => import('@/views/plugins/[id].vue'),
        meta: { title: '插件详情', requiresAuth: true }
      }
    ]
  },
  {
    path: '/:pathMatch(.*)*',
    redirect: '/agents'
  }
]

const router = createRouter({
  history: createWebHistory(import.meta.env.BASE_URL),
  routes
})

router.beforeEach((to, _from, next) => {
  document.title = (to.meta.title as string) || '智能体创作平台'
  const userStore = useUserStore()
  const token = userStore.token

  if (to.meta.requiresAuth && !token) {
    next({ path: '/login', query: { redirect: to.fullPath } })
    return
  }

  if ((to.path === '/login' || to.path === '/register') && token) {
    next('/agents')
    return
  }

  next()
})

export default router
