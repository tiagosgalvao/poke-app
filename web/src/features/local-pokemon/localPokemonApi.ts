import { keepPreviousData, useMutation, useQuery, useQueryClient } from '@tanstack/react-query'
import { apiRequest } from '../../api/client'
import type { LocalPokemon, Page, ProprietaryData, SyncSummary } from '../../api/types'

const LOCAL_POKEMON = ['local-pokemon']
export const LOCAL_PAGE_SIZE = 20

export function useLocalPokemonPage(page: number) {
  return useQuery({
    queryKey: [...LOCAL_POKEMON, 'page', page],
    queryFn: ({ signal }) =>
      apiRequest<Page<LocalPokemon>>(`/local-pokemon?page=${page - 1}&size=${LOCAL_PAGE_SIZE}`, { signal }),
    placeholderData: keepPreviousData,
  })
}

export function useLocalPokemon(id: number) {
  return useQuery({
    queryKey: [...LOCAL_POKEMON, id],
    queryFn: ({ signal }) => apiRequest<LocalPokemon>(`/local-pokemon/${id}`, { signal }),
  })
}

function useInvalidatingMutation<TInput, TOutput>(mutationFn: (input: TInput) => Promise<TOutput>) {
  const queryClient = useQueryClient()
  return useMutation({
    mutationFn,
    onSuccess: () => queryClient.invalidateQueries({ queryKey: LOCAL_POKEMON }),
  })
}

export function useImportPokemon() {
  return useInvalidatingMutation((idOrName: string) =>
    apiRequest<LocalPokemon>('/local-pokemon', { method: 'POST', body: { idOrName } }),
  )
}

export function useUpdateLocalPokemon(id: number) {
  return useInvalidatingMutation((change: { version: number } & ProprietaryData) =>
    apiRequest<LocalPokemon>(`/local-pokemon/${id}`, { method: 'PUT', body: change }),
  )
}

export function useDeleteLocalPokemon() {
  return useInvalidatingMutation((id: number) => apiRequest<void>(`/local-pokemon/${id}`, { method: 'DELETE' }))
}

export function useSyncPokemon() {
  return useInvalidatingMutation((ids: number[]) =>
    apiRequest<SyncSummary>('/local-pokemon/sync', { method: 'POST', body: { ids } }),
  )
}
