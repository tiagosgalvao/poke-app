import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useNavigate, useParams } from 'react-router'
import { ApiError } from '../../api/client'
import type { LocalPokemon } from '../../api/types'
import { primaryButton, secondaryButton } from '../../components/buttonStyles'
import { ErrorAlert } from '../../components/ErrorAlert'
import { ErrorState } from '../../components/ErrorState'
import { FormField } from '../../components/FormField'
import { displayName } from '../../components/format'
import { useLocalPokemon, useUpdateLocalPokemon } from './localPokemonApi'
import { editSchema, parseTags, type EditForm } from './schemas'

const CONFLICT = 409
const EDITABLE_FIELDS: (keyof EditForm)[] = ['localizedName', 'region', 'habitat', 'tags', 'notes']

function toForm(pokemon: LocalPokemon): EditForm {
  return {
    localizedName: pokemon.localizedName ?? '',
    region: pokemon.region ?? '',
    habitat: pokemon.habitat ?? '',
    tags: pokemon.tags.join(', '),
    notes: pokemon.notes ?? '',
  }
}

function blankToNull(value: string) {
  return value.trim() === '' ? null : value.trim()
}

export function EditLocalPokemonPage() {
  const id = Number(useParams().id)
  const navigate = useNavigate()
  const current = useLocalPokemon(id)
  const update = useUpdateLocalPokemon(id)
  const {
    register,
    handleSubmit,
    setError,
    formState: { errors },
  } = useForm<EditForm>({ resolver: zodResolver(editSchema), values: current.data ? toForm(current.data) : undefined })

  if (current.isPending) {
    return <div aria-busy="true" className="h-64 animate-pulse rounded-xl bg-white" />
  }
  if (current.isError) {
    return <ErrorState message={current.error.message} onRetry={() => current.refetch()} />
  }

  const save = (form: EditForm) =>
    update.mutate(
      {
        version: current.data.version,
        localizedName: blankToNull(form.localizedName),
        region: blankToNull(form.region),
        habitat: blankToNull(form.habitat),
        tags: parseTags(form.tags),
        notes: blankToNull(form.notes),
      },
      {
        onSuccess: () => navigate('/my-pokedex'),
        onError: (error) => {
          if (error instanceof ApiError) {
            error.fieldErrors.forEach(({ field, message }) => {
              const formField = EDITABLE_FIELDS.find((name) => field.startsWith(name))
              if (formField) {
                setError(formField, { message })
              }
            })
          }
        },
      },
    )
  const conflict = update.error instanceof ApiError && update.error.status === CONFLICT

  return (
    <section className="mx-auto w-full max-w-xl">
      <h2 className="mb-1 text-2xl font-semibold">Edit {displayName(current.data.name)}</h2>
      <p className="mb-6 text-sm text-slate-500">Proprietary data stored only in this app (version {current.data.version}).</p>
      <form className="flex flex-col gap-4" noValidate onSubmit={handleSubmit(save)}>
        {update.error && (update.error instanceof ApiError ? update.error.fieldErrors.length === 0 : true) && (
          <div className="flex flex-col gap-2">
            <ErrorAlert message={update.error.message} />
            {conflict && (
              <button
                type="button"
                className={secondaryButton}
                onClick={() => {
                  update.reset()
                  current.refetch()
                }}
              >
                Reload latest
              </button>
            )}
          </div>
        )}
        <FormField label="Localized name" error={errors.localizedName?.message} {...register('localizedName')} />
        <FormField label="Region" error={errors.region?.message} {...register('region')} />
        <FormField label="Habitat" error={errors.habitat?.message} {...register('habitat')} />
        <FormField label="Tags (comma separated)" error={errors.tags?.message} {...register('tags')} />
        <FormField label="Notes" error={errors.notes?.message} {...register('notes')} />
        <div className="flex gap-3">
          <button type="submit" className={primaryButton} disabled={update.isPending}>
            {update.isPending ? 'Saving…' : 'Save'}
          </button>
          <Link to="/my-pokedex" className={secondaryButton}>
            Cancel
          </Link>
        </div>
      </form>
    </section>
  )
}
