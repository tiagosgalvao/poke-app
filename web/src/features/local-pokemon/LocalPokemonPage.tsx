import { ErrorState } from '../../components/ErrorState'
import { Pagination } from '../../components/Pagination'
import { usePageParam } from '../../components/usePageParam'
import { useLocalPokemonPage } from './localPokemonApi'
import { LocalPokemonCard } from './LocalPokemonCard'
import { SyncPanel } from './SyncPanel'

export function LocalPokemonPage() {
  const [page, setPage] = usePageParam()
  const localPage = useLocalPokemonPage(page)

  return (
    <section className="flex flex-col gap-6">
      <div className="flex flex-wrap items-baseline justify-between gap-2">
        <h2 className="text-2xl font-semibold">My Pokedex</h2>
        {localPage.data && <p className="text-sm text-slate-500">{localPage.data.totalElements} Pokemon stored locally</p>}
      </div>
      <SyncPanel />
      {localPage.isPending && <div aria-busy="true" className="h-40 animate-pulse rounded-xl bg-white" />}
      {localPage.isError && <ErrorState message={localPage.error.message} onRetry={() => localPage.refetch()} />}
      {localPage.data && localPage.data.content.length === 0 && (
        <p className="text-slate-600">Your Pokedex is empty. Add Pokemon from the catalog or sync some ids above.</p>
      )}
      {localPage.data && localPage.data.content.length > 0 && (
        <>
          <div className="grid grid-cols-1 gap-4 sm:grid-cols-2 lg:grid-cols-3">
            {localPage.data.content.map((pokemon) => (
              <LocalPokemonCard key={pokemon.id} pokemon={pokemon} />
            ))}
          </div>
          <Pagination page={page} totalPages={localPage.data.totalPages} onChange={setPage} />
        </>
      )}
    </section>
  )
}
