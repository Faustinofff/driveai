import { expect, type APIRequestContext, type Page } from '@playwright/test'

export const API_URL = process.env.E2E_API_URL ?? 'http://localhost:8080'

/** Email único por ejecución para no colisionar con datos previos. */
export function uniqueEmail(prefix = 'e2e'): string {
  const stamp = Date.now().toString(36)
  const rand = Math.random().toString(36).slice(2, 8)
  return `${prefix}-${stamp}-${rand}@driveai.test`
}

export interface Session {
  email: string
  password: string
  fullName: string
  token: string
  userId: number
}

/**
 * Crea una cuenta CUSTOMER directamente contra la API. Se usa para preparar
 * estado sin depender del formulario (los tests de UI cubren el registro).
 */
export async function registerCustomer(
  request: APIRequestContext,
  prefix = 'e2e',
): Promise<Session> {
  const email = uniqueEmail(prefix)
  const password = 'E2ePassw0rd!'
  const fullName = 'Cliente E2E'
  const response = await request.post(`${API_URL}/api/auth/register`, {
    data: { email, password, fullName },
  })
  expect(response.status(), await response.text()).toBe(201)
  const body = await response.json()
  return { email, password, fullName, token: body.token, userId: body.user.id }
}

/** Inicia sesión por la UI y espera a que el navbar muestre la sesión. */
export async function loginViaUi(page: Page, email: string, password: string): Promise<void> {
  await page.goto('/#/login')
  await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
  await page.getByLabel('Correo electrónico').fill(email)
  await page.getByLabel('Contraseña').fill(password)
  await page.locator('form').getByRole('button', { name: 'Ingresar', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Cerrar sesión' }).first()).toBeVisible()
}

/** Registra una cuenta nueva por la UI (flujo completo del formulario). */
export async function registerViaUi(
  page: Page,
  email: string,
  password: string,
  fullName: string,
): Promise<void> {
  await page.goto('/#/login')
  await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
  await page.getByRole('button', { name: 'Crear cuenta' }).first().click()
  await page.getByLabel('Nombre completo').fill(fullName)
  await page.getByLabel('Correo electrónico').fill(email)
  await page.getByLabel('Contraseña').fill(password)
  await page.locator('form').getByRole('button', { name: 'Crear cuenta', exact: true }).click()
  await expect(page.getByRole('button', { name: 'Cerrar sesión' }).first()).toBeVisible()
}

/** Inyecta la sesión en localStorage (evita repetir el login en tests de flujo). */
export async function seedSession(page: Page, session: Session): Promise<void> {
  await page.addInitScript(
    ([token, email, fullName]) => {
      window.localStorage.setItem(
        'driveai-auth',
        JSON.stringify({
          state: {
            token,
            user: { id: 0, email, fullName, role: 'CUSTOMER' },
          },
          version: 0,
        }),
      )
    },
    [session.token, session.email, session.fullName] as const,
  )
}
