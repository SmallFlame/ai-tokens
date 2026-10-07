import axios from 'axios'
import { Message } from 'element-ui'
import store from '@/store'
import router from '@/router'

const request = axios.create({
  baseURL: process.env.VUE_APP_API_BASE || '/api',
  timeout: 60000
})

// 请求拦截：注入 Token
request.interceptors.request.use(config => {
  const token = store.state.token
  if (token) config.headers['Authorization'] = token
  return config
})

// 响应拦截：统一处理错误，剥离外层 Result 包装
request.interceptors.response.use(
  response => {
    const res = response.data
    if (res.code !== 200) {
      Message.error(res.message || '操作失败')
      if (res.code === 401) {
        store.commit('CLEAR_AUTH')
        if (router.currentRoute.path !== '/login') {
          router.push('/login')
        }
      }
      return Promise.reject(new Error(res.message || 'error'))
    }
    // 直接返回 data 字段，组件中无需再 .data
    return res.data
  },
  error => {
    const msg = error.response?.data?.message || error.message || '网络错误'
    Message.error(msg)
    return Promise.reject(error)
  }
)

export default request
