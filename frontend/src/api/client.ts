import axios from 'axios'
import { useSesion } from '../store/auth'

export const api = axios.create({
  baseURL: '/api',
})

api.interceptors.request.use((config) => {
  const token = useSesion.getState().token
  if (token) {
    config.headers.Authorization = `Bearer ${token}`
  }
  return config
})

api.interceptors.response.use(
  (respuesta) => respuesta,
  (error) => {
    if (error.response?.status === 401) {
      useSesion.getState().logout()
    }
    return Promise.reject(error)
  },
)
