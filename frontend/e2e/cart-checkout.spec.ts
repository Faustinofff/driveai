import { expect, test } from '@playwright/test'
import { registerCustomer, seedSession } from './helpers'

test.describe('HU-CART — Carrito y checkout', () => {
  test('agrega un vehículo al carrito, finaliza la compra y confirma la orden', async ({
    page,
    request,
  }) => {
    const session = await registerCustomer(request, 'cart')
    await seedSession(page, session)

    await page.goto('/#/catalog')
    await expect(page.getByRole('button', { name: /Ver detalle de / }).first()).toBeVisible()

    const addButtons = page.getByRole('button', { name: 'Agregar al carrito' })
    await addButtons.first().click()

    // El badge del carrito refleja la unidad agregada.
    await expect(page.getByRole('button', { name: 'Abrir carrito' })).toBeVisible()
    await page.getByRole('button', { name: 'Abrir carrito' }).click()

    const cart = page.getByRole('dialog')
    await expect(cart.getByRole('heading', { name: /Mi carrito/ })).toBeVisible()
    await expect(cart.getByRole('button', { name: 'Finalizar compra' })).toBeVisible()

    await cart.getByRole('button', { name: 'Finalizar compra' }).click()

    await expect(page.getByRole('heading', { name: '¡Pedido confirmado!' })).toBeVisible()
    await expect(page.getByText(/Estado:\s*CONFIRMED/)).toBeVisible()

    await page.getByRole('button', { name: 'Seguir comprando' }).click()
    await expect(page.getByRole('heading', { name: '¡Pedido confirmado!' })).toBeHidden()
  })

  test('el checkout exige autenticación: usuario anónimo es enviado al login', async ({ page }) => {
    await page.goto('/#/catalog')
    await page.getByRole('button', { name: 'Agregar al carrito' }).first().click()
    await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
    await expect(page.getByText('Inicia sesión para comprar en DriveAI.')).toBeVisible()
  })
})
