import { Link, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import { ErrorState } from '../../components/ErrorState'
import { dexNumber, displayName } from '../../components/format'
import { usePokemonDetail } from './catalogApi'
import { EvolutionChain } from './EvolutionChain'
import { StatBar } from './StatBar'
import { TypeBadge } from './TypeBadge'

const NOT_FOUND = 404

export function PokemonDetailPage() {
  const { idOrName = '' } = useParams()
  const detail = usePokemonDetail(idOrName)

  if (detail.isPending) {
    return <div aria-busy="true" className="h-96 animate-pulse rounded-xl bg-white" />
  }
  if (detail.isError) {
    if (detail.error instanceof ApiError && detail.error.status === NOT_FOUND) {
      return (
        <section className="flex flex-col items-start gap-3">
          <h2 className="text-2xl font-semibold">Pokemon not found</h2>
          <p className="text-slate-600">{detail.error.message}</p>
          <Link className="font-medium text-sky-700 hover:underline" to="/">
            Back to the catalog
          </Link>
        </section>
      )
    }
    return <ErrorState message={detail.error.message} onRetry={() => detail.refetch()} />
  }

  const pokemon = detail.data
  const name = displayName(pokemon.name)
  return (
    <article className="grid gap-8 md:grid-cols-[minmax(0,2fr)_minmax(0,3fr)]">
      <div className="flex flex-col items-center gap-4 rounded-xl border border-slate-200 bg-white p-6">
        {pokemon.imageUrl && (
          <img src={pokemon.imageUrl} alt={`${name} artwork`} width={320} height={320} className="h-auto w-full max-w-xs" />
        )}
        <div className="flex gap-2">
          {pokemon.types.map((type) => (
            <TypeBadge key={type} type={type} />
          ))}
        </div>
      </div>
      <div className="flex flex-col gap-6">
        <header>
          <p className="text-sm font-semibold text-slate-400">{dexNumber(pokemon.id)}</p>
          <h2 className="text-3xl font-bold">{name}</h2>
          <p className="text-slate-500">{pokemon.category ?? 'Unknown category'}</p>
        </header>
        {pokemon.description && <p className="text-slate-700">{pokemon.description}</p>}
        <dl className="grid grid-cols-3 gap-4 text-sm">
          <div>
            <dt className="text-slate-500">Weight</dt>
            <dd className="font-semibold">{pokemon.weightKg} kg</dd>
          </div>
          <div>
            <dt className="text-slate-500">Height</dt>
            <dd className="font-semibold">{pokemon.heightM} m</dd>
          </div>
          <div>
            <dt className="text-slate-500">Abilities</dt>
            <dd className="font-semibold">
              {pokemon.abilities.map((ability) => ability.name + (ability.hidden ? ' (hidden)' : '')).join(', ')}
            </dd>
          </div>
        </dl>
        <section>
          <h3 className="mb-3 font-semibold">Base stats</h3>
          <div className="flex flex-col gap-2">
            {pokemon.stats.map((stat) => (
              <StatBar key={stat.name} name={stat.name} value={stat.value} />
            ))}
          </div>
        </section>
        <section>
          <h3 className="mb-3 font-semibold">Evolution</h3>
          <EvolutionChain stages={pokemon.evolution} currentId={pokemon.id} />
        </section>
      </div>
    </article>
  )
}
