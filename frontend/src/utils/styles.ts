/**
 * Clases compartidas para que las superficies, avisos y botones se vean igual
 * en toda la app, en tema claro y oscuro. Tailwind las detecta porque viven en
 * un archivo incluido en `content` (tailwind.config.js).
 */

export const PANEL =
  'rounded-2xl border border-slate-200 bg-white shadow-sm dark:border-slate-800 dark:bg-slate-900'

export const SURFACE_MUTED = 'bg-slate-50 dark:bg-slate-800/60'

export const TEXT_MUTED = 'text-slate-500 dark:text-slate-400'
export const TEXT_STRONG = 'text-slate-900 dark:text-white'
export const TEXT_BRAND = 'text-brand-700 dark:text-brand-300'

export const ALERT_ERROR =
  'rounded-xl border border-rose-200 bg-rose-50 px-3 py-2 text-sm text-rose-700 dark:border-rose-500/40 dark:bg-rose-500/10 dark:text-rose-300'
export const ALERT_INFO =
  'rounded-xl border border-brand-200 bg-brand-50 px-3 py-2 text-sm text-brand-800 dark:border-brand-500/40 dark:bg-brand-500/10 dark:text-brand-200'
export const ALERT_WARNING =
  'rounded-xl border border-amber-200 bg-amber-50 px-3 py-2 text-sm text-amber-800 dark:border-amber-500/40 dark:bg-amber-500/10 dark:text-amber-200'

export const BTN_PRIMARY =
  'inline-flex items-center justify-center gap-2 rounded-xl bg-brand-600 px-4 py-2.5 text-sm font-semibold text-white shadow-md shadow-brand-600/20 transition hover:bg-brand-700 active:scale-[0.98] disabled:cursor-not-allowed disabled:opacity-60'
export const BTN_SECONDARY =
  'inline-flex items-center justify-center gap-2 rounded-xl border border-slate-300 bg-white px-4 py-2.5 text-sm font-semibold text-slate-700 shadow-sm transition hover:border-brand-400 hover:text-brand-700 disabled:cursor-not-allowed disabled:opacity-60 dark:border-slate-700 dark:bg-slate-900 dark:text-slate-200 dark:hover:border-brand-400 dark:hover:text-brand-300'
export const BTN_DANGER =
  'inline-flex items-center justify-center gap-2 rounded-xl border border-rose-200 bg-rose-50 px-4 py-2.5 text-sm font-semibold text-rose-700 transition hover:bg-rose-100 disabled:cursor-not-allowed disabled:opacity-60 dark:border-rose-500/40 dark:bg-rose-500/10 dark:text-rose-300 dark:hover:bg-rose-500/20'

export const ICON_BUTTON =
  'flex h-8 w-8 items-center justify-center rounded-full text-slate-500 transition hover:bg-slate-100 hover:text-slate-700 dark:text-slate-400 dark:hover:bg-slate-800 dark:hover:text-slate-100'

export const OVERLAY = 'absolute inset-0 bg-slate-900/60 backdrop-blur-sm'
