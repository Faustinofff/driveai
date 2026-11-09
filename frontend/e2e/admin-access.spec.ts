import { expect, test } from '@playwright/test'
import { loginViaUi, registerCustomer, seedSession, uniqueEmail } from './helpers'

test.describe('HU-ADMIN — Autorización y panel administrativo', () => {
  test('anónimo en #/admin es enviado al login', async ({ page }) => {
    await page.goto('/#/admin')
    await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
    await expect(
      page.getByText('Inicia sesión para entrar al panel de administración.'),
    ).toBeVisible()
  })

  test('CUSTOMER en #/admin ve Acceso restringido (no la tabla de administración)', async ({
    page,
    request,
  }) => {
    const session = await registerCustomer(request, 'customer-admin')
    await seedSession(page, session)

    await page.goto('/#/admin')
    await expect(page.getByRole('heading', { name: 'Acceso restringido' })).toBeVisible()
    await expect(
      page.getByRole('heading', { name: 'Panel de administración' }),
    ).toHaveCount(0)
  })

  test('ADMIN publica y elimina un auto desde el panel', async ({ page }) => {
    const adminEmail = process.env.ADMIN_EMAIL
    const adminPassword = process.env.ADMIN_PASSWORD
    test.skip(
      !adminEmail || !adminPassword,
      'ADMIN_EMAIL / ADMIN_PASSWORD no configurados: escenario admin omitido',
    )

    const model = `E2E-${Date.now().toString(36)}`
    await loginViaUi(page, adminEmail!, adminPassword!)
    await page.goto('/#/admin')
    await expect(page.getByRole('heading', { name: 'Panel de administración' })).toBeVisible()

    await page.getByRole('button', { name: 'Nuevo auto' }).click()
    const form = page.getByRole('dialog')
    await expect(form.getByRole('heading', { name: 'Publicar nuevo auto' })).toBeVisible()
    await form.getByLabel('Marca').fill('TestBrand')
    await form.getByLabel('Modelo').fill(model)
    await form.getByLabel('Año').fill('2024')
    await form.getByLabel('Precio (USD)').fill('19999')
    await form.getByLabel('Kilometraje').fill('100')
    await form.getByLabel('Stock').fill('2')
    await form.getByLabel('Consumo (L/100km)').fill('5.5')
    await form
      .getByLabel('URL de imagen')
      .fill('https://images.unsplash.com/photo-1553440569-bcc63803a83d?auto=format&fit=crop&w=1200&q=80')
    await form.getByLabel('Descripción').fill('Vehículo creado por la suite E2E.')
    await form.getByRole('button', { name: 'Publicar auto' }).click()

    const row = page.getByRole('row', { name: new RegExp(`TestBrand ${model}`) })
    await expect(row).toBeVisible()

    page.once('dialog', (dialog) => dialog.accept())
    await row.getByRole('button', { name: 'Eliminar' }).click()
    await expect(row).toHaveCount(0)
  })
})
