export type AppView = 'home' | 'catalog' | 'ai' | 'orders' | 'admin' | 'login'

/** Vistas que requieren sesión iniciada. */
export const PROTECTED_VIEWS: AppView[] = ['ai', 'orders', 'admin']

export const LOGIN_NOTICES: Partial<Record<AppView, string>> = {
  ai: 'Inicia sesión para consultar al asesor de IA.',
  orders: 'Inicia sesión para ver tus pedidos.',
  admin: 'Inicia sesión para entrar al panel de administración.',
}
