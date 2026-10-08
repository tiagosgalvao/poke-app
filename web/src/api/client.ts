import type { FieldError, ProblemDetail } from './types'

const API_BASE = '/api/v1'
const NETWORK_ERROR_STATUS = 0
const NO_CONTENT = 204
const UNAUTHORIZED = 401
const JSON_CONTENT_TYPE = 'application/json'
const NETWORK_ERROR_MESSAGE = 'Could not reach the server. Check your connection and try again.'
const UNEXPECTED_ERROR_MESSAGE = 'Something went wrong. Please try again.'

export class ApiError extends Error {
  readonly status: number
  readonly detail: string
  readonly fieldErrors: FieldError[]

  constructor(status: number, detail: string, fieldErrors: FieldError[] = []) {
    super(detail)
    this.name = 'ApiError'
    this.status = status
    this.detail = detail
    this.fieldErrors = fieldErrors
  }
}

interface ApiClientConfig {
  getToken: () => string | null
  onUnauthorized: () => void
}

let config: ApiClientConfig = { getToken: () => null, onUnauthorized: () => {} }

export function configureApiClient(next: ApiClientConfig) {
  config = next
}

interface RequestOptions {
  method?: 'GET' | 'POST' | 'PUT' | 'PATCH' | 'DELETE'
  body?: unknown
  signal?: AbortSignal
}

export async function apiRequest<T>(path: string, options: RequestOptions = {}): Promise<T> {
  const headers = new Headers({ Accept: JSON_CONTENT_TYPE })
  if (options.body !== undefined) {
    headers.set('Content-Type', JSON_CONTENT_TYPE)
  }
  const token = config.getToken()
  if (token) {
    headers.set('Authorization', `Bearer ${token}`)
  }

  let response: Response
  try {
    response = await fetch(new URL(API_BASE + path, window.location.origin), {
      method: options.method ?? 'GET',
      headers,
      body: options.body === undefined ? undefined : JSON.stringify(options.body),
      signal: options.signal,
    })
  } catch {
    throw new ApiError(NETWORK_ERROR_STATUS, NETWORK_ERROR_MESSAGE)
  }

  if (response.status === UNAUTHORIZED) {
    config.onUnauthorized()
  }
  if (!response.ok) {
    throw await toApiError(response)
  }
  if (response.status === NO_CONTENT) {
    return undefined as T
  }
  return (await response.json()) as T
}

async function toApiError(response: Response): Promise<ApiError> {
  const problem = (await response.json().catch(() => ({}))) as ProblemDetail
  return new ApiError(response.status, problem.detail ?? UNEXPECTED_ERROR_MESSAGE, problem.fieldErrors ?? [])
}
