import { Link } from 'react-router'
import type { PokemonSummary } from '../../api/types'
import { dexNumber, displayName } from '../../components/format'

export function PokemonCard({ pokemon }: { pokemon: PokemonSummary }) {
  return (
    <Link
      to={`/pokemon/${pokemon.name}`}
      className="flex flex-col gap-2 rounded-xl border border-slate-200 bg-white p-4 shadow-sm transition hover:-translate-y-0.5 hover:shadow-md focus-visible:ring-2 focus-visible:ring-sky-400 focus-visible:outline-none"
    >
      <div className="flex items-start justify-between">
        <span className="text-xs font-semibold text-slate-400">{dexNumber(pokemon.id)}</span>
        <span className="text-xs text-slate-500">{pokemon.weightKg} kg</span>
      </div>
      {pokemon.spriteUrl ? (
        <img src={pokemon.spriteUrl} alt={pokemon.name} width={96} height={96} loading="lazy" className="mx-auto h-24 w-24" />
      ) : (
        <div role="img" aria-label={pokemon.name} className="mx-auto h-24 w-24 rounded-full bg-slate-100" />
      )}
      <h3 className="text-center font-semibold">{displayName(pokemon.name)}</h3>
      <p className="text-center text-sm text-slate-500">{pokemon.category ?? 'Unknown category'}</p>
      <ul aria-label="Abilities" className="flex flex-wrap justify-center gap-1">
        {pokemon.abilities.map((ability) => (
          <li
            key={ability.name}
            title={ability.hidden ? 'Hidden ability' : undefined}
            className={`rounded-full px-2 py-0.5 text-xs ${ability.hidden ? 'bg-amber-50 text-amber-800' : 'bg-sky-50 text-sky-800'}`}
          >
            {ability.name}
            {ability.hidden && ' ★'}
          </li>
        ))}
      </ul>
    </Link>
  )
}

export function PokemonCardSkeleton() {
  return (
    <div className="flex animate-pulse flex-col gap-2 rounded-xl border border-slate-200 bg-white p-4">
      <div className="h-3 w-10 rounded bg-slate-200" />
      <div className="mx-auto h-24 w-24 rounded-full bg-slate-200" />
      <div className="mx-auto h-4 w-24 rounded bg-slate-200" />
      <div className="mx-auto h-3 w-20 rounded bg-slate-100" />
    </div>
  )
}
