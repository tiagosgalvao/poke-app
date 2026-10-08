import { zodResolver } from '@hookform/resolvers/zod'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router'
import { primaryButton } from '../../components/buttonStyles'
import { ErrorAlert } from '../../components/ErrorAlert'
import { FormField } from '../../components/FormField'
import { loginSchema, type LoginForm } from './schemas'
import { useSessionLogin } from './useSessionLogin'

export function LoginPage() {
  const [searchParams] = useSearchParams()
  const sessionLogin = useSessionLogin()
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<LoginForm>({ resolver: zodResolver(loginSchema) })

  return (
    <section className="mx-auto w-full max-w-sm">
      <h2 className="mb-6 text-2xl font-semibold">Log in</h2>
      <form className="flex flex-col gap-4" noValidate onSubmit={handleSubmit((form) => sessionLogin.mutate(form))}>
        {sessionLogin.error && <ErrorAlert message={sessionLogin.error.message} />}
        <FormField label="Username" autoComplete="username" error={errors.username?.message} {...register('username')} />
        <FormField
          label="Password"
          type="password"
          autoComplete="current-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <button type="submit" className={primaryButton} disabled={sessionLogin.isPending}>
          {sessionLogin.isPending ? 'Logging in…' : 'Log in'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        No account yet?{' '}
        <Link className="font-medium text-sky-700 hover:underline" to={`/register?${searchParams.toString()}`}>
          Create one
        </Link>
      </p>
    </section>
  )
}
