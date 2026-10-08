import { zodResolver } from '@hookform/resolvers/zod'
import { useMutation } from '@tanstack/react-query'
import { useForm } from 'react-hook-form'
import { Link, useSearchParams } from 'react-router'
import { primaryButton } from '../../components/buttonStyles'
import { ErrorAlert } from '../../components/ErrorAlert'
import { FormField } from '../../components/FormField'
import { register as registerAccount } from './authApi'
import { registerSchema, type RegisterForm } from './schemas'
import { useSessionLogin } from './useSessionLogin'

export function RegisterPage() {
  const [searchParams] = useSearchParams()
  const sessionLogin = useSessionLogin()
  const registration = useMutation({
    mutationFn: registerAccount,
    onSuccess: (_user, form) => sessionLogin.mutate({ username: form.username, password: form.password }),
  })
  const {
    register,
    handleSubmit,
    formState: { errors },
  } = useForm<RegisterForm>({ resolver: zodResolver(registerSchema) })
  const failure = registration.error ?? sessionLogin.error
  const pending = registration.isPending || sessionLogin.isPending

  return (
    <section className="mx-auto w-full max-w-sm">
      <h2 className="mb-6 text-2xl font-semibold">Create an account</h2>
      <form className="flex flex-col gap-4" noValidate onSubmit={handleSubmit((form) => registration.mutate(form))}>
        {failure && <ErrorAlert message={failure.message} />}
        <FormField label="Username" autoComplete="username" error={errors.username?.message} {...register('username')} />
        <FormField label="Email" type="email" autoComplete="email" error={errors.email?.message} {...register('email')} />
        <FormField
          label="Password"
          type="password"
          autoComplete="new-password"
          error={errors.password?.message}
          {...register('password')}
        />
        <button type="submit" className={primaryButton} disabled={pending}>
          {pending ? 'Creating account…' : 'Create account'}
        </button>
      </form>
      <p className="mt-4 text-sm text-slate-600">
        Already registered?{' '}
        <Link className="font-medium text-sky-700 hover:underline" to={`/login?${searchParams.toString()}`}>
          Log in
        </Link>
      </p>
    </section>
  )
}
