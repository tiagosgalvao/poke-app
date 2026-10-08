import { http, HttpResponse } from 'msw'
import { afterEach, describe, expect, it, vi } from 'vitest'
import { server } from '../test/server'
import { ApiError, apiRequest, configureApiClient } from './client'

describe('apiRequest', () => {
  afterEach(() => configureApiClient({ getToken: () => null, onUnauthorized: () => {} }))

  it('returns the parsed JSON body', async () => {
    server.use(http.get('*/api/v1/pokemon/25', () => HttpResponse.json({ id: 25, name: 'pikachu' })))

    await expect(apiRequest('/pokemon/25')).resolves.toEqual({ id: 25, name: 'pikachu' })
  })

  it('sends the bearer token and a JSON body', async () => {
    let authorization: string | null = null
    let body: unknown = null
    server.use(
      http.post('*/api/v1/local-pokemon', async ({ request }) => {
        authorization = request.headers.get('Authorization')
        body = await request.json()
        return HttpResponse.json({ id: 25 }, { status: 201 })
      }),
    )
    configureApiClient({ getToken: () => 'jwt-token', onUnauthorized: () => {} })

    await apiRequest('/local-pokemon', { method: 'POST', body: { idOrName: 'pikachu' } })

    expect(authorization).toBe('Bearer jwt-token')
    expect(body).toEqual({ idOrName: 'pikachu' })
  })

  it('resolves to undefined for 204 No Content', async () => {
    server.use(http.delete('*/api/v1/local-pokemon/25', () => new HttpResponse(null, { status: 204 })))

    await expect(apiRequest('/local-pokemon/25', { method: 'DELETE' })).resolves.toBeUndefined()
  })

  it('turns a ProblemDetail into an ApiError with field errors', async () => {
    server.use(
      http.put('*/api/v1/local-pokemon/25', () =>
        HttpResponse.json(
          {
            title: 'Bad Request',
            status: 400,
            detail: 'Validation failed',
            fieldErrors: [{ field: 'localizedName', message: 'must not be blank' }],
          },
          { status: 400, headers: { 'Content-Type': 'application/problem+json' } },
        ),
      ),
    )

    const error = await apiRequest('/local-pokemon/25', { method: 'PUT', body: {} }).catch((e: unknown) => e)

    expect(error).toBeInstanceOf(ApiError)
    expect(error).toMatchObject({
      status: 400,
      detail: 'Validation failed',
      fieldErrors: [{ field: 'localizedName', message: 'must not be blank' }],
    })
  })

  it('calls the unauthorized handler on 401', async () => {
    const onUnauthorized = vi.fn()
    configureApiClient({ getToken: () => 'expired', onUnauthorized })
    server.use(http.patch('*/api/v1/local-pokemon/25', () => HttpResponse.json({ status: 401 }, { status: 401 })))

    await expect(apiRequest('/local-pokemon/25', { method: 'PATCH', body: {} })).rejects.toMatchObject({ status: 401 })
    expect(onUnauthorized).toHaveBeenCalledOnce()
  })

  it('reports network failures as an ApiError with status 0', async () => {
    server.use(http.get('*/api/v1/pokemon', () => HttpResponse.error()))

    await expect(apiRequest('/pokemon')).rejects.toMatchObject({ status: 0 })
  })
})
