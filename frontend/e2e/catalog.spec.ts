import { expect, test } from '@playwright/test'

test.describe('HU-CAT — Catálogo, búsqueda y detalle', () => {
  test('lista el catálogo, busca por marca y abre el detalle de un vehículo', async ({ page }) => {
    await page.goto('/#/catalog')
    await expect(
      page.getByRole('heading', { name: 'Catálogo de autos certificados' }),
    ).toBeVisible()

    // Espera a que la primera página del catálogo se renderice.
    await expect(page.getByRole('button', { name: /Ver detalle de / }).first()).toBeVisible()

    // Búsqueda libre con debounce de 400 ms.
    const search = page.getByPlaceholder('Buscar marca, modelo…').first()
    await search.fill('Mazda')
    await expect(page.getByRole('heading', { name: /Mazda/i }).first()).toBeVisible()

    // Abre el detalle del primer resultado y valida contenido real del vehículo.
    await page.getByRole('button', { name: /Ver detalle de / }).first().click()
    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    await expect(dialog.getByRole('heading', { level: 2 })).toContainText('Mazda')
    await expect(dialog.getByText('Kilometraje')).toBeVisible()
    await expect(dialog.getByText('Transmisión')).toBeVisible()
    await expect(dialog.getByRole('button', { name: 'Volver al catálogo' })).toBeVisible()

    await dialog.getByRole('button', { name: 'Volver al catálogo' }).click()
    await expect(dialog).toBeHidden()
  })

  test('búsqueda sin resultados muestra estado vacío y permite limpiar filtros', async ({
    page,
  }) => {
    await page.goto('/#/catalog')
    const search = page.getByPlaceholder('Buscar marca, modelo…').first()
    await search.fill('zzzz-no-existe-zzzz')

    await expect(page.getByText('Sin resultados')).toBeVisible()
    await page.getByRole('button', { name: 'Limpiar filtros' }).click()
    await expect(page.getByRole('button', { name: /Ver detalle de / }).first()).toBeVisible()
  })

  test('el detalle se cierra con la tecla Escape', async ({ page }) => {
    await page.goto('/#/catalog')
    await page.getByRole('button', { name: /Ver detalle de / }).first().click()
    await expect(page.getByRole('dialog')).toBeVisible()
    await page.keyboard.press('Escape')
    await expect(page.getByRole('dialog')).toBeHidden()
  })
})
