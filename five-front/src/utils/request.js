import { useUserIderStore } from '@/stores/userIder'
import axios from 'axios'
import router from '@/router'
import { ElMessage } from 'element-plus'
import 'element-plus/theme-chalk/el-loading.css'
import 'element-plus/theme-chalk/el-message.css'

// Interceptors
let baseURL = 'http://localhost:8080/api'

const instance = axios.create({
  baseURL,
  timeout: 10000 // Request timeout
})

instance.interceptors.request.use(
  // Add token to the configuration object
  (config) => {
    const userIderStore = useUserIderStore()
    config.headers.jwt = userIderStore.userInfo.jwt
    return config
  },
  (err) => Promise.reject(err)
)

instance.interceptors.response.use(
  (res) => {
    if (res.data.code === 1) {
      return res.data
    }
    ElMessage({ message: res.data.msg || 'Unknown exception 1, please contact the administrator', type: 'error' })
    return Promise.reject(res.data)
  },
  (err) => {
    if (!err.response) {
      ElMessage({ message: 'Server is not accessible, please try again later.', type: 'error' })
    } else {
      ElMessage({
        message: err.response.data.msg || 'Unknown exception 2, please contact the administrator',
        type: 'error'
      })
      // If it's a 401 error, redirect to the login page
      if (err.response?.status === 401) {
        router.push('/introduction')
      }
    }
    return Promise.reject(err)
  }
)

export default instance
export { baseURL }
