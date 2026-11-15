import { expect, test } from '@playwright/test'
import { API_URL, registerCustomer, seedSession } from './helpers'

test.describe('HU-ORD — Mis pedidos y cancelación', () => {
  test('tras el checkout el pedido aparece en Mis pedidos y se puede cancelar una sola vez', async ({
    page,
    request,
  }) => {
    const session = await registerCustomer(request, 'orders')
    await seedSession(page, session)

    await page.goto('/#/catalog')
    await page.getByRole('button', { name: 'Agregar al carrito' }).first().click()
    await page.getByRole('button', { name: 'Abrir carrito' }).click()
    await page.getByRole('dialog').getByRole('button', { name: 'Finalizar compra' }).click()
    await expect(page.getByRole('heading', { name: '¡Pedido confirmado!' })).toBeVisible()

    // El modal de confirmación enlaza directo con el historial.
    await page.getByRole('button', { name: 'Ver mis pedidos' }).click()
    await expect(page.getByRole('heading', { name: 'Mis pedidos' })).toBeVisible()
    await expect(page).toHaveURL(/#\/orders$/)

    const order = page.getByRole('article', { name: /Orden #\d+/ }).first()
    await expect(order).toBeVisible()
    await expect(order.getByText('Confirmada')).toBeVisible()

    // Cancelación con confirmación inline (sin window.confirm).
    await order.getByRole('button', { name: 'Cancelar pedido' }).click()
    await order.getByRole('button', { name: 'Sí, cancelar' }).click()
    await expect(order.getByText('Cancelada')).toBeVisible()
    await expect(order.getByRole('button', { name: 'Cancelar pedido' })).toHaveCount(0)

    // El backend coincide con lo que muestra la UI: la orden quedó CANCELLED.
    const listed = await request.get(`${API_URL}/api/orders`, {
      headers: { Authorization: `Bearer ${session.token}` },
    })
    expect(listed.status()).toBe(200)
    const orders = (await listed.json()) as { status: string }[]
    expect(orders).toHaveLength(1)
    expect(orders[0].status).toBe('CANCELLED')
  })

  test('anónimo en #/orders es enviado al login con aviso', async ({ page }) => {
    await page.goto('/#/orders')
    await expect(page.getByRole('heading', { name: 'Tu cuenta DriveAI' })).toBeVisible()
    await expect(page.getByText('Inicia sesión para ver tus pedidos.')).toBeVisible()
  })

  test('sin pedidos se muestra el estado vacío con acceso al catálogo', async ({
    page,
    request,
  }) => {
    await seedSession(page, await registerCustomer(request, 'orders-empty'))
    await page.goto('/#/orders')
    await expect(page.getByText('Todavía no hiciste ningún pedido')).toBeVisible()
    await page.getByRole('button', { name: 'Ir al catálogo' }).click()
    await expect(
      page.getByRole('heading', { name: 'Catálogo de autos certificados' }),
    ).toBeVisible()
  })
})
