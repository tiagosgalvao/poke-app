import { apiRequest } from '../../api/client'
import type { AccessToken, User } from '../../api/types'
import type { LoginForm, RegisterForm } from './schemas'

export function login(credentials: LoginForm) {
  return apiRequest<AccessToken>('/auth/login', { method: 'POST', body: credentials })
}

export function register(registration: RegisterForm) {
  return apiRequest<User>('/auth/register', { method: 'POST', body: registration })
}
