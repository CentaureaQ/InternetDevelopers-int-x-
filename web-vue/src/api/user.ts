import { http } from '@/utils/http'

interface LoginRequest {
  username: string
  password: string
}

interface RegisterRequest {
  username: string
  password: string
  nickname: string
  email?: string
  phone?: string
}

interface UserInfo {
  id: number
  username: string
  nickname: string
  email?: string
  phone?: string
  avatar?: string
  token: string
}

export const login = (data: LoginRequest): Promise<UserInfo> => {
  return http.post('/api/user/login', data)
}

export const register = (data: RegisterRequest): Promise<void> => {
  return http.post('/api/user/register', data)
}

export const getUserInfo = (): Promise<UserInfo> => {
  return http.get('/api/user/info')
}



