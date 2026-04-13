import axios from 'axios'

// 创建axios实例
const service = axios.create({
  baseURL: '/', // 后端服务地址，通过vite代理转发
  timeout: 10000 // 请求超时时间
})

// 请求拦截器
service.interceptors.request.use(
  config => {
    // 从localStorage获取token
    const token = localStorage.getItem('token')
    // 如果有token，添加到请求头
    if (token) {
      config.headers.Authorization = `Bearer ${token}`
    }
    return config
  },
  error => {
    console.error('请求错误:', error)
    return Promise.reject(error)
  }
)

// 响应拦截器
service.interceptors.response.use(
  response => {
    // 返回完整的响应对象
    return response
  },
  error => {
    console.error('响应错误:', error)
    return Promise.reject(error)
  }
)

export default service