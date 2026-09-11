import { Sparkles } from 'lucide-react'
import { AiChat } from '../components/AiChat'

export function AiConciergePage() {
  return (
    <div className="mx-auto max-w-4xl px-4 py-6">
      <div className="mb-5 flex items-center gap-3">
        <span className="flex h-11 w-11 items-center justify-center rounded-2xl bg-gradient-to-br from-brand-500 to-brand-700 text-white shadow-lg shadow-brand-600/25">
          <Sparkles className="h-6 w-6" />
        </span>
        <div>
          <h1 className="text-2xl font-extrabold tracking-tight">Asesor de compra con IA</h1>
          <p className="text-sm text-slate-500">
            Recomendaciones reales de nuestro inventario, generadas con Groq Cloud.
          </p>
        </div>
      </div>

      <AiChat />
    </div>
  )
}