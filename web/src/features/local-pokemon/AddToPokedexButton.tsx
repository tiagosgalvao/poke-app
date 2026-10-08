import { Link } from 'react-router'
import { primaryButton } from '../../components/buttonStyles'
import { ErrorAlert } from '../../components/ErrorAlert'
import { isAuthenticated, useAuthStore } from '../auth/authStore'
import { useImportPokemon } from './localPokemonApi'

export function AddToPokedexButton({ name }: { name: string }) {
  const authenticated = useAuthStore(isAuthenticated)
  const importPokemon = useImportPokemon()

  if (!authenticated) {
    return (
      <Link className="text-sm font-medium text-sky-700 hover:underline" to={`/login?redirect=${encodeURIComponent(`/pokemon/${name}`)}`}>
        Log in to add it to My Pokedex
      </Link>
    )
  }
  if (importPokemon.isSuccess) {
    return (
      <p role="status" className="text-sm text-emerald-700">
        Added to your Pokedex.{' '}
        <Link className="font-medium underline" to="/my-pokedex">
          Open My Pokedex
        </Link>
      </p>
    )
  }
  return (
    <div className="flex flex-col items-start gap-2">
      <button type="button" className={primaryButton} disabled={importPokemon.isPending} onClick={() => importPokemon.mutate(name)}>
        {importPokemon.isPending ? 'Adding…' : 'Add to My Pokedex'}
      </button>
      {importPokemon.error && <ErrorAlert message={importPokemon.error.message} />}
    </div>
  )
}
