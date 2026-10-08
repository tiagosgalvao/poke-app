import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import type { SyncSummary } from '../../api/types'
import { primaryButton } from '../../components/buttonStyles'
import { ErrorAlert } from '../../components/ErrorAlert'
import { FormField } from '../../components/FormField'
import { useSyncPokemon } from './localPokemonApi'
import { parseIds, syncSchema, type SyncForm } from './schemas'

function describe(summary: SyncSummary) {
  const failed = summary.failed.length > 0 ? ` (${summary.failed.join(', ')})` : ''
  return `${summary.created.length} created, ${summary.refreshed.length} refreshed, ${summary.failed.length} failed${failed}`
}

export function SyncPanel() {
  const sync = useSyncPokemon()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<SyncForm>({ resolver: zodResolver(syncSchema) })

  return (
    <section className="rounded-xl border border-slate-200 bg-white p-4">
      <h3 className="mb-1 font-semibold">Sync from PokeAPI</h3>
      <p className="mb-3 text-sm text-slate-500">
        Creates missing Pokemon and refreshes existing ones. Your proprietary data is never overwritten.
      </p>
      <form className="flex flex-wrap items-end gap-3" noValidate onSubmit={handleSubmit((form) => sync.mutate(parseIds(form.ids)))}>
        <div className="min-w-60 flex-1">
          <FormField label="Pokemon ids" placeholder="1, 4, 7" error={errors.ids?.message} {...register('ids')} />
        </div>
        <button type="submit" className={primaryButton} disabled={sync.isPending}>
          {sync.isPending ? 'Syncing…' : 'Sync'}
        </button>
      </form>
      {sync.error && (
        <div className="mt-3">
          <ErrorAlert message={sync.error.message} />
        </div>
      )}
      {sync.data && (
        <p role="status" className="mt-3 text-sm text-emerald-700">
          {describe(sync.data)}
        </p>
      )}
    </section>
  )
}
