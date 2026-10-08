import { keepPreviousData, useQuery } from '@tanstack/react-query'
import { apiRequest } from '../../api/client'
import type { Page, PokemonDetail, PokemonSummary } from '../../api/types'

export const CATALOG_PAGE_SIZE = 20

export function useCatalogPage(page: number) {
  return useQuery({
    queryKey: ['catalog', page],
    queryFn: ({ signal }) =>
      apiRequest<Page<PokemonSummary>>(`/pokemon?page=${page - 1}&size=${CATALOG_PAGE_SIZE}`, { signal }),
    placeholderData: keepPreviousData,
  })
}

export function usePokemonDetail(idOrName: string) {
  return useQuery({
    queryKey: ['pokemon', idOrName],
    queryFn: ({ signal }) => apiRequest<PokemonDetail>(`/pokemon/${encodeURIComponent(idOrName)}`, { signal }),
  })
}
