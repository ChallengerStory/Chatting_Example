<template>
    <div class="login-container">
        <div class="login-box">
            <h2>채팅 로그인</h2>
            <div class="input-group">
                <input 
                    type="text" 
                    v-model="username" 
                    placeholder="아이디"
                    @keyup.enter="login"
                />
            </div>
            <div class="input-group">
                <input 
                    type="password" 
                    v-model="password" 
                    placeholder="비밀번호"
                    @keyup.enter="login"
                />
            </div>
            <button @click="login" :disabled="isLoading">
                {{ isLoading ? '로그인 중...' : '로그인' }}
            </button>
            <p v-if="error" class="error-message">{{ error }}</p>
        </div>
    </div>
</template>

<script setup>
import { ref } from 'vue'
import axios from 'axios'
import { useRouter } from 'vue-router'

const username = ref('')
const password = ref('')
const error = ref('')
const isLoading = ref(false)
const router = useRouter()

const login = async () => {
    if (!username.value || !password.value) {
        error.value = '아이디와 비밀번호를 입력해주세요.'
        return
    }

    try {
        isLoading.value = true
        const response = await axios.post('http://localhost:5000/auth/login', {
            username: username.value,
            password: password.value
        })

        // 토큰과 사용자 정보 저장
        localStorage.setItem('accessToken', response.data.accessToken)
        localStorage.setItem('refreshToken', response.data.refreshToken)
        localStorage.setItem('userId', response.data.userId)
        localStorage.setItem('username', response.data.username)

        // 채팅 페이지로 이동
        router.push('/chat')
    } catch (err) {
        error.value = '로그인에 실패했습니다. 아이디와 비밀번호를 확인해주세요.'
        console.error('Login error:', err)
    } finally {
        isLoading.value = false
    }
}
</script>

<style scoped>
.login-container {
    height: 100vh;
    display: flex;
    justify-content: center;
    align-items: center;
    background-color: #f5f6f8;
}

.login-box {
    background: white;
    padding: 2rem;
    border-radius: 8px;
    box-shadow: 0 2px 4px rgba(0, 0, 0, 0.1);
    width: 100%;
    max-width: 400px;
}

h2 {
    text-align: center;
    color: #333;
    margin-bottom: 2rem;
}

.input-group {
    margin-bottom: 1rem;
}

input {
    width: 100%;
    padding: 0.8rem;
    border: 1px solid #ddd;
    border-radius: 4px;
    font-size: 1rem;
}

button {
    width: 100%;
    padding: 0.8rem;
    background-color: #ffeb33;
    border: none;
    border-radius: 4px;
    font-size: 1rem;
    font-weight: bold;
    cursor: pointer;
    transition: background-color 0.2s;
}

button:disabled {
    background-color: #ddd;
    cursor: not-allowed;
}

.error-message {
    color: #ff4444;
    text-align: center;
    margin-top: 1rem;
    font-size: 0.9rem;
}
</style> 