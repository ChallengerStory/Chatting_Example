import { createApp } from 'vue'
import App from './App.vue'
import router from './router'
import axios from 'axios'

// axios 인터셉터 설정
axios.interceptors.request.use(
    config => {
        const token = localStorage.getItem('accessToken')
        if (token) {
            config.headers.Authorization = `Bearer ${token}`
        }
        return config
    },
    error => {
        return Promise.reject(error)
    }
)

// 401 에러 처리 및 토큰 갱신
axios.interceptors.response.use(
    response => response,
    async error => {
        const originalRequest = error.config;
        
        if (error.response?.status === 401 && !originalRequest._retry) {
            originalRequest._retry = true;
            
            try {
                const refreshToken = localStorage.getItem('refreshToken');
                if (!refreshToken) {
                    throw new Error('No refresh token available');
                }

                // 토큰 갱신 요청
                const response = await axios.post('http://localhost:5000/auth/refresh', refreshToken, {
                    headers: {
                        'Content-Type': 'text/plain'
                    },
                    _retry: true  // 이 요청은 재시도하지 않음
                });

                const { accessToken, refreshToken: newRefreshToken } = response.data;
                
                // 새로운 토큰들 저장
                localStorage.setItem('accessToken', accessToken);
                if (newRefreshToken) {
                    localStorage.setItem('refreshToken', newRefreshToken);
                }

                // 모든 axios 요청의 기본 헤더 업데이트
                axios.defaults.headers.common['Authorization'] = `Bearer ${accessToken}`;
                
                // 원래 요청 재시도
                return axios(originalRequest);
            } catch (refreshError) {
                console.error('Token refresh failed:', refreshError);
                localStorage.clear();
                window.location.href = '/login';
                return Promise.reject(refreshError);
            }
        }
        return Promise.reject(error);
    }
)

const app = createApp(App)
app.use(router)
app.mount('#app')
