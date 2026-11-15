import { expect, test } from '@playwright/test'
import { API_URL, registerCustomer, seedSession } from './helpers'

test.describe('HU-CART — Carrito y checkout', () => {
  test('recorrido del manual: detalle, cantidad y eliminacion de una linea', async ({ page, request }) => {
    const email = process.env.ADMIN_EMAIL
    const password = process.env.ADMIN_PASSWORD
    test.skip(!email || !password, 'Requiere admin del entorno de demo')
    const login = await request.post(`${API_URL}/api/auth/login`, { data: { email, password } })
    expect(login.status()).toBe(200)
    const { token } = await login.json()
    const model = `Manual-${Date.now().toString(36)}`
    const created = await request.post(`${API_URL}/api/cars`, {
      headers: { Authorization: `Bearer ${token}` },
      data: { brand: 'ManualTest', model, year: 2024, price: 10000, mileage: 1,
        fuelType: 'GASOLINA', fuelConsumption: 5, transmission: 'MANUAL', bodyType: 'SEDAN',
        imageUrl: 'https://img.test/manual.jpg', description: 'Recorrido del manual', stock: 3 },
    })
    expect(created.status()).toBe(201)
    const car = await created.json()
    try {
      await seedSession(page, await registerCustomer(request, 'manual'))
      await page.goto('/?brand=ManualTest#/catalog')
      await page.getByRole('button', { name: `Ver detalle de ManualTest ${model}`, exact: true }).click()
      const detail = page.getByRole('dialog')
      await expect(detail).toContainText('Recorrido del manual')
      await detail.getByRole('button', { name: 'Agregar al carrito', exact: true }).click()
      await detail.getByRole('button', { name: 'Volver al catálogo' }).click()
      await page.getByRole('button', { name: 'Abrir carrito' }).click()
      const cart = page.getByRole('dialog')
      const quantity = cart.getByRole('button', { name: 'Disminuir cantidad' }).locator('..').locator('span')
      await cart.getByRole('button', { name: 'Aumentar cantidad' }).click()
      await expect(quantity).toHaveText('2')
      await cart.getByRole('button', { name: 'Disminuir cantidad' }).click()
      await expect(quantity).toHaveText('1')
      await cart.getByRole('button', { name: 'Quitar del carrito' }).click()
      await expect(cart.getByRole('button', { name: 'Finalizar compra' })).toBeHidden()
    } finally {
      const deleted = await request.delete(`${API_URL}/api/cars/${car.id}`, {
        headers: { Authorization: `Bearer ${token}` },
      })
      expect(deleted.status()).toBe(204)
    }
  })
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
