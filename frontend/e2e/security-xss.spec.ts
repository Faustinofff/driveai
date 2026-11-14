import { expect, test } from '@playwright/test'
import { API_URL, registerCustomer, seedSession } from './helpers'

test('HTML de catálogo y respuesta IA se muestran como texto, sin ejecutar scripts', async ({ page, request }) => {
  const email = process.env.ADMIN_EMAIL
  const password = process.env.ADMIN_PASSWORD
  test.skip(!email || !password, 'Requiere credenciales admin del entorno de prueba')
  const login = await request.post(`${API_URL}/api/auth/login`, { data: { email, password } })
  expect(login.status()).toBe(200)
  const { token } = await login.json()
  const payload = '<img src=x onerror="window.__driveaiXss=1"><script>window.__driveaiXss=2</script>'
  const model = `XSS-${Date.now().toString(36)}`
  const created = await request.post(`${API_URL}/api/cars`, {
    headers: { Authorization: `Bearer ${token}` },
    data: { brand: 'SecurityTest', model, year: 2024, price: 10000, mileage: 1,
      fuelType: 'GASOLINA', fuelConsumption: 5, transmission: 'MANUAL', bodyType: 'SEDAN',
      imageUrl: 'https://img.test/security.jpg', description: payload, stock: 1 },
  })
  expect(created.status()).toBe(201)
  const car = await created.json()
  try {
    let dialogs = 0
    page.on('dialog', async (dialog) => { dialogs++; await dialog.dismiss() })
    await page.goto('/?brand=SecurityTest#/catalog')
    await page.getByRole('button', { name: `Ver detalle de SecurityTest ${model}`, exact: true }).click()
    await expect(page.getByRole('dialog')).toContainText(payload)
    expect(await page.getByRole('dialog').locator('script, img[onerror]').count()).toBe(0)
    expect(await page.evaluate(() => Reflect.get(window, '__driveaiXss'))).toBeUndefined()
    expect(dialogs).toBe(0)

    const session = await registerCustomer(request, 'xss-ai')
    await seedSession(page, session)
    // Aislar el renderer de una respuesta no confiable sin llamar al proveedor real.
    await page.route('**/api/ai/recommend', (route) => route.fulfill({ json: {
      sessionId: 'xss-test', reasoning: payload, recommendations: [],
    } }))
    await page.goto('/#/ai')
    await page.getByPlaceholder('Describe el auto que buscas.').fill('Recomienda un auto')
    await page.getByRole('button', { name: /Enviar/ }).click()
    const answer = page.getByTestId('ai-assistant-message')
    await expect(answer).toContainText(payload)
    expect(await answer.locator('script, img[onerror]').count()).toBe(0)
    expect(await page.evaluate(() => Reflect.get(window, '__driveaiXss'))).toBeUndefined()
    expect(dialogs).toBe(0)
  } finally {
    const deleted = await request.delete(`${API_URL}/api/cars/${car.id}`, {
      headers: { Authorization: `Bearer ${token}` },
    })
    expect(deleted.status()).toBe(204)
  }
})
