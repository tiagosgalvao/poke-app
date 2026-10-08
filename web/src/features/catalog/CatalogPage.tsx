import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { usePageParam } from '../../components/usePageParam'
import { CATALOG_PAGE_SIZE, useCatalogPage } from './catalogApi'
import { PokemonCard, PokemonCardSkeleton } from './PokemonCard'

const GRID = 'grid grid-cols-2 gap-4 sm:grid-cols-3 lg:grid-cols-4 xl:grid-cols-5'

export function CatalogPage() {
  const [page, setPage] = usePageParam()
  const catalog = useCatalogPage(page)

  return (
    <section>
      <div className="mb-6 flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-2xl font-semibold">Pokemon catalog</h2>
        {catalog.data && <p className="text-sm text-slate-500">{catalog.data.totalElements} Pokemon from PokeAPI</p>}
      </div>
      {catalog.isError ? (
        <ErrorState message={catalog.error.message} onRetry={() => catalog.refetch()} />
      ) : (
        <>
          <div className={GRID} aria-busy={catalog.isPending}>
            {catalog.isPending
              ? Array.from({ length: CATALOG_PAGE_SIZE }, (_, index) => <PokemonCardSkeleton key={index} />)
              : catalog.data.content.map((pokemon) => <PokemonCard key={pokemon.id} pokemon={pokemon} />)}
          </div>
          {catalog.data && <Pagination page={page} totalPages={catalog.data.totalPages} onChange={setPage} />}
        </>
      )}
    </section>
  )
}
