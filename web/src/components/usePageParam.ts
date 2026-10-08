import { useSearchParams } from 'react-router'

const PAGE_PARAM = 'page'
const FIRST_PAGE = 1

export function usePageParam() {
  const [searchParams, setSearchParams] = useSearchParams()
  const requested = Number(searchParams.get(PAGE_PARAM))
  const page = Number.isInteger(requested) && requested >= FIRST_PAGE ? requested : FIRST_PAGE

  const setPage = (next: number) => {
    setSearchParams((params) => {
      params.set(PAGE_PARAM, String(next))
      return params
    })
  }
  return [page, setPage] as const
}
