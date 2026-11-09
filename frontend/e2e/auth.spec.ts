import { expect, test } from '@playwright/test'
import { loginViaUi, registerCustomer, registerViaUi, uniqueEmail } from './helpers'

test.describe('HU-AUTH — Registro e inicio de sesión', () => {
  test('registro por UI, logout y login con las mismas credenciales', async ({ page }) => {
    const email = uniqueEmail('auth')
    const password = 'E2ePassw0rd!'

    await registerViaUi(page, email, password, 'Ana E2E')

    // La sesión quedó iniciada: el navbar muestra el botón de logout.
    await expect(page.getByRole('button', { name: 'Cerrar sesión' }).first()).toBeVisible()

    await page.getByRole('button', { name: 'Cerrar sesión' }).first().click()
    await expect(page.getByRole('button', { name: 'Ingresar' }).first()).toBeVisible()

    await loginViaUi(page, email, password)
    await expect(page.getByRole('button', { name: 'Cerrar sesión' }).first()).toBeVisible()
  })

  test('login con contraseña incorrecta muestra error y no autentica', async ({ page, request }) => {
    const session = await registerCustomer(request, 'auth-bad')

    await page.goto('/#/login')
    await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
    await page.getByLabel('Correo electrónico').fill(session.email)
    await page.getByLabel('Contraseña').fill('PasswordIncorrecta!')
    await page.locator('form').getByRole('button', { name: 'Ingresar', exact: true }).click()

    await expect(page.getByText('Email o contraseña incorrectos.')).toBeVisible()
    await expect(page.getByRole('button', { name: 'Cerrar sesión' })).toHaveCount(0)
  })

  test('acceso anónimo a una vista protegida redirige al login con aviso', async ({ page }) => {
    await page.goto('/#/ai')
    await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
    await expect(
      page.getByText('Inicia sesión para consultar al asesor de IA.'),
    ).toBeVisible()
  })
})
