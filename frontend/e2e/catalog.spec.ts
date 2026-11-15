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

  test('el detalle se superpone a toda la página aunque la tarjeta esté en hover', async ({
    page,
  }) => {
    // Regresión: la tarjeta se eleva con `transform` al pasar el mouse y eso la
    // convertía en el contenedor del modal `fixed`, que quedaba recortado y
    // encogido dentro de la tarjeta. Ahora el diálogo se monta bajo <body>.
    await page.goto('/#/catalog')
    const trigger = page.getByRole('button', { name: /Ver detalle de / }).first()
    await trigger.hover()
    await trigger.click()

    const dialog = page.getByRole('dialog')
    await expect(dialog).toBeVisible()
    expect(await dialog.evaluate((node) => node.parentElement === document.body)).toBe(true)

    const box = await dialog.boundingBox()
    const viewport = page.viewportSize()!
    expect(box?.width).toBeGreaterThanOrEqual(viewport.width - 1)
    expect(box?.height).toBeGreaterThanOrEqual(viewport.height - 1)

    // La página de fondo no se desplaza mientras el diálogo está abierto…
    await expect
      .poll(() => page.evaluate(() => document.body.style.overflow))
      .toBe('hidden')
    await dialog.getByRole('button', { name: 'Cerrar detalle' }).click()
    await expect(dialog).toBeHidden()
    // …y recupera el scroll al cerrarlo.
    await expect.poll(() => page.evaluate(() => document.body.style.overflow)).toBe('')
  })

  test('una imagen que no carga muestra un reemplazo en vez de un esqueleto infinito', async ({
    page,
  }) => {
    await page.route('**/images.unsplash.com/**', (route) => route.abort())
    await page.goto('/#/catalog')
    await expect(page.getByRole('button', { name: /Ver detalle de / }).first()).toBeVisible()
    await expect(page.getByText('Imagen no disponible').first()).toBeVisible()
  })

  test('escribir en el buscador desde el inicio lleva al catálogo filtrado', async ({ page }) => {
    await page.goto('/')
    await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible()
    await page.getByPlaceholder('Buscar marca, modelo…').first().fill('Mazda')
    await expect(
      page.getByRole('heading', { name: 'Catálogo de autos certificados' }),
    ).toBeVisible()
    await expect(page.getByRole('heading', { name: /Mazda/i }).first()).toBeVisible()
    await expect(page).toHaveURL(/#\/catalog$/)
  })

  test('la vista actual se conserva en el hash al navegar con la barra superior', async ({
    page,
  }) => {
    await page.goto('/')
    await page.getByRole('banner').getByRole('button', { name: 'Productos' }).click()
    await expect(page).toHaveURL(/#\/catalog$/)
    await page.reload()
    await expect(
      page.getByRole('heading', { name: 'Catálogo de autos certificados' }),
    ).toBeVisible()
    await page.goBack()
    await expect(page).toHaveURL(/#\/home$/)
    await expect(page.getByRole('heading', { level: 1 }).first()).toBeVisible()
  })
})
