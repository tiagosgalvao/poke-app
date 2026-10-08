import { z } from 'zod'

const PASSWORD_MIN = 8
const PASSWORD_MAX = 72
const USERNAME = /^\s*[A-Za-z0-9_.-]{3,30}\s*$/

export const loginSchema = z.object({
  username: z.string().trim().min(1, 'Enter your username'),
  password: z.string().min(1, 'Enter your password'),
})

export const registerSchema = z.object({
  username: z.string().regex(USERNAME, 'Must be 3 to 30 letters, digits, dots, hyphens or underscores'),
  email: z.email('Enter a valid email address'),
  password: z
    .string()
    .min(PASSWORD_MIN, `Must be at least ${PASSWORD_MIN} characters`)
    .max(PASSWORD_MAX, `Must be at most ${PASSWORD_MAX} characters`),
})

export type LoginForm = z.infer<typeof loginSchema>
export type RegisterForm = z.infer<typeof registerSchema>
