import { createRouter, createWebHistory } from 'vue-router'
import Login from '../components/Login.vue'
import Chat from '../components/Chat.vue'

const routes = [
    {
        path: '/',
        redirect: '/login'
    },
    {
        path: '/login',
        component: Login
    },
    {
        path: '/chat',
        component: Chat,
        meta: { requiresAuth: true }
    }
]

const router = createRouter({
    history: createWebHistory(),
    routes
})

// 네비게이션 가드 설정
router.beforeEach((to, from, next) => {
    const accessToken = localStorage.getItem('accessToken')
    
    if (to.matched.some(record => record.meta.requiresAuth)) {
        if (!accessToken) {
            next('/login')
        } else {
            next()
        }
    } else {
        if (accessToken && to.path === '/login') {
            next('/chat')
        } else {
            next()
        }
    }
})

export default router 