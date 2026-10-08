import { useId, useState } from 'react'
import { Link } from 'react-router'
import type { LocalPokemon } from '../../api/types'
import { dangerButton, secondaryButton } from '../../components/buttonStyles'
import { ErrorAlert } from '../../components/ErrorAlert'
import { dexNumber, displayName } from '../../components/format'
import { useDeleteLocalPokemon } from './localPokemonApi'

export function LocalPokemonCard({ pokemon }: { pokemon: LocalPokemon }) {
  const headingId = useId()
  const [confirming, setConfirming] = useState(false)
  const deletion = useDeleteLocalPokemon()
  const place = [pokemon.region, pokemon.habitat].filter(Boolean).join(' · ')

  return (
    <article aria-labelledby={headingId} className="flex flex-col gap-3 rounded-xl border border-slate-200 bg-white p-4 shadow-sm">
      <div className="flex items-center gap-3">
        {pokemon.spriteUrl && <img src={pokemon.spriteUrl} alt="" width={64} height={64} className="h-16 w-16" />}
        <div className="min-w-0">
          <p className="text-xs font-semibold text-slate-400">{dexNumber(pokemon.id)}</p>
          <h3 id={headingId} className="truncate font-semibold">
            <Link to={`/pokemon/${pokemon.name}`} className="hover:underline">
              {displayName(pokemon.name)}
            </Link>
          </h3>
          {pokemon.localizedName && <p className="text-sm text-slate-600">{pokemon.localizedName}</p>}
        </div>
      </div>
      {place && <p className="text-sm text-slate-600">{place}</p>}
      {pokemon.tags.length > 0 && (
        <ul aria-label="Tags" className="flex flex-wrap gap-1">
          {pokemon.tags.map((tag) => (
            <li key={tag} className="rounded-full bg-emerald-50 px-2 py-0.5 text-xs text-emerald-800">
              {tag}
            </li>
          ))}
        </ul>
      )}
      {pokemon.notes && <p className="text-sm text-slate-500 italic">{pokemon.notes}</p>}
      {deletion.error && <ErrorAlert message={deletion.error.message} />}
      <div className="mt-auto flex flex-wrap gap-2">
        <Link to={`/my-pokedex/${pokemon.id}/edit`} className={secondaryButton}>
          Edit
        </Link>
        {confirming ? (
          <>
            <button type="button" className={dangerButton} disabled={deletion.isPending} onClick={() => deletion.mutate(pokemon.id)}>
              Confirm delete
            </button>
            <button type="button" className={secondaryButton} onClick={() => setConfirming(false)}>
              Cancel
            </button>
          </>
        ) : (
          <button type="button" className={secondaryButton} onClick={() => setConfirming(true)}>
            Delete
          </button>
        )}
      </div>
    </article>
  )
}
