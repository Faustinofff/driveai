import { useState, type FormEvent } from 'react'
import { Car, Loader2, LogIn, UserPlus } from 'lucide-react'
import { useAuthStore } from '../store/authStore'
import type { ApiError } from '../services/api'

interface LoginPageProps {
  onSuccess: () => void
  notice?: string
}

type Mode = 'login' | 'register'

export function LoginPage({ onSuccess, notice }: LoginPageProps) {
  const login = useAuthStore((state) => state.login)
  const register = useAuthStore((state) => state.register)
  const user = useAuthStore((state) => state.user)

  const [mode, setMode] = useState<Mode>('login')
  const [fullName, setFullName] = useState('')
  const [email, setEmail] = useState('')
  const [password, setPassword] = useState('')
  const [busy, setBusy] = useState(false)
  const [error, setError] = useState<string | null>(null)

  if (user) {
    return (
      <div className="mx-auto flex max-w-md flex-col items-center px-4 py-16 text-center">
        <span className="flex h-14 w-14 items-center justify-center rounded-2xl bg-emerald-100 text-emerald-600">
          <LogIn className="h-7 w-7" />
        </span>
        <h1 className="mt-4 text-2xl font-extrabold tracking-tight">
          Ya iniciaste sesión
        </h1>
        <p className="mt-1 text-sm text-slate-500">
          Hola, <span className="font-semibold text-slate-700">{user.fullName}</span>. Ya puedes
          usar el carrito, comprar y preguntarle al asesor de IA.
        </p>
        <button
          type="button"
          onClick={onSuccess}
          className="mt-6 rounded-xl bg-brand-600 px-5 py-2.5 text-sm font-semibold text-white transition hover:bg-brand-700"
        >
          Volver al catálogo
        </button>
      </div>
    )
  }

  const handleSubmit = async (event: FormEvent) => {
    event.preventDefault()
    setBusy(true)
    setError(null)
    try {
      if (mode === 'login') {
        await login(email.trim(), password)
      } else {
        await register(email.trim(), password, fullName.trim())
      }
      onSuccess()
    } catch (err) {
      setError((err as ApiError).message)
    } finally {
      setBusy(false)
    }
  }

  return (
    <div className="mx-auto flex max-w-md flex-col px-4 py-10">
      <div className="mb-6 flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-brand-600 text-white shadow-lg shadow-brand-600/25">
          <Car className="h-6 w-6" />
        </span>
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Tu cuenta DriveAI</h1>
          <p className="text-sm text-slate-500">
            {mode === 'login'
              ? 'Ingresa para comprar y hablar con la IA.'
              : 'Crea tu cuenta para comprar y hablar con la IA.'}
          </p>
        </div>
      </div>

      {notice && (
        <p className="mb-4 rounded-xl border border-brand-200 bg-brand-50 px-3 py-2 text-sm text-brand-800">
          {notice}
        </p>
      )}

      <div className="relative mb-6 grid grid-cols-2 rounded-full border border-slate-200 bg-slate-100 p-1">
        <button
          type="button"
          onClick={() => {
            setMode('login')
            setError(null)
          }}
          className={`flex items-center justify-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition ${
            mode === 'login' ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-600 hover:text-slate-700'
          }`}
        >
          <LogIn className="h-4 w-4" />
          Ingresar
        </button>
        <button
          type="button"
          onClick={() => {
            setMode('register')
            setError(null)
          }}
          className={`flex items-center justify-center gap-1.5 rounded-full px-3 py-1.5 text-sm font-medium transition ${
            mode === 'register' ? 'bg-white text-brand-700 shadow-sm' : 'text-slate-600 hover:text-slate-700'
          }`}
        >
          <UserPlus className="h-4 w-4" />
          Crear cuenta
        </button>
      </div>

      <form
        onSubmit={(event) => void handleSubmit(event)}
        className="space-y-4 rounded-2xl border border-slate-200 bg-white p-6 shadow-sm"
      >
        {mode === 'register' && (
          <div>
            <label
              htmlFor="login-full-name"
              className="mb-1 block text-xs font-medium text-slate-500"
            >
              Nombre completo
            </label>
            <input
              id="login-full-name"
              type="text"
              value={fullName}
              onChange={(event) => setFullName(event.target.value)}
              placeholder="Ej. Ana Cliente"
              required
              className="input-base"
            />
          </div>
        )}

        <div>
          <label
            htmlFor="login-email"
            className="mb-1 block text-xs font-medium text-slate-500"
          >
            Correo electrónico
          </label>
          <input
            id="login-email"
            type="email"
            value={email}
            onChange={(event) => setEmail(event.target.value)}
            placeholder="tu@correo.com"
            required
            className="input-base"
          />
        </div>

        <div>
          <label
            htmlFor="login-password"
            className="mb-1 block text-xs font-medium text-slate-500"
          >
            Contraseña
          </label>
          <input
            id="login-password"
            type="password"
            value={password}
            onChange={(event) => setPassword(event.target.value)}
            placeholder={mode === 'register' ? 'Mínimo 8 caracteres' : 'Tu contraseña'}
            minLength={8}
            required
            className="input-base"
          />
        </div>

        {error && (
          <p className="rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700">
            {error}
          </p>
        )}

        <button
          type="submit"
          disabled={busy}
          className="inline-flex w-full items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-bold text-white shadow-lg shadow-brand-600/25 transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-60"
        >
          {busy && <Loader2 className="h-4 w-4 animate-spin" />}
          {busy
            ? 'Procesando…'
            : mode === 'login'
              ? 'Ingresar'
              : 'Crear cuenta'}
        </button>
      </form>
    </div>
  )
}
