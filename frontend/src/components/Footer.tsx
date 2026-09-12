import { Car, Facebook, Instagram, Linkedin, Mail, MapPin, Phone, Twitter } from 'lucide-react'
import type { AppView } from '../types/view'

interface FooterProps {
  onNavigate: (view: AppView) => void
}

const NAV_LINKS: { label: string; view: AppView }[] = [
  { label: 'Inicio', view: 'home' },
  { label: 'Productos', view: 'catalog' },
  { label: 'Asesor IA', view: 'ai' },
]

const SOCIALS = [
  { label: 'Instagram', Icon: Instagram },
  { label: 'Facebook', Icon: Facebook },
  { label: 'X', Icon: Twitter },
  { label: 'LinkedIn', Icon: Linkedin },
]

const INFO_PLACEHOLDERS = [
  { label: 'Sobre nosotros', target: 'home' },
  { label: 'Contacto', href: 'mailto:hola@driveai.com' },
  { label: 'Términos y condiciones', href: '#' },
  { label: 'Política de privacidad', href: '#' },
] as const

export function Footer({ onNavigate }: FooterProps) {
  return (
    <footer className="border-t border-slate-200 bg-white transition-colors duration-300 dark:border-slate-800 dark:bg-slate-900">
      <div className="mx-auto max-w-7xl px-4 py-12 sm:px-6 lg:px-8">
        <div className="grid grid-cols-1 gap-10 sm:grid-cols-2 lg:grid-cols-4">
          <div className="sm:col-span-2 lg:col-span-1">
            <a
              href="/"
              className="flex items-center gap-2 font-bold tracking-tight"
              onClick={(event) => {
                event.preventDefault()
                onNavigate('home')
              }}
            >
              <span className="flex h-9 w-9 items-center justify-center rounded-xl bg-brand-600 text-white shadow-md shadow-brand-600/30">
                <Car className="h-5 w-5" />
              </span>
              <span className="text-lg">
                Drive<span className="text-brand-600">AI</span>
              </span>
            </a>
            <p className="mt-4 max-w-xs text-sm leading-relaxed text-slate-500 dark:text-slate-400">
              E-commerce de autos certificados con asesor de compra inteligente. Unidades reales,
              stock transparente y una experiencia pensada para decidir tranquilo.
            </p>
            <div className="mt-5 flex gap-2">
              {SOCIALS.map(({ label, Icon }) => (
                <a
                  key={label}
                  href="#"
                  title={label}
                  aria-label={label}
                  className="flex h-9 w-9 items-center justify-center rounded-full border border-slate-200 text-slate-500 transition hover:border-brand-400 hover:bg-brand-50 hover:text-brand-700 dark:border-slate-700 dark:text-slate-400 dark:hover:border-brand-400 dark:hover:bg-slate-800 dark:hover:text-brand-300"
                >
                  <Icon className="h-4 w-4" />
                </a>
              ))}
            </div>
          </div>

          <div>
            <h3 className="text-sm font-bold uppercase tracking-wide text-slate-800 dark:text-slate-100">
              Navegación
            </h3>
            <ul className="mt-4 space-y-2.5">
              {NAV_LINKS.map((link) => (
                <li key={link.label}>
                  <button
                    type="button"
                    onClick={() => onNavigate(link.view)}
                    className="text-sm text-slate-500 transition hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300"
                  >
                    {link.label}
                  </button>
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-bold uppercase tracking-wide text-slate-800 dark:text-slate-100">
              Información
            </h3>
            <ul className="mt-4 space-y-2.5">
              {INFO_PLACEHOLDERS.map((item) => (
                <li key={item.label}>
                  {'target' in item ? (
                    <button
                      type="button"
                      onClick={() => onNavigate('home')}
                      className="text-sm text-slate-500 transition hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300"
                    >
                      {item.label}
                    </button>
                  ) : (
                    <a
                      href={item.href}
                      className="text-sm text-slate-500 transition hover:text-brand-700 dark:text-slate-400 dark:hover:text-brand-300"
                    >
                      {item.label}
                    </a>
                  )}
                </li>
              ))}
            </ul>
          </div>

          <div>
            <h3 className="text-sm font-bold uppercase tracking-wide text-slate-800 dark:text-slate-100">
              Contacto
            </h3>
            <ul className="mt-4 space-y-3 text-sm text-slate-500 dark:text-slate-400">
              <li className="flex items-center gap-2.5">
                <MapPin className="h-4 w-4 shrink-0 text-brand-600 dark:text-brand-400" />
                Buenos Aires, Argentina
              </li>
              <li className="flex items-center gap-2.5">
                <Mail className="h-4 w-4 shrink-0 text-brand-600 dark:text-brand-400" />
                hola@driveai.com
              </li>
              <li className="flex items-center gap-2.5">
                <Phone className="h-4 w-4 shrink-0 text-brand-600 dark:text-brand-400" />
                +54 11 5555 0100
              </li>
            </ul>
          </div>
        </div>

        <div className="mt-10 flex flex-col items-center justify-between gap-3 border-t border-slate-200 pt-6 text-center text-xs text-slate-400 dark:border-slate-800 dark:text-slate-500 sm:flex-row sm:text-left">
          <span>© 2026 DriveAI. Todos los derechos reservados.</span>
          <span className="flex items-center gap-1.5">
            Hecho con
            <span className="text-brand-600 dark:text-brand-400">IA</span>
            para que decidas mejor
          </span>
        </div>
      </div>
    </footer>
  )
}