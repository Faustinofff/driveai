import { useEffect, useRef, useState } from 'react'
import { AlertTriangle, Bot, Check, Plus, RefreshCw, Send, User } from 'lucide-react'
import { recommendCars } from '../services/api'
import type { ApiError } from '../services/api'
import { selectSessionId, useCartStore } from '../store/cartStore'
import type { Car } from '../types'
import { FUEL_COLORS, FUEL_LABELS } from '../types'
import { formatMoney } from '../utils/formatters'

type ErrorKind = 'rate-limit' | 'busy' | 'network' | 'bad-request'
type MessageKind = ErrorKind | 'success'

interface ChatMessage {
  id: string
  role: 'user' | 'assistant'
  content: string
  reasoning?: string
  cars?: Car[]
  kind?: MessageKind
  retryPrompt?: string
}

const QUICK_SUGGESTIONS = [
  'Primer auto bueno y barato (menos de $20,000) que gaste poco',
  'SUV familiar espaciosa e híbrida',
  'Auto automático con poco kilometraje para ciudad',
  'Eléctrico para ahorrar en combustible',
]

const KIND_MESSAGES: Record<ErrorKind, string> = {
  'rate-limit':
    'Alcanzaste el límite de consultas del asesor por minuto. Espera unos segundos y vuelve a intentarlo.',
  busy: 'El servicio de IA está saturado en este momento. Inténtalo de nuevo en unos segundos.',
  network: 'No se pudo conectar con el servidor. Verifica tu conexión y reintenta.',
  'bad-request': 'El asesor no pudo procesar tu consulta. Reformúlala e inténtalo de nuevo.',
}

function uid(): string {
  if (typeof crypto !== 'undefined' && 'randomUUID' in crypto) {
    return crypto.randomUUID()
  }
  return `m-${Date.now()}-${Math.random().toString(36).slice(2, 8)}`
}

function kindFromError(error: ApiError): ErrorKind {
  if (error.status === 429) return 'rate-limit'
  if (error.status === 503) return 'busy'
  if (error.status === undefined || error.message.includes('conectar')) return 'network'
  return 'bad-request'
}

function SuggestionCard({ car, onAdd }: { car: Car; onAdd: (car: Car) => void }) {
  const [justAdded, setJustAdded] = useState(false)

  const handleAdd = () => {
    onAdd(car)
    setJustAdded(true)
    window.setTimeout(() => setJustAdded(false), 1200)
  }

  return (
    <article className="flex overflow-hidden rounded-xl border border-slate-200 bg-white shadow-sm">
      <img
        src={car.imageUrl}
        alt={`${car.brand} ${car.model}`}
        loading="lazy"
        className="h-24 w-24 shrink-0 object-cover sm:h-28 sm:w-28"
      />
      <div className="flex flex-1 flex-col justify-between gap-1 p-2.5">
        <div>
          <p className="text-sm font-bold leading-tight text-slate-900">
            {car.brand} {car.model}
          </p>
          <span className={`mt-1 inline-block rounded-full border px-2 py-0.5 text-[10px] font-semibold ${FUEL_COLORS[car.fuelType]}`}>
            {FUEL_LABELS[car.fuelType]}
          </span>
        </div>
        <div className="flex items-center justify-between gap-2">
          <span className="text-sm font-extrabold text-brand-700">{formatMoney(car.price)}</span>
          <button
            type="button"
            disabled={car.stock <= 0}
            onClick={handleAdd}
            className={`inline-flex items-center gap-1 rounded-lg px-2.5 py-1.5 text-xs font-semibold transition ${
              justAdded ? 'bg-emerald-600 text-white' : 'bg-brand-600 text-white hover:bg-brand-700'
            }`}
          >
            {justAdded ? <Check className="h-3.5 w-3.5" /> : <Plus className="h-3.5 w-3.5" />}
            {justAdded ? 'Agregado' : 'Carrito'}
          </button>
        </div>
      </div>
    </article>
  )
}

function ThinkingBubble() {
  return (
    <div className="flex items-start gap-3 animate-fade-in-up">
      <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-md">
        <Bot className="h-5 w-5" />
      </span>
      <div className="flex items-center gap-1.5 rounded-2xl rounded-tl-sm bg-white px-4 py-3.5 shadow-sm ring-1 ring-slate-200">
        <span className="h-2 w-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: '0ms' }} />
        <span className="h-2 w-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: '150ms' }} />
        <span className="h-2 w-2 rounded-full bg-brand-400 animate-bounce" style={{ animationDelay: '300ms' }} />
      </div>
    </div>
  )
}

export function AiChat() {
  const sessionId = useCartStore(selectSessionId)
  const add = useCartStore((state) => state.add)
  const [messages, setMessages] = useState<ChatMessage[]>([])
  const [input, setInput] = useState('')
  const [thinking, setThinking] = useState(false)
  const bottomRef = useRef<HTMLDivElement | null>(null)

  useEffect(() => {
    bottomRef.current?.scrollIntoView({ behavior: 'smooth' })
  }, [messages, thinking])

  const send = async (rawPrompt: string, quiet = false) => {
    const prompt = rawPrompt.trim()
    if (!prompt || thinking) return

    if (!quiet) {
      setMessages((previous) => [
        ...previous,
        { id: uid(), role: 'user', content: prompt },
      ])
    }
    setInput('')
    setThinking(true)

    try {
      const result = await recommendCars({ prompt, sessionId })
      setMessages((previous) => [
        ...previous,
        {
          id: uid(),
          role: 'assistant',
          content: result.reasoning ?? 'Aquí tienes mis recomendaciones:',
          reasoning: result.reasoning ?? undefined,
          cars: result.recommendations,
          kind: 'success',
        },
      ])
    } catch (error) {
      const apiError = error as ApiError
      const kind = kindFromError(apiError)
      setMessages((previous) => [
        ...previous,
        {
          id: uid(),
          role: 'assistant',
          content: KIND_MESSAGES[kind],
          kind,
          retryPrompt: prompt,
        },
      ])
    } finally {
      setThinking(false)
    }
  }

  const handleSubmit = (event: React.FormEvent<HTMLFormElement>) => {
    event.preventDefault()
    void send(input)
  }

  return (
    <div className="flex h-[calc(100vh-9.5rem)] min-h-[480px] flex-col rounded-2xl border border-slate-200 bg-white shadow-sm">
      <div className="flex-1 space-y-4 overflow-y-auto p-4 scrollbar-none sm:p-6">
        {messages.length === 0 && (
          <div className="animate-fade-in-up rounded-2xl border border-dashed border-brand-200 bg-brand-50/60 p-5 text-center">
            <Bot className="mx-auto h-8 w-8 text-brand-600" />
            <p className="mt-2 font-semibold text-slate-800">
              Hola, soy tu asesor de compra.
            </p>
            <p className="mx-auto mt-1 max-w-xl text-sm text-slate-600">
              Descríbeme lo que buscas y te recomendaré los autos ideales de nuestro
              inventario real. Elige una sugerencia o escribe tu propia consulta.
            </p>
          </div>
        )}

        {messages.map((message) => {
          if (message.role === 'user') {
            return (
              <div key={message.id} className="flex justify-end animate-fade-in-up">
                <div className="flex max-w-[85%] items-end gap-2">
                  <div className="rounded-2xl rounded-br-sm bg-brand-600 px-4 py-2.5 text-sm text-white shadow-md shadow-brand-600/15">
                    {message.content}
                  </div>
                  <span className="flex h-8 w-8 shrink-0 items-center justify-center rounded-full bg-brand-100 text-brand-700">
                    <User className="h-4 w-4" />
                  </span>
                </div>
              </div>
            )
          }

          if (message.kind && message.kind !== 'success') {
            return (
              <div key={message.id} className="flex items-start gap-3 animate-fade-in-up">
                <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-md">
                  <Bot className="h-5 w-5" />
                </span>
                <div className="max-w-[85%] rounded-2xl rounded-tl-sm border border-amber-200 bg-amber-50 px-4 py-3 shadow-sm">
                  <div className="flex items-start gap-2">
                    <AlertTriangle className="mt-0.5 h-4 w-4 shrink-0 text-amber-600" />
                    <p className="text-sm text-amber-800">{message.content}</p>
                  </div>
                  {message.retryPrompt && (
                    <button
                      type="button"
                      onClick={() => void send(message.retryPrompt ?? '', true)}
                      className="mt-3 inline-flex items-center gap-1.5 rounded-lg bg-amber-600 px-3 py-1.5 text-xs font-semibold text-white transition hover:bg-amber-700"
                    >
                      <RefreshCw className="h-3.5 w-3.5" />
                      Reintentar
                    </button>
                  )}
                </div>
              </div>
            )
          }

          return (
            <div key={message.id} className="flex items-start gap-3 animate-fade-in-up">
              <span className="flex h-9 w-9 shrink-0 items-center justify-center rounded-full bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-md">
                <Bot className="h-5 w-5" />
              </span>
              <div className="max-w-[85%] space-y-3">
                {message.content && (
                  <div className="rounded-2xl rounded-tl-sm bg-slate-50 px-4 py-3 text-sm text-slate-800 shadow-sm ring-1 ring-slate-200">
                    <p className="whitespace-pre-line leading-relaxed">{message.content}</p>
                  </div>
                )}
                {message.cars && message.cars.length > 0 && (
                  <div className="grid gap-2.5 sm:grid-cols-2">
                    {message.cars.slice(0, 4).map((car) => (
                      <SuggestionCard key={car.id} car={car} onAdd={add} />
                    ))}
                  </div>
                )}
                {message.cars && message.cars.length === 0 && (
                  <p className="rounded-2xl rounded-tl-sm bg-slate-50 px-4 py-3 text-sm text-slate-500 shadow-sm ring-1 ring-slate-200">
                    No encontré vehículos que coincidan exactamente con tu consulta. Prueba
                    relajar algún criterio (precio, tipo, combustible).
                  </p>
                )}
              </div>
            </div>
          )
        })}

        {thinking && <ThinkingBubble />}
        <div ref={bottomRef} />
      </div>

      <div className="border-t border-slate-200 p-3 sm:p-4">
        <div className="mb-3 flex flex-wrap gap-2">
          {QUICK_SUGGESTIONS.map((suggestion) => (
            <button
              key={suggestion}
              type="button"
              disabled={thinking}
              onClick={() => void send(suggestion)}
              className="rounded-full border border-brand-200 bg-brand-50 px-3 py-1.5 text-xs font-medium text-brand-700 transition hover:bg-brand-100 disabled:cursor-not-allowed disabled:opacity-50"
            >
              {suggestion}
            </button>
          ))}
        </div>

        <form onSubmit={handleSubmit} className="flex items-center gap-2">
          <input
            type="text"
            value={input}
            onChange={(event) => setInput(event.target.value)}
            placeholder="Describe el auto que buscas…"
            disabled={thinking}
            className="input-base"
          />
          <button
            type="submit"
            disabled={!input.trim() || thinking}
            className="inline-flex h-10 shrink-0 items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700 disabled:cursor-not-allowed disabled:opacity-50"
          >
            {thinking ? 'Enviando…' : 'Enviar'}
            <Send className="h-4 w-4" />
          </button>
        </form>
      </div>
    </div>
  )
}